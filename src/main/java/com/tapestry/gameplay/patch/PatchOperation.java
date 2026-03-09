package com.tapestry.gameplay.patch;

import java.util.Optional;

/**
 * Represents a single modification applied to a target gameplay object.
 * 
 * <p>Operations are stateless and deterministic - applying the same operation
 * to the same input data must always produce the same output. This property
 * is essential for datapack reload support.</p>
 * 
 * <p>Operations mutate the target object directly rather than creating copies,
 * avoiding expensive copying of large data structures.</p>
 * 
 * <p>Example implementations:</p>
 * <ul>
 *   <li>AddTradeOperation - adds a trade to a villager profession</li>
 *   <li>RemovePoolOperation - removes a loot pool from a loot table</li>
 *   <li>ReplaceInputOperation - changes trade input items</li>
 * </ul>
 * 
 * @param <T> The type of gameplay data this operation modifies
 */
public interface PatchOperation<T> {
    /**
     * Applies this operation to the target object.
     * 
     * <p>Operations mutate the target directly. Implementations must be
     * deterministic and stateless to support datapack reload.</p>
     * 
     * @param target The gameplay data object to modify
     * @throws PatchApplicationException if the operation fails
     */
    void apply(T target);
    
    /**
     * Returns an optional identifier for debugging purposes.
     * 
     * <p>Used in log messages to identify which operation failed or
     * produced no effect. Implementations are not required to provide
     * a debug ID.</p>
     * 
     * @return An optional debug identifier, or empty if not provided
     */
    default Optional<String> getDebugId() {
        return Optional.empty();
    }

    /**
     * Performs compilation-time validation of this operation using the provided
     * {@link PatchContext}.  This method is invoked by {@link PatchPlan#compile}
     * after patches are registered but before they are applied.
     *
     * <p>The default implementation does nothing.  Operations embedding filters or
     * identifier references should override to call those filters' validate
     * methods or perform context lookups.  Throw an exception (e.g. IllegalArgumentException
     * or {@link com.tapestry.gameplay.patch.filter.FilterValidationException}) if
     * validation fails.</p>
     *
     * @param context context used for registry lookups
     * @throws Exception if validation fails
     */
    default void validate(PatchContext context) throws Exception {
        // no-op by default
    }
}
