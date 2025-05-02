package com.comerzzia.custom.erp.config;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.util.FileCopyUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Factory para crear PropertySource desde archivos JSON Permite usar archivos JSON como fuentes de propiedades en
 * Spring con la anotación @PropertySource
 */
public class JsonPropertySourceFactory implements PropertySourceFactory {

	private static final Logger logger = LoggerFactory.getLogger(JsonPropertySourceFactory.class);
	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
		String sourceName = name != null ? name : resource.getResource().getFilename();
		Map<String, Object> properties = extractProperties(resource.getResource());
		return new JsonPropertySource(sourceName, properties);
	}

	/**
	 * Extrae las propiedades del archivo JSON
	 */
	private Map<String, Object> extractProperties(Resource resource) {
		if (resource == null) {
			return Collections.emptyMap();
		}

		try {
			String jsonContent = new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
			ObjectNode root = mapper.readValue(jsonContent, ObjectNode.class);
			return convertJsonToFlatMap(root, null);
		}
		catch (IOException e) {
			logger.error("Error al leer el archivo JSON: {}", e.getMessage(), e);
			return Collections.emptyMap();
		}
	}

	/**
	 * Convierte un JSON anidado en un mapa plano con claves separadas por puntos
	 */
	private Map<String, Object> convertJsonToFlatMap(ObjectNode node, String prefix) {
		Map<String, Object> properties = new HashMap<>();

		node.fields().forEachRemaining(entry -> {
			String key = prefix != null ? prefix + "." + entry.getKey() : entry.getKey();

			if (entry.getValue().isObject()) {
				properties.putAll(convertJsonToFlatMap((ObjectNode) entry.getValue(), key));
			}
			else if (entry.getValue().isArray()) {
				// Para arrays, simplemente guardamos el valor JSON completo
				properties.put(key, entry.getValue().toString());
			}
			else {
				// Para valores primitivos, extraemos el valor real
				if (entry.getValue().isTextual()) {
					properties.put(key, entry.getValue().textValue());
				}
				else if (entry.getValue().isNumber()) {
					properties.put(key, entry.getValue().numberValue());
				}
				else if (entry.getValue().isBoolean()) {
					properties.put(key, entry.getValue().booleanValue());
				}
				else {
					properties.put(key, entry.getValue().asText());
				}
			}
		});

		return properties;
	}

	/**
	 * PropertySource personalizado basado en JSON
	 */
	private static class JsonPropertySource extends PropertySource<Map<String, Object>> {

		private final Map<String, Object> properties;

		JsonPropertySource(String name, Map<String, Object> properties) {
			super(name, properties);
			this.properties = properties;
		}

		@Override
		public Object getProperty(String name) {
			return properties.get(name);
		}
	}
}