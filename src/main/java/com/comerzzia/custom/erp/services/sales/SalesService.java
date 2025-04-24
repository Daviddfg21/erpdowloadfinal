package com.comerzzia.custom.erp.services.sales;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.comerzzia.core.model.impuestos.ImpuestoBean;
import com.comerzzia.core.model.impuestos.ImpuestosBean;
import com.comerzzia.core.model.impuestos.grupos.GrupoImpuestosBean;
import com.comerzzia.core.model.impuestos.porcentajes.PorcentajeImpuestoBean;
import com.comerzzia.core.model.tiposdocumentos.TipoDocumentoBean;
import com.comerzzia.core.model.tiposdocumentos.TipoDocumentoExample;
import com.comerzzia.core.model.usuarios.UsuarioBean;
import com.comerzzia.core.persistencia.tiposdocumentos.TipoDocumentoMapper;
import com.comerzzia.core.servicios.impuestos.grupos.GruposImpuestosService;
import com.comerzzia.core.servicios.impuestos.grupos.ServicioGruposImpuestosImpl;
import com.comerzzia.core.servicios.impuestos.porcentajes.PorcentajesImpuestosService;
import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.core.servicios.tipodocumento.TipoDocumentoNotFoundException;
import com.comerzzia.core.servicios.usuarios.UsuarioException;
import com.comerzzia.core.servicios.usuarios.UsuarioNotFoundException;
import com.comerzzia.core.util.base.Estado;
import com.comerzzia.core.util.db.Connection;
import com.comerzzia.core.util.db.Database;
import com.comerzzia.core.util.fechas.Fecha;
import com.comerzzia.core.util.numeros.BigDecimalUtil;
import com.comerzzia.core.util.numeros.Numero;
import com.comerzzia.custom.integration.persistence.EntityIntegrationLogBean;
import com.comerzzia.custom.integration.services.EntityIntegrationLogImpl;
import com.comerzzia.integrations.model.eticket.IdentificationCard;
import com.comerzzia.integrations.model.eticket.PaymentData;
import com.comerzzia.integrations.model.eticket.Promotion;
import com.comerzzia.integrations.model.eticket.SalesDocument;
import com.comerzzia.integrations.model.eticket.SalesDocumentsDTO;
import com.comerzzia.integrations.model.eticket.TicketItem;
import com.comerzzia.model.fidelizacion.fidelizados.FidelizadoBean;
import com.comerzzia.model.fidelizacion.fidelizados.contactos.TiposContactoFidelizadoBean;
import com.comerzzia.model.fidelizacion.tarjetas.TarjetaBean;
import com.comerzzia.model.general.almacenes.AlmacenBean;
import com.comerzzia.model.general.articulos.ArticuloBean;
import com.comerzzia.model.general.clientes.ClienteBean;
import com.comerzzia.model.general.mediospago.vencimientos.VencimientoBean;
import com.comerzzia.model.general.servicios.ServicioBean;
import com.comerzzia.model.general.servicios.contactos.ServicioContactosBean;
import com.comerzzia.model.general.servicios.tipos.EstadoServicio;
import com.comerzzia.model.general.servicios.tipos.ServicioTipoBean;
import com.comerzzia.model.ventas.albaranes.articulos.ArticuloAlbaranVentaBean;
import com.comerzzia.model.ventas.albaranes.descuentos.DetalleDescuentoAlbaranVentaBean;
import com.comerzzia.model.ventas.albaranes.pagos.PagoAlbaranVentaBean;
import com.comerzzia.model.ventas.promociones.uso.PromocionUsoBean;
import com.comerzzia.servicios.fidelizacion.fidelizados.FidelizadoNotFoundException;
import com.comerzzia.servicios.fidelizacion.fidelizados.FidelizadosService;
import com.comerzzia.servicios.fidelizacion.fidelizados.colectivos.ColectivoException;
import com.comerzzia.servicios.fidelizacion.fidelizados.contactos.TiposContactoFidelizadoException;
import com.comerzzia.servicios.fidelizacion.fidelizados.contactos.TiposContactoFidelizadoService;
import com.comerzzia.servicios.fidelizacion.tarjetas.ServicioTarjetasImpl;
import com.comerzzia.servicios.fidelizacion.tarjetas.TarjetaNotFoundException;
import com.comerzzia.servicios.general.almacenes.ServicioAlmacenesImpl;
import com.comerzzia.servicios.general.articulos.ArticuloException;
import com.comerzzia.servicios.general.articulos.ArticuloNotFoundException;
import com.comerzzia.servicios.general.articulos.ArticulosService;
import com.comerzzia.servicios.general.clientes.ServicioClientesImpl;
import com.comerzzia.servicios.general.mediospago.vencimientos.ServicioVencimientosImpl;
import com.comerzzia.servicios.general.secciones.SeccionException;
import com.comerzzia.servicios.general.secciones.SeccionNotFoundException;
import com.comerzzia.servicios.general.servicios.ServicioServiciosImpl;
import com.comerzzia.servicios.general.servicios.ServiciosNotFoundException;
import com.comerzzia.servicios.general.servicios.tipos.ServicioTiposException;
import com.comerzzia.servicios.general.servicios.tipos.ServicioTiposNotFoundException;
import com.comerzzia.servicios.general.servicios.tipos.ServiciosTiposService;
import com.comerzzia.servicios.general.unidadesmedida.UnidadMedidaException;
import com.comerzzia.servicios.procesamiento.ventas.albaranes.PreparacionesAlbaranHandler;
import com.comerzzia.servicios.ventas.albaranes.AlbaranVenta;
import com.comerzzia.servicios.ventas.albaranes.ServicioAlbaranesVentasImpl;
import com.comerzzia.servicios.ventas.promociones.uso.PromocionUsoNotFoundException;
import com.comerzzia.servicios.ventas.promociones.uso.PromocionesUsoService;

