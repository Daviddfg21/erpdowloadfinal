package com.comerzzia.custom.erp.services.promotions;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;

public class PromotionsSearchParams {
	public static final String SIN_ACTIVAR = "1";
	public static final String ACTIVO = "2";
	public static final String FINALIZADO = "3";
	public static final String CADUCADA = "4";
	
	protected DatosSesionBean sessionData;
	protected String status;
	protected Long promotionId;
	protected Long erpPromotionId;
	
	public PromotionsSearchParams () {
		
	}

	public PromotionsSearchParams(String status) {
		super();
		
		if (status == null) {
		   this.status = ACTIVO;	
		} else {
		   this.status = status;
		}
	}
	
	public PromotionsSearchParams(Long promotionId, Long erpPromotionId) {
		super();
		this.promotionId = promotionId;
		this.erpPromotionId = erpPromotionId;
	}

	public DatosSesionBean getSessionData() {
		return sessionData;
	}

	public void setSessionData(DatosSesionBean sessionData) {
		this.sessionData = sessionData;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getPromotionId() {
		return promotionId;
	}

	public void setPromotionId(Long promotionId) {
		this.promotionId = promotionId;
	}

	public Long getErpPromotionId() {
		return erpPromotionId;
	}

	public void setErpPromotionId(Long erpPromotionId) {
		this.erpPromotionId = erpPromotionId;
	}

	@Override
	public String toString() {
		return "PromotionsSearchParams [status=" + status + ", promotionId=" + promotionId + ", erpPromotionId="
				+ erpPromotionId + "]";
	}

}
