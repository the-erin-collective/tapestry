package com.tapestry.lifecycle;

/**
 * Represents the explicit lifecycle phases of the Tapestry framework.
 * 
 * Phases advance monotonically and never regress. Only one phase is active at any time.
 * Each phase defines what operations are allowed and forbidden.
 */
public enum TapestryPhase {
    /**
     * JVM is running. Tapestry core initializes.
     * No Fabric interaction yet.
     */
    BOOTSTRAP,
    
    /**
     * Tapestry-aware extensions are discovered via Fabric entrypoints.
     * Must be side-effect free.
     */
    DISCOVERY,
    
    /**
     * Extensions are discovered and validated with no backend side effects.
     * Pure validation stage - no mutations allowed.
     */
    VALIDATION,
    
    /**
     * Extensions are allowed to define API shape.
     * This is the only phase where the API surface may change.
     */
    REGISTRATION,
    
    /**
     * API surface is sealed permanently.
     * Only validation and consistency checks allowed.
     */
    FREEZE,
    
    /**
     * TypeScript runtime is initialized.
     * JS engine starts and frozen API object is injected.
     * No user TS code execution yet.
     */
    TS_LOAD,
    
    /**
     * Mod registration phase.
     * All mod scripts are evaluated and metadata is collected.
     * Only mod.define() calls are allowed.
     */
    TS_REGISTER,
    
    /**
     * Mod activation phase.
     * Dependency graph is resolved and mods are activated in order.
     * mod.export() and mod.require() are allowed.
     */
    TS_ACTIVATE,
    
    /**
     * TypeScript setup is allowed.
     * Safe setup window for TypeScript mods.
     */
    TS_READY,
    
    /**
     * Persistence layer is initialized.
     * Mod state is loaded and ready for runtime access.
     * This phase is always executed, becomes no-op if unused.
     */
    PERSISTENCE_READY,
    
    /**
     * Server is live and gameplay begins.
     * Normal mod operation.
     */
    RUNTIME,
    
    /**
     * Client presentation layer is initialized.
     * Overlay registration and rendering is allowed.
     * This phase is client-side only.
     */
    CLIENT_PRESENTATION_READY;
    
    /**
     * Returns true if this phase comes after the given phase.
     * 
     * @param other the phase to compare against
     * @return true if this phase is later in the lifecycle
     */
    public boolean isAfter(TapestryPhase other) {
        return this.ordinal() > other.ordinal();
    }
    
    /**
     * Returns true if this phase comes before the given phase.
     * 
     * @param other the phase to compare against
     * @return true if this phase is earlier in the lifecycle
     */
    public boolean isBefore(TapestryPhase other) {
        return this.ordinal() < other.ordinal();
    }
    
    /**
     * Returns true if this phase is the given phase or comes after it.
     * 
     * @param other the phase to compare against
     * @return true if this phase is the given phase or later
     */
    public boolean isAtOrAfter(TapestryPhase other) {
        return this.ordinal() >= other.ordinal();
    }
    
    /**
     * Returns true if this phase is the given phase or comes before it.
     * 
     * @param other the phase to compare against
     * @return true if this phase is the given phase or earlier
     */
    public boolean isAtOrBefore(TapestryPhase other) {
        return this.ordinal() <= other.ordinal();
    }
}
