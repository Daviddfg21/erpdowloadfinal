package com.comerzzia.custom.erp.services.promotions.types;

import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypeTextIssuer;
import com.comerzzia.servicios.ventas.promociones.tipos.especificos.PromocionTextoBean;

@Service
public class PromotionTypeTextManager extends PromotionTypeAbstractManager<PromotionTypeTextIssuer, PromocionTextoBean> {

	@Override
	protected void erpPromotionToCZZCustomData(final PromotionTypeTextIssuer erpPromotionDTO, PromocionTextoBean promotion, final DatosSesionBean sessionData) {
		promotion.setCondicion1(readRules(erpPromotionDTO.getItemsConditionsRules()));
						
		promotion.setVisibleVentas(erpPromotionDTO.getShowToOperator()); 
				
		promotion.setTextoPromocion(erpPromotionDTO.getPromotionText());
		promotion.setImagenPromocion(erpPromotionDTO.getPromotionImageURL());	
	}
	
	@Override
	protected void czzPromotionToERPCustomData(PromotionTypeTextIssuer erpPromotionDTO, final PromocionTextoBean czzPromotion, final DatosSesionBean sessionData) {
		erpPromotionDTO.setItemsConditionsRules(rulesToBase64(czzPromotion.getCondicion1()));

		erpPromotionDTO.setShowToOperator(czzPromotion.isVisibleVentas());
				
		erpPromotionDTO.setPromotionText(czzPromotion.getTextoPromocion());
		erpPromotionDTO.setPromotionImageURL(czzPromotion.getImagenPromocion());				
	}	

}
