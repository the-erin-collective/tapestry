package com.tapestry.gameplay.loot.operations;

import com.tapestry.gameplay.loot.filter.LootEntryFilter;
import com.tapestry.gameplay.loot.filter.LootPoolFilter;
import com.tapestry.gameplay.patch.PatchOperation;
import com.tapestry.gameplay.patch.PatchContext;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.LootPoolEntry;

import java.util.Objects;
import java.util.Optional;

/**
 * A patch operation that removes entries from loot pools matching filters.
 * 
 * <p>This operation uses a {@link LootPoolFilter} to identify which pools should be modified,
 * and a {@link LootEntryFilter} to identify which entries should be removed from those pools.</p>
 * 
 * <p>This operation is stateless and deterministic - applying the same filters to the same
 * loot table will always remove the same entries.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * LootPoolFilter poolFilter = new LootPoolFilter(
 *     Optional.of("main"),
 *     Optional.empty(),
 *     Optional.empty()
 * );
 * LootEntryFilter entryFilter = new LootEntryFilter(
 *     Optional.of(Identifier.of("minecraft:diamond")),
 *     Optional.empty()
 * );
 * RemoveEntryOperation operation = new RemoveEntryOperation(poolFilter, entryFilter);
 * operation.apply(lootTable);
 * }</pre>
 * 
 * @see PatchOperation
 * @see LootTable
 * @see LootPoolFilter
 * @see LootEntryFilter
 */
public record RemoveEntryOperation(
    LootPoolFilter poolFilter,
    LootEntryFilter entryFilter
) implements PatchOperation<LootTable> {
    
    /**
     * Compact constructor that validates parameters are not null.
     * 
     * @throws NullPointerException if poolFilter or entryFilter is null
     */
    public RemoveEntryOperation {
        Objects.requireNonNull(poolFilter, "Loot pool filter cannot be null");
        Objects.requireNonNull(entryFilter, "Loot entry filter cannot be null");
    }
    
    /**
     * Applies this operation by removing entries matching the entry filter from all pools
     * matching the pool filter.
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
            
            // Filter pools and remove entries from matching ones
            var poolPredicate = poolFilter.toPredicate();
            var entryPredicate = entryFilter.toPredicate();
            
            for (LootPool pool : pools) {
                if (poolPredicate.test(pool)) {
                    // Access the entries field using reflection
                    var entriesField = LootPool.class.getDeclaredField("entries");
                    entriesField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    var entries = (java.util.List<LootPoolEntry>) entriesField.get(pool);
                    entries.removeIf(entryPredicate);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to remove entries from loot pools", e);
        }
    }
    
    /**
     * Returns a debug identifier for this operation.
     * 
     * @return A debug string describing the pool filter and entry filter
     */
    @Override
    public void validate(PatchContext context) throws Exception {
        // validate nested filters using provided context
        poolFilter.validate(context);
        entryFilter.validate(context);
    }

    @Override
    public Optional<String> getDebugId() {
        StringBuilder criteria = new StringBuilder("RemoveEntry[poolFilter={");
        boolean first = true;
        
        if (poolFilter.name().isPresent()) {
            criteria.append("name=").append(poolFilter.name().get());
            first = false;
        }
        if (poolFilter.rolls().isPresent()) {
            if (!first) criteria.append(", ");
            criteria.append("rolls=").append(poolFilter.rolls().get());
            first = false;
        }
        if (poolFilter.bonusRolls().isPresent()) {
            if (!first) criteria.append(", ");
            criteria.append("bonusRolls=").append(poolFilter.bonusRolls().get());
        }
        
        criteria.append("}, entryFilter={");
        first = true;
        
        if (entryFilter.item().isPresent()) {
            criteria.append("item=").append(entryFilter.item().get());
            first = false;
        }
        if (entryFilter.type().isPresent()) {
            if (!first) criteria.append(", ");
            criteria.append("type=").append(entryFilter.type().get());
        }
        
        criteria.append("}]");
        return Optional.of(criteria.toString());
    }
}
