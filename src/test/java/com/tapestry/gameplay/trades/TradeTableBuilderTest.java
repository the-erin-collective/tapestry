package com.tapestry.gameplay.trades;

import com.tapestry.gameplay.patch.PatchOperation;
import com.tapestry.gameplay.patch.PatchSet;
import com.tapestry.gameplay.patch.PatchTarget;
import com.tapestry.gameplay.trades.operations.RemoveTradeOperation;
import com.tapestry.gameplay.trades.operations.ReplaceTradeInputOperation;
import com.tapestry.gameplay.trades.operations.ReplaceTradeOutputOperation;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TradeTableBuilder}.
 */
class TradeTableBuilderTest {
    
    private TradeTableBuilder builder;
    private Identifier modId;
    private PatchTarget<TradeTable> target;
    
    @BeforeEach
    void setUp() {
        builder = new TradeTableBuilder();
        modId = Identifier.of("testmod:test");
        target = new PatchTarget<>(
            Identifier.of("minecraft:villager/fisherman"),
            TradeTable.class
        );
    }
    
    @Test
    void testRemove_AddsRemoveOperation() {
        Map<String, Object> filterSpec = Map.of(
            "input", "minecraft:cod",
            "level", 1
        );
        
        PatchSet<TradeTable> patchSet = builder
            .remove(filterSpec)
            .build(modId, target, 0);
        
        assertEquals(1, patchSet.operations().size());
        assertTrue(patchSet.operations().get(0) instanceof RemoveTradeOperation);
    }
    
    @Test
    void testRemove_NullFilterSpec_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            builder.remove(null);
        });
    }
    
    @Test
    void testReplaceInput_AddsReplaceInputOperation() {
        Map<String, Object> filterSpec = Map.of("input", "minecraft:cod");
        Identifier newInput = Identifier.of("minecraft:salmon");
        
        PatchSet<TradeTable> patchSet = builder
            .replaceInput(filterSpec, newInput)
            .build(modId, target, 0);
        
        assertEquals(1, patchSet.operations().size());
        assertTrue(patchSet.operations().get(0) instanceof ReplaceTradeInputOperation);
        
        ReplaceTradeInputOperation operation = 
            (ReplaceTradeInputOperation) patchSet.operations().get(0);
        assertEquals(newInput, operation.newInput());
    }
    
    @Test
    void testReplaceInput_NullFilterSpec_ThrowsException() {
        Identifier newInput = Identifier.of("minecraft:salmon");
        assertThrows(NullPointerException.class, () -> {
            builder.replaceInput(null, newInput);
        });
    }
    
    @Test
    void testReplaceInput_NullNewInput_ThrowsException() {
        Map<String, Object> filterSpec = Map.of("input", "minecraft:cod");
        assertThrows(NullPointerException.class, () -> {
            builder.replaceInput(filterSpec, null);
        });
    }
    
    @Test
    void testReplaceOutput_AddsReplaceOutputOperation() {
        Map<String, Object> filterSpec = Map.of("output", "minecraft:emerald");
        Identifier newOutput = Identifier.of("minecraft:diamond");
        
        PatchSet<TradeTable> patchSet = builder
            .replaceOutput(filterSpec, newOutput)
            .build(modId, target, 0);
        
        assertEquals(1, patchSet.operations().size());
        assertTrue(patchSet.operations().get(0) instanceof ReplaceTradeOutputOperation);
        
        ReplaceTradeOutputOperation operation = 
            (ReplaceTradeOutputOperation) patchSet.operations().get(0);
        assertEquals(newOutput, operation.newOutput());
    }
    
    @Test
    void testReplaceOutput_NullFilterSpec_ThrowsException() {
        Identifier newOutput = Identifier.of("minecraft:diamond");
        assertThrows(NullPointerException.class, () -> {
            builder.replaceOutput(null, newOutput);
        });
    }
    
    @Test
    void testReplaceOutput_NullNewOutput_ThrowsException() {
        Map<String, Object> filterSpec = Map.of("output", "minecraft:emerald");
        assertThrows(NullPointerException.class, () -> {
            builder.replaceOutput(filterSpec, null);
        });
    }
    
    @Test
    void testAdd_ThrowsUnsupportedOperationException() {
        Map<String, Object> tradeSpec = Map.of(
            "input", "minecraft:nautilus_shell",
            "output", "minecraft:emerald",
            "level", 2
        );
        
        // Currently not implemented, should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> {
            builder.add(tradeSpec);
        });
    }
    
    @Test
    void testAdd_NullTradeSpec_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            builder.add(null);
        });
    }
    
    @Test
    void testBuild_CreatesValidPatchSet() {
        Map<String, Object> filterSpec = Map.of("input", "minecraft:cod");
        
        PatchSet<TradeTable> patchSet = builder
            .remove(filterSpec)
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
        Map<String, Object> removeSpec = Map.of("input", "minecraft:cod", "level", 1);
        Map<String, Object> replaceInputSpec = Map.of("input", "minecraft:salmon");
        Map<String, Object> replaceOutputSpec = Map.of("output", "minecraft:emerald");
        Identifier newInput = Identifier.of("minecraft:tropical_fish");
        Identifier newOutput = Identifier.of("minecraft:diamond");
        
        PatchSet<TradeTable> patchSet = builder
            .remove(removeSpec)
            .replaceInput(replaceInputSpec, newInput)
            .replaceOutput(replaceOutputSpec, newOutput)
            .build(modId, target, 0);
        
        assertEquals(3, patchSet.operations().size());
        assertTrue(patchSet.operations().get(0) instanceof RemoveTradeOperation);
        assertTrue(patchSet.operations().get(1) instanceof ReplaceTradeInputOperation);
        assertTrue(patchSet.operations().get(2) instanceof ReplaceTradeOutputOperation);
    }
    
    @Test
    void testBuild_WithCustomPriority() {
        Map<String, Object> filterSpec = Map.of("input", "minecraft:cod");
        int customPriority = 100;
        
        PatchSet<TradeTable> patchSet = builder
            .remove(filterSpec)
            .build(modId, target, customPriority);
        
        assertEquals(customPriority, patchSet.priority());
    }
    
    @Test
    void testBuild_EmptyOperations_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> builder.build(modId, target, 0));
    }
    
    @Test
    void testBuilder_CanBeReused() {
        Map<String, Object> filterSpec1 = Map.of("input", "minecraft:cod");
        Map<String, Object> filterSpec2 = Map.of("input", "minecraft:salmon");
        
        PatchSet<TradeTable> patchSet1 = builder
            .remove(filterSpec1)
            .build(modId, target, 0);
        
        PatchSet<TradeTable> patchSet2 = builder
            .remove(filterSpec2)
            .build(modId, target, 0);
        
        // First patch set should have 1 operation
        assertEquals(1, patchSet1.operations().size());
        
        // Second patch set should have 2 operations (accumulated)
        assertEquals(2, patchSet2.operations().size());
    }
    
    @Test
    void testBuild_OperationsListIsIndependent() {
        Map<String, Object> filterSpec = Map.of("input", "minecraft:cod");
        
        builder.remove(filterSpec);
        PatchSet<TradeTable> patchSet = builder.build(modId, target, 0);
        
        // Add another operation after building
        builder.remove(Map.of("input", "minecraft:salmon"));
        
        // Original patch set should still have only 1 operation
        assertEquals(1, patchSet.operations().size());
    }
}
