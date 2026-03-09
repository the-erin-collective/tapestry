package com.tapestry.gameplay.model;

import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;

import java.util.function.Predicate;

/**
 * A convenience wrapper around Minecraft's {@link LootTable} that exposes
 * an API more suitable for gameplay patch operations. This is an example of
 * an optional {@link TapestryModel} implementation; additional wrappers can
 * be added for other data types such as trade tables.
 */
public class TapestryLootTable implements TapestryModel<LootTable> {
    private final LootTable vanilla;

    public TapestryLootTable(LootTable vanilla) {
        if (vanilla == null) {
            throw new NullPointerException("LootTable cannot be null");
        }
        this.vanilla = vanilla;
    }

    /**
     * Adds a pool to the underlying loot table.
     *
     * @param pool the pool to add
     */
    @SuppressWarnings("unchecked")
    public void addPool(LootPool pool) {
        try {
            java.lang.reflect.Field field = LootTable.class.getDeclaredField("pools");
            field.setAccessible(true);
            ((java.util.List<LootPool>) field.get(vanilla)).add(pool);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to add pool via reflection", e);
        }
    }

    /**
     * Removes pools that match the provided predicate.
     *
     * @param predicate predicate used to test existing pools
     */
    @SuppressWarnings("unchecked")
    public void removePool(Predicate<LootPool> predicate) {
        try {
            java.lang.reflect.Field field = LootTable.class.getDeclaredField("pools");
            field.setAccessible(true);
            ((java.util.List<LootPool>) field.get(vanilla)).removeIf(predicate);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to remove pool via reflection", e);
        }
    }

    @Override
    public LootTable unwrap() {
        return vanilla;
    }
}
