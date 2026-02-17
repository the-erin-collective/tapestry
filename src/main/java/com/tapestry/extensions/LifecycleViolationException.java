package com.tapestry.extensions;

/**
 * Exception thrown when an invalid lifecycle state transition is attempted.
 * 
 * This exception enforces the Phase 15 state machine invariants
 * and prevents illegal state mutations.
 */
public class LifecycleViolationException extends Exception {
    
    /**
     * Creates a new lifecycle violation exception.
     * 
     * @param message detailed error message
     */
    public LifecycleViolationException(String message) {
        super(message);
    }
    
    /**
     * Creates a new lifecycle violation exception with cause.
     * 
     * @param message detailed error message
     * @param cause the underlying cause
     */
    public LifecycleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
