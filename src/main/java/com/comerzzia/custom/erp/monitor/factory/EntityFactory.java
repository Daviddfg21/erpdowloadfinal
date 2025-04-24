package com.comerzzia.custom.erp.monitor.factory;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.comerzzia.custom.erp.monitor.domain.Entity;
import com.comerzzia.custom.erp.monitor.domain.entities.GenericEntity;

@Component
public class EntityFactory {
    private static final Logger logger = LoggerFactory.getLogger(EntityFactory.class);
    
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Crea una nueva entidad del tipo especificado definido en la configuración YAML.
     */
    public Entity createEntity(String type, String name, String baseFolder, int priority) throws Exception {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de entidad no puede ser nulo o vacío");
        }
        
        String typeKey = type.toLowerCase().trim();
        
        try {
            // Crear instancia de GenericEntity con el tipo correcto
            GenericEntity entity = new GenericEntity(name, baseFolder, priority, typeKey);
            
            // Inyectar dependencias de Spring
            applicationContext.getAutowireCapableBeanFactory().autowireBean(entity);
            
            logger.info("Creada entidad: {}", entity);
            return entity;
        } catch (Exception e) {
            logger.error("Error al crear entidad de tipo '{}': {}", typeKey, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Obtiene los tipos de entidad disponibles basados en la configuración
     */
    public Set<String> getAvailableEntityTypes() {
        Set<String> types = new HashSet<>();
        
        try {
            // Obtener tipos de la configuración
            applicationContext.getBean("configuredEntities");
            
            // También se podrían obtener los tipos desde folder-monitor.yml
            
            logger.info("Tipos de entidad disponibles: {}", types);
        } catch (Exception e) {
            logger.error("Error al obtener los tipos de entidad disponibles", e);
        }
        
        return types;
    }
}