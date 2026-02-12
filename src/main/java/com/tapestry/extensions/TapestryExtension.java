package com.tapestry.extensions;

/**
 * Main extension interface for capability registration.
 * 
 * Phase 3: describe() - returns validated descriptor
 * Registration: register() - registers capabilities with strict validation
 */
public interface TapestryExtension {
    
    /**
     * @return the extension descriptor (Phase 3 output)
     */
    TapestryExtensionDescriptor describe();
    
    /**
     * Registers this extension's capabilities with the given context.
     * 
     * @param ctx the registration context providing access to registries
     * @throws ExtensionRegistrationException if registration fails
     */
    void register(TapestryExtensionContext ctx) throws ExtensionRegistrationException;
}
