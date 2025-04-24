package com.comerzzia.custom.erp.services.promotions;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.comerzzia.core.model.usuarios.UsuarioBean;
import com.comerzzia.core.servicios.clases.parametros.valoresobjeto.ValoresParametrosObjetosService;
import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.core.util.db.Connection;
import com.comerzzia.core.util.db.Database;
import com.comerzzia.custom.erp.services.promotions.types.PromotionTypeComboHeaderManager;
import com.comerzzia.custom.erp.services.promotions.types.PromotionTypeComboLineManager;
import com.comerzzia.custom.erp.services.promotions.types.PromotionTypeComboNxMManager;
import com.comerzzia.custom.erp.services.promotions.types.PromotionTypeCouponApplyManager;
import com.comerzzia.custom.erp.services.promotions.types.PromotionTypeCouponIssuerManager;
import com.comerzzia.custom.erp.services.promotions.types.PromotionTypeDiscountManager;
import com.comerzzia.custom.erp.services.promotions.types.PromotionTypeNxMManager;
import com.comerzzia.custom.erp.services.promotions.types.PromotionTypePackManager;
import com.comerzzia.custom.erp.services.promotions.types.PromotionTypePointIssuerManager;
import com.comerzzia.custom.erp.services.promotions.types.PromotionTypePriceManager;
import com.comerzzia.custom.erp.services.promotions.types.PromotionTypeRappelManager;
import com.comerzzia.custom.erp.services.promotions.types.PromotionTypeTextManager;
import com.comerzzia.integrations.model.catalog.promotions.PromotionsDTO;
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypeDiscount;
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypeNxM;
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypePrice;
import com.comerzzia.integrations.model.catalog.promotions.basic.PromotionTypeRappel;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypeComboHeader;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypeComboLine;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypeComboNxM;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypeCouponApply;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypeCouponIssuer;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypePack;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypePointIssuer;
import com.comerzzia.integrations.model.catalog.promotions.complex.PromotionTypeTextIssuer;
import com.comerzzia.servicios.ventas.promociones.PromocionesService;

@SuppressWarnings({"deprecation"})
@Service
public class PromotionsService {
	protected Logger log = Logger.getLogger(this.getClass());
	
	protected static final String ID_CLASE_PROMOCIONES = "ERP.ID_PROMOCION";
	protected static final String PARAMETRO_PROMOCIONES = "ID_PROMOCION_CZZ";
	
	@Autowired
	protected ValoresParametrosObjetosService valoresParametrosObjetosService;
	
	@Autowired
	private PromotionTypePriceManager promotionTypePriceManager;
	
	@Autowired
	private PromotionTypeNxMManager promotionTypeNxMManager;
	
	@Autowired
	private PromotionTypeRappelManager promotionTypeRappelManager;
	
	@Autowired
	private PromotionTypeDiscountManager promotionTypeDiscountManager;
	
	@Autowired
	private PromotionTypeComboHeaderManager promotionTypeComboHeaderManager;
	
	@Autowired
	private PromotionTypeComboLineManager promotionTypeComboLineManager;
	
	@Autowired
	private PromotionTypeComboNxMManager promotionTypeComboNxMManager;
	
	@Autowired
	private PromotionTypeCouponApplyManager promotionTypeCouponApplyManager;
	
	@Autowired
	private PromotionTypeCouponIssuerManager promotionTypeCouponIssuerManager;
	
	@Autowired
	private PromotionTypePackManager promotionTypePackManager;
	
	@Autowired
	private PromotionTypePointIssuerManager promotionTypePointIssuerManager;
	
	@Autowired
	private PromotionTypeTextManager promotionTypetextManager;
	
	@Autowired
	protected PromocionesService servicioPromociones;

	private DatosSesionBean sessionData;
	
	// inicializa los valores de sesion y los servicios a utilizar
	@PostConstruct
	private void init() throws Exception {
		sessionData = new DatosSesionBean();

		UsuarioBean usuario = new UsuarioBean();
		usuario.setIdUsuario(new Long(0));
		usuario.setUsuario("ADMINISTRADOR");
		usuario.setDesUsuario("ADMINISTRADOR");
		sessionData.setUsuario(usuario);
		
		sessionData.setUidActividad(System.getenv().get("uidActividad"));
		sessionData.setUidInstancia(System.getenv().get("uidInstancia"));		
		
		if (StringUtils.isEmpty(sessionData.getUidActividad()) || StringUtils.isEmpty(sessionData.getUidInstancia())) {
			throw new RuntimeException("No se han establecido las variables de entorno para la actividad e instancia");
		}
	}
	
