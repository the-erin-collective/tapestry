package com.tapestry.extensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry for resolved capabilities.
 * 
 * Provides access to capability implementations after successful validation.
 * Immutable after boot resolution.
 */
public class CapabilityRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CapabilityRegistry.class);
    
    private static volatile boolean frozen = false;
    private static final Map<String, Object> capabilities = new ConcurrentHashMap<>();
    private static final Map<String, String> providers = new ConcurrentHashMap<>();
    
    /**
     * Initializes the capability registry with resolved capabilities.
     * 
     * @param capabilityProviders map of capability name to providing mod ID
     * @param implementations map of capability name to implementation object
     */
    public static void initialize(Map<String, String> capabilityProviders, Map<String, Object> implementations) {
        if (frozen) {
            throw new IllegalStateException("CapabilityRegistry is already frozen");
        }
        
        capabilities.clear();
        providers.clear();
        
        capabilities.putAll(implementations);
        providers.putAll(capabilityProviders);
        
        LOGGER.info("CapabilityRegistry initialized with {} capabilities", capabilities.size());
    }
    
    /**
     * Freezes the registry, making it immutable.
     * Called after successful validation.
     */
    public static void freeze() {
        frozen = true;
        LOGGER.debug("CapabilityRegistry frozen - no further modifications allowed");
    }
    
    /**
     * Gets a capability implementation.
     * 
     * @param capabilityName the name of the capability
     * @return the implementation object
     * @throws IllegalStateException if called before resolution or capability not found
     */
    public static Object getCapability(String capabilityName) {
        if (!frozen) {
            throw new IllegalStateException("CapabilityRegistry not yet resolved - getCapability() only available after validation completes");
        }
        
        Object implementation = capabilities.get(capabilityName);
        if (implementation == null) {
            throw new IllegalStateException("Capability '" + capabilityName + "' not found in registry");
        }
        
        return implementation;
    }
    
    /**
     * Gets the provider mod ID for a capability.
     * 
     * @param capabilityName the name of the capability
     * @return the mod ID that provides this capability
     * @throws IllegalStateException if called before resolution or capability not found
     */
    public static String getCapabilityProvider(String capabilityName) {
        if (!frozen) {
            throw new IllegalStateException("CapabilityRegistry not yet resolved - getCapabilityProvider() only available after validation completes");
        }
        
        String provider = providers.get(capabilityName);
        if (provider == null) {
            throw new IllegalStateException("Capability '" + capabilityName + "' not found in registry");
        }
        
        return provider;
    }
    
    /**
     * Checks if the registry is frozen.
     * 
     * @return true if frozen, false otherwise
     */
    public static boolean isFrozen() {
        return frozen;
    }
    
    /**
     * Gets all registered capability names.
     * 
     * @return immutable set of capability names
     */
    public static Map<String, Object> getAllCapabilities() {
        if (!frozen) {
            throw new IllegalStateException("CapabilityRegistry not yet resolved");
        }
        
        return Collections.unmodifiableMap(capabilities);
    }
    
    /**
     * Gets all capability providers.
     * 
     * @return immutable map of capability names to provider mod IDs
     */
    public static Map<String, String> getAllProviders() {
        if (!frozen) {
            throw new IllegalStateException("CapabilityRegistry not yet resolved");
        }
        
        return Collections.unmodifiableMap(providers);
    }
    
    /**
     * Resets the registry (for testing purposes).
     */
    static void reset() {
        frozen = false;
        capabilities.clear();
        providers.clear();
        LOGGER.debug("CapabilityRegistry reset");
    }
}
