package com.comerzzia.custom.erp.services.items;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.comerzzia.core.model.clases.parametros.valoresobjetos.ValorParametroObjeto;
import com.comerzzia.core.model.etiquetas.categorias.EtiquetaBean;
import com.comerzzia.core.model.etiquetas.enlaces.EtiquetaEnlaceBean;
import com.comerzzia.core.model.i18n.I18NBean;
import com.comerzzia.core.model.impuestos.grupos.GrupoImpuestosBean;
import com.comerzzia.core.model.impuestos.porcentajes.PorcentajeImpuestoBean;
import com.comerzzia.core.model.usuarios.UsuarioBean;
import com.comerzzia.core.servicios.clases.parametros.valoresobjeto.ValorParametroObjetoConstraintViolationException;
import com.comerzzia.core.servicios.clases.parametros.valoresobjeto.ValorParametroObjetoException;
import com.comerzzia.core.servicios.clases.parametros.valoresobjeto.ValoresParametrosObjetosService;
import com.comerzzia.core.servicios.etiquetas.EtiquetasConstraintViolationException;
import com.comerzzia.core.servicios.etiquetas.EtiquetasException;
import com.comerzzia.core.servicios.etiquetas.EtiquetasNotFoundException;
import com.comerzzia.core.servicios.etiquetas.EtiquetasService;
import com.comerzzia.core.servicios.etiquetas.enlaces.EtiquetasEnlacesConstraintViolationException;
import com.comerzzia.core.servicios.etiquetas.enlaces.EtiquetasEnlacesException;
import com.comerzzia.core.servicios.etiquetas.enlaces.EtiquetasEnlacesService;
import com.comerzzia.core.servicios.i18n.I18NService;
import com.comerzzia.core.servicios.impuestos.grupos.GruposImpuestosService;
import com.comerzzia.core.servicios.impuestos.porcentajes.PorcentajesImpuestosService;
import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.core.util.base.Estado;
import com.comerzzia.core.util.db.Connection;
import com.comerzzia.core.util.db.Database;
import com.comerzzia.core.util.numeros.BigDecimalUtil;
import com.comerzzia.custom.erp.services.items.persistence.TaxItemMapper;
import com.comerzzia.custom.integration.persistence.EntityIntegrationLogBean;
import com.comerzzia.custom.integration.services.EntityIntegrationLogImpl;
import com.comerzzia.integrations.model.catalog.items.ItemBarcodeDTO;
import com.comerzzia.integrations.model.catalog.items.ItemDTO;
import com.comerzzia.integrations.model.catalog.items.ItemRateDTO;
import com.comerzzia.integrations.model.catalog.items.ItemTagDTO;
import com.comerzzia.integrations.model.catalog.items.ItemUnitMeasureDTO;
import com.comerzzia.integrations.model.catalog.items.ItemsDTO;
import com.comerzzia.model.general.articulos.ArticuloBean;
import com.comerzzia.model.general.articulos.canal.CanalArticuloBean;
import com.comerzzia.model.general.articulos.codigosbarras.CodigoBarrasArticuloBean;
import com.comerzzia.model.general.articulos.unidadesmedidas.UnidadMedidaArticuloBean;
import com.comerzzia.model.general.canales.CanalVentaBean;
import com.comerzzia.model.general.unidadesmedida.UnidadMedidaBean;
import com.comerzzia.model.ventas.tarifas.TarifaBean;
import com.comerzzia.model.ventas.tarifas.articulos.ArticuloTarifaBean;
import com.comerzzia.persistencia.general.articulos.ArticulosDao;
import com.comerzzia.persistencia.ventas.tarifas.articulos.ArticulosTarifaDao;
import com.comerzzia.servicios.general.articulos.ArticuloNotFoundException;
import com.comerzzia.servicios.general.articulos.ArticulosService;
import com.comerzzia.servicios.general.articulos.canales.CanalArticuloException;
import com.comerzzia.servicios.general.articulos.canales.ServicioCanalesArticulosImpl;
import com.comerzzia.servicios.general.articulos.codigosbarras.CodigoBarrasArticuloConstraintViolationException;
import com.comerzzia.servicios.general.articulos.codigosbarras.CodigoBarrasArticuloException;
import com.comerzzia.servicios.general.articulos.codigosbarras.CodigoBarrasArticuloNotFoundException;
import com.comerzzia.servicios.general.articulos.codigosbarras.ServicioCodigosBarrasArticulosImpl;
import com.comerzzia.servicios.general.articulos.unidadesmedidas.ServicioUnidadesMedidasArticulosImpl;
import com.comerzzia.servicios.general.articulos.unidadesmedidas.UnidadMedidaArticuloConstraintViolationException;
import com.comerzzia.servicios.general.articulos.unidadesmedidas.UnidadMedidaArticuloException;
import com.comerzzia.servicios.general.articulos.unidadesmedidas.UnidadMedidaArticuloNotFoundException;
import com.comerzzia.servicios.general.canalesventa.CanalVentaException;
import com.comerzzia.servicios.general.canalesventa.CanalVentaNotFoundException;
import com.comerzzia.servicios.general.canalesventa.ServicioCanalesVentasImpl;
import com.comerzzia.servicios.general.unidadesmedida.ServicioUnidadesMedidaImpl;
import com.comerzzia.servicios.general.unidadesmedida.UnidadMedidaConstraintViolationException;
import com.comerzzia.servicios.general.unidadesmedida.UnidadMedidaException;
import com.comerzzia.servicios.general.unidadesmedida.UnidadMedidaNotFoundException;
import com.comerzzia.servicios.ventas.tarifas.ServicioTarifasImpl;

