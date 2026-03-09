package com.tapestry.gameplay.loot;

import com.tapestry.gameplay.patch.PatchRegistry;
import com.tapestry.gameplay.patch.PatchSet;
import com.tapestry.gameplay.patch.PatchTarget;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import net.minecraft.loot.LootTable;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LootAPI}.
 * 
 * Tests Requirements:
 * - 11.1: LootAPI provides modify method accepting loot table identifier and builder function
 * - 11.2: Store builder functions during TS_REGISTER phase
 * - 11.3: Execute builders and translate to patch operations at phase completion
 * - 11.4: Register generated PatchSets with PatchRegistry
 */
class LootAPITest {
    
    private PatchRegistry registry;
    private Identifier lootTableId;
    private Identifier modId;
    
    @BeforeEach
    void setUp() {
        // Reset phase controller and advance to TS_REGISTER
        PhaseController.reset();
        PhaseController controller = PhaseController.getInstance();
        // PhaseController starts at BOOTSTRAP after reset, so advance from there
        controller.advanceTo(TapestryPhase.DISCOVERY);
        controller.advanceTo(TapestryPhase.VALIDATION);
        controller.advanceTo(TapestryPhase.REGISTRATION);
        controller.advanceTo(TapestryPhase.FREEZE);
        controller.advanceTo(TapestryPhase.TS_LOAD);
        controller.advanceTo(TapestryPhase.TS_REGISTER);
        
        // Create test fixtures
        registry = new PatchRegistry();
        lootTableId = Identifier.of("minecraft:chests/simple_dungeon");
        modId = Identifier.of("testmod:test");
        
        // Set current mod ID
        LootAPI.setCurrentModId(modId);
        
        // Clear any pending modifications from previous tests
        LootAPI.clearPendingModifications();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up
        LootAPI.clearPendingModifications();
        PhaseController.reset();
    }
    
    @Test
    void testModify_StoresBuilderFunction() {
        // Requirement 11.1, 11.2: Store builder function during TS_REGISTER phase
        
        LootAPI.modify(lootTableId, loot -> {
            loot.removePool(Map.of("name", "main"));
        });
        
        assertEquals(1, LootAPI.getPendingModificationCount(),
            "Should store one pending modification");
    }
    
    @Test
    void testModify_MultipleModifications_StoresAll() {
        // Requirement 11.2: Store multiple builder functions
        
        LootAPI.modify(lootTableId, loot -> {
            loot.removePool(Map.of("name", "main"));
        });
        
        LootAPI.modify(Identifier.of("minecraft:chests/end_city_treasure"), loot -> {
            loot.removePool(Map.of("name", "pool1"));
        });
        
        assertEquals(2, LootAPI.getPendingModificationCount(),
            "Should store two pending modifications");
    }
    
    @Test
    void testModify_WithCustomPriority_StoresCorrectly() {
        // Requirement 11.1: Support custom priority
        
        int customPriority = 100;
        
        LootAPI.modify(lootTableId, loot -> {
            loot.removePool(Map.of("name", "main"));
        }, customPriority);
        
        assertEquals(1, LootAPI.getPendingModificationCount(),
            "Should store one pending modification with custom priority");
    }
    
