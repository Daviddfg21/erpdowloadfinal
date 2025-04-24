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
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypeRappel;
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypeRappelGroup;
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypeRappelRange;
import com.comerzzia.model.ventas.promociones.detalles.tipos.especificos.bean.DetalleEscaladoPromocionBean;
import com.comerzzia.model.ventas.tarifas.articulos.ArticuloTarifaBean;
import com.comerzzia.persistencia.ventas.promociones.detalles.ParametrosBuscarDetallesPromocionesBean;
import com.comerzzia.servicios.ventas.promociones.detalle.especificos.escalado.DetalleEscaladoPromocionException;
import com.comerzzia.servicios.ventas.promociones.detalle.especificos.escalado.DetalleEscaladoPromocionesService;
import com.comerzzia.servicios.ventas.promociones.tipos.PromocionListaArticulosBean;
import com.comerzzia.servicios.ventas.tarifas.articulos.ArticuloTarifaException;
import com.comerzzia.servicios.ventas.tarifas.articulos.ArticuloTarifaNotFoundException;
import com.comerzzia.servicios.ventas.tarifas.articulos.ServicioArticulosTarifasImpl;

@Service
public class PromotionTypeRappelManager extends PromotionTypeAbstractManager<PromotionTypeRappel, PromocionListaArticulosBean<DetalleEscaladoPromocionBean>> {
	@Autowired
	private DetalleEscaladoPromocionesService servicioDetallePromocion;
	
	private ServicioArticulosTarifasImpl servicioArticulosTarifas;
	
	@PostConstruct
	protected void init() {
		servicioArticulosTarifas = ServicioArticulosTarifasImpl.get();
	}
	
