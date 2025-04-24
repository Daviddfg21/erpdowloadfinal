package com.comerzzia.custom.erp.services.promotions.types;

import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypePack;
import com.comerzzia.servicios.ventas.promociones.tipos.especificos.PromocionPackBean;

@Service
public class PromotionTypePackManager extends PromotionTypeAbstractManager<PromotionTypePack, PromocionPackBean> {

	@Override
	protected void erpPromotionToCZZCustomData(final PromotionTypePack erpPromotionDTO, PromocionPackBean promotion, final DatosSesionBean sessionData) {
		promotion.setCondicion1(readRules(erpPromotionDTO.getItemsConditionsRules()));

		promotion.setAplicacion(readRules(erpPromotionDTO.getItemsApplyRules()));

		promotion.setPrecioPack(erpPromotionDTO.getPackPrice().doubleValue());
		if (erpPromotionDTO.getApplyQuantityLimit() != null) {
		   promotion.setCantidadMaximaAplicacion(erpPromotionDTO.getApplyQuantityLimit().intValue());
		}

		promotion.setTextoPromocion(erpPromotionDTO.getPromotionText());
		promotion.setImagenPromocion(erpPromotionDTO.getPromotionImageURL());
	}
	

	@Override
	protected void czzPromotionToERPCustomData(PromotionTypePack erpPromotionDTO, final PromocionPackBean czzPromotion, final DatosSesionBean sessionData) {
		erpPromotionDTO.setItemsConditionsRules(rulesToBase64(czzPromotion.getCondicion1()));		

		erpPromotionDTO.setItemsApplyRules(rulesToBase64(czzPromotion.getAplicacion()));

		erpPromotionDTO.setPackPrice(getValueAsBigDecimal(czzPromotion.getPrecioPack(), 2));
		erpPromotionDTO.setApplyQuantityLimit(czzPromotion.getCantidadMaximaAplicacion());
		
		erpPromotionDTO.setPromotionText(czzPromotion.getTextoPromocion());
		erpPromotionDTO.setPromotionImageURL(czzPromotion.getImagenPromocion());		
	}

}
