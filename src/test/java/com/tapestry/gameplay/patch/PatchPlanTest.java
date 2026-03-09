package com.tapestry.gameplay.patch;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import com.tapestry.gameplay.loot.operations.RemoveEntryOperation;
import com.tapestry.gameplay.loot.filter.LootPoolFilter;
import com.tapestry.gameplay.loot.filter.LootEntryFilter;
import net.minecraft.loot.LootTable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PatchPlan.
 * 
 * Tests cover:
 * - Compilation from registry
 * - Sorted patch retrieval
 * - Compilation statistics
 * - Immutability of compiled plan
 * - Null parameter handling
 */
class PatchPlanTest {
    
    private PatchRegistry registry;
    private ModLoadOrder modLoadOrder;
    
    // simple stub context used by tests to avoid triggering mc registry initialization
    private static final PatchContext TEST_CONTEXT = new PatchContext() {
        @Override public boolean isModLoaded(String modId) { return false; }
        @Override public boolean registryContains(Identifier id) { return false; }
        @Override public boolean traitExists(Identifier traitId) { return false; }
        @Override public net.minecraft.registry.Registry<?> getRegistry(Identifier registryId) { return null; }
    };
    
    // Mock operation for testing
    private static class MockOperation implements PatchOperation<String> {
        @Override
        public void apply(String target) {
            // No-op for testing
        }
    }
    
    // Simple mod load order implementation for testing
    private static class TestModLoadOrder implements ModLoadOrder {
        @Override
        public int compare(Identifier modA, Identifier modB) {
            // Simple alphabetical ordering for testing
            return modA.toString().compareTo(modB.toString());
        }
    }
    
    @BeforeEach
    void setUp() {
        registry = new PatchRegistry();
        modLoadOrder = new TestModLoadOrder();
    }
    
    @Test
    void testCompileFromEmptyRegistry() {
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        assertNotNull(plan);
        
        PatchStatistics stats = plan.getCompilationStats();
        assertEquals(0, stats.totalTargets());
        assertEquals(0, stats.totalOperations());
    }
    
    @Test
    void testCompileFromRegistryWithPatches() {
        // Create test data
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(
            new MockOperation(),
            new MockOperation()
        );
        
        PatchSet<String> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(patchSet);
        
        // Compile
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        assertNotNull(plan);
        
        // Verify statistics
        PatchStatistics stats = plan.getCompilationStats();
        assertEquals(1, stats.totalTargets());
        assertEquals(2, stats.totalOperations());
    }
    
    @Test
    void testGetPatchesForTarget() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(patchSet);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        List<PatchSet<String>> patches = plan.getPatchesFor(target);
        
