package com.tapestry.mod;

/**
 * Exception thrown when mod activation fails.
 * 
 * Provides improved developer experience by wrapping raw activation exceptions
 * with clear mod identification and context.
 */
public class ModActivationException extends RuntimeException {
    
    private final String modId;
    
    public ModActivationException(String modId, String message) {
        super(String.format("Mod activation failed for '%s': %s", modId, message));
        this.modId = modId;
    }
    
    public ModActivationException(String modId, String message, Throwable cause) {
        super(String.format("Mod activation failed for '%s': %s", modId, message), cause);
        this.modId = modId;
    }
    
    public ModActivationException(String modId, Throwable cause) {
        super(String.format("Mod activation failed for '%s': %s", modId, cause.getMessage()), cause);
        this.modId = modId;
    }
    
    public String getModId() {
        return modId;
    }
    
    /**
     * Gets the original cause if available, providing access to the root
     * activation error for debugging purposes.
     */
    public Throwable getOriginalCause() {
        return getCause();
    }
}
