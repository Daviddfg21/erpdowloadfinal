package com.comerzzia.custom.erp.services.rates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.comerzzia.core.model.impuestos.grupos.GrupoImpuestosBean;
import com.comerzzia.core.model.impuestos.porcentajes.PorcentajeImpuestoBean;
import com.comerzzia.core.model.usuarios.UsuarioBean;
import com.comerzzia.core.servicios.impuestos.grupos.GruposImpuestosService;
import com.comerzzia.core.servicios.impuestos.porcentajes.PorcentajesImpuestosService;
import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.core.util.base.Estado;
import com.comerzzia.core.util.db.Connection;
import com.comerzzia.core.util.db.Database;
import com.comerzzia.core.util.numeros.BigDecimalUtil;
import com.comerzzia.custom.integration.persistence.EntityIntegrationLogBean;
import com.comerzzia.custom.integration.services.EntityIntegrationLogImpl;
import com.comerzzia.integrations.model.catalog.rates.RateDTO;
import com.comerzzia.integrations.model.catalog.rates.RateItemDTO;
import com.comerzzia.integrations.model.catalog.rates.RatesDTO;
import com.comerzzia.model.general.articulos.ArticuloBean;
import com.comerzzia.model.ventas.tarifas.TarifaBean;
import com.comerzzia.model.ventas.tarifas.articulos.ArticuloTarifaBean;
import com.comerzzia.persistencia.ventas.tarifas.articulos.ArticulosTarifaDao;
import com.comerzzia.servicios.general.articulos.ArticulosService;
import com.comerzzia.servicios.ventas.tarifas.ServicioTarifasImpl;
import com.comerzzia.servicios.ventas.tarifas.TarifaNotFoundException;

@SuppressWarnings("deprecation")
@Service
public class RatesService {
	protected Logger log = Logger.getLogger(this.getClass());

	@Autowired
	private ArticulosService itemsService;
	
	private ServicioTarifasImpl servicioTarifas;
	
	@Autowired
	private PorcentajesImpuestosService porcentajesImpuestosService; 
	
	@Autowired
	private GruposImpuestosService gruposImpuestosService;
	
	@Autowired
	private EntityIntegrationLogImpl entityIntegrationLog;

	private DatosSesionBean sessionData;

	// inicializa los valores de sesion y los servicios a utilizar
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

