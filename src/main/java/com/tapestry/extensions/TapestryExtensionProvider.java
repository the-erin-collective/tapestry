package com.tapestry.extensions;

/**
 * Provider interface for Tapestry extensions.
 * Providers must be pure at descriptor time.
 */
public interface TapestryExtensionProvider {
    TapestryExtensionDescriptor describe();
    
    /**
     * Creates the actual extension instance for registration.
     * This method is called during capability registration.
     * 
     * @return the extension instance
     */
    TapestryExtension create();
}
