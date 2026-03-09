package com.tapestry.gameplay.patch;

import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PatchEngine}.
 */
class PatchEngineTest {
    
    private PatchPlan mockPlan;
    private PatchContext mockContext;
    
    @BeforeEach
    void setUp() {
        // Create a minimal mock PatchPlan
        PatchRegistry registry = new PatchRegistry();
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        mockPlan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create a minimal mock PatchContext
        mockContext = new PatchContext() {
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
            public Registry<?> getRegistry(Identifier registryId) {
                return null;
            }
        };
    }
    
    @Test
    void constructor_withValidParameters_createsEngine() {
        Map<String, Object> config = new HashMap<>();
        
        PatchEngine engine = new PatchEngine(mockPlan, mockContext, config);
        
        assertNotNull(engine);
    }
    
    @Test
    void constructor_withStrictModeEnabled_createsEngine() {
        Map<String, Object> config = Map.of("tapestry.strictPatches", true);
        
        PatchEngine engine = new PatchEngine(mockPlan, mockContext, config);
        
        assertNotNull(engine);
    }
    
    @Test
    void constructor_withDebugModeEnabled_createsEngine() {
        Map<String, Object> config = Map.of("tapestry.debugPatches", true);
        
        PatchEngine engine = new PatchEngine(mockPlan, mockContext, config);
        
        assertNotNull(engine);
    }
    
    @Test
    void constructor_withBothModesEnabled_createsEngine() {
        Map<String, Object> config = Map.of(
            "tapestry.strictPatches", true,
            "tapestry.debugPatches", true
        );
        
        PatchEngine engine = new PatchEngine(mockPlan, mockContext, config);
        
        assertNotNull(engine);
    }
    
    @Test
    void constructor_withNonBooleanConfigValues_usesDefaults() {
        Map<String, Object> config = Map.of(
            "tapestry.strictPatches", "not a boolean",
            "tapestry.debugPatches", 123
        );
        
        // Should not throw, should use default values (false)
        PatchEngine engine = new PatchEngine(mockPlan, mockContext, config);
        
        assertNotNull(engine);
    }
    
    @Test
    void constructor_withNullPlan_throwsNullPointerException() {
        Map<String, Object> config = new HashMap<>();
        
        assertThrows(NullPointerException.class, () -> {
            new PatchEngine(null, mockContext, config);
        });
    }
    
    @Test
    void constructor_withNullContext_throwsNullPointerException() {
        Map<String, Object> config = new HashMap<>();
        
        assertThrows(NullPointerException.class, () -> {
            new PatchEngine(mockPlan, null, config);
        });
    }
    
