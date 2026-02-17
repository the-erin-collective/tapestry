package com.tapestry.extensions;

import com.tapestry.mod.ModRegistry;
import com.tapestry.mod.ModDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Phase 15: Unit tests for ExtensionLifecycleManager.
 * 
 * Tests state transitions, dependency enforcement, and failure propagation.
 */
public class ExtensionLifecycleManagerTest {
    
    @Mock
    private ModRegistry mockModRegistry;
    
    @Mock
    private ModDescriptor mockModA;
    
    @Mock
    private ModDescriptor mockModB;
    
    @Mock
    private ModDescriptor mockModC;
    
    private ExtensionLifecycleManager lifecycleManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mock mods
        when(mockModA.getId()).thenReturn("mod-a");
        when(mockModB.getId()).thenReturn("mod-b");
        when(mockModC.getId()).thenReturn("mod-c");
        
        // Setup dependencies: B depends on A, C depends on B
        when(mockModA.dependencies()).thenReturn(Collections.emptyList());
        when(mockModB.dependencies()).thenReturn(Collections.singletonList("mod-a"));
        when(mockModC.dependencies()).thenReturn(Collections.singletonList("mod-b"));
        
        // Setup mod registry behavior
        when(mockModRegistry.getModDescriptor("mod-a")).thenReturn(mockModA);
        when(mockModRegistry.getModDescriptor("mod-b")).thenReturn(mockModB);
        when(mockModRegistry.getModDescriptor("mod-c")).thenReturn(mockModC);
        when(mockModRegistry.getAllModDescriptors()).thenReturn(Arrays.asList(mockModA, mockModB, mockModC));
        