	public void processPromotions(PromotionsDTO listaPromocionesERPDTO) throws Exception {		
		Connection conn = new Connection();		
		
		try {
			conn.abrirConexion(Database.getConnection());
			
			// procesamiento PROMOCIONES APLICACION DE CUPONES
			// Se procesan las primeras ya que pueden haber otras promociones que dependan de esta.
			promotionTypeCouponApplyManager.erpPromotionsToCZZ(conn, listaPromocionesERPDTO.getDocumentId(), listaPromocionesERPDTO.getPromotionsTypeCouponApply(), sessionData);

			//procesamiento PROMOCIONES PRECIO
			promotionTypePriceManager.erpPromotionsToCZZ(conn, listaPromocionesERPDTO.getDocumentId(), listaPromocionesERPDTO.getPromotionsTypePrice(), sessionData);
						
			//procesamiento PROMOCIONES de Descuento
			promotionTypeDiscountManager.erpPromotionsToCZZ(conn, listaPromocionesERPDTO.getDocumentId(), listaPromocionesERPDTO.getPromotionsTypeDiscount(), sessionData);
						
			//procesamiento PROMOCIONES NxM
			promotionTypeNxMManager.erpPromotionsToCZZ(conn, listaPromocionesERPDTO.getDocumentId(), listaPromocionesERPDTO.getPromotionsTypeNxM(), sessionData);
			
			// Promociones de escalado
			promotionTypeRappelManager.erpPromotionsToCZZ(conn, listaPromocionesERPDTO.getDocumentId(), listaPromocionesERPDTO.getPromotionsTypeRappel(), sessionData);
									
			// procesamiento PROMOCIONES COMBINADA CABECERA
			promotionTypeComboHeaderManager.erpPromotionsToCZZ(conn, listaPromocionesERPDTO.getDocumentId(), listaPromocionesERPDTO.getPromotionsTypeComboHeader(), sessionData);
						
			// procesamiento PROMOCIONES COMBINADA LINEA
			promotionTypeComboLineManager.erpPromotionsToCZZ(conn, listaPromocionesERPDTO.getDocumentId(), listaPromocionesERPDTO.getPromotionsTypeComboLine(), sessionData);
						
			// procesamiento PROMOCIONES COMBINADA NXM
			promotionTypeComboNxMManager.erpPromotionsToCZZ(conn, listaPromocionesERPDTO.getDocumentId(), listaPromocionesERPDTO.getPromotionsTypeComboNxM(), sessionData);						
			
			// procesamiento PROMOCIONES GENERACION DE CUPONES
			promotionTypeCouponIssuerManager.erpPromotionsToCZZ(conn, listaPromocionesERPDTO.getDocumentId(), listaPromocionesERPDTO.getPromotionsTypeCouponIssuer(), sessionData);						
						
			// procesamiento PROMOCIONES DE PACK
			promotionTypePackManager.erpPromotionsToCZZ(conn, listaPromocionesERPDTO.getDocumentId(), listaPromocionesERPDTO.getPromotionsTypePack(), sessionData);			
			
			// procesamiento PROMOCIONES DE PACK
			promotionTypePointIssuerManager.erpPromotionsToCZZ(conn, listaPromocionesERPDTO.getDocumentId(), listaPromocionesERPDTO.getPromotionsTypePointIssuer(), sessionData);
			
  		    // procesamiento PROMOCIONES DE TEXTO
			promotionTypetextManager.erpPromotionsToCZZ(conn, listaPromocionesERPDTO.getDocumentId(), listaPromocionesERPDTO.getPromotionsTypeTextIssuer(), sessionData);
		} finally {
			conn.cerrarConexion();
		}
	}	
	


	public PromotionsDTO getPromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
								
		PromotionsDTO listaPromocionesERPDTO = new PromotionsDTO();
		
		//procesamiento PROMOCIONES PRECIO
		listaPromocionesERPDTO.setPromotionsTypePrice(getPricePromotions(params));
					
		//procesamiento PROMOCIONES de Descuento
		listaPromocionesERPDTO.setPromotionsTypeDiscount(getDiscountPromotions(params));
					
		//procesamiento PROMOCIONES NxM
		listaPromocionesERPDTO.setPromotionsTypeNxM(getNxMPromotions(params));			
		
		// Promociones de escalado
		listaPromocionesERPDTO.setPromotionsTypeRappel(getRappelPromotions(params));			
								
		// procesamiento PROMOCIONES COMBINADA CABECERA
		listaPromocionesERPDTO.setPromotionsTypeComboHeader(getComboHeaderPromotions(params));			
								