    @Test
    void testModify_OutsideRegisterPhase_ThrowsException() {
        // Requirement 11.2: Only accept registrations during TS_REGISTER phase
        
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_ACTIVATE);
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> LootAPI.modify(lootTableId, loot -> {})
        );
        
        assertTrue(exception.getMessage().contains("TS_REGISTER"),
            "Error message should mention TS_REGISTER phase");
        assertTrue(exception.getMessage().contains("TS_ACTIVATE"),
            "Error message should mention current phase");
    }
    
    @Test
    void testModify_NullLootTableId_ThrowsException() {
        // Requirement 11.1: Validate loot table identifier
        
        assertThrows(
            NullPointerException.class,
            () -> LootAPI.modify(null, loot -> {})
        );
    }
    
    @Test
    void testModify_NullBuilderFunction_ThrowsException() {
        // Requirement 11.1: Validate builder function
        
        assertThrows(
            NullPointerException.class,
            () -> LootAPI.modify(lootTableId, null)
        );
    }
    
    @Test
    void testExecuteBuilders_ExecutesAllBuilders() {
        // Requirement 11.3: Execute all stored builders at phase completion
        
        LootAPI.modify(lootTableId, loot -> {
            loot.removePool(Map.of("name", "main"));
        });
        
        LootAPI.modify(Identifier.of("minecraft:chests/end_city_treasure"), loot -> {
            loot.removePool(Map.of("name", "pool1"));
        });
        
        LootAPI.executeBuilders(registry);
        
        // Verify all modifications were processed
        assertEquals(0, LootAPI.getPendingModificationCount(),
            "Should clear pending modifications after execution");
        
        // Verify patch sets were registered
        PatchTarget<LootTable> dungeonTarget = new PatchTarget<>(
            lootTableId,
            LootTable.class
        );
        List<PatchSet<LootTable>> dungeonPatches = registry.getPatchesFor(dungeonTarget);
        assertEquals(1, dungeonPatches.size(),
            "Should register one patch set for simple_dungeon");
        
        PatchTarget<LootTable> endCityTarget = new PatchTarget<>(
            Identifier.of("minecraft:chests/end_city_treasure"),
            LootTable.class
        );
        List<PatchSet<LootTable>> endCityPatches = registry.getPatchesFor(endCityTarget);
        assertEquals(1, endCityPatches.size(),
            "Should register one patch set for end_city_treasure");
    }
    
    @Test
    void testExecuteBuilders_RegistersPatchSetsWithCorrectModId() {
        // Requirement 11.4: Register PatchSets with correct mod identifier
        
        LootAPI.modify(lootTableId, loot -> {
            loot.removePool(Map.of("name", "main"));
        });
        
        LootAPI.executeBuilders(registry);
        
        PatchTarget<LootTable> target = new PatchTarget<>(
            lootTableId,
            LootTable.class
        );
        List<PatchSet<LootTable>> patches = registry.getPatchesFor(target);
        
        assertEquals(1, patches.size());
        assertEquals(modId, patches.get(0).modId(),
            "Patch set should have correct mod identifier");
    }
    
    @Test
    void testExecuteBuilders_RegistersPatchSetsWithCorrectTarget() {
        // Requirement 11.4: Register PatchSets with correct target
        
        LootAPI.modify(lootTableId, loot -> {
            loot.removePool(Map.of("name", "main"));
        });
        
        LootAPI.executeBuilders(registry);
        
        PatchTarget<LootTable> target = new PatchTarget<>(
            lootTableId,
            LootTable.class
        );
        List<PatchSet<LootTable>> patches = registry.getPatchesFor(target);
        
        assertEquals(1, patches.size());
        assertEquals(lootTableId, patches.get(0).target().id(),
            "Patch set should have correct target identifier");
        assertEquals(LootTable.class, patches.get(0).target().type(),
            "Patch set should have correct target type");
    }
    
    @Test
    void testExecuteBuilders_RegistersPatchSetsWithCorrectPriority() {
        // Requirement 11.4: Register PatchSets with correct priority
        
        int customPriority = 100;
        
        LootAPI.modify(lootTableId, loot -> {
            loot.removePool(Map.of("name", "main"));
        }, customPriority);
        
        LootAPI.executeBuilders(registry);
        
        PatchTarget<LootTable> target = new PatchTarget<>(
            lootTableId,
            LootTable.class
        );
        List<PatchSet<LootTable>> patches = registry.getPatchesFor(target);
        
        assertEquals(1, patches.size());
        assertEquals(customPriority, patches.get(0).priority(),
            "Patch set should have correct priority");
    }
    
    @Test
    void testExecuteBuilders_TranslatesBuilderOperationsToPatchOperations() {
        // Requirement 11.3: Translate builder operations to patch operations
        
        LootAPI.modify(lootTableId, loot -> {
            loot.removePool(Map.of("name", "main"));
            loot.removeEntry(
                Map.of("name", "pool1"),
                Map.of("item", "minecraft:diamond")
            );
        });
        
        LootAPI.executeBuilders(registry);
        
        PatchTarget<LootTable> target = new PatchTarget<>(
            lootTableId,
            LootTable.class
        );
        List<PatchSet<LootTable>> patches = registry.getPatchesFor(target);
        
        assertEquals(1, patches.size());
        assertEquals(2, patches.get(0).operations().size(),
            "Patch set should contain two operations");
    }
    
    @Test
    void testExecuteBuilders_NullRegistry_ThrowsException() {
        // Requirement 11.4: Validate registry parameter
        
        assertThrows(
            NullPointerException.class,
            () -> LootAPI.executeBuilders(null)
        );
    }
    
    @Test
    void testExecuteBuilders_EmptyPendingModifications_CompletesSuccessfully() {
        // Requirement 11.3: Handle empty modifications list
        
        assertDoesNotThrow(() -> LootAPI.executeBuilders(registry));
    }
    
    @Test
    void testExecuteBuilders_BuilderThrowsException_ContinuesWithOthers() {
        // Requirement 11.3: Handle builder failures gracefully
        
        LootAPI.modify(lootTableId, loot -> {
            throw new RuntimeException("Test exception");
        });
        
        LootAPI.modify(Identifier.of("minecraft:chests/end_city_treasure"), loot -> {
            loot.removePool(Map.of("name", "pool1"));
        });
        
        // Should not throw exception
        assertDoesNotThrow(() -> LootAPI.executeBuilders(registry));
        
        // Should still register the successful modification
        PatchTarget<LootTable> endCityTarget = new PatchTarget<>(
            Identifier.of("minecraft:chests/end_city_treasure"),
            LootTable.class
        );
        List<PatchSet<LootTable>> endCityPatches = registry.getPatchesFor(endCityTarget);
        assertEquals(1, endCityPatches.size(),
            "Should register successful patch set despite other failures");
    }
    
    @Test
    void testExecuteBuilders_ClearsPendingModifications() {
        // Requirement 11.3: Clear pending modifications after execution
        
        LootAPI.modify(lootTableId, loot -> {
            loot.removePool(Map.of("name", "main"));
        });
        
        assertEquals(1, LootAPI.getPendingModificationCount());
        
        LootAPI.executeBuilders(registry);
        
        assertEquals(0, LootAPI.getPendingModificationCount(),
            "Should clear pending modifications after execution");
    }
    
    @Test
    void testExecuteBuilders_CanBeCalledMultipleTimes() {
        // Requirement 11.3: Support multiple executions
        
        LootAPI.modify(lootTableId, loot -> {
            loot.removePool(Map.of("name", "main"));
        });
        
        LootAPI.executeBuilders(registry);
        
        // Second execution with no pending modifications
        assertDoesNotThrow(() -> LootAPI.executeBuilders(registry));
    }
}
