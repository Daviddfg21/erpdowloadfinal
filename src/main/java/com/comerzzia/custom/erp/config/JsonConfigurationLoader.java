package com.comerzzia.custom.erp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cargador de configuración JSON para el monitor de carpetas
 */
@Component
public class JsonConfigurationLoader {

	private static final Logger logger = LoggerFactory.getLogger(JsonConfigurationLoader.class);
	private final ObjectMapper objectMapper;

	public JsonConfigurationLoader() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Carga un archivo JSON y lo mapea a la clase especificada
	 * 
	 * @param <T>
	 *            Tipo de objeto al que se mapeará el JSON
	 * @param configPath
	 *            Ruta al archivo de configuración
	 * @param targetType
	 *            Clase a la que se mapeará el JSON
	 * @return El objeto mapeado, o un Optional vacío si ocurre un error
	 */
	public <T> Optional<T> loadConfiguration(String configPath, Class<T> targetType) {
		try {
			Resource resource = new ClassPathResource(configPath);
			if (!resource.exists()) {
				logger.error("El archivo de configuración no existe: {}", configPath);
				return Optional.empty();
			}

			byte[] jsonData = FileCopyUtils.copyToByteArray(resource.getInputStream());
			return Optional.of(objectMapper.readValue(jsonData, targetType));
		}
		catch (IOException e) {
			logger.error("Error cargando configuración de {}: {}", configPath, e.getMessage(), e);
			return Optional.empty();
		}
	}

	/**
	 * Carga un archivo JSON y devuelve el nodo raíz para navegación manual
	 * 
	 * @param configPath
	 *            Ruta al archivo de configuración
	 * @return El nodo raíz del JSON, o un Optional vacío si ocurre un error
	 */
	public Optional<JsonNode> loadJsonTree(String configPath) {
		try {
			Resource resource = new ClassPathResource(configPath);
			if (!resource.exists()) {
				logger.error("El archivo de configuración no existe: {}", configPath);
				return Optional.empty();
			}

			try (InputStream is = resource.getInputStream()) {
				return Optional.of(objectMapper.readTree(is));
			}
		}
		catch (IOException e) {
			logger.error("Error cargando configuración de {}: {}", configPath, e.getMessage(), e);
			return Optional.empty();
		}
	}

	/**
	 * Obtiene un nodo específico de la configuración JSON
	 * 
	 * @param configPath
	 *            Ruta al archivo de configuración
	 * @param nodePath
	 *            Ruta al nodo (separada por puntos, ej: "folder-monitor.entities")
	 * @return El nodo solicitado, o un Optional vacío si ocurre un error o no existe
	 */
	public Optional<JsonNode> getConfigNode(String configPath, String nodePath) {
		Optional<JsonNode> rootNode = loadJsonTree(configPath);
		if (!rootNode.isPresent()) {
			return Optional.empty();
		}

		JsonNode currentNode = rootNode.get();
		String[] pathSegments = nodePath.split("\\.");

		for (String segment : pathSegments) {
			currentNode = currentNode.get(segment);
			if (currentNode == null) {
				logger.warn("No se encontró el nodo '{}' en la configuración", nodePath);
				return Optional.empty();
			}
		}

		return Optional.of(currentNode);
	}

	/**
	 * Mapea un nodo JSON a una clase específica
	 * 
	 * @param <T>
	 *            Tipo de objeto al que se mapeará el nodo
	 * @param node
	 *            Nodo JSON a mapear
	 * @param targetType
	 *            Clase a la que se mapeará el nodo
	 * @return El objeto mapeado, o un Optional vacío si ocurre un error
	 */
	public <T> Optional<T> convertNodeToObject(JsonNode node, Class<T> targetType) {
		try {
			return Optional.of(objectMapper.treeToValue(node, targetType));
		}
		catch (Exception e) {
			logger.error("Error convirtiendo nodo a {}: {}", targetType.getSimpleName(), e.getMessage(), e);
			return Optional.empty();
		}
	}
}