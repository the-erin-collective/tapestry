package com.tapestry.extensions;

import java.util.Map;

/**
 * A claim by an extension that it wants to add a named capability in some domain.
 */
public record CapabilityDecl(
    String name,                     // e.g. "worldgen.onResolveBlock"
    CapabilityType type,             // HOOK | API | SERVICE
    boolean exclusive,               // explicit exclusivity flag
    Map<String, Object> meta         // optional structured metadata
) {}
