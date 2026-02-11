package com.tapestry.api;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core API surface that will eventually be exposed to TypeScript.
 * 
 * In Phase 1, this contains empty domains but provides the structure
 * and enforcement mechanisms for future API extensions.
 */
public class TapestryAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryAPI.class);
    
    // Core domains - these are main API namespaces
    private final GuardedMap<String, Object> worlds;
    private final GuardedMap<String, Object> worldgen;
    private final GuardedMap<String, Object> events;
    private final GuardedMap<String, Map<String, Object>> mods;
    private final GuardedMap<String, Object> core;
    
    // Internal state
    private volatile boolean frozen = false;
    
    public TapestryAPI() {
        // Initialize domains as GuardedMap instances
        this.worlds = new GuardedMap<>();
        this.worldgen = new GuardedMap<>();
        this.events = new GuardedMap<>();
        this.mods = new GuardedMap<>();
        this.core = new GuardedMap<>();
        
        // Initialize core with phase information
        this.core.put("phases", Arrays.asList(TapestryPhase.values()));
        this.core.put("capabilities", new ArrayList<>());
        
        LOGGER.debug("TapestryAPI initialized with empty domains");
    }
    
    /**
     * Extends a core domain with a new property.
     * Only allowed during REGISTRATION phase and before freeze.
     * 
     * @param domain the domain to extend
     * @param key the property key
     * @param value the property value
     * @throws IllegalStateException if called outside REGISTRATION or after freeze
     * @throws IllegalArgumentException if the property already exists
     */
    public void extendDomain(String domain, String key, Object value) {
        PhaseController.getInstance().requirePhase(TapestryPhase.REGISTRATION);
        
        if (frozen) {
            throw new IllegalStateException("Cannot extend domain " + domain + ": API is frozen");
        }
        
        Map<String, Object> domainMap = getDomainMap(domain);
        if (domainMap.containsKey(key)) {
            throw new IllegalArgumentException(
                String.format("Domain %s already contains key '%s'", domain, key)
            );
        }
        
        domainMap.put(key, value);
        LOGGER.debug("Extended domain {} with key {} = {}", domain, key, value);
    }
    
    /**
     * Registers a mod-owned API under the mods namespace.
     * Only allowed during REGISTRATION phase and before freeze.
     * 
     * @param extensionId the extension ID
     * @param key the API key
     * @param value the API object
     * @throws IllegalStateException if called outside REGISTRATION or after freeze
     * @throws IllegalArgumentException if the key already exists for this extension
     */
    public void registerModAPI(String extensionId, String key, Object value) {
        PhaseController.getInstance().requirePhase(TapestryPhase.REGISTRATION);
        
        if (frozen) {
            throw new IllegalStateException("Cannot register mod API: API is frozen");
        }
        
        Map<String, Object> extensionMods = mods.computeIfAbsent(extensionId, k -> new HashMap<>());
        if (extensionMods.containsKey(key)) {
            throw new IllegalArgumentException(
                String.format("Extension %s already has mod API key '%s'", extensionId, key)
            );
        }
        
        extensionMods.put(key, value);
        LOGGER.debug("Registered mod API for extension {}: {} = {}", extensionId, key, value);
    }
    
    /**
     * Freezes all domains, making them immutable.
     * After this call, no further modifications are allowed.
     */
    public void freeze() {
        PhaseController.getInstance().requirePhase(TapestryPhase.REGISTRATION);
        
        if (frozen) {
            LOGGER.warn("API is already frozen");
            return;
        }
        
        // Freeze all GuardedMap instances
        worlds.setFrozen(true);
        worldgen.setFrozen(true);
        events.setFrozen(true);
        mods.setFrozen(true);
        core.setFrozen(true);
        
        this.frozen = true;
        this.phaseTransitionTime = Instant.now();
        LOGGER.info("TapestryAPI frozen - no further modifications allowed");
    }
    
    /**
     * Gets the domain map for the given domain name.
     * 
     * @param domain the domain name
     * @return the domain map
     * @throws IllegalArgumentException if the domain name is invalid
     */
    private Map<String, Object> getDomainMap(String domain) {
        return switch (domain) {
            case "worlds" -> worlds;
            case "worldgen" -> worldgen;
            case "events" -> events;
            case "core" -> core;
            default -> throw new IllegalArgumentException("Unknown domain: " + domain);
        };
    }
    
    /**
     * Converts a mutable map to an unmodifiable map (shallow freeze).
     * 
     * @param map the map to freeze
     */
    private void freezeDomain(Map<String, Object> map) {
        // Create unmodifiable view and replace contents
        Map<String, Object> frozen = Collections.unmodifiableMap(new HashMap<>(map));
        map.clear();
        map.putAll(frozen);
    }
    
    /**
     * Gets the worlds domain (read-only).
     * 
     * @return worlds domain
     */
    public Map<String, Object> getWorlds() {
        return worlds.unmodifiableView();
    }
    
    /**
     * Gets the worldgen domain (read-only).
     * 
     * @return worldgen domain
     */
    public Map<String, Object> getWorldgen() {
        return worldgen.unmodifiableView();
    }
    
    /**
     * Gets the events domain (read-only).
     * 
     * @return events domain
     */
    public Map<String, Object> getEvents() {
        return events.unmodifiableView();
    }
    
    /**
     * Gets the mods namespace (read-only).
     * 
     * @return mods namespace
     */
    public Map<String, Map<String, Object>> getMods() {
        return mods.unmodifiableView();
    }
    
    /**
     * Gets the core domain (read-only).
     * 
     * @return core domain
     */
    public Map<String, Object> getCore() {
        return core.unmodifiableView();
    }
    
    /**
     * Checks if the API is frozen.
     * 
     * @return true if the API is frozen
     */
    public boolean isFrozen() {
        return frozen;
    }
    
    /**
     * Gets the time of the last phase transition.
     * 
     * @return the timestamp of the last transition
     */
    public Instant getPhaseTransitionTime() {
        return phaseTransitionTime;
    }
}
