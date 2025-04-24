package com.comerzzia.custom.erp.services.promotions.types;

import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypeComboLine;
import com.comerzzia.servicios.ventas.promociones.tipos.especificos.PromocionDescuentoCombinadoLineaBean;

@Service
public class PromotionTypeComboLineManager
		extends PromotionTypeAbstractManager<PromotionTypeComboLine, PromocionDescuentoCombinadoLineaBean> {

	@Override
	protected void erpPromotionToCZZCustomData(final PromotionTypeComboLine erpPromotionDTO,
			PromocionDescuentoCombinadoLineaBean promotion, final DatosSesionBean sessionData) {
		promotion.setCondicion1(readRules(erpPromotionDTO.getItemsConditionsRules()));

		promotion.setAplicacion(readRules(erpPromotionDTO.getItemsApplyRules()));

		promotion.setTextoPromocion(erpPromotionDTO.getPromotionText());
		promotion.setImagenPromocion(erpPromotionDTO.getPromotionImageURL());
		promotion.setTipoFiltro(getTipoFiltro(erpPromotionDTO.getApplyDiscountTypeId()));
		promotion.setPrecioDescuento(erpPromotionDTO.getApplyDiscount().doubleValue());
		promotion.setCantidadTipo(erpPromotionDTO.getApplyQuantityLimit());
	}

	@Override
	protected void czzPromotionToERPCustomData(PromotionTypeComboLine erpPromotionDTO, final PromocionDescuentoCombinadoLineaBean czzPromotion, final DatosSesionBean sessionData) {
		erpPromotionDTO.setItemsConditionsRules(rulesToBase64(czzPromotion.getCondicion1()));

		erpPromotionDTO.setItemsApplyRules(rulesToBase64(czzPromotion.getAplicacion()));

		erpPromotionDTO.setPromotionText(czzPromotion.getTextoPromocion());
		erpPromotionDTO.setPromotionImageURL(czzPromotion.getImagenPromocion());
		erpPromotionDTO.setApplyDiscountTypeId(getTipoFiltro(czzPromotion.getTipoFiltro()));
		erpPromotionDTO.setApplyDiscount(getValueAsBigDecimal(czzPromotion.getPrecioDescuento(), 2));
		erpPromotionDTO.setApplyQuantityLimit(czzPromotion.getCantidadTipo());
	}
}
