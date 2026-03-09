package com.tapestry.gameplay.loot;

import com.tapestry.gameplay.loot.operations.AddEntryOperation;
import com.tapestry.gameplay.loot.operations.AddPoolOperation;
import com.tapestry.gameplay.loot.operations.RemoveEntryOperation;
import com.tapestry.gameplay.loot.operations.RemovePoolOperation;
import com.tapestry.gameplay.patch.PatchSet;
import com.tapestry.gameplay.patch.PatchTarget;
import net.minecraft.loot.LootTable;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LootTableBuilder}.
 */
class LootTableBuilderTest {
    
    private LootTableBuilder builder;
    private Identifier modId;
    private PatchTarget<LootTable> target;
    
    @BeforeEach
    void setUp() {
        builder = new LootTableBuilder();
        modId = Identifier.of("testmod:test");
        target = new PatchTarget<>(
            Identifier.of("minecraft:chests/simple_dungeon"),
            LootTable.class
        );
    }
    
    @Test
    void testAddPool_WithoutLootPoolObject_ThrowsUnsupportedOperationException() {
        Map<String, Object> poolSpec = Map.of(
            "name", "main",
            "rolls", 3
        );
        
        assertThrows(UnsupportedOperationException.class, () -> {
            builder.addPool(poolSpec);
        });
    }
    
