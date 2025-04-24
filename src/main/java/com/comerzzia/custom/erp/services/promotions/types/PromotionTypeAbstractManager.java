package com.comerzzia.custom.erp.services.promotions.types;



import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.comerzzia.core.model.clases.parametros.valoresobjetos.ValorParametroObjeto;
import com.comerzzia.core.servicios.clases.parametros.valores.ValorParametroClaseNotFoundException;
import com.comerzzia.core.servicios.clases.parametros.valoresobjeto.ValoresParametrosObjetosService;
import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.core.util.base.Estado;
import com.comerzzia.core.util.db.Connection;
import com.comerzzia.core.util.db.Database;
import com.comerzzia.core.util.fechas.FechaException;
import com.comerzzia.core.util.paginacion.PaginaResultados;
import com.comerzzia.custom.erp.services.promotions.CustomPromotionsBuilder;
import com.comerzzia.custom.erp.services.promotions.PromotionsSearchParams;
import com.comerzzia.custom.integration.persistence.EntityIntegrationLogBean;
import com.comerzzia.custom.integration.services.EntityIntegrationLogImpl;
import com.comerzzia.integrations.model.catalog.promotions.PromotionCouponTemplate;
import com.comerzzia.integrations.model.catalog.promotions.PromotionItem;
import com.comerzzia.integrations.model.catalog.promotions.PromotionTypeAbstract;
import com.comerzzia.integrations.model.catalog.promotions.PromotionsDTO;
import com.comerzzia.model.ventas.promociones.PromocionBean;
import com.comerzzia.model.ventas.promociones.almacenes.AlmacenPromocionBean;
import com.comerzzia.model.ventas.promociones.detalles.DetalleGeneralPromocionBean;
import com.comerzzia.persistencia.ventas.promociones.ParametrosBuscarPromocionesBean;
import com.comerzzia.servicios.ventas.promociones.Promocion;
import com.comerzzia.servicios.ventas.promociones.PromocionException;
import com.comerzzia.servicios.ventas.promociones.PromocionNotFoundException;
import com.comerzzia.servicios.ventas.promociones.PromocionesService;
import com.comerzzia.servicios.ventas.promociones.tipos.DatosCuponPromocionBean;

import net.sf.json.JSON;
import net.sf.json.xml.XMLSerializer;

public abstract class PromotionTypeAbstractManager<T extends PromotionTypeAbstract, V extends Promocion> {
	protected Logger log = Logger.getLogger(this.getClass());
	
	protected static final String ID_CLASE_PROMOCIONES = "ERP.ID_PROMOCION";
	protected static final String PARAMETRO_PROMOCIONES = "ID_PROMOCION_CZZ";
	
	@Autowired
	protected PromocionesService servicioPromociones;
	
	@Autowired
	protected ValoresParametrosObjetosService valoresParametrosObjetosService;
	
	@Autowired
	protected EntityIntegrationLogImpl entityIntegration;
	
