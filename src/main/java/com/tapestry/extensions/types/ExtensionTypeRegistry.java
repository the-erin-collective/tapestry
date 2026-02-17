package com.tapestry.extensions.types;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for storing all exported type surfaces in-memory.
 * 
 * Phase 14: Cross-Mod Type Contracts
 */
public class ExtensionTypeRegistry {
    
    /**
     * In-memory storage of type modules by extension ID.
     * Map<ExtensionId, TypeModule>
     */
    private final Map<String, TypeModule> typeModules = new ConcurrentHashMap<>();
    
    /**
     * Whether the registry is frozen (immutable after TYPE_INIT).
     */
    private volatile boolean frozen = false;
    
    /**
     * Represents a type module for an extension.
     */
    public static record TypeModule(
        String extensionId,
        String dtsSource
    ) {}
    
    /**
     * Store a type module during DISCOVERY phase.
     * 
     * @param extensionId The extension ID
     * @param dtsSource The .d.ts file content
     * @throws IllegalStateException if registry is frozen
     */
    public void storeTypeModule(String extensionId, String dtsSource) {
        if (frozen) {
            throw new IllegalStateException("ExtensionTypeRegistry is frozen - cannot store type modules");
        }
        
        typeModules.put(extensionId, new TypeModule(extensionId, dtsSource));
    }
    
    /**
     * Get a type module by extension ID.
     * 
     * @param extensionId The extension ID
     * @return The type module, or null if not found
     */
    public TypeModule getTypeModule(String extensionId) {
        return typeModules.get(extensionId);
    }
    
    /**
     * Check if an extension has types registered.
     * 
     * @param extensionId The extension ID
     * @return true if the extension exports types
     */
    public boolean hasTypeModule(String extensionId) {
        return typeModules.containsKey(extensionId);
    }
    
    /**
     * Get all registered extension IDs.
     * 
     * @return Immutable set of extension IDs
     */
    public java.util.Set<String> getAllExtensionIds() {
        return java.util.Collections.unmodifiableSet(typeModules.keySet());
    }
    
    /**
     * Freeze the registry during TYPE_INIT sub-step.
     * After this call, the registry becomes read-only.
     */
    public void freeze() {
        this.frozen = true;
    }
    
    /**
     * Check if the registry is frozen.
     * 
     * @return true if frozen
     */
    public boolean isFrozen() {
        return frozen;
    }
    
    /**
     * Clear the registry (for testing only).
     */
    public void clear() {
        if (frozen) {
            throw new IllegalStateException("Cannot clear frozen ExtensionTypeRegistry");
        }
        typeModules.clear();
    }
    
    /**
     * Get the current memory footprint estimate.
     * 
     * @return Estimated memory usage in bytes
     */
    public long getMemoryFootprint() {
        long total = 0;
        for (TypeModule module : typeModules.values()) {
            total += module.extensionId().getBytes().length;
            total += module.dtsSource().getBytes().length;
        }
        return total;
    }
}
