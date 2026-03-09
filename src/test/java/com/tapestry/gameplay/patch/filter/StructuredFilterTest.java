package com.tapestry.gameplay.patch.filter;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.Optional;

import com.tapestry.gameplay.patch.PatchContext;
import com.tapestry.gameplay.loot.filter.LootEntryFilter;
import net.minecraft.util.Identifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StructuredFilter interface.
 * Tests basic functionality using a simple test implementation.
 */
class StructuredFilterTest {
    
    /**
     * Simple test implementation of StructuredFilter for testing purposes.
     */
    private static class TestFilter implements StructuredFilter<String> {
        private final String expectedValue;
        private final boolean shouldValidate;
        
        TestFilter(String expectedValue, boolean shouldValidate) {
            this.expectedValue = expectedValue;
            this.shouldValidate = shouldValidate;
        }
        
        @Override
        public Predicate<String> toPredicate() {
            return s -> s != null && s.equals(expectedValue);
        }
        
        @Override
        public void validate() throws FilterValidationException {
            if (!shouldValidate) {
                throw new FilterValidationException("Test validation failure");
            }
        }
    }
    
    @Test
    void testToPredicateReturnsValidPredicate() {
        TestFilter filter = new TestFilter("test", true);
        Predicate<String> predicate = filter.toPredicate();
        
        assertNotNull(predicate);
        assertTrue(predicate.test("test"));
        assertFalse(predicate.test("other"));
        assertFalse(predicate.test(null));
    }
    
    @Test
    void testValidateSucceeds() {
        TestFilter filter = new TestFilter("test", true);
        
        assertDoesNotThrow(() -> filter.validate());
    }
    
    @Test
    void testValidateThrowsException() {
        TestFilter filter = new TestFilter("test", false);
        
        FilterValidationException exception = assertThrows(
            FilterValidationException.class,
            () -> filter.validate()
        );
        
        assertEquals("Test validation failure", exception.getMessage());
    }
    
    @Test
    void testPredicateCanBeUsedInStream() {
        TestFilter filter = new TestFilter("match", true);
        Predicate<String> predicate = filter.toPredicate();
        
        java.util.List<String> items = java.util.List.of("match", "no-match", "match", "other");
        long count = items.stream().filter(predicate).count();
        
        assertEquals(2, count);
    }
    
    /**
     * Test filter with multiple criteria (simulating AND logic).
     */
    private static class MultiCriteriaFilter implements StructuredFilter<String> {
        private final String prefix;
        private final int minLength;
        
        MultiCriteriaFilter(String prefix, int minLength) {
            this.prefix = prefix;
            this.minLength = minLength;
        }
        
        @Override
        public Predicate<String> toPredicate() {
            return s -> s != null 
                && s.startsWith(prefix) 
                && s.length() >= minLength;
        }
        
        @Override
        public void validate() throws FilterValidationException {
            if (prefix == null || prefix.isEmpty()) {
                throw new FilterValidationException("Prefix cannot be null or empty");
            }
            if (minLength < 0) {
                throw new FilterValidationException("Min length cannot be negative");
            }
        }
    }
    
    @Test
    void testMultiCriteriaFilter() {
        MultiCriteriaFilter filter = new MultiCriteriaFilter("test", 5);
        Predicate<String> predicate = filter.toPredicate();
        
        assertTrue(predicate.test("test123"));
        assertTrue(predicate.test("testing"));
        assertFalse(predicate.test("test")); // Too short
        assertFalse(predicate.test("other")); // Wrong prefix
        assertFalse(predicate.test(null));
    }
    
    @Test
    void testMultiCriteriaValidation() {
        MultiCriteriaFilter validFilter = new MultiCriteriaFilter("test", 5);
        assertDoesNotThrow(() -> validFilter.validate());
        
        MultiCriteriaFilter invalidPrefix = new MultiCriteriaFilter("", 5);
        FilterValidationException exception1 = assertThrows(
            FilterValidationException.class,
            () -> invalidPrefix.validate()
        );
        assertTrue(exception1.getMessage().contains("Prefix"));
        
        MultiCriteriaFilter invalidLength = new MultiCriteriaFilter("test", -1);
        FilterValidationException exception2 = assertThrows(
            FilterValidationException.class,
            () -> invalidLength.validate()
        );
        assertTrue(exception2.getMessage().contains("Min length"));
    }
    
    @Test
    void testValidateWithContextDelegates() {
        PatchContext stub = new PatchContext() {
            @Override public boolean isModLoaded(String modId) { return false; }
            @Override public boolean registryContains(Identifier id) { return false; }
            @Override public boolean traitExists(Identifier traitId) { return false; }
            @Override public net.minecraft.registry.Registry<?> getRegistry(Identifier registryId) { return null; }
        };
        TestFilter filter = new TestFilter("test", true);
        assertDoesNotThrow(() -> filter.validate(stub));
        TestFilter failing = new TestFilter("test", false);
        assertThrows(FilterValidationException.class, () -> failing.validate(stub));
    }
    
    @Test
    void testLootEntryFilterContextAwareValidation() {
        LootEntryFilter filter = new LootEntryFilter(
            Optional.of(Identifier.of("minecraft:nonexistent")),
            Optional.empty()
        );
        PatchContext stub = new PatchContext() {
            @Override public boolean isModLoaded(String modId) { return false; }
            @Override public boolean registryContains(Identifier id) { return false; }
            @Override public boolean traitExists(Identifier traitId) { return false; }
            @Override public net.minecraft.registry.Registry<?> getRegistry(Identifier registryId) { return null; }
        };
        FilterValidationException ex = assertThrows(
            FilterValidationException.class,
            () -> filter.validate(stub)
        );
        assertTrue(ex.getMessage().contains("Referenced loot item"));
    }
}
