package com.tapestry.gameplay.lifecycle;

/**
 * Lifecycle phases for gameplay API.
 * 
 * These phases extend the core Tapestry lifecycle with gameplay-specific phases.
 * Note: This is a conceptual representation. The actual phases are managed by
 * the core TapestryPhase enum.
 */
public enum LifecyclePhase {
    /**
     * TypeScript registration phase.
     * Items, traits, brewing recipes, and loot modifiers are registered.
     */
    TS_REGISTER,
    
    /**
     * Trait validation phase.
     * Trait definitions are validated and prepared.
     */
    TRAITS,
    
    /**
     * Composition phase.
     * Trait resolution, tag generation, and registry freezing.
     */
    COMPOSITION,
    
    /**
     * Initialization phase.
     * Service capability resolution and vanilla patches applied.
     */
    INITIALIZATION,
    
    /**
     * Runtime phase.
     * Normal gameplay, no registration allowed.
     */
    RUNTIME
}
