package com.tapestry.gameplay.trades;

import com.tapestry.gameplay.patch.PatchRegistry;
import com.tapestry.gameplay.patch.PatchSet;
import com.tapestry.gameplay.patch.PatchTarget;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TradeAPI}.
 * 
 * Tests Requirements:
 * - 10.1: TradeAPI provides modify method accepting profession identifier and builder function
 * - 10.2: Store builder functions during TS_REGISTER phase
 * - 10.3: Execute builders and translate to patch operations at phase completion
 * - 10.4: Register generated PatchSets with PatchRegistry
 */
class TradeAPITest {
    
    private PatchRegistry registry;
    private Identifier professionId;
    private Identifier modId;
    
    @BeforeEach
    void setUp() {
        // Initialize Minecraft Bootstrap for Identifier
        net.minecraft.Bootstrap.initialize();
        
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
        professionId = Identifier.of("minecraft:villager/fisherman");
        modId = Identifier.of("testmod:test");
        
        // Set current mod ID
        TradeAPI.setCurrentModId(modId);
        
        // Clear any pending modifications from previous tests
        TradeAPI.clearPendingModifications();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up
        TradeAPI.clearPendingModifications();
        PhaseController.reset();
    }
    
    @Test
    void testModify_StoresBuilderFunction() {
        // Requirement 10.1, 10.2: Store builder function during TS_REGISTER phase
        
        TradeAPI.modify(professionId, trades -> {
            trades.remove(Map.of("input", "minecraft:cod"));
        });
        
        assertEquals(1, TradeAPI.getPendingModificationCount(),
            "Should store one pending modification");
    }
    
    @Test
    void testModify_MultipleModifications_StoresAll() {
        // Requirement 10.2: Store multiple builder functions
        
        TradeAPI.modify(professionId, trades -> {
            trades.remove(Map.of("input", "minecraft:cod"));
        });
        
        TradeAPI.modify(Identifier.of("minecraft:villager/farmer"), trades -> {
            trades.remove(Map.of("input", "minecraft:wheat"));
        });
        
        assertEquals(2, TradeAPI.getPendingModificationCount(),
            "Should store two pending modifications");
    }
    
    @Test
    void testModify_WithCustomPriority_StoresCorrectly() {
        // Requirement 10.1: Support custom priority
        
        int customPriority = 100;
        
        TradeAPI.modify(professionId, trades -> {
            trades.remove(Map.of("input", "minecraft:cod"));
        }, customPriority);
        
        assertEquals(1, TradeAPI.getPendingModificationCount(),
            "Should store one pending modification with custom priority");
    }
    
