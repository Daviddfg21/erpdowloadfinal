package com.comerzzia.custom.erp.services.promotions.types;

import java.math.BigDecimal;
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
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypePrice;
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypePriceItem;
import com.comerzzia.model.ventas.promociones.detalles.tipos.especificos.bean.DetallePrecioPuntosPromocionBean;
import com.comerzzia.model.ventas.tarifas.articulos.ArticuloTarifaBean;
import com.comerzzia.persistencia.ventas.promociones.detalles.ParametrosBuscarDetallesPromocionesBean;
import com.comerzzia.servicios.ventas.promociones.detalle.especificos.preciopuntos.DetallePrecioPuntosPromocionException;
import com.comerzzia.servicios.ventas.promociones.detalle.especificos.preciopuntos.DetallePrecioPuntosPromocionesService;
import com.comerzzia.servicios.ventas.promociones.tipos.PromocionListaArticulosBean;
import com.comerzzia.servicios.ventas.tarifas.articulos.ArticuloTarifaException;
import com.comerzzia.servicios.ventas.tarifas.articulos.ArticuloTarifaNotFoundException;
import com.comerzzia.servicios.ventas.tarifas.articulos.ServicioArticulosTarifasImpl;

@Service
public class PromotionTypePriceManager extends PromotionTypeAbstractManager<PromotionTypePrice, PromocionListaArticulosBean<DetallePrecioPuntosPromocionBean>> {
	@Autowired
	private DetallePrecioPuntosPromocionesService servicioDetallePromocion;
	
	private ServicioArticulosTarifasImpl servicioArticulosTarifas;
	
	@PostConstruct
	protected void init() {
		servicioArticulosTarifas = ServicioArticulosTarifasImpl.get();
	}
	
	@Override
	protected void erpPromotionToCZZCustomData(final PromotionTypePrice erpPromotionDTO, PromocionListaArticulosBean<DetallePrecioPuntosPromocionBean> promotion, final DatosSesionBean sessionData) {

		promotion.setArticulosCargados(true);
				
		List<DetallePrecioPuntosPromocionBean> promoItems = new ArrayList<>();
		
		for(PromotionTypePriceItem erpPromotionItem: erpPromotionDTO.getPromotionItems()) {
			DetallePrecioPuntosPromocionBean promoItem = null;
			
			if (promotion.getPromocionBean().getEstadoBean() == Estado.MODIFICADO) {
				try {
					promoItem = servicioDetallePromocion.consultarDetallePrecioPuntosPromocion(sessionData.getConfigEmpresa(), String.valueOf(promotion.getIdPromocion()), erpPromotionItem.getItemCode());
					
					if(!promoItem.getPrecioTotal().equals(erpPromotionItem.getSalesPriceWithTaxes().doubleValue())) {
						promoItem.setEstadoBean(Estado.MODIFICADO);
					}
				} catch(Exception ignore) {}
			}
			
			if (promoItem == null) {
				promoItem = new DetallePrecioPuntosPromocionBean();
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
			
			promoItem.setPuntos(erpPromotionItem.getPoints() != null ? new BigDecimal(erpPromotionItem.getPoints()) : null);
			promoItem.setPrecioTotal(erpPromotionItem.getSalesPriceWithTaxes().doubleValue());
			promoItem.setPrecioVenta(erpPromotionItem.getSalesPrice().doubleValue());
			promoItem.setTextoPromocion(erpPromotionItem.getPromotionText());
			
			updatePromoItemDateRange(erpPromotionDTO, promotion, erpPromotionItem, promoItem);			
			
			promoItems.add(promoItem);
		}
		
		promotion.setListaDetalles(promoItems);		
	}
	
	@Override
	protected void czzPromotionToERPCustomData(PromotionTypePrice erpPromotionDTO, final PromocionListaArticulosBean<DetallePrecioPuntosPromocionBean> czzPromotion, final DatosSesionBean sessionData) {
		ParametrosBuscarDetallesPromocionesBean params = new ParametrosBuscarDetallesPromocionesBean();
		params.setIdPromocion(erpPromotionDTO.getPromotionId());
		
		List<DetallePrecioPuntosPromocionBean> articulos;
		try {
			articulos = servicioDetallePromocion.consultarDetallePrecioPuntosPromocion(params, sessionData);
		} catch (DetallePrecioPuntosPromocionException e) {
			throw new RuntimeException(e);
		}
			
		List<PromotionTypePriceItem> promotionItems = new ArrayList<>();
		
		for(DetallePrecioPuntosPromocionBean promoItem: articulos) {
			PromotionTypePriceItem erpPromotionItem = new PromotionTypePriceItem();
			erpPromotionItem.setItemCode(promoItem.getCodArt());
			erpPromotionItem.setCombination1Code(promoItem.getDesglose1());
			erpPromotionItem.setCombination2Code(promoItem.getDesglose2());
			erpPromotionItem.setPoints(promoItem.getPuntos() != null ? promoItem.getPuntos().longValue() : null);
			erpPromotionItem.setSalesPriceWithTaxes(promoItem.getPrecioTotal());
			erpPromotionItem.setSalesPrice(promoItem.getPrecioVenta());
			erpPromotionItem.setPromotionText(promoItem.getTextoPromocion());
			erpPromotionItem.setStartDate(promoItem.getFechaInicio());
			erpPromotionItem.setEndDate(promoItem.getFechaFin());
									
			promotionItems.add(erpPromotionItem);
		}
		
		erpPromotionDTO.setPromotionItems(promotionItems);
	}

}