@SuppressWarnings("deprecation")
@Service
public class SalesService {
	protected Logger log = Logger.getLogger(this.getClass());
	
	@Autowired
	private EntityIntegrationLogImpl entityIntegrationLog;
	@Autowired
	private ArticulosService servicioArticulos;
	@Autowired
	private TipoDocumentoMapper tipoDocumentoMapper;
	@Autowired
	private PorcentajesImpuestosService porcentajesImpuestosService; 
	@Autowired
	private GruposImpuestosService gruposImpuestosService;
	@Autowired
	private FidelizadosService fidelizadosService;
	@Autowired
	private ServiciosTiposService serviciosTiposService;	
	@Autowired
	private TiposContactoFidelizadoService tiposContactoFidelizado;
	@Autowired
	private PromocionesUsoService promocionesUsoService;
	
	private ServicioAlbaranesVentasImpl servicioAlbaranesVentas;
	private ServicioAlmacenesImpl servicioAlmacenes;
	private ServicioVencimientosImpl servicioVencimientosImpl;
	private ServicioClientesImpl servicioClientesImpl;
	private ServicioGruposImpuestosImpl servicioGruposImpuestos;
	private ServicioTarjetasImpl servicioTarjetas;
	private DatosSesionBean sessionData;
	
	protected PreparacionesAlbaranHandler preparacionesAlbaranHandler; 
	
	protected SimpleDateFormat sdf = new SimpleDateFormat(Fecha.PATRON_FECHA_CORTA);

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

