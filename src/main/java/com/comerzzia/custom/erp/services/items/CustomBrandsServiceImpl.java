package com.comerzzia.custom.erp.services.items;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.items.ItemDTO;
import com.comerzzia.model.general.marcas.MarcaBean;
import com.comerzzia.servicios.general.marcas.MarcaNotFoundException;
import com.comerzzia.servicios.general.marcas.ServicioMarcasImpl;

@Service
public class CustomBrandsServiceImpl {	
	protected Logger log = Logger.getLogger(this.getClass());
	
	protected ServicioMarcasImpl brandsService = ServicioMarcasImpl.get();
	
	public void salvarMarcaArticulo(DatosSesionBean datosSesion, ItemDTO itemDTO) throws Exception{
		if(StringUtils.isNotBlank(itemDTO.getBrandCode())){
			MarcaBean marca = new MarcaBean();
			marca.setActivo(Boolean.TRUE);
			marca.setCodMarca(itemDTO.getBrandCode());
			marca.setDesMarca(itemDTO.getBrandDes() != null ? itemDTO.getBrandDes() : itemDTO.getBrandCode());
	
			try {
				try {
					MarcaBean marcaBean = brandsService.consultar(marca.getCodMarca(), datosSesion);
					if(!marcaBean.getDesMarca().equals(marca.getDesMarca())){
						brandsService.modificar(marca, datosSesion);
					}
				} catch (MarcaNotFoundException e) {
					brandsService.crear(marca, datosSesion);
				}
			} catch (Exception e) {
				String detalle = "Error salvar marca:" + "\r\n" + 
								 " Cod_Marca: " + itemDTO.getBrandCode() + "\r\n" +
								 " Des_Marca: " + itemDTO.getBrandDes() + "\r\n";
				log.error(detalle, e);
				throw new Exception(detalle + " - " + e.getMessage());
			}
		}
	}
}
