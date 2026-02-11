package com.tapestry.typescript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Registry for TypeScript mods discovered and loaded during Phase 2.
 * 
 * This class manages mod definitions, prevents duplicates, and provides
 * deterministic iteration order for mod loading.
 */
public class TsModRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TsModRegistry.class);
    
    // Mod ID validation pattern: [a-z][a-z0-9_]{0,63}
    private static final Pattern MOD_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");
    
    // Use TreeMap for deterministic ordering by mod ID
    private final Map<String, TsModDefinition> mods = new TreeMap<>();
    private boolean discoveryComplete = false;
    private boolean loadingComplete = false;
    
    /**
     * Registers a mod definition.
     * 
     * @param mod the mod definition to register
     * @throws IllegalStateException if called outside TS_LOAD phase
     * @throws IllegalArgumentException if mod ID is invalid or duplicate
     */
    public void registerMod(TsModDefinition mod) {
        if (discoveryComplete) {
            throw new IllegalStateException("Cannot register mods after discovery phase");
        }
        
        validateModId(mod.id());
        
        if (mods.containsKey(mod.id())) {
            throw new IllegalArgumentException(
                String.format("Mod ID '%s' is already registered by %s", 
                    mod.id(), mods.get(mod.id()).source())
            );
        }
        
        mods.put(mod.id(), mod);
        LOGGER.info("Registered mod '{}' from source '{}'", mod.id(), mod.source());
    }
    
    /**
     * Validates a mod ID against the required pattern.
     * 
     * @param modId the mod ID to validate
     * @throws IllegalArgumentException if the ID is invalid
     */
    private void validateModId(String modId) {
        if (!MOD_ID_PATTERN.matcher(modId).matches()) {
            throw new IllegalArgumentException(
                String.format("Invalid mod ID '%s'. Must match pattern: [a-z][a-z0-9_]{0,63}", modId)
            );
        }
    }
    
    /**
     * Marks the discovery phase as complete.
     * No further mod registrations will be allowed.
     */
    public void completeDiscovery() {
        if (discoveryComplete) {
            LOGGER.warn("Discovery already marked as complete");
            return;
        }
        
        discoveryComplete = true;
        LOGGER.info("Mod discovery complete. Found {} mods.", mods.size());
    }
    
    /**
     * Marks the loading phase as complete.
     * This is called after all onLoad functions have been executed.
     */
    public void completeLoading() {
        if (!discoveryComplete) {
            throw new IllegalStateException("Cannot complete loading before discovery is complete");
        }
        
        if (loadingComplete) {
            LOGGER.warn("Loading already marked as complete");
            return;
        }
        
        loadingComplete = true;
        LOGGER.info("Mod loading complete. All {} mods loaded successfully.", mods.size());
    }
    
    /**
     * Gets an unmodifiable list of all registered mods in deterministic order.
     * 
     * @return list of mods sorted by ID
     */
    public List<TsModDefinition> getMods() {
        return Collections.unmodifiableList(new ArrayList<>(mods.values()));
    }
    
    /**
     * Gets a mod by ID.
     * 
     * @param modId the mod ID
     * @return the mod definition, or null if not found
     */
    public TsModDefinition getMod(String modId) {
        return mods.get(modId);
    }
    
    /**
     * Gets the number of registered mods.
     * 
     * @return the mod count
     */
    public int getModCount() {
        return mods.size();
    }
    
    /**
     * Checks if discovery is complete.
     * 
     * @return true if discovery phase is complete
     */
    public boolean isDiscoveryComplete() {
        return discoveryComplete;
    }
    
    /**
     * Checks if loading is complete.
     * 
     * @return true if loading phase is complete
     */
    public boolean isLoadingComplete() {
        return loadingComplete;
    }
    
    /**
     * Clears the registry (for testing purposes).
     */
    public void clear() {
        mods.clear();
        discoveryComplete = false;
        loadingComplete = false;
        LOGGER.debug("Mod registry cleared");
    }
}
