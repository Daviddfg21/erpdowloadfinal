package com.comerzzia.custom.erp.services.items;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.items.ItemDTO;
import com.comerzzia.model.general.familias.FamiliaBean;
import com.comerzzia.servicios.general.familias.FamiliaNotFoundException;
import com.comerzzia.servicios.general.familias.ServicioFamiliasImpl;

@Service
public class CustomFamiliesServiceImpl {	
	protected Logger log = Logger.getLogger(this.getClass());
	
	protected ServicioFamiliasImpl servicioFamilias = ServicioFamiliasImpl.get();
		
	public void salvarFamiliaArticulo(DatosSesionBean datosSesion, ItemDTO itemDTO) throws Exception{
		if(StringUtils.isNotBlank(itemDTO.getFamilyCode())){
			FamiliaBean familia = new FamiliaBean();
			familia.setActivo(Boolean.TRUE);
			familia.setCodFam(itemDTO.getFamilyCode());
			familia.setDesFam(itemDTO.getFamilyDes());
			
			try {
				try {
					FamiliaBean familiaBean = servicioFamilias.consultar(familia.getCodFam(), datosSesion);
					if(!familiaBean.getDesFam().equals(familia.getDesFam())){
						servicioFamilias.modificar(familia, datosSesion);
					}
				} catch (FamiliaNotFoundException e) {
					servicioFamilias.crear(familia, datosSesion);
				}
			} catch (Exception e) {
				String detalle = "Error salvar familia:" + "\r\n" + 
								 " Cod_Familia: " + itemDTO.getFamilyCode() + "\r\n" +
								 " Cod_Articulo: " + itemDTO.getItemCode();
						 
				log.error(detalle,e);
				throw new Exception(detalle + " - " + e.getMessage());
			}
		}
	}
}
