package com.tapestry.gameplay.patch;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PatchRegistry.
 * 
 * Tests cover:
 * - Successful registration before freeze
 * - Indexing by target
 * - Null target identifier rejection
 * - Post-freeze registration rejection
 * - Retrieval of patches by target
 * - Unmodifiable view of all patches
 */
class PatchRegistryTest {
    
    private PatchRegistry registry;
    
    // Mock operation for testing
    private static class MockOperation implements PatchOperation<String> {
        @Override
        public void apply(String target) {
            // No-op for testing
        }
    }
    
    @BeforeEach
    void setUp() {
        // ensure global singleton is reset to avoid interference from other tests
        PatchRegistry.reset();
        registry = new PatchRegistry();
    }
    
    @Test
    void testSuccessfulRegistrationBeforeFreeze() {
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
        
        // Should not throw
        assertDoesNotThrow(() -> registry.register(patchSet));
        
        // Verify it was registered
        List<PatchSet<String>> patches = registry.getPatchesFor(target);
        assertEquals(1, patches.size());
        assertEquals(patchSet, patches.get(0));
    }
    
    @Test
    void testIndexingByTarget() {
        Identifier modId = Identifier.of("testmod:test");
        
        // Create two different targets
        PatchTarget<String> target1 = new PatchTarget<>(
            Identifier.of("minecraft:target1"),
            String.class
        );
        PatchTarget<String> target2 = new PatchTarget<>(
            Identifier.of("minecraft:target2"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet1 = new PatchSet<>(
            modId, target1, PatchPriority.NORMAL, operations, Optional.empty()
        );
        PatchSet<String> patchSet2 = new PatchSet<>(
            modId, target2, PatchPriority.NORMAL, operations, Optional.empty()
        );
        
        registry.register(patchSet1);
        registry.register(patchSet2);
        
        // Verify each target has its own patches
        List<PatchSet<String>> patches1 = registry.getPatchesFor(target1);
        List<PatchSet<String>> patches2 = registry.getPatchesFor(target2);
        
        assertEquals(1, patches1.size());
        assertEquals(1, patches2.size());
        assertEquals(patchSet1, patches1.get(0));
        assertEquals(patchSet2, patches2.get(0));
    }
    
    @Test
    void testMultiplePatchSetsForSameTarget() {
        Identifier modId1 = Identifier.of("testmod1:test");
        Identifier modId2 = Identifier.of("testmod2:test");
        
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet1 = new PatchSet<>(
            modId1, target, PatchPriority.NORMAL, operations, Optional.empty()
        );
        PatchSet<String> patchSet2 = new PatchSet<>(
            modId2, target, PatchPriority.NORMAL, operations, Optional.empty()
        );
        
        registry.register(patchSet1);
        registry.register(patchSet2);
        
        // Both patch sets should be indexed under the same target
        List<PatchSet<String>> patches = registry.getPatchesFor(target);
        assertEquals(2, patches.size());
        assertTrue(patches.contains(patchSet1));
        assertTrue(patches.contains(patchSet2));
    }
    
    @Test
    void testNullTargetIdentifierRejection() {
        // Note: PatchTarget itself validates non-null identifier in its constructor,
        // so we can't actually create a PatchTarget with null identifier.
        // This test verifies that the registry would reject it if it somehow got one.
        // In practice, the PatchTarget constructor will throw first.
        
        // Verify PatchTarget rejects null identifier
        assertThrows(NullPointerException.class, () -> {
            new PatchTarget<String>(null, String.class);
        });
    }

    @Test
    void testInvalidPriorityRejection() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());

        // priority outside allowed range
        assertThrows(IllegalArgumentException.class, () -> {
            PatchSet<String> bad = new PatchSet<>(
                modId, target, 2000, operations, Optional.empty()
            );
            registry.register(bad);
        });
    }

    @Test
    void testEmptyOperationsListRejection() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of();

