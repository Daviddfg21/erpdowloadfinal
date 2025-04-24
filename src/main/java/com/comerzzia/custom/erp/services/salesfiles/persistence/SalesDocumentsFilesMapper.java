package com.comerzzia.custom.erp.services.salesfiles.persistence;


import org.apache.ibatis.annotations.Param;

public interface SalesDocumentsFilesMapper {
    int insert(@Param("uidActividad") String uidActividad,
    		   @Param("uidDocumento") String uidDocumento,
    		   @Param("mimeType") String mimeType,
    		   @Param("contenidoDocumento") byte[] contenidoDocumento,
    		   @Param("longitud") int longitud);

}