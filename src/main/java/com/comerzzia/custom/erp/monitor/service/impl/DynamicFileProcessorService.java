package com.comerzzia.custom.erp.monitor.service.impl;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import com.comerzzia.custom.erp.monitor.domain.Entity;
import com.comerzzia.custom.erp.monitor.domain.entities.GenericEntity;
import com.comerzzia.custom.erp.monitor.service.FileProcessorService;

/**
 * Procesador de archivos dinámico que determina el tipo de entidad,
 * convierte el XML al DTO correspondiente y llama al servicio adecuado
 * en base a la configuración del YAML.
 * MEJORA: Implementación mejorada de la interfaz con ResultadoProcesamiento
 */
@Service
@Primary
public class DynamicFileProcessorService implements FileProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(DynamicFileProcessorService.class);
    
    @Autowired
    private ApplicationContext appContext;
    
    @Autowired
    private List<Entity> configuredEntities;
    
    // Configuración de entidades y sus correspondientes clases DTO y métodos de servicio
    private Map<String, EntityTypeConfig> entityConfigs = new HashMap<>();
    
    // Cache de JAXBContext para mejorar rendimiento
    private Map<Class<?>, JAXBContext> jaxbContextCache = new HashMap<>();
    
    @PostConstruct
    public void initialize() {
        try {
            // Cargar configuración de entidades desde el YAML
            loadEntityConfigurations();
            
            logger.info("Procesador dinámico de archivos inicializado con {} tipos de entidad", entityConfigs.size());
            
            // Verificar que todos los servicios y métodos existen
            validateServiceMethods();
            
            // Listar las entidades configuradas para depuración
            for (Entity entity : configuredEntities) {
                if (entity instanceof GenericEntity) {
                    GenericEntity genericEntity = (GenericEntity) entity;
                    logger.info("Entidad configurada: {} (tipo: {})", 
                        entity.getName(), genericEntity.getEntityType());
                } else {
                    logger.info("Entidad configurada: {}", entity.getName());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error inicializando el procesador dinámico", e);
        }
    }
    
    /**
     * Carga las configuraciones de entidades desde el archivo folder-monitor.yml
     */
    private void loadEntityConfigurations() {
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = new ClassPathResource("config/folder-monitor.yml").getInputStream();
            Map<String, Object> config = yaml.load(inputStream);

            @SuppressWarnings("unchecked")
            Map<String, Object> folderMonitor = (Map<String, Object>) config.get("folder-monitor");
            if (folderMonitor == null) return;
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entities = (List<Map<String, Object>>) folderMonitor.get("entities");
            if (entities == null) return;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> integrationConfig = (Map<String, Object>) folderMonitor.get("integration-config");
            
            for (Map<String, Object> entityConfig : entities) {
                String entityType = (String) entityConfig.get("type");
                
                // Buscar configuración de integración para esta entidad
                if (integrationConfig != null && integrationConfig.containsKey(entityType)) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> entityIntegration = (Map<String, String>) integrationConfig.get(entityType);
                    
                    String dtoClassName = entityIntegration.get("dto-class");
                    String serviceBeanName = entityIntegration.get("service-bean");
                    String serviceMethodName = entityIntegration.get("process-method");
                    
                    if (dtoClassName != null && serviceBeanName != null && serviceMethodName != null) {
                        try {
                            // Cargar la clase DTO
                            Class<?> dtoClass = Class.forName(dtoClassName);
                            
                            // Configurar el mapeo para esta entidad
                            EntityTypeConfig typeConfig = new EntityTypeConfig(
                                entityType, dtoClass, serviceBeanName, serviceMethodName);
                            
                            entityConfigs.put(entityType.toLowerCase(), typeConfig);
                            
                            logger.info("Registrada configuración para entidad: {}", entityType);
                        } catch (ClassNotFoundException e) {
                            logger.error("No se pudo cargar la clase DTO '{}' para la entidad '{}'", 
                                dtoClassName, entityType, e);
                        }
                    } else {
                        logger.warn("Configuración incompleta para la entidad: {}", entityType);
                    }
                } else {
                    logger.warn("No se encontró configuración de integración para la entidad: {}", entityType);
                }
            }
        } catch (Exception e) {
            logger.error("Error al cargar configuración de entidades", e);
        }
    }
    
    /**
     * Valida que todos los servicios y métodos configurados existan
     */
    private void validateServiceMethods() {
        for (EntityTypeConfig config : entityConfigs.values()) {
            try {
                // Verificar que el bean de servicio existe
                if (!appContext.containsBean(config.serviceBeanName)) {
                    logger.error("El bean de servicio '{}' para la entidad '{}' no existe", 
                        config.serviceBeanName, config.entityType);
                    continue;
                }
                
                // Obtener el servicio
                Object service = appContext.getBean(config.serviceBeanName);
                
                // Verificar que el método existe con el parámetro correcto
                boolean methodFound = false;
                for (Method method : service.getClass().getMethods()) {
                    if (method.getName().equals(config.serviceMethodName)) {
                        Class<?>[] paramTypes = method.getParameterTypes();
                        if (paramTypes.length == 1 && paramTypes[0].isAssignableFrom(config.dtoClass)) {
                            methodFound = true;
                            break;
                        }
                    }
                }
                
                if (!methodFound) {
                    logger.error("El método '{}' con parámetro '{}' no existe en el servicio '{}'",
                        config.serviceMethodName, config.dtoClass.getSimpleName(), config.serviceBeanName);
                }
                
            } catch (Exception e) {
                logger.error("Error validando configuración para entidad '{}'", config.entityType, e);
            }
        }
    }
    
    /**
     * MEJORA: Implementación mejorada que devuelve ResultadoProcesamiento
     */
    @Override
    public ResultadoProcesamiento processFile(Path filePath) {
        try {
            // Validaciones básicas
            if (filePath == null) {
                return ResultadoProcesamiento.error("Ruta de archivo nula");
            }
            
            if (!Files.exists(filePath)) {
                return ResultadoProcesamiento.error("El archivo no existe: " + filePath);
            }
            
            if (!Files.isReadable(filePath)) {
                return ResultadoProcesamiento.error("El archivo no es legible: " + filePath);
            }
            
            // Determinar el tipo de entidad basado en la ruta del archivo
            String entityType = determineEntityTypeFromPath(filePath);
            
            if (entityType == null) {
                return ResultadoProcesamiento.error("No se pudo determinar el tipo de entidad para: " + filePath);
            }
            
            // Obtener la configuración para esta entidad
            EntityTypeConfig config = entityConfigs.get(entityType.toLowerCase());
            if (config == null) {
                return ResultadoProcesamiento.error("No hay configuración para el tipo de entidad: " + entityType);
            }
            
            logger.info("Procesando archivo como entidad tipo '{}': {}", entityType, filePath);
            
            // Mostrar detalles del archivo para debugging
            if (logger.isDebugEnabled()) {
                try {
                    byte[] fileBytes = Files.readAllBytes(filePath);
                    String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
                    logger.debug("DETALLES DEL ARCHIVO");
                    logger.debug("Ruta: {}", filePath);
                    logger.debug("Tamaño: {} bytes", fileBytes.length);
                    logger.debug("Primeros 200 caracteres:\n{}", 
                        fileContent.substring(0, Math.min(fileContent.length(), 200)));
                } catch (Exception e) {
                    logger.warn("No se pudo leer el contenido del archivo para debug: {}", e.getMessage());
                }
            }
            
            // Parsear el XML al DTO correspondiente
            Object dto;
            try {
                dto = parseXml(filePath, config.dtoClass);
                if (dto == null) {
                    return ResultadoProcesamiento.error("Error al parsear XML a DTO para entidad " + entityType);
                }
            } catch (Exception e) {
                logger.error("Error al parsear XML: {}", e.getMessage(), e);
                return ResultadoProcesamiento.error("Error al parsear XML: " + e.getMessage(), e);
            }
            
            // Procesar el DTO con el servicio correspondiente
            try {
                boolean success = processWithService(config, dto, filePath);
                if (success) {
                    return ResultadoProcesamiento.exito("Procesamiento exitoso para entidad " + entityType);
                } else {
                    return ResultadoProcesamiento.error("Procesamiento fallido para entidad " + entityType);
                }
            } catch (Exception e) {
                logger.error("Error al procesar con servicio: {}", e.getMessage(), e);
                return ResultadoProcesamiento.error("Error al procesar con servicio: " + e.getMessage(), e);
            }
            
        } catch (Exception e) {
            logger.error("Error inesperado al procesar archivo {}: {}", filePath, e.getMessage(), e);
            return ResultadoProcesamiento.error("Error inesperado: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determina el tipo de entidad basado en la ruta del archivo
     */
    private String determineEntityTypeFromPath(Path filePath) {
        String pathStr = filePath.toString().toLowerCase();
        
        // Iterar sobre las entidades configuradas para encontrar la coincidencia
        for (Entity entity : configuredEntities) {
            String folderPath = entity.getInputFolder().toString().toLowerCase();
            
            // Verificar si el archivo está en la carpeta de entrada de esta entidad
            if (pathStr.startsWith(folderPath)) {
                // Si es una GenericEntity, usar su tipo configurado
                if (entity instanceof GenericEntity) {
                    GenericEntity genericEntity = (GenericEntity) entity;
                    String entityType = genericEntity.getEntityType();
                    logger.debug("Entidad determinada por carpeta: {} (tipo: {})", 
                        entity.getName(), entityType);
                    return entityType;
                }
                
                // Para otros tipos de entidades, obtener el tipo del nombre de la clase
                String className = entity.getClass().getSimpleName().toLowerCase();
                if (className.endsWith("entity")) {
                    className = className.substring(0, className.length() - 6);
                }
                
                logger.debug("Entidad determinada por carpeta: {} (clase: {})", 
                    entity.getName(), className);
                return className;
            }
        }
        
        // Extraer el tipo de entidad de la ruta
        String[] pathSegments = pathStr.split("[\\\\/]"); // Split por / o \
        
        for (String segment : pathSegments) {
            // Verificar si este segmento podría ser un tipo de entidad
            if (!segment.isEmpty() && !segment.contains(".") && entityConfigs.containsKey(segment)) {
                logger.debug("Entidad determinada por segmento de ruta: {}", segment);
                return segment;
            }
        }
        
        // Como fallback, buscar por el directorio padre inmediato
        Path parent = filePath.getParent();
        if (parent != null) {
            String dirName = parent.getFileName().toString().toLowerCase();
            if (entityConfigs.containsKey(dirName)) {
                logger.debug("Entidad determinada por directorio padre: {}", dirName);
                return dirName;
            }
            
            // El directorio inmediato podría ser "input", así que verificamos el abuelo
            Path grandParent = parent.getParent();
            if (grandParent != null) {
                String grandParentName = grandParent.getFileName().toString().toLowerCase();
                if (entityConfigs.containsKey(grandParentName)) {
                    logger.debug("Entidad determinada por directorio abuelo: {}", grandParentName);
                    return grandParentName;
                }
            }
        }
        
        logger.warn("No se pudo determinar el tipo de entidad para: {}", filePath);
        return null;
    }
    
    /**
     * Parsea un archivo XML a un objeto DTO
     */
    private <T> T parseXml(Path filePath, Class<T> dtoClass) throws Exception {
        try (InputStream is = Files.newInputStream(filePath)) {
            // Obtener o crear JAXBContext (reutilizando el cache)
            JAXBContext context = jaxbContextCache.computeIfAbsent(dtoClass, 
                clazz -> {
                    try {
                        return JAXBContext.newInstance(clazz);
                    } catch (Exception e) {
                        throw new RuntimeException("Error creando JAXBContext para " + clazz.getName(), e);
                    }
                });
            
            Unmarshaller unmarshaller = context.createUnmarshaller();
            
            @SuppressWarnings("unchecked")
            T result = (T) unmarshaller.unmarshal(is);
            
            return result;
        }
    }
    
    /**
     * Procesa un DTO llamando dinámicamente al servicio correspondiente
     */
    private boolean processWithService(EntityTypeConfig config, Object dto, Path filePath) {
        try {
            // Obtener el servicio desde el contexto
            Object service = appContext.getBean(config.serviceBeanName);
            
            // Buscar el método de procesamiento
            Method processMethod = null;
            for (Method method : service.getClass().getMethods()) {
                if (method.getName().equals(config.serviceMethodName)) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 1 && paramTypes[0].isAssignableFrom(dto.getClass())) {
                        processMethod = method;
                        break;
                    }
                }
            }
            
            if (processMethod == null) {
                logger.error("No se encontró el método '{}' en el servicio '{}'", 
                    config.serviceMethodName, config.serviceBeanName);
                return false;
            }
            
            // Invocar el método
            processMethod.invoke(service, dto);
            
            logger.info("Procesamiento exitoso de entidad tipo '{}': {}", 
                config.entityType, filePath.getFileName());
            return true;
            
        } catch (Exception e) {
            logger.error("Error procesando entidad tipo '{}': {}", 
                config.entityType, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Clase interna para almacenar la configuración de cada tipo de entidad
     */
    private static class EntityTypeConfig {
        final String entityType;
        final Class<?> dtoClass;
        final String serviceBeanName;
        final String serviceMethodName;
        
        EntityTypeConfig(String entityType, Class<?> dtoClass, String serviceBeanName, String serviceMethodName) {
            this.entityType = entityType;
            this.dtoClass = dtoClass;
            this.serviceBeanName = serviceBeanName;
            this.serviceMethodName = serviceMethodName;
        }
    }
}