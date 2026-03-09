package com.tapestry.gameplay.trades.operations;

import com.tapestry.gameplay.trades.TradeTable;
import com.tapestry.gameplay.trades.filter.TradeEntry;
import com.tapestry.gameplay.trades.filter.TradeFilter;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for trade patch operations.
 */
class TradeOperationsTest {
    
    /**
     * Mock implementation of TradeEntry for testing.
     */
    private static class MockTradeEntry implements TradeEntry {
        private Identifier inputItem;
        private Identifier outputItem;
        private final int level;
        private final int maxUses;
        private final String inputTag;
        private final String outputTag;
        
        MockTradeEntry(Identifier inputItem, Identifier outputItem, int level, int maxUses) {
            this(inputItem, outputItem, level, maxUses, null, null);
        }
        
        MockTradeEntry(Identifier inputItem, Identifier outputItem, int level, int maxUses,
                      String inputTag, String outputTag) {
            this.inputItem = inputItem;
            this.outputItem = outputItem;
            this.level = level;
            this.maxUses = maxUses;
            this.inputTag = inputTag;
            this.outputTag = outputTag;
        }
        
        @Override
        public Identifier getInputItem() {
            return inputItem;
        }
        
        @Override
        public boolean hasInputTag(String tag) {
            return inputTag != null && inputTag.equals(tag);
        }
        
        @Override
        public Identifier getOutputItem() {
            return outputItem;
        }
        
        @Override
        public boolean hasOutputTag(String tag) {
            return outputTag != null && outputTag.equals(tag);
        }
        
        @Override
        public int getLevel() {
            return level;
        }
        
        @Override
        public int getMaxUses() {
            return maxUses;
        }
        
        @Override
        public void setInputItem(Identifier inputItem) {
            this.inputItem = inputItem;
        }
        
        @Override
        public void setOutputItem(Identifier outputItem) {
            this.outputItem = outputItem;
        }
    }
    
    /**
     * Mock implementation of TradeTable for testing.
     */
    private static class MockTradeTable implements TradeTable {
        private final List<TradeEntry> trades = new ArrayList<>();
        
        @Override
        public void add(TradeEntry entry) {
            trades.add(entry);
        }
        
        @Override
        public boolean removeIf(Predicate<TradeEntry> predicate) {
            return trades.removeIf(predicate);
        }
        
        @Override
        public Stream<TradeEntry> stream() {
            return trades.stream();
        }
        
        public int size() {
            return trades.size();
        }
        
        public TradeEntry get(int index) {
            return trades.get(index);
        }
    }
    
    private MockTradeTable tradeTable;
    
    @BeforeEach
    void setUp() {
        tradeTable = new MockTradeTable();
    }
    
    // AddTradeOperation Tests
    
    @Test
    void testAddTradeOperation_AddsTradeToTable() {
        MockTradeEntry entry = new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:wheat"),
            1,
            10
        );
        
        AddTradeOperation operation = new AddTradeOperation(entry);
        operation.apply(tradeTable);
        