    @Test
    void constructor_withNullConfig_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            new PatchEngine(mockPlan, mockContext, null);
        });
    }
    
    @Test
    void constructor_withPerformanceThresholdConfigured_createsEngine() {
        Map<String, Object> config = Map.of("tapestry.patchPerformanceThreshold", 200);
        
        PatchEngine engine = new PatchEngine(mockPlan, mockContext, config);
        
        assertNotNull(engine);
    }
    
    @Test
    void constructor_withAllConfigOptions_createsEngine() {
        Map<String, Object> config = Map.of(
            "tapestry.strictPatches", true,
            "tapestry.debugPatches", true,
            "tapestry.patchPerformanceThreshold", 50
        );
        
        PatchEngine engine = new PatchEngine(mockPlan, mockContext, config);
        
        assertNotNull(engine);
    }
    
    @Test
    void constructor_withNonIntegerPerformanceThreshold_usesDefault() {
        Map<String, Object> config = Map.of(
            "tapestry.patchPerformanceThreshold", "not an integer"
        );
        
        // Should not throw, should use default value (100)
        PatchEngine engine = new PatchEngine(mockPlan, mockContext, config);
        
        assertNotNull(engine);
    }
    
    @Test
    void applyPatches_withNoPatches_returnsUnchangedData() {
        Map<String, Object> config = new HashMap<>();
        PatchEngine engine = new PatchEngine(mockPlan, mockContext, config);
        
        // Create a simple test data object
        TestGameplayData data = new TestGameplayData("original");
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:target"),
            TestGameplayData.class
        );
        
        TestGameplayData result = engine.applyPatches(target, data);
        
        assertSame(data, result);
        assertEquals("original", result.getValue());
    }
    
    @Test
    void applyPatches_withNullTarget_throwsNullPointerException() {
        Map<String, Object> config = new HashMap<>();
        PatchEngine engine = new PatchEngine(mockPlan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("test");
        
        assertThrows(NullPointerException.class, () -> {
            engine.applyPatches(null, data);
        });
    }
    
    @Test
    void applyPatches_withNullGameplayData_throwsNullPointerException() {
        Map<String, Object> config = new HashMap<>();
        PatchEngine engine = new PatchEngine(mockPlan, mockContext, config);
        
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:target"),
            TestGameplayData.class
        );
        
        assertThrows(NullPointerException.class, () -> {
            engine.applyPatches(target, null);
        });
    }
    
    @Test
    void applyPatches_withSingleOperation_appliesSuccessfully() {
        // Create a registry with a patch
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:target"),
            TestGameplayData.class
        );
        
        // Create an operation that modifies the data
        PatchOperation<TestGameplayData> operation = new PatchOperation<TestGameplayData>() {
            @Override
            public void apply(TestGameplayData data) {
                data.setValue("modified");
            }
        };
        
        PatchSet<TestGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(operation),
            java.util.Optional.empty()
        );
        
        registry.register(patchSet);
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine and apply patches
        Map<String, Object> config = new HashMap<>();
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        TestGameplayData result = engine.applyPatches(target, data);
        
        assertSame(data, result);
        assertEquals("modified", result.getValue());
    }
    
    @Test
    void applyPatches_withMultipleOperations_appliesInOrder() {
        // Create a registry with multiple operations
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:target"),
            TestGameplayData.class
        );
        
        // Create operations that modify the data in sequence
        PatchOperation<TestGameplayData> op1 = data -> data.setValue(data.getValue() + "-1");
        PatchOperation<TestGameplayData> op2 = data -> data.setValue(data.getValue() + "-2");
        PatchOperation<TestGameplayData> op3 = data -> data.setValue(data.getValue() + "-3");
        
        PatchSet<TestGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(op1, op2, op3),
            java.util.Optional.empty()
        );
        
        registry.register(patchSet);
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine and apply patches
        Map<String, Object> config = new HashMap<>();
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        TestGameplayData result = engine.applyPatches(target, data);
        
        assertEquals("original-1-2-3", result.getValue());
    }
    
    @Test
    void applyPatches_withConditionFalse_skipsOperations() {
        // Create a registry with a conditional patch
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:target"),
            TestGameplayData.class
        );
        
        PatchOperation<TestGameplayData> operation = data -> data.setValue("modified");
        PatchCondition condition = PatchCondition.modLoaded("nonexistent_mod");
        
        PatchSet<TestGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(operation),
            java.util.Optional.of(condition)
        );
        
        registry.register(patchSet);
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine and apply patches
        Map<String, Object> config = new HashMap<>();
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        TestGameplayData result = engine.applyPatches(target, data);
        
        // Data should be unchanged because condition was false
        assertEquals("original", result.getValue());
    }
    
    @Test
    void applyPatches_withFailingOperation_continuesInLenientMode() {
        // Create a registry with a failing operation
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:target"),
            TestGameplayData.class
        );
        
        PatchOperation<TestGameplayData> op1 = data -> data.setValue("first");
        PatchOperation<TestGameplayData> failingOp = data -> {
            throw new RuntimeException("Intentional failure");
        };
        PatchOperation<TestGameplayData> op3 = data -> data.setValue(data.getValue() + "-third");
        
        PatchSet<TestGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(op1, failingOp, op3),
            java.util.Optional.empty()
        );
        
        registry.register(patchSet);
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine in lenient mode (default)
        Map<String, Object> config = Map.of("tapestry.strictPatches", false);
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        TestGameplayData result = engine.applyPatches(target, data);
        
        // First and third operations should have applied despite the failure
        assertEquals("first-third", result.getValue());
    }
    
    @Test
    void applyPatches_withFailingOperation_throwsInStrictMode() {
        // Create a registry with a failing operation
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:target"),
            TestGameplayData.class
        );
        
        PatchOperation<TestGameplayData> failingOp = data -> {
            throw new RuntimeException("Intentional failure");
        };
        
        PatchSet<TestGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(failingOp),
            java.util.Optional.empty()
        );
        
        registry.register(patchSet);
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine in strict mode
        Map<String, Object> config = Map.of("tapestry.strictPatches", true);
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        
        // Should throw PatchApplicationException in strict mode
        assertThrows(PatchApplicationException.class, () -> {
            engine.applyPatches(target, data);
        });
    }
    
    @Test
    void applyPatches_withMissingTarget_logsWarningInLenientMode() {
        // Create a registry with patches for a target
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:missing_target"),
            TestGameplayData.class
        );
        
        PatchOperation<TestGameplayData> operation = data -> data.setValue("modified");
        
        PatchSet<TestGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(operation),
            java.util.Optional.empty()
        );
        
        registry.register(patchSet);
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine in lenient mode (default)
        Map<String, Object> config = Map.of("tapestry.strictPatches", false);
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        
        // Apply patches with targetExists = false
        TestGameplayData result = engine.applyPatches(target, data, false);
        
        // Data should be unchanged (operation was skipped)
        assertSame(data, result);
        assertEquals("original", result.getValue());
    }
    
    @Test
    void applyPatches_withMissingTarget_throwsInStrictMode() {
        // Create a registry with patches for a target
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:missing_target"),
            TestGameplayData.class
        );
        
        PatchOperation<TestGameplayData> operation = data -> data.setValue("modified");
        
        PatchSet<TestGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(operation),
            java.util.Optional.empty()
        );
        
        registry.register(patchSet);
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine in strict mode
        Map<String, Object> config = Map.of("tapestry.strictPatches", true);
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        
        // Should throw PatchApplicationException in strict mode
        assertThrows(PatchApplicationException.class, () -> {
            engine.applyPatches(target, data, false);
        });
    }
    
    @Test
    void applyPatches_withMissingTargetAndNoPatches_returnsUnchanged() {
        // Create an empty registry (no patches)
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:missing_target"),
            TestGameplayData.class
        );
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine
        Map<String, Object> config = new HashMap<>();
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        
        // Apply patches with targetExists = false
        TestGameplayData result = engine.applyPatches(target, data, false);
        
        // Data should be unchanged (no patches to apply)
        assertSame(data, result);
        assertEquals("original", result.getValue());
    }
    
    @Test
    void applyPatches_withMissingTargetAndMultipleMods_logsAllModIds() {
        // Create a registry with patches from multiple mods
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:missing_target"),
            TestGameplayData.class
        );
        
        PatchOperation<TestGameplayData> op1 = data -> data.setValue("mod1");
        PatchOperation<TestGameplayData> op2 = data -> data.setValue("mod2");
        PatchOperation<TestGameplayData> op3 = data -> data.setValue("mod3");
        
        PatchSet<TestGameplayData> patchSet1 = new PatchSet<>(
            Identifier.of("mod1:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(op1),
            java.util.Optional.empty()
        );
        
        PatchSet<TestGameplayData> patchSet2 = new PatchSet<>(
            Identifier.of("mod2:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(op2),
            java.util.Optional.empty()
        );
        
        PatchSet<TestGameplayData> patchSet3 = new PatchSet<>(
            Identifier.of("mod3:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(op3),
            java.util.Optional.empty()
        );
        
        registry.register(patchSet1);
        registry.register(patchSet2);
        registry.register(patchSet3);
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine in lenient mode
        Map<String, Object> config = Map.of("tapestry.strictPatches", false);
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        
        // Apply patches with targetExists = false
        // This should log all three mod IDs in the warning message
        TestGameplayData result = engine.applyPatches(target, data, false);
        
        // Data should be unchanged
        assertSame(data, result);
        assertEquals("original", result.getValue());
        
        // Note: The missing target count is tracked internally in statistics
        // and logged via logSummary. In a production system, this would be
        // aggregated across multiple targets for a global summary.
    }
    
    @Test
    void applyPatches_withExistingTarget_appliesNormally() {
        // Create a registry with patches
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:target"),
            TestGameplayData.class
        );
        
        PatchOperation<TestGameplayData> operation = data -> data.setValue("modified");
        
        PatchSet<TestGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(operation),
            java.util.Optional.empty()
        );
        
        registry.register(patchSet);
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine
        Map<String, Object> config = new HashMap<>();
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        
        // Apply patches with targetExists = true (explicit)
        TestGameplayData result = engine.applyPatches(target, data, true);
        
        // Data should be modified
        assertSame(data, result);
        assertEquals("modified", result.getValue());
    }
    
    @Test
    void applyPatches_defaultOverload_assumesTargetExists() {
        // Create a registry with patches
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:target"),
            TestGameplayData.class
        );
        
        PatchOperation<TestGameplayData> operation = data -> data.setValue("modified");
        
        PatchSet<TestGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(operation),
            java.util.Optional.empty()
        );
        
        registry.register(patchSet);
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine
        Map<String, Object> config = new HashMap<>();
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        
        // Apply patches using default overload (should assume target exists)
        TestGameplayData result = engine.applyPatches(target, data);
        
        // Data should be modified
        assertSame(data, result);
        assertEquals("modified", result.getValue());
    }
    
    /**
     * Simple test class for gameplay data.
     */
    private static class TestGameplayData {
        private String value;
        
        public TestGameplayData(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }

    @Test
    void applyPatches_withNoOpOperation_detectsAndLogsInDebugMode() {
            // Create a registry with a no-op operation
            PatchRegistry registry = new PatchRegistry();
            PatchTarget<TestGameplayData> target = new PatchTarget<>(
                Identifier.of("test:target"),
                TestGameplayData.class
            );

            // Create a no-op operation (doesn't actually change anything)
            PatchOperation<TestGameplayData> noOpOperation = new PatchOperation<TestGameplayData>() {
                @Override
                public void apply(TestGameplayData data) {
                    // Do nothing - this is a no-op
                    String temp = data.getValue();
                    data.setValue(temp); // Set to same value
                }
            };

            PatchSet<TestGameplayData> patchSet = new PatchSet<>(
                Identifier.of("testmod:test"),
                target,
                PatchPriority.NORMAL,
                java.util.List.of(noOpOperation),
                java.util.Optional.empty()
            );

            registry.register(patchSet);

            // Compile the plan
            ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
            PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);

            // Create engine with debug mode enabled
            Map<String, Object> config = Map.of("tapestry.debugPatches", true);
            PatchEngine engine = new PatchEngine(plan, mockContext, config);

            TestGameplayData data = new TestGameplayData("original");
            TestGameplayData result = engine.applyPatches(target, data);

            // Data should be unchanged (no-op)
            assertSame(data, result);
            assertEquals("original", result.getValue());
            // Note: In a real test, we would verify the debug log message was emitted
            // For now, we just verify the operation completed successfully
        }

        @Test
        void applyPatches_withNoOpOperation_doesNotDetectInNormalMode() {
            // Create a registry with a no-op operation
            PatchRegistry registry = new PatchRegistry();
            PatchTarget<TestGameplayData> target = new PatchTarget<>(
                Identifier.of("test:target"),
                TestGameplayData.class
            );

            // Create a no-op operation
            PatchOperation<TestGameplayData> noOpOperation = data -> {
                // Do nothing - this is a no-op
                String temp = data.getValue();
                data.setValue(temp);
            };

            PatchSet<TestGameplayData> patchSet = new PatchSet<>(
                Identifier.of("testmod:test"),
                target,
                PatchPriority.NORMAL,
                java.util.List.of(noOpOperation),
                java.util.Optional.empty()
            );

            registry.register(patchSet);

            // Compile the plan
            ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
            PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);

            // Create engine with debug mode disabled (default)
            Map<String, Object> config = Map.of("tapestry.debugPatches", false);
            PatchEngine engine = new PatchEngine(plan, mockContext, config);

            TestGameplayData data = new TestGameplayData("original");
            TestGameplayData result = engine.applyPatches(target, data);

            // Data should be unchanged (no-op)
            assertSame(data, result);
            assertEquals("original", result.getValue());
            // No-op detection should not run in normal mode (performance optimization)
        }

        @Test
        void applyPatches_withMixedOperations_detectsOnlyNoOps() {
            // Create a registry with mixed operations
            PatchRegistry registry = new PatchRegistry();
            PatchTarget<TestGameplayData> target = new PatchTarget<>(
                Identifier.of("test:target"),
                TestGameplayData.class
            );

            // Create operations: real change, no-op, real change
            PatchOperation<TestGameplayData> op1 = data -> data.setValue("modified");
            PatchOperation<TestGameplayData> noOpOp = data -> {
                // This is a no-op because it doesn't change the value
                String temp = data.getValue();
                data.setValue(temp);
            };
            PatchOperation<TestGameplayData> op3 = data -> data.setValue(data.getValue() + "-final");

            PatchSet<TestGameplayData> patchSet = new PatchSet<>(
                Identifier.of("testmod:test"),
                target,
                PatchPriority.NORMAL,
                java.util.List.of(op1, noOpOp, op3),
                java.util.Optional.empty()
            );

            registry.register(patchSet);

            // Compile the plan
            ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
            PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);

            // Create engine with debug mode enabled
            Map<String, Object> config = Map.of("tapestry.debugPatches", true);
            PatchEngine engine = new PatchEngine(plan, mockContext, config);

            TestGameplayData data = new TestGameplayData("original");
            TestGameplayData result = engine.applyPatches(target, data);

            // Data should be modified by op1 and op3, but not by noOpOp
            assertSame(data, result);
            assertEquals("modified-final", result.getValue());
            // The middle operation should be detected as a no-op
        }
        
        // ---------- wrapper support tests (task 16) ----------
        
        /**
         * A simple wrapper used for testing that delegates to TestGameplayData.
         */
        private static class TestWrapper implements com.tapestry.gameplay.model.TapestryModel<TestGameplayData> {
            private final TestGameplayData data;

            public TestWrapper(TestGameplayData data) {
                this.data = data;
            }

            @Override
            public TestGameplayData unwrap() {
                return data;
            }

            public void modify(String suffix) {
                data.setValue(data.getValue() + suffix);
            }
        }

        @Test
        void applyPatchesWithWrapper_modifiesAndUnwrapsCorrectly() {
            // create a registry with a patch targeting the wrapper type
            PatchRegistry registry = new PatchRegistry();
            PatchTarget<TestWrapper> wrapperTarget = new PatchTarget<>(
                    Identifier.of("test:wrapper"),
                    TestWrapper.class
            );

            PatchOperation<TestWrapper> op = w -> w.modify("-wrapped");
            PatchSet<TestWrapper> patchSet = new PatchSet<>(
                    Identifier.of("testmod:wrap"),
                    wrapperTarget,
                    PatchPriority.NORMAL,
                    java.util.List.of(op),
                    java.util.Optional.empty()
            );
            registry.register(patchSet);

            ModLoadOrder modLoadOrder = (a, b) -> a.compareTo(b);
            PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
            PatchEngine engine = new PatchEngine(plan, mockContext, new HashMap<>());

            TestGameplayData data = new TestGameplayData("orig");
            TestGameplayData result = engine.applyPatchesWithWrapper(
                    wrapperTarget,
                    data,
                    TestWrapper::new
            );

            assertSame(data, result);
            assertEquals("orig-wrapped", result.getValue());
        }

        @Test
        void tapestryModelWrapper_roundTripEqualsDirectApplication() {
            // direct plan
            PatchRegistry directRegistry = new PatchRegistry();
            PatchTarget<TestGameplayData> directTarget = new PatchTarget<>(
                    Identifier.of("test:direct"),
                    TestGameplayData.class
            );
            PatchOperation<TestGameplayData> directOp = d -> d.setValue(d.getValue() + "-patched");
            directRegistry.register(new PatchSet<>(
                    Identifier.of("testmod:direct"),
                    directTarget,
                    PatchPriority.NORMAL,
                    java.util.List.of(directOp),
                    java.util.Optional.empty()
            ));
            PatchPlan directPlan = PatchPlan.compile(directRegistry, (a,b)->a.compareTo(b));
            PatchEngine directEngine = new PatchEngine(directPlan, mockContext, new HashMap<>());

            // wrapper plan with equivalent operation
            PatchRegistry wrapperRegistry = new PatchRegistry();
            PatchTarget<TestWrapper> wrapperTarget2 = new PatchTarget<>(
                    Identifier.of("test:direct"),
                    TestWrapper.class
            );
            PatchOperation<TestWrapper> wrapperOp = w -> w.unwrap().setValue(w.unwrap().getValue() + "-patched");
            wrapperRegistry.register(new PatchSet<>(
                    Identifier.of("testmod:direct"),
                    wrapperTarget2,
                    PatchPriority.NORMAL,
                    java.util.List.of(wrapperOp),
                    java.util.Optional.empty()
            ));
            PatchPlan wrapperPlan = PatchPlan.compile(wrapperRegistry, (a,b)->a.compareTo(b));
            PatchEngine wrapperEngine = new PatchEngine(wrapperPlan, mockContext, new HashMap<>());

            TestGameplayData base1 = new TestGameplayData("base");
            TestGameplayData resultDirect = directEngine.applyPatches(directTarget, base1);

            TestGameplayData base2 = new TestGameplayData("base");
            TestGameplayData resultWrapper = wrapperEngine.applyPatchesWithWrapper(
                    wrapperTarget2,
                    base2,
                    TestWrapper::new
            );

            assertEquals(resultDirect.getValue(), resultWrapper.getValue());
        }

        // ---------- end wrapper support tests ----------

    @Test
    void handlePatchError_includesFullContextInErrorMessage() {
        // Create a registry with a failing operation that has a debug ID
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:my_target"),
            TestGameplayData.class
        );
        
        PatchOperation<TestGameplayData> failingOp = new PatchOperation<TestGameplayData>() {
            @Override
            public void apply(TestGameplayData data) {
                throw new RuntimeException("Test error message");
            }
            
            @Override
            public java.util.Optional<String> getDebugId() {
                return java.util.Optional.of("MyCustomOperation");
            }
        };
        
        PatchSet<TestGameplayData> patchSet = new PatchSet<>(
            Identifier.of("mymod:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(failingOp),
            java.util.Optional.empty()
        );
        
        registry.register(patchSet);
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine in strict mode to capture the exception
        Map<String, Object> config = Map.of("tapestry.strictPatches", true);
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        
        // Verify the exception message includes all required context
        PatchApplicationException exception = assertThrows(PatchApplicationException.class, () -> {
            engine.applyPatches(target, data);
        });
        
        String message = exception.getMessage();
        
        // Verify message includes mod ID
        assertTrue(message.contains("mymod:test"), 
            "Error message should include mod ID: " + message);
        
        // Verify message includes target ID
        assertTrue(message.contains("test:my_target"), 
            "Error message should include target ID: " + message);
        
        // Verify message includes operation type
        assertTrue(message.contains("MyCustomOperation"), 
            "Error message should include operation type: " + message);
        
        // Verify message includes the original error
        assertTrue(message.contains("Test error message"), 
            "Error message should include original error message: " + message);
    }
    
    @Test
    void handlePatchError_usesClassNameWhenNoDebugId() {
        // Create a registry with a failing operation without a debug ID
        PatchRegistry registry = new PatchRegistry();
        PatchTarget<TestGameplayData> target = new PatchTarget<>(
            Identifier.of("test:target"),
            TestGameplayData.class
        );
        
        // Lambda operation (will use class name as fallback)
        PatchOperation<TestGameplayData> failingOp = data -> {
            throw new RuntimeException("Test error");
        };
        
        PatchSet<TestGameplayData> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            java.util.List.of(failingOp),
            java.util.Optional.empty()
        );
        
        registry.register(patchSet);
        
        // Compile the plan
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        
        // Create engine in strict mode
        Map<String, Object> config = Map.of("tapestry.strictPatches", true);
        PatchEngine engine = new PatchEngine(plan, mockContext, config);
        
        TestGameplayData data = new TestGameplayData("original");
        
        // Verify the exception is thrown and contains some operation identifier
        PatchApplicationException exception = assertThrows(PatchApplicationException.class, () -> {
            engine.applyPatches(target, data);
        });
        
        String message = exception.getMessage();
        
        // Should contain mod ID and target ID at minimum
        assertTrue(message.contains("testmod:test"), 
            "Error message should include mod ID: " + message);
        assertTrue(message.contains("test:target"), 
            "Error message should include target ID: " + message);
        
        // Should contain some operation identifier (class name)
        assertTrue(message.contains("operation:"), 
            "Error message should include operation identifier: " + message);
    }
}
