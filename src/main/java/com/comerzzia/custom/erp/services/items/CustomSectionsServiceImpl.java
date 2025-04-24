package com.comerzzia.custom.erp.services.items;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.items.ItemDTO;
import com.comerzzia.model.general.secciones.SeccionBean;
import com.comerzzia.servicios.general.secciones.SeccionNotFoundException;
import com.comerzzia.servicios.general.secciones.ServicioSeccionesImpl;

@Service
public class CustomSectionsServiceImpl {	
	protected Logger log = Logger.getLogger(CustomSectionsServiceImpl.class);
		
	protected ServicioSeccionesImpl servicioSecciones = ServicioSeccionesImpl.get();
		
	public void salvarSeccionArticulo(DatosSesionBean datosSesion, ItemDTO itemDTO) throws Exception{
		if(StringUtils.isNotBlank(itemDTO.getSectionCode())){
			SeccionBean seccion = new SeccionBean();
			seccion.setActivo(Boolean.TRUE);
			seccion.setCodSeccion(itemDTO.getSectionCode());
			seccion.setDesSeccion(itemDTO.getSectionDes());
			try {
				try {
					SeccionBean seccionBean = servicioSecciones.consultar(seccion.getCodSeccion(), datosSesion);
					if(!seccionBean.getDesSeccion().equals(seccion.getDesSeccion())){
						servicioSecciones.modificar(seccion, datosSesion);
					}
				} catch (SeccionNotFoundException e) {
					servicioSecciones.crear(seccion, datosSesion);
				}
			} catch (Exception e) {
				String detalle = "Error salvar seccion:" + "\r\n" + 
			                     " Cod_Seccion: " + itemDTO.getSectionCode() + "\r\n" +
								 " Cod_Articulo: " + itemDTO.getItemCode();
				log.error(detalle, e);
				throw new Exception(detalle + " - " + e.getMessage());
			}
		}
	}
}
