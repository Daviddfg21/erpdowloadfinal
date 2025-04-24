package com.comerzzia.custom.erp.monitor.config;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.comerzzia.custom.erp.monitor.domain.Entity;
import com.comerzzia.custom.erp.monitor.service.FileManagementService;
import com.comerzzia.custom.erp.monitor.service.FileProcessorService;
import com.comerzzia.custom.erp.monitor.service.FileProcessorService.ResultadoProcesamiento;

@Configuration
@EnableScheduling
public class FileMonitorConfig {
    private static final Logger logger = LoggerFactory.getLogger(FileMonitorConfig.class);

    private static final long FILE_PROCESSING_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutos

    @Value("${file.monitor.max-files-per-batch:10}")
    private int maxFilesPerBatch;

    @Autowired
    private FileProcessorService fileProcessorService;

    @Autowired
    private FileManagementService fileManagementService;

    @Autowired
    private List<Entity> configuredEntities;
    
    // Caché de dependencias precalculada - Optimización
    private Map<String, Set<String>> entityDependencyCache;
    private Map<String, Entity> entitiesByType;

    // Executor para procesamiento concurrente
    private final ExecutorService executorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );

    // Mapa para registrar qué archivos están siendo procesados actualmente
    private final ConcurrentHashMap<String, Long> filesInProcess = new ConcurrentHashMap<>();
    
    // Mapa para registrar qué tipos de entidades están siendo procesados actualmente
    private final ConcurrentHashMap<String, Integer> entitiesInProcess = new ConcurrentHashMap<>();
    
    // Cola de prioridad para archivos
    private final PriorityBlockingQueue<PrioritizedFile> fileQueue = 
        new PriorityBlockingQueue<>(100, 
            Comparator.comparing((PrioritizedFile pf) -> pf.entity.getPriority()).reversed());

    // Clase para archivos con prioridad
    private static class PrioritizedFile {
        final Path filePath;
        final Entity entity;

        PrioritizedFile(Path filePath, Entity entity) {
            this.filePath = filePath;
            this.entity = entity;
        }
    }
    
    @PostConstruct
    public void initialize() {
        // Precalcular y cachear las dependencias
        entityDependencyCache = buildDependencyCache();
        
        // Construir mapa de entidades por tipo
        entitiesByType = buildEntitiesByTypeMap();
        
        // Mostrar información de inicialización
        logger.info("Monitor de archivos inicializado con {} entidades configuradas", configuredEntities.size());
        List<Entity> sortedEntities = getSortedEntities();
        for (Entity entity : sortedEntities) {
            logger.info("Entidad configurada: {} (prioridad: {})", entity.getName(), entity.getPriority());
        }
        
        // Loguear las dependencias configuradas
        logDependencies();
    }

    /**
     * Método para loguear las dependencias entre entidades
     */
    private void logDependencies() {
        logger.info("Dependencias configuradas:");
        for (Entity entity : configuredEntities) {
            String entityType = getEntityType(entity);
            if (entityType != null) {
                Set<String> blockedEntities = entityDependencyCache.getOrDefault(entityType, Collections.emptySet());
                if (!blockedEntities.isEmpty()) {
                    logger.info("  {} bloquea a: {}", entityType, String.join(", ", blockedEntities));
                }
            }
        }
    }
    
    /**
     * Construye un mapa de entidades por su tipo para búsqueda rápida
     */
    private Map<String, Entity> buildEntitiesByTypeMap() {
        Map<String, Entity> map = new HashMap<>();
        for (Entity entity : configuredEntities) {
            String entityType = getEntityType(entity);
            if (entityType != null) {
                map.put(entityType.toLowerCase(), entity);
            }
        }
        return map;
    }

    /**
     * Obtiene el tipo de una entidad
     */
    private String getEntityType(Entity entity) {
        // Este método debe extraer el tipo de entidad
        return entity.getName().toLowerCase();
    }

    /**
     * Construye un caché de dependencias para evitar recálculos
     * Optimización: Crear un mapa más eficiente que indique qué entidades son bloqueadas por cada entidad
     */
    private Map<String, Set<String>> buildDependencyCache() {
        Map<String, Set<String>> cache = new HashMap<>();
        
        for (Entity entity : configuredEntities) {
            String entityType = getEntityType(entity);
            
            // Entidades que esta entidad bloquea
            Set<String> blocks = configuredEntities.stream()
                .filter(other -> !entity.canProcessConcurrently(other))
                .map(this::getEntityType)
                .collect(Collectors.toSet());
                
            cache.put(entityType, blocks);
        }
        
        return cache;
    }
    
    /**
     * Obtiene las entidades ordenadas por prioridad (mayor a menor)
     */
    private List<Entity> getSortedEntities() {
        List<Entity> sortedEntities = new ArrayList<>(configuredEntities);
        sortedEntities.sort(Comparator.comparing(Entity::getPriority).reversed());
        return sortedEntities;
    }

    @Scheduled(fixedDelayString = "${file.monitor.scan-interval:10000}")
    public void scanAndQueueFiles() {
        List<Entity> sortedEntities = getSortedEntities();

        for (Entity entity : sortedEntities) {
            File[] eligibleFiles = getEligibleFiles(entity);
            if (eligibleFiles == null || eligibleFiles.length == 0) continue;

            // Verificar si la entidad puede procesar archivos basado en dependencias
            if (canEntityProcessFiles(entity)) {
                queueFiles(entity, eligibleFiles);
            } else {
                logBlockedFiles(entity, eligibleFiles);
            }
        }

        // Procesar archivos en cola
        processQueuedFiles();
    }
    
    /**
     * Obtiene los archivos disponibles para una entidad
     */
    private File[] getEligibleFiles(Entity entity) {
        File inputFolder = entity.getInputFolder().toFile();
        if (!inputFolder.exists() || !inputFolder.isDirectory()) return null;
        
        return inputFolder.listFiles(file -> !file.isDirectory());
    }
    
    /**
     * Verifica si una entidad puede procesar archivos basado en sus dependencias
     * Optimización: Utilizar el caché de dependencias precalculado
     */
    protected boolean canEntityProcessFiles(Entity currentEntity) {
        String entityType = getEntityType(currentEntity);
        int priority = currentEntity.getPriority();

        if (isBlockedByEntitiesInProcess(entityType)) {
            return false;
        }

        if (isBlockedByPendingHigherPriority(entityType, priority)) {
            return false;
        }

        return true;
    }

    
    /**
     * Verifica si alguna entidad actualmente en proceso bloquea a la entidad dada.
     */
    private boolean isBlockedByEntitiesInProcess(String entityType) {
        for (String processingType : entitiesInProcess.keySet()) {
            Entity processingEntity = entitiesByType.get(processingType);
            if (processingEntity != null) {
                Set<String> blockedByProcessing = entityDependencyCache.getOrDefault(processingType, Collections.emptySet());
                if (blockedByProcessing.contains(entityType)) {
                    logger.debug("Entidad {} está bloqueada por {} (en procesamiento)", entityType, processingType);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifica si alguna entidad con archivos pendientes bloquea a la entidad dada.
     */
    private boolean isBlockedByPendingHigherPriority(String entityType, int priority) {
        for (Entity otherEntity : configuredEntities) {
            String otherType = getEntityType(otherEntity);
            if (otherType.equals(entityType)) continue;

            Set<String> blockedByOther = entityDependencyCache.getOrDefault(otherType, Collections.emptySet());

            if (blockedByOther.contains(entityType)
                && otherEntity.getPriority() > priority
                && hasFilesToProcess(otherEntity)) {

                logger.debug("Entidad {} está bloqueada por archivos pendientes de {}", entityType, otherType);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Encola archivos para procesamiento
     */
    private void queueFiles(Entity entity, File[] files) {
        for (File file : files) {
            String filePath = file.getAbsolutePath();
            
            // Si el archivo está siendo procesado, lo saltamos
            if (filesInProcess.containsKey(filePath)) {
                continue;
            }
            
            // Marcar como en procesamiento y encolar
            filesInProcess.put(filePath, System.currentTimeMillis());
            fileQueue.offer(new PrioritizedFile(file.toPath(), entity));
            logger.info("Archivo encolado para procesamiento: {} - Entidad: {}", 
                       file.getName(), entity.getName());
        }
    }
    
    /**
     * Registra archivos bloqueados en el log
     * Optimización: Usar el caché de dependencias para determinar las entidades bloqueantes
     */
    private void logBlockedFiles(Entity entity, File[] files) {
        String entityType = getEntityType(entity);
        
        // Determinar qué entidades están bloqueando a esta
        List<String> blockingEntities = new ArrayList<>();
        
        // Entidades en procesamiento
        for (String processingType : entitiesInProcess.keySet()) {
            Entity processingEntity = entitiesByType.get(processingType);
            if (processingEntity != null) {
                Set<String> blockedByProcessing = entityDependencyCache.getOrDefault(processingType, Collections.emptySet());
                if (blockedByProcessing.contains(entityType)) {
                    blockingEntities.add(processingType + " (en procesamiento)");
                }
            }
        }
        
        // Entidades con archivos pendientes
        for (Entity otherEntity : configuredEntities) {
            if (otherEntity.equals(entity)) continue;
            
            String otherType = getEntityType(otherEntity);
            Set<String> blockedByOther = entityDependencyCache.getOrDefault(otherType, Collections.emptySet());
            
            if (blockedByOther.contains(entityType) && 
                otherEntity.getPriority() > entity.getPriority() && 
                hasFilesToProcess(otherEntity)) {
                blockingEntities.add(otherType + " (pendiente)");
            }
        }
        
        String blockingEntitiesStr = String.join(", ", blockingEntities);
        
        for (File file : files) {
            logger.warn("ARCHIVO BLOQUEADO: {} - Entidad: {} - Bloqueado por: {}", 
                file.getName(), entityType, blockingEntitiesStr);
        }
    }

    // Método auxiliar para verificar si una entidad tiene archivos pendientes
    private boolean hasFilesToProcess(Entity entity) {
        File[] files = getEligibleFiles(entity);
        return files != null && files.length > 0;
    }

    /**
     * Procesa los archivos en la cola respetando las prioridades y dependencias
     * Se mantiene la implementación secuencial, pero se optimiza la verificación de dependencias
     */
    private void processQueuedFiles() {
        // Extraer todos los archivos de la cola manteniendo el orden de prioridad
        List<PrioritizedFile> allFiles = drainQueueWithPriority();
        if (allFiles.isEmpty()) {
            return;
        }
        
        // Agrupar por entidad para procesamiento
        Map<Entity, List<Path>> filesByEntity = groupFilesByEntity(allFiles);
        
        // Ordenar entidades por prioridad (mayor a menor)
        List<Map.Entry<Entity, List<Path>>> sortedEntities = new ArrayList<>(filesByEntity.entrySet());
        sortedEntities.sort(Comparator.<Map.Entry<Entity, List<Path>>>comparingInt(
            entry -> entry.getKey().getPriority()).reversed());
        
        // Log para depuración del orden de procesamiento
        logProcessingOrder(filesByEntity);
        
        // Procesar entidades secuencialmente respetando dependencias
        for (Map.Entry<Entity, List<Path>> entry : sortedEntities) {
            Entity entity = entry.getKey();
            List<Path> files = entry.getValue();
            
            // Verificar si esta entidad puede procesar ahora
            String entityType = getEntityType(entity);
            boolean canProcess = true;
            
            // Verificar si hay entidades en procesamiento que bloquean a esta
            for (String processingType : entitiesInProcess.keySet()) {
                Entity processingEntity = entitiesByType.get(processingType);
                if (processingEntity != null) {
                    Set<String> blockedByProcessing = entityDependencyCache.getOrDefault(processingType, Collections.emptySet());
                    if (blockedByProcessing.contains(entityType)) {
                        logger.info("Entidad {} debe esperar porque {} está en procesamiento", 
                                  entityType, processingType);
                        canProcess = false;
                        break;
                    }
                }
            }
            
            if (!canProcess) {
                // Devolver archivos a la cola para próximo intento
                for (Path file : files) {
                    fileQueue.offer(new PrioritizedFile(file, entity));
                }
                continue;
            }
            
            // Marcar esta entidad como en procesamiento
            entitiesInProcess.put(entityType, files.size());
            
            try {
                // Procesar todos los archivos de esta entidad
                processEntityFilesSequentially(entity, files);
            } finally {
                // Eliminar esta entidad de las que están en procesamiento
                entitiesInProcess.remove(entityType);
            }
        }
    }
    
    /**
     * Procesa los archivos de una entidad secuencialmente, esperando a que todos terminen
     */
    private void processEntityFilesSequentially(Entity entity, List<Path> files) {
        CountDownLatch latch = new CountDownLatch(files.size());
        
        for (Path filePath : files) {
            CompletableFuture.runAsync(() -> {
                try {
                    // Utiliza el fileProcessorService para procesar el archivo
                    ResultadoProcesamiento resultado = fileProcessorService.processFile(filePath);
                    
                    try {
                        fileManagementService.moveProcessedFile(entity, filePath, resultado.isExitoso());
                        
                        if (resultado.isExitoso()) {
                            logger.info("Archivo procesado correctamente: {} - {}", 
                                       filePath, resultado.getMensaje());
                        } else {
                            logger.error("Error procesando archivo: {} - {}", 
                                       filePath, resultado.getMensaje());
                        }
                    } catch (Exception moveEx) {
                        logger.error("No se pudo mover el archivo: {}. Error: {}", 
                                   filePath, moveEx.getMessage());
                    }
                } catch (Exception e) {
                    logger.error("Error procesando archivo: {}", e.getMessage(), e);
                } finally {
                    filesInProcess.remove(filePath.toString());
                    latch.countDown();
                }
            }, executorService);
        }
        
        try {
            // Esperar a que todos los archivos de esta entidad terminen
            if (!latch.await(30, TimeUnit.MINUTES)) {
                logger.warn("Timeout esperando que terminen los archivos de la entidad {}", entity.getName());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupción esperando archivos de entidad {}", entity.getName());
        }
    }
    
    /**
     * Extrae elementos de la cola manteniendo el orden de prioridad
     */
    private List<PrioritizedFile> drainQueueWithPriority() {
        List<PrioritizedFile> filesToProcess = new ArrayList<>();
        PrioritizedFile pf;
        while ((pf = fileQueue.poll()) != null) {
            filesToProcess.add(pf);
        }
        return filesToProcess;
    }
    
    /**
     * Agrupa archivos por entidad manteniendo el orden de prioridad
     */
    private Map<Entity, List<Path>> groupFilesByEntity(List<PrioritizedFile> files) {
        Map<Entity, List<Path>> filesByEntity = new LinkedHashMap<>();
        
        for (PrioritizedFile pf : files) {
            Entity entity = pf.entity;
            if (!filesByEntity.containsKey(entity)) {
                filesByEntity.put(entity, new ArrayList<>());
            }
            filesByEntity.get(entity).add(pf.filePath);
        }
        
        return filesByEntity;
    }
    
    /**
     * Registra en el log el orden de procesamiento
     */
    private void logProcessingOrder(Map<Entity, List<Path>> filesByEntity) {
        logger.info("Orden de procesamiento por entidad:");
        
        // Lista para ordenar entidades por prioridad para mejor visualización
        List<Map.Entry<Entity, List<Path>>> sortedEntities = new ArrayList<>(filesByEntity.entrySet());
        sortedEntities.sort(Comparator.<Map.Entry<Entity, List<Path>>>comparingInt(
            entry -> entry.getKey().getPriority()).reversed());
        
        for (Map.Entry<Entity, List<Path>> entry : sortedEntities) {
            Entity entity = entry.getKey();
            List<Path> paths = entry.getValue();
            String entityType = getEntityType(entity);
            
            // Determinar si esta entidad puede procesar ahora
            boolean canProcess = true;
            List<String> blockingReasons = new ArrayList<>();
            
            // Verificar entidades en procesamiento
            for (String processingType : entitiesInProcess.keySet()) {
                Entity processingEntity = entitiesByType.get(processingType);
                if (processingEntity != null) {
                    Set<String> blockedByProcessing = entityDependencyCache.getOrDefault(processingType, Collections.emptySet());
                    if (blockedByProcessing.contains(entityType)) {
                        canProcess = false;
                        blockingReasons.add(processingType + " (en procesamiento)");
                    }
                }
            }
            
            if (canProcess) {
                logger.info("  {} (prioridad: {}): {} archivos - Procesando", 
                          entityType, entity.getPriority(), paths.size());
            } else {
                logger.info("  {} (prioridad: {}): {} archivos - Bloqueado por: {}", 
                          entityType, entity.getPriority(), paths.size(), 
                          String.join(", ", blockingReasons));
            }
        }
    }
    
    /**
     * Limpia archivos que podrían estar bloqueados en procesamiento
     */
    @Scheduled(fixedDelay = 60000) // Ejecutar cada minuto
    public void cleanupStalledProcessing() {
        long currentTime = System.currentTimeMillis();
        
        // Eliminar entradas antiguas que podrían estar bloqueadas
        filesInProcess.entrySet().removeIf(entry -> {
            long elapsedTime = currentTime - entry.getValue();
            if (elapsedTime > FILE_PROCESSING_TIMEOUT_MS) {
                logger.warn("Eliminando archivo bloqueado del registro: {} (tiempo: {} ms)", 
                           entry.getKey(), elapsedTime);
                return true;
            }
            return false;
        });
    }
    
    /**
     * Verifica que todos los directorios monitoreados existan
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    public void verifyAllDirectories() {
        logger.debug("Verificando que todos los directorios existan");
        configuredEntities.forEach(Entity::verifyDirectories);
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Apagando el monitor de archivos...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                logger.warn("El executor no terminó normalmente, forzando cierre");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Interrupción durante el apagado del executor", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Monitor de archivos apagado correctamente");
    }
}