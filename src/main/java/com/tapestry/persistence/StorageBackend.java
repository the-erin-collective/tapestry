package com.tapestry.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles low-level JSON serialization and file I/O for persistence.
 * 
 * This class encapsulates all disk operations and provides clean error boundaries
 * for persistence operations.
 */
public class StorageBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageBackend.class);
    private static final int CURRENT_SCHEMA_VERSION = 1;
    
    private final Gson gson;
    private final Path storageDirectory;
    
    /**
     * Creates a new storage backend for the given directory.
     * 
     * @param storageDirectory base directory for persistence files
     */
    public StorageBackend(Path storageDirectory) {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        this.storageDirectory = storageDirectory;
        
        // Ensure storage directory exists
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create persistence directory: " + storageDirectory, e);
        }
    }
    
    /**
     * Loads mod state from disk.
     * 
     * @param modId the mod identifier
     * @return the loaded state data, or empty map if no file exists
     * @throws IllegalStateException if file is corrupt or unreadable
     */
    public Map<String, Object> loadModState(String modId) {
        Path filePath = getModFilePath(modId);
        
        if (!Files.exists(filePath)) {
            LOGGER.debug("No existing persistence file for mod: {}", modId);
            return new ConcurrentHashMap<>();
        }
        
        try (Reader reader = Files.newBufferedReader(filePath)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            
            if (root == null) {
                LOGGER.warn("Empty or invalid JSON file for mod: {}", modId);
                return new ConcurrentHashMap<>();
            }
            
            // Check schema version
            if (!root.has("schemaVersion")) {
                LOGGER.warn("Legacy persistence file for mod: {} (no schema version)", modId);
                // Legacy format - treat entire root as data
                return convertLegacyFormat(root);
            }
            
            int schemaVersion = root.get("schemaVersion").getAsInt();
            if (schemaVersion != CURRENT_SCHEMA_VERSION) {
                throw new IllegalStateException(
                    String.format("Unsupported schema version %d for mod %s (expected %d)", 
                        schemaVersion, modId, CURRENT_SCHEMA_VERSION)
                );
            }
            
            JsonObject data = root.getAsJsonObject("data");
            if (data == null) {
                LOGGER.warn("Invalid persistence format for mod: {} (missing data field)", modId);
                return new ConcurrentHashMap<>();
            }
            
            return convertJsonToMap(data);
        } catch (JsonSyntaxException e) {
            String errorMsg = String.format("Invalid JSON in persistence file for mod '%s': %s", modId, e.getMessage());
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        } catch (JsonParseException e) {
            String errorMsg = String.format("Failed to parse persistence file for mod '%s': %s", modId, e.getMessage());
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        } catch (IOException e) {
            String errorMsg = String.format("Failed to read persistence file for mod '%s' at %s: %s", 
                modId, filePath, e.getMessage());
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }
    
    /**
     * Saves mod state to disk.
     * 
     * @param modId the mod identifier
     * @param state the state data to save
     * @throws IllegalStateException if write fails
     */
    public void saveModState(String modId, Map<String, Object> state) {
        Path filePath = getModFilePath(modId);
        
        // Create the JSON structure with version field
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", CURRENT_SCHEMA_VERSION);
        root.add("data", gson.toJsonTree(state));
        
        try (Writer writer = Files.newBufferedWriter(filePath)) {
            gson.toJson(root, writer);
            LOGGER.debug("Saved persistence state for mod: {} to {}", modId, filePath);
        } catch (IOException e) {
            String errorMsg = String.format("Failed to write persistence file for mod '%s' at %s: %s", 
                modId, filePath, e.getMessage());
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }
    
    /**
     * Gets the file path for a mod's persistence file.
     * 
     * @param modId the mod identifier
     * @return the full path to the mod's JSON file
     */
    private Path getModFilePath(String modId) {
        String fileName = modId + ".json";
        return storageDirectory.resolve(fileName);
    }
    
    /**
     * Checks if a mod has existing persistence data.
     * 
     * @param modId the mod identifier
     * @return true if the persistence file exists
     */
    public boolean hasModState(String modId) {
        return Files.exists(getModFilePath(modId));
    }
    
    /**
     * Deletes persistence data for a mod.
     * 
     * @param modId the mod identifier
     * @throws IllegalStateException if deletion fails
     */
    public void deleteModState(String modId) {
        Path filePath = getModFilePath(modId);
        
        if (Files.exists(filePath)) {
            try {
                Files.delete(filePath);
                LOGGER.info("Deleted persistence file for mod: {}", modId);
            } catch (IOException e) {
                String errorMsg = String.format("Failed to delete persistence file for mod '%s' at %s: %s", 
                    modId, filePath, e.getMessage());
                LOGGER.error(errorMsg);
                throw new IllegalStateException(errorMsg, e);
            }
        }
    }
    
    /**
     * Converts legacy format (no schema version) to map.
     * Treats the entire root object as data.
     * 
     * @param root the legacy JSON root
     * @return converted map
     */
    private Map<String, Object> convertLegacyFormat(JsonObject root) {
        LOGGER.warn("Converting legacy persistence format to new schema");
        return convertJsonToMap(root);
    }
    
    /**
     * Converts a JsonObject to a Map<String, Object>.
     * 
     * @param jsonObject the JSON object to convert
     * @return the converted map
     */
    private Map<String, Object> convertJsonToMap(JsonObject jsonObject) {
        Map<String, Object> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObject.entrySet()) {
            result.put(entry.getKey(), convertJsonElement(entry.getValue()));
        }
        return result;
    }
    
    /**
     * Converts a JsonElement to its appropriate Java type.
     * 
     * @param element the JSON element to convert
     * @return the converted Java object
     */
    private Object convertJsonElement(com.google.gson.JsonElement element) {
        if (element.isJsonNull()) {
            return null;
        } else if (element.isJsonPrimitive()) {
            com.google.gson.JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                return primitive.getAsNumber();
            } else {
                return primitive.getAsString();
            }
        } else if (element.isJsonArray()) {
            // Convert arrays to lists
            return gson.fromJson(element, Object.class);
        } else if (element.isJsonObject()) {
            // Convert nested objects to maps
            return convertJsonToMap(element.getAsJsonObject());
        }
        return null;
    }
}
