package com.tapestry.extensions;

/**
 * Provider interface for Tapestry extensions.
 * Providers must be pure at descriptor time.
 */
public interface TapestryExtensionProvider {
    TapestryExtensionDescriptor describe();
}
