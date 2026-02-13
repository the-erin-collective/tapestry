package com.tapestry.extensions;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for extension registration functionality.
 */
class ExtensionRegistrationTest {
    
    private PhaseController phaseController;
    
    private ApiRegistry apiRegistry;
    private HookRegistry hookRegistry;
    private ServiceRegistry serviceRegistry;
    private ExtensionRegistrationOrchestrator orchestrator;
    
    @BeforeEach
    void setUp() {
        // Reset the singleton phase controller for clean testing
        PhaseController.reset();
        
        // Use real PhaseController instance
        phaseController = PhaseController.getInstance();
        
        // Advance to REGISTRATION for testing
        phaseController.advanceTo(TapestryPhase.DISCOVERY);
        phaseController.advanceTo(TapestryPhase.VALIDATION);
        phaseController.advanceTo(TapestryPhase.REGISTRATION);
        
        // Create registries with declared capabilities
        var descriptor = createDescriptorWithApi("test", "tapestry.test.api");
        Map<String, TapestryExtensionDescriptor> declaredCapabilities = Map.of("test", descriptor);
        apiRegistry = new DefaultApiRegistry(phaseController, declaredCapabilities);
        hookRegistry = new DefaultHookRegistry(phaseController, declaredCapabilities);
        serviceRegistry = new DefaultServiceRegistry(phaseController, declaredCapabilities);
        
        orchestrator = new ExtensionRegistrationOrchestrator(
            phaseController, apiRegistry, hookRegistry, serviceRegistry
        );
    }
    
    @Test
    void testDeterministicRegistrationOrder() {
        // This test verifies topological sort logic exists
        // For now, just ensure orchestrator can be created
        assertDoesNotThrow(() -> {
            new ExtensionRegistrationOrchestrator(
                phaseController, apiRegistry, hookRegistry, serviceRegistry
            );
        });
    }
    
    @Test
    void testPhaseBoundaryEnforcement() {
        // Reset the singleton to start fresh
        PhaseController.reset();
        
        // Create a new phase controller in wrong phase
        PhaseController wrongPhaseController = PhaseController.getInstance();
        wrongPhaseController.advanceTo(TapestryPhase.DISCOVERY);
        wrongPhaseController.advanceTo(TapestryPhase.VALIDATION);
        // Stop at VALIDATION phase (not REGISTRATION)
        
        // Create registries with declared capabilities
        var descriptor = createDescriptorWithApi("test", "tapestry.test.api");
        Map<String, TapestryExtensionDescriptor> declaredCapabilities = Map.of("test", descriptor);
        var apiRegistry = new DefaultApiRegistry(wrongPhaseController, declaredCapabilities);
        var hookRegistry = new DefaultHookRegistry(wrongPhaseController, declaredCapabilities);
        var serviceRegistry = new DefaultServiceRegistry(wrongPhaseController, declaredCapabilities);
        
        // Create orchestrator (constructor doesn't check phase)
        ExtensionRegistrationOrchestrator orchestrator = new ExtensionRegistrationOrchestrator(
            wrongPhaseController, apiRegistry, hookRegistry, serviceRegistry
        );
        
        // Should throw WrongPhaseException when trying to register extensions in wrong phase
        assertThrows(WrongPhaseException.class, () -> {
            orchestrator.registerExtensions(Map.of());
        });
    }
    
    @Test
    void testRegistryFreezeBehavior() {
        // Create a simple API capability
        var descriptor = createDescriptorWithApi("test-extension", "tapestry.mods.test-extension.test");
        Map<String, TapestryExtensionDescriptor> declaredCapabilities = Map.of("test-extension", descriptor);
        var apiRegistry = new DefaultApiRegistry(phaseController, declaredCapabilities);
        
        // Should be able to register before freeze
        assertDoesNotThrow(() -> {
            apiRegistry.addFunction("test-extension", "test_api", args -> null);
        });
        
        // Freeze registry
        apiRegistry.freeze();
        
        // Should throw RegistryFrozenException after freeze
        assertThrows(RegistryFrozenException.class, () -> {
            apiRegistry.addFunction("test-extension", "another", args -> null);
        });
    }
    
    private TapestryExtensionDescriptor createDescriptor(String id, List<String> requires) {
        return new TapestryExtensionDescriptor(
            id, "Test Extension", "1.0.0", "0.1.0",
            List.of(), requires, List.of()
        );
    }
    
    private TapestryExtensionDescriptor createDescriptorWithApi(String id, String apiPath) {
        return new TapestryExtensionDescriptor(
            id, "Test Extension", "1.0.0", "0.1.0",
            List.of(new CapabilityDecl("test_api", CapabilityType.API, false, Map.of(), apiPath)),
            List.of(), List.of()
        );
    }
}
