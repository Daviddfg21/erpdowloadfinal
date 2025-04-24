package com.comerzzia.custom.erp.services.items;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.items.ItemDTO;
import com.comerzzia.model.general.categorizaciones.CategorizacionBean;
import com.comerzzia.servicios.general.categorizaciones.CategorizacionNotFoundException;
import com.comerzzia.servicios.general.categorizaciones.ServicioCategorizacionesImpl;

@Service
public class CustomCategorizationServiceImpl {	
	protected Logger log = Logger.getLogger(this.getClass());
	
	ServicioCategorizacionesImpl servicioCategorizaciones = ServicioCategorizacionesImpl.get();
			
	public void salvarCategorizacionArticulo(DatosSesionBean datosSesion, ItemDTO itemDTO) throws Exception{
		if(StringUtils.isNotBlank(itemDTO.getCategoryCode())){
			CategorizacionBean categorizacion = new CategorizacionBean();
			categorizacion.setActivo(Boolean.TRUE);
			categorizacion.setCodCat(itemDTO.getCategoryCode());
			categorizacion.setDesCat(itemDTO.getCategoryDes());
			
			try {
				try {
					CategorizacionBean categorizacionBean = servicioCategorizaciones.consultar(categorizacion.getCodCat(), datosSesion);
					if(!categorizacionBean.getDesCat().equals(categorizacion.getDesCat())){
						servicioCategorizaciones.modificar(categorizacion, datosSesion);
					}
				} catch (CategorizacionNotFoundException e) {
 				    servicioCategorizaciones.crear(categorizacion, datosSesion);
				}
			} catch (Exception e) {
				String detalle = "Error salvar categorizacion:" + "\r\n" + 
								 " Cod_Categorizacion: " + itemDTO.getCategoryCode() + "\r\n" +
								 " Cod_Articulo: " + itemDTO.getItemCode();
						 
				log.error(detalle,e);
				throw new Exception(detalle + " - "+e.getMessage());
			}
		}
	}
}
