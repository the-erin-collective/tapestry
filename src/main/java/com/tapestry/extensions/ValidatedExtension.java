package com.tapestry.extensions;

import net.fabricmc.loader.api.ModContainer;

import java.util.List;

/**
 * A successfully validated extension ready for registration.
 */
public record ValidatedExtension(
    TapestryExtensionDescriptor descriptor,
    TapestryExtensionProvider provider,
    ModContainer sourceMod,
    List<CapabilityDecl> capabilitiesResolved,
    List<String> resolvedDependencies
) {}