@SuppressWarnings("deprecation")
@Service
public class ItemsService {
	protected Logger log = Logger.getLogger(this.getClass());

	@Autowired
	protected CustomCategorizationServiceImpl customCategorizationService;
	@Autowired
	protected CustomBrandsServiceImpl customBrandsService;
	@Autowired
	protected CustomFamiliesServiceImpl customFamiliesService;
	@Autowired
	protected CustomSectionsServiceImpl customSectionsService;
	@Autowired
	protected CustomMeasurementUnitsServiceImpl customMeasurementUnitsService;
	@Autowired
	protected CustomSuppliersServiceImpl customSuppliersService;
	@Autowired
	protected ArticulosService itemsService;
	
	protected ServicioTarifasImpl servicioTarifas = ServicioTarifasImpl.get();
	
	@Autowired
	protected GruposImpuestosService gruposImpuestosServices;
	
	@Autowired
	protected PorcentajesImpuestosService porcentajeImpuestosService;
	
	@Autowired
	protected ValoresParametrosObjetosService valoresParametrosObjetosService;
	
	protected ServicioCodigosBarrasArticulosImpl servicioCodigosBarrasArticulos = ServicioCodigosBarrasArticulosImpl.get();
	
	protected ServicioUnidadesMedidaImpl servicioUnidadesMedidas = ServicioUnidadesMedidaImpl.get();
	
	protected ServicioUnidadesMedidasArticulosImpl servicioUnidadesMedidasArticulos = ServicioUnidadesMedidasArticulosImpl.get();
	
	protected ServicioCanalesArticulosImpl servicioCanalesArticulosImpl = ServicioCanalesArticulosImpl.get();
	
	protected ServicioCanalesVentasImpl salesCannelsService = ServicioCanalesVentasImpl.get();
	
	@Autowired
	protected I18NService servicioI18N;
	
	@Autowired
	protected EtiquetasService servicioEtiquetas;
	
	@Autowired
	protected EtiquetasEnlacesService servicioEtiquetasEnlaces;

	@Autowired
	protected EntityIntegrationLogImpl entityIntegrationLog;
	
	@Autowired
	protected TaxServiceImpl taxServices;

	protected DatosSesionBean sessionData;
	
	protected static final String ID_CLASE_ENL_ETIQUETA = "D_ARTICULOS_TBL.CODART";
	protected static final String ID_CLASE_I18N = "D_ARTICULOS_TBL.DESART";

	// inicializa los valores de sesion y los servicios a utilizar
	protected void init() {
		sessionData = new DatosSesionBean();

		UsuarioBean usuario = new UsuarioBean();
		usuario.setIdUsuario(new Long(0));
		usuario.setUsuario("ADMINISTRADOR");
		usuario.setDesUsuario("ADMINISTRADOR");
		sessionData.setUsuario(usuario);
	}

