package com.comerzzia.custom.erp.services.promotions.types;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypeCouponIssuer;
import com.comerzzia.servicios.ventas.promociones.tipos.especificos.PromocionGeneracionCuponesBean;

@Service
public class PromotionTypeCouponIssuerManager
		extends PromotionTypeAbstractManager<PromotionTypeCouponIssuer, PromocionGeneracionCuponesBean> {

	@Override
	protected void erpPromotionToCZZCustomData(final PromotionTypeCouponIssuer erpPromotionDTO, PromocionGeneracionCuponesBean promotion, final DatosSesionBean sessionData) { 
		if (erpPromotionDTO.getFutureDiscountCoupon() == null) {
			throw new RuntimeException("La promoción de generación de cupones necesita los datos de cupón");
		}

		// force coupon data assign independent of discount type
		promotion.setDatosCuponPromo(headerCouponConverter(erpPromotionDTO.getFutureDiscountCoupon(), sessionData));

		promotion.setCondicion1(readRules(erpPromotionDTO.getItemsConditionsRules()));

		promotion.setImpresionManual(erpPromotionDTO.getPermitManualPrint() ? "S" : "N");
		promotion.setImpresionAleatoria(erpPromotionDTO.getAleatoryPrintIndexRange().toString());
	}

	@Override
	protected void czzPromotionToERPCustomData(PromotionTypeCouponIssuer erpPromotionDTO, final PromocionGeneracionCuponesBean czzPromotion, final DatosSesionBean sessionData) {
		erpPromotionDTO.setFutureDiscountCoupon(headerCouponConverter(czzPromotion.getDatosCuponPromo(), sessionData));
		
		erpPromotionDTO.setItemsConditionsRules(rulesToBase64(czzPromotion.getCondicion1()));
				
		erpPromotionDTO.setPermitManualPrint(StringUtils.equals(czzPromotion.getImpresionManual(), "S"));
		
		if (StringUtils.isNotBlank(czzPromotion.getImpresionAleatoria())) {
		   erpPromotionDTO.setAleatoryPrintIndexRange(new Integer(czzPromotion.getImpresionAleatoria()));
		}
	}	
}
