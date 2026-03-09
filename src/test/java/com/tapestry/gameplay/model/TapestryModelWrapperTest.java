package com.tapestry.gameplay.model;

import com.tapestry.gameplay.trades.filter.TradeEntry;
import com.tapestry.gameplay.trades.TradeTable;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.DummyLootTable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class TapestryModelWrapperTest {

    @org.junit.jupiter.api.Disabled("Minecraft classes require bootstrap, skip in unit tests")
    @Test
    void tapestryLootTable_wrapAndUnwrap() {
        // create a minimal loot table via our test subclass with public constructor
        LootTable vanilla = new DummyLootTable();
        TapestryLootTable wrapper = new TapestryLootTable(vanilla);
        assertSame(vanilla, wrapper.unwrap());

        // modify using wrapper and verify underlying list changes; use null as a sentinel value
        wrapper.addPool(null);
        assertTrue(getPools(vanilla).contains(null));

        wrapper.removePool(p -> p == null);
        assertFalse(getPools(vanilla).contains(null));

    }

    @Test
    void tapestryTradeTable_basicOperations() {
        // simple in-memory trade table for testing
        TradeTable base = new TradeTable() {
            private final List<TradeEntry> entries = new ArrayList<>();

            @Override
            public void add(TradeEntry entry) {
                entries.add(entry);
            }

            @Override
            public boolean removeIf(Predicate<TradeEntry> predicate) {
                return entries.removeIf(predicate);
            }

            @Override
            public java.util.stream.Stream<TradeEntry> stream() {
                return entries.stream();
            }
        };

        TapestryTradeTable wrapper = new TapestryTradeTable(base);
        assertSame(base, wrapper.unwrap());

        TradeEntry entry = new TradeEntry() {
            @Override public net.minecraft.util.Identifier getInputItem() { return null; }
            @Override public boolean hasInputTag(String tag) { return false; }
            @Override public net.minecraft.util.Identifier getOutputItem() { return null; }
            @Override public boolean hasOutputTag(String tag) { return false; }
            @Override public int getLevel() { return 0; }
            @Override public int getMaxUses() { return 0; }
            @Override public void setInputItem(net.minecraft.util.Identifier inputItem) {}
            @Override public void setOutputItem(net.minecraft.util.Identifier outputItem) {}
        };
        wrapper.addTrade(entry);
        assertTrue(base.stream().anyMatch(e -> e == entry));

        boolean removed = wrapper.removeIf(e -> e == entry);
        assertTrue(removed);
        assertFalse(base.stream().findAny().isPresent());
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<LootPool> getPools(LootTable table) {
        try {
            java.lang.reflect.Field field = LootTable.class.getDeclaredField("pools");
            field.setAccessible(true);
            return (java.util.List<LootPool>) field.get(table);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
