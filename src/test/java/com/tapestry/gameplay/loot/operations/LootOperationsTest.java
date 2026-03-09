package com.tapestry.gameplay.loot.operations;

import com.tapestry.gameplay.loot.filter.LootEntryFilter;
import com.tapestry.gameplay.loot.filter.LootPoolFilter;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for loot patch operations.
 * 
 * Note: These tests focus on operation construction and validation.
 * Tests that require Minecraft LootTable/LootPool objects are skipped
 * because they require Minecraft bootstrap. Full integration tests
 * are performed separately in the integration test suite.
 */
class LootOperationsTest {
    
    // AddPoolOperation Tests
    
    @Test
    void testAddPoolOperation_NullPool_ThrowsException() {
        assertThrows(
            NullPointerException.class,
            () -> new AddPoolOperation(null),
            "AddPoolOperation should reject null pool"
        );
    }
    
    // RemovePoolOperation Tests
    
    @Test
    void testRemovePoolOperation_NullFilter_ThrowsException() {
        assertThrows(
            NullPointerException.class,
            () -> new RemovePoolOperation(null),
            "RemovePoolOperation should reject null filter"
        );
    }
    
    @Test
    void testRemovePoolOperation_HasDebugId() {
        LootPoolFilter filter = new LootPoolFilter(
            Optional.of("main"),
            Optional.of(1),
            Optional.of(0)
        );
        
        RemovePoolOperation operation = new RemovePoolOperation(filter);
        
        assertTrue(operation.getDebugId().isPresent(), "RemovePoolOperation should provide debug ID");
        String debugId = operation.getDebugId().get();
        assertTrue(debugId.contains("RemovePool"), "Debug ID should contain operation name");
        assertTrue(debugId.contains("name=main"), "Debug ID should contain name filter");
        assertTrue(debugId.contains("rolls=1"), "Debug ID should contain rolls filter");
    }
    
    @Test
    void testRemovePoolOperation_StoresFilter() {
        LootPoolFilter filter = new LootPoolFilter(
            Optional.of("main"),
            Optional.empty(),
            Optional.empty()
        );
        
        RemovePoolOperation operation = new RemovePoolOperation(filter);
        
        assertEquals(filter, operation.filter(), "Operation should store the filter");
    }
    
    // AddEntryOperation Tests
    
    @Test
    void testAddEntryOperation_NullPoolFilter_ThrowsException() {
        assertThrows(
            NullPointerException.class,
            () -> new AddEntryOperation(null, null),
            "AddEntryOperation should reject null pool filter"
        );
    }
    
    // RemoveEntryOperation Tests
    
    @Test
    void testRemoveEntryOperation_NullPoolFilter_ThrowsException() {
        LootEntryFilter entryFilter = new LootEntryFilter(
            Optional.of(Identifier.of("minecraft:diamond")),
            Optional.empty()
        );
        
        assertThrows(
            NullPointerException.class,
            () -> new RemoveEntryOperation(null, entryFilter),
            "RemoveEntryOperation should reject null pool filter"
        );
    }
    
    @Test
    void testRemoveEntryOperation_NullEntryFilter_ThrowsException() {
        LootPoolFilter poolFilter = new LootPoolFilter(
            Optional.of("main"),
            Optional.empty(),
            Optional.empty()
        );
        
        assertThrows(
            NullPointerException.class,
            () -> new RemoveEntryOperation(poolFilter, null),
            "RemoveEntryOperation should reject null entry filter"
        );
    }
    
    @Test
    void testRemoveEntryOperation_HasDebugId() {
        LootPoolFilter poolFilter = new LootPoolFilter(
            Optional.of("main"),
            Optional.of(1),
            Optional.empty()
        );
        
        LootEntryFilter entryFilter = new LootEntryFilter(
            Optional.of(Identifier.of("minecraft:diamond")),
            Optional.of("minecraft:item")
        );
        
        RemoveEntryOperation operation = new RemoveEntryOperation(poolFilter, entryFilter);
        
        assertTrue(operation.getDebugId().isPresent(), "RemoveEntryOperation should provide debug ID");
        String debugId = operation.getDebugId().get();
        assertTrue(debugId.contains("RemoveEntry"), "Debug ID should contain operation name");
        assertTrue(debugId.contains("name=main"), "Debug ID should contain pool filter");
        assertTrue(debugId.contains("item=minecraft:diamond"), "Debug ID should contain entry filter");
    }
    
    @Test
    void testRemoveEntryOperation_StoresPoolFilterAndEntryFilter() {
        LootPoolFilter poolFilter = new LootPoolFilter(
            Optional.of("main"),
            Optional.empty(),
            Optional.empty()
        );
        
        LootEntryFilter entryFilter = new LootEntryFilter(
            Optional.of(Identifier.of("minecraft:diamond")),
            Optional.empty()
        );
        
        RemoveEntryOperation operation = new RemoveEntryOperation(poolFilter, entryFilter);
        
        assertEquals(poolFilter, operation.poolFilter(), "Operation should store the pool filter");
        assertEquals(entryFilter, operation.entryFilter(), "Operation should store the entry filter");
    }
}
