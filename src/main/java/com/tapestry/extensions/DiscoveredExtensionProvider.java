package com.tapestry.extensions;

import net.fabricmc.loader.api.ModContainer;

/**
 * Wrapper for discovered extension provider with its source mod container.
 */
public record DiscoveredExtensionProvider(
    TapestryExtensionProvider provider,
    ModContainer sourceMod,
    TapestryExtensionDescriptor descriptor
) {}
