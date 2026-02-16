package com.tapestry.extensions;

import java.util.List;
import java.util.Map;

/**
 * Represents a capability mask file for user-level structural overrides.
 * 
 * Mask files are owned by users, not mod authors, and allow
 * selective disabling of capabilities provided by mods.
 */
public record CapabilityMask(
    List<String> disable,    // capabilities to disable
    Map<String, Object> meta   // optional metadata
) {}
