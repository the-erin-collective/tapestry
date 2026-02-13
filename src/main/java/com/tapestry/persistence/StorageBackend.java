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
                LOGGER.warn("Empty persistence file for mod: {}", modId);
                return new ConcurrentHashMap<>();
            }
            
            // Extract data section, ignoring __version field for now
            JsonElement dataElement = root.get("data");
            if (dataElement == null || !dataElement.isJsonObject()) {
                LOGGER.warn("Invalid persistence file format for mod: {} - missing data section", modId);
                return new ConcurrentHashMap<>();
            }
            
            Map<String, Object> data = gson.fromJson(dataElement, Map.class);
            return data != null ? new ConcurrentHashMap<>(data) : new ConcurrentHashMap<>();
            
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
        root.addProperty("__version", 1); // Reserved for future migrations
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
}
