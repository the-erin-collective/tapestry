package com.tapestry.extensions;

import java.util.List;
import java.util.Map;

/**
 * A data-only description of an extension and what it wants to add.
 */
public record TapestryExtensionDescriptor(
    String id,                       // e.g. "infinite_dimensions"
    String displayName,              // optional
    String version,                  // optional, informational
    String minTapestry,             // semantic version, inclusive
    List<CapabilityDecl> capabilities,
    List<String> requires,           // hard dependencies
    List<String> optional            // soft dependencies
) {}
