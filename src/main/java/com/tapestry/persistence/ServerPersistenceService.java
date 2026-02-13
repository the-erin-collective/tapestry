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
 * Server-side persistence service.
 * 
 * This service handles persistence for server mods, storing data in the
 * world directory rather than game directory.
 */
public class ServerPersistenceService implements PersistenceServiceInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerPersistenceService.class);
    private static ServerPersistenceService instance;
    
    private final Map<String, Object> modStates;
    private final Map<String, ModStateStore> modStores;
    private final StorageBackend storageBackend;
    
    /**
     * Gets the server persistence service instance.
     * 
     * @return the server persistence service instance
     * @throws IllegalStateException if not initialized
     */
    public static ServerPersistenceService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ServerPersistenceService not initialized");
        }
        return instance;
    }
    
    /**
     * Initializes the server persistence service with the given world directory.
     * Must be called during PERSISTENCE_READY phase.
     * 
     * @param worldDirectory the world directory for server persistence files
     * @throws IllegalStateException if not in PERSISTENCE_READY phase
     */
    public static void initialize(Path worldDirectory) {
        PhaseController phaseController = PhaseController.getInstance();
        phaseController.requireAtLeast(TapestryPhase.PERSISTENCE_READY);
        
        if (instance != null) {
            LOGGER.warn("ServerPersistenceService already initialized");
            return;
        }
        
        // Server persistence goes in <world>/data/tapestry/
        Path serverStorageDir = worldDirectory.resolve("data").resolve("tapestry");
        instance = new ServerPersistenceService(serverStorageDir);
        instance.loadAllModStates();
        
        LOGGER.info("ServerPersistenceService initialized with storage directory: {}", serverStorageDir);
    }
    
    /**
     * Private constructor for singleton pattern.
     * 
     * @param storageDirectory the directory for server persistence files
     */
    private ServerPersistenceService(Path storageDirectory) {
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
        LOGGER.info("Loading server persistence states...");
        
        // For now, we don't have a way to discover server mods
        // This will be enhanced when server mod discovery is implemented
        LOGGER.info("Server persistence service loaded (no mods to load yet)");
    }
    
    /**
     * Registers server lifecycle hooks for persistence.
     */
    private void registerLifecycleHooks() {
        // Save all states when server stops
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LOGGER.info("Server stopping, saving all persistence states...");
            saveAll();
        });
        
        // Save all states when world saves
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            LOGGER.debug("World unloading, saving persistence states...");
            saveAll();
        });
    }
    
    /**
     * Gets a state store for a specific mod.
     * 
     * @param modId the mod identifier
     * @return the mod's state store
     */
    public ModStateStore getModStateStore(String modId) {
        return modStores.computeIfAbsent(modId, id -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) modStates.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
            return new ModStateStore(id, state, this);
        });
    }
    
    /**
     * Gets the number of loaded mods.
     * 
     * @return the number of loaded mods
     */
    public int getLoadedModCount() {
        return modStores.size();
    }
    
    /**
     * Checks if the service is initialized.
     * 
     * @return true if initialized
     */
    public boolean isInitialized() {
        return instance != null;
    }
    
    /**
     * Saves all mod states to disk.
     * This is called on server stop and world save.
     */
    public void saveAll() {
        LOGGER.info("Saving all server persistence states...");
        
        for (Map.Entry<String, ModStateStore> entry : modStores.entrySet()) {
            String modId = entry.getKey();
            ModStateStore store = entry.getValue();
            
            try {
                storageBackend.saveModState(modId, store.getAll());
                LOGGER.debug("Saved server state for mod: {}", modId);
            } catch (Exception e) {
                LOGGER.error("Failed to save server state for mod: {}", modId, e);
            }
        }
        
        LOGGER.info("Server persistence save completed");
    }
    
    /**
     * Saves a specific mod's state to disk.
     * 
     * @param modId the mod identifier
     * @param state the state data to save
     */
    public void saveModState(String modId, Map<String, Object> state) {
        storageBackend.saveModState(modId, state);
        LOGGER.debug("Saved server state for mod: {}", modId);
    }
}
