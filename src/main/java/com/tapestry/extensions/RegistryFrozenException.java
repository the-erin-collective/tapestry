package com.tapestry.extensions;

/**
 * Thrown when attempting to register after the registry is frozen.
 */
public class RegistryFrozenException extends ExtensionRegistrationException {
    
    public RegistryFrozenException() {
        super("API registry is frozen - no further registrations allowed");
    }
}
