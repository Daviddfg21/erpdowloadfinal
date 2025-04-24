package com.comerzzia.custom.erp.services.items;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.items.ItemDTO;
import com.comerzzia.model.general.proveedores.ProveedorBean;
import com.comerzzia.servicios.general.proveedores.ProveedorNotFoundException;
import com.comerzzia.servicios.general.proveedores.ServicioProveedoresImpl;

@Service
public class CustomSuppliersServiceImpl {	
	protected Logger log = Logger.getLogger(this.getClass());
	
	protected ServicioProveedoresImpl servicioProveedores = ServicioProveedoresImpl.get();
		
	public void salvarProveedorArticulo(DatosSesionBean datosSesion, ItemDTO itemDTO) throws Exception{
		if(StringUtils.isNotBlank(itemDTO.getSupplierCode())){
			ProveedorBean proveedor = new ProveedorBean();
			proveedor.setActivo(Boolean.TRUE);
			proveedor.setCodPro(itemDTO.getSupplierCode());
			proveedor.setDesPro(itemDTO.getSupplierDes() != null ? itemDTO.getSupplierDes() : itemDTO.getSupplierCode());
			proveedor.setCodPais("ES");//valor por defecto
			proveedor.setIdTratImp(1L);//valor por defecto
			
			try {
				try {
					ProveedorBean proveedorBean = servicioProveedores.consultar(proveedor.getCodPro(), datosSesion);
					if(!proveedorBean.getDesPro().equals(proveedor.getDesPro())){
						servicioProveedores.modificar(proveedor, datosSesion);
					}
				} catch (ProveedorNotFoundException e) {
					servicioProveedores.crear(proveedor, datosSesion);
				}
			} catch (Exception e) {
				String detalle = "Error salvar proveedor:" + "\r\n" + 
								 " Cod_Proveedor: " + itemDTO.getFamilyCode() + "\r\n" +
								 " Cod_Articulo: " + itemDTO.getItemCode();
						 
				log.error(detalle,e);
				throw new Exception(detalle + " - " + e.getMessage());
			}
		}
	}
}
