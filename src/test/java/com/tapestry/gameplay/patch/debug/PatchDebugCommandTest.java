package com.tapestry.gameplay.patch.debug;

import com.tapestry.gameplay.patch.*;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PatchDebugCommand.
 * 
 * Tests cover:
 * - Formatting output for targets with patches
 * - Handling missing targets
 * - Displaying mod IDs, priorities, and operations
 * - Displaying operation debug IDs
 * - Displaying statistics
 * - Null parameter handling
 */
class PatchDebugCommandTest {
    
    private PatchRegistry registry;
    private ModLoadOrder modLoadOrder;
    
    // Mock operation with debug ID
    private static class MockOperationWithDebugId implements PatchOperation<String> {
        private final String debugId;
        
        MockOperationWithDebugId(String debugId) {
            this.debugId = debugId;
        }
        
        @Override
        public void apply(String target) {
            // No-op for testing
        }
        
        @Override
        public Optional<String> getDebugId() {
            return Optional.of(debugId);
        }
    }
    
    // Mock operation without debug ID
    private static class MockOperationWithoutDebugId implements PatchOperation<String> {
        @Override
        public void apply(String target) {
            // No-op for testing
        }
    }
    
    // Simple mod load order implementation for testing
    private static class TestModLoadOrder implements ModLoadOrder {
        @Override
        public int compare(Identifier modA, Identifier modB) {
            return modA.toString().compareTo(modB.toString());
        }
    }
    
    @BeforeEach
    void setUp() {
        registry = new PatchRegistry();
        modLoadOrder = new TestModLoadOrder();
    }
    
    @Test
    void testExecuteWithNoPatches() {
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        PatchDebugCommand command = new PatchDebugCommand(plan);
        
        String output = command.execute(Identifier.of("minecraft:nonexistent"));
        
        assertTrue(output.contains("No patches found"));
        assertTrue(output.contains("minecraft:nonexistent"));
    }
    
    @Test
    void testExecuteWithSinglePatchSet() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(
            new MockOperationWithDebugId("operation1"),
            new MockOperationWithDebugId("operation2")
        );
        
        PatchSet<String> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(patchSet);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        PatchDebugCommand command = new PatchDebugCommand(plan);
        
        String output = command.execute(Identifier.of("minecraft:test_target"));
        
        // Verify header
        assertTrue(output.contains("Target: minecraft:test_target"));
        assertTrue(output.contains("String"));
        
        // Verify patch set info
        assertTrue(output.contains("Applied 1 patch set"));
        assertTrue(output.contains("testmod:test"));
        assertTrue(output.contains("priority: 0"));
        
        // Verify operations
        assertTrue(output.contains("MockOperationWithDebugId: operation1"));
        assertTrue(output.contains("MockOperationWithDebugId: operation2"));
        