		servicioAlbaranesVentas = new ServicioAlbaranesVentasImpl();
		servicioAlmacenes = new ServicioAlmacenesImpl();
		servicioVencimientosImpl = new ServicioVencimientosImpl();
		servicioClientesImpl = new ServicioClientesImpl();
		servicioGruposImpuestos = new ServicioGruposImpuestosImpl();
		preparacionesAlbaranHandler =  new PreparacionesAlbaranHandler();
		servicioTarjetas = new ServicioTarjetasImpl();
	}

	public void processSales(SalesDocumentsDTO salesDocuments)
			throws Exception {
		
		if (sessionData == null) {
			init();
		}

		Long comienzo = java.lang.System.currentTimeMillis();

		Connection conn = new Connection();
		int correctos = 0;		
		try {
			conn.abrirConexion(Database.getConnection());
			conn.iniciaTransaccion();			
			
			for (SalesDocument salesDocument : salesDocuments.getSalesDocuments()) {				 				
				// control de errores por cada canal
				try {
					conn.iniciaTransaccion();
					
					AlbaranVenta albaran = new AlbaranVenta(sessionData.getConfigEmpresa());
					albaran.setEstadoBean(Estado.NUEVO);
					
					//Seller 
					albaran.setCif(salesDocument.getSeller().getTaxIdentificationNumber());
					albaran.setDomicilio(salesDocument.getSeller().getAddress());
					albaran.setPoblacion(salesDocument.getSeller().getTown());
					albaran.setLocalidad(salesDocument.getSeller().getTown());
					albaran.setProvincia(salesDocument.getSeller().getProvince());
					albaran.setCp(salesDocument.getSeller().getPostalCode());
					String codPais = salesDocument.getSeller().getCountryCode();
					albaran.setCodPais(codPais);
										
					//Store
					String codAlm = salesDocument.getStore().getStoreId();
					albaran.setCodAlmacen(codAlm);
					albaran.setDesAlmacen(salesDocument.getStore().getName());
					AlmacenBean almacen = servicioAlmacenes.consultar(conn, albaran.getCodAlmacen(), sessionData.getConfigEmpresa());
					albaran.setCodEmpresa(almacen.getCodEmp());
					
					//Ticket
					//TicketHeader
					albaran.setPeriodo(new Integer(salesDocument.getTicket().getTicketIssueData().getAccountingDate().toGregorianCalendar().get(Calendar.YEAR)).shortValue());										
					albaran.setCodSerie(codAlm);
					albaran.setNumAlbaran(salesDocument.getTicket().getTicketHeader().getDocumentId().longValue());
					
					albaran.setCodAlbaran(salesDocument.getTicket().getTicketHeader().getInvoiceNumber());
					albaran.setSerieAlbaran(salesDocument.getTicket().getTicketHeader().getSerial());

					String uidDocumento = salesDocument.getTicket().getTicketHeader().getDocumentUid();
					String codTipoDoc = salesDocument.getTicket().getTicketHeader().getInvoiceDocumentType();
					
					// por compatibilidad con primeras versiones, donde no era obligatorio
					if (uidDocumento == null) {
						uidDocumento = almacen.getCodEmp() + 
								       "-" + codTipoDoc + 
								       "-" + salesDocument.getTicket().getTicketHeader().getSerial() + 
								       "-" + salesDocument.getTicket().getTicketHeader().getDocumentId().toString();
					}
					
					albaran.getBean().setUidTicket(uidDocumento);					
					
					// tipo de documento
					TipoDocumentoExample tipoDocumentoExample = new TipoDocumentoExample();
					tipoDocumentoExample.or().andUidActividadEqualTo(sessionData.getUidActividad()).andCodTipoDocumentoEqualTo(codTipoDoc).andCodPaisEqualTo(codPais);
					
					List<TipoDocumentoBean> tiposDocumento = tipoDocumentoMapper.selectFromViewByExample(tipoDocumentoExample);
					TipoDocumentoBean tipoDocumento = null;
					if(!tiposDocumento.isEmpty()){
						tipoDocumento =  tiposDocumento.get(0);
					}

					if (tipoDocumento == null) {
						String msg = "No se ha encontrado el tipo de documento con identificador " + codTipoDoc + " para el país "+codPais;
						log.info("consultar() - " + msg);
						throw new TipoDocumentoNotFoundException(msg);
					}
					
					albaran.setIdTipoDocumento(tipoDocumento.getIdTipoDocumento());
					albaran.setCodAplicacion(tipoDocumento.getCodAplicacion());
					albaran.setCodConceptoAlmacen(tipoDocumento.getCodConAlm());
					albaran.setCodCliente(almacen.getCodCliente());
					albaran.setCodPais(codPais);
					albaran.setCodCaja(salesDocument.getTicket().getTicketHeader().getPosId());
					albaran.setUsuario(salesDocument.getTicket().getTicketHeader().getOperatorId());					
					
					ClienteBean clienteBean = servicioClientesImpl.consultar(conn, albaran.getCodCliente(), sessionData);
					albaran.setDesCliente(clienteBean.getDesCli());
					albaran.setCodcliFactura(clienteBean.getCodcliFactura() != null ? clienteBean.getCodcliFactura() : albaran.getCodCliente());
					
					//TicketIssueData
					Date fecha = salesDocument.getTicket().getTicketIssueData().getIssueDate().toGregorianCalendar().getTime();
					albaran.getBean().setFechaCompleta(fecha);
					albaran.getBean().setFecha(DateUtils.truncate(fecha, Calendar.DAY_OF_MONTH));
					SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm:ss");					
					albaran.getBean().setHora(sdfHora.format(fecha));
					albaran.setFechaSuministro(salesDocument.getTicket().getTicketIssueData().getAccountingDate().toGregorianCalendar().getTime());
					albaran.setCodDivisa(salesDocument.getTicket().getTicketIssueData().getInvoiceCurrencyCode());
					
					GrupoImpuestosBean grupoImpuestosBean = servicioGruposImpuestos.consultar(conn, sessionData, new Date());
					albaran.setIdGrupoImpuestos(grupoImpuestosBean.getIdGrupoImpuestos());
					albaran.setIdTratamientoImpuestosCliente(clienteBean.getIdTratImp());
										
					//TicketItems
					Map<String, BigDecimal> mapaImp = new HashMap<>();
					
					if(salesDocument.getTicket().getTicketItems().getTicketItem() != null){
						List<ArticuloAlbaranVentaBean> articulos = new ArrayList<>();
						
						Integer linea = 1;
						
						for(TicketItem ticketItem : salesDocument.getTicket().getTicketItems().getTicketItem()){
							ArticuloBean articulo = servicioArticulos.consultar(conn, ticketItem.getItemCode(), sessionData);
							PorcentajeImpuestoBean porcentajeBean =  consultarPorcentaje(sessionData, articulo.getCodImpuesto(), clienteBean.getIdTratImp());
							
							//obtiene el factor de impuesto para extraer las bases
                            BigDecimal factorImpuesto  = BigDecimal.ONE.add(porcentajeBean.getPorcentaje().divide(new BigDecimal(100),4,BigDecimal.ROUND_HALF_UP));
							
							ArticuloAlbaranVentaBean articuloAlbaran = new ArticuloAlbaranVentaBean();
							articuloAlbaran.setEstadoBean(Estado.NUEVO);
							articuloAlbaran.setCodArticulo(ticketItem.getItemCode());
							articuloAlbaran.setDesArticulo(ticketItem.getItemDescription());
							articuloAlbaran.setFecha(albaran.getFecha());
							articuloAlbaran.setCodImpuesto(articulo.getCodImpuesto());
							articuloAlbaran.setDesglose1("*");
							articuloAlbaran.setDesglose2("*");
							articuloAlbaran.setCantidad(ticketItem.getQuantity().doubleValue());
							articuloAlbaran.setUnidadMedida(ticketItem.getUnitOfMeasure());
							
							if (ticketItem.getUnitMeasureQuantity() != null) {
								articuloAlbaran.setCantidadMedida(ticketItem.getUnitMeasureQuantity().doubleValue());
							}
							
							articuloAlbaran.setPrecio(ticketItem.getUnitPrice().doubleValue());							
							articuloAlbaran.setPrecioTotal(ticketItem.getUnitPrice().doubleValue());
							articuloAlbaran.setImporte(ticketItem.getTotalAmount().doubleValue());							
							articuloAlbaran.setImporteTotal(ticketItem.getTotalAmount().doubleValue());
							articuloAlbaran.setDescuento(0.0);
							
							// quitar los impuestos
							if(!BigDecimalUtil.isIgualACero(factorImpuesto)){
								BigDecimal importeSinImpuestos = ticketItem.getTotalAmount().divide(factorImpuesto, 4, BigDecimal.ROUND_HALF_UP);
								
								// los importes intermedios se reondean a dos decimales, y el precio unitario se obtiene dividiendo el importe/cantidad
								articuloAlbaran.setImporte(BigDecimalUtil.redondear(importeSinImpuestos, 2).doubleValue());
								articuloAlbaran.setPrecio(importeSinImpuestos.divide(ticketItem.getQuantity(), 4, BigDecimal.ROUND_HALF_UP).doubleValue());
							}												
							
							BigDecimal descuentos = BigDecimal.ZERO;
							
							if (ticketItem.getPromotions() != null) {
								for (Promotion promotion : ticketItem.getPromotions().getPromotion()) {
									descuentos = descuentos.add(promotion.getDiscountAmount());

									// Agregar a D_CLIE_VENTAS_DET_MOD_PRE_TBL
									List<DetalleDescuentoAlbaranVentaBean> listDetallesDescuentos = new ArrayList<>();
									int lineaModificacion = 0;

									lineaModificacion++;
									DetalleDescuentoAlbaranVentaBean detalleDescuento = new DetalleDescuentoAlbaranVentaBean();
									detalleDescuento.setLinea(linea);
									detalleDescuento.setLineaModificacion(lineaModificacion);
									detalleDescuento.setOrigenModificacion("PROMOCION");
									detalleDescuento.setDocumentoReferencia(promotion.getPromotionId().toString());
									detalleDescuento.setPrecioEntrada(BigDecimal.ZERO);
									detalleDescuento.setPrecioSalida(BigDecimal.ZERO);
									detalleDescuento.setModificacion(promotion.getDiscountAmount());
									detalleDescuento.setAplicadoEn("L");
									listDetallesDescuentos.add(detalleDescuento);

									// Salvamos el uso de la promocion
									Double importeDescuento = promotion.getDiscountAmount() != null ? promotion.getDiscountAmount().doubleValue() : 0.0;

									PromocionUsoBean promocionUso = null;
									try {
										promocionUso = promocionesUsoService.consultar(conn, Long.parseLong(promotion.getPromotionId().toString()), PromocionUsoBean.ID_CLASE_PROMOCION,
										        promotion.getPromotionId().toString(), sessionData.getConfigEmpresa());
										promocionUso.setEstadoBean(Estado.MODIFICADO);
										Date fechaAlbaran = Fecha.getFechaHora(sdf.format(albaran.getFecha()), albaran.getHora()).getDate();
										promocionUso.setFechaUltimoUso(fechaAlbaran);
										promocionUso.setNumeroUsos(promocionUso.getNumeroUsos() + 1);
										promocionUso.setImporteDescuento(Numero.redondea((promocionUso.getImporteDescuento() + importeDescuento), 2));
										promocionUso.setImporteVenta(Numero.redondea((promocionUso.getImporteVenta() + importeDescuento), 2));

									}
									catch (PromocionUsoNotFoundException e) {
										promocionUso = new PromocionUsoBean();
										promocionUso.setEstadoBean(Estado.NUEVO);
										promocionUso.setIdPromocion(promotion.getPromotionId().longValue());
										promocionUso.setIdClase(PromocionUsoBean.ID_CLASE_PROMOCION);
										promocionUso.setIdObjeto(promotion.getPromotionId().toString());
										promocionUso.setNumeroUsos(Integer.valueOf(1));
										promocionUso.setFechaPrimerUso(new Date());
										promocionUso.setFechaUltimoUso(new Date());
										promocionUso.setConfirmado("S");
										promocionUso.setImporteVenta(articuloAlbaran.getImporteTotal());
										promocionUso.setImporteDescuento(importeDescuento);
									}
									promocionesUsoService.salvar(conn, promocionUso, sessionData.getConfigEmpresa());

									albaran.getBean().setDetalleDescuentos(listDetallesDescuentos);
								}
							}
							
							// porcentaje de descuento calculado
							if(!BigDecimalUtil.isIgualACero(descuentos) && !BigDecimalUtil.isIgualACero(ticketItem.getTotalAmount())){
								articuloAlbaran.setDescuento(descuentos.multiply(new BigDecimal(100)).divide(ticketItem.getTotalAmount(), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
							}
																												
							articuloAlbaran.setPrecioCosto(0.0);
							articuloAlbaran.setLinea(linea);

							linea++;
							articulos.add(articuloAlbaran);
														
							// acmulamos los totales por cada tipo de impuesto							
							BigDecimal acumulado = mapaImp.get(articulo.getCodImpuesto());
							
							if(acumulado == null){								
								acumulado = ticketItem.getTotalAmount();
							} else {
								acumulado = acumulado.add(ticketItem.getTotalAmount());								
							}							
							mapaImp.put(articulo.getCodImpuesto(), acumulado);
						}
						
						albaran.setArticulos(articulos);
					}
					
					// calcular los impuestos
					List<ImpuestoBean> listadoImpuestos = new ArrayList<>();
					
					for(Map.Entry<String, BigDecimal> entry : mapaImp.entrySet()){													
						ImpuestoBean impuestoBean = new ImpuestoBean();
						impuestoBean.setCodImpuesto(entry.getKey());
						impuestoBean.setBase(entry.getValue().doubleValue());
						impuestoBean.setImpuestos(0.0);
						
						// obtener la base y el impuesto retrayendo del importe con impuestos
						PorcentajeImpuestoBean porcentajeBean =  consultarPorcentaje(sessionData, impuestoBean.getCodImpuesto(), clienteBean.getIdTratImp());
						
						//obtiene el factor de impuesto para extraer las bases
						BigDecimal factorImpuesto  = BigDecimal.ONE.add(porcentajeBean.getPorcentaje().divide(new BigDecimal(100),4,BigDecimal.ROUND_HALF_UP));
					                            
						if(!BigDecimalUtil.isIgualACero(factorImpuesto)){
							BigDecimal base = entry.getValue().divide(factorImpuesto, 2, BigDecimal.ROUND_HALF_UP);
						
							impuestoBean.setBase(base.doubleValue());
							impuestoBean.setImpuestos(entry.getValue().subtract(base).doubleValue());
						}
						
						listadoImpuestos.add(impuestoBean);
					}
										
					ImpuestosBean impuestosBean = new ImpuestosBean();
					impuestosBean.setListaImpuestos(listadoImpuestos);
					albaran.setDetalleImpuestos(impuestosBean);
					
					// totales
					Double total = salesDocument.getTicket().getTicketIssueData().getTotalGrandAmount().doubleValue();
					albaran.getBean().setTotal(total);
					
					Double base = salesDocument.getTicket().getTicketIssueData().getTotalGrossAmount().doubleValue();
					albaran.getBean().setBase(base);
					
					Double Impuestos = total - base;
					albaran.getBean().setImpuestos(Impuestos);	
					
					//PaymentsData
					if (salesDocument.getPaymentsData() == null) {
						throw new Exception("La venta no tiene asignada el tag PaymentsData");						
					}
					
					if(salesDocument.getPaymentsData().getPaymentData() != null){
						List<PagoAlbaranVentaBean> pagos = new ArrayList<>();
						for(PaymentData paymentData : salesDocument.getPaymentsData().getPaymentData()){
							PagoAlbaranVentaBean pagoAlbaran = new PagoAlbaranVentaBean();
							pagoAlbaran.setEstadoBean(Estado.NUEVO);
							
							List<VencimientoBean> vencimientos = servicioVencimientosImpl.consultarVencimientos(paymentData.getPaymentMethodCode(), sessionData);
							Long idMedioPagoVencimiento = null;
							if(vencimientos.isEmpty()){
								throw new Exception("No se ha encontrado un idMedPagVen que corresponda con el metodo de pago recibido " + paymentData.getPaymentMethodCode());
							}else{
								idMedioPagoVencimiento = vencimientos.get(0).getIdMedioPagoVencimiento();
							}
							pagoAlbaran.setIdMedioPagoVencimiento(idMedioPagoVencimiento);
							pagoAlbaran.setImporte(paymentData.getPaymentAmount().doubleValue());
							
							pagos.add(pagoAlbaran);
						}
						albaran.setPagos(pagos);
					}
			
					// Asignar id de fidelizado
					if (salesDocument.getLoyaltyDetails() != null && 
						StringUtils.isNotBlank(salesDocument.getLoyaltyDetails().getLoyalCustomerId())){
						albaran.setIdFidelizado(Long.parseLong(salesDocument.getLoyaltyDetails().getLoyalCustomerId()));
					}
					
					//IdentificationCards
					String tarjetaFidelizacion = null;
					
					if(salesDocument.getIdentificationCards() != null && salesDocument.getIdentificationCards().getIdentificationCard() != null){
						for(IdentificationCard identificationCard : salesDocument.getIdentificationCards().getIdentificationCard()){
							if(identificationCard.getCardType().equals("F")){
								albaran.setTarjetaFidelizacion(identificationCard.getCardCode());
								break;
							}
						}
					}
					
					//Si llega Id de Fidelizado, pero no la tarjeta de fidelización, buscar y asignar la primera tarjeta de FIDELIZACIÓN asignada al fidelizado
					if (tarjetaFidelizacion == null && albaran.getIdFidelizado() != null){
                        List<TarjetaBean> tarjetasFidelizacion = servicioTarjetas.consultarTarjetasCliente(albaran.getIdFidelizado(), sessionData);
						
                        if (tarjetasFidelizacion != null) {
                           for (TarjetaBean tarjeta : tarjetasFidelizacion) {
                        	   // que sea tarjeta de fidelización
                        	   if (tarjeta.getPermiteVincular() && !tarjeta.getPermitePago()) {
                        		   tarjetaFidelizacion = tarjeta.getNumeroTarjeta();
                        		   break;
                        	   }
                        	   
                           }
						}
                        
                        if (tarjetaFidelizacion == null) {
                        	log.warn("Se ha recibido una venta con el fidelizado " + albaran.getIdFidelizado().toString() + ", pero este no dispone de tarjeta de fidelización");
                        }
					}
					
					albaran.setTarjetaFidelizacion(tarjetaFidelizacion);
					
					//Salvamos el albaran 
					servicioAlbaranesVentas.crear(albaran, sessionData, conn);
					
					//Si tiene fidelizado generamos servicio
					if (albaran.getIdFidelizado() != null) {					   
					   generarServicio(conn, sessionData, albaran, albaran.getBean().getUidTicket());
					}
					
					conn.commit();
					conn.finalizaTransaccion();			
					correctos++;
				} catch (Exception e) {
			    	conn.deshacerTransaccion();
			    	
			    	log.error(e.getMessage(), e);			    	
			    				    	
			    	// Registrar excepcion y continuar con la siguiente venta
			    	
			    	entityIntegrationLog.registraExcepcion(sessionData.getUidActividad(), new EntityIntegrationLogBean(SalesDocumentsDTO.ENTITY, salesDocument.getTicket().getTicketHeader().getInvoiceNumber(), salesDocuments.getDocumentId(), "*"
				    		 , "Error interno al salvar la venta: " + e.getMessage(), e));
				    
			    }
			}
		} finally {
			conn.cerrarConexion();

			log.info("Tiempo de procesamiento: "
					+ (java.lang.System.currentTimeMillis() - comienzo + " ms. ventas: " + salesDocuments.getSalesDocuments().size() + " Correctos: " + correctos + " Erroneos: " + (salesDocuments.getSalesDocuments().size()-correctos)));
		}
	}
	
	private PorcentajeImpuestoBean consultarPorcentaje (DatosSesionBean datosSesion, String codImp, Long idTratImp) throws Exception{
		PorcentajeImpuestoBean porcentaje =null;
		GrupoImpuestosBean grupoImp = null;
		try{			
			grupoImp     = gruposImpuestosService.consultar(datosSesion.getConfigEmpresa(), new Date());
			porcentaje   = porcentajesImpuestosService.consultar(datosSesion.getConfigEmpresa(), grupoImp.getIdGrupoImpuestos(), idTratImp , codImp);
		} catch (Exception e){
			String detalle = "Error al obtener el porcentaje de impuestos asociado al tipo de impuesto " + codImp;
			log.error("consultarPorcentaje() - " + detalle,e);
			throw new Exception(detalle);
		}
		return porcentaje;
	}

	protected void generarServicio(Connection conn, DatosSesionBean datosSesion, AlbaranVenta albaran, String uidTicket) 
			throws TiposContactoFidelizadoException, ColectivoException, ServicioTiposException, ServicioTiposNotFoundException, SeccionException, SeccionNotFoundException, ArticuloNotFoundException, ArticuloException, UnidadMedidaException, UsuarioException, UsuarioNotFoundException, ServiciosNotFoundException, FidelizadoNotFoundException, TarjetaNotFoundException {
		
		ServicioBean servicio = new ServicioBean();
		servicio.setUidServicio(UUID.randomUUID().toString());
		servicio.setUidDocumentoOrigen(uidTicket);
		servicio.setIdFidelizado(albaran.getIdFidelizado());
		
		servicio.setFechaRegistro(albaran.getFechaCompleta());
		servicio.setFechaServicio(servicio.getFechaRegistro());
		
		servicio.setCodalmDestino(albaran.getCodAlmacen());
		servicio.setCodalmOrigen(albaran.getCodAlmacen());
		servicio.setCodalmPreparacion(albaran.getCodAlmacen());

		servicio.setFechaEstado(servicio.getFechaRegistro());
		servicio.setImporte(new BigDecimal(albaran.getTotal()));
		servicio.setImportePendiente(BigDecimal.ZERO);
				
		servicio.setCodtipserv(EstadoServicio.COD_SERV_VENTA_DIRECTA);
		servicio.setIdEstado(1L);
				
		ServicioTipoBean servicioTipo = serviciosTiposService.consultar(servicio.getCodtipserv(), datosSesion);
		
		servicio.setIdAccionEstados(servicioTipo.getIdAccionEstados());
		
		FidelizadoBean fidelizado = null;
		try {
		   fidelizado = fidelizadosService.consultar(albaran.getIdFidelizado(), datosSesion);
		} catch (FidelizadoNotFoundException ignore) {
		   // Si el fidelizado no se encuentra, ignorar y generar el servicio sin sus datos
		}
		
		if(fidelizado != null){
			servicio.setNombre(fidelizado.getNombre());
			servicio.setApellidos(fidelizado.getApellidos());
			servicio.setDomicilio(fidelizado.getDomicilio());
			servicio.setPoblacion(fidelizado.getPoblacion());
			servicio.setLocalidad(fidelizado.getLocalidad());
			servicio.setProvincia(fidelizado.getProvincia());
			servicio.setCp(fidelizado.getCp());
			servicio.setCodpais(fidelizado.getCodPais());
			servicio.setContactosCargados(true);
			
			List<TiposContactoFidelizadoBean> lstTipoContactoFidelizado = tiposContactoFidelizado.consultar(albaran.getIdFidelizado(), datosSesion);

			for(TiposContactoFidelizadoBean fidelizadoContacto : lstTipoContactoFidelizado){
				if(fidelizadoContacto.getRecibeNotificaciones()){
					ServicioContactosBean contacto = new ServicioContactosBean(datosSesion.getUidActividad(), servicio.getUidServicio(), fidelizadoContacto.getCodTipoCon(), fidelizadoContacto.getValor());
                    servicio.getLstContactos().add(contacto);					
				}
			}
		}
		
		ServicioServiciosImpl.get().crear(conn, servicio, datosSesion);
	}
	
	
}
