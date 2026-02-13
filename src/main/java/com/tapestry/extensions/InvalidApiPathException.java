package com.tapestry.extensions;

/**
 * Exception thrown when an API path doesn't follow required namespace conventions.
 */
public class InvalidApiPathException extends ExtensionRegistrationException {
    
    public InvalidApiPathException(String message, String extensionId) {
        super(message, extensionId);
    }
}
