package com.comerzzia.custom.erp.monitor.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import com.comerzzia.custom.erp.monitor.domain.Entity;
import com.comerzzia.custom.erp.monitor.factory.EntityFactory;

@Configuration
public class EntityConfig {
    private static final Logger logger = LoggerFactory.getLogger(EntityConfig.class);

    @Autowired
    private EntityFactory entityFactory;

    // Clase para manejar dependencias de entidades
    public static class EntityDependencies {
        private List<String> block = new ArrayList<>();
        private List<String> blockedBy = new ArrayList<>();
        private String precedence = "first"; // valores: always, first, last

        // Getters y setters
        public List<String> getBlock() { return block; }
        public void setBlock(List<String> block) { this.block = block; }
        public List<String> getBlockedBy() { return blockedBy; }
        public void setBlockedBy(List<String> blockedBy) { this.blockedBy = blockedBy; }
        public String getPrecedence() { return precedence; }
        public void setPrecedence(String precedence) { this.precedence = precedence; }
    }

    @Bean
    public List<Entity> configuredEntities() {
        List<Entity> entities = new ArrayList<>();

        try {
            // Cargar configuración YAML
        	Yaml yaml = new Yaml();
            InputStream inputStream = new ClassPathResource("config/folder-monitor.yml").getInputStream();
            Map<String, Object> config = yaml.load(inputStream);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entityConfigs = 
                (List<Map<String, Object>>) ((Map<String, Object>) config.get("folder-monitor")).get("entities");

            // Procesar cada configuración de entidad
            for (Map<String, Object> entityConfig : entityConfigs) {
                String type = (String) entityConfig.get("type");
                String name = (String) entityConfig.get("name");
                String folder = (String) entityConfig.get("folder");
                Integer priority = (Integer) entityConfig.getOrDefault("priority", 100);

                // Procesar dependencias
                EntityDependencies dependencies = new EntityDependencies();
                if (entityConfig.containsKey("dependencies")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dependencyConfig = 
                        (Map<String, Object>) entityConfig.get("dependencies");
                    
                    if (dependencyConfig.containsKey("block")) {
                        dependencies.setBlock((List<String>) dependencyConfig.get("block"));
                    }
                    if (dependencyConfig.containsKey("blocked-by")) {
                        dependencies.setBlockedBy((List<String>) dependencyConfig.get("blocked-by"));
                    }
                    if (dependencyConfig.containsKey("precedence")) {
                        dependencies.setPrecedence((String) dependencyConfig.get("precedence"));
                    }
                }

                // Crear entidad
                Entity entity = entityFactory.createEntity(type, name, folder, priority);
                
                // Establecer dependencias
                entity.setEntityDependencies(dependencies);

                entities.add(entity);
            }

            return entities;
        } catch (Exception e) {
            logger.error("Error al cargar configuración de entidades", e);
            return createDefaultEntities();
        }
    }

    // Método para crear entidades por defecto (como en la implementación original)
    private List<Entity> createDefaultEntities() {
        List<Entity> defaultEntities = new ArrayList<>();
        
        try {
            defaultEntities.add(entityFactory.createEntity("items", "Productos", 
                "C:/comerzzia/monitor/items", 200));

            defaultEntities.add(entityFactory.createEntity("promotions", "Promociones", 
                "C:/comerzzia/monitor/promotions", 100));

            defaultEntities.add(entityFactory.createEntity("warehouse", "Almacén", 
                "C:/comerzzia/monitor/warehouse", 150));
        } catch (Exception e) {
            logger.error("Error al crear entidades por defecto", e);
        }

        return defaultEntities;
    }
}