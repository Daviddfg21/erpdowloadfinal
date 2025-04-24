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
import com.comerzzia.core.util.xml.XMLDocumentException;
import com.comerzzia.integrations.model.catalog.promotions.PromotionItem;
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypeNxM;
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypeNxMGroup;
import com.comerzzia.model.ventas.promociones.detalles.tipos.especificos.bean.DetalleNxMPromocionBean;
import com.comerzzia.model.ventas.tarifas.articulos.ArticuloTarifaBean;
import com.comerzzia.persistencia.ventas.promociones.detalles.ParametrosBuscarDetallesPromocionesBean;
import com.comerzzia.servicios.ventas.promociones.detalle.especificos.nxm.DetalleNxMPromocionException;
import com.comerzzia.servicios.ventas.promociones.detalle.especificos.nxm.DetalleNxMPromocionesService;
import com.comerzzia.servicios.ventas.promociones.tipos.PromocionListaArticulosBean;
import com.comerzzia.servicios.ventas.tarifas.articulos.ArticuloTarifaException;
import com.comerzzia.servicios.ventas.tarifas.articulos.ArticuloTarifaNotFoundException;
import com.comerzzia.servicios.ventas.tarifas.articulos.ServicioArticulosTarifasImpl;

@Service
public class PromotionTypeNxMManager extends PromotionTypeAbstractManager<PromotionTypeNxM, PromocionListaArticulosBean<DetalleNxMPromocionBean>> {
	@Autowired
	private DetalleNxMPromocionesService servicioDetallePromocion;
	
	private ServicioArticulosTarifasImpl servicioArticulosTarifas;
	
	@PostConstruct
	protected void init() {
		servicioArticulosTarifas = ServicioArticulosTarifasImpl.get();
	}
	
	@Override
	protected void erpPromotionToCZZCustomData(final PromotionTypeNxM erpPromotionDTO, PromocionListaArticulosBean<DetalleNxMPromocionBean> promotion, final DatosSesionBean sessionData) {
		promotion.setArticulosCargados(true);
				
		List<DetalleNxMPromocionBean> listaArticulos = new ArrayList<>();
				
		for(PromotionTypeNxMGroup erpItemGroup: erpPromotionDTO.getItemsGroups()) {
			DetalleNxMPromocionBean promoItemGroup = null;
			
			promoItemGroup = new DetalleNxMPromocionBean();
			promoItemGroup.setEstadoBean(Estado.NUEVO);
			
			promoItemGroup.setIdAgrupacion(erpItemGroup.getGroupId());
			promoItemGroup.setN(erpItemGroup.getComboUnits().longValue());
			promoItemGroup.setM(erpItemGroup.getComboUnitsToPay().longValue());
			promoItemGroup.setDescuento(erpItemGroup.getApplyDiscount().doubleValue());
			promoItemGroup.setTextoAgrupacion(erpItemGroup.getGroupDes());
			promoItemGroup.setImagenPromocion(erpItemGroup.getPromotionImageURL());
			promoItemGroup.setTipoFiltro(getTipoFiltro(erpItemGroup.getApplyDiscountTypeId()));
			promoItemGroup.setFechaInicio(erpPromotionDTO.getStartDate());
			promoItemGroup.setFechaFin(erpPromotionDTO.getEndDate());
			
			List<DetalleNxMPromocionBean> articulosAsociados = new ArrayList<>();
			List<DetalleNxMPromocionBean> articulosAsociadosOriginal = new ArrayList<>();
			
			for (PromotionItem erpItem : erpItemGroup.getPromotionItems()) {
				DetalleNxMPromocionBean promoItem = null;
				
				if (promotion.getPromocionBean().getEstadoBean() == Estado.MODIFICADO) {
					//Consultamos si existe la agrupacion
					try{
						// 		
						promoItem = servicioDetallePromocion.consultarDetalleNxMPromocion(sessionData.getConfigEmpresa(), String.valueOf(promotion.getIdPromocion()), erpItem.getItemCode());
						
						// SE ASIGNA DESGLOSE2 PORQUE EL DAO TIENE UN ERROR Y NO ASIGNA EL VALOR 
						// A LA PROPIEDAD DESGLOSE2
						promoItem.setDesglose2(erpItem.getCombination1Code());
						
						promoItem.setEstadoBean(Estado.MODIFICADO);
						articulosAsociadosOriginal.add(promoItem);
					}catch(Exception ignore){					
					}
				}
				
				if (promoItem == null) {
					promoItem = new DetalleNxMPromocionBean();
					promoItem.setEstadoBean(Estado.NUEVO);
					promoItem.setCodArt(erpItem.getItemCode());
					promoItem.setDesglose1(erpItem.getCombination1Code());
					promoItem.setDesglose2(erpItem.getCombination2Code());
					promoItem.setFechaInicio(erpPromotionDTO.getStartDate());
					promoItem.setFechaFin(erpPromotionDTO.getEndDate());
				}
				
				promoItem.setIdAgrupacion(erpItemGroup.getGroupId());
				promoItem.setTextoAgrupacion(erpItemGroup.getGroupDes());
				
				promoItem.setN(erpItemGroup.getComboUnits().longValue());
				promoItem.setM(erpItemGroup.getComboUnitsToPay().longValue());
				promoItem.setDescuento(erpItemGroup.getApplyDiscount().doubleValue());
				
				promoItem.setTextoPromocion(erpItemGroup.getPromotionText());
				promoItem.setImagenPromocion(erpItemGroup.getPromotionImageURL());
				promoItem.setTipoFiltro(getTipoFiltro(erpItemGroup.getApplyDiscountTypeId()));
				
				//Item
				ArticuloTarifaBean articuloTarifa = null;
				try {
					articuloTarifa = servicioArticulosTarifas.consultarArticuloTarifa(promotion.getCodTar(), erpItem.getItemCode(), erpItem.getCombination1Code(), erpItem.getCombination2Code(), DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH), sessionData);
				} catch(ArticuloTarifaNotFoundException | ArticuloTarifaException ignorar){}
				
				if(articuloTarifa != null) {
					promoItem.setPrecioTarifa(articuloTarifa.getPrecioVenta());
					promoItem.setPrecioTarifaConImpuestos(articuloTarifa.getPrecioTotal());
				} else {
					promoItem.setPrecioTarifa(0.);
					promoItem.setPrecioTarifaConImpuestos(0.);
				}
										
				if (erpItem.getStartDate() != null) {
					promoItem.setFechaInicio(DateUtils.truncate(erpItem.getStartDate(), Calendar.DAY_OF_MONTH));
				}
				
				if (erpItem.getEndDate() != null) {
					promoItem.setFechaFin(DateUtils.truncate(erpItem.getEndDate(), Calendar.DAY_OF_MONTH));
				}
				
				articulosAsociados.add(promoItem);				
			}
			
			promoItemGroup.setListaArticulosAsociados(articulosAsociados);
			promoItemGroup.setListaArticulosAsociadosOriginal(articulosAsociadosOriginal);
			
			listaArticulos.add(promoItemGroup);
		}
		
