package com.tapestry.gameplay.patch;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for bootstrap patch application infrastructure.
 * 
 * <p>These tests verify that the PatchPlan compilation and PatchEngine
 * initialization work correctly during the bootstrap phase.</p>
 */
class BootstrapPatchApplicationTest {
    
    private PatchRegistry registry;
    private ModLoadOrder modLoadOrder;
    
    // Mock operation for testing
    private static class MockOperation<T> implements PatchOperation<T> {
        private boolean applied = false;
        
        @Override
        public void apply(T target) {
            applied = true;
        }
        
        public boolean wasApplied() {
            return applied;
        }
        
        @Override
        public Optional<String> getDebugId() {
            return Optional.of("MockOperation");
        }
    }
    
    // Mock gameplay data type
    private static class MockGameplayData {
        private String state = "initial";
        
        public void setState(String state) {
            this.state = state;
        }
        
        public String getState() {
            return state;
        }
    }
    
    @BeforeEach
    void setUp() {
        registry = new PatchRegistry();
        modLoadOrder = (modA, modB) -> modA.toString().compareTo(modB.toString());
    }
    
    @Test
    void testCompilePatchPlanFromEmptyRegistry() {
        // Freeze empty registry
        registry.freeze();
        
        // Compile plan
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        assertNotNull(plan);
        assertEquals(0, plan.getCompilationStats().totalTargets());
        assertEquals(0, plan.getCompilationStats().totalOperations());
    }
    
    @Test
    void testCompilePatchPlanWithRegisteredPatches() {
        // Create target and operations
        PatchTarget<MockGameplayData> target = new PatchTarget<>(
            Identifier.of("minecraft:test_data"),
            MockGameplayData.class
        );
        
        MockOperation<MockGameplayData> op1 = new MockOperation<>();
        MockOperation<MockGameplayData> op2 = new MockOperation<>();
        
        // Create patch set
        PatchSet<MockGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            0,
            List.of(op1, op2),
            Optional.empty()
        );
        
        // Register and freeze
        registry.register(patchSet);
        registry.freeze();
        
        // Compile plan
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        assertNotNull(plan);
        assertEquals(1, plan.getCompilationStats().totalTargets());
        assertEquals(2, plan.getCompilationStats().totalOperations());
        
        // Verify patches are in the plan
        List<PatchSet<MockGameplayData>> patches = plan.getPatchesFor(target);
        assertEquals(1, patches.size());
        assertEquals(2, patches.get(0).operations().size());
    }
    
    @Test
    void testPatchEngineCreationWithCompiledPlan() {
        // Create and compile plan
        registry.freeze();
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create patch context
        PatchContext context = new PatchContext() {
            @Override
            public boolean isModLoaded(String modId) {
                return true;
            }
            
            @Override
            public boolean registryContains(Identifier id) {
                return true;
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
        
        // Create configuration
        Map<String, Object> config = new HashMap<>();
        config.put("tapestry.strictPatches", false);
        config.put("tapestry.debugPatches", false);
        config.put("tapestry.patchPerformanceThreshold", 100);
        
        // Create engine
        PatchEngine engine = new PatchEngine(plan, context, config);
        
        assertNotNull(engine);
    }
    
    @Test
    void testBootstrapPatchApplicationFlow() {
        // Create target and operations
        PatchTarget<MockGameplayData> target = new PatchTarget<>(
            Identifier.of("minecraft:test_data"),
            MockGameplayData.class
        );
        
        MockOperation<MockGameplayData> op1 = new MockOperation<>();
        MockOperation<MockGameplayData> op2 = new MockOperation<>();
        
        // Create patch set
        PatchSet<MockGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            0,
            List.of(op1, op2),
            Optional.empty()
        );
        
        // Register and freeze
        registry.register(patchSet);
        registry.freeze();
        
        // Compile plan
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create context
        PatchContext context = new PatchContext() {
            @Override
            public boolean isModLoaded(String modId) {
                return true;
            }
            
            @Override
            public boolean registryContains(Identifier id) {
                return true;
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
        
        // Create configuration
        Map<String, Object> config = new HashMap<>();
        config.put("tapestry.strictPatches", false);
        config.put("tapestry.debugPatches", false);
        config.put("tapestry.patchPerformanceThreshold", 100);
        
        // Create engine
        PatchEngine engine = new PatchEngine(plan, context, config);
        
        // Apply patches to mock data
        MockGameplayData data = new MockGameplayData();
        MockGameplayData result = engine.applyPatches(target, data);
        
        // Verify patches were applied
        assertSame(data, result);
        assertTrue(op1.wasApplied());
        assertTrue(op2.wasApplied());
    }
    
    @Test
    void testMultipleTargetsInBootstrap() {
        // Create two different targets
        PatchTarget<MockGameplayData> target1 = new PatchTarget<>(
            Identifier.of("minecraft:data1"),
            MockGameplayData.class
        );
        
        PatchTarget<MockGameplayData> target2 = new PatchTarget<>(
            Identifier.of("minecraft:data2"),
            MockGameplayData.class
        );
        
        MockOperation<MockGameplayData> op1 = new MockOperation<>();
        MockOperation<MockGameplayData> op2 = new MockOperation<>();
        
        // Create patch sets for both targets
        PatchSet<MockGameplayData> patchSet1 = new PatchSet<>(
            Identifier.of("testmod:test"),
            target1,
            0,
            List.of(op1),
            Optional.empty()
        );
        
        PatchSet<MockGameplayData> patchSet2 = new PatchSet<>(
            Identifier.of("testmod:test"),
            target2,
            0,
            List.of(op2),
            Optional.empty()
        );
        
        // Register and freeze
        registry.register(patchSet1);
        registry.register(patchSet2);
        registry.freeze();
        
        // Compile plan
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Verify both targets are in the plan
        assertEquals(2, plan.getCompilationStats().totalTargets());
        assertEquals(2, plan.getCompilationStats().totalOperations());
        
        List<PatchSet<MockGameplayData>> patches1 = plan.getPatchesFor(target1);
        List<PatchSet<MockGameplayData>> patches2 = plan.getPatchesFor(target2);
        
        assertEquals(1, patches1.size());
        assertEquals(1, patches2.size());
    }
    
    @Test
    void testPatchPlanIsReusable() {
        // Create target and operations
        PatchTarget<MockGameplayData> target = new PatchTarget<>(
            Identifier.of("minecraft:test_data"),
            MockGameplayData.class
        );
        
        MockOperation<MockGameplayData> op = new MockOperation<>();
        
        // Create patch set
        PatchSet<MockGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            0,
            List.of(op),
            Optional.empty()
        );
        
        // Register and freeze
        registry.register(patchSet);
        registry.freeze();
        
        // Compile plan once
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Use plan multiple times (simulating datapack reload)
        List<PatchSet<MockGameplayData>> patches1 = plan.getPatchesFor(target);
        List<PatchSet<MockGameplayData>> patches2 = plan.getPatchesFor(target);
        
        // Verify same patches are returned
        assertEquals(patches1.size(), patches2.size());
        assertSame(patches1.get(0), patches2.get(0));
    }
}
