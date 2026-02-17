package com.tapestry.extensions;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A data-only description of an extension and what it wants to add.
 */
public record TapestryExtensionDescriptor(
    String id,                       // e.g. "infinite_dimensions"
    String displayName,              // optional
    String version,                  // optional, informational
    String minTapestry,             // semantic version, inclusive
    List<CapabilityDecl> capabilities,    // extension capabilities
    List<String> requires,           // hard dependencies only
    List<String> requiresCapabilities, // required capabilities
    Optional<String> typeExportEntry, // path to public .d.ts file (Phase 14)
    List<String> typeImports        // list of extension IDs to import types from (Phase 14)
) {}
