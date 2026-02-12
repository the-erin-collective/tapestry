package com.tapestry.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State service for TypeScript mods.
 * 
 * Provides in-memory, namespaced state storage for mods with
 * JSON-serializable value enforcement and persistence-agnostic API.
 */
public class ModStateService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ModStateService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    // Per-mod state storage
    private final Map<String, Map<String, Object>> modStates = new ConcurrentHashMap<>();
    
    /**
     * Sets a value in the mod's state namespace.
     * 
     * @param modId the mod ID
     * @param key the state key
     * @param value the value to store (must be JSON-serializable)
     * @throws IllegalArgumentException if value is not JSON-serializable
     */
    public void set(String modId, String key, Object value) {
        PhaseController.getInstance().requireAtLeast(TapestryPhase.RUNTIME);
        
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("Mod ID cannot be null or empty");
        }
        
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("State key cannot be null or empty");
        }
        
        // Validate that value is JSON-serializable
        if (!isJsonSerializable(value)) {
            throw new IllegalArgumentException(
                String.format("State value for key '%s' is not JSON-serializable: %s", 
                    key, value.getClass().getSimpleName())
            );
        }
        
        Map<String, Object> state = modStates.computeIfAbsent(modId, k -> new ConcurrentHashMap<>());
        state.put(key, value);
        
        LOGGER.debug("Set state for mod '{}': {} = {}", modId, key, value);
    }
    
    /**
     * Gets a value from the mod's state namespace.
     * 
     * @param modId the mod ID
     * @param key the state key
     * @return the stored value, or null if not found
     */
    public Object get(String modId, String key) {
        PhaseController.getInstance().requireAtLeast(TapestryPhase.RUNTIME);
        
        Map<String, Object> state = modStates.get(modId);
        if (state == null) {
            return null;
        }
        
        Object value = state.get(key);
        LOGGER.debug("Got state for mod '{}': {} = {}", modId, key, value);
        
        return value;
    }
    
    /**
     * Checks if a key exists in the mod's state namespace.
     * 
     * @param modId the mod ID
     * @param key the state key
     * @return true if the key exists
     */
    public boolean has(String modId, String key) {
        PhaseController.getInstance().requireAtLeast(TapestryPhase.RUNTIME);
        
        Map<String, Object> state = modStates.get(modId);
        return state != null && state.containsKey(key);
    }
    
    /**
     * Deletes a key from the mod's state namespace.
     * 
     * @param modId the mod ID
     * @param key the state key
     * @return true if the key was deleted, false if it didn't exist
     */
    public boolean delete(String modId, String key) {
        PhaseController.getInstance().requireAtLeast(TapestryPhase.RUNTIME);
        
        Map<String, Object> state = modStates.get(modId);
        if (state == null) {
            return false;
        }
        
        Object removed = state.remove(key);
        boolean deleted = removed != null;
        
        if (deleted) {
            LOGGER.debug("Deleted state for mod '{}': {}", modId, key);
        }
        
        return deleted;
    }
    
    /**
     * Gets all keys in the mod's state namespace.
     * 
     * @param modId the mod ID
     * @return set of all keys for the mod
     */
    public Set<String> getKeys(String modId) {
        PhaseController.getInstance().requireAtLeast(TapestryPhase.RUNTIME);
        
        Map<String, Object> state = modStates.get(modId);
        return state != null ? Collections.unmodifiableSet(state.keySet()) : Collections.emptySet();
    }
    
    /**
     * Gets the entire state namespace for a mod.
     * 
     * @param modId the mod ID
     * @return read-only map of all state for the mod
     */
    public Map<String, Object> getAll(String modId) {
        PhaseController.getInstance().requireAtLeast(TapestryPhase.RUNTIME);
        
        Map<String, Object> state = modStates.get(modId);
        return state != null ? Collections.unmodifiableMap(state) : Collections.emptyMap();
    }
    
    /**
     * Clears all state for a mod.
     * 
     * @param modId the mod ID
     */
    public void clearMod(String modId) {
        PhaseController.getInstance().requireAtLeast(TapestryPhase.RUNTIME);
        
        Map<String, Object> removed = modStates.remove(modId);
        if (removed != null) {
            LOGGER.debug("Cleared all state for mod '{}'", modId);
        }
    }
    
    /**
     * Clears all state for all mods (for testing/shutdown).
     */
    public void clear() {
        modStates.clear();
        LOGGER.debug("Mod state service cleared");
    }
    
    /**
     * Checks if a value is JSON-serializable.
     * 
     * @param value the value to check
     * @return true if the value can be serialized to JSON
     */
    private boolean isJsonSerializable(Object value) {
        if (value == null) {
            return true;
        }
        
        // Primitive types and strings are serializable
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return true;
        }
        
        // Arrays of serializable elements
        if (value instanceof Object[]) {
            for (Object element : (Object[]) value) {
                if (!isJsonSerializable(element)) {
                    return false;
                }
            }
            return true;
        }
        
        // Collections of serializable elements
        if (value instanceof Collection) {
            for (Object element : (Collection<?>) value) {
                if (!isJsonSerializable(element)) {
                    return false;
                }
            }
            return true;
        }
        
        // Maps with serializable keys and values
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!isJsonSerializable(entry.getKey()) || !isJsonSerializable(entry.getValue())) {
                    return false;
                }
            }
            return true;
        }
        
        // Reject functions, host objects, etc.
        return false;
    }
    
    /**
     * Gets the number of stored keys for a mod.
     * 
     * @param modId the mod ID
     * @return number of stored keys
     */
    public int getSize(String modId) {
        Map<String, Object> state = modStates.get(modId);
        return state != null ? state.size() : 0;
    }
    
    /**
     * Gets all mod IDs that have state.
     * 
     * @return set of mod IDs with stored state
     */
    public Set<String> getModIds() {
        return Collections.unmodifiableSet(modStates.keySet());
    }
}
