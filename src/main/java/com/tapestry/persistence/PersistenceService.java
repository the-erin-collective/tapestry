package com.tapestry.persistence;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core persistence service for managing mod state storage and lifecycle.
 * 
 * This service provides the main persistence functionality for Tapestry mods,
 * including loading, saving, and managing mod state stores.
 * 
 * Note: This class is deprecated in favor of ClientPersistenceService and ServerPersistenceService.
 * It remains for backward compatibility during the transition.
 */
@Deprecated
public class PersistenceService implements PersistenceServiceInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceService.class);
    private static PersistenceService instance;
    
    private final Map<String, Map<String, Object>> modStates;
    private final Map<String, ModStateStore> modStores;
    private final StorageBackend storageBackend;
    private boolean initialized = false;
    
    /**
     * Gets the singleton persistence service instance.
     * 
     * @return the persistence service instance
     */
    public static PersistenceService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PersistenceService not initialized");
        }
        return instance;
    }
    
    /**
     * Initializes the persistence service with the given storage directory.
     * Must be called during PERSISTENCE_READY phase.
     * 
     * @param storageDirectory the directory for persistence files
     * @throws IllegalStateException if not in PERSISTENCE_READY phase
     */
    public static void initialize(Path storageDirectory) {
        PhaseController phaseController = PhaseController.getInstance();
        phaseController.requireAtLeast(TapestryPhase.PERSISTENCE_READY);
        
        if (instance != null) {
            LOGGER.warn("PersistenceService already initialized");
            return;
        }
        
        instance = new PersistenceService(storageDirectory);
        instance.loadAllModStates();
        
        LOGGER.info("PersistenceService initialized with storage directory: {}", storageDirectory);
    }
    
    /**
     * Private constructor for singleton pattern.
     * 
     * @param storageDirectory the directory for persistence files
     */
    private PersistenceService(Path storageDirectory) {
        this.modStates = new ConcurrentHashMap<>();
        this.modStores = new ConcurrentHashMap<>();
        this.storageBackend = new StorageBackend(storageDirectory);
        
        // Register lifecycle hooks
        registerLifecycleHooks();
    }
    
    /**
     * Loads all existing mod states from disk.
     * This is called during initialization.
     */
    private void loadAllModStates() {
        LOGGER.info("Loading persistence states...");
        
        // For Phase 9, we don't know which mods exist yet
        // So we'll load states on-demand when mods request them
        // This keeps the loading simple and explicit
        
        initialized = true;
        LOGGER.info("PersistenceService loading completed");
    }
    
    /**
     * Gets or creates a state store for a specific mod.
     * 
     * @param modId the mod identifier
     * @return the mod's state store
     */
    public ModStateStore getModStateStore(String modId) {
        if (!initialized) {
            throw new IllegalStateException("PersistenceService not initialized");
        }
        
        return modStores.computeIfAbsent(modId, this::createModStateStore);
    }
    
    /**
     * Creates a new state store for a mod, loading existing state if available.
     * 
     * @param modId the mod identifier
     * @return a new mod state store
     */
    private ModStateStore createModStateStore(String modId) {
        LOGGER.debug("Creating state store for mod: {}", modId);
        
        // Load existing state or create empty state
        Map<String, Object> state = modStates.computeIfAbsent(modId, 
            id -> storageBackend.loadModState(id));
        
        return new ModStateStore(modId, state, this);
    }
    
    /**
     * Saves a specific mod's state to disk.
     * 
     * @param modId the mod identifier
     */
    public void saveModState(String modId) {
        if (!initialized) {
            throw new IllegalStateException("PersistenceService not initialized");
        }
        
        Map<String, Object> state = modStates.get(modId);
        if (state != null) {
            storageBackend.saveModState(modId, state);
            LOGGER.debug("Saved state for mod: {}", modId);
        }
    }
    
    /**
     * Saves all mod states to disk.
     * This is called during server shutdown.
     */
    public void saveAll() {
        if (!initialized) {
            LOGGER.warn("Attempted to save uninitialized PersistenceService");
            return;
        }
        
        saveAllModStates();
    }
    
    /**
     * Saves all mod states to disk.
     * This is called during server shutdown.
     */
    public void saveAllModStates() {
        if (!initialized) {
            LOGGER.warn("Attempted to save uninitialized PersistenceService");
            return;
        }
        
        LOGGER.info("Saving all persistence states...");
        
        for (Map.Entry<String, Map<String, Object>> entry : modStates.entrySet()) {
            try {
                storageBackend.saveModState(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                LOGGER.error("Failed to save state for mod {}: {}", entry.getKey(), e.getMessage(), e);
            }
        }
        
        LOGGER.info("PersistenceService save completed");
    }
    
    /**
     * Registers Fabric lifecycle hooks for save operations.
     */
    private void registerLifecycleHooks() {
        // Save on server stopping
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        
        // Optional: Save on world save (can be added later)
        // ServerWorldEvents.SAVE.register(this::onWorldSave);
        
        LOGGER.debug("Registered persistence lifecycle hooks");
    }
    
    /**
     * Handles server stopping event.
     * 
     * @param server the minecraft server
     */
    private void onServerStopping(MinecraftServer server) {
        LOGGER.info("Server stopping - saving persistence states");
        saveAllModStates();
    }
    
    /**
     * Handles world save event (optional, for future use).
     * 
     * @param server the minecraft server
     * @param world the world being saved
     */
    private void onWorldSave(MinecraftServer server, net.minecraft.server.world.ServerWorld world) {
        // This can be enabled later for autosave functionality
        // For Phase 9, we only save on shutdown
        LOGGER.debug("World save event received (autosave not implemented in Phase 9)");
    }
    
    /**
     * Gets the storage backend for direct access (used by ModStateStore).
     * 
     * @return the storage backend
     */
    StorageBackend getStorageBackend() {
        return storageBackend;
    }
    
    /**
     * Checks if the persistence service has been initialized.
     * 
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Gets the number of mods with loaded state.
     * 
     * @return the number of loaded mods
     */
    public int getLoadedModCount() {
        return modStates.size();
    }
    
    /**
     * Deletes all persistence data for a mod.
     * 
     * @param modId the mod identifier
     */
    public void deleteModState(String modId) {
        modStates.remove(modId);
        modStores.remove(modId);
        storageBackend.deleteModState(modId);
        LOGGER.info("Deleted all persistence data for mod: {}", modId);
    }
    
    /**
     * Saves a specific mod's state to disk.
     * 
     * @param modId the mod identifier
     * @param state the state data to save
     */
    public void saveModState(String modId, Map<String, Object> state) {
        storageBackend.saveModState(modId, state);
        LOGGER.debug("Saved state for mod: {}", modId);
    }
}
