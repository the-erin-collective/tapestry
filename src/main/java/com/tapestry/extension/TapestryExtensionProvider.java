package com.tapestry.extension;

/**
 * Interface for Tapestry extensions that want to register APIs with the framework.
 * 
 * Extensions are discovered via the "tapestry:extension" Fabric entrypoint.
 * Each extension must implement this interface and be declared in fabric.mod.json.
 */
public interface TapestryExtensionProvider {
    
    /**
     * Describes the extension and its capabilities.
     * This method must be side-effect free and should not access any Tapestry APIs.
     * It's called during the DISCOVERY phase.
     * 
     * @return a descriptor describing this extension
     */
    TapestryExtensionDescriptor describe();
    
    /**
     * Registers the extension's APIs with the framework.
     * This method is called only during the REGISTRATION phase.
     * The provided context allows extending domains and registering mod-owned APIs.
     * 
     * @param context the registration context for this extension
     */
    void register(TapestryExtensionContext context);
}
