package com.comerzzia.custom.erp.services.items;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.integrations.model.catalog.items.ItemDTO;
import com.comerzzia.model.general.unidadesmedida.etiquetas.UnidadesMedidaEtiquetasBean;
import com.comerzzia.servicios.general.unidadesmedida.etiquetas.ServicioUnidadesMedidaEtiquetasImpl;
import com.comerzzia.servicios.general.unidadesmedida.etiquetas.UnidadesMedidaEtiquetaNotFoundException;

@Service
public class CustomMeasurementUnitsServiceImpl {	
	protected Logger log = Logger.getLogger(this.getClass());
	protected ServicioUnidadesMedidaEtiquetasImpl servicioUnidadesMedidaEtiquetas = ServicioUnidadesMedidaEtiquetasImpl.get();
	
	public void salvarUMEtiqueta(DatosSesionBean datosSesion, ItemDTO itemDTO) throws Exception{
		if(StringUtils.isNotBlank(itemDTO.getLabelUnitMeasureCode())){
			UnidadesMedidaEtiquetasBean unidadMedida = new UnidadesMedidaEtiquetasBean();
			unidadMedida.setCod_UM_Etiqueta(itemDTO.getLabelUnitMeasureCode());
			
			try {
				try {
					servicioUnidadesMedidaEtiquetas.consultar(unidadMedida.getCod_UM_Etiqueta(), datosSesion);
				} catch (UnidadesMedidaEtiquetaNotFoundException e) {
					unidadMedida.setActivo(Boolean.TRUE);
					unidadMedida.setDes_UM_Etiqueta(itemDTO.getLabelUnitMeasureCode());
					unidadMedida.setDesEtiqueta(itemDTO.getLabelUnitMeasureCode());
					unidadMedida.setFactor(1L);
					
					servicioUnidadesMedidaEtiquetas.crear(unidadMedida, datosSesion);
				}
			} catch (Exception e) {
				String detalle = "Error salvar unidad de medida de etiqueta:" + "\r\n" + 
			                     " Cod_UM_Etiqueta: " + itemDTO.getLabelUnitMeasureCode() + "\r\n" +
								 " Cod_Articulo: " + itemDTO.getItemCode();
				log.error(detalle, e);
				throw new Exception(detalle + " - " + e.getMessage());
			}
		}
	}
}
