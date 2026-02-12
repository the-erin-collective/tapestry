package com.tapestry.extensions;

/**
 * Base exception for all extension registration failures.
 */
public class ExtensionRegistrationException extends RuntimeException {
    
    public ExtensionRegistrationException(String message) {
        super(message);
    }
    
    public ExtensionRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ExtensionRegistrationException(String message, String extensionId) {
        super(message + " (extension: " + extensionId + ")");
    }
}
