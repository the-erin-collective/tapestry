package com.tapestry.extensions;

/**
 * Thrown when an API path is already registered by another extension.
 */
public class DuplicateApiPathException extends ExtensionRegistrationException {
    
    public DuplicateApiPathException(String apiPath, String existingExtension, String attemptingExtension) {
        super("API path '" + apiPath + "' already registered by extension '" + existingExtension + 
               "', attempted by extension '" + attemptingExtension + "'");
    }
}
