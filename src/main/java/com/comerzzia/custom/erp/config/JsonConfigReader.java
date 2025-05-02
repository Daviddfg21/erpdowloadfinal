package com.comerzzia.custom.erp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Componente genérico para leer configuración JSON. Proporciona métodos para obtener valores tipados desde cualquier
 * archivo JSON de configuración.
 */
@Component
public class JsonConfigReader {

	private static final Logger logger = LoggerFactory.getLogger(JsonConfigReader.class);
	private final ObjectMapper objectMapper;
	private final ResourceLoader resourceLoader;

	@Autowired
	public JsonConfigReader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Lee un archivo de configuración JSON y devuelve el nodo raíz
	 * 
	 * @param configPath
	 *            Ruta al archivo de configuración
	 * @return Optional con el nodo raíz o vacío si hay error
	 */
	public Optional<JsonNode> readConfig(String configPath) {
		try {
			Resource resource = resourceLoader.getResource("classpath:" + configPath);
			if (!resource.exists()) {
				logger.error("Archivo de configuración no encontrado: {}", configPath);
				return Optional.empty();
			}

			try (InputStream is = resource.getInputStream()) {
				return Optional.of(objectMapper.readTree(is));
			}
		}
		catch (IOException e) {
			logger.error("Error leyendo configuración JSON {}: {}", configPath, e.getMessage(), e);
			return Optional.empty();
		}
	}

	/**
	 * Obtiene un valor específico de la configuración
	 * 
	 * @param configPath
	 *            Ruta al archivo de configuración
	 * @param path
	 *            Ruta de acceso en notación de puntos (ej: "folder-monitor.entities[0].name")
	 * @param defaultValue
	 *            Valor por defecto si no se encuentra
	 * @return El valor encontrado o el valor por defecto
	 */
	public String getString(String configPath, String path, String defaultValue) {
		Optional<JsonNode> node = getNodeAtPath(configPath, path);
		return node.map(JsonNode::asText).orElse(defaultValue);
	}

	/**
	 * Obtiene un valor entero específico de la configuración
	 * 
	 * @param configPath
	 *            Ruta al archivo de configuración
	 * @param path
	 *            Ruta de acceso en notación de puntos
	 * @param defaultValue
	 *            Valor por defecto si no se encuentra
	 * @return El valor encontrado o el valor por defecto
	 */
	public int getInt(String configPath, String path, int defaultValue) {
		Optional<JsonNode> node = getNodeAtPath(configPath, path);
		return node.map(JsonNode::asInt).orElse(defaultValue);
	}

	/**
	 * Obtiene un valor booleano específico de la configuración
	 * 
	 * @param configPath
	 *            Ruta al archivo de configuración
	 * @param path
	 *            Ruta de acceso en notación de puntos
	 * @param defaultValue
	 *            Valor por defecto si no se encuentra
	 * @return El valor encontrado o el valor por defecto
	 */
	public boolean getBoolean(String configPath, String path, boolean defaultValue) {
		Optional<JsonNode> node = getNodeAtPath(configPath, path);
		return node.map(JsonNode::asBoolean).orElse(defaultValue);
	}

	/**
	 * Obtiene una lista de valores desde la configuración
	 * 
	 * @param configPath
	 *            Ruta al archivo de configuración
	 * @param path
	 *            Ruta de acceso en notación de puntos a un array
	 * @return Lista de nodos o lista vacía si no se encuentra
	 */
	public List<JsonNode> getList(String configPath, String path) {
		Optional<JsonNode> node = getNodeAtPath(configPath, path);
		List<JsonNode> result = new ArrayList<>();

		if (node.isPresent() && node.get().isArray()) {
			node.get().forEach(result::add);
		}

		return result;
	}

	/**
	 * Obtiene un mapa de valores desde la configuración
	 * 
	 * @param configPath
	 *            Ruta al archivo de configuración
	 * @param path
	 *            Ruta de acceso en notación de puntos a un objeto
	 * @return Mapa de clave-valor o mapa vacío si no se encuentra
	 */
	public Map<String, JsonNode> getMap(String configPath, String path) {
		Optional<JsonNode> node = getNodeAtPath(configPath, path);
		Map<String, JsonNode> result = new HashMap<>();

		if (node.isPresent() && node.get().isObject()) {
			ObjectNode objNode = (ObjectNode) node.get();
			Iterator<Map.Entry<String, JsonNode>> fields = objNode.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = fields.next();
				result.put(entry.getKey(), entry.getValue());
			}
		}

		return result;
	}

	/**
	 * Obtiene un nodo específico de la configuración
	 * 
	 * @param configPath
	 *            Ruta al archivo de configuración
	 * @param path
	 *            Ruta de acceso en notación de puntos
	 * @return Optional con el nodo o vacío si no se encuentra
	 */
	private Optional<JsonNode> getNodeAtPath(String configPath, String path) {
		Optional<JsonNode> rootNode = readConfig(configPath);
		if (!rootNode.isPresent()) {
			return Optional.empty();
		}

		JsonNode current = rootNode.get();
		String[] segments = path.split("\\.");

		for (String segment : segments) {
			if (segment.contains("[") && segment.endsWith("]")) {
				// Manejar acceso a arrays
				String arrayName = segment.substring(0, segment.indexOf("["));
				int index = Integer.parseInt(segment.substring(segment.indexOf("[") + 1, segment.indexOf("]")));

				current = current.get(arrayName);
				if (current == null || !current.isArray() || index >= current.size()) {
					return Optional.empty();
				}
				current = current.get(index);
			}
			else {
				// Acceso normal a propiedad
				current = current.get(segment);
			}

			if (current == null) {
				return Optional.empty();
			}
		}

		return Optional.of(current);
	}

	/**
	 * Convierte un nodo JSON a un objeto de clase específica
	 * 
	 * @param <T>
	 *            Tipo de objeto a devolver
	 * @param node
	 *            Nodo JSON a convertir
	 * @param valueType
	 *            Clase del objeto a devolver
	 * @return El objeto convertido
	 * @throws IOException
	 *             Si hay error en la conversión
	 */
	public <T> T convertValue(JsonNode node, Class<T> valueType) throws IOException {
		return objectMapper.treeToValue(node, valueType);
	}
}