	@Override
	protected void erpPromotionToCZZCustomData(final PromotionTypeRappel erpPromotionDTO, PromocionListaArticulosBean<DetalleEscaladoPromocionBean> promotion, final DatosSesionBean sessionData) {
		promotion.setArticulosCargados(true);		
		
		ParametrosBuscarDetallesPromocionesBean detailParams = new ParametrosBuscarDetallesPromocionesBean();
		detailParams.setIdPromocion(promotion.getIdPromocion());
		
		List<DetalleEscaladoPromocionBean> listaArticulos = new ArrayList<>();
				
		for(PromotionTypeRappelGroup erpItemGroup: erpPromotionDTO.getItemsGroups()) {
			// cargar matriz de rappel, que es común para todo el grupo
			ArrayList<ArrayList<String>> matriz = new ArrayList<ArrayList<String>>();
									
			for (PromotionTypeRappelRange range : erpItemGroup.getRanges()) {
				ArrayList<String> nuevaLinea = new ArrayList<String>();
				nuevaLinea.add(range.getQuantityFrom().toString());
				nuevaLinea.add(range.getQuantityTo().toString());
				nuevaLinea.add(range.getApplyDiscountValue().setScale(2).toPlainString());
				matriz.add(nuevaLinea);
			}
			
			DetalleEscaladoPromocionBean promoItemGroup = new DetalleEscaladoPromocionBean();
			promoItemGroup.setEstadoBean(Estado.NUEVO);
			
			promoItemGroup.setIdAgrupacion(erpItemGroup.getGroupId());
			promoItemGroup.setMatriz(matriz);			
			promoItemGroup.setTextoAgrupacion(erpItemGroup.getGroupDes());
			promoItemGroup.setImagenPromocion(erpItemGroup.getPromotionImageURL());
			promoItemGroup.setTipoFiltro(getTipoFiltro(erpItemGroup.getApplyDiscountTypeId()));
			promoItemGroup.setFechaInicio(erpPromotionDTO.getStartDate());
			promoItemGroup.setFechaFin(erpPromotionDTO.getEndDate());
			
			List<DetalleEscaladoPromocionBean> articulosAsociados = new ArrayList<>();
			List<DetalleEscaladoPromocionBean> articulosAsociadosOriginal = new ArrayList<>();
			
			for (PromotionItem erpItem : erpItemGroup.getPromotionItems()) {
				DetalleEscaladoPromocionBean promoItem = null;
				
				if (promotion.getPromocionBean().getEstadoBean() == Estado.MODIFICADO) {
					//Consultamos si existe el artículo
					try{					
						detailParams.setCodArt(erpItem.getItemCode());
						List<DetalleEscaladoPromocionBean> currentDetail = servicioDetallePromocion.consultar(detailParams, sessionData);
						
						if (currentDetail.size() == 1) {
							promoItem = currentDetail.get(0);
							promoItem.setEstadoBean(Estado.MODIFICADO);
							articulosAsociadosOriginal.add(promoItem);
						}
					}catch(Exception ignore){					
					}
				}
				
				if (promoItem == null) {
					promoItem = new DetalleEscaladoPromocionBean();
					promoItem.setEstadoBean(Estado.NUEVO);
					promoItem.setCodArt(erpItem.getItemCode());
					promoItem.setDesglose1(erpItem.getCombination1Code());
					promoItem.setDesglose2(erpItem.getCombination2Code());
					promoItem.setFechaInicio(erpPromotionDTO.getStartDate());
					promoItem.setFechaFin(erpPromotionDTO.getEndDate());
				}
				
				promoItem.setIdAgrupacion(erpItemGroup.getGroupId());
				promoItem.setTextoAgrupacion(erpItemGroup.getGroupDes());
				
				promoItem.setMatriz(matriz);				
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
	protected void czzPromotionToERPCustomData(PromotionTypeRappel erpPromotionDTO, final PromocionListaArticulosBean<DetalleEscaladoPromocionBean> czzPromotion, final DatosSesionBean sessionData) {
		ParametrosBuscarDetallesPromocionesBean params = new ParametrosBuscarDetallesPromocionesBean();
		params.setIdPromocion(erpPromotionDTO.getPromotionId());
		
		List<DetalleEscaladoPromocionBean> articulos;
		try {
			articulos = servicioDetallePromocion.consultar(params, sessionData);
		} catch (DetalleEscaladoPromocionException e) {
			throw new RuntimeException(e);
		}
		
		List<PromotionTypeRappelGroup> itemsGroups = new ArrayList<>();
		
		for(DetalleEscaladoPromocionBean czzPromoGroup: articulos) {
			PromotionTypeRappelGroup promotionGroup = new PromotionTypeRappelGroup();

			promotionGroup.setGroupId(czzPromoGroup.getIdAgrupacion());
			promotionGroup.setGroupDes(czzPromoGroup.getTextoAgrupacion());
			promotionGroup.setPromotionImageURL(czzPromoGroup.getImagenPromocion());
			promotionGroup.setPromotionText(czzPromoGroup.getTextoPromocion());				
			promotionGroup.setPromotionItems(new ArrayList<>());
			promotionGroup.setRanges(new ArrayList<>());
			try {
				czzPromoGroup.leerDatosPromocion();
				
				// needs read promotion data xml
				promotionGroup.setApplyDiscountTypeId(getTipoFiltro(czzPromoGroup.getTipoFiltro()));
				
				for(ArrayList<String> czzRange : czzPromoGroup.getMatriz()) {
					PromotionTypeRappelRange range = new PromotionTypeRappelRange();
					
					range.setQuantityFrom(getValueAsBigDecimal(czzRange.get(0)));
					range.setQuantityTo(getValueAsBigDecimal(czzRange.get(1)));
					range.setApplyDiscountValue(getValueAsBigDecimal(czzRange.get(2)));
					promotionGroup.getRanges().add(range);
				}				
			} catch (XMLDocumentException e) {
				throw new RuntimeException("La promoción " + czzPromoGroup.getIdPromocion() + " contiene errores en sus datos de promocion: " + e.getMessage() + "\n" + new String(czzPromoGroup.getDatosPromocion()));
			}
				
            itemsGroups.add(promotionGroup);
			
			for (DetalleEscaladoPromocionBean czzPromoItem : czzPromoGroup.getListaArticulosAsociados()) {
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
