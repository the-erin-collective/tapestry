package com.tapestry.gameplay.patch;

import java.util.Arrays;

/**
 * Determines whether a patch set should be applied based on runtime conditions.
 * 
 * <p>Conditions are evaluated after datapack content is loaded, ensuring that
 * registry-dependent conditions can access datapack data. This allows patches
 * to be conditionally applied based on:</p>
 * <ul>
 *   <li>Mod presence - apply only when specific mods are loaded</li>
 *   <li>Registry contents - apply only when specific items/blocks exist</li>
 *   <li>Trait existence - apply only when specific traits are registered</li>
 * </ul>
 * 
 * <p>Conditions can be composed using logical combinators (and, or) to create
 * complex conditional logic.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Apply only when a specific mod is loaded
 * PatchCondition condition = PatchCondition.modLoaded("example_mod");
 * 
 * // Apply only when both conditions are met
 * PatchCondition combined = PatchCondition.and(
 *     PatchCondition.modLoaded("example_mod"),
 *     PatchCondition.registryContains(new Identifier("example:item"))
 * );
 * }</pre>
 */
public interface PatchCondition {
    /**
     * Evaluates this condition against the provided context.
     * 
     * @param context The patch context providing environment information
     * @return true if the condition is satisfied, false otherwise
     */
    boolean evaluate(PatchContext context);

    /**
     * Performs a lightweight validation of this condition.
     *
     * <p>The default implementation does nothing.  Conditions that embed
     * identifiers should override to ensure the identifier is non-null and,
     * when possible, that it exists in the provided context.</p>
     *
     * @param context context used for registry lookups
     * @throws IllegalArgumentException if validation fails
     */
    default void validate(PatchContext context) throws IllegalArgumentException {
        // no-op
    }
    
    /**
     * Creates a condition that checks if a mod is loaded.
     * 
     * @param modId The mod identifier to check
     * @return A condition that evaluates to true if the mod is loaded
     */
    static PatchCondition modLoaded(String modId) {
        return new PatchCondition() {
            @Override
            public boolean evaluate(PatchContext context) {
                return context.isModLoaded(modId);
            }

            @Override
            public void validate(PatchContext context) {
                if (modId == null || modId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Mod ID cannot be null or empty");
                }
            }
        };
    }
    
    /**
     * Creates a condition that checks if a registry contains a specific entry.
     * 
     * <p>This condition is evaluated after datapack load, ensuring that
     * datapack-provided registry entries are available.</p>
     * 
     * @param id The identifier to check in the registry
     * @return A condition that evaluates to true if the registry contains the entry
     */
    static PatchCondition registryContains(net.minecraft.util.Identifier id) {
        return new PatchCondition() {
            @Override
            public boolean evaluate(PatchContext context) {
                return context.registryContains(id);
            }

            @Override
            public void validate(PatchContext context) {
                if (id == null) {
                    throw new IllegalArgumentException("Registry identifier cannot be null");
                }
                if (!context.registryContains(id)) {
                    throw new IllegalArgumentException("Registry does not contain " + id);
                }
            }
        };
    }
    
    /**
     * Creates a condition that checks if a trait exists.
     * 
     * @param traitId The trait identifier to check
     * @return A condition that evaluates to true if the trait exists
     */
    static PatchCondition traitExists(net.minecraft.util.Identifier traitId) {
        return new PatchCondition() {
            @Override
            public boolean evaluate(PatchContext context) {
                return context.traitExists(traitId);
            }

            @Override
            public void validate(PatchContext context) {
                if (traitId == null) {
                    throw new IllegalArgumentException("Trait identifier cannot be null");
                }
            }
        };
    }
    
    /**
     * Creates a condition that evaluates to true only if all provided conditions are true.
     * 
     * <p>This implements logical AND - all conditions must be satisfied.</p>
     * 
     * @param conditions The conditions to combine with AND logic
     * @return A condition that evaluates to true if all conditions are true
     */
    static PatchCondition and(PatchCondition... conditions) {
        return context -> Arrays.stream(conditions)
            .allMatch(c -> c.evaluate(context));
    }
    
    /**
     * Creates a condition that evaluates to true if any provided condition is true.
     * 
     * <p>This implements logical OR - at least one condition must be satisfied.</p>
     * 
     * @param conditions The conditions to combine with OR logic
     * @return A condition that evaluates to true if any condition is true
     */
    static PatchCondition or(PatchCondition... conditions) {
        return context -> Arrays.stream(conditions)
            .anyMatch(c -> c.evaluate(context));
    }
}
