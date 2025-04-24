package com.comerzzia.custom.erp.services.warehouse;

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
import com.comerzzia.integrations.model.warehouse.WarehouseItemDTO;
import com.comerzzia.integrations.model.warehouse.WarehouseItemsDTO;
import com.comerzzia.model.general.almacenes.articulos.AlmacenArticuloBean;
import com.comerzzia.servicios.general.almacenes.articulos.AlmacenArticuloNotFoundException;
import com.comerzzia.servicios.general.almacenes.articulos.ServicioAlmacenesArticulosImpl;

@SuppressWarnings("deprecation")
@Service
public class WarehouseService {
	protected Logger log = Logger.getLogger(this.getClass());

	@Autowired
	private EntityIntegrationLogImpl entityIntegrationLog;
	
	private ServicioAlmacenesArticulosImpl servicioAlmacenesArticulos;

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

		servicioAlmacenesArticulos = ServicioAlmacenesArticulosImpl.get();
	}

	public void processStocks(WarehouseItemsDTO warehouseItemsDTO)
			throws Exception {
		
		if (sessionData == null) {
			init();
		}

		Long comienzo = java.lang.System.currentTimeMillis();

		Connection conn = new Connection();
		int erroneos = 0;		
		try {
			conn.abrirConexion(Database.getConnection());
			conn.iniciaTransaccion();			
			
			for(WarehouseItemDTO item: warehouseItemsDTO.getWhItems()){
				//Actualizamos directamente la tabla de D_ALMACENES_ARTICULOS_TBL. No hacemos regularizacion.
				try{					
					Boolean stockModificado = true;
					AlmacenArticuloBean almacenArticulo = null;
					try{
						almacenArticulo = servicioAlmacenesArticulos.consultar(conn, item.getItemCode(), warehouseItemsDTO.getWhCode(), item.getCombination1Code(), item.getCombination2Code(), sessionData);
					}catch(AlmacenArticuloNotFoundException e){
						stockModificado = false;
					}
					
					almacenArticulo = initAlmacenArticulo(warehouseItemsDTO, item, almacenArticulo, stockModificado);
					
					if(almacenArticulo.getEstadoBean() == Estado.NUEVO){
						//creamos stock
						servicioAlmacenesArticulos.crear(conn, almacenArticulo, sessionData);
					}else if(almacenArticulo.getEstadoBean() == Estado.MODIFICADO){
						//Actualizamos stock
						servicioAlmacenesArticulos.update(conn, sessionData, almacenArticulo);
					}

					// Marcar entidad/objeto como valido
					entityIntegrationLog.entidadOk(conn, sessionData.getUidActividad(), WarehouseItemsDTO.ENTITY, item.getItemCode(), warehouseItemsDTO.getWhCode(), warehouseItemsDTO.getDocumentId());
					
					conn.commit();
					conn.finalizaTransaccion();
				}catch(Exception e){
					conn.deshacerTransaccion();
					erroneos++;
					log.error(e.getMessage(), e);
					
					// Registrar excepcion y continuar con el siguiente articulo
			    	entityIntegrationLog.registraExcepcion(sessionData.getUidActividad(), new EntityIntegrationLogBean(WarehouseItemsDTO.ENTITY, item.getItemCode(), warehouseItemsDTO.getDocumentId(), warehouseItemsDTO.getWhCode()
				    		 , "Error interno al salvar el stock del articulo: " + e.getMessage(), e));
				}
			}
			
		} finally {
			conn.cerrarConexion();

			log.info("Tiempo de procesamiento: "
					+ (java.lang.System.currentTimeMillis() - comienzo + " ms. Stocks de articulos bajados : " + warehouseItemsDTO.getWhItems().size() + " del almacen " + warehouseItemsDTO.getWhCode() + " Correctos: " + (warehouseItemsDTO.getWhItems().size() - erroneos) + " Erroneos: " + erroneos));
		}
	}
	
	private AlmacenArticuloBean initAlmacenArticulo(WarehouseItemsDTO warehouseItemsDTO, WarehouseItemDTO item, AlmacenArticuloBean almacenArticulo, Boolean stockModificado){
		if(stockModificado){
			almacenArticulo.setEstadoBean(Estado.MODIFICADO);
		}else{
			almacenArticulo = new AlmacenArticuloBean();
			almacenArticulo.setEstadoBean(Estado.NUEVO);
			almacenArticulo.setCodAlm(warehouseItemsDTO.getWhCode());
			almacenArticulo.setCodArt(item.getItemCode());
			almacenArticulo.setDesglose1(item.getCombination1Code());
			almacenArticulo.setDesglose2(item.getCombination2Code());
			almacenArticulo.setStockUMA(0.0);
			almacenArticulo.setStockMinimo(0.0);
			almacenArticulo.setStockMaximo(0.0);
			almacenArticulo.setStockPendRecibir(0.0);
			almacenArticulo.setStockPendServir(0.0);
			almacenArticulo.setPmp(0.0);
		}
		almacenArticulo.setStock(item.getStock());
		almacenArticulo.setActivo(item.getActive());
		
		return almacenArticulo;
	}

}