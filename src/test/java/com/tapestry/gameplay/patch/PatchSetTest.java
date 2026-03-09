package com.tapestry.gameplay.patch;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PatchSet record.
 */
class PatchSetTest {
    
    // Mock operation for testing
    private static class MockOperation implements PatchOperation<String> {
        @Override
        public void apply(String target) {
            // No-op for testing
        }
    }
    
    @Test
    void testValidPatchSetCreation() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet = new PatchSet<>(
            modId,
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        assertEquals(modId, patchSet.modId());
        assertEquals(target, patchSet.target());
        assertEquals(PatchPriority.NORMAL, patchSet.priority());
        assertEquals(1, patchSet.operations().size());
        assertTrue(patchSet.condition().isEmpty());
    }
    
    @Test
    void testPatchSetWithCondition() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        PatchCondition condition = PatchCondition.modLoaded("example_mod");
        
        PatchSet<String> patchSet = new PatchSet<>(
            modId,
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.of(condition)
        );
        
        assertTrue(patchSet.condition().isPresent());
    }
    
    @Test
    void testNullModId() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new PatchSet<>(
                null,
                target,
                PatchPriority.NORMAL,
                operations,
                Optional.empty()
            )
        );
        
        assertTrue(exception.getMessage().contains("Mod identifier cannot be null"));
    }
    
    @Test
    void testNullTarget() {
        Identifier modId = Identifier.of("testmod:test");
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new PatchSet<>(
                modId,
                null,
                PatchPriority.NORMAL,
                operations,
                Optional.empty()
            )
        );
        
        assertTrue(exception.getMessage().contains("Patch target cannot be null"));
    }
    
    @Test
    void testNullOperations() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new PatchSet<>(
                modId,
                target,
                PatchPriority.NORMAL,
                null,
                Optional.empty()
            )
        );
        
        assertTrue(exception.getMessage().contains("Operations list cannot be null"));
    }
    
    @Test
    void testNullCondition() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new PatchSet<>(
                modId,
                target,
                PatchPriority.NORMAL,
                operations,
                null
            )
        );
        
        assertTrue(exception.getMessage().contains("Condition must be non-null Optional"));
    }
    
    @Test
    void testDefensiveCopyOfOperations() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        // Create a mutable list
        List<PatchOperation<String>> operations = new ArrayList<>();
        operations.add(new MockOperation());
        
        PatchSet<String> patchSet = new PatchSet<>(
            modId,
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        // Modify the original list
        operations.add(new MockOperation());
        
        // PatchSet should still have only 1 operation (defensive copy)
        assertEquals(1, patchSet.operations().size());
    }
    
    @Test
    void testOperationsListIsImmutable() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet = new PatchSet<>(
            modId,
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        // Should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> {
            patchSet.operations().add(new MockOperation());
        });
    }
    
    @Test
    void testShouldApply_NoCondition() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet = new PatchSet<>(
            modId,
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        // Mock context (not used when no condition)
        PatchContext context = new PatchContext() {
            @Override
            public boolean isModLoaded(String modId) {
                return false;
            }
            
            @Override
            public boolean registryContains(Identifier id) {
                return false;
            }
            
            @Override
            public boolean traitExists(Identifier traitId) {
                return false;
            }
            
            @Override
            public net.minecraft.registry.Registry<?> getRegistry(Identifier registryId) {
                return null;
            }
        };
        
        // Should always return true when no condition
        assertTrue(patchSet.shouldApply(context));
    }
    
    @Test
    void testShouldApply_ConditionTrue() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        PatchCondition condition = PatchCondition.modLoaded("example_mod");
        
        PatchSet<String> patchSet = new PatchSet<>(
            modId,
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.of(condition)
        );
        
        // Mock context where mod is loaded
        PatchContext context = new PatchContext() {
            @Override
            public boolean isModLoaded(String modId) {
                return "example_mod".equals(modId);
            }
            
            @Override
            public boolean registryContains(Identifier id) {
                return false;
            }
            
            @Override
            public boolean traitExists(Identifier traitId) {
                return false;
            }
            
            @Override
            public net.minecraft.registry.Registry<?> getRegistry(Identifier registryId) {
                return null;
            }
        };
        
        assertTrue(patchSet.shouldApply(context));
    }
    
    @Test
    void testShouldApply_ConditionFalse() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        PatchCondition condition = PatchCondition.modLoaded("example_mod");
        
        PatchSet<String> patchSet = new PatchSet<>(
            modId,
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.of(condition)
        );
        
        // Mock context where mod is NOT loaded
        PatchContext context = new PatchContext() {
            @Override
            public boolean isModLoaded(String modId) {
                return false;
            }
            
            @Override
            public boolean registryContains(Identifier id) {
                return false;
            }
            
            @Override
            public boolean traitExists(Identifier traitId) {
                return false;
            }
            
            @Override
            public net.minecraft.registry.Registry<?> getRegistry(Identifier registryId) {
                return null;
            }
        };
        
        assertFalse(patchSet.shouldApply(context));
    }
    
    @Test
    void testPriorityValues() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        // Test various priority values
        PatchSet<String> veryEarly = new PatchSet<>(
            modId, target, PatchPriority.VERY_EARLY, operations, Optional.empty()
        );
        PatchSet<String> early = new PatchSet<>(
            modId, target, PatchPriority.EARLY, operations, Optional.empty()
        );
        PatchSet<String> normal = new PatchSet<>(
            modId, target, PatchPriority.NORMAL, operations, Optional.empty()
        );
        PatchSet<String> late = new PatchSet<>(
            modId, target, PatchPriority.LATE, operations, Optional.empty()
        );
        PatchSet<String> veryLate = new PatchSet<>(
            modId, target, PatchPriority.VERY_LATE, operations, Optional.empty()
        );
        // verify priority getters
        assertEquals(PatchPriority.VERY_EARLY, veryEarly.priority());
        assertEquals(PatchPriority.EARLY, early.priority());
        assertEquals(PatchPriority.NORMAL, normal.priority());
        assertEquals(PatchPriority.LATE, late.priority());
        assertEquals(PatchPriority.VERY_LATE, veryLate.priority());
    }
    
    @Test
    void constructor_withInvalidPriority_throwsException() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());

        assertThrows(IllegalArgumentException.class, () -> {
            new PatchSet<>(modId, target, 5000, operations, Optional.empty());
        });
    }

    @Test
    void constructor_withEmptyOperationsList_throwsException() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of();

        assertThrows(IllegalArgumentException.class, () -> {
            new PatchSet<>(modId, target, PatchPriority.NORMAL, operations, Optional.empty());
        });
    }
}

