package com.tapestry.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration service for TypeScript mods.
 * 
 * Loads, validates, and provides read-only access to mod configuration
 * with schema validation and fail-fast error handling.
 */
public class ConfigService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private final Path configDir;
    private final Map<String, ModConfig> configs = new ConcurrentHashMap<>();
    
    public ConfigService(Path configDir) {
        this.configDir = configDir;
    }
    
    /**
     * Loads and validates configuration for a mod.
     * 
     * @param modId the mod ID
     * @param schema the configuration schema
     * @throws ConfigValidationException if validation fails
     * @throws ConfigLoadException if loading fails
     */
    public void loadConfig(String modId, ConfigSchema schema) throws ConfigValidationException, ConfigLoadException {
        Path configFile = configDir.resolve(modId + ".json");
        
        Map<String, Object> config = new HashMap<>();
        
        // Load config file if it exists
        if (Files.exists(configFile)) {
            try {
                String content = Files.readString(configFile);
                JsonNode jsonNode = MAPPER.readTree(content);
                
                if (jsonNode.isObject()) {
                    ObjectNode objectNode = (ObjectNode) jsonNode;
                    Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
                    
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        config.put(field.getKey(), convertJsonNode(field.getValue()));
                    }
                }
                
                LOGGER.debug("Loaded config for mod {} from {}", modId, configFile);
                
            } catch (IOException e) {
                throw new ConfigLoadException(
                    String.format("Failed to load config for mod '%s' from '%s'", modId, configFile), e
                );
            }
        } else {
            LOGGER.debug("No config file found for mod {}, using defaults", modId);
        }
        
        // Validate and apply defaults
        schema.validate(config);
        
        // Store the validated config
        configs.put(modId, new ModConfig(config, schema));
        
        LOGGER.info("Loaded and validated config for mod {} with {} fields", 
            modId, config.size());
    }
    
    /**
     * Gets configuration for a specific mod as a read-only ProxyObject.
     * 
     * @param modId the mod ID
     * @return read-only configuration object
     */
    public ProxyObject getConfig(String modId) {
        ModConfig modConfig = configs.get(modId);
        if (modConfig == null) {
            throw new IllegalArgumentException(
                String.format("No configuration loaded for mod '%s'", modId)
            );
        }
        
        return modConfig.asProxyObject();
    }
    
    /**
     * Gets configuration for the current mod (self-scoped).
     * 
     * @param modId the current mod ID
     * @return read-only configuration object
     */
    public ProxyObject getSelfConfig(String modId) {
        return getConfig(modId);
    }
    
    /**
     * Gets the raw configuration map for a mod (internal use).
     * 
     * @param modId the mod ID
     * @return configuration map
     */
    public Map<String, Object> getRawConfig(String modId) {
        ModConfig modConfig = configs.get(modId);
        return modConfig != null ? modConfig.config() : Collections.emptyMap();
    }
    
    /**
     * Converts a JsonNode to a Java object.
     */
    private Object convertJsonNode(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            if (node.isInt()) {
                return node.asInt();
            } else if (node.isLong()) {
                return node.asLong();
            } else {
                return node.asDouble();
            }
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isNull()) {
            return null;
        } else if (node.isArray()) {
            List<Object> array = new ArrayList<>();
            for (JsonNode element : node) {
                array.add(convertJsonNode(element));
            }
            return array;
        } else if (node.isObject()) {
            Map<String, Object> object = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                object.put(field.getKey(), convertJsonNode(field.getValue()));
            }
            return object;
        }
        return null;
    }
    
    /**
     * Clears all loaded configurations (for testing/shutdown).
     */
    public void clear() {
        configs.clear();
        LOGGER.debug("Config service cleared");
    }
    
    /**
     * Represents a loaded mod configuration.
     */
    private static class ModConfig {
        private final Map<String, Object> config;
        private final ConfigSchema schema;
        private final ProxyObject proxyObject;
        
        ModConfig(Map<String, Object> config, ConfigSchema schema) {
            this.config = Map.copyOf(config);
            this.schema = schema;
            this.proxyObject = ProxyObject.fromMap(this.config);
        }
        
        Map<String, Object> config() { return config; }
        ConfigSchema schema() { return schema; }
        ProxyObject asProxyObject() { return proxyObject; }
    }
}