        // Verify statistics
        assertTrue(output.contains("Statistics:"));
        assertTrue(output.contains("Total operations: 2"));
        assertTrue(output.contains("Total patch sets: 1"));
    }
    
    @Test
    void testExecuteWithMultiplePatchSets() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations1 = List.of(
            new MockOperationWithDebugId("op1")
        );
        
        List<PatchOperation<String>> operations2 = List.of(
            new MockOperationWithDebugId("op2"),
            new MockOperationWithDebugId("op3")
        );
        
        PatchSet<String> patchSet1 = new PatchSet<>(
            Identifier.of("mod1:test"),
            target,
            PatchPriority.EARLY,
            operations1,
            Optional.empty()
        );
        
        PatchSet<String> patchSet2 = new PatchSet<>(
            Identifier.of("mod2:test"),
            target,
            PatchPriority.LATE,
            operations2,
            Optional.empty()
        );
        
        registry.register(patchSet1);
        registry.register(patchSet2);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        PatchDebugCommand command = new PatchDebugCommand(plan);
        
        String output = command.execute(Identifier.of("minecraft:test_target"));
        
        // Verify header
        assertTrue(output.contains("Applied 2 patch sets"));
        
        // Verify patch sets are listed in order
        assertTrue(output.contains("1. mod1:test (priority: -500)"));
        assertTrue(output.contains("2. mod2:test (priority: 500)"));
        
        // Verify operations
        assertTrue(output.contains("op1"));
        assertTrue(output.contains("op2"));
        assertTrue(output.contains("op3"));
        
        // Verify statistics
        assertTrue(output.contains("Total operations: 3"));
        assertTrue(output.contains("Total patch sets: 2"));
    }
    
    @Test
    void testExecuteWithOperationsWithoutDebugId() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(
            new MockOperationWithoutDebugId()
        );
        
        PatchSet<String> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(patchSet);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        PatchDebugCommand command = new PatchDebugCommand(plan);
        
        String output = command.execute(Identifier.of("minecraft:test_target"));
        
        // Should show operation type without debug ID
        assertTrue(output.contains("MockOperationWithoutDebugId"));
        // Should not have a colon after the operation type (no debug info)
        assertFalse(output.contains("MockOperationWithoutDebugId:"));
    }
    
    @Test
    void testExecuteWithMixedDebugIds() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(
            new MockOperationWithDebugId("with_id"),
            new MockOperationWithoutDebugId()
        );
        
        PatchSet<String> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(patchSet);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        PatchDebugCommand command = new PatchDebugCommand(plan);
        
        String output = command.execute(Identifier.of("minecraft:test_target"));
        
        // Should show operation with debug ID
        assertTrue(output.contains("MockOperationWithDebugId: with_id"));
        
        // Should show operation without debug ID (no colon)
        assertTrue(output.contains("MockOperationWithoutDebugId"));
        String[] lines = output.split("\n");
        boolean foundWithoutDebugId = false;
        for (String line : lines) {
            if (line.contains("MockOperationWithoutDebugId") && !line.contains(":")) {
                foundWithoutDebugId = true;
                break;
            }
        }
        assertTrue(foundWithoutDebugId, "Should have operation without debug ID");
    }
    
    @Test
    void testExecuteWithDifferentPriorities() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(
            new MockOperationWithDebugId("op")
        );
        
        PatchSet<String> veryEarly = new PatchSet<>(
            Identifier.of("mod1:test"),
            target,
            PatchPriority.VERY_EARLY,
            operations,
            Optional.empty()
        );
        
        PatchSet<String> normal = new PatchSet<>(
            Identifier.of("mod2:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        PatchSet<String> veryLate = new PatchSet<>(
            Identifier.of("mod3:test"),
            target,
            PatchPriority.VERY_LATE,
            operations,
            Optional.empty()
        );
        
        registry.register(veryEarly);
        registry.register(normal);
        registry.register(veryLate);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        PatchDebugCommand command = new PatchDebugCommand(plan);
        
        String output = command.execute(Identifier.of("minecraft:test_target"));
        
        // Verify priorities are displayed correctly
        assertTrue(output.contains("priority: -1000"));
        assertTrue(output.contains("priority: 0"));
        assertTrue(output.contains("priority: 1000"));
        
        // Verify order (VERY_EARLY should be first)
        int veryEarlyIndex = output.indexOf("mod1:test");
        int normalIndex = output.indexOf("mod2:test");
        int veryLateIndex = output.indexOf("mod3:test");
        
        assertTrue(veryEarlyIndex < normalIndex);
        assertTrue(normalIndex < veryLateIndex);
    }
    
    @Test
    void testConstructorWithNullPlanThrows() {
        assertThrows(NullPointerException.class, () -> {
            new PatchDebugCommand(null);
        });
    }
    
    @Test
    void testExecuteWithNullTargetIdThrows() {
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        PatchDebugCommand command = new PatchDebugCommand(plan);
        
        assertThrows(NullPointerException.class, () -> {
            command.execute(null);
        });
    }
    
    @Test
    void testOutputFormatting() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(
            new MockOperationWithDebugId("test_operation")
        );
        
        PatchSet<String> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(patchSet);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        PatchDebugCommand command = new PatchDebugCommand(plan);
        
        String output = command.execute(Identifier.of("minecraft:test_target"));
        
        // Verify output structure
        String[] lines = output.split("\n");
        
        // Should have multiple lines
        assertTrue(lines.length > 5);
        
        // First line should be target info
        assertTrue(lines[0].startsWith("Target:"));
        
        // Should have blank lines for readability
        boolean hasBlankLines = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                hasBlankLines = true;
                break;
            }
        }
        assertTrue(hasBlankLines);
        
        // Operations should be indented
        boolean hasIndentedOperations = false;
        for (String line : lines) {
            if (line.startsWith("   - ")) {
                hasIndentedOperations = true;
                break;
            }
        }
        assertTrue(hasIndentedOperations);
    }
    
    @Test
    void testSingularVsPluralPatchSets() {
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:test_target"),
            String.class
        );
        
        List<PatchOperation<String>> operations = List.of(
            new MockOperationWithDebugId("op")
        );
        
        // Test with single patch set
        PatchSet<String> singlePatch = new PatchSet<>(
            Identifier.of("mod1:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(singlePatch);
        
        PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
        PatchDebugCommand command = new PatchDebugCommand(plan);
        
        String output = command.execute(Identifier.of("minecraft:test_target"));
        
        // Should use singular form
        assertTrue(output.contains("Applied 1 patch set:"));
        
        // Test with multiple patch sets
        registry = new PatchRegistry();
        
        PatchSet<String> patch1 = new PatchSet<>(
            Identifier.of("mod1:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        PatchSet<String> patch2 = new PatchSet<>(
            Identifier.of("mod2:test"),
            target,
            PatchPriority.NORMAL,
            operations,
            Optional.empty()
        );
        
        registry.register(patch1);
        registry.register(patch2);
        
        plan = PatchPlan.compile(registry, modLoadOrder);
        command = new PatchDebugCommand(plan);
        
        output = command.execute(Identifier.of("minecraft:test_target"));
        
        // Should use plural form
        assertTrue(output.contains("Applied 2 patch sets:"));
    }
}
