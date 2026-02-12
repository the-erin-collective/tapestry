package com.tapestry.extensions;

/**
 * Types of hooks that can be registered.
 * These are backend concepts - actual handlers come from TypeScript.
 */
public enum HookType {
    /**
     * Hook for world generation block resolution.
     */
    RESOLVE_BLOCK,
    
    /**
     * Hook for world generation chunk population.
     */
    POPULATE_CHUNK,
    
    /**
     * Hook for world generation structure building.
     */
    BUILD_STRUCTURE,
    
    /**
     * Hook for world generation finalization.
     */
    FINALIZE_WORLD
}