		servicioTarifas = ServicioTarifasImpl.get();
	}

	public void processRates(RatesDTO ratesDTO)
			throws Exception {
		
		if (sessionData == null) {
			init();
		}

		Long comienzo = java.lang.System.currentTimeMillis();

		Connection conn = new Connection();
		int erroneas = 0;
		try {
			conn.abrirConexion(Database.getConnection());
			conn.iniciaTransaccion();			
			
			for (RateDTO rateDTO : ratesDTO.getRates()) {	
				TarifaBean tarifaBean = null;
				try{
					conn.iniciaTransaccion();
					
					//Salvamos la tarifa. Control de errores por cada tarifa
					tarifaBean = initTarifa(rateDTO, sessionData);
					servicioTarifas.salvar(conn, tarifaBean, sessionData);
					
					conn.commit();
					conn.finalizaTransaccion();		
					
					for(RateItemDTO rateItemDTO : rateDTO.getRateItems()){
						// control de errores por cada articulo de tarifa
						try {
							conn.iniciaTransaccion();
												
							ArticuloBean articulo = itemsService.consultar(rateItemDTO.getItemCode(), sessionData.getConfigEmpresa());
							
							
							//Inicializamos y salvamos el articuloTarifa
							salvarArticulosTarifa(conn, sessionData, rateItemDTO, articulo, tarifaBean);
						
						
							// Marcar entidad/objeto como valido
							entityIntegrationLog.entidadOk(conn, sessionData.getUidActividad(), RatesDTO.ENTITY, rateItemDTO.getItemCode(), "*", ratesDTO.getDocumentId());
							
							conn.commit();
							conn.finalizaTransaccion();					
						} catch (Exception e) {					
					    	conn.deshacerTransaccion();
					    	
					    	log.error(e.getMessage(), e);			    	
					    				    	
					    	// Registrar excepcion y continuar con el siguiente articulo
					    	entityIntegrationLog.registraExcepcion(sessionData.getUidActividad(), new EntityIntegrationLogBean(RatesDTO.ENTITY, rateItemDTO.getItemCode(), ratesDTO.getDocumentId(), "*"
						    		 , "Error interno al salvar la tarifa del articulo " + rateItemDTO.getItemCode() + " : " + e.getMessage(), e));
					    } 
					}
				}catch(Exception e){
					conn.deshacerTransaccion();
					erroneas++;
		    	
			    	log.error(e.getMessage(), e);			    	
			    				    	
			    	// Registrar excepcion y continuar con el siguiente articulo
			    	entityIntegrationLog.registraExcepcion(sessionData.getUidActividad(), new EntityIntegrationLogBean(RatesDTO.ENTITY, rateDTO.getRateCode(), ratesDTO.getDocumentId(), "*"
				    		 , "Error interno al salvar la tarifa " + rateDTO.getRateCode() + " : " + e.getMessage(), e));
				} 
			}
		} finally {
			conn.cerrarConexion();
			log.info("Tiempo de procesamiento: "
					+ (java.lang.System.currentTimeMillis() - comienzo + " ms. Tarifas: " + ratesDTO.getRates() + " . Correctas: " + (ratesDTO.getRates().size() - erroneas) + " Erroneas: " + erroneas));
		}
	}

	private TarifaBean initTarifa(RateDTO rateDTO, DatosSesionBean datosSesion) throws Exception {
		TarifaBean tarifa = new TarifaBean();
		GrupoImpuestosBean grupoImp = null;
		
		try{
			grupoImp = gruposImpuestosService.consultar(datosSesion.getConfigEmpresa(), new Date());
			tarifa.setCodTar(rateDTO.getRateCode());
			tarifa.setIdTratImpuestos(rateDTO.getTaxesTreatmentId());
			tarifa.setIdGrupoImpuesto(grupoImp.getIdGrupoImpuestos());
			tarifa.setDesTar(rateDTO.getRateDes());
			if(rateDTO.getPricesWithTaxes()) {
				tarifa.setPreciosConImpuestos("S");
			} else {
				tarifa.setPreciosConImpuestos("N");
			}
			
			try {
				// si la tarifa no existe se crea como nueva , en caso contrario se declara con estado modificado y se inicializa version original
				TarifaBean tarifaOriginal = ServicioTarifasImpl.get().consultar(tarifa.getCodTar(), sessionData);
				tarifa.setVersion(tarifaOriginal.getVersion());
				tarifa.setEstadoBean(Estado.MODIFICADO);
			} catch (TarifaNotFoundException e) {
				tarifa.setEstadoBean(Estado.NUEVO);
			}
			
			return tarifa;
		 } catch (Exception e){
			String detalle ="No se ha podido inicializar tarifa con codTar " + rateDTO.getRateCode() + " y desTar " + rateDTO.getRateDes() + " : " + e.getMessage(); 
			log.error("initTarifa() - " + detalle );
			
			throw new Exception(detalle);
		}
	}

	private void salvarArticulosTarifa(Connection conn, DatosSesionBean datosSesion, RateItemDTO rateItemDTO, ArticuloBean articulo, TarifaBean tarifaBean) throws Exception {

		PorcentajeImpuestoBean porcentajeBean = porcentajesImpuestosService.consultar(datosSesion.getConfigEmpresa(), tarifaBean.getIdGrupoImpuesto(), tarifaBean.getIdTratImpuestos(), articulo.getCodImpuesto());
		
		ArticuloTarifaBean articuloTarifa = null;
		articuloTarifa = ArticulosTarifaDao.consultar(conn, 
				                    datosSesion.getConfigEmpresa(), 
				                    tarifaBean.getCodTar(), 
				                    articulo.getCodArticulo(), 
				                    rateItemDTO.getCombination1Code(), 
				                    rateItemDTO.getCombination2Code(),
				                    rateItemDTO.getStartDate() != null ? DateUtils.truncate(rateItemDTO.getStartDate(), Calendar.DAY_OF_MONTH) : DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH));
		
		if (articuloTarifa != null) {
			articuloTarifa.setEstadoBean(Estado.MODIFICADO);
		} else {
			articuloTarifa = new ArticuloTarifaBean();
			articuloTarifa.setEstadoBean(Estado.NUEVO);
			articuloTarifa.setCodTar(tarifaBean.getCodTar());
			articuloTarifa.setCodArt(articulo.getCodArticulo());
			articuloTarifa.setPorcentajeImpuesto(porcentajeBean.getPorcentajeImpuestos().doubleValue()); 
			articuloTarifa.setDesglose1(rateItemDTO.getCombination1Code());
			articuloTarifa.setDesglose2(rateItemDTO.getCombination2Code());
		}
		articuloTarifa.setPrecioCosto(rateItemDTO.getUnitCostPrice());
		articuloTarifa.setPrecioVenta(rateItemDTO.getSalesPrice());
		articuloTarifa.setPrecioTotal(rateItemDTO.getSalesPriceWithTaxes());
		articuloTarifa.setPrecioVentaRef(rateItemDTO.getSalesPriceRef());
		articuloTarifa.setPrecioVentaRefTotal(rateItemDTO.getSalesPriceRefWithTaxes());
		articuloTarifa.setFactorMarcaje(rateItemDTO.getProfitFactor());
		articuloTarifa.setFechaInicio(rateItemDTO.getStartDate() != null ? DateUtils.truncate(rateItemDTO.getStartDate(), Calendar.DAY_OF_MONTH) : DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH));
		
		compruebaCalculaPrecios(conn, datosSesion, articulo, tarifaBean, articuloTarifa);
		
		// insert/update | Nota: solo actualiza (de un detalle de tarifa creado) la version de la tarifa del articulo en caso de que se modifique el precioVenta
		servicioTarifas.salvarArticuloTarifa(conn, articuloTarifa, datosSesion);		
	}
	
	private void compruebaCalculaPrecios(Connection conn, DatosSesionBean datosSesion, ArticuloBean articulo, TarifaBean tarifaBean, ArticuloTarifaBean articuloTarifa) throws Exception {
		try{	
			//verifica que estan todos los campos necesarios para calcular el precio si no genera evento y lanza excepcion
			compruebaCamposPrecio(articuloTarifa, tarifaBean);
			//obtiene el porcentaje para calcular el precio
			PorcentajeImpuestoBean porcentajeBean =  consultarPorcentaje(datosSesion, articulo, tarifaBean);
			calculoPrecio(porcentajeBean, articuloTarifa);						
		}catch (Exception e){
			throw new Exception("Se ha producido un error al comprobar los precios del articulos");
		}
	}
	
	private static void compruebaCamposPrecio(ArticuloTarifaBean articuloTarifa, TarifaBean tarifaBean) throws Exception {		
		if(articuloTarifa.getFactorMarcaje() == null && articuloTarifa.getPrecioTotal() == null && articuloTarifa.getPrecioVenta() == null){
			String detalleError = "Imposible calcular precios: Precio Total, Precio Venta y Factor Marcaje Nulos,"+ "\r\n" +
					              " CodArt:" + articuloTarifa.getCodArt()+ "\r\n" +
			           		      " CodTar:" + tarifaBean.getCodTar();
			throw new Exception(detalleError);
		}
	}
	
	private PorcentajeImpuestoBean consultarPorcentaje (DatosSesionBean datosSesion, ArticuloBean articulo, TarifaBean tarifaBean) throws Exception{
		PorcentajeImpuestoBean porcentaje =null;
		GrupoImpuestosBean grupoImp = null;
		try{			
			grupoImp     = gruposImpuestosService.consultar(datosSesion.getConfigEmpresa(), new Date());
			porcentaje   = porcentajesImpuestosService.consultar(datosSesion.getConfigEmpresa(), grupoImp.getIdGrupoImpuestos(), tarifaBean.getIdTratImpuestos() , articulo.getCodImpuesto());
		} catch (Exception e){
			String detalle = "Error al obtener el porcentaje de impuestos asociado al articulo " + articulo.getCodArticulo() + " de la tarifa " + tarifaBean.getCodTar();
			log.error("consultarPorcentaje() - " + detalle,e);
			throw new Exception(detalle);
		}
		return porcentaje;
	}
	
	private void calculoPrecio(PorcentajeImpuestoBean porcentajeBean , ArticuloTarifaBean articuloTarifaBean) throws Exception{
		BigDecimal CIEN = new BigDecimal(100);
		
		//si el costo viene a nulo, lo establecemos a 0
		if(articuloTarifaBean.getPrecioCosto() == null){
			articuloTarifaBean.setPrecioCosto(0.0);
		}
	
		if(articuloTarifaBean.getPrecioTotal()!=null){
			//calculo de precio venta
			articuloTarifaBean.setPrecioTotal(articuloTarifaBean.getPrecioTotal());
			
			BigDecimal precioVenta = new BigDecimal(articuloTarifaBean.getPrecioTotal());
			BigDecimal porcentaje  = BigDecimal.ONE.add(porcentajeBean.getPorcentaje().divide(CIEN,4,BigDecimal.ROUND_HALF_UP));
			
			if(!BigDecimalUtil.isIgualACero(porcentaje)){
				precioVenta = precioVenta.divide(porcentaje,4,BigDecimal.ROUND_HALF_UP);
			}
			
			articuloTarifaBean.setPrecioVenta(precioVenta.doubleValue()); 
		}
		else {
			BigDecimal precioVenta = null, precioTotal = null;
			if(articuloTarifaBean.getFactorMarcaje() != null){
				BigDecimal factorMarcaje = new BigDecimal(articuloTarifaBean.getFactorMarcaje());
				BigDecimal precioCosto =null;
				if(articuloTarifaBean.getPrecioCosto()!= null){
					precioCosto = new BigDecimal(articuloTarifaBean.getPrecioCosto()).setScale(4, RoundingMode.HALF_UP);
				}
				else{
					precioCosto = BigDecimal.ZERO;
					articuloTarifaBean.setPrecioCosto(0.0);
				}
			 
				if (articuloTarifaBean.getPrecioVenta() != null && !articuloTarifaBean.getPrecioVenta().equals(BigDecimal.ZERO)) {
					precioVenta = new BigDecimal(articuloTarifaBean.getPrecioVenta()).setScale(4, RoundingMode.HALF_UP);
				}
				else{
					if(factorMarcaje.equals(CIEN)){
						factorMarcaje = BigDecimal.ZERO;
					}
					
					BigDecimal divisor = CIEN.subtract(factorMarcaje);
					divisor = divisor.divide(CIEN,4,BigDecimal.ROUND_HALF_UP);
				
					precioVenta = precioCosto.divide(divisor,4,BigDecimal.ROUND_HALF_UP);
				}
				
				if(precioVenta == null){
					precioVenta = BigDecimal.ZERO;
				}
			}
			else{
				precioVenta = new BigDecimal(articuloTarifaBean.getPrecioVenta()).setScale(4, RoundingMode.HALF_UP);
			}
			
			BigDecimal incremento =  BigDecimal.ONE.add(porcentajeBean.getPorcentaje().divide(CIEN,4,BigDecimal.ROUND_HALF_UP));
			precioTotal = BigDecimalUtil.redondear(precioVenta.multiply(incremento));
			articuloTarifaBean.setPrecioVenta(precioVenta.doubleValue());
			articuloTarifaBean.setPrecioTotal(precioTotal.doubleValue());
		}	
	}
	
}
