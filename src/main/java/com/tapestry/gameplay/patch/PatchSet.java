package com.tapestry.gameplay.patch;

import net.minecraft.util.Identifier;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Groups related patch operations applied to the same target.
 * 
 * <p>A PatchSet represents a collection of operations registered by a single mod
 * that all target the same gameplay object. Patch sets are sorted by priority,
 * mod load order, and registration order to ensure deterministic application.</p>
 * 
 * <p>Patch sets can optionally include a condition that determines whether the
 * operations should be applied. If no condition is provided, the patch set is
 * always applied.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * PatchTarget<TradeTable> target = new PatchTarget<>(
 *     new Identifier("minecraft:villager/farmer"),
 *     TradeTable.class
 * );
 * 
 * List<PatchOperation<TradeTable>> operations = List.of(
 *     new AddTradeOperation(tradeEntry),
 *     new RemoveTradeOperation(filter)
 * );
 * 
 * PatchSet<TradeTable> patchSet = new PatchSet<>(
 *     new Identifier("mymod:farmer_trades"),
 *     target,
 *     PatchPriority.NORMAL,
 *     operations,
 *     Optional.empty()
 * );
 * }</pre>
 * 
 * @param <T> The type of gameplay data this patch set modifies
 * @param modId The identifier of the mod registering this patch set
 * @param target The target gameplay object to modify
 * @param priority The priority value for ordering (lower values apply first)
 * @param operations The list of operations to apply to the target
 * @param condition Optional condition determining whether to apply this patch set
 */
public record PatchSet<T>(
    Identifier modId,
    PatchTarget<T> target,
    int priority,
    List<PatchOperation<T>> operations,
    Optional<PatchCondition> condition
) {
    /**
     * Compact constructor that validates fields and creates defensive copies.
     * 
     * <p>This constructor ensures that:</p>
     * <ul>
     *   <li>All required fields are non-null</li>
     *   <li>The operations list is defensively copied to prevent external modification</li>
     *   <li>The condition is a non-null Optional (may be empty)</li>
     * </ul>
     * 
     * @throws NullPointerException if any required field is null
     */
    public PatchSet {
        Objects.requireNonNull(modId, "Mod identifier cannot be null");
        Objects.requireNonNull(target, "Patch target cannot be null");
        Objects.requireNonNull(operations, "Operations list cannot be null");
        Objects.requireNonNull(condition, "Condition must be non-null Optional");
        
        // Priority bounds enforced at registration time as well
        if (priority < -1000 || priority > 1000) {
            throw new IllegalArgumentException("Priority must be between -1000 and 1000, got: " + priority);
        }
        
        if (operations.isEmpty()) {
            throw new IllegalArgumentException("Operations list cannot be empty");
        }
        
        // Defensive copy to prevent external modification
        operations = List.copyOf(operations);
    }
    
    /**
     * Determines whether this patch set should be applied based on its condition.
     * 
     * <p>If no condition is present (empty Optional), this method returns true,
     * meaning the patch set is always applied. If a condition is present, it is
     * evaluated against the provided context.</p>
     * 
     * @param context The patch context providing environment information
     * @return true if the patch set should be applied, false otherwise
     */
    public boolean shouldApply(PatchContext context) {
        return condition.map(c -> c.evaluate(context)).orElse(true);
    }
}
