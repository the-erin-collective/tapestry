package com.tapestry.gameplay.loot.operations;

import com.tapestry.gameplay.loot.filter.LootPoolFilter;
import com.tapestry.gameplay.patch.PatchOperation;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.LootPoolEntry;

import java.util.Objects;
import java.util.Optional;

/**
 * A patch operation that adds a new entry to loot pools matching a filter.
 * 
 * <p>This operation uses a {@link LootPoolFilter} to identify which pools should receive
 * the new entry. The entry is added to all pools matching the filter criteria.</p>
 * 
 * <p>This operation is stateless and deterministic - adding the same entry to the same
 * pools will always produce the same result.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * LootPoolFilter poolFilter = new LootPoolFilter(
 *     Optional.of("main"),
 *     Optional.empty(),
 *     Optional.empty()
 * );
 * LootPoolEntry entry = // ... create loot entry
 * AddEntryOperation operation = new AddEntryOperation(poolFilter, entry);
 * operation.apply(lootTable);
 * }</pre>
 * 
 * @see PatchOperation
 * @see LootTable
 * @see LootPoolFilter
 * @see LootPoolEntry
 */
public record AddEntryOperation(
    LootPoolFilter poolFilter,
    LootPoolEntry entry
) implements PatchOperation<LootTable> {
    
    /**
     * Compact constructor that validates parameters are not null.
     * 
     * @throws NullPointerException if poolFilter or entry is null
     */
    public AddEntryOperation {
        Objects.requireNonNull(poolFilter, "Loot pool filter cannot be null");
        Objects.requireNonNull(entry, "Loot entry cannot be null");
    }
    
    /**
     * Applies this operation by adding the entry to all pools matching the filter.
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
            
            // Filter pools and add entry to matching ones
            var predicate = poolFilter.toPredicate();
            for (LootPool pool : pools) {
                if (predicate.test(pool)) {
                    // Access the entries field using reflection
                    var entriesField = LootPool.class.getDeclaredField("entries");
                    entriesField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    var entries = (java.util.List<LootPoolEntry>) entriesField.get(pool);
                    entries.add(entry);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to add entry to loot pools", e);
        }
    }
    
    /**
     * Returns a debug identifier for this operation.
     * 
     * @return A debug string describing the pool filter and entry
     */
    @Override
    public Optional<String> getDebugId() {
        StringBuilder criteria = new StringBuilder("AddEntry[poolFilter={");
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
        
        criteria.append("}, entry=").append(entry).append("]");
        return Optional.of(criteria.toString());
    }
}
