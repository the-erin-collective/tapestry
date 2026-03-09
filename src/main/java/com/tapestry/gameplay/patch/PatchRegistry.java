package com.tapestry.gameplay.patch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores all registered patch sets and manages registration lifecycle.
 * 
 * <p>The PatchRegistry accepts patch set registrations during the TS_REGISTER phase
 * and then freezes to prevent further modifications. Once frozen, the registry
 * provides read-only access to registered patches indexed by target.</p>
 * 
 * <p>The registry indexes patches by target for efficient O(1) lookup during
 * patch application. Multiple patch sets can target the same gameplay object,
 * and they will be sorted by priority during compilation.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * PatchRegistry registry = new PatchRegistry();
 * 
 * // During TS_REGISTER phase
 * registry.register(tradePatchSet);
 * registry.register(lootPatchSet);
 * 
 * // At end of TS_REGISTER phase
 * registry.freeze();
 * 
 * // Later, during patch application
 * List<PatchSet<TradeTable>> patches = registry.getPatchesFor(tradeTarget);
 * }</pre>
 */
public class PatchRegistry {
    private static volatile PatchRegistry instance;

    private final Map<PatchTarget<?>, List<PatchSet<?>>> patchesByTarget;
    private boolean frozen = false;

    /**
     * Creates a new empty patch registry.
     *
     * <p>For production use, prefer {@link #getInstance()} to get the singleton instance.
     * This constructor is public to support testing with isolated registry instances.</p>
     */
    public PatchRegistry() {
        this.patchesByTarget = new HashMap<>();
    }

    /**
     * Gets the singleton instance of the patch registry.
     *
     * <p>This is the primary way to access the registry in production code.
     * The instance is created lazily on first access.</p>
     *
     * @return the singleton PatchRegistry instance
     */
    public static PatchRegistry getInstance() {
        PatchRegistry result = instance;
        if (result == null) {
            synchronized (PatchRegistry.class) {
                if (instance == null) {
                    instance = result = new PatchRegistry();
                }
            }
        }
        return result;
    }

    /**
     * Registers a patch set with this registry.
     *
     * <p>The patch set is indexed by its target for efficient lookup. Multiple
     * patch sets can target the same gameplay object.</p>
     *
     * @param <T> The type of gameplay data the patch set modifies
     * @param patchSet The patch set to register
     * @throws IllegalStateException if the registry is frozen
     * @throws IllegalArgumentException if the target identifier is null
     */
    public <T> void register(PatchSet<T> patchSet) {
        if (frozen) {
            throw new IllegalStateException(
                "Cannot register patches after TS_REGISTER phase"
            );
        }

        // Validate target identifier
        if (patchSet.target().id() == null) {
            throw new IllegalArgumentException("Target identifier cannot be null");
        }
        // Validate priority range
        int priority = patchSet.priority();
        if (priority < -1000 || priority > 1000) {
            throw new IllegalArgumentException("Priority must be between -1000 and 1000, got: " + priority);
        }
        // Validate operations list
        if (patchSet.operations() == null || patchSet.operations().isEmpty()) {
            throw new IllegalArgumentException("PatchSet must contain at least one operation");
        }

        // Index by target for efficient lookup
        patchesByTarget
            .computeIfAbsent(patchSet.target(), k -> new ArrayList<>())
            .add(patchSet);
    }

    /**
     * Freezes this registry, preventing further registrations.
     *
     * <p>This method should be called at the end of the TS_REGISTER phase.
     * After freezing, any attempt to register new patch sets will throw
     * an IllegalStateException.</p>
     */
    public void freeze() {
        frozen = true;
    }

    /**
     * Checks if this registry is frozen.
     *
     * @return true if the registry is frozen and no longer accepts registrations
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Retrieves all patch sets registered for the given target.
     *
     * <p>Returns an empty list if no patches are registered for the target.
     * The returned list is a view of the internal storage and should not be
     * modified by callers.</p>
     *
     * @param <T> The type of gameplay data the target represents
     * @param target The target to retrieve patches for
     * @return A list of patch sets for the target, or an empty list if none exist
     */
    public <T> List<PatchSet<T>> getPatchesFor(PatchTarget<T> target) {
        List<PatchSet<?>> rawPatches = patchesByTarget.getOrDefault(target, List.of());
        @SuppressWarnings("unchecked")
        List<PatchSet<T>> patches = (List<PatchSet<T>>) (List<?>) rawPatches;
        return patches;
    }

    /**
     * Returns an unmodifiable view of all registered patches.
     *
     * <p>The returned map is indexed by target and contains all patch sets
     * registered with this registry. The map cannot be modified.</p>
     *
     * @return An unmodifiable map of all patches indexed by target
     */
    public Map<PatchTarget<?>, List<PatchSet<?>>> getAllPatches() {
        return Collections.unmodifiableMap(patchesByTarget);
    }

    /**
     * Resets the singleton instance for testing purposes.
     *
     * <p>This method should only be used in test code to ensure test isolation.</p>
     */
    public static void reset() {
        synchronized (PatchRegistry.class) {
            instance = null;
        }
    }
}