		promotion.setListaDetalles(listaArticulos);		
	}

	@Override
	protected void czzPromotionToERPCustomData(PromotionTypeNxM erpPromotionDTO, PromocionListaArticulosBean<DetalleNxMPromocionBean> czzPromotion, final DatosSesionBean sessionData) {
		ParametrosBuscarDetallesPromocionesBean params = new ParametrosBuscarDetallesPromocionesBean();
		params.setIdPromocion(erpPromotionDTO.getPromotionId());
		
		List<DetalleNxMPromocionBean> articulos;
		try {
			articulos = servicioDetallePromocion.consultarDetalleNxMPromocion(params, sessionData);
		} catch (DetalleNxMPromocionException | XMLDocumentException e) {
			throw new RuntimeException(e);
		}
		
		List<PromotionTypeNxMGroup> itemsGroups = new ArrayList<>();
		
		for(DetalleNxMPromocionBean czzPromoGroup: articulos) {
		    PromotionTypeNxMGroup promotionGroup = new PromotionTypeNxMGroup();
			promotionGroup.setGroupId(czzPromoGroup.getIdAgrupacion());
			promotionGroup.setGroupDes(czzPromoGroup.getTextoAgrupacion());
			promotionGroup.setPromotionImageURL(czzPromoGroup.getImagenPromocion());
			promotionGroup.setPromotionText(czzPromoGroup.getTextoPromocion());
			promotionGroup.setComboUnits(czzPromoGroup.getN().intValue());
			promotionGroup.setComboUnitsToPay(czzPromoGroup.getM().intValue());
			promotionGroup.setApplyDiscount(getValueAsBigDecimal(czzPromoGroup.getDescuento(), 2));
			promotionGroup.setApplyDiscountTypeId(getTipoFiltro(czzPromoGroup.getTipoFiltro()));
			promotionGroup.setPromotionItems(new ArrayList<>());
			
			itemsGroups.add(promotionGroup);
			
			for (DetalleNxMPromocionBean czzPromoItem : czzPromoGroup.getListaArticulosAsociados()) {
				PromotionItem erpPromotionItem = new PromotionItem();
				erpPromotionItem.setItemCode(czzPromoItem.getCodArt());
				erpPromotionItem.setCombination1Code(czzPromoItem.getDesglose1());
				erpPromotionItem.setCombination2Code(czzPromoItem.getDesglose2());
				
				erpPromotionItem.setPromotionText(czzPromoItem.getTextoPromocion());
				erpPromotionItem.setStartDate(czzPromoItem.getFechaInicio());
				erpPromotionItem.setEndDate(czzPromoItem.getFechaFin());
									
				promotionGroup.getPromotionItems().add(erpPromotionItem);	
			}			
		}
				
		erpPromotionDTO.setItemsGroups(itemsGroups);		
	}
}
