package com.tapestry.extensions;

/**
 * Thrown when an extension tries to register a capability it didn't declare.
 */
public class UndeclaredCapabilityException extends ExtensionRegistrationException {
    
    public UndeclaredCapabilityException(String capabilityName, String extensionId) {
        super("Extension '" + extensionId + "' did not declare capability '" + capabilityName + "'");
    }
}
