package com.comerzzia.custom.erp.services.promotions.types;

import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypeComboHeader;
import com.comerzzia.servicios.ventas.promociones.tipos.especificos.PromocionDescuentoCombinadoCabeceraBean;

@Service
public class PromotionTypeComboHeaderManager extends PromotionTypeAbstractManager<PromotionTypeComboHeader, PromocionDescuentoCombinadoCabeceraBean> {


	@Override
	protected void erpPromotionToCZZCustomData(final PromotionTypeComboHeader erpPromotionDTO,
			PromocionDescuentoCombinadoCabeceraBean promotion, final DatosSesionBean sessionData) {
		promotion.setCondicion1(readRules(erpPromotionDTO.getItemsConditionsRules()));
		
		promotion.setLineasAplicacion(readRules(erpPromotionDTO.getItemsApplyRules()));
		promotion.setAplicacion(readRules(erpPromotionDTO.getRangesApplyRules()));
		
		promotion.setTextoPromocion(erpPromotionDTO.getPromotionText());
		promotion.setImagenPromocion(erpPromotionDTO.getPromotionImageURL());
		promotion.setTipoFiltro(getTipoFiltro(erpPromotionDTO.getApplyDiscountTypeId()));
	}

	@Override
	protected void czzPromotionToERPCustomData(PromotionTypeComboHeader erpPromotionDTO, final PromocionDescuentoCombinadoCabeceraBean czzPromotion, final DatosSesionBean sessionData) {		
	    erpPromotionDTO.setItemsConditionsRules(rulesToBase64(czzPromotion.getCondicion1()));

		erpPromotionDTO.setItemsApplyRules(rulesToBase64(czzPromotion.getLineasAplicacion()));			
		
		erpPromotionDTO.setRangesApplyRules(rulesToBase64(czzPromotion.getAplicacion()));			
				
		erpPromotionDTO.setPromotionText(czzPromotion.getTextoPromocion());
		erpPromotionDTO.setPromotionImageURL(czzPromotion.getImagenPromocion());
		erpPromotionDTO.setApplyDiscountTypeId(getTipoFiltro(czzPromotion.getTipoFiltro()));				
	}
}
