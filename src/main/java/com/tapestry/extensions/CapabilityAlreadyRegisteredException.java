package com.tapestry.extensions;

/**
 * Thrown when a capability is already registered.
 */
public class CapabilityAlreadyRegisteredException extends ExtensionRegistrationException {
    
    public CapabilityAlreadyRegisteredException(String capabilityName, String extensionId) {
        super("Capability '" + capabilityName + "' already registered by extension '" + extensionId + "'");
    }
}
