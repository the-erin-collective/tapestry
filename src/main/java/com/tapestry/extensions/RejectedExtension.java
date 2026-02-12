package com.tapestry.extensions;

import net.fabricmc.loader.api.ModContainer;

import java.util.List;

/**
 * A rejected extension with validation errors.
 */
public record RejectedExtension(
    TapestryExtensionDescriptor descriptor,
    ModContainer sourceMod,
    List<ValidationMessage> errors
) {}
