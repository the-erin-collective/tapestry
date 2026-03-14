package com.tapestry.behavior;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry that allows other parts of the code (or extensions) to declare
 * that a Tapestry tag should be merged into a vanilla tag during datapack
 * loading.  This enables the optional "trait-driven" injection described in
 * the deferred features spec.
 */
public class VanillaTagMergeRegistry {
    private static final Map<String, String> merges = new HashMap<>();

    private VanillaTagMergeRegistry() { }

    /**
     * Register a merge from a tapestry tag into a vanilla tag.
     *
     * @param tapestryTag  full identifier of the tapestry-generated tag
     * @param vanillaTag   full identifier of the vanilla tag to extend
     */
    public static void register(String tapestryTag, String vanillaTag) {
        merges.put(tapestryTag, vanillaTag);
    }

    /**
     * Returns an unmodifiable view of registered merges.
     */
    public static Map<String, String> getMerges() {
        return Collections.unmodifiableMap(merges);
    }
}
