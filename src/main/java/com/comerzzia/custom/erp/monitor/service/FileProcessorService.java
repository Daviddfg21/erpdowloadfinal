package com.comerzzia.custom.erp.monitor.service;
import java.nio.file.Path;

/**
 * Interfaz base para servicios de procesamiento de archivos
 * MEJORA: Interfaz mejorada con manejo detallado de resultados
 */
public interface FileProcessorService {
    /**
     * Procesa un archivo para un tipo específico de entidad
     * 
     * @param filePath Ruta del archivo a procesar
     * @return ResultadoProcesamiento con el resultado del procesamiento
     */
    ResultadoProcesamiento processFile(Path filePath);
    
    /**
     * Clase que encapsula el resultado de procesar un archivo
     */
    class ResultadoProcesamiento {
        private final boolean exitoso;
        private final String mensaje;
        private final Exception excepcion;
        
        public static ResultadoProcesamiento exito() {
            return new ResultadoProcesamiento(true, "Procesamiento exitoso", null);
        }
        
        public static ResultadoProcesamiento exito(String mensaje) {
            return new ResultadoProcesamiento(true, mensaje, null);
        }
        
        public static ResultadoProcesamiento error(String mensaje) {
            return new ResultadoProcesamiento(false, mensaje, null);
        }
        
        public static ResultadoProcesamiento error(String mensaje, Exception excepcion) {
            return new ResultadoProcesamiento(false, mensaje, excepcion);
        }
        
        private ResultadoProcesamiento(boolean exitoso, String mensaje, Exception excepcion) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.excepcion = excepcion;
        }
        
        public boolean isExitoso() {
            return exitoso;
        }
        
        public String getMensaje() {
            return mensaje;
        }
        
        public Exception getExcepcion() {
            return excepcion;
        }
    }
}