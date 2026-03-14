package com.tapestry.gameplay.traits;

import com.tapestry.gameplay.GameplayAPI;

/**
 * Static facade used by external code (for example, TypeScript mods) to
 * register traits and consumption rules without needing a GameplayAPI
 * instance reference. All operations are forwarded to the API object's
 * trait system.
 */
public final class TraitAPI {
    private TraitAPI() {
        // prevent instantiation
    }

    /**
     * Registers a trait with the global gameplay API.
     *
     * @param name the trait name
     * @param config configuration object (may be null)
     */
    public static void register(String name, TraitConfig config) {
        GameplayAPI.getInstance().getTraits().register(name, config);
    }

    /**
     * Declares that an entity consumes a particular trait for a behavior.
     *
     * @param name the trait name
     * @param config consumption configuration (must not be null)
     */
    public static void consume(String name, ConsumptionConfig config) {
        GameplayAPI.getInstance().getTraits().consume(name, config);
    }
}
