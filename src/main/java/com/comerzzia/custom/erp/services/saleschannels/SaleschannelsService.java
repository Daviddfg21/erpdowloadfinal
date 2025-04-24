package com.comerzzia.custom.erp.services.saleschannels;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.comerzzia.core.model.usuarios.UsuarioBean;
import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.core.util.base.Estado;
import com.comerzzia.core.util.db.Connection;
import com.comerzzia.core.util.db.Database;
import com.comerzzia.custom.integration.persistence.EntityIntegrationLogBean;
import com.comerzzia.custom.integration.services.EntityIntegrationLogImpl;
import com.comerzzia.integrations.model.catalog.saleschannels.SaleChannelDTO;
import com.comerzzia.integrations.model.catalog.saleschannels.SalesChannelsDTO;
import com.comerzzia.model.general.canales.CanalVentaBean;
import com.comerzzia.servicios.general.canalesventa.CanalVentaNotFoundException;
import com.comerzzia.servicios.general.canalesventa.ServicioCanalesVentasImpl;
import com.comerzzia.servicios.ventas.tarifas.ServicioTarifasImpl;

@SuppressWarnings("deprecation")
@Service
public class SaleschannelsService {
	protected Logger log = Logger.getLogger(this.getClass());

	private ServicioCanalesVentasImpl salesCannelsService;
	
	@Autowired
	private EntityIntegrationLogImpl entityIntegrationLog;
		
	private ServicioTarifasImpl servicioTarifas;

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

		salesCannelsService = new ServicioCanalesVentasImpl();
		servicioTarifas = ServicioTarifasImpl.get();
	}

	public void processSalesChannels(SalesChannelsDTO salesChannelsDTO)
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
			
			for (SaleChannelDTO saleChannelDTO : salesChannelsDTO.getChannels()) {				 				
				// control de errores por cada canal
				try {
					conn.iniciaTransaccion();
					
					Boolean salesCannelModifified = true;
					
					CanalVentaBean canalVenta = null;
					try{
						canalVenta = salesCannelsService.consultar(saleChannelDTO.getSalesChannelCode(), sessionData);
					}catch(CanalVentaNotFoundException e ){
						salesCannelModifified = false;
					}
					
					//Consultamos que exista la tarifa en cmz
					servicioTarifas.consultar(conn, saleChannelDTO.getRateCode(), sessionData);
					
					canalVenta = initSaleChannel(canalVenta, saleChannelDTO, salesCannelModifified, sessionData, conn);
					
					salesCannelsService.salvar(canalVenta, sessionData);
					
					// Marcar entidad/objeto como valido
					entityIntegrationLog.entidadOk(conn, sessionData.getUidActividad(), SalesChannelsDTO.ENTITY, saleChannelDTO.getSalesChannelCode(), "*", salesChannelsDTO.getDocumentId());
					
					conn.commit();
					conn.finalizaTransaccion();			
					correctos++;
				} catch (Exception e) {
			    	conn.deshacerTransaccion();
			    	
			    	log.error(e.getMessage(), e);			    	
			    				    	
			    	// Registrar excepcion y continuar con el siguiente articulo
			    	entityIntegrationLog.registraExcepcion(sessionData.getUidActividad(), new EntityIntegrationLogBean(SalesChannelsDTO.ENTITY, saleChannelDTO.getSalesChannelCode(), salesChannelsDTO.getDocumentId(), "*"
				    		 , "Error interno al salvar el artÃ­culo: " + e.getMessage(), e));
			    }
			}
		} finally {
			conn.cerrarConexion();

			log.info("Tiempo de procesamiento: "
					+ (java.lang.System.currentTimeMillis() - comienzo + " ms. Canales venta: " + salesChannelsDTO.getChannels().size() + " Correctos: " + correctos + " Erroneos: " + (salesChannelsDTO.getChannels().size()-correctos)));
		}
	}

	private CanalVentaBean initSaleChannel(CanalVentaBean canalVenta, SaleChannelDTO saleChannelDTO, Boolean salesCannelModifified, DatosSesionBean sessionData, Connection conn){
		
		if (salesCannelModifified) {
			canalVenta.setEstadoBean(Estado.MODIFICADO);
		} else {
			canalVenta = new CanalVentaBean();
			canalVenta.setEstadoBean(Estado.NUEVO);
			canalVenta.setCodCanal(saleChannelDTO.getSalesChannelCode());
		}
		
		canalVenta.setDesCanal(saleChannelDTO.getSalesChannelDes());
		//Valores opcionales
		if(saleChannelDTO.getRateCode() != null){
			canalVenta.setCodTarifa(saleChannelDTO.getRateCode());
		}
		if(saleChannelDTO.getReferenceRateCode() != null){
			canalVenta.setCodTarifaReferencia(saleChannelDTO.getReferenceRateCode());
		}
		
		
		return canalVenta;
	}

}
