package com.comerzzia.custom.erp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuración que carga las propiedades desde un archivo JSON. Esta clase utiliza el JsonPropertySourceFactory para
 * cargar application.json en lugar del tradicional application.yml o application.properties
 */
@Configuration
@PropertySource(value = "classpath:application.json", factory = JsonPropertySourceFactory.class)
public class JsonPropertyConfig {
	// No necesitamos contenido adicional
}