package com.comerzzia.custom.erp.monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.comerzzia.custom.erp.monitor.domain.Entity;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class FileManagementService {
    private static final Logger logger = LoggerFactory.getLogger(FileManagementService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Mueve un archivo procesado a la carpeta correspondiente
     * MEJORA: Verificación de espacio en disco y timeout al mover archivos
     * 
     * @param entity     Entidad que procesa el archivo
     * @param filePath   Ruta del archivo original
     * @param processed  Indica si el procesamiento fue exitoso
     * @return           Ruta del archivo después de moverlo
     */
    public Path moveProcessedFile(Entity entity, Path filePath, boolean processed) {
        try {
            // Si el archivo ya no existe, solo registrarlo
            if (!Files.exists(filePath)) {
                logger.warn("El archivo ya no existe en su ubicación original: {}", filePath);
                return null;
            }

            // Determinar carpeta de destino
            Path destinationFolder = processed ? 
                entity.getProcessedFolder() : 
                entity.getFailedFolder();

            // Asegurar que los directorios existen
            Files.createDirectories(destinationFolder);
            
            // MEJORA: Verificar si hay espacio en disco
            if (!hasEnoughDiskSpace(destinationFolder)) {
                logger.error("No hay suficiente espacio en disco en la carpeta: {}", destinationFolder);
                return null;
            }

            // Generar nombre de archivo único
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String originalFileName = filePath.getFileName().toString();
            String uniqueFileName = timestamp + "_" + originalFileName;

            // Construir ruta de destino
            Path destinationPath = destinationFolder.resolve(uniqueFileName);

            // MEJORA: Intentar mover el archivo con timeout
            try {
                // Mover con timeout
                CompletableFuture<Path> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return Files.move(filePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                });
                
                Path movedFilePath = future.get(30, TimeUnit.SECONDS);
                logger.info("Archivo {} movido a {} (Procesamiento: {})", 
                        originalFileName, movedFilePath, processed ? "ÉXITO" : "FALLO");
                    
                return movedFilePath;
            } catch (TimeoutException e) {
                logger.error("Timeout al intentar mover el archivo {}", filePath);
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Proceso interrumpido al mover archivo {}", filePath);
                return null;
            } catch (ExecutionException e) {
                logger.error("Error al mover archivo {}: {}", filePath, e.getCause().getMessage());
                return null;
            }
        } catch (IOException e) {
            logger.error("Error al mover archivo {}: {}", filePath, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * MEJORA: Verifica si hay suficiente espacio en disco disponible
     */
    private boolean hasEnoughDiskSpace(Path folder) {
        try {
            FileStore store = Files.getFileStore(folder);
            long availableBytes = store.getUsableSpace();
            // Verificar que hay al menos 50MB disponibles
            return availableBytes > 50 * 1024 * 1024;
        } catch (IOException e) {
            logger.error("Error al verificar espacio en disco: {}", e.getMessage());
            return false;
        }
    }
}