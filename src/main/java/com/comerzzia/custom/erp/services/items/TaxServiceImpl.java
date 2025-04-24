package com.comerzzia.custom.erp.services.items;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.custom.erp.services.items.persistence.TaxItemMapper;
import com.comerzzia.integrations.model.catalog.items.ItemDTO;
import com.comerzzia.integrations.model.catalog.items.ItemTaxDTO;

/**
 * ISKAYPET - Envio de impuestos de articulos.
 */
@Service
public class TaxServiceImpl{

	public static final String TABLA_ARTICULOS_IMPUESTOS = "D_ARTICULOS_IMP_TBL";
	
	private static final Logger log = Logger.getLogger(TaxServiceImpl.class);
	
	public void saveTaxItem(TaxItemMapper taxItemMapper, DatosSesionBean datosSesion, ItemDTO itemDTO) throws Exception{

		// Eliminamos siempre los datos ya existentes de los impuestos.
		taxItemMapper.deleteAll(datosSesion.getUidActividad(), itemDTO.getItemCode());

		// Comprobamos si podemos insertar nuevos.
		List<ItemTaxDTO> listTaxItem = itemDTO.getItemTaxCodes();
		if(listTaxItem != null && !listTaxItem.isEmpty()){
			log.debug("saveTaxItem() - Se han encontrado un total de " + listTaxItem.size() 
			+ " datos de impuestos para el articulo " + itemDTO.getItemCode() + ".");
			for(ItemTaxDTO taxItem : listTaxItem){
				try{
					taxItemMapper.insertTaxItem(datosSesion.getUidActividad(), itemDTO.getItemCode(), taxItem.getTaxesTreatmentId(), taxItem.getTaxCode());
				}
				catch(Exception e){
					String msgError = "Error al salvar los datos de impuestos del articulo " 
							+ itemDTO.getItemCode() + " (TAXCODE : " + taxItem.getTaxCode() 
							+ " - TAXESTREATMENTID : " + taxItem.getTaxesTreatmentId() + ")";
					log.error("saveTaxItem() - " + msgError + " : " + e.getMessage(), e);
					throw new Exception(msgError, e);
				}
			}
		}
		else{
			log.debug("saveTaxItem() - No se han encontrado datos de impuestos para el articulo " + itemDTO.getItemCode() + ".");
		}
	}
	
}
