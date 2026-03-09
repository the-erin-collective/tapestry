package com.tapestry.gameplay.loot.operations;

import com.tapestry.gameplay.patch.PatchOperation;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;

import java.util.Objects;
import java.util.Optional;

/**
 * A patch operation that adds a new loot pool to a loot table.
 * 
 * <p>This operation is stateless and deterministic - adding the same loot pool
 * to the same loot table will always produce the same result.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * LootPool pool = // ... create loot pool
 * AddPoolOperation operation = new AddPoolOperation(pool);
 * operation.apply(lootTable);
 * }</pre>
 * 
 * @see PatchOperation
 * @see LootTable
 * @see LootPool
 */
public record AddPoolOperation(LootPool pool) implements PatchOperation<LootTable> {
    
    /**
     * Compact constructor that validates the loot pool is not null.
     * 
     * @throws NullPointerException if pool is null
     */
    public AddPoolOperation {
        Objects.requireNonNull(pool, "Loot pool cannot be null");
    }
    
    /**
     * Applies this operation by adding the loot pool to the target loot table.
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
            pools.add(pool);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to add loot pool to loot table", e);
        }
    }
    
    /**
     * Returns a debug identifier for this operation.
     * 
     * @return A debug string describing the pool being added
     */
    @Override
    public Optional<String> getDebugId() {
        return Optional.of("AddPool[pool=" + pool + "]");
    }
}