	@SuppressWarnings("deprecation")
	public void erpPromotionsToCZZ(Connection conn, final String documentId, List<T> erpPromotionsDTO, DatosSesionBean sessionData) throws Exception {
		if (erpPromotionsDTO == null || (erpPromotionsDTO != null && erpPromotionsDTO.size() == 0)) return;
		
		SqlSession sqlSession = Database.getSqlSession(conn); 
		
		Long comienzo = java.lang.System.currentTimeMillis();
		int promoOk=0;
		int promoNew=0;
		int promoUpdate=0;
		int promoKo=0;
		
		log.info("Processing " + erpPromotionsDTO.size() + " promotions");
		
		for(T promotionERPDTO: erpPromotionsDTO) {
			// control de errores por cada promocion
			try {
				conn.iniciaTransaccion();
				
				if (promotionERPDTO.getPromotionERPId() == null ||
					(promotionERPDTO.getPromotionERPId() != null &&
					StringUtils.isEmpty(promotionERPDTO.getPromotionERPId().toString()))) {
					throw new RuntimeException("Property promotionERPId is null or empty");
					
				}
				
				//Traducir consultando el idPromocion recibido con el que corresponde en CMZ.
				Long idPromocionCMZ = getCzzPromotionId(promotionERPDTO.getPromotionERPId(), sessionData);
									
				Promocion promotion = erpPromotionToCZZ(conn, promotionERPDTO, idPromocionCMZ, sessionData);
				
				servicioPromociones.salvar(conn, promotion, sessionData);
				
				if(idPromocionCMZ == null) {
					//Actualizamos tabla de enlaces (ya que no existía el enlace de la promoción) para añadir el enlace/traducción entre la promoción de ERP y la recién creada en CMZ
					ValorParametroObjeto promocionEnlace = new ValorParametroObjeto();
					promocionEnlace.setEstadoBean(Estado.NUEVO);
					promocionEnlace.setIdClase(ID_CLASE_PROMOCIONES);
					promocionEnlace.setIdObjeto(promotionERPDTO.getPromotionERPId().toString());
					promocionEnlace.setParametro(PARAMETRO_PROMOCIONES);
					promocionEnlace.setValor(promotion.getIdPromocion().toString());
					
					valoresParametrosObjetosService.salvar(sqlSession, promocionEnlace, sessionData);
				}
			
				if (promotion.getPromocionBean().isSinActivar()) { // getVersionTarifa() == null || promotion.getVersionTarifa().compareTo(0L) == 0) {
				   servicioPromociones.activar(conn, promotion, sessionData);
				} else {
				   if (promotion.getPromocionBean().isActiva()) {
				      servicioPromociones.versionar(promotion, sessionData, conn);
				   } else {
					   log.warn("Se han recibido modificaciones sobre la promocion " + promotion.getIdPromocion() + ", pero ya no está activa. No se realiza versionado.");
				   }
				}

				// Marcar entidad/objeto como valido
				entityIntegration.entidadOk(conn, sessionData.getUidActividad(), PromotionsDTO.ENTITY, promotionERPDTO.getPromotionERPId().toString(), "*", documentId);
				
				conn.commit();
				conn.finalizaTransaccion();	
				promoOk ++;
				
				if (promotion.getEstadoBean() == Estado.NUEVO) {
					promoNew++;
				} else {
					promoUpdate++;
				}
		    } catch(Exception e) {
		    	conn.deshacerTransaccion();
		    	
		    	promoKo ++;
		    	
		    	log.error(e.getMessage(), e);
		    	
				// excepcion general
		    	entityIntegration.registraExcepcion(sessionData.getUidActividad(), 
					   new EntityIntegrationLogBean(PromotionsDTO.ENTITY, promotionERPDTO.getPromotionERPId().toString(), documentId, "*"
		    		     , "Error interno al salvar promocion: " + e.getMessage(), e)
					   );
			} 
		}
		
		log.info(erpPromotionsDTO.size() + " promotions proccessed in "	+ (java.lang.System.currentTimeMillis() - comienzo + " ms."));
		
		if (promoUpdate > 0) {
			log.info("Inserted: " + promoNew + " Updated: " + promoUpdate);
		}
				
		if (promoKo > 0) {
			log.info("OK: " + promoOk + " KO: " + promoKo);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected Promocion erpPromotionToCZZ(Connection conn, final T erpPromotionDTO, final Long idPromocionCMZ, final DatosSesionBean sessionData) throws Exception {
		V promotion = null;
				
		
		if(idPromocionCMZ != null) {
			// modificacion de promocion
			Promocion currentPromotion = (Promocion)servicioPromociones.consultar(conn, idPromocionCMZ, sessionData);
						
			if (currentPromotion.getIdTipoPromocion().compareTo(erpPromotionDTO.getPromotionTypeId()) != 0) {
			   throw new RuntimeException("Se está intentando modificar una promoción con un tipo diferente al original");
			}
			
			promotion = (V)currentPromotion;
						
			promotion.setFechaInicio(DateUtils.truncate(erpPromotionDTO.getStartDate(), Calendar.DAY_OF_MONTH));
			promotion.setFechaFin(DateUtils.truncate(erpPromotionDTO.getEndDate(), Calendar.DAY_OF_MONTH));
			promotion.setDescripcion(erpPromotionDTO.getDescription());
			promotion.setTipoDto(erpPromotionDTO.getDiscountType());
			promotion.setExclusiva(erpPromotionDTO.getExclusive());
			promotion.setSoloFidelizacion(erpPromotionDTO.getOnlyLoyalty());
			
			if (StringUtils.equals(erpPromotionDTO.getApplyToRates(), "R")) {
				promotion.setAplicaATarifas("R");
			} else {
				promotion.setAplicaATarifas("V");	
			}
			
			if (StringUtils.equals(erpPromotionDTO.getOnlyAccessByCouponCode(), "#COUPON#")) {
				promotion.setCupon("#TPV#");
			} else {
				promotion.setCupon(erpPromotionDTO.getOnlyAccessByCouponCode());
			}
			
			PromocionBean promotionBean = promotion.getPromocionBean();
			
			promotion = (V)CustomPromotionsBuilder.create(promotionBean);
			promotion.setEstadoBean(Estado.MODIFICADO);
		} else {
			// nueva promocion
			PromocionBean promotionBean = new PromocionBean();
			promotionBean.setEstadoBean(Estado.NUEVO);
			promotionBean.setIdTipoPromocion(erpPromotionDTO.getPromotionTypeId());
			
			promotionBean.setFechaInicio(DateUtils.truncate(erpPromotionDTO.getStartDate(), Calendar.DAY_OF_MONTH));
			promotionBean.setFechaFin(DateUtils.truncate(erpPromotionDTO.getEndDate(), Calendar.DAY_OF_MONTH));
			promotionBean.setDescripcion(erpPromotionDTO.getDescription());
			promotionBean.setCodTar(erpPromotionDTO.getRateCode());
			promotionBean.setTipoDto(erpPromotionDTO.getDiscountType());
			promotionBean.setExclusiva(erpPromotionDTO.getExclusive());
			promotionBean.setSoloFidelizacion(erpPromotionDTO.getOnlyLoyalty());
			
			if (StringUtils.equals(erpPromotionDTO.getApplyToRates(), "R")) {
				promotionBean.setAplicaATarifas("R");
			} else {
				promotionBean.setAplicaATarifas("V");	
			}
			
			if (StringUtils.equals(erpPromotionDTO.getOnlyAccessByCouponCode(), "#COUPON#")) {
				promotionBean.setCodCupon("#TPV#");
			} else {
				promotionBean.setCodCupon(erpPromotionDTO.getOnlyAccessByCouponCode());
			}
						
			promotion = (V)CustomPromotionsBuilder.create(promotionBean);
			promotion.setEstadoBean(Estado.NUEVO);
			
			Map<String, Object> extensiones = new HashMap<>();
			extensiones.put("ID_PROMOCION_ERP", String.valueOf(erpPromotionDTO.getPromotionERPId()));
			promotion.setExtensiones(extensiones);			
												
			if(erpPromotionDTO.getStores() != null && !erpPromotionDTO.getStores().isEmpty()){
				//Lista de tiendas a las que se baja la promocion. Si la lista es vacía se añade a todas las tiendas
				List<AlmacenPromocionBean> almacenes = new ArrayList<>();
				for(String store: erpPromotionDTO.getStores()){
					AlmacenPromocionBean almacenPromocionBean = new AlmacenPromocionBean();
					almacenPromocionBean.setEstadoBean(Estado.NUEVO);
					almacenPromocionBean.setCodAlm(store);
					almacenPromocionBean.setDesAlm("Almacén - " + store);
					almacenes.add(almacenPromocionBean);
					promotion.setAlmacenesCargados(true);
				}
				promotion.setListaAlmacenes(almacenes);
			}
		}

		// Update promotion header common data
		promotion.setCondicionCabecera(readRules(erpPromotionDTO.getHeaderConditionsRules()));
		
		if (promotion.getTipoDto().compareTo(T.DISCOUNT_TYPE_FUTURE) == 0) {
			if (erpPromotionDTO.getFutureDiscountCoupon() == null) {
				throw new RuntimeException("La promoción es de descuento futuro pero no tiene los datos del cupón a emitir");
			}
			promotion.setDatosCuponPromo(headerCouponConverter(erpPromotionDTO.getFutureDiscountCoupon(), sessionData));
		} else {
			promotion.setDatosCuponPromo(null);
		}
		
		// Update custom promotion data
		erpPromotionToCZZCustomData(erpPromotionDTO, promotion, sessionData);
		
		return promotion;
	}	
	
	protected abstract void erpPromotionToCZZCustomData(final T erpPromotionDTO, V promotion, final DatosSesionBean sessionData);
	
	protected String readRules(final String rulesInBase64) {
		String rulesTmp = rulesInBase64;
		
		if (StringUtils.isBlank(rulesInBase64)){
			rulesTmp = PromotionTypeAbstract.EMPTY_JSON_BASE64;
		}
		
		String rules;
		try {
			rules = new String(Base64.getDecoder().decode(rulesTmp), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
		if (StringUtils.isBlank(rules)) return null;
		
		if (rules.substring(0, 1).equals("<")) {
			// Convert XML to JSON
			XMLSerializer serializerRead = new XMLSerializer();        
			JSON jsonAplicacion = serializerRead.read(rules);
			
			return jsonAplicacion.toString();
		} else {
			// return JSON
			return rules;
		}
	}
	
	protected DatosCuponPromocionBean headerCouponConverter(final PromotionCouponTemplate coupon, final DatosSesionBean sessionData) {
		if (coupon == null) return null;
		
		DatosCuponPromocionBean result = new DatosCuponPromocionBean();
		result.setCouponTypeCode(coupon.getCouponTypeCode());

		result.setCustomerMaxUses(coupon.getCustomerMaxUses());
		result.setDescripcionCupon(coupon.getCouponDescription());

		Long czzPromotion = null;
		
		if (coupon.getApplyPromotionERPId() != null) {
			czzPromotion = getCzzPromotionId(coupon.getApplyPromotionERPId(), sessionData);
			
			if (czzPromotion == null) {
				throw new RuntimeException("Apply ERP Promotion Id " + coupon.getApplyPromotionERPId().toString() + " not found");
			}			
		} else {
			try {
				czzPromotion =  servicioPromociones.consultar(coupon.getApplyPromotionId(), sessionData).getIdPromocion();
			} catch (PromocionException | PromocionNotFoundException e) {
				throw new RuntimeException("Apply comerzzia Promotion Id " + coupon.getApplyPromotionERPId().toString() + " not found");
			}	
		}
		
		result.setIdPromoAplicar(czzPromotion.toString());
		
		result.setTituloCupon(coupon.getCouponTitle());
		result.setUrlImage(coupon.getCouponImageUrl());
		
		return result;
	}
	
	protected PromotionCouponTemplate headerCouponConverter(final DatosCuponPromocionBean  coupon, final DatosSesionBean sessionData) {
		if (coupon == null) return null;
		
		PromotionCouponTemplate result = new PromotionCouponTemplate();
		result.setCouponTypeCode(coupon.getCouponTypeCode());

		result.setCustomerMaxUses(coupon.getCustomerMaxUses());
		result.setCouponDescription(coupon.getDescripcionCupon());
				
		if (coupon.getIdPromoAplicar() != null) {
			Long promotionCzzId = new Long(coupon.getIdPromoAplicar());
			Long promotionErpId = getERPPromotionId(promotionCzzId, sessionData);
			
		    result.setApplyPromotionId(promotionCzzId);
		    result.setApplyPromotionERPId(promotionErpId);
		}
		
		result.setCouponTitle(coupon.getTituloCupon());
		result.setCouponImageUrl(coupon.getUrlImage());
		
		return result;
	}
	
	protected String getTipoFiltro(Long discountTypeId) {
		if (discountTypeId.compareTo(T.APPLY_DISCOUNT_TYPE_PRICE) == 0) {
			return "Precio";
		} else if (discountTypeId.compareTo(T.APPLY_DISCOUNT_TYPE_AMOUNT) == 0) {
			return "Importe";
		} else if (discountTypeId.compareTo(T.APPLY_DISCOUNT_TYPE_PERCENTAGE) == 0) {
			return "Porcentaje";
		}else {
			return "Descuento";
		}
	}
	
	protected Long getTipoFiltro(String discountTypeId) {
		if (StringUtils.equals(discountTypeId, "Precio")) {
			return T.APPLY_DISCOUNT_TYPE_PRICE;
		} else if (StringUtils.equals(discountTypeId, "Importe")) {
			return T.APPLY_DISCOUNT_TYPE_AMOUNT;
		} else {
			return T.APPLY_DISCOUNT_TYPE_PERCENTAGE;
		}
	}
	
	protected void updatePromoItemDateRange(final T erpPromotionDTO, Promocion promotion, PromotionItem erpPromotionItem, DetalleGeneralPromocionBean item) {
		// item date range
		if (item.getFechaInicio() == null) {
			item.setFechaInicio(DateUtils.truncate(erpPromotionItem.getStartDate(), Calendar.DAY_OF_MONTH));
		}
		
		if (item.getFechaFin() == null) {
			item.setFechaFin(DateUtils.truncate(erpPromotionItem.getEndDate(), Calendar.DAY_OF_MONTH));
		}
		
		// header date range
		if (item.getFechaInicio() == null) {
			item.setFechaInicio(DateUtils.truncate(erpPromotionDTO.getStartDate(), Calendar.DAY_OF_MONTH));
		}
		
		if (item.getFechaFin() == null) {
			item.setFechaFin(DateUtils.truncate(erpPromotionDTO.getEndDate(), Calendar.DAY_OF_MONTH));
		}
	}
	
	protected ParametrosBuscarPromocionesBean buildSearchParams(PromotionsSearchParams params) {
		ParametrosBuscarPromocionesBean result = new ParametrosBuscarPromocionesBean();
		
		result.setTipoPromocion(createPromotionInstance().getPromotionTypeId().toString());
		if (params.getStatus() != null) {
		   result.setEstado(params.getStatus());
		}
		if (params.getPromotionId() != null) {
		   result.setIdPromocion(params.getPromotionId());
		}

		if (params.getErpPromotionId() != null) {
		   Long idPromocionCMZ = getCzzPromotionId(params.getErpPromotionId(), params.getSessionData());
		   
		   if (idPromocionCMZ != null) {
			   throw new NotFoundException();
		   } 
		   
		   result.setIdPromocion(-1L);
		}

		result.setOrden("ID_PROMOCION");
		result.setTamañoPagina(1000);
		result.setNumPagina(1);
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public List<T> czzPromotionsToERP(PromotionsSearchParams params) {		
		Long comienzo = java.lang.System.currentTimeMillis();
				
		PaginaResultados czzPromotions;
		try {
			czzPromotions = servicioPromociones.consultar(buildSearchParams(params), params.getSessionData());
		} catch (PromocionException | FechaException e) {
			throw new RuntimeException(e);
		}
		
		log.info("Reading " + czzPromotions.getTotalResultados() + " promotions");
		
		List<T> erpPromotionsDTO = new ArrayList<>();		
		
		for(PromocionBean czzPromotion: (List<PromocionBean>)czzPromotions.getPagina()) {
			erpPromotionsDTO.add(czzPromotionToERP(czzPromotion.getIdPromocion(), params.getSessionData()));				
		}		
		
		log.info(erpPromotionsDTO.size() + " promotions readed in "	+ (java.lang.System.currentTimeMillis() - comienzo + " ms."));
		
		return erpPromotionsDTO;
	}
	
	@SuppressWarnings("unchecked")
	public T czzPromotionToERP(PromotionsSearchParams params) {		
		PaginaResultados czzPromotions;
		try {
			czzPromotions = servicioPromociones.consultar(buildSearchParams(params), params.getSessionData());
		} catch (PromocionException | FechaException e) {
			throw new RuntimeException(e);
		}
		
		if (czzPromotions.getTotalResultados() == 0) {
			throw new NotFoundException();
		}
		
		if (czzPromotions.getTotalResultados() > 1) {
			throw new RuntimeException("Invalid search params. More than one result found: " + params.toString());
		}
				
        return czzPromotionToERP(((List<PromocionBean>)czzPromotions.getPagina()).get(0).getIdPromocion(), params.getSessionData());				
	}
	
	@SuppressWarnings("unchecked")
	protected T czzPromotionToERP(final Long idPromocionCMZ, final DatosSesionBean sessionData) {
		Promocion currentPromotion;
		try {
			currentPromotion = (Promocion)servicioPromociones.consultar(idPromocionCMZ, sessionData);
		} catch (PromocionException | PromocionNotFoundException e1) {
			throw new RuntimeException(e1);
		}								
		
		V promotion = (V)currentPromotion;

		T erpPromotionDTO = createPromotionInstance();
		
		erpPromotionDTO.setPromotionId(promotion.getIdPromocion());
		erpPromotionDTO.setStartDate(promotion.getFechaInicio());
		erpPromotionDTO.setEndDate(promotion.getFechaFin());
		erpPromotionDTO.setDescription(promotion.getDescripcion());
		erpPromotionDTO.setDiscountType(promotion.getTipoDto());
		erpPromotionDTO.setExclusive(StringUtils.equals(promotion.getExclusiva(), "S"));
		erpPromotionDTO.setOnlyLoyalty(StringUtils.equals(promotion.getSoloFidelizacion(), "S"));
		erpPromotionDTO.setRateCode(promotion.getCodTar());
		
		if (StringUtils.equals(promotion.getAplicaATarifas(), "R")) {
			erpPromotionDTO.setApplyToRates("R");
		} else {
			erpPromotionDTO.setApplyToRates("S");
		}		
		
		if (StringUtils.equals(promotion.getCupon(), "#TPV#")) {
			erpPromotionDTO.setOnlyAccessByCouponCode("#COUPON#");
		} else {
			erpPromotionDTO.setOnlyAccessByCouponCode(promotion.getCupon());
		}
																
		if(currentPromotion.getListaAlmacenes() != null && !currentPromotion.getListaAlmacenes().isEmpty()){
			List<String> stores = new ArrayList<>();
							
			//Lista de tiendas a las que se baja la promocion. Si la lista es vacía se añade a todas las tiendas
			for(AlmacenPromocionBean store: currentPromotion.getListaAlmacenes()){
				stores.add(store.getCodAlm());
			}
			
			erpPromotionDTO.setStores(stores);
		}

		// Update promotion header common data
        erpPromotionDTO.setHeaderConditionsRules(rulesToBase64(promotion.getCondicionCabecera()));
		
		if (erpPromotionDTO.getDiscountType().compareTo(T.DISCOUNT_TYPE_FUTURE) == 0) {
			if (promotion.getDatosCuponPromo() == null) {
  			    throw new RuntimeException("La promoción " + promotion.getIdPromocion() + " es de descuento futuro pero no tiene los datos del cupón a emitir");
			}
			
			erpPromotionDTO.setFutureDiscountCoupon(headerCouponConverter(promotion.getDatosCuponPromo(), sessionData));
		}
		
		// try obtaing erp promotion Id from Extension
		//erpPromotionDTO.setPromotionERPId(0L);
		try {
		   erpPromotionDTO.setPromotionERPId(new Long((String)promotion.getExtension("ID_PROMOCION_ERP")));
		} catch (Exception ignore) {}
		
		// Load promotion details data
		try {
		   czzPromotionToERPCustomData(erpPromotionDTO, promotion, sessionData);
		} catch (Exception e) {
			if ((e instanceof RuntimeException) && !(e instanceof NullPointerException) ) {
			   throw e;				
			}
			
			throw new RuntimeException("La promoción " + promotion.getIdPromocion() + " de tipo " + promotion.getIdTipoPromocion() +  " contiene errores en sus datos de promocion: " + e.getMessage());	
		}
		
		return erpPromotionDTO;
	}
		
	protected abstract void czzPromotionToERPCustomData(final T erpPromotionDTO, final V czzPromotion, final DatosSesionBean sessionData);
	
	protected String rulesToBase64(String rules) {
		if (StringUtils.isBlank(rules)) {
			return PromotionTypeAbstract.EMPTY_JSON_BASE64;
		} else {
			return Base64.getEncoder().encodeToString(rules.getBytes());
		}
	}
	
	@SuppressWarnings("unchecked")
	protected T createPromotionInstance() {
        Type type = getClass().getGenericSuperclass();
        ParameterizedType paramType = (ParameterizedType) type;
        Class<T> clazz = (Class<T>)paramType.getActualTypeArguments()[0];
        try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}		  
	}
	
	protected Long getCzzPromotionId(Long promotionERPId, final DatosSesionBean sessionData) {
		Long idPromocionCMZ = null;
		try{
			ValorParametroObjeto valorPromocionSap = valoresParametrosObjetosService.consultar(ID_CLASE_PROMOCIONES, promotionERPId.toString(), PARAMETRO_PROMOCIONES, sessionData);
			idPromocionCMZ = Long.parseLong(valorPromocionSap.getValor());
		}catch(ValorParametroClaseNotFoundException ignore){}
		
		return idPromocionCMZ;
	}
	
	protected Long getERPPromotionId(Long promotionId, final DatosSesionBean sessionData) {
		
		Long idPromocion = null;
		try{
			Promocion promocion = servicioPromociones.consultar(promotionId, sessionData);
			idPromocion = Long.parseLong((String)promocion.getExtension("ID_PROMOCION_ERP"));
		}catch(Exception ignore){}
		
		return idPromocion;
	}
	
	protected BigDecimal getValueAsBigDecimal(String value) {
		if (StringUtils.isBlank(value)) return BigDecimal.ZERO;
		
		return new BigDecimal(value.replace(",", "."));
	}
	
	protected BigDecimal getValueAsBigDecimal(Double value, int newScale) {
		if (value == null) return BigDecimal.ZERO;
		
		return new BigDecimal(value).setScale(newScale, RoundingMode.HALF_UP);
	}
}
