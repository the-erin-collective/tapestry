package com.tapestry.gameplay.patch;

/**
 * Exception thrown when a patch operation fails to apply.
 * This is a RuntimeException to allow for flexible error handling strategies.
 */
public class PatchApplicationException extends RuntimeException {
    /**
     * Creates a new PatchApplicationException with a message and cause.
     * 
     * @param message The error message
     * @param cause The underlying exception that caused the failure
     */
    public PatchApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new PatchApplicationException with only a message.
     * 
     * @param message The error message
     */
    public PatchApplicationException(String message) {
        super(message);
    }
}
