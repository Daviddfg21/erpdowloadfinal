package com.comerzzia.custom.erp.services.promotions.types;

import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypeCouponApply;
import com.comerzzia.servicios.ventas.promociones.tipos.especificos.PromocionAplicacionCuponDescuentoBean;

@Service
public class PromotionTypeCouponApplyManager extends PromotionTypeAbstractManager<PromotionTypeCouponApply, PromocionAplicacionCuponDescuentoBean> {

	@Override
	protected void erpPromotionToCZZCustomData(final PromotionTypeCouponApply erpPromotionDTO, PromocionAplicacionCuponDescuentoBean promotion, final DatosSesionBean sessionData) {
		promotion.setCupon("#TPV#");
		promotion.setCondicion1(readRules(erpPromotionDTO.getItemsConditionsRules()));
						
		promotion.setAplicacion(readRules(erpPromotionDTO.getItemsApplyRules()));
				
		promotion.setTextoPromocion(erpPromotionDTO.getPromotionText());
		promotion.setImagenPromocion(erpPromotionDTO.getPromotionImageURL());	
	}

	@Override
	protected void czzPromotionToERPCustomData(PromotionTypeCouponApply erpPromotionDTO, final PromocionAplicacionCuponDescuentoBean czzPromotion, final DatosSesionBean sessionData) {
		erpPromotionDTO.setOnlyAccessByCouponCode("#COUPON#");
		erpPromotionDTO.setItemsConditionsRules(rulesToBase64(czzPromotion.getCondicion1()));

		erpPromotionDTO.setItemsApplyRules(rulesToBase64(czzPromotion.getAplicacion()));

		erpPromotionDTO.setPromotionText(czzPromotion.getTextoPromocion());
		erpPromotionDTO.setPromotionImageURL(czzPromotion.getImagenPromocion());
	}	
}
