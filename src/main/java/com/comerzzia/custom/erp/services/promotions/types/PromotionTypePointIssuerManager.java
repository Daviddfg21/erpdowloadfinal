package com.comerzzia.custom.erp.services.promotions.types;

import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypePointIssuer;
import com.comerzzia.servicios.ventas.promociones.tipos.especificos.PromocionPuntosBean;

@Service
public class PromotionTypePointIssuerManager extends PromotionTypeAbstractManager<PromotionTypePointIssuer, PromocionPuntosBean> {

	@Override
	protected void erpPromotionToCZZCustomData(final PromotionTypePointIssuer erpPromotionDTO, PromocionPuntosBean promotion, final DatosSesionBean sessionData) {
		promotion.setCondicion1(readRules(erpPromotionDTO.getItemsConditionsRules()));

		promotion.setPuntosEuros(erpPromotionDTO.getAmountForPoint().setScale(2).toPlainString());

		promotion.setTextoPromocion(erpPromotionDTO.getPromotionText());
		promotion.setImagenPromocion(erpPromotionDTO.getPromotionImageURL());
	}

	@Override
	protected void czzPromotionToERPCustomData(PromotionTypePointIssuer erpPromotionDTO, final PromocionPuntosBean czzPromotion, final DatosSesionBean sessionData) {
		erpPromotionDTO.setItemsConditionsRules(rulesToBase64(czzPromotion.getCondicion1()));

		erpPromotionDTO.setAmountForPoint(getValueAsBigDecimal(czzPromotion.getPuntosEuros()));
				
		erpPromotionDTO.setPromotionText(czzPromotion.getTextoPromocion());
		erpPromotionDTO.setPromotionImageURL(czzPromotion.getImagenPromocion());
	}	
}
