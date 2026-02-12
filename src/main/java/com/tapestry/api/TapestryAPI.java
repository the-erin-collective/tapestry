package com.tapestry.api;

import com.tapestry.extensions.TapestryExtensionContext;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The core Tapestry API surface exposed to TypeScript mods.
 * 
 * This class represents the main API object that gets injected into the JavaScript context.
 * It provides access to various domains (worlds, worldgen, events, mods, core) and manages
 * the lifecycle of API extensions and mod registrations.
 */
public class TapestryAPI {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryAPI.class);
    
    private final GuardedMap<String, Object> domains = new GuardedMap<>();
    private final GuardedMap<String, Object> mods = new GuardedMap<>();
    private boolean frozen = false;
    private Instant freezeTime;
    
    /**
     * Creates a new TapestryAPI instance with default domains.
     */
    public TapestryAPI() {
        // Initialize default domains
        domains.put("worlds", new HashMap<String, Object>());
        domains.put("worldgen", new HashMap<String, Object>());
        domains.put("events", new HashMap<String, Object>());
        domains.put("core", new HashMap<String, Object>());
        
        LOGGER.debug("TapestryAPI initialized with default domains");
    }
    
    /**
     * Extends a domain with additional API methods.
     * 
     * @param domainName domain to extend
     * @param extensions extensions to add
     * @throws IllegalStateException if API is frozen
     */
    public void extendDomain(String domainName, Map<String, Object> extensions) {
        if (frozen) {
            throw new IllegalStateException("Cannot extend domain after API is frozen");
        }
        
        if (!domains.containsKey(domainName)) {
            throw new IllegalArgumentException("Unknown domain: " + domainName);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> domainMap = (Map<String, Object>) domains.get(domainName);
        domainMap.putAll(extensions);
        
        LOGGER.debug("Extended domain '{}' with {} methods", domainName, extensions.size());
    }
    
    /**
     * Registers a mod's API surface.
     * 
     * @param modId mod identifier
     * @param modApi mod's API object
     * @throws IllegalStateException if API is frozen
     */
    public void registerModApi(String modId, Object modApi) {
        if (frozen) {
            throw new IllegalStateException("Cannot register mod API after API is frozen");
        }
        
        mods.put(modId, modApi);
        
        LOGGER.debug("Registered API for mod: {}", modId);
    }
    
    /**
     * Freezes API surface, making it immutable.
     * 
     * After freezing, no further modifications can be made to domains or mods.
     */
    public void freeze() {
        if (frozen) {
            LOGGER.warn("API is already frozen");
            return;
        }
        
        domains.freeze();
        mods.freeze();
        frozen = true;
        freezeTime = Instant.now();
        
        LOGGER.info("TapestryAPI frozen at {}", freezeTime);
    }
    
    /**
     * Gets the domains map (unmodifiable if frozen).
     * 
     * @return domains map
     */
    public Map<String, Object> getDomains() {
        return domains.unmodifiableView();
    }
    
    /**
     * Gets the worlds domain.
     * 
     * @return worlds domain
     */
    public Map<String, Object> getWorlds() {
        return (Map<String, Object>) domains.get("worlds");
    }
    
    /**
     * Gets the worldgen domain.
     * 
     * @return worldgen domain
     */
    public Map<String, Object> getWorldgen() {
        return (Map<String, Object>) domains.get("worldgen");
    }
    
    /**
     * Gets the events domain.
     * 
     * @return events domain
     */
    public Map<String, Object> getEvents() {
        return (Map<String, Object>) domains.get("events");
    }
    
    /**
     * Gets the core domain.
     * 
     * @return core domain
     */
    public Map<String, Object> getCore() {
        return (Map<String, Object>) domains.get("core");
    }
    
    /**
     * Gets the mods map (unmodifiable if frozen).
     * 
     * @return mods map
     */
    public Map<String, Object> getMods() {
        return mods.unmodifiableView();
    }
    
    /**
     * Checks if the API is frozen.
     * 
     * @return true if frozen
     */
    public boolean isFrozen() {
        return frozen;
    }
    
    /**
     * Gets the time when the API was frozen.
     * 
     * @return freeze time, or null if not frozen
     */
    public Instant getFreezeTime() {
        return freezeTime;
    }
    
    /**
     * Creates an extension context for capability registration.
     * 
     * @param extensionId the extension ID
     * @param phase the phase to create context for
     * @param apiRegistry the API registry
     * @param hookRegistry the hook registry  
     * @param serviceRegistry the service registry
     * @param declaredCapabilities the declared capabilities
     * @return a new extension context
     */
    public TapestryExtensionContext createContext(String extensionId, 
                                                  TapestryPhase phase,
                                                  ApiRegistry apiRegistry,
                                                  HookRegistry hookRegistry,
                                                  ServiceRegistry serviceRegistry,
                                                  Map<String, TapestryExtensionDescriptor> declaredCapabilities) {
        // Only create context in REGISTRATION phase
        if (phase != TapestryPhase.REGISTRATION) {
            throw new IllegalStateException(
                "Extension context can only be created in REGISTRATION phase, but current phase is " + phase
            );
        }
        
        // Create extension context with proper registries
        Map<String, Set<String>> registeredCapabilities = new HashMap<>();
        return new ExtensionRegistrationContext(
            extensionId,
            apiRegistry,
            hookRegistry, 
            serviceRegistry,
            registeredCapabilities
        );
    }
}
