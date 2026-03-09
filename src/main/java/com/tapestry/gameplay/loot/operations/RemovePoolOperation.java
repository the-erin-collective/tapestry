package com.tapestry.gameplay.loot.operations;

import com.tapestry.gameplay.loot.filter.LootPoolFilter;
import com.tapestry.gameplay.patch.PatchOperation;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;

import java.util.Objects;
import java.util.Optional;

/**
 * A patch operation that removes loot pools matching a filter from a loot table.
 * 
 * <p>This operation uses a {@link LootPoolFilter} to identify which pools should be removed.
 * All pools matching the filter criteria will be removed from the table.</p>
 * 
 * <p>This operation is stateless and deterministic - applying the same filter to the same
 * loot table will always remove the same pools.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * LootPoolFilter filter = new LootPoolFilter(
 *     Optional.of("main"),
 *     Optional.empty(),
 *     Optional.empty()
 * );
 * RemovePoolOperation operation = new RemovePoolOperation(filter);
 * operation.apply(lootTable);
 * }</pre>
 * 
 * @see PatchOperation
 * @see LootTable
 * @see LootPoolFilter
 */
public record RemovePoolOperation(LootPoolFilter filter) implements PatchOperation<LootTable> {
    
    /**
     * Compact constructor that validates the filter is not null.
     * 
     * @throws NullPointerException if filter is null
     */
    public RemovePoolOperation {
        Objects.requireNonNull(filter, "Loot pool filter cannot be null");
    }
    
    /**
     * Applies this operation by removing all pools matching the filter from the target loot table.
     * 
     * @param target The loot table to modify
     * @throws NullPointerException if target is null
     */
    @Override
    public void apply(LootTable target) {
        Objects.requireNonNull(target, "Loot table cannot be null");
        
        try {
            // Access the pools field using reflection
            var poolsField = LootTable.class.getDeclaredField("pools");
            poolsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var pools = (java.util.List<LootPool>) poolsField.get(target);
            pools.removeIf(filter.toPredicate());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to remove loot pools from loot table", e);
        }
    }
    
    /**
     * Returns a debug identifier for this operation.
     * 
     * @return A debug string describing the filter criteria
     */
    @Override
    public Optional<String> getDebugId() {
        StringBuilder criteria = new StringBuilder("RemovePool[");
        boolean first = true;
        
        if (filter.name().isPresent()) {
            criteria.append("name=").append(filter.name().get());
            first = false;
        }
        if (filter.rolls().isPresent()) {
            if (!first) criteria.append(", ");
            criteria.append("rolls=").append(filter.rolls().get());
            first = false;
        }
        if (filter.bonusRolls().isPresent()) {
            if (!first) criteria.append(", ");
            criteria.append("bonusRolls=").append(filter.bonusRolls().get());
        }
        
        criteria.append("]");
        return Optional.of(criteria.toString());
    }
}
