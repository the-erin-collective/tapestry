package com.tapestry.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin wrapper providing per-mod persistence access.
 * 
 * This class is injected into each mod's runtime context and provides
 * a simple Map-like interface for persistence operations.
 * All operations are single-thread bound by design.
 */
public class ModStateStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModStateStore.class);
    
    private final String modId;
    private final Map<String, Object> state;
    private final PersistenceService persistenceService;
    
    /**
     * Creates a new mod state store.
     * 
     * @param modId the mod identifier
     * @param state the in-memory state map for this mod
     * @param persistenceService the persistence service for save operations
     */
    public ModStateStore(String modId, Map<String, Object> state, PersistenceService persistenceService) {
        this.modId = modId;
        this.state = state;
        this.persistenceService = persistenceService;
    }
    
    /**
     * Gets a value from the mod's persistence store.
     * 
     * @param key the key to retrieve
     * @return the stored value, or null if not found
     */
    public Object get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        Object value = state.get(key);
        LOGGER.debug("Mod {} retrieved key '{}' -> {}", modId, key, value);
        return value;
    }
    
    /**
     * Sets a value in the mod's persistence store.
     * 
     * @param key the key to set
     * @param value the value to store (must be valid type)
     * @throws IllegalArgumentException if key is null or value type is invalid
     */
    public void set(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        if (value != null && !isValidType(value)) {
            throw new IllegalArgumentException(
                String.format("Invalid value type for key '%s' in mod '%s': %s", 
                    key, modId, value.getClass().getName())
            );
        }
        
        state.put(key, value);
        LOGGER.debug("Mod {} set key '{}' = {}", modId, key, value);
    }
    
    /**
     * Deletes a value from the mod's persistence store.
     * 
     * @param key the key to delete
     * @throws IllegalArgumentException if key is null
     */
    public void delete(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        Object removed = state.remove(key);
        LOGGER.debug("Mod {} deleted key '{}' (was: {})", modId, key, removed);
    }
    
    /**
     * Gets all keys in the mod's persistence store.
     * 
     * @return a set of all keys
     */
    public Set<String> keys() {
        return Set.copyOf(state.keySet());
    }
    
    /**
     * Checks if a key exists in the mod's persistence store.
     * 
     * @param key the key to check
     * @return true if the key exists
     */
    public boolean hasKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        return state.containsKey(key);
    }
    
    /**
     * Gets the number of key-value pairs in the mod's persistence store.
     * 
     * @return the size of the store
     */
    public int size() {
        return state.size();
    }
    
    /**
     * Clears all data from the mod's persistence store.
     * This does not affect the persisted file until save is called.
     */
    public void clear() {
        state.clear();
        LOGGER.debug("Mod {} cleared all persistence data", modId);
    }
    
    /**
     * Triggers an immediate save of the mod's state to disk.
     * 
     * This is typically called automatically by the persistence service
     * during shutdown, but mods can call it explicitly if needed.
     */
    public void save() {
        persistenceService.saveModState(modId);
        LOGGER.debug("Mod {} triggered manual save", modId);
    }
    
    /**
     * Validates that a value is of an allowed type for persistence.
     * 
     * @param value the value to validate
     * @return true if the value type is allowed
     */
    private boolean isValidType(Object value) {
        if (value == null) {
            return true; // null is allowed
        }
        
        Class<?> clazz = value.getClass();
        
        // Primitive types and their wrappers
        if (clazz == String.class || 
            clazz == Boolean.class || 
            clazz == Integer.class || 
            clazz == Long.class || 
            clazz == Double.class || 
            clazz == Float.class) {
            return true;
        }
        
        // Arrays
        if (clazz.isArray()) {
            return true; // Arrays are allowed (recursive validation would be Phase 9.5)
        }
        
        // Maps (for plain objects)
        if (Map.class.isAssignableFrom(clazz)) {
            return true; // Maps are allowed (recursive validation would be Phase 9.5)
        }
        
        // Disallowed types
        return false;
    }
}
