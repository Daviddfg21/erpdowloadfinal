package com.comerzzia.custom.erp.monitor.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import com.comerzzia.custom.erp.monitor.domain.Entity;
import com.comerzzia.custom.erp.monitor.factory.EntityFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

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
		public List<String> getBlock() {
			return block;
		}

		public void setBlock(List<String> block) {
			this.block = block;
		}

		public List<String> getBlockedBy() {
			return blockedBy;
		}

		public void setBlockedBy(List<String> blockedBy) {
			this.blockedBy = blockedBy;
		}

		public String getPrecedence() {
			return precedence;
		}

		public void setPrecedence(String precedence) {
			this.precedence = precedence;
		}
	}

	/**
	 * Clase para mapear la estructura del JSON de configuración
	 */
	public static class FolderMonitorConfig {

		private List<EntityConfig> entities;
		private Map<String, IntegrationConfig> integrationConfig;
		private GlobalSettings globalSettings;

		// Getters y setters
		public List<EntityConfig> getEntities() {
			return entities;
		}

		public void setEntities(List<EntityConfig> entities) {
			this.entities = entities;
		}

		public Map<String, IntegrationConfig> getIntegrationConfig() {
			return integrationConfig;
		}

		public void setIntegrationConfig(Map<String, IntegrationConfig> integrationConfig) {
			this.integrationConfig = integrationConfig;
		}

		public GlobalSettings getGlobalSettings() {
			return globalSettings;
		}

		public void setGlobalSettings(GlobalSettings globalSettings) {
			this.globalSettings = globalSettings;
		}
	}

	/**
	 * Clase para mapear la configuración de una entidad
	 */
	public static class EntityConfig {

		private String type;
		private String name;
		private String folder;
		private Integer priority = 100;
		private EntityDependencies dependencies;

		// Getters y setters
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getFolder() {
			return folder;
		}

		public void setFolder(String folder) {
			this.folder = folder;
		}

		public Integer getPriority() {
			return priority;
		}

		public void setPriority(Integer priority) {
			this.priority = priority;
		}

		public EntityDependencies getDependencies() {
			return dependencies;
		}

		public void setDependencies(EntityDependencies dependencies) {
			this.dependencies = dependencies;
		}
	}

	/**
	 * Clase para mapear la configuración de integración
	 */
	public static class IntegrationConfig {

		private String dtoClass;
		private String serviceBean;
		private String processMethod;

		// Getters y setters
		public String getDtoClass() {
			return dtoClass;
		}

		public void setDtoClass(String dtoClass) {
			this.dtoClass = dtoClass;
		}

		public String getServiceBean() {
			return serviceBean;
		}

		public void setServiceBean(String serviceBean) {
			this.serviceBean = serviceBean;
		}

		public String getProcessMethod() {
			return processMethod;
		}

		public void setProcessMethod(String processMethod) {
			this.processMethod = processMethod;
		}
	}

	/**
	 * Clase para mapear la configuración global
	 */
	public static class GlobalSettings {

		private Integer pollInterval;
		private Integer maxFilesPerPoll;

		// Getters y setters
		public Integer getPollInterval() {
			return pollInterval;
		}

		public void setPollInterval(Integer pollInterval) {
			this.pollInterval = pollInterval;
		}

		public Integer getMaxFilesPerPoll() {
			return maxFilesPerPoll;
		}

		public void setMaxFilesPerPoll(Integer maxFilesPerPoll) {
			this.maxFilesPerPoll = maxFilesPerPoll;
		}
	}

	@Bean
	public List<Entity> configuredEntities() {
		List<Entity> entities = new ArrayList<>();

		try {
			// Cargar configuración JSON
			ObjectMapper mapper = new ObjectMapper();
			Resource resource = new ClassPathResource("config/folder-monitor.json");

			String json = new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
			JsonNode rootNode = mapper.readTree(json);
			JsonNode folderMonitorNode = rootNode.get("folder-monitor");
			JsonNode entitiesNode = folderMonitorNode.get("entities");

			for (JsonNode entityNode : entitiesNode) {
				String type = entityNode.get("type").asText();
				String name = entityNode.get("name").asText();
				String folder = entityNode.get("folder").asText();
				Integer priority = entityNode.has("priority") ? entityNode.get("priority").asInt() : 100;

				// Procesar dependencias
				EntityDependencies dependencies = new EntityDependencies();
				if (entityNode.has("dependencies")) {
					JsonNode dependenciesNode = entityNode.get("dependencies");

					if (dependenciesNode.has("block")) {
						JsonNode blockNode = dependenciesNode.get("block");
						List<String> block = new ArrayList<>();
						if (blockNode.isArray()) {
							for (JsonNode item : blockNode) {
								block.add(item.asText());
							}
						}
						dependencies.setBlock(block);
					}

					if (dependenciesNode.has("blocked-by")) {
						JsonNode blockedByNode = dependenciesNode.get("blocked-by");
						List<String> blockedBy = new ArrayList<>();
						if (blockedByNode.isArray()) {
							for (JsonNode item : blockedByNode) {
								blockedBy.add(item.asText());
							}
						}
						dependencies.setBlockedBy(blockedBy);
					}

					if (dependenciesNode.has("precedence")) {
						dependencies.setPrecedence(dependenciesNode.get("precedence").asText());
					}
				}

				// Crear entidad
				Entity entity = entityFactory.createEntity(type, name, folder, priority);

				// Establecer dependencias
				entity.setEntityDependencies(dependencies);

				entities.add(entity);
			}

			return entities;
		}
		catch (Exception e) {
			logger.error("Error al cargar configuración de entidades desde JSON", e);
			return createDefaultEntities();
		}
	}

	/**
	 * Método para cargar la configuración JSON como objeto Java usando Jackson
	 */
	@Bean
	public FolderMonitorConfig folderMonitorConfig() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Resource resource = new ClassPathResource("config/folder-monitor.json");
			return mapper.readValue(resource.getInputStream(), FolderMonitorConfig.class);
		}
		catch (IOException e) {
			logger.error("Error al cargar la configuración JSON", e);
			return new FolderMonitorConfig();
		}
	}

	// Método para crear entidades por defecto (en caso de fallo)
	private List<Entity> createDefaultEntities() {
		List<Entity> defaultEntities = new ArrayList<>();

		try {
			defaultEntities.add(entityFactory.createEntity("items", "Productos", "C:/comerzzia/monitor/items", 200));

			defaultEntities.add(entityFactory.createEntity("promotions", "Promociones", "C:/comerzzia/monitor/promotions", 100));

			defaultEntities.add(entityFactory.createEntity("warehouse", "Almacén", "C:/comerzzia/monitor/warehouse", 150));
		}
		catch (Exception e) {
			logger.error("Error al crear entidades por defecto", e);
		}

		return defaultEntities;
	}
}