    @Test
    void testAddPool_NullPoolSpec_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            builder.addPool(null);
        });
    }
    
    @Test
    void testRemovePool_AddsRemovePoolOperation() {
        Map<String, Object> filterSpec = Map.of(
            "name", "main",
            "rolls", 3
        );
        
        PatchSet<LootTable> patchSet = builder
            .removePool(filterSpec)
            .build(modId, target, 0);
        
        assertEquals(1, patchSet.operations().size());
        assertTrue(patchSet.operations().get(0) instanceof RemovePoolOperation);
    }
    
    @Test
    void testRemovePool_NullFilterSpec_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            builder.removePool(null);
        });
    }
    
    @Test
    void testAddEntry_WithoutLootPoolEntryObject_ThrowsUnsupportedOperationException() {
        Map<String, Object> poolFilterSpec = Map.of("name", "main");
        Map<String, Object> entrySpec = Map.of(
            "item", "minecraft:diamond",
            "weight", 1
        );
        
        assertThrows(UnsupportedOperationException.class, () -> {
            builder.addEntry(poolFilterSpec, entrySpec);
        });
    }
    
    @Test
    void testAddEntry_NullPoolFilterSpec_ThrowsException() {
        Map<String, Object> entrySpec = Map.of("item", "minecraft:diamond");
        
        assertThrows(NullPointerException.class, () -> {
            builder.addEntry(null, entrySpec);
        });
    }
    
    @Test
    void testAddEntry_NullEntrySpec_ThrowsException() {
        Map<String, Object> poolFilterSpec = Map.of("name", "main");
        
        assertThrows(NullPointerException.class, () -> {
            builder.addEntry(poolFilterSpec, null);
        });
    }
    
    @Test
    void testRemoveEntry_AddsRemoveEntryOperation() {
        Map<String, Object> poolFilterSpec = Map.of("name", "main");
        Map<String, Object> entryFilterSpec = Map.of("item", "minecraft:diamond");
        
        PatchSet<LootTable> patchSet = builder
            .removeEntry(poolFilterSpec, entryFilterSpec)
            .build(modId, target, 0);
        
        assertEquals(1, patchSet.operations().size());
        assertTrue(patchSet.operations().get(0) instanceof RemoveEntryOperation);
    }
    
    @Test
    void testRemoveEntry_NullPoolFilterSpec_ThrowsException() {
        Map<String, Object> entryFilterSpec = Map.of("item", "minecraft:diamond");
        
        assertThrows(NullPointerException.class, () -> {
            builder.removeEntry(null, entryFilterSpec);
        });
    }
    
    @Test
    void testRemoveEntry_NullEntryFilterSpec_ThrowsException() {
        Map<String, Object> poolFilterSpec = Map.of("name", "main");
        
        assertThrows(NullPointerException.class, () -> {
            builder.removeEntry(poolFilterSpec, null);
        });
    }
    
    @Test
    void testBuild_CreatesValidPatchSet() {
        Map<String, Object> filterSpec = Map.of("name", "main");
        
        PatchSet<LootTable> patchSet = builder
            .removePool(filterSpec)
            .build(modId, target, 0);
        
        assertNotNull(patchSet);
        assertEquals(modId, patchSet.modId());
        assertEquals(target, patchSet.target());
        assertEquals(0, patchSet.priority());
        assertEquals(1, patchSet.operations().size());
        assertTrue(patchSet.condition().isEmpty());
    }
    
    @Test
    void testBuild_NullModId_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            builder.build(null, target, 0);
        });
    }
    
    @Test
    void testBuild_NullTarget_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            builder.build(modId, null, 0);
        });
    }
    
    @Test
    void testMethodChaining_MultipleOperations() {
        Map<String, Object> removePoolSpec = Map.of("name", "old_pool");
        Map<String, Object> poolFilterSpec = Map.of("name", "main");
        Map<String, Object> entryFilterSpec = Map.of("item", "minecraft:diamond");
        
        PatchSet<LootTable> patchSet = builder
            .removePool(removePoolSpec)
            .removeEntry(poolFilterSpec, entryFilterSpec)
            .build(modId, target, 0);
        
        assertEquals(2, patchSet.operations().size());
        assertTrue(patchSet.operations().get(0) instanceof RemovePoolOperation);
        assertTrue(patchSet.operations().get(1) instanceof RemoveEntryOperation);
    }
    
    @Test
    void testBuild_WithCustomPriority() {
        Map<String, Object> filterSpec = Map.of("name", "main");
        int customPriority = 100;
        
        PatchSet<LootTable> patchSet = builder
            .removePool(filterSpec)
            .build(modId, target, customPriority);
        
        assertEquals(customPriority, patchSet.priority());
    }
    
    @Test
    void testBuild_EmptyOperations_ThrowsException() {
        // PatchSet constructor now disallows empty operation lists
        assertThrows(IllegalArgumentException.class, () -> builder.build(modId, target, 0));
    }
    
    @Test
    void testBuilder_CanBeReused() {
        Map<String, Object> filterSpec1 = Map.of("name", "pool1");
        Map<String, Object> filterSpec2 = Map.of("name", "pool2");
        
        PatchSet<LootTable> patchSet1 = builder
            .removePool(filterSpec1)
            .build(modId, target, 0);
        
        PatchSet<LootTable> patchSet2 = builder
            .removePool(filterSpec2)
            .build(modId, target, 0);
        
        // First patch set should have 1 operation
        assertEquals(1, patchSet1.operations().size());
        
        // Second patch set should have 2 operations (accumulated)
        assertEquals(2, patchSet2.operations().size());
    }
    
    @Test
    void testBuild_OperationsListIsIndependent() {
        Map<String, Object> filterSpec = Map.of("name", "main");
        
        builder.removePool(filterSpec);
        PatchSet<LootTable> patchSet = builder.build(modId, target, 0);
        
        // Add another operation after building
        builder.removePool(Map.of("name", "other"));
        
        // Original patch set should still have only 1 operation
        assertEquals(1, patchSet.operations().size());
    }
    
    @Test
    void testRemovePool_WithMultipleCriteria() {
        Map<String, Object> filterSpec = Map.of(
            "name", "main",
            "rolls", 3,
            "bonusRolls", 1
        );
        
        PatchSet<LootTable> patchSet = builder
            .removePool(filterSpec)
            .build(modId, target, 0);
        
        assertEquals(1, patchSet.operations().size());
        assertTrue(patchSet.operations().get(0) instanceof RemovePoolOperation);
    }
    
    @Test
    void testRemoveEntry_WithMultipleCriteria() {
        Map<String, Object> poolFilterSpec = Map.of(
            "name", "main",
            "rolls", 3
        );
        Map<String, Object> entryFilterSpec = Map.of(
            "item", "minecraft:diamond",
            "type", "minecraft:item"
        );
        
        PatchSet<LootTable> patchSet = builder
            .removeEntry(poolFilterSpec, entryFilterSpec)
            .build(modId, target, 0);
        
        assertEquals(1, patchSet.operations().size());
        assertTrue(patchSet.operations().get(0) instanceof RemoveEntryOperation);
    }
}