	public void processItems(ItemsDTO listaArticulosERPDTO)
			throws Exception {
		
		if (sessionData == null) {
			init();
			
			sessionData.setUidActividad(System.getenv().get("uidActividad"));
			sessionData.setUidInstancia(System.getenv().get("uidInstancia"));		
			
			if (StringUtils.isEmpty(sessionData.getUidActividad()) || StringUtils.isEmpty(sessionData.getUidInstancia())) {
				throw new RuntimeException("No se han establecido las variables de entorno para la actividad e instancia");
			}
		}

		Long comienzo = java.lang.System.currentTimeMillis();

		Connection conn = new Connection();
		SqlSession sqlSession = null;
		int versionados = 0;
		int versionadosSurtido = 0;
		
		try {
			conn.abrirConexion(Database.getConnection());
			conn.iniciaTransaccion();		
			sqlSession = sessionData.getSqlSessionFactory().openSession(conn);
			TaxItemMapper taxItemMapper = sqlSession.getMapper(TaxItemMapper.class);
			
			for (ItemDTO itemDTO : listaArticulosERPDTO.getItems()) {				 				
				// control de errores por cada articulo
				Boolean updateError = false;
				
				try {
					conn.iniciaTransaccion();
					
					Boolean articuloModificado = true;
					Boolean articuloVersionado = true;					
					
					// salvar entidades auxiliares
					customCategorizationService.salvarCategorizacionArticulo(sessionData, itemDTO);
					customBrandsService.salvarMarcaArticulo(sessionData, itemDTO);
					customFamiliesService.salvarFamiliaArticulo(sessionData, itemDTO);
					customSectionsService.salvarSeccionArticulo(sessionData, itemDTO);
					customMeasurementUnitsService.salvarUMEtiqueta(sessionData, itemDTO);
					customSuppliersService.salvarProveedorArticulo(sessionData, itemDTO);
					
					
					ArticuloBean articulo = null;
					
					try {
						articulo = itemsService.consultar(conn, itemDTO.getItemCode(), sessionData);
					} catch (ArticuloNotFoundException e) {
						articuloModificado = false;
					}
							
					articulo = inicializarArticulo(articulo, itemDTO, articuloModificado, sessionData, conn);
																	
					itemsService.salvar(conn, articulo, sessionData);
					
					// el servicio de articulos pone la propiedad version a null
					// cuando el articulo ha sufrido modificaciones
					articuloVersionado = articulo.getVersion() == null;
					
					if (articuloVersionado) versionados++;
					
					salvarUnidadMedida(sessionData, conn, itemDTO);
					
					// Impuestos de articulos
					taxServices.saveTaxItem(taxItemMapper, sessionData, itemDTO);
					
					// Marcar entidad/objeto como valido
					entityIntegrationLog.entidadOk(conn, sessionData.getUidActividad(), ItemsDTO.ENTITY, itemDTO.getItemCode(), "*", listaArticulosERPDTO.getDocumentId());
					
					conn.commit();
					conn.finalizaTransaccion();					
				} catch (Exception e) {
			    	conn.deshacerTransaccion();
			    	updateError = true;
			    	
			    	log.error(e.getMessage(), e);			    	
			    				    	
			    	// Registrar excepcion y continuar con el siguiente articulo
			    	entityIntegrationLog.registraExcepcion(sessionData.getUidActividad(), new EntityIntegrationLogBean(ItemsDTO.ENTITY, itemDTO.getItemCode(), listaArticulosERPDTO.getDocumentId(), "*"
				    		 , "Error interno al salvar el artículo: " + e.getMessage(), e));
			    }
				
				// control de errores por cada articulo-tarifas
				// no actualizar tarifa si hubo error insertando artículo
				if (!updateError) {				
					try {
						conn.iniciaTransaccion();
						
						//Solo guardamos los articulos de las tarifas. Si la tarifa no existe se registra error
	  				    salvarArticulosTarifas(conn, sessionData, itemDTO);
						
						// Marcar entidad/objeto como valido
						entityIntegrationLog.entidadOk(conn, sessionData.getUidActividad(), ItemsDTO.ENTITY + "-RATES", itemDTO.getItemCode(), "*", listaArticulosERPDTO.getDocumentId());

						conn.commit();
						conn.finalizaTransaccion();					
					} catch (Exception e) {
				    	conn.deshacerTransaccion();
				    	
				    	log.error(e.getMessage());
				    	
						// excepcion general
					    entityIntegrationLog.registraExcepcion(sessionData.getUidActividad(), 
							   new EntityIntegrationLogBean(ItemsDTO.ENTITY + "-RATES", itemDTO.getItemCode(), listaArticulosERPDTO.getDocumentId(), "*"
				    		     , "Error interno al salvar tarifas : " + e.getMessage(), e)
							   );
				    }
				}
			}
		} finally {
			conn.cerrarConexion();

			log.info("Tiempo de procesamiento: "
					+ (java.lang.System.currentTimeMillis() - comienzo + " ms. Articulos: " + listaArticulosERPDTO.getItems().size() + " Versionados: " + versionados + " CambioSurtido: " + versionadosSurtido));
		}
	}

	protected ArticuloBean inicializarArticulo(ArticuloBean articulo, ItemDTO itemDTO, Boolean articuloModificado,
			DatosSesionBean datosSesion, Connection conn) throws Exception {
		if (articuloModificado) {
			articulo.setEstadoBean(Estado.MODIFICADO);
		} else {
			articulo = new ArticuloBean();
			articulo.setEstadoBean(Estado.NUEVO);
			articulo.setCodArticulo(itemDTO.getItemCode());
			
			ArticuloTarifaBean articuloTarifa = articulo.getArticuloTarifa();
			articuloTarifa.setCodArt(itemDTO.getItemCode());
			articuloTarifa.setCodImpuesto(itemDTO.getTaxCode());
			articuloTarifa.setFactorMarcaje(0.0);
			articuloTarifa.setPrecioCosto(0.0);
			articuloTarifa.setPrecioVenta(0.0);
			articuloTarifa.setPrecioTotal(0.0);
			articuloTarifa.setFechaInicio(itemDTO.getCreationDate() != null ? DateUtils.truncate(itemDTO.getCreationDate(), Calendar.DAY_OF_MONTH): DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH));
			articulo.setArticuloTarifa(articuloTarifa);
		}

