package com.tapestry.persistence;

import java.util.Map;

/**
 * Common interface for persistence services.
 * 
 * This allows both client and server persistence services to be used
 * interchangeably in the main Tapestry mod.
 */
public interface PersistenceServiceInterface {
    /**
     * Gets a state store for a specific mod.
     * 
     * @param modId the mod identifier
     * @return the mod's state store
     */
    ModStateStore getModStateStore(String modId);
    
    /**
     * Gets the number of loaded mods.
     * 
     * @return the number of loaded mods
     */
    int getLoadedModCount();
    
    /**
     * Checks if the service is initialized.
     * 
     * @return true if initialized
     */
    boolean isInitialized();
    
    /**
     * Saves all mod states to disk.
     */
    void saveAll();
    
    /**
     * Saves a specific mod's state to disk.
     * 
     * @param modId the mod identifier
     * @param state the state data to save
     */
    void saveModState(String modId, Map<String, Object> state);
}
