package com.comerzzia.custom.erp.services.promotions;

import org.apache.log4j.Logger;

import com.comerzzia.model.ventas.promociones.PromocionBean;
import com.comerzzia.servicios.ventas.promociones.Promocion;
import com.comerzzia.servicios.ventas.promociones.PromocionBuilder;
import com.comerzzia.servicios.ventas.promociones.PromocionException;

public class CustomPromotionsBuilder extends PromocionBuilder {

	protected static final Logger log = Logger.getLogger(CustomPromotionsBuilder.class.getName());

	public static Promocion create(PromocionBean promocionBean) throws PromocionException {
		try {
			Promocion promo = PromocionBuilder.create(promocionBean);
			if (promo != null) {
				return promo;
			}
			if (promocionBean.getIdTipoPromocion() == 1001L) {
				log.debug("create() - Custom promotion xxxx");
				//MyCustomPromotion promotion = new MyCustomPromotion(promocionBean);
				//return (Promocion) promocion;
			}

			return null; // Promotion type not implemented
		}
		catch (Exception e) {
			log.error("create() - Error reading promotion: " + promocionBean + ". Error processing XML: " + e.getMessage());
			throw new PromocionException(e.getMessage(), e);
		}
	}
}