    @Test
    void testModify_OutsideRegisterPhase_ThrowsException() {
        // Requirement 10.2: Only accept registrations during TS_REGISTER phase
        
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_ACTIVATE);
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> TradeAPI.modify(professionId, trades -> {})
        );
        
        assertTrue(exception.getMessage().contains("TS_REGISTER"),
            "Error message should mention TS_REGISTER phase");
        assertTrue(exception.getMessage().contains("TS_ACTIVATE"),
            "Error message should mention current phase");
    }
    
    @Test
    void testModify_NullProfessionId_ThrowsException() {
        // Requirement 10.1: Validate profession identifier
        
        assertThrows(
            NullPointerException.class,
            () -> TradeAPI.modify(null, trades -> {})
        );
    }
    
    @Test
    void testModify_NullBuilderFunction_ThrowsException() {
        // Requirement 10.1: Validate builder function
        
        assertThrows(
            NullPointerException.class,
            () -> TradeAPI.modify(professionId, null)
        );
    }
    
    @Test
    void testExecuteBuilders_ExecutesAllBuilders() {
        // Requirement 10.3: Execute all stored builders at phase completion
        
        TradeAPI.modify(professionId, trades -> {
            trades.remove(Map.of("input", "minecraft:cod"));
        });
        
        TradeAPI.modify(Identifier.of("minecraft:villager/farmer"), trades -> {
            trades.remove(Map.of("input", "minecraft:wheat"));
        });
        
        TradeAPI.executeBuilders(registry);
        
        // Verify all modifications were processed
        assertEquals(0, TradeAPI.getPendingModificationCount(),
            "Should clear pending modifications after execution");
        
        // Verify patch sets were registered
        PatchTarget<TradeTable> fishermanTarget = new PatchTarget<>(
            professionId,
            TradeTable.class
        );
        List<PatchSet<TradeTable>> fishermanPatches = registry.getPatchesFor(fishermanTarget);
        assertEquals(1, fishermanPatches.size(),
            "Should register one patch set for fisherman");
        
        PatchTarget<TradeTable> farmerTarget = new PatchTarget<>(
            Identifier.of("minecraft:villager/farmer"),
            TradeTable.class
        );
        List<PatchSet<TradeTable>> farmerPatches = registry.getPatchesFor(farmerTarget);
        assertEquals(1, farmerPatches.size(),
            "Should register one patch set for farmer");
    }
    
    @Test
    void testExecuteBuilders_RegistersPatchSetsWithCorrectModId() {
        // Requirement 10.4: Register PatchSets with correct mod identifier
        
        TradeAPI.modify(professionId, trades -> {
            trades.remove(Map.of("input", "minecraft:cod"));
        });
        
        TradeAPI.executeBuilders(registry);
        
        PatchTarget<TradeTable> target = new PatchTarget<>(
            professionId,
            TradeTable.class
        );
        List<PatchSet<TradeTable>> patches = registry.getPatchesFor(target);
        
        assertEquals(1, patches.size());
        assertEquals(modId, patches.get(0).modId(),
            "Patch set should have correct mod identifier");
    }
    
    @Test
    void testExecuteBuilders_RegistersPatchSetsWithCorrectTarget() {
        // Requirement 10.4: Register PatchSets with correct target
        
        TradeAPI.modify(professionId, trades -> {
            trades.remove(Map.of("input", "minecraft:cod"));
        });
        
        TradeAPI.executeBuilders(registry);
        
        PatchTarget<TradeTable> target = new PatchTarget<>(
            professionId,
            TradeTable.class
        );
        List<PatchSet<TradeTable>> patches = registry.getPatchesFor(target);
        
        assertEquals(1, patches.size());
        assertEquals(professionId, patches.get(0).target().id(),
            "Patch set should have correct target identifier");
        assertEquals(TradeTable.class, patches.get(0).target().type(),
            "Patch set should have correct target type");
    }
    
    @Test
    void testExecuteBuilders_RegistersPatchSetsWithCorrectPriority() {
        // Requirement 10.4: Register PatchSets with correct priority
        
        int customPriority = 100;
        
        TradeAPI.modify(professionId, trades -> {
            trades.remove(Map.of("input", "minecraft:cod"));
        }, customPriority);
        
        TradeAPI.executeBuilders(registry);
        
        PatchTarget<TradeTable> target = new PatchTarget<>(
            professionId,
            TradeTable.class
        );
        List<PatchSet<TradeTable>> patches = registry.getPatchesFor(target);
        
        assertEquals(1, patches.size());
        assertEquals(customPriority, patches.get(0).priority(),
            "Patch set should have correct priority");
    }
    
    @Test
    void testExecuteBuilders_TranslatesBuilderOperationsToPatchOperations() {
        // Requirement 10.3: Translate builder operations to patch operations
        
        TradeAPI.modify(professionId, trades -> {
            trades.remove(Map.of("input", "minecraft:cod"));
            trades.replaceInput(
                Map.of("input", "minecraft:salmon"),
                Identifier.of("minecraft:tropical_fish")
            );
        });
        
        TradeAPI.executeBuilders(registry);
        
        PatchTarget<TradeTable> target = new PatchTarget<>(
            professionId,
            TradeTable.class
        );
        List<PatchSet<TradeTable>> patches = registry.getPatchesFor(target);
        
        assertEquals(1, patches.size());
        assertEquals(2, patches.get(0).operations().size(),
            "Patch set should contain two operations");
    }
    
    @Test
    void testExecuteBuilders_NullRegistry_ThrowsException() {
        // Requirement 10.4: Validate registry parameter
        
        assertThrows(
            NullPointerException.class,
            () -> TradeAPI.executeBuilders(null)
        );
    }
    
    @Test
    void testExecuteBuilders_EmptyPendingModifications_CompletesSuccessfully() {
        // Requirement 10.3: Handle empty modifications list
        
        assertDoesNotThrow(() -> TradeAPI.executeBuilders(registry));
    }
    
    @Test
    void testExecuteBuilders_BuilderThrowsException_ContinuesWithOthers() {
        // Requirement 10.3: Handle builder failures gracefully
        
        TradeAPI.modify(professionId, trades -> {
            throw new RuntimeException("Test exception");
        });
        
        TradeAPI.modify(Identifier.of("minecraft:villager/farmer"), trades -> {
            trades.remove(Map.of("input", "minecraft:wheat"));
        });
        
        // Should not throw exception
        assertDoesNotThrow(() -> TradeAPI.executeBuilders(registry));
        
        // Should still register the successful modification
        PatchTarget<TradeTable> farmerTarget = new PatchTarget<>(
            Identifier.of("minecraft:villager/farmer"),
            TradeTable.class
        );
        List<PatchSet<TradeTable>> farmerPatches = registry.getPatchesFor(farmerTarget);
        assertEquals(1, farmerPatches.size(),
            "Should register successful patch set despite other failures");
    }
    
    @Test
    void testExecuteBuilders_ClearsPendingModifications() {
        // Requirement 10.3: Clear pending modifications after execution
        
        TradeAPI.modify(professionId, trades -> {
            trades.remove(Map.of("input", "minecraft:cod"));
        });
        
        assertEquals(1, TradeAPI.getPendingModificationCount());
        
        TradeAPI.executeBuilders(registry);
        
        assertEquals(0, TradeAPI.getPendingModificationCount(),
            "Should clear pending modifications after execution");
    }
    
    @Test
    void testExecuteBuilders_CanBeCalledMultipleTimes() {
        // Requirement 10.3: Support multiple executions
        
        TradeAPI.modify(professionId, trades -> {
            trades.remove(Map.of("input", "minecraft:cod"));
        });
        
        TradeAPI.executeBuilders(registry);
        
        // Second execution with no pending modifications
        assertDoesNotThrow(() -> TradeAPI.executeBuilders(registry));
    }
}