		articulo.setDesArticulo(itemDTO.getItemDes());
		articulo.setDesglose1(itemDTO.getCombination1Active());
		articulo.setDesglose2(itemDTO.getCombination2Active());
		articulo.setCodgrupodesDesglose1(itemDTO.getCombination1GroupCode());
		articulo.setCodgrupodesDesglose2(itemDTO.getCombination2GroupCode());
		articulo.setCodCategorizacion(itemDTO.getCategoryCode());
		articulo.setDesCategorizacion(itemDTO.getCategoryDes());
		articulo.setCodProveedor(itemDTO.getSupplierCode());
		articulo.setDesProveedor(itemDTO.getSupplierDes());
		articulo.setReferencia(itemDTO.getSupplierReference());
		articulo.setCodMarca(itemDTO.getBrandCode());
		articulo.setDesMarca(itemDTO.getBrandDes());
		articulo.setCodFamilia(itemDTO.getFamilyCode());
		articulo.setDesFamilia(itemDTO.getFamilyDes());
		articulo.setCodSeccion(itemDTO.getSectionCode());
		articulo.setDesSeccion(itemDTO.getSectionDes());
		articulo.setDtoProveedor(itemDTO.getSupplierDiscount());
		articulo.setCodImpuesto(itemDTO.getTaxCode());
		articulo.setActivo(itemDTO.getActive());
		articulo.setFormato(itemDTO.getFormat());
		articulo.setGenerico(itemDTO.getGeneric());
		articulo.setFechaAlta(itemDTO.getCreationDate() != null ? DateUtils.truncate(itemDTO.getCreationDate(), Calendar.DAY_OF_MONTH): DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH));
		articulo.setBalanzaSeccion(itemDTO.getScaleSection());
		articulo.setBalanzaTipoArticulo(itemDTO.getScaleItemType());
		articulo.setBalanzaPlu(itemDTO.getScalePLU() != null ? itemDTO.getScalePLU().intValue() : null);
		articulo.setUnidadMedAlt(itemDTO.getUnitMeasureAltCode());
		articulo.setCodUnidadMedidaEtiq(itemDTO.getLabelUnitMeasureCode());
		articulo.setCantidadUnidadMedidaEtiq(itemDTO.getLabelUnitMeasureQuantity());
		articulo.setConfirmarPrecioVenta(itemDTO.getConfirmSalesPrice());
		articulo.setActAutomaticaCosto(itemDTO.getAutomaticCostUpdate());
		
		if(itemDTO.getLastCostUpdateDate() != null){
			articulo.setFechaPrecioCosto(itemDTO.getLastCostUpdateDate());//No se actualizar al llamar al update de articulo, por lo que es necesario llamar a este
			ArticulosDao.actualizarFechaPrecioCosto(conn, datosSesion.getConfigEmpresa(), articulo);
		}
		
		articulo.setObservaciones(itemDTO.getComments());
		articulo.setNumSeries(itemDTO.getSerialNumbersActive());
		
		// Codigos de barra
		articulo.setCodigosCargados(true);
		articulo.setCodigosBarras(cargarCodigosBarras(datosSesion, conn, itemDTO));
		
		// Propiedades dinamicas
		articulo.setPropiedadesDinamicasCargadas(true);
		articulo.setPropiedadesDinamicas(cargarPropiedadesDinamicas(articulo, itemDTO, datosSesion));
		
		//Etiquetas
		articulo.setEtiquetasCargadas(true);
		articulo.setEtiquetas(cargarEtiquetas(datosSesion, conn, itemDTO));
		
		//Canales
		articulo.setCanalesCargados(true);
		articulo.setCanales(cargarCanalesVenta(datosSesion, conn, itemDTO));
		
		//Internacionalizacion
		articulo.setLista18N(ID_CLASE_I18N, cargarInternacionalizacion(datosSesion, conn, itemDTO));
		
		return articulo;
	}

	protected List<ValorParametroObjeto> cargarPropiedadesDinamicas(ArticuloBean articulo, ItemDTO itemDTO,
			DatosSesionBean datosSesion)
			throws ValorParametroObjetoException, ValorParametroObjetoConstraintViolationException {
		
		List<ValorParametroObjeto> propiedadesDinamicasDefinitivas = new ArrayList<>();
		Map<String, String> propiedadesDinamicasNuevas = itemDTO.getDynamicProperties();
		try {
			List<ValorParametroObjeto> propiedadesDinamicasActuales = valoresParametrosObjetosService.consultarValoresParametrosPorClaseYObjeto(ValorParametroObjeto.CLASE_ARTICULOS,
								articulo.getCodArticulo(), datosSesion);

			
			//Eliminamos las propiedades dinamicas que no vengan en la bajada
			for(ValorParametroObjeto propDinamicaAct: propiedadesDinamicasActuales){
				if(!propiedadesDinamicasNuevas.containsKey(propDinamicaAct.getParametro())){
					propDinamicaAct.setEstadoBean(Estado.BORRADO);
				}
				propiedadesDinamicasDefinitivas.add(propDinamicaAct);
			}
			
			
			
			// verificar cambios en propiedades dinámicas actuales del artículo
			for (ValorParametroObjeto propiedadDinamicaActual : propiedadesDinamicasActuales) {
				if (propiedadesDinamicasNuevas.containsKey(propiedadDinamicaActual.getParametro())) {
					// evaluar cambios en la propiedad dinámica
					String nuevoValor = propiedadesDinamicasNuevas.get(propiedadDinamicaActual.getParametro());
					boolean valorActualizado = nuevoValor != null && !StringUtils.equals(nuevoValor, propiedadDinamicaActual.getValor());
					
					if(valorActualizado){
						propiedadDinamicaActual.setValor(nuevoValor);
						propiedadDinamicaActual.setEstadoBean(Estado.MODIFICADO);
					}
					
				}else {
					// propiedad dinámica borrada
					propiedadDinamicaActual.setEstadoBean(Estado.BORRADO);
				}
				propiedadesDinamicasDefinitivas.add(propiedadDinamicaActual);	
			}
			
			// insertar nuevas propiedades dinámicas
			for (Map.Entry<String, String> entry : propiedadesDinamicasNuevas.entrySet()) {			
				// buscar propiedad dinámica en la lista cargada
				ValorParametroObjeto propiedadDinamica = null;
			    
			    for (ValorParametroObjeto propiedadDinamicaActual : propiedadesDinamicasDefinitivas) {
			    	if (StringUtils.equals(propiedadDinamicaActual.getParametro(), entry.getKey())) {
			    		propiedadDinamica = propiedadDinamicaActual;
				    	break;
			    	}
			    }
			    
			    if (propiedadDinamica == null && entry.getValue() != null) {
			    	// nueva propiedad dinámica
			    	propiedadDinamica = new ValorParametroObjeto();
			    	propiedadDinamica.setEstadoBean(Estado.NUEVO);
			    	propiedadDinamica.setIdClase(ValorParametroObjeto.CLASE_ARTICULOS);
			    	propiedadDinamica.setIdObjeto(articulo.getCodArticulo());
			    	propiedadDinamica.setParametro(entry.getKey());
			    	propiedadDinamica.setValor(entry.getValue());
			    	propiedadesDinamicasDefinitivas.add(propiedadDinamica);
			    }
			}	
			
			
		} catch (Exception e) {
			throw new ValorParametroObjetoException(e.getMessage(), e);
		}
		
		return propiedadesDinamicasDefinitivas;
	}

	protected List<CodigoBarrasArticuloBean> cargarCodigosBarras(DatosSesionBean datosSesion, Connection conn, ItemDTO itemDTO)
			throws SQLException, CodigoBarrasArticuloException, CodigoBarrasArticuloConstraintViolationException,
			EtiquetasException, EtiquetasConstraintViolationException {
		List<CodigoBarrasArticuloBean> codigosBarras = new ArrayList<>();
		List<String> codigosBarrasNuevos = new ArrayList<>();
		
		//Añadimos a la lista los codBarras nuevos que vamos a crear
		for(ItemBarcodeDTO barCode : itemDTO.getItemBarcodes()){
			codigosBarrasNuevos.add(barCode.getBarcode());
		}
		
		
		//Consulto los codBarras del articulo en bbdd
		List<CodigoBarrasArticuloBean> listCodBarras = servicioCodigosBarrasArticulos.consultarCodigosBarras(itemDTO.getItemCode(), sessionData);
		//Eliminamos los codBarras que no vengan en la bajada
		for(CodigoBarrasArticuloBean codBarra: listCodBarras){
			if(!codigosBarrasNuevos.contains(codBarra.getCodigoBarras())){
				codBarra.setEstadoBean(Estado.BORRADO);
			}
			codigosBarras.add(codBarra);
		}
		
		for (ItemBarcodeDTO barCode : itemDTO.getItemBarcodes()) {
			CodigoBarrasArticuloBean codBar = null;
			try {
				codBar = servicioCodigosBarrasArticulos.consultarPorCodigoBarras(conn, barCode.getBarcode(),
						datosSesion);
				
				if (!codBar.getCodArticulo().equals(itemDTO.getItemCode())) {
				   // codigo de barras ha cambiado de artículo. Borrar del actual y dar de alta en el nuevo
					servicioCodigosBarrasArticulos.eliminar(codBar, datosSesion, conn);
					
					codBar.setEstadoBean(Estado.NUEVO);
				} else {				
					String principal = "N";
					if(barCode.getMain()) {
						principal = "S";
					}
					
					if((codBar.getFactorConversion() != null && !codBar.getFactorConversion().equals(barCode.getConversionFactor())) || (codBar.getFactorConversion() == null && barCode.getConversionFactor() != null)){
						codBar.setEstadoBean(Estado.MODIFICADO);
					}
					
					if(!codBar.getPrincipal().equals(principal)) {
						codBar.setEstadoBean(Estado.MODIFICADO);
					}
				}
			} catch (CodigoBarrasArticuloNotFoundException e) {
				codBar = new CodigoBarrasArticuloBean();
				codBar.setEstadoBean(Estado.NUEVO);
				codBar.setCodigoBarras(barCode.getBarcode());
			}

			codBar.setCodArticulo(itemDTO.getItemCode());
			codBar.setDesglose1(barCode.getCombination1Code());
			codBar.setDesglose2(barCode.getCombination2Code());			
			codBar.setDun14(barCode.getDun14());
			codBar.setPrincipal(barCode.getMain());

			codigosBarras.add(codBar);
		}
		return codigosBarras;
	}
	
	protected List<EtiquetaBean> cargarEtiquetas(DatosSesionBean datosSesion, Connection conn, ItemDTO itemDTO) throws Exception{
		
		//Borramos los enlaces de etiquetas del articulo que estuvieran asociados
		List<EtiquetaBean> listaEtiquetasPorArticulo = servicioEtiquetas.consultarEtiquetasPorClaseYObjeto(ID_CLASE_ENL_ETIQUETA, itemDTO.getItemCode(), datosSesion);
		for(EtiquetaBean etiquetaArticulo : listaEtiquetasPorArticulo){
			try {
				EtiquetaEnlaceBean etiquetaEnlace = new EtiquetaEnlaceBean();
				etiquetaEnlace.setUidEtiqueta(etiquetaArticulo.getUidEtiqueta());
				etiquetaEnlace.setIdClase(ID_CLASE_ENL_ETIQUETA);
				etiquetaEnlace.setIdObjeto(itemDTO.getItemCode());
				servicioEtiquetasEnlaces.eliminar(etiquetaEnlace, datosSesion);
			} catch (EtiquetasEnlacesException | EtiquetasEnlacesConstraintViolationException e) {
				throw new Exception("Se ha producido un error eliminando la etiqueta " + etiquetaArticulo.getEtiqueta() + " enlazada al articulo " + itemDTO.getItemCode() + " : " + e.getMessage(), e);
			}
		}
		
		//Añadimos las etiquetas nuevas
		List<EtiquetaBean> etiquetas = new ArrayList<>();
		for (ItemTagDTO itemTag : itemDTO.getItemTags()) {
			//Primero consultamos si existe la etiqueta. De no existir inicializamos como nueva
			EtiquetaBean etiqueta = null;
			try {
				etiqueta = servicioEtiquetas.consultar(itemTag.getTag(), datosSesion);
			} catch (EtiquetasNotFoundException | EtiquetasException e) {
				etiqueta = new EtiquetaBean();
				etiqueta.setUidEtiqueta(itemTag.getTag());
				etiqueta.setEtiqueta(itemTag.getTag());
				etiqueta.setCategoria(itemTag.getTagCategory());
				etiqueta.setPrioridad(itemTag.getPriority() != null ? itemTag.getPriority().intValue() : null);
			}
			
			//Luego enlazamos la etiqueta al articulo
			//Consultamos si existe enlace de la etiqueta, si no existe se crea
			EtiquetaEnlaceBean etiquetaEnlaceBean = servicioEtiquetasEnlaces.consultar(ID_CLASE_ENL_ETIQUETA, itemDTO.getItemCode(), itemTag.getTag(), datosSesion);
			if(etiquetaEnlaceBean == null){
				etiqueta.setEstadoBean(Estado.NUEVO);
			}
			etiqueta.setIdClaseEtiquetaEnlazada(ID_CLASE_ENL_ETIQUETA);
			etiqueta.setIdObjetoEtiquetaEnlazada(itemDTO.getItemCode());
			etiqueta.setUidEtiquetaEnlazada(itemTag.getTag());
			etiqueta.setPrioridadEnlace(itemTag.getPriority() != null ? itemTag.getPriority().intValue() : null);
			
			etiquetas.add(etiqueta);
		}
		return etiquetas;
	}
	
	protected List<I18NBean> cargarInternacionalizacion(DatosSesionBean datosSesion, Connection conn, ItemDTO itemDTO){
		List<I18NBean> traducciones = new ArrayList<I18NBean>();
		
		//Traduccion de descripcion del articulo por cada idioma
		for(Map.Entry<String, String> entry : itemDTO.getI18n().entrySet()){
			
			I18NBean i18n = new I18NBean();
			i18n.setUidActividad(datosSesion.getUidActividad());
			i18n.setIdClase("D_ARTICULOS_TBL.DESART");
			i18n.setCodlengua(entry.getKey());
			i18n.setIdObjeto(itemDTO.getItemCode());
			i18n.setValor(entry.getValue());
			i18n.setEstadoBean(Estado.NUEVO);

			I18NBean existe = servicioI18N.selectByPrimaryKey(i18n, datosSesion);
			if (existe!=null){
				i18n.setEstadoBean(Estado.MODIFICADO);
			}
			traducciones.add(i18n);
		}
		
		return traducciones;
	}
	
	protected List<CanalArticuloBean> cargarCanalesVenta(DatosSesionBean datosSesion, Connection conn, ItemDTO itemDTO) throws CanalArticuloException{
		List<CanalArticuloBean> canalesVenta = new ArrayList<>();
		
		//Consultamos los canales de venta asociados al articulo
		List<CanalArticuloBean> canalesVentaArticuloActuales = new ArrayList<>();
		List<String> codCanalesVentaArticuloActuales = new ArrayList<>();
		List<String> codCanalesVentaArticuloNuevos = new ArrayList<>();
		codCanalesVentaArticuloNuevos.addAll(itemDTO.getSalesChannels());
		
		try {
			canalesVentaArticuloActuales = servicioCanalesArticulosImpl.consultar(itemDTO.getItemCode(), datosSesion, conn);
			for(CanalArticuloBean canalArticuloBean : canalesVentaArticuloActuales){
				codCanalesVentaArticuloActuales.add(canalArticuloBean.getCodCanal());
			}
		}catch(CanalArticuloException e){
			throw new CanalArticuloException(e.getMessage(), e);
		}
		
		//Eliminamos los canales que no vengan en la bajada
		for(CanalArticuloBean canalArticuloActual : canalesVentaArticuloActuales){
			if(!codCanalesVentaArticuloNuevos.contains(canalArticuloActual.getCodCanal())){
				canalArticuloActual.setEstadoBean(Estado.BORRADO);
			}
			canalesVenta.add(canalArticuloActual);
		}
		
		//Si el articulo no contiene el canal que se está intentando añadir pues se añade. En caso contrario no hacemos nada
		for (String saleChannel : itemDTO.getSalesChannels()) {
			if(!codCanalesVentaArticuloActuales.contains(saleChannel)){
				//Consultamos si existe el canal venta. Si existe creamos el enlace con el articulo
				CanalVentaBean canalVenta = null;
				try{
					canalVenta = salesCannelsService.consultar(saleChannel, datosSesion);
				}catch(CanalVentaNotFoundException | CanalVentaException e){
					log.info("No se ha encontrado el canal de venta especificado: " + saleChannel + " por lo que solo se añadirá al GENERAL");
				}
				
				if(canalVenta != null){
					//añadimos el canal para que se persista al salvar el articulo
					CanalArticuloBean canalArticuloBean = new CanalArticuloBean();
					canalArticuloBean.setEstadoBean(Estado.NUEVO);
					canalArticuloBean.setCodCanal(saleChannel);
					canalArticuloBean.setDesCanal(saleChannel);
					canalArticuloBean.setCodArticulo(itemDTO.getItemCode());
					canalesVenta.add(canalArticuloBean);
				}
			}
		}
		return canalesVenta;
	}
	
	protected void salvarArticulosTarifas(Connection conn, DatosSesionBean datosSesion, ItemDTO itemDTO) throws Exception {
		for (ItemRateDTO itemRateDTO : itemDTO.getItemRates()) {
			
			//Consultamos que exista la tarifa. Si no existe que salte la excepcion
			TarifaBean tarifaBean = servicioTarifas.consultar(conn, itemRateDTO.getRateCode(), sessionData);

			ArticuloTarifaBean articuloTarifa = null;
			articuloTarifa = ArticulosTarifaDao.consultar(conn, 
					                    datosSesion.getConfigEmpresa(), 
					                    itemRateDTO.getRateCode(), 
					                    itemDTO.getItemCode(), 
					                    itemRateDTO.getCombination1Code(), itemRateDTO.getCombination2Code(),
					                    itemRateDTO.getStartDate() != null ? DateUtils.truncate(itemRateDTO.getStartDate(), Calendar.DAY_OF_MONTH): DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH));
			
			if (articuloTarifa != null) {
				articuloTarifa.setPrecioCosto(itemRateDTO.getUnitCostPrice());
				articuloTarifa.setPrecioVenta(itemRateDTO.getSalesPrice());
				articuloTarifa.setPrecioTotal(itemRateDTO.getSalesPriceWithTaxes());
				articuloTarifa.setFechaInicio(itemRateDTO.getStartDate() != null ? DateUtils.truncate(itemRateDTO.getStartDate(), Calendar.DAY_OF_MONTH): DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH));
				articuloTarifa.setEstadoBean(Estado.MODIFICADO);
			} else {
				articuloTarifa = new ArticuloTarifaBean();
				articuloTarifa.setEstadoBean(Estado.NUEVO);
				articuloTarifa.setCodTar(itemRateDTO.getRateCode());
				articuloTarifa.setCodArt(itemDTO.getItemCode());
				articuloTarifa.setPrecioCosto(itemRateDTO.getUnitCostPrice());
				articuloTarifa.setPrecioVenta(itemRateDTO.getSalesPrice());
				articuloTarifa.setPrecioTotal(itemRateDTO.getSalesPriceWithTaxes());
				articuloTarifa.setDesglose1(itemRateDTO.getCombination1Code());
				articuloTarifa.setDesglose2(itemRateDTO.getCombination2Code());
				articuloTarifa.setFechaInicio(itemRateDTO.getStartDate() != null ? DateUtils.truncate(itemRateDTO.getStartDate(), Calendar.DAY_OF_MONTH): DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH));
			}
			
			//Comprobamos y calculamos los precios en funcion de lo recibido
			compruebaCalculaPrecios(conn, datosSesion, itemDTO, tarifaBean, articuloTarifa);
			
			// insert/update
			servicioTarifas.salvarArticuloTarifa(conn, articuloTarifa, datosSesion);			
		}
	}
	
	protected void salvarUnidadMedida(DatosSesionBean datosSesion, Connection conn, ItemDTO itemDTO)
			throws UnidadMedidaArticuloException, UnidadMedidaArticuloConstraintViolationException, UnidadMedidaException, UnidadMedidaConstraintViolationException {
		if (itemDTO.getItemUnitMeasures() != null) {
			if(itemDTO.getItemUnitMeasures().size() > 0){
				for(ItemUnitMeasureDTO unitMeasure : itemDTO.getItemUnitMeasures()){
					
					//Si no existe la creamos
					try{
						servicioUnidadesMedidas.consultar(unitMeasure.getUnitMeasureCode(), datosSesion);
					}catch(UnidadMedidaException | UnidadMedidaNotFoundException e){
						log.info("No existe la unidad de medida " + unitMeasure.getUnitMeasureCode() + " por lo que se procede a crearse." );
						
						UnidadMedidaBean unidadMedida = new UnidadMedidaBean();
						unidadMedida.setEstadoBean(Estado.NUEVO);
						unidadMedida.setUnidadMedida(unitMeasure.getUnitMeasureCode());
						unidadMedida.setDesunidadMedida(unitMeasure.getUnitMeasureDes());
						unidadMedida.setPrioridadUnidadVenta(unitMeasure.getPrioritySalesUnit());
						
						servicioUnidadesMedidas.salvar(unidadMedida, datosSesion);
					}
					
					//Creamos el enlace de la unidad de medida con el articulo
					UnidadMedidaArticuloBean unidadMedida = new UnidadMedidaArticuloBean();
					try {
						unidadMedida = servicioUnidadesMedidasArticulos.consultarUnidadMedida(datosSesion,
								itemDTO.getItemCode(), unitMeasure.getUnitMeasureCode());
						// modificar si cambia el factor de conversion
						if (unidadMedida.getFactorConversion().compareTo(unitMeasure.getConversionFactor()) != 0) {
							unidadMedida.setEstadoBean(Estado.MODIFICADO);
							unidadMedida.setFactorConversion(unitMeasure.getConversionFactor());
							//Campos opcionales
							unidadMedida.setAlto(unitMeasure.getHeight());
							unidadMedida.setAncho(unitMeasure.getWidth());
							unidadMedida.setFondo(unitMeasure.getDepth());
							unidadMedida.setPeso(unitMeasure.getWeight());
							unidadMedida.setPrioridadUnidadVenta(unitMeasure.getPrioritySalesUnit());
							unidadMedida.setCantidadMinima(unitMeasure.getMinQuantity());
							unidadMedida.setCantidadMaxima(unitMeasure.getMaxQuantity());
							unidadMedida.setMultiplosCantidad(unitMeasure.getMultipleQuantity());
							
							servicioUnidadesMedidasArticulos.modificar(unidadMedida, datosSesion, conn);
						}
					} catch (UnidadMedidaArticuloNotFoundException e) {
						unidadMedida = new UnidadMedidaArticuloBean();
						unidadMedida.setEstadoBean(Estado.NUEVO);
						unidadMedida.setCodArticulo(itemDTO.getItemCode());
						unidadMedida.setUnidadMedida(unitMeasure.getUnitMeasureCode());
						unidadMedida.setFactorConversion(unitMeasure.getConversionFactor());
						//Campos opcionales
						unidadMedida.setAlto(unitMeasure.getHeight());
						unidadMedida.setAncho(unitMeasure.getWidth());
						unidadMedida.setFondo(unitMeasure.getDepth());
						unidadMedida.setPeso(unitMeasure.getWeight());
						unidadMedida.setPrioridadUnidadVenta(unitMeasure.getPrioritySalesUnit());
						unidadMedida.setCantidadMinima(unitMeasure.getMinQuantity());
						unidadMedida.setCantidadMaxima(unitMeasure.getMaxQuantity());
						unidadMedida.setMultiplosCantidad(unitMeasure.getMultipleQuantity());
						servicioUnidadesMedidasArticulos.crear(unidadMedida, datosSesion, conn);
					}
				}
			}
		}
	}
	
	protected void compruebaCalculaPrecios(Connection conn, DatosSesionBean datosSesion, ItemDTO itemDTO, TarifaBean tarifaBean, ArticuloTarifaBean articuloTarifa) throws Exception {
		try{	
			//verifica que estan todos los campos necesarios para calcular el precio si no genera evento y lanza excepcion
			compruebaCamposPrecio(articuloTarifa, tarifaBean);
			//obtiene el porcentaje para calcular el precio
			PorcentajeImpuestoBean porcentajeBean =  consultarPorcentaje(datosSesion, itemDTO, tarifaBean);
			calculoPrecio(porcentajeBean, articuloTarifa);						
		}catch (Exception e){
			throw new Exception("Se ha producido un error al comprobar los precios del articulos: " + e.getMessage(), e);
		}
	}
	
	protected static void compruebaCamposPrecio(ArticuloTarifaBean articuloTarifa, TarifaBean tarifaBean) throws Exception {		
		if(articuloTarifa.getFactorMarcaje() == null && articuloTarifa.getPrecioTotal() == null && articuloTarifa.getPrecioVenta() == null){
			String detalleError = "Imposible calcular precios: Precio Total, Precio Venta y Factor Marcaje Nulos,"+ "\r\n" +
					              " CodArt:" + articuloTarifa.getCodArt()+ "\r\n" +
			           		      " CodTar:" + tarifaBean.getCodTar();
			throw new Exception(detalleError);
		}
	}
	
	protected PorcentajeImpuestoBean consultarPorcentaje (DatosSesionBean datosSesion, ItemDTO itemDTO, TarifaBean tarifaBean) throws Exception{
		PorcentajeImpuestoBean porcentaje =null;
		GrupoImpuestosBean grupoImp = null;
		try{			
			grupoImp     = gruposImpuestosServices.consultar(datosSesion.getConfigEmpresa(), new Date());
			porcentaje   = porcentajeImpuestosService.consultar(datosSesion.getConfigEmpresa(), grupoImp.getIdGrupoImpuestos(), tarifaBean.getIdTratImpuestos() , itemDTO.getTaxCode());
		} catch (Exception e){
			String detalle = "Error al obtener el porcentaje de impuestos asociado al articulo " + itemDTO.getItemCode() + " de la tarifa " + tarifaBean.getCodTar();
			log.error("consultarPorcentaje() - " + detalle,e);
			throw new Exception(detalle);
		}
		return porcentaje;
	}
	
	protected void calculoPrecio(PorcentajeImpuestoBean porcentajeBean , ArticuloTarifaBean articuloTarifaBean) throws Exception{
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
