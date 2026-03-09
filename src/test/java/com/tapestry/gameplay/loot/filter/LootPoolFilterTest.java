package com.tapestry.gameplay.loot.filter;

import com.tapestry.gameplay.patch.filter.FilterValidationException;
import net.minecraft.loot.LootPool;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LootPoolFilter structured filter.
 */
class LootPoolFilterTest {
    
    @Test
    void testEmptyFilter_CreatesValidPredicate() {
        // Empty filter should create a valid predicate
        LootPoolFilter filter = new LootPoolFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        
        Predicate<LootPool> predicate = filter.toPredicate();
        
        assertNotNull(predicate, "Empty filter should create a valid predicate");
    }
    
    @Test
    void testNameFilter_CreatesValidPredicate() {
        LootPoolFilter filter = new LootPoolFilter(
            Optional.of("main"),
            Optional.empty(),
            Optional.empty()
        );
        
        Predicate<LootPool> predicate = filter.toPredicate();
        
        // Note: Since we can't easily create and configure LootPool instances in unit tests,
        // this test verifies the predicate is created without errors.
        // The actual name matching is tested in integration tests.
        assertNotNull(predicate, "Filter should create a valid predicate");
    }
    
    @Test
    void testRollsFilter_CreatesValidPredicate() {
        LootPoolFilter filter = new LootPoolFilter(
            Optional.empty(),
            Optional.of(1),
            Optional.empty()
        );
        
        Predicate<LootPool> predicate = filter.toPredicate();
        
        // Note: Since we can't easily create and configure LootPool instances in unit tests,
        // this test verifies the predicate is created without errors.
        assertNotNull(predicate, "Filter should create a valid predicate");
    }
    
    @Test
    void testBonusRollsFilter_CreatesValidPredicate() {
        LootPoolFilter filter = new LootPoolFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.of(2)
        );
        
        Predicate<LootPool> predicate = filter.toPredicate();
        
        // Note: Since we can't easily create and configure LootPool instances in unit tests,
        // this test verifies the predicate is created without errors.
        assertNotNull(predicate, "Filter should create a valid predicate");
    }
    
    @Test
    void testMultipleCriteria_CreatesValidPredicate() {
        LootPoolFilter filter = new LootPoolFilter(
            Optional.of("main"),
            Optional.of(1),
            Optional.of(0)
        );
        
        Predicate<LootPool> predicate = filter.toPredicate();
        
        // Verify predicate is created with all criteria
        assertNotNull(predicate, "Filter should create a valid predicate with multiple criteria");
    }
    
    @Test
    void testValidate_ValidFilter() {
        LootPoolFilter filter = new LootPoolFilter(
            Optional.of("main"),
            Optional.of(1),
            Optional.of(0)
        );
        
        assertDoesNotThrow(() -> filter.validate(), "Valid filter should not throw exception");
    }
    
    @Test
    void testValidate_EmptyName() {
        LootPoolFilter filter = new LootPoolFilter(
            Optional.of(""),  // Invalid: empty name
            Optional.empty(),
            Optional.empty()
        );
        
        FilterValidationException exception = assertThrows(
            FilterValidationException.class,
            () -> filter.validate(),
            "Filter with empty name should throw validation exception"
        );
        
        assertTrue(exception.getMessage().contains("Pool name cannot be null or empty"));
    }
    
    @Test
    void testValidate_NegativeRolls() {
        LootPoolFilter filter = new LootPoolFilter(
            Optional.empty(),
            Optional.of(-1),  // Invalid: negative rolls
            Optional.empty()
        );
        
        FilterValidationException exception = assertThrows(
            FilterValidationException.class,
            () -> filter.validate(),
            "Filter with negative rolls should throw validation exception"
        );
        
        assertTrue(exception.getMessage().contains("Rolls must be non-negative"));
    }
    
    @Test
    void testValidate_NegativeBonusRolls() {
        LootPoolFilter filter = new LootPoolFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.of(-5)  // Invalid: negative bonusRolls
        );
        
        FilterValidationException exception = assertThrows(
            FilterValidationException.class,
            () -> filter.validate(),
            "Filter with negative bonusRolls should throw validation exception"
        );
        
        assertTrue(exception.getMessage().contains("BonusRolls must be non-negative"));
    }
    
    @Test
    void testValidate_ZeroRolls_IsValid() {
        LootPoolFilter filter = new LootPoolFilter(
            Optional.empty(),
            Optional.of(0),  // Valid: zero rolls is allowed
            Optional.empty()
        );
        
        assertDoesNotThrow(() -> filter.validate(), "Filter with zero rolls should be valid");
    }
    
    @Test
    void testFromSpec_ValidSpec() {
        Map<String, Object> spec = new HashMap<>();
        spec.put("name", "main");
        spec.put("rolls", 1);
        spec.put("bonusRolls", 2);
        
        LootPoolFilter filter = LootPoolFilter.fromSpec(spec);
        
        assertTrue(filter.name().isPresent());
        assertEquals("main", filter.name().get());
        assertTrue(filter.rolls().isPresent());
        assertEquals(1, filter.rolls().get());
        assertTrue(filter.bonusRolls().isPresent());
        assertEquals(2, filter.bonusRolls().get());
    }
    
    @Test
    void testFromSpec_EmptySpec() {
        Map<String, Object> spec = new HashMap<>();
        
        LootPoolFilter filter = LootPoolFilter.fromSpec(spec);
        
        assertFalse(filter.name().isPresent());
        assertFalse(filter.rolls().isPresent());
        assertFalse(filter.bonusRolls().isPresent());
    }
    
    @Test
    void testFromSpec_NullSpec() {
        assertThrows(
            IllegalArgumentException.class,
            () -> LootPoolFilter.fromSpec(null),
            "fromSpec should throw exception for null spec"
        );
    }
    
    @Test
    void testFromSpec_InvalidType() {
        Map<String, Object> spec = new HashMap<>();
        spec.put("rolls", "not a number");  // Invalid type
        
        assertThrows(
            IllegalArgumentException.class,
            () -> LootPoolFilter.fromSpec(spec),
            "fromSpec should throw exception for invalid type"
        );
    }
    
    @Test
    void testFromSpec_NumberConversion() {
        Map<String, Object> spec = new HashMap<>();
        spec.put("rolls", 1.5);  // Double should be converted to int
        spec.put("bonusRolls", 2L);  // Long should be converted to int
        
        LootPoolFilter filter = LootPoolFilter.fromSpec(spec);
        
        assertTrue(filter.rolls().isPresent());
        assertEquals(1, filter.rolls().get());
        assertTrue(filter.bonusRolls().isPresent());
        assertEquals(2, filter.bonusRolls().get());
    }
    
    @Test
    void testNullOptionals_ConvertedToEmpty() {
        // Compact constructor should convert null to Optional.empty()
        LootPoolFilter filter = new LootPoolFilter(null, null, null);
        
        assertFalse(filter.name().isPresent());
        assertFalse(filter.rolls().isPresent());
        assertFalse(filter.bonusRolls().isPresent());
    }
    
    @Test
    void testToPredicate_ReturnsNonNullPredicate() {
        LootPoolFilter filter = new LootPoolFilter(
            Optional.of("main"),
            Optional.of(1),
            Optional.of(0)
        );
        
        Predicate<LootPool> predicate = filter.toPredicate();
        
        assertNotNull(predicate, "toPredicate should return a non-null predicate");
    }
}
