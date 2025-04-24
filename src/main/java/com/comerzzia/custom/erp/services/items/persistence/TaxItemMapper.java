package com.comerzzia.custom.erp.services.items.persistence;



import org.apache.ibatis.annotations.Param;

public interface TaxItemMapper {
	void insertTaxItem(@Param("uidActividad") String uidActividad, @Param("codArt") String codArt, 
            @Param("idTratImpuestos") Long idTratImpuestos, @Param("codImp") String codImp);

    void deleteAll(@Param("uidActividad") String uidActividad, @Param("codArt") String codArt);
}
