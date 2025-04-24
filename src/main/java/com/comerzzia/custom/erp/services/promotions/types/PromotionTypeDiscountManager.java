package com.comerzzia.custom.erp.services.promotions.types;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.core.util.base.Estado;
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypeDiscount;
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypeDiscountItem;
import com.comerzzia.model.ventas.promociones.detalles.tipos.especificos.bean.DetallePorcentajePromocionBean;
import com.comerzzia.model.ventas.tarifas.articulos.ArticuloTarifaBean;
import com.comerzzia.persistencia.ventas.promociones.detalles.ParametrosBuscarDetallesPromocionesBean;
import com.comerzzia.servicios.ventas.promociones.detalle.especificos.porcentaje.DetallePorcentajePromocionesService;
import com.comerzzia.servicios.ventas.promociones.tipos.PromocionListaArticulosBean;
import com.comerzzia.servicios.ventas.tarifas.articulos.ArticuloTarifaException;
import com.comerzzia.servicios.ventas.tarifas.articulos.ArticuloTarifaNotFoundException;
import com.comerzzia.servicios.ventas.tarifas.articulos.ServicioArticulosTarifasImpl;

@Service
public class PromotionTypeDiscountManager extends PromotionTypeAbstractManager<PromotionTypeDiscount, PromocionListaArticulosBean<DetallePorcentajePromocionBean>> {
	@Autowired
	protected DetallePorcentajePromocionesService servicioDetallePorcentajePromociones;
	
	private ServicioArticulosTarifasImpl servicioArticulosTarifas;
	
	@PostConstruct
	protected void init() {
		servicioArticulosTarifas = ServicioArticulosTarifasImpl.get();
	}
	
	@Override
	protected void erpPromotionToCZZCustomData(final PromotionTypeDiscount erpPromotionDTO, PromocionListaArticulosBean<DetallePorcentajePromocionBean> promotion, final DatosSesionBean sessionData) {

		promotion.setArticulosCargados(true);
				
		List<DetallePorcentajePromocionBean> promoItems = new ArrayList<>();
		
		for(PromotionTypeDiscountItem erpPromotionItem: erpPromotionDTO.getPromotionItems()) {
			DetallePorcentajePromocionBean promoItem = null;
			
			if (promotion.getPromocionBean().getEstadoBean() == Estado.MODIFICADO) {
				try {
					promoItem = servicioDetallePorcentajePromociones.consultarDetallePorcentajePromocion(sessionData.getConfigEmpresa(), String.valueOf(promotion.getIdPromocion()), erpPromotionItem.getItemCode());
					
					promoItem.setEstadoBean(Estado.MODIFICADO);
				} catch(Exception ignore) {}
			}
			
			if (promoItem == null) {
				promoItem = new DetallePorcentajePromocionBean();
				promoItem.setEstadoBean(Estado.NUEVO);

				ArticuloTarifaBean articuloTarifa = null;
				try {
					articuloTarifa = servicioArticulosTarifas.consultarArticuloTarifa(promotion.getCodTar(), erpPromotionItem.getItemCode(), erpPromotionItem.getCombination1Code(), erpPromotionItem.getCombination2Code(), DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH), sessionData);
				} catch(ArticuloTarifaNotFoundException | ArticuloTarifaException ignorar){}
				
				promoItem.setCodArt(erpPromotionItem.getItemCode());
				promoItem.setDesglose1(erpPromotionItem.getCombination1Code());
				promoItem.setDesglose2(erpPromotionItem.getCombination2Code());
				
				if(articuloTarifa != null) {
					promoItem.setPrecioTarifa(articuloTarifa.getPrecioVenta());
					promoItem.setPrecioTarifaConImpuestos(articuloTarifa.getPrecioTotal());
				} else {
					promoItem.setPrecioTarifa(0.);
					promoItem.setPrecioTarifaConImpuestos(0.);
				}				
			}
			
			promoItem.setTextoPromocion(erpPromotionItem.getPromotionText());
			promoItem.setTipoFiltro(getTipoFiltro(erpPromotionItem.getApplyDiscountTypeId()));
			promoItem.setDescuento(erpPromotionItem.getDiscount());
			
			// solo se guardan a modo informativo para ser presentados en pantalla
			promoItem.setPrecioTotal(0.0);
			promoItem.setPrecioVenta(0.0);
			
			updatePromoItemDateRange(erpPromotionDTO, promotion, erpPromotionItem, promoItem);
			
			promoItems.add(promoItem);
		}
		
		promotion.setListaDetalles(promoItems);		
	}
	
	@Override
	protected void czzPromotionToERPCustomData(PromotionTypeDiscount erpPromotionDTO, final PromocionListaArticulosBean<DetallePorcentajePromocionBean> czzPromotion, final DatosSesionBean sessionData) {
		ParametrosBuscarDetallesPromocionesBean params = new ParametrosBuscarDetallesPromocionesBean();
		params.setIdPromocion(erpPromotionDTO.getPromotionId());
		
		List<DetallePorcentajePromocionBean> articulos;
		try {
			articulos = servicioDetallePorcentajePromociones.consultarDetallePorcentajePromocion(params, sessionData);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
			
		List<PromotionTypeDiscountItem> promotionItems = new ArrayList<>();
		
		for(DetallePorcentajePromocionBean promoItem: articulos) {
			PromotionTypeDiscountItem erpPromotionItem = new PromotionTypeDiscountItem();
			erpPromotionItem.setItemCode(promoItem.getCodArt());
			erpPromotionItem.setCombination1Code(promoItem.getDesglose1());
			erpPromotionItem.setCombination2Code(promoItem.getDesglose2());
			erpPromotionItem.setPromotionText(promoItem.getTextoPromocion());
			erpPromotionItem.setStartDate(promoItem.getFechaInicio());
			erpPromotionItem.setEndDate(promoItem.getFechaFin());
			erpPromotionItem.setApplyDiscountTypeId(getTipoFiltro(promoItem.getTipoFiltro()));
			erpPromotionItem.setDiscount(promoItem.getDescuento());
									
			promotionItems.add(erpPromotionItem);
		}
		
		erpPromotionDTO.setPromotionItems(promotionItems);
	}
}