        assertThrows(IllegalArgumentException.class, () -> {
            PatchSet<String> bad = new PatchSet<>(
                modId, target, PatchPriority.NORMAL, operations, Optional.empty()
            );
            registry.register(bad);
        });
    }
    
    @Test
    void testPostFreezeRegistrationRejection() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet = new PatchSet<>(
            modId, target, PatchPriority.NORMAL, operations, Optional.empty()
        );
        
        // Freeze the registry
        registry.freeze();
        
        // Attempt to register after freeze should throw
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> registry.register(patchSet)
        );
        
        assertTrue(exception.getMessage().contains("Cannot register patches after TS_REGISTER phase"));
    }
    
    @Test
    void testFreezeMethod() {
        // Should not throw
        assertDoesNotThrow(() -> registry.freeze());
        
        // Freezing multiple times should be safe
        assertDoesNotThrow(() -> registry.freeze());
    }
    
    @Test
    void testGetPatchesForNonExistentTarget() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:nonexistent"),
            String.class
        );
        
        List<PatchSet<String>> patches = registry.getPatchesFor(target);
        
        // Should return empty list, not null
        assertNotNull(patches);
        assertTrue(patches.isEmpty());
    }
    
    @Test
    void testGetAllPatches() {
        Identifier modId = Identifier.of("testmod:test");
        
        PatchTarget<String> target1 = new PatchTarget<>(
            Identifier.of("minecraft:target1"),
            String.class
        );
        PatchTarget<String> target2 = new PatchTarget<>(
            Identifier.of("minecraft:target2"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet1 = new PatchSet<>(
            modId, target1, PatchPriority.NORMAL, operations, Optional.empty()
        );
        PatchSet<String> patchSet2 = new PatchSet<>(
            modId, target2, PatchPriority.NORMAL, operations, Optional.empty()
        );
        
        registry.register(patchSet1);
        registry.register(patchSet2);
        
        Map<PatchTarget<?>, List<PatchSet<?>>> allPatches = registry.getAllPatches();
        
        assertEquals(2, allPatches.size());
        assertTrue(allPatches.containsKey(target1));
        assertTrue(allPatches.containsKey(target2));
    }
    
    @Test
    void testGetAllPatchesReturnsUnmodifiableMap() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet = new PatchSet<>(
            modId, target, PatchPriority.NORMAL, operations, Optional.empty()
        );
        
        registry.register(patchSet);
        
        Map<PatchTarget<?>, List<PatchSet<?>>> allPatches = registry.getAllPatches();
        
        // Attempting to modify the map should throw
        PatchTarget<String> newTarget = new PatchTarget<>(
            Identifier.of("minecraft:new_target"),
            String.class
        );
        
        assertThrows(UnsupportedOperationException.class, () -> {
            allPatches.put(newTarget, List.of());
        });
    }
    
    @Test
    void testEmptyRegistryGetAllPatches() {
        Map<PatchTarget<?>, List<PatchSet<?>>> allPatches = registry.getAllPatches();
        
        assertNotNull(allPatches);
        assertTrue(allPatches.isEmpty());
    }
    
    @Test
    void testRegistrationOrderPreserved() {
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet1 = new PatchSet<>(
            Identifier.of("mod1:test"), target, PatchPriority.NORMAL, operations, Optional.empty()
        );
        PatchSet<String> patchSet2 = new PatchSet<>(
            Identifier.of("mod2:test"), target, PatchPriority.NORMAL, operations, Optional.empty()
        );
        PatchSet<String> patchSet3 = new PatchSet<>(
            Identifier.of("mod3:test"), target, PatchPriority.NORMAL, operations, Optional.empty()
        );
        
        registry.register(patchSet1);
        registry.register(patchSet2);
        registry.register(patchSet3);
        
        List<PatchSet<String>> patches = registry.getPatchesFor(target);
        
        // Order should be preserved
        assertEquals(3, patches.size());
        assertEquals(patchSet1, patches.get(0));
        assertEquals(patchSet2, patches.get(1));
        assertEquals(patchSet3, patches.get(2));
    }
    
    @Test
    void testGetInstance_ReturnsSameInstance() {
        // Requirement 4.2, 6.1: Singleton instance for lifecycle integration
        PatchRegistry instance1 = PatchRegistry.getInstance();
        PatchRegistry instance2 = PatchRegistry.getInstance();
        
        assertNotNull(instance1);
        assertSame(instance1, instance2, "getInstance should return the same instance");
    }
    
    @Test
    void testGetInstance_CanRegisterPatches() {
        // Requirement 4.2: Singleton instance accepts registrations
        PatchRegistry instance = PatchRegistry.getInstance();
        
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet = new PatchSet<>(
            modId, target, PatchPriority.NORMAL, operations, Optional.empty()
        );
        
        assertDoesNotThrow(() -> instance.register(patchSet));
        
        List<PatchSet<String>> patches = instance.getPatchesFor(target);
        assertEquals(1, patches.size());
    }
    
    @Test
    void testReset_ClearsInstance() {
        // Test utility method for test isolation
        PatchRegistry instance1 = PatchRegistry.getInstance();
        assertNotNull(instance1);
        
        PatchRegistry.reset();
        
        PatchRegistry instance2 = PatchRegistry.getInstance();
        assertNotNull(instance2);
        assertNotSame(instance1, instance2, "reset should clear the singleton instance");
    }
    
    @Test
    void testIsFrozen_InitiallyFalse() {
        // Requirement 4.2: Registry starts unfrozen
        PatchRegistry registry = new PatchRegistry();
        assertFalse(registry.isFrozen(), "Registry should not be frozen initially");
    }
    
    @Test
    void testIsFrozen_TrueAfterFreeze() {
        // Requirement 4.3: Registry is frozen after freeze() call
        PatchRegistry registry = new PatchRegistry();
        registry.freeze();
        assertTrue(registry.isFrozen(), "Registry should be frozen after freeze() call");
    }
    
    @Test
    void testLifecycleIntegration_AcceptsDuringRegisterPhase() {
        // Requirement 6.1: Accept registrations during TS_REGISTER phase
        // This is implicitly tested by all registration tests, but we verify explicitly
        PatchRegistry registry = new PatchRegistry();
        
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet = new PatchSet<>(
            modId, target, PatchPriority.NORMAL, operations, Optional.empty()
        );
        
        // Before freeze (simulating TS_REGISTER phase)
        assertDoesNotThrow(() -> registry.register(patchSet));
        assertFalse(registry.isFrozen());
    }
    
    @Test
    void testLifecycleIntegration_RejectsAfterPhaseCompletes() {
        // Requirement 6.7: Freeze after TS_REGISTER phase completes
        PatchRegistry registry = new PatchRegistry();
        
        // Simulate phase completion
        registry.freeze();
        
        Identifier modId = Identifier.of("testmod:test");
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet = new PatchSet<>(
            modId, target, PatchPriority.NORMAL, operations, Optional.empty()
        );
        
        // After freeze (simulating post-TS_REGISTER phase)
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> registry.register(patchSet)
        );
        
        assertTrue(exception.getMessage().contains("Cannot register patches after TS_REGISTER phase"));
        assertTrue(registry.isFrozen());
    }
}

