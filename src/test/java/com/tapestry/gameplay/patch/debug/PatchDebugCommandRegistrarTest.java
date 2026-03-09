package com.tapestry.gameplay.patch.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tapestry.gameplay.patch.*;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PatchDebugCommandRegistrar.
 */
class PatchDebugCommandRegistrarTest {
    
    private CommandDispatcher<ServerCommandSource> dispatcher;
    private CommandRegistryAccess registryAccess;
    private PatchRegistry registry;
    private PatchPlan patchPlan;
    private ServerCommandSource commandSource;
    
    // No static bootstrap; tests operate with minimal Minecraft classes and avoid
    // triggering registry initialization. Using Identifier.of directly works without
    // calling Bootstrap in the unit-test environment.
    
    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher<>();
        registryAccess = mock(CommandRegistryAccess.class);
        commandSource = mock(ServerCommandSource.class);
        
        // Create a test registry and plan
        registry = new PatchRegistry();
        registry.freeze();
        
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        patchPlan = PatchPlan.compile(registry, modLoadOrder);
    }
    
    @Test
    void testRegisterCommand() {
        // Register the command
        PatchDebugCommandRegistrar.register(
            dispatcher,
            registryAccess,
            CommandManager.RegistrationEnvironment.ALL,
            patchPlan
        );
        
        // Verify the command was registered
        var nodes = dispatcher.getRoot().getChildren();
        assertTrue(nodes.stream().anyMatch(node -> node.getName().equals("tapestry")),
            "Command 'tapestry' should be registered");
    }
    
    @Test
    void testRegisterWithNullDispatcherThrows() {
        assertThrows(NullPointerException.class, () -> {
            PatchDebugCommandRegistrar.register(
                null,
                registryAccess,
                CommandManager.RegistrationEnvironment.ALL,
                patchPlan
            );
        });
    }
    
    @Test
    void testRegisterWithNullPatchPlanThrows() {
        assertThrows(NullPointerException.class, () -> {
            PatchDebugCommandRegistrar.register(
                dispatcher,
                registryAccess,
                CommandManager.RegistrationEnvironment.ALL,
                null
            );
        });
    }
    
    @Test
    void testExecuteCommandWithValidTarget() throws CommandSyntaxException {
        // Create a patch set for testing
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:villager/farmer"),
            String.class
        );
        
        PatchOperation<String> operation = new PatchOperation<String>() {
            @Override
            public void apply(String data) {
                // No-op for testing
            }
            
            @Override
            public Optional<String> getDebugId() {
                return Optional.of("TestOperation");
            }
        };
        
        PatchSet<String> patchSet = new PatchSet<>(
            Identifier.of("testmod:test"),
            target,
            0,
            List.of(operation),
            Optional.empty()
        );
        
        // Create a new registry with the patch set
        PatchRegistry testRegistry = new PatchRegistry();
        testRegistry.register(patchSet);
        testRegistry.freeze();
        
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan testPlan = PatchPlan.compile(testRegistry, modLoadOrder);
        
        // Register the command
        PatchDebugCommandRegistrar.register(
            dispatcher,
            registryAccess,
            CommandManager.RegistrationEnvironment.ALL,
            testPlan
        );
        
        // Execute the command
        int result = dispatcher.execute("tapestry patches minecraft:villager/farmer", commandSource);
        
        // Verify the command executed successfully
        assertEquals(1, result, "Command should return success (1)");
        
        // Verify feedback was sent
        verify(commandSource, atLeastOnce()).sendFeedback(any(), anyBoolean());
    }
    
    @Test
    void testExecuteCommandWithInvalidTarget() throws CommandSyntaxException {
        // Register the command
        PatchDebugCommandRegistrar.register(
            dispatcher,
            registryAccess,
            CommandManager.RegistrationEnvironment.ALL,
            patchPlan
        );
        
        // Execute the command with a non-existent target
        int result = dispatcher.execute("tapestry patches minecraft:nonexistent", commandSource);
        
        // Verify the command executed successfully (returns 1 even for non-existent targets)
        assertEquals(1, result, "Command should return success (1)");
        
        // Verify feedback was sent (should contain "No patches found")
        ArgumentCaptor<Supplier<Text>> feedbackCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(commandSource, atLeastOnce()).sendFeedback(feedbackCaptor.capture(), anyBoolean());
        
        // Check that at least one feedback message contains "No patches found"
        boolean foundNoPatches = feedbackCaptor.getAllValues().stream()
            .anyMatch(supplier -> {
                Text text = supplier.get();
                return text.getString().contains("No patches found");
            });
        
        assertTrue(foundNoPatches, "Feedback should contain 'No patches found' message");
    }
    
    @Test
    void testCommandRequiresTargetIdArgument() {
        // Register the command
        PatchDebugCommandRegistrar.register(
            dispatcher,
            registryAccess,
            CommandManager.RegistrationEnvironment.ALL,
            patchPlan
        );
        
        // Try to execute the command without the target_id argument
        assertThrows(CommandSyntaxException.class, () -> {
            dispatcher.execute("tapestry patches", commandSource);
        });
    }
    
    @Test
    void testCommandOutputFormat() throws CommandSyntaxException {
        // Create a patch set with multiple operations
        PatchTarget<String> target = new PatchTarget<>(
            Identifier.of("minecraft:villager/farmer"),
            String.class
        );
        
        PatchOperation<String> operation1 = new PatchOperation<String>() {
            @Override
            public void apply(String data) {}
            
            @Override
            public Optional<String> getDebugId() {
                return Optional.of("AddTrade[wheat->emerald]");
            }
        };
        
        PatchOperation<String> operation2 = new PatchOperation<String>() {
            @Override
            public void apply(String data) {}
            
            @Override
            public Optional<String> getDebugId() {
                return Optional.of("RemoveTrade[cod]");
            }
        };
        
        PatchSet<String> patchSet = new PatchSet<>(
            Identifier.of("testmod:farmer_trades"),
            target,
            0,
            List.of(operation1, operation2),
            Optional.empty()
        );
        
        // Create a new registry with the patch set
        PatchRegistry testRegistry = new PatchRegistry();
        testRegistry.register(patchSet);
        testRegistry.freeze();
        
        ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
        PatchPlan testPlan = PatchPlan.compile(testRegistry, modLoadOrder);
        
        // Register the command
        PatchDebugCommandRegistrar.register(
            dispatcher,
            registryAccess,
            CommandManager.RegistrationEnvironment.ALL,
            testPlan
        );
        
        // Execute the command
        dispatcher.execute("tapestry patches minecraft:villager/farmer", commandSource);
        
        // Capture the feedback
        ArgumentCaptor<Supplier<Text>> feedbackCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(commandSource, atLeastOnce()).sendFeedback(feedbackCaptor.capture(), anyBoolean());
        
        // Get the feedback text
        String feedback = feedbackCaptor.getValue().get().getString();
        
        // Verify the output contains expected information
        assertTrue(feedback.contains("minecraft:villager/farmer"), "Output should contain target ID");
        assertTrue(feedback.contains("testmod:farmer_trades"), "Output should contain mod ID");
        assertTrue(feedback.contains("priority: 0"), "Output should contain priority");
        assertTrue(feedback.contains("AddTrade[wheat->emerald]"), "Output should contain operation 1");
        assertTrue(feedback.contains("RemoveTrade[cod]"), "Output should contain operation 2");
        assertTrue(feedback.contains("Statistics:"), "Output should contain statistics");
    }
}
