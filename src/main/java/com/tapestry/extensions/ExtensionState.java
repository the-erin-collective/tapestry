package com.tapestry.extensions;

/**
 * Per-extension lifecycle state for Phase 15.
 * 
 * This enum tracks infrastructure-level state for each extension/mod.
 * It is separate from domain state and controlled exclusively by the host.
 */
public enum ExtensionState {
    
    /**
     * Extension has been discovered but not yet validated.
     * Initial state after discovery phase.
     */
    DISCOVERED,
    
    /**
     * Extension descriptor and dependency graph have been validated.
     * Extension is structurally valid.
     */
    VALIDATED,
    
    /**
     * Type contracts have been processed and resolved.
     * Phase 14 TYPE_INIT sub-step completed.
     */
    TYPE_INITIALIZED,
    
    /**
     * All registries are sealed and immutable.
     * No further structural mutation is allowed.
     */
    FROZEN,
    
    /**
     * Runtime JavaScript execution is in progress.
     * Extension is being activated.
     */
    LOADING,
    
    /**
     * Extension has successfully executed and is operational.
     * Normal runtime state.
     */
    READY,
    
    /**
     * Extension failed irrecoverably during boot.
     * Terminal state - no further transitions allowed.
     */
    FAILED;
    
    /**
     * Returns true if this state represents a successful operational state.
     * 
     * @return true if extension is ready for normal operation
     */
    public boolean isOperational() {
        return this == READY;
    }
    
    /**
     * Returns true if this state represents a failure state.
     * 
     * @return true if extension has failed
     */
    public boolean isFailed() {
        return this == FAILED;
    }
    
    /**
     * Returns true if this state allows structural mutations.
     * 
     * @return true if extension can still modify its structure
     */
    public boolean allowsStructuralMutation() {
        return this == DISCOVERED || this == VALIDATED || this == TYPE_INITIALIZED;
    }
    
    /**
     * Returns true if this state allows runtime execution.
     * 
     * @return true if extension can execute runtime code
     */
    public boolean allowsRuntimeExecution() {
        return this == LOADING || this == READY;
    }
}