		// procesamiento PROMOCIONES COMBINADA LINEA
		listaPromocionesERPDTO.setPromotionsTypeComboLine(getComboLinePromotions(params));			
					
		// procesamiento PROMOCIONES COMBINADA NXM
		listaPromocionesERPDTO.setPromotionsTypeComboNxM(getComboNxMPromotions(params));						
		
		// procesamiento PROMOCIONES GENERACION DE CUPONES
		listaPromocionesERPDTO.setPromotionsTypeCouponIssuer(getCouponIssuerPromotions(params));
		
		// procesamiento PROMOCIONES APLICACION DE CUPONES
		listaPromocionesERPDTO.setPromotionsTypeCouponApply(getCouponApplyPromotions(params));
		
		// procesamiento PROMOCIONES DE PACK
		listaPromocionesERPDTO.setPromotionsTypePack(getPackPromotions(params));
		
		// procesamiento PROMOCIONES DE PACK
		listaPromocionesERPDTO.setPromotionsTypePointIssuer(getPointIssuerPromotions(params));			
		
	    // procesamiento PROMOCIONES DE TEXTO
		listaPromocionesERPDTO.setPromotionsTypeTextIssuer(getTextIssuerPromotions(params));
				
		return listaPromocionesERPDTO;
	}	
	
	public List<PromotionTypePrice> getPricePromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypePriceManager.czzPromotionsToERP(params);
	}
	
	public PromotionTypePrice getPricePromotion(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypePriceManager.czzPromotionToERP(params);
	}
	
	public List<PromotionTypeDiscount> getDiscountPromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeDiscountManager.czzPromotionsToERP(params);
	}
	
	public PromotionTypeDiscount getDiscountPromotion(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeDiscountManager.czzPromotionToERP(params);
	}
	
	public List<PromotionTypeNxM> getNxMPromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeNxMManager.czzPromotionsToERP(params);
	}
	
	public PromotionTypeNxM getNxMPromotion(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeNxMManager.czzPromotionToERP(params);
	}
	
	public List<PromotionTypeRappel> getRappelPromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeRappelManager.czzPromotionsToERP(params);
	}
	
	public PromotionTypeRappel getRappelPromotion(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeRappelManager.czzPromotionToERP(params);
	}
	
	public List<PromotionTypeComboHeader> getComboHeaderPromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeComboHeaderManager.czzPromotionsToERP(params);
	}
	
	public PromotionTypeComboHeader getComboHeaderPromotion(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeComboHeaderManager.czzPromotionToERP(params);
	}
	
	public List<PromotionTypeComboLine> getComboLinePromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeComboLineManager.czzPromotionsToERP(params);
	}
	
	public PromotionTypeComboLine getComboLinePromotion(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeComboLineManager.czzPromotionToERP(params);
	}
	
	public List<PromotionTypeComboNxM> getComboNxMPromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeComboNxMManager.czzPromotionsToERP(params);
	}
	
	public PromotionTypeComboNxM getComboNxMPromotion(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeComboNxMManager.czzPromotionToERP(params);
	}
	
	public List<PromotionTypeCouponIssuer> getCouponIssuerPromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeCouponIssuerManager.czzPromotionsToERP(params);
	}
	
	public PromotionTypeCouponIssuer getCouponIssuerPromotion(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeCouponIssuerManager.czzPromotionToERP(params);
	}
	
	public List<PromotionTypeCouponApply> getCouponApplyPromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeCouponApplyManager.czzPromotionsToERP(params);
	}
	
	public PromotionTypeCouponApply getCouponApplyPromotion(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypeCouponApplyManager.czzPromotionToERP(params);
	}
	
	public List<PromotionTypePack> getPackPromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypePackManager.czzPromotionsToERP(params);
	}
	
	public PromotionTypePack getPackPromotion(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypePackManager.czzPromotionToERP(params);
	}
	
	public List<PromotionTypePointIssuer> getPointIssuerPromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypePointIssuerManager.czzPromotionsToERP(params);
	}
	
	public PromotionTypePointIssuer getPointIssuerPromotion(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypePointIssuerManager.czzPromotionToERP(params);
	}
	
	public List<PromotionTypeTextIssuer> getTextIssuerPromotions(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypetextManager.czzPromotionsToERP(params);
	}
	
	public PromotionTypeTextIssuer getTextIssuerPromotion(PromotionsSearchParams params) {
		params.setSessionData(sessionData);
		return promotionTypetextManager.czzPromotionToERP(params);
	}
}
