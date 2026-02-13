package com.tapestry.persistence;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side persistence service.
 * 
 * This service handles persistence for client-only mods, storing data in the
 * game directory rather than world directory.
 */
public class ClientPersistenceService implements PersistenceServiceInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientPersistenceService.class);
    private static ClientPersistenceService instance;
    
    private final Map<String, Object> modStates;
    private final Map<String, ModStateStore> modStores;
    private final StorageBackend storageBackend;
    
    /**
     * Gets the client persistence service instance.
     * 
     * @return the client persistence service instance
     * @throws IllegalStateException if not initialized
     */
    public static ClientPersistenceService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ClientPersistenceService not initialized");
        }
        return instance;
    }
    
    /**
     * Initializes the client persistence service with the given game directory.
     * Must be called during PERSISTENCE_READY phase.
     * 
     * @param gameDirectory the game directory for client persistence files
     * @throws IllegalStateException if not in PERSISTENCE_READY phase
     */
    public static void initialize(Path gameDirectory) {
        PhaseController phaseController = PhaseController.getInstance();
        phaseController.requireAtLeast(TapestryPhase.PERSISTENCE_READY);
        
        if (instance != null) {
            LOGGER.warn("ClientPersistenceService already initialized");
            return;
        }
        
        // Client persistence goes in <gameDir>/tapestry-client/
        Path clientStorageDir = gameDirectory.resolve("tapestry-client");
        instance = new ClientPersistenceService(clientStorageDir);
        instance.loadAllModStates();
        
        LOGGER.info("ClientPersistenceService initialized with storage directory: {}", clientStorageDir);
    }
    
    /**
     * Private constructor for singleton pattern.
     * 
     * @param storageDirectory the directory for client persistence files
     */
    private ClientPersistenceService(Path storageDirectory) {
        this.modStates = new ConcurrentHashMap<>();
        this.modStores = new ConcurrentHashMap<>();
        this.storageBackend = new StorageBackend(storageDirectory);
    }
    
    /**
     * Loads all existing mod states from disk.
     * This is called during initialization.
     */
    private void loadAllModStates() {
        LOGGER.info("Loading client persistence states...");
        
        // For now, we don't have a way to discover client mods
        // This will be enhanced when client mod discovery is implemented
        LOGGER.info("Client persistence service loaded (no mods to load yet)");
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
     * This is typically called on client shutdown.
     */
    public void saveAll() {
        LOGGER.info("Saving all client persistence states...");
        
        for (Map.Entry<String, ModStateStore> entry : modStores.entrySet()) {
            String modId = entry.getKey();
            ModStateStore store = entry.getValue();
            
            try {
                storageBackend.saveModState(modId, store.getAll());
                LOGGER.debug("Saved client state for mod: {}", modId);
            } catch (Exception e) {
                LOGGER.error("Failed to save client state for mod: {}", modId, e);
            }
        }
        
        LOGGER.info("Client persistence save completed");
    }
    
    /**
     * Saves a specific mod's state to disk.
     * 
     * @param modId the mod identifier
     * @param state the state data to save
     */
    public void saveModState(String modId, Map<String, Object> state) {
        storageBackend.saveModState(modId, state);
        LOGGER.debug("Saved client state for mod: {}", modId);
    }
}