        assertEquals(1, tradeTable.size(), "Trade table should contain one trade");
        assertEquals(entry, tradeTable.get(0), "Trade table should contain the added trade");
    }
    
    @Test
    void testAddTradeOperation_NullEntry_ThrowsException() {
        assertThrows(
            NullPointerException.class,
            () -> new AddTradeOperation(null),
            "AddTradeOperation should reject null entry"
        );
    }
    
    @Test
    void testAddTradeOperation_NullTarget_ThrowsException() {
        MockTradeEntry entry = new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:wheat"),
            1,
            10
        );
        
        AddTradeOperation operation = new AddTradeOperation(entry);
        
        assertThrows(
            NullPointerException.class,
            () -> operation.apply(null),
            "AddTradeOperation should reject null target"
        );
    }
    
    @Test
    void testAddTradeOperation_HasDebugId() {
        MockTradeEntry entry = new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:wheat"),
            1,
            10
        );
        
        AddTradeOperation operation = new AddTradeOperation(entry);
        
        assertTrue(operation.getDebugId().isPresent(), "AddTradeOperation should provide debug ID");
        String debugId = operation.getDebugId().get();
        assertTrue(debugId.contains("AddTrade"), "Debug ID should contain operation name");
        assertTrue(debugId.contains("minecraft:emerald"), "Debug ID should contain input item");
        assertTrue(debugId.contains("minecraft:wheat"), "Debug ID should contain output item");
    }
    
    // RemoveTradeOperation Tests
    
    @Test
    void testRemoveTradeOperation_RemovesMatchingTrades() {
        tradeTable.add(new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:wheat"),
            1,
            10
        ));
        tradeTable.add(new MockTradeEntry(
            Identifier.of("minecraft:diamond"),
            Identifier.of("minecraft:bread"),
            2,
            5
        ));
        tradeTable.add(new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:carrot"),
            1,
            10
        ));
        
        TradeFilter filter = new TradeFilter(
            Optional.of(Identifier.of("minecraft:emerald")),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1),
            Optional.empty()
        );
        
        RemoveTradeOperation operation = new RemoveTradeOperation(filter);
        operation.apply(tradeTable);
        
        assertEquals(1, tradeTable.size(), "Trade table should contain one trade after removal");
        assertEquals(
            Identifier.of("minecraft:diamond"),
            tradeTable.get(0).getInputItem(),
            "Remaining trade should be the non-matching one"
        );
    }
    
    @Test
    void testRemoveTradeOperation_NoMatchingTrades_NoChange() {
        tradeTable.add(new MockTradeEntry(
            Identifier.of("minecraft:diamond"),
            Identifier.of("minecraft:bread"),
            2,
            5
        ));
        
        TradeFilter filter = new TradeFilter(
            Optional.of(Identifier.of("minecraft:emerald")),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        
        RemoveTradeOperation operation = new RemoveTradeOperation(filter);
        operation.apply(tradeTable);
        
        assertEquals(1, tradeTable.size(), "Trade table should still contain one trade");
    }
    
    @Test
    void testRemoveTradeOperation_NullFilter_ThrowsException() {
        assertThrows(
            NullPointerException.class,
            () -> new RemoveTradeOperation(null),
            "RemoveTradeOperation should reject null filter"
        );
    }
    
    @Test
    void testRemoveTradeOperation_NullTarget_ThrowsException() {
        TradeFilter filter = new TradeFilter(
            Optional.of(Identifier.of("minecraft:emerald")),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        
        RemoveTradeOperation operation = new RemoveTradeOperation(filter);
        
        assertThrows(
            NullPointerException.class,
            () -> operation.apply(null),
            "RemoveTradeOperation should reject null target"
        );
    }
    
    @Test
    void testRemoveTradeOperation_HasDebugId() {
        TradeFilter filter = new TradeFilter(
            Optional.of(Identifier.of("minecraft:emerald")),
            Optional.empty(),
            Optional.of(Identifier.of("minecraft:wheat")),
            Optional.empty(),
            Optional.of(1),
            Optional.empty()
        );
        
        RemoveTradeOperation operation = new RemoveTradeOperation(filter);
        
        assertTrue(operation.getDebugId().isPresent(), "RemoveTradeOperation should provide debug ID");
        String debugId = operation.getDebugId().get();
        assertTrue(debugId.contains("RemoveTrade"), "Debug ID should contain operation name");
        assertTrue(debugId.contains("minecraft:emerald"), "Debug ID should contain input filter");
        assertTrue(debugId.contains("minecraft:wheat"), "Debug ID should contain output filter");
        assertTrue(debugId.contains("level=1"), "Debug ID should contain level filter");
    }
    
    // ReplaceTradeInputOperation Tests
    
    @Test
    void testReplaceTradeInputOperation_ReplacesMatchingTrades() {
        MockTradeEntry trade1 = new MockTradeEntry(
            Identifier.of("minecraft:cod"),
            Identifier.of("minecraft:emerald"),
            1,
            10
        );
        MockTradeEntry trade2 = new MockTradeEntry(
            Identifier.of("minecraft:salmon"),
            Identifier.of("minecraft:emerald"),
            1,
            10
        );
        MockTradeEntry trade3 = new MockTradeEntry(
            Identifier.of("minecraft:cod"),
            Identifier.of("minecraft:diamond"),
            2,
            5
        );
        
        tradeTable.add(trade1);
        tradeTable.add(trade2);
        tradeTable.add(trade3);
        
        TradeFilter filter = new TradeFilter(
            Optional.of(Identifier.of("minecraft:cod")),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1),
            Optional.empty()
        );
        
        ReplaceTradeInputOperation operation = new ReplaceTradeInputOperation(
            filter,
            Identifier.of("minecraft:tropical_fish")
        );
        operation.apply(tradeTable);
        
        assertEquals(
            Identifier.of("minecraft:tropical_fish"),
            trade1.getInputItem(),
            "First trade input should be replaced"
        );
        assertEquals(
            Identifier.of("minecraft:salmon"),
            trade2.getInputItem(),
            "Second trade input should not be replaced"
        );
        assertEquals(
            Identifier.of("minecraft:cod"),
            trade3.getInputItem(),
            "Third trade input should not be replaced (different level)"
        );
    }
    
    @Test
    void testReplaceTradeInputOperation_NullFilter_ThrowsException() {
        assertThrows(
            NullPointerException.class,
            () -> new ReplaceTradeInputOperation(null, Identifier.of("minecraft:emerald")),
            "ReplaceTradeInputOperation should reject null filter"
        );
    }
    
    @Test
    void testReplaceTradeInputOperation_NullNewInput_ThrowsException() {
        TradeFilter filter = new TradeFilter(
            Optional.of(Identifier.of("minecraft:cod")),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        
        assertThrows(
            NullPointerException.class,
            () -> new ReplaceTradeInputOperation(filter, null),
            "ReplaceTradeInputOperation should reject null newInput"
        );
    }
    
    @Test
    void testReplaceTradeInputOperation_NullTarget_ThrowsException() {
        TradeFilter filter = new TradeFilter(
            Optional.of(Identifier.of("minecraft:cod")),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        
        ReplaceTradeInputOperation operation = new ReplaceTradeInputOperation(
            filter,
            Identifier.of("minecraft:salmon")
        );
        
        assertThrows(
            NullPointerException.class,
            () -> operation.apply(null),
            "ReplaceTradeInputOperation should reject null target"
        );
    }
    
    @Test
    void testReplaceTradeInputOperation_HasDebugId() {
        TradeFilter filter = new TradeFilter(
            Optional.of(Identifier.of("minecraft:cod")),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1),
            Optional.empty()
        );
        
        ReplaceTradeInputOperation operation = new ReplaceTradeInputOperation(
            filter,
            Identifier.of("minecraft:salmon")
        );
        
        assertTrue(operation.getDebugId().isPresent(), "ReplaceTradeInputOperation should provide debug ID");
        String debugId = operation.getDebugId().get();
        assertTrue(debugId.contains("ReplaceTradeInput"), "Debug ID should contain operation name");
        assertTrue(debugId.contains("minecraft:cod"), "Debug ID should contain filter criteria");
        assertTrue(debugId.contains("minecraft:salmon"), "Debug ID should contain new input");
    }
    
    // ReplaceTradeOutputOperation Tests
    
    @Test
    void testReplaceTradeOutputOperation_ReplacesMatchingTrades() {
        MockTradeEntry trade1 = new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:wheat"),
            1,
            10
        );
        MockTradeEntry trade2 = new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:bread"),
            1,
            10
        );
        MockTradeEntry trade3 = new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:wheat"),
            2,
            5
        );
        
        tradeTable.add(trade1);
        tradeTable.add(trade2);
        tradeTable.add(trade3);
        
        TradeFilter filter = new TradeFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.of(Identifier.of("minecraft:wheat")),
            Optional.empty(),
            Optional.of(1),
            Optional.empty()
        );
        
        ReplaceTradeOutputOperation operation = new ReplaceTradeOutputOperation(
            filter,
            Identifier.of("minecraft:carrot")
        );
        operation.apply(tradeTable);
        
        assertEquals(
            Identifier.of("minecraft:carrot"),
            trade1.getOutputItem(),
            "First trade output should be replaced"
        );
        assertEquals(
            Identifier.of("minecraft:bread"),
            trade2.getOutputItem(),
            "Second trade output should not be replaced"
        );
        assertEquals(
            Identifier.of("minecraft:wheat"),
            trade3.getOutputItem(),
            "Third trade output should not be replaced (different level)"
        );
    }
    
    @Test
    void testReplaceTradeOutputOperation_NullFilter_ThrowsException() {
        assertThrows(
            NullPointerException.class,
            () -> new ReplaceTradeOutputOperation(null, Identifier.of("minecraft:emerald")),
            "ReplaceTradeOutputOperation should reject null filter"
        );
    }
    
    @Test
    void testReplaceTradeOutputOperation_NullNewOutput_ThrowsException() {
        TradeFilter filter = new TradeFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.of(Identifier.of("minecraft:wheat")),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        
        assertThrows(
            NullPointerException.class,
            () -> new ReplaceTradeOutputOperation(filter, null),
            "ReplaceTradeOutputOperation should reject null newOutput"
        );
    }
    
    @Test
    void testReplaceTradeOutputOperation_NullTarget_ThrowsException() {
        TradeFilter filter = new TradeFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.of(Identifier.of("minecraft:wheat")),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        
        ReplaceTradeOutputOperation operation = new ReplaceTradeOutputOperation(
            filter,
            Identifier.of("minecraft:carrot")
        );
        
        assertThrows(
            NullPointerException.class,
            () -> operation.apply(null),
            "ReplaceTradeOutputOperation should reject null target"
        );
    }
    
    @Test
    void testReplaceTradeOutputOperation_HasDebugId() {
        TradeFilter filter = new TradeFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.of(Identifier.of("minecraft:wheat")),
            Optional.empty(),
            Optional.of(1),
            Optional.empty()
        );
        
        ReplaceTradeOutputOperation operation = new ReplaceTradeOutputOperation(
            filter,
            Identifier.of("minecraft:carrot")
        );
        
        assertTrue(operation.getDebugId().isPresent(), "ReplaceTradeOutputOperation should provide debug ID");
        String debugId = operation.getDebugId().get();
        assertTrue(debugId.contains("ReplaceTradeOutput"), "Debug ID should contain operation name");
        assertTrue(debugId.contains("minecraft:wheat"), "Debug ID should contain filter criteria");
        assertTrue(debugId.contains("minecraft:carrot"), "Debug ID should contain new output");
    }
}
