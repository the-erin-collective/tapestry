package com.tapestry.gameplay.trades.filter;

import com.tapestry.gameplay.patch.filter.FilterValidationException;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TradeFilter structured filter.
 */
class TradeFilterTest {
    
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
    
    @Test
    void testEmptyFilter_MatchesAllTrades() {
        // Empty filter should match all trades
        TradeFilter filter = new TradeFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        
        Predicate<TradeEntry> predicate = filter.toPredicate();
        
        MockTradeEntry trade = new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:wheat"),
            1,
            10
        );
        
        assertTrue(predicate.test(trade), "Empty filter should match all trades");
    }
    
    @Test
    void testInputItemFilter_MatchesCorrectTrade() {
        Identifier emerald = Identifier.of("minecraft:emerald");
        
        TradeFilter filter = new TradeFilter(
            Optional.of(emerald),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        
        Predicate<TradeEntry> predicate = filter.toPredicate();
        
        MockTradeEntry matchingTrade = new MockTradeEntry(
            emerald,
            Identifier.of("minecraft:wheat"),
            1,
            10
        );
        
        MockTradeEntry nonMatchingTrade = new MockTradeEntry(
            Identifier.of("minecraft:diamond"),
            Identifier.of("minecraft:wheat"),
            1,
            10
        );
        
        assertTrue(predicate.test(matchingTrade), "Filter should match trade with correct input item");
        assertFalse(predicate.test(nonMatchingTrade), "Filter should not match trade with different input item");
    }
    
    @Test
    void testMultipleCriteria_LogicalAnd() {
        Identifier emerald = Identifier.of("minecraft:emerald");
        Identifier wheat = Identifier.of("minecraft:wheat");
        
        TradeFilter filter = new TradeFilter(
            Optional.of(emerald),
            Optional.empty(),
            Optional.of(wheat),
            Optional.empty(),
            Optional.of(1),
            Optional.empty()
        );
        
        Predicate<TradeEntry> predicate = filter.toPredicate();
        
        // Trade matching all criteria
        MockTradeEntry matchingTrade = new MockTradeEntry(emerald, wheat, 1, 10);
        assertTrue(predicate.test(matchingTrade), "Filter should match trade with all criteria");
        
        // Trade with wrong input
        MockTradeEntry wrongInput = new MockTradeEntry(
            Identifier.of("minecraft:diamond"), wheat, 1, 10
        );
        assertFalse(predicate.test(wrongInput), "Filter should not match trade with wrong input");
        
        // Trade with wrong output
        MockTradeEntry wrongOutput = new MockTradeEntry(
            emerald, Identifier.of("minecraft:bread"), 1, 10
        );
        assertFalse(predicate.test(wrongOutput), "Filter should not match trade with wrong output");
        
        // Trade with wrong level
        MockTradeEntry wrongLevel = new MockTradeEntry(emerald, wheat, 2, 10);
        assertFalse(predicate.test(wrongLevel), "Filter should not match trade with wrong level");
    }
    
    @Test
    void testLevelFilter() {
        TradeFilter filter = new TradeFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(2),
            Optional.empty()
        );
        
        Predicate<TradeEntry> predicate = filter.toPredicate();
        
        MockTradeEntry level2Trade = new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:wheat"),
            2,
            10
        );
        
        MockTradeEntry level1Trade = new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:wheat"),
            1,
            10
        );
        
        assertTrue(predicate.test(level2Trade), "Filter should match trade with correct level");
        assertFalse(predicate.test(level1Trade), "Filter should not match trade with different level");
    }
    
    @Test
    void testMaxUsesFilter() {
        TradeFilter filter = new TradeFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(16)
        );
        
        Predicate<TradeEntry> predicate = filter.toPredicate();
        
        MockTradeEntry matching = new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:wheat"),
            1,
            16
        );
        
        MockTradeEntry nonMatching = new MockTradeEntry(
            Identifier.of("minecraft:emerald"),
            Identifier.of("minecraft:wheat"),
            1,
            10
        );
        
        assertTrue(predicate.test(matching), "Filter should match trade with correct maxUses");
        assertFalse(predicate.test(nonMatching), "Filter should not match trade with different maxUses");
    }
    
    @Test
    void testTagFilter() {
        TradeFilter filter = new TradeFilter(
            Optional.empty(),
            Optional.of("minecraft:fish"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        
        Predicate<TradeEntry> predicate = filter.toPredicate();
        
        MockTradeEntry withTag = new MockTradeEntry(
            Identifier.of("minecraft:cod"),
            Identifier.of("minecraft:emerald"),
            1,
            10,
            "minecraft:fish",
            null
        );
        
        MockTradeEntry withoutTag = new MockTradeEntry(
            Identifier.of("minecraft:wheat"),
            Identifier.of("minecraft:emerald"),
            1,
            10,
            null,
            null
        );
        
        assertTrue(predicate.test(withTag), "Filter should match trade with correct tag");
        assertFalse(predicate.test(withoutTag), "Filter should not match trade without tag");
    }
    
    @Test
    void testValidate_ValidFilter() {
        // Note: This test skips item validation as the registry is not initialized in test environment
        // Item validation is tested in integration tests
        TradeFilter filter = new TradeFilter(
            Optional.empty(),  // Skip item validation
            Optional.of("minecraft:fish"),
            Optional.empty(),  // Skip item validation
            Optional.empty(),
            Optional.of(1),
            Optional.of(10)
        );
        
        assertDoesNotThrow(() -> filter.validate(), "Valid filter should not throw exception");
    }
    
    @Test
    void testValidate_InvalidLevel() {
        TradeFilter filter = new TradeFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(0),  // Invalid: level must be positive
            Optional.empty()
        );
        
        FilterValidationException exception = assertThrows(
            FilterValidationException.class,
            () -> filter.validate(),
            "Filter with zero level should throw validation exception"
        );
        
        assertTrue(exception.getMessage().contains("Level must be positive"));
    }
    
    @Test
    void testValidate_InvalidMaxUses() {
        TradeFilter filter = new TradeFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(-5)  // Invalid: maxUses must be positive
        );
        
        FilterValidationException exception = assertThrows(
            FilterValidationException.class,
            () -> filter.validate(),
            "Filter with negative maxUses should throw validation exception"
        );
        
        assertTrue(exception.getMessage().contains("MaxUses must be positive"));
    }
    
    @Test
    void testValidate_EmptyTag() {
        TradeFilter filter = new TradeFilter(
            Optional.empty(),
            Optional.of(""),  // Invalid: empty tag
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        
        FilterValidationException exception = assertThrows(
            FilterValidationException.class,
            () -> filter.validate(),
            "Filter with empty tag should throw validation exception"
        );
        
        assertTrue(exception.getMessage().contains("tag cannot be null or empty"));
    }
    
    @Test
    void testFromSpec_ValidSpec() {
        Map<String, Object> spec = new HashMap<>();
        spec.put("input", "minecraft:emerald");
        spec.put("output", "minecraft:wheat");
        spec.put("level", 1);
        spec.put("maxUses", 10);
        
        TradeFilter filter = TradeFilter.fromSpec(spec);
        
        assertTrue(filter.inputItem().isPresent());
        assertEquals(Identifier.of("minecraft:emerald"), filter.inputItem().get());
        assertTrue(filter.outputItem().isPresent());
        assertEquals(Identifier.of("minecraft:wheat"), filter.outputItem().get());
        assertTrue(filter.level().isPresent());
        assertEquals(1, filter.level().get());
        assertTrue(filter.maxUses().isPresent());
        assertEquals(10, filter.maxUses().get());
    }
    
    @Test
    void testFromSpec_AlternativeKeys() {
        Map<String, Object> spec = new HashMap<>();
        spec.put("inputItem", "minecraft:emerald");  // Alternative key
        spec.put("outputItem", "minecraft:wheat");   // Alternative key
        
        TradeFilter filter = TradeFilter.fromSpec(spec);
        
        assertTrue(filter.inputItem().isPresent());
        assertEquals(Identifier.of("minecraft:emerald"), filter.inputItem().get());
        assertTrue(filter.outputItem().isPresent());
        assertEquals(Identifier.of("minecraft:wheat"), filter.outputItem().get());
    }
    
    @Test
    void testFromSpec_EmptySpec() {
        Map<String, Object> spec = new HashMap<>();
        
        TradeFilter filter = TradeFilter.fromSpec(spec);
        
        assertFalse(filter.inputItem().isPresent());
        assertFalse(filter.inputTag().isPresent());
        assertFalse(filter.outputItem().isPresent());
        assertFalse(filter.outputTag().isPresent());
        assertFalse(filter.level().isPresent());
        assertFalse(filter.maxUses().isPresent());
    }
    
    @Test
    void testFromSpec_NullSpec() {
        assertThrows(
            IllegalArgumentException.class,
            () -> TradeFilter.fromSpec(null),
            "fromSpec should throw exception for null spec"
        );
    }
    
    @Test
    void testFromSpec_InvalidType() {
        Map<String, Object> spec = new HashMap<>();
        spec.put("level", "not a number");  // Invalid type
        
        assertThrows(
            IllegalArgumentException.class,
            () -> TradeFilter.fromSpec(spec),
            "fromSpec should throw exception for invalid type"
        );
    }
    
    @Test
    void testNullOptionals_ConvertedToEmpty() {
        // Compact constructor should convert null to Optional.empty()
        TradeFilter filter = new TradeFilter(null, null, null, null, null, null);
        
        assertFalse(filter.inputItem().isPresent());
        assertFalse(filter.inputTag().isPresent());
        assertFalse(filter.outputItem().isPresent());
        assertFalse(filter.outputTag().isPresent());
        assertFalse(filter.level().isPresent());
        assertFalse(filter.maxUses().isPresent());
    }
}