        assertNotNull(patches);
        assertEquals(1, patches.size());
        assertEquals(patchSet, patches.get(0));
    }
    
    @Test
    void testGetPatchesForNonExistentTarget() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:nonexistent"),
            String.class
        );
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        List<PatchSet<String>> patches = plan.getPatchesFor(target);
        
        assertNotNull(patches);
        assertTrue(patches.isEmpty());
    }
    
    @Test
    void testPatchesSortedByPriority() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        // Register patches with different priorities
        PatchSet<String> latePatch = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.LATE,
            operations,
            Optional.empty()
        );
        
        PatchSet<String> earlyPatch = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.EARLY,
            operations,
            Optional.empty()
        );
        
        PatchSet<String> normalPatch = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        // Register in non-sorted order
        registry.register(latePatch);
        registry.register(earlyPatch);
        registry.register(normalPatch);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        List<PatchSet<String>> patches = plan.getPatchesFor(target);
        
        // Should be sorted by priority: EARLY < NORMAL < LATE
        assertEquals(3, patches.size());
        assertEquals(earlyPatch, patches.get(0));
        assertEquals(normalPatch, patches.get(1));
        assertEquals(latePatch, patches.get(2));
    }

    @Test
    void compile_withInvalidItemIdentifier_throwsException() {
        // create patch set with a dummy operation that references a bad id via filter
        PatchTarget<LootTable> lootTarget = new PatchTarget<>(
            Identifier.of("minecraft:test_loot"), LootTable.class
        );
        LootEntryFilter filter = new LootEntryFilter(
            Optional.of(Identifier.of("minecraft:does_not_exist")),
            Optional.empty()
        );
        // Use RemoveEntryOperation to exercise filter validation
        RemoveEntryOperation op = new RemoveEntryOperation(
            new LootPoolFilter(Optional.empty(), Optional.empty(), Optional.empty()),
            filter
        );
        PatchSet<LootTable> patchSet = new PatchSet<>(
            Identifier.of("testmod:invalid"),
            lootTarget,
            PatchPriority.NORMAL,
            List.of(op),
            Optional.empty()
        );
        registry.register(patchSet);

        assertThrows(IllegalArgumentException.class, () -> {
            PatchPlan.compile(registry, modLoadOrder, TEST_CONTEXT);
        });
    }

    @Test
    void compile_withInvalidCondition_throwsException() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test"),
            String.class
        );
        List<PatchOperation<String>> ops = List.of(new MockOperation());
        PatchCondition badCond = PatchCondition.registryContains(Identifier.of("nonexistent:entry"));
        PatchSet<String> patchSet = new PatchSet<>(
            Identifier.of("testmod:cond"),
            target,
            PatchPriority.NORMAL,
            ops,
            Optional.of(badCond)
        );
        registry.register(patchSet);

        assertThrows(IllegalArgumentException.class, () -> {
            PatchPlan.compile(registry, modLoadOrder, TEST_CONTEXT);
        });
    }
    
    @Test
    void testPatchesSortedByModLoadOrder() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        // Register patches with same priority but different mod IDs
        // TestModLoadOrder sorts alphabetically
        PatchSet<String> patchC = new PatchSet<>(
            Identifier.of("modc:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        PatchSet<String> patchA = new PatchSet<>(
            Identifier.of("moda:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        PatchSet<String> patchB = new PatchSet<>(
            Identifier.of("modb:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        // Register in non-sorted order
        registry.register(patchC);
        registry.register(patchA);
        registry.register(patchB);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        List<PatchSet<String>> patches = plan.getPatchesFor(target);
        
        // Should be sorted by mod load order: moda < modb < modc
        assertEquals(3, patches.size());
        assertEquals(patchA, patches.get(0));
        assertEquals(patchB, patches.get(1));
        assertEquals(patchC, patches.get(2));
    }
    
    @Test
    void testMultipleTargets() {
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
            Identifier.of("testmod:test"),
            target1,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        PatchSet<String> patchSet2 = new PatchSet<>(
            Identifier.of("testmod:test"),
            target2,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(patchSet1);
        registry.register(patchSet2);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Verify statistics
        PatchStatistics stats = plan.getCompilationStats();
        assertEquals(2, stats.totalTargets());
        assertEquals(2, stats.totalOperations());
        
        // Verify each target has its patches
        List<PatchSet<String>> patches1 = plan.getPatchesFor(target1);
        List<PatchSet<String>> patches2 = plan.getPatchesFor(target2);
        
        assertEquals(1, patches1.size());
        assertEquals(1, patches2.size());
        assertEquals(patchSet1, patches1.get(0));
        assertEquals(patchSet2, patches2.get(0));
    }
    
    @Test
    void testCompilationStatisticsCountsAllOperations() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        // Create patch set with 3 operations
        List<PatchOperation<String>> operations = List.of(
            new MockOperation(),
            new MockOperation(),
            new MockOperation()
        );
        
        PatchSet<String> patchSet1 = new PatchSet<>(
            Identifier.of("mod1:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        // Create another patch set with 2 operations
        List<PatchOperation<String>> operations2 = List.of(
            new MockOperation(),
            new MockOperation()
        );
        
        PatchSet<String> patchSet2 = new PatchSet<>(
            Identifier.of("mod2:test"),
            target,
            PatchPriority.NORMAL,
            operations2,
            Optional.empty()
        );
        
        registry.register(patchSet1);
        registry.register(patchSet2);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        PatchStatistics stats = plan.getCompilationStats();
        assertEquals(1, stats.totalTargets());
        assertEquals(5, stats.totalOperations()); // 3 + 2
    }
    
    @Test
    void testCompileWithNullRegistryThrows() {
        assertThrows(NullPointerException.class, () -> {
            PatchPlan.compile(null, modLoadOrder);
        });
    }
    
    @Test
    void testCompileWithNullModLoadOrderThrows() {
        assertThrows(NullPointerException.class, () -> {
            PatchPlan.compile(registry, null);
        });
    }
    
    @Test
    void testCompilationStatsInitializedToZero() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(patchSet);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        PatchStatistics stats = plan.getCompilationStats();
        
        // These counters should be zero at compilation time
        assertEquals(0, stats.applied());
        assertEquals(0, stats.failed());
        assertEquals(0, stats.skipped());
        assertEquals(0, stats.noOps());
        assertEquals(0, stats.missingTargets());
    }
    
    @Test
    void testPlanIsImmutable() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(new MockOperation());
        
        PatchSet<String> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(patchSet);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Get patches
        List<PatchSet<String>> patches = plan.getPatchesFor(target);
        
        // Verify we got the patch
        assertEquals(1, patches.size());
        
        // Register another patch after compilation
        PatchSet<String> newPatchSet = new PatchSet<>(
            Identifier.of("newmod:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(newPatchSet);
        
        // The plan should still only have the original patch
        List<PatchSet<String>> patchesAfter = plan.getPatchesFor(target);
        assertEquals(1, patchesAfter.size());
        assertEquals(patchSet, patchesAfter.get(0));
    }
    
    @Test
    void testGetAllTargets() {
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
            Identifier.of("testmod:test"),
            target1,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        PatchSet<String> patchSet2 = new PatchSet<>(
            Identifier.of("testmod:test"),
            target2,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(patchSet1);
        registry.register(patchSet2);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Get all targets
        var allTargets = plan.getAllTargets();
        
        assertNotNull(allTargets);
        assertEquals(2, allTargets.size());
        assertTrue(allTargets.containsKey(target1));
        assertTrue(allTargets.containsKey(target2));
        
        // Verify the returned map is immutable
        assertThrows(UnsupportedOperationException.class, () -> {
            allTargets.put(new PatchTarget<>(Identifier.of("test:new"), String.class), List.of());
        });
    }
}
