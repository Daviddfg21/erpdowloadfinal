package com.comerzzia.custom.erp.services.salesfiles;



import java.util.Base64;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.comerzzia.core.model.usuarios.UsuarioBean;
import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.custom.erp.services.salesfiles.persistence.SalesDocumentsFilesMapper;
import com.comerzzia.custom.integration.persistence.EntityIntegrationLogBean;
import com.comerzzia.custom.integration.services.EntityIntegrationLogImpl;
import com.comerzzia.integrations.model.sales.model.SalesDocumentFile;
import com.comerzzia.integrations.model.sales.model.SalesDocumentsFilesDTO;
import com.comerzzia.servicios.procesamiento.ventas.albaranes.PreparacionesAlbaranHandler;

@Service
public class SalesFilesService {
	protected Logger log = Logger.getLogger(this.getClass());
	
	@Autowired
	SalesDocumentsFilesMapper mapper;
	
	@Autowired
	private EntityIntegrationLogImpl entityIntegrationLog;
	private DatosSesionBean sessionData;
	
	protected PreparacionesAlbaranHandler preparacionesAlbaranHandler; 

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
	}

	public void processSalesDoc(SalesDocumentsFilesDTO salesDocuments)
			throws Exception {
		
		if (sessionData == null) {
			init();
		}

		Long comienzo = java.lang.System.currentTimeMillis();

		int correctos = 0;		
		try {			
			for (SalesDocumentFile salesDocument : salesDocuments.getSalesDocumentsFiles()) {				 				
				// control de errores por cada canal
				try {
					byte[] content = Base64.getDecoder().decode(salesDocument.getBase64Content());
					
					mapper.insert(sessionData.getUidActividad(), salesDocument.getDocumentUid(), salesDocument.getMimeType(), content, content.length);					
					
					correctos++;
				} catch (Exception e) {
			    	log.error(e.getMessage(), e);			    	
			    				    	
			    	// Registrar excepcion y continuar con la siguiente venta
			    	
			    	entityIntegrationLog.registraExcepcion(sessionData.getUidActividad(), new EntityIntegrationLogBean(SalesDocumentsFilesDTO.ENTITY, salesDocument.getDocumentUid(), salesDocuments.getDocumentId(), "*"
				    		 , "Error interno al salvar archivo del documento venta: " + e.getMessage(), e));
				    
			    }
			}
		} finally {
			log.info("Tiempo de procesamiento: "
					+ (java.lang.System.currentTimeMillis() - comienzo + " ms. archivos ventas: " + salesDocuments.getSalesDocumentsFiles().size() + " Correctos: " + correctos + " Erroneos: " + (salesDocuments.getSalesDocumentsFiles().size()-correctos)));
		}
	}
		
}
