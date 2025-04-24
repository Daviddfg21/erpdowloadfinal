package com.comerzzia.custom.erp.services.promotions.types;

import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypeComboNxM;
import com.comerzzia.servicios.ventas.promociones.tipos.especificos.PromocionDescuentoCombinadoNxMBean;

@Service
public class PromotionTypeComboNxMManager
		extends PromotionTypeAbstractManager<PromotionTypeComboNxM, PromocionDescuentoCombinadoNxMBean> {

	@Override
	protected void erpPromotionToCZZCustomData(final PromotionTypeComboNxM erpPromotionDTO, PromocionDescuentoCombinadoNxMBean promotion, final DatosSesionBean sessionData) {
		promotion.setCondicion1(readRules(erpPromotionDTO.getItemsConditionsRules()));

		promotion.setCondicionN(erpPromotionDTO.getComboUnits());
		promotion.setCondicionM(erpPromotionDTO.getComboUnitsToPay());
		
		if (erpPromotionDTO.getApplyDiscountTypeId() == PromotionTypeComboNxM.APPLY_DISCOUNT_TYPE_PERCENTAGE) {
			promotion.setDescuentoPorcentaje(erpPromotionDTO.getApplyDiscount().doubleValue());	
		} else {
			promotion.setPrecioUnidadRegalada(erpPromotionDTO.getApplyDiscount().doubleValue());
		}

		promotion.setTextoPromocion(erpPromotionDTO.getPromotionText());
		promotion.setImagenPromocion(erpPromotionDTO.getPromotionImageURL());
	}

	@Override
	protected void czzPromotionToERPCustomData(PromotionTypeComboNxM erpPromotionDTO, final PromocionDescuentoCombinadoNxMBean czzPromotion, final DatosSesionBean sessionData) {
		erpPromotionDTO.setItemsConditionsRules(rulesToBase64(czzPromotion.getCondicion1()));

		erpPromotionDTO.setComboUnits(czzPromotion.getCondicionN());
		erpPromotionDTO.setComboUnitsToPay(czzPromotion.getCondicionM());
		
		if (czzPromotion.getDescuentoPorcentaje() != null) {
			erpPromotionDTO.setApplyDiscountTypeId(PromotionTypeComboNxM.APPLY_DISCOUNT_TYPE_PERCENTAGE);
			erpPromotionDTO.setApplyDiscount(getValueAsBigDecimal(czzPromotion.getDescuentoPorcentaje(), 2));
		} else {
			erpPromotionDTO.setApplyDiscountTypeId(PromotionTypeComboNxM.APPLY_DISCOUNT_TYPE_PRICE);
			erpPromotionDTO.setApplyDiscount(getValueAsBigDecimal(czzPromotion.getPrecioUnidadRegalada(), 2));
		}
		
		erpPromotionDTO.setPromotionText(czzPromotion.getTextoPromocion());
		erpPromotionDTO.setPromotionImageURL(czzPromotion.getImagenPromocion());		
	}		
}
