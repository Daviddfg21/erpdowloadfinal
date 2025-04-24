package com.comerzzia.custom.erp.monitor.domain;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comerzzia.custom.erp.monitor.config.EntityConfig;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(Entity.class);
    
    private String name;
    private String baseFolder;
    private int priority;
    private EntityConfig.EntityDependencies entityDependencies;

    public Entity() {
        // Constructor por defecto
    }

    public Entity(String name, String baseFolder, int priority) {
        this.name = name;
        this.baseFolder = baseFolder;
        this.priority = priority;
        
        // Crear directorios al crear la entidad
        initializeFolders();
    }

    /**
     * Inicializa las carpetas necesarias para la entidad
     */
    private void initializeFolders() {
        try {
            // Crear carpeta base
            Path basePath = Paths.get(baseFolder);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                logger.info("Creada carpeta base: {}", basePath);
            }

            // Crear subcarpetas
            Path inputPath = getInputFolder();
            Path processedPath = getProcessedFolder();
            Path failedPath = getFailedFolder();

            // Crear subcarpetas si no existen
            if (!Files.exists(inputPath)) {
                Files.createDirectories(inputPath);
                logger.info("Creada carpeta de entrada: {}", inputPath);
            }
            
            if (!Files.exists(processedPath)) {
                Files.createDirectories(processedPath);
                logger.info("Creada carpeta de procesados: {}", processedPath);
            }
            
            if (!Files.exists(failedPath)) {
                Files.createDirectories(failedPath);
                logger.info("Creada carpeta de fallidos: {}", failedPath);
            }
        } catch (Exception e) {
            logger.error("Error al crear directorios para la entidad {}: {}", 
                         name, e.getMessage(), e);
        }
    }
    
    /**
     * MEJORA: Método para verificar y recrear directorios si no existen
     */
    public void verifyDirectories() {
        try {
            Path basePath = Paths.get(baseFolder);
            Path inputPath = getInputFolder();
            Path processedPath = getProcessedFolder();
            Path failedPath = getFailedFolder();
            
            // Verificar y recrear directorios si no existen
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                logger.info("Se ha recreado directorio base: {}", basePath);
            }
            
            if (!Files.exists(inputPath)) {
                Files.createDirectories(inputPath);
                logger.info("Se ha recreado directorio de entrada: {}", inputPath);
            }
            
            if (!Files.exists(processedPath)) {
                Files.createDirectories(processedPath);
                logger.info("Se ha recreado directorio de procesados: {}", processedPath);
            }
            
            if (!Files.exists(failedPath)) {
                Files.createDirectories(failedPath);
                logger.info("Se ha recreado directorio de fallidos: {}", failedPath);
            }
        } catch (Exception e) {
            logger.error("Error al recrear directorios para la entidad {}: {}", 
                         name, e.getMessage(), e);
        }
    }

    /**
     * Obtiene la carpeta de entrada para archivos
     */
    public Path getInputFolder() {
        return Paths.get(baseFolder, "input");
    }

    /**
     * Obtiene la carpeta de archivos procesados
     */
    public Path getProcessedFolder() {
        return Paths.get(baseFolder, "processed");
    }

    /**
     * Obtiene la carpeta de archivos fallidos
     */
    public Path getFailedFolder() {
        return Paths.get(baseFolder, "failed");
    }

    // Método para verificar si puede procesar concurrentemente
    public boolean canProcessConcurrently(Entity otherEntity) {
        if (entityDependencies == null) return true;

        String otherEntityName = otherEntity.getName().toLowerCase();
        
        // Verificar bloqueos
        if (entityDependencies.getBlock() != null && 
            entityDependencies.getBlock().contains(otherEntityName)) {
            return false;
        }

        if (entityDependencies.getBlockedBy() != null && 
            entityDependencies.getBlockedBy().contains(otherEntityName)) {
            return false;
        }

        return true;
    }

    // Método para resolver precedencia en caso de conflicto
    public boolean shouldYield(Entity otherEntity) {
        if (entityDependencies == null) return false;

        switch (entityDependencies.getPrecedence()) {
            case "always":
                return true;
            case "first":
                return this.getPriority() < otherEntity.getPriority();
            case "last":
                return this.getPriority() > otherEntity.getPriority();
            default:
                return false;
        }
    }

    // Getters y Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseFolder() {
        return baseFolder;
    }

    public void setBaseFolder(String baseFolder) {
        this.baseFolder = baseFolder;
        // Reinicializar carpetas si se cambia la carpeta base
        initializeFolders();
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setEntityDependencies(EntityConfig.EntityDependencies dependencies) {
        this.entityDependencies = dependencies;
    }

    public EntityConfig.EntityDependencies getEntityDependencies() {
        return this.entityDependencies;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "name='" + name + '\'' +
                ", baseFolder='" + baseFolder + '\'' +
                ", priority=" + priority +
                '}';
    }
}