        lifecycleManager = ExtensionLifecycleManager.create(mockModRegistry);
    }
    
    @Test
    @DisplayName("Should initialize extensions to DISCOVERED state")
    void testInitializeDiscoveredExtensions() {
        Set<String> extensionIds = new HashSet<>(Arrays.asList("mod-a", "mod-b", "mod-c"));
        
        lifecycleManager.initializeDiscoveredExtensions(extensionIds);
        
        assertEquals(ExtensionState.DISCOVERED, lifecycleManager.getExtensionState("mod-a"));
        assertEquals(ExtensionState.DISCOVERED, lifecycleManager.getExtensionState("mod-b"));
        assertEquals(ExtensionState.DISCOVERED, lifecycleManager.getExtensionState("mod-c"));
    }
    
    @Test
    @DisplayName("Should allow valid sequential state transitions")
    void testValidStateTransitions() throws LifecycleViolationException {
        lifecycleManager.initializeDiscoveredExtensions(new HashSet<>(Arrays.asList("mod-a")));
        
        // Valid sequence: DISCOVERED → VALIDATED → TYPE_INITIALIZED → FROZEN → LOADING → READY
        lifecycleManager.transitionState("mod-a", ExtensionState.VALIDATED);
        assertEquals(ExtensionState.VALIDATED, lifecycleManager.getExtensionState("mod-a"));
        
        lifecycleManager.transitionState("mod-a", ExtensionState.TYPE_INITIALIZED);
        assertEquals(ExtensionState.TYPE_INITIALIZED, lifecycleManager.getExtensionState("mod-a"));
        
        lifecycleManager.transitionState("mod-a", ExtensionState.FROZEN);
        assertEquals(ExtensionState.FROZEN, lifecycleManager.getExtensionState("mod-a"));
        
        lifecycleManager.transitionState("mod-a", ExtensionState.LOADING);
        assertEquals(ExtensionState.LOADING, lifecycleManager.getExtensionState("mod-a"));
        
        lifecycleManager.transitionState("mod-a", ExtensionState.READY);
        assertEquals(ExtensionState.READY, lifecycleManager.getExtensionState("mod-a"));
    }
    
    @Test
    @DisplayName("Should reject invalid state transitions")
    void testInvalidStateTransitions() {
        lifecycleManager.initializeDiscoveredExtensions(new HashSet<>(Arrays.asList("mod-a")));
        
        // Invalid: DISCOVERED → LOADING (skipping intermediate states)
        assertThrows(LifecycleViolationException.class, 
            () -> lifecycleManager.transitionState("mod-a", ExtensionState.LOADING));
        
        // Invalid: VALIDATED → DISCOVERED (backwards transition)
        assertThrows(LifecycleViolationException.class, 
            () -> lifecycleManager.transitionState("mod-a", ExtensionState.VALIDATED));
        assertThrows(LifecycleViolationException.class, 
            () -> lifecycleManager.transitionState("mod-a", ExtensionState.DISCOVERED));
    }
    
    @Test
    @DisplayName("Should allow ANY → FAILED transition")
    void testAnyToFailedTransition() throws LifecycleViolationException {
        lifecycleManager.initializeDiscoveredExtensions(new HashSet<>(Arrays.asList("mod-a")));
        
        // Should allow transition from any state to FAILED
        lifecycleManager.transitionState("mod-a", ExtensionState.VALIDATED);
        lifecycleManager.transitionState("mod-a", ExtensionState.FAILED);
        assertEquals(ExtensionState.FAILED, lifecycleManager.getExtensionState("mod-a"));
        
        // Even from FAILED state (though it's already terminal)
        lifecycleManager.initializeDiscoveredExtensions(new HashSet<>(Arrays.asList("mod-b")));
        lifecycleManager.transitionState("mod-b", ExtensionState.FAILED);
        assertEquals(ExtensionState.FAILED, lifecycleManager.getExtensionState("mod-b"));
    }
    
    @Test
    @DisplayName("Should reject transitions from terminal states")
    void testTerminalStateTransitions() throws LifecycleViolationException {
        lifecycleManager.initializeDiscoveredExtensions(new HashSet<>(Arrays.asList("mod-a")));
        
        // Transition to READY (terminal)
        lifecycleManager.transitionState("mod-a", ExtensionState.VALIDATED);
        lifecycleManager.transitionState("mod-a", ExtensionState.TYPE_INITIALIZED);
        lifecycleManager.transitionState("mod-a", ExtensionState.FROZEN);
        lifecycleManager.transitionState("mod-a", ExtensionState.LOADING);
        lifecycleManager.transitionState("mod-a", ExtensionState.READY);
        
        // Should reject any transition from READY (except to FAILED)
        assertThrows(LifecycleViolationException.class, 
            () -> lifecycleManager.transitionState("mod-a", ExtensionState.LOADING));
        assertThrows(LifecycleViolationException.class, 
            () -> lifecycleManager.transitionState("mod-a", ExtensionState.FROZEN));
        
        // Should allow READY → FAILED
        assertDoesNotThrow(() -> lifecycleManager.transitionState("mod-a", ExtensionState.FAILED));
    }
    
    @Test
    @DisplayName("Should enforce dependency readiness before LOADING")
    void testDependencyReadinessEnforcement() throws LifecycleViolationException {
        lifecycleManager.initializeDiscoveredExtensions(new HashSet<>(Arrays.asList("mod-a", "mod-b", "mod-c")));
        
        // Transition mod-a to READY (no dependencies)
        lifecycleManager.transitionState("mod-a", ExtensionState.VALIDATED);
        lifecycleManager.transitionState("mod-a", ExtensionState.TYPE_INITIALIZED);
        lifecycleManager.transitionState("mod-a", ExtensionState.FROZEN);
        lifecycleManager.transitionState("mod-a", ExtensionState.LOADING);
        lifecycleManager.transitionState("mod-a", ExtensionState.READY);
        
        // Try to transition mod-b to LOADING without dependencies ready
        lifecycleManager.transitionState("mod-b", ExtensionState.VALIDATED);
        lifecycleManager.transitionState("mod-b", ExtensionState.TYPE_INITIALIZED);
        lifecycleManager.transitionState("mod-b", ExtensionState.FROZEN);
        
        // Should fail because mod-a is ready, but we're testing the logic
        // Actually, this should work since mod-a is ready
        assertDoesNotThrow(() -> lifecycleManager.transitionState("mod-b", ExtensionState.LOADING));
        
        // Now try mod-c without mod-b ready
        lifecycleManager.transitionState("mod-c", ExtensionState.VALIDATED);
        lifecycleManager.transitionState("mod-c", ExtensionState.TYPE_INITIALIZED);
        lifecycleManager.transitionState("mod-c", ExtensionState.FROZEN);
        
        // Should fail because mod-b is not ready yet (still in LOADING)
        assertThrows(LifecycleViolationException.class, 
            () -> lifecycleManager.transitionState("mod-c", ExtensionState.LOADING));
    }
    
    @Test
    @DisplayName("Should propagate failure to dependents")
    void testFailurePropagation() throws LifecycleViolationException {
        lifecycleManager.initializeDiscoveredExtensions(new HashSet<>(Arrays.asList("mod-a", "mod-b", "mod-c")));
        
        // Get mod-b to LOADING state
        lifecycleManager.transitionState("mod-a", ExtensionState.VALIDATED);
        lifecycleManager.transitionState("mod-a", ExtensionState.TYPE_INITIALIZED);
        lifecycleManager.transitionState("mod-a", ExtensionState.FROZEN);
        lifecycleManager.transitionState("mod-a", ExtensionState.LOADING);
        lifecycleManager.transitionState("mod-a", ExtensionState.READY);
        
        lifecycleManager.transitionState("mod-b", ExtensionState.VALIDATED);
        lifecycleManager.transitionState("mod-b", ExtensionState.TYPE_INITIALIZED);
        lifecycleManager.transitionState("mod-b", ExtensionState.FROZEN);
        lifecycleManager.transitionState("mod-b", ExtensionState.LOADING);
        
        // Fail mod-a
        lifecycleManager.transitionState("mod-a", ExtensionState.FAILED);
        lifecycleManager.setFailureReason("mod-a", "Test failure");
        
        // mod-b should fail due to dependency failure
        assertEquals(ExtensionState.FAILED, lifecycleManager.getExtensionState("mod-b"));
        assertEquals("Dependency 'mod-a' failed", lifecycleManager.getFailureReason("mod-b"));
    }
    
    @Test
    @DisplayName("Should get extensions in specific state")
    void testGetExtensionsInState() throws LifecycleViolationException {
        lifecycleManager.initializeDiscoveredExtensions(new HashSet<>(Arrays.asList("mod-a", "mod-b", "mod-c")));
        
        // Transition mods to different states
        lifecycleManager.transitionState("mod-a", ExtensionState.VALIDATED);
        lifecycleManager.transitionState("mod-b", ExtensionState.VALIDATED);
        lifecycleManager.transitionState("mod-b", ExtensionState.TYPE_INITIALIZED);
        lifecycleManager.transitionState("mod-c", ExtensionState.VALIDATED);
        lifecycleManager.transitionState("mod-c", ExtensionState.FAILED);
        
        Set<String> validated = lifecycleManager.getExtensionsInState(ExtensionState.VALIDATED);
        Set<String> typeInitialized = lifecycleManager.getExtensionsInState(ExtensionState.TYPE_INITIALIZED);
        Set<String> failed = lifecycleManager.getExtensionsInState(ExtensionState.FAILED);
        
        assertEquals(new HashSet<>(Arrays.asList("mod-a", "mod-c")), validated);
        assertEquals(new HashSet<>(Arrays.asList("mod-b")), typeInitialized);
        assertEquals(new HashSet<>(Arrays.asList("mod-c")), failed);
    }
    
    @Test
    @DisplayName("Should provide diagnostic information")
    void testGetDiagnostics() throws LifecycleViolationException {
        lifecycleManager.initializeDiscoveredExtensions(new HashSet<>(Arrays.asList("mod-a", "mod-b", "mod-c")));
        
        // Transition mods to different states
        lifecycleManager.transitionState("mod-a", ExtensionState.VALIDATED);
        lifecycleManager.transitionState("mod-b", ExtensionState.VALIDATED);
        lifecycleManager.transitionState("mod-b", ExtensionState.TYPE_INITIALIZED);
        lifecycleManager.transitionState("mod-c", ExtensionState.FAILED);
        lifecycleManager.setFailureReason("mod-c", "Test failure");
        
        var diagnostics = lifecycleManager.getDiagnostics();
        
        assertEquals(1, diagnostics.getStateCounts().get(ExtensionState.VALIDATED).intValue());
        assertEquals(1, diagnostics.getStateCounts().get(ExtensionState.TYPE_INITIALIZED).intValue());
        assertEquals(1, diagnostics.getStateCounts().get(ExtensionState.FAILED).intValue());
        assertEquals(1, diagnostics.getStateCounts().get(ExtensionState.DISCOVERED).intValue());
        
        assertEquals("Test failure", diagnostics.getFailureReasons().get("mod-c"));
    }
    
    @Test
    @DisplayName("Should handle unknown extensions gracefully")
    void testUnknownExtensionHandling() {
        // Unknown extension should default to DISCOVERED
        assertEquals(ExtensionState.DISCOVERED, lifecycleManager.getExtensionState("unknown-mod"));
        
        // Failure reason should be null for non-failed extensions
        assertNull(lifecycleManager.getFailureReason("unknown-mod"));
    }
    
    @Test
    @DisplayName("Should handle extensions with no dependencies")
    void testNoDependencies() throws LifecycleViolationException {
        lifecycleManager.initializeDiscoveredExtensions(new HashSet<>(Arrays.asList("mod-a")));
        
        // Mod with no dependencies should transition normally
        assertDoesNotThrow(() -> {
            lifecycleManager.transitionState("mod-a", ExtensionState.VALIDATED);
            lifecycleManager.transitionState("mod-a", ExtensionState.TYPE_INITIALIZED);
            lifecycleManager.transitionState("mod-a", ExtensionState.FROZEN);
            lifecycleManager.transitionState("mod-a", ExtensionState.LOADING);
            lifecycleManager.transitionState("mod-a", ExtensionState.READY);
        });
    }
}
