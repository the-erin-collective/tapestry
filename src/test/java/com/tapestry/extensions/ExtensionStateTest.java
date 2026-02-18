package com.tapestry.extensions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 15: Unit tests for ExtensionState enum.
 * 
 * Tests state properties and utility methods.
 */
public class ExtensionStateTest {
    
    @Test
    @DisplayName("Should identify operational states correctly")
    void testIsOperational() {
        assertTrue(ExtensionState.READY.isOperational());
        
        assertFalse(ExtensionState.DISCOVERED.isOperational());
        assertFalse(ExtensionState.VALIDATED.isOperational());
        assertFalse(ExtensionState.TYPE_INITIALIZED.isOperational());
        assertFalse(ExtensionState.FROZEN.isOperational());
        assertFalse(ExtensionState.LOADING.isOperational());
        assertFalse(ExtensionState.FAILED.isOperational());
    }
    
    @Test
    @DisplayName("Should identify failed states correctly")
    void testIsFailed() {
        assertTrue(ExtensionState.FAILED.isFailed());
        
        assertFalse(ExtensionState.DISCOVERED.isFailed());
        assertFalse(ExtensionState.VALIDATED.isFailed());
        assertFalse(ExtensionState.TYPE_INITIALIZED.isFailed());
        assertFalse(ExtensionState.FROZEN.isFailed());
        assertFalse(ExtensionState.LOADING.isFailed());
        assertFalse(ExtensionState.READY.isFailed());
    }
    
    @Test
    @DisplayName("Should identify states allowing structural mutation")
    void testAllowsStructuralMutation() {
        assertTrue(ExtensionState.DISCOVERED.allowsStructuralMutation());
        assertTrue(ExtensionState.VALIDATED.allowsStructuralMutation());
        assertTrue(ExtensionState.TYPE_INITIALIZED.allowsStructuralMutation());
        
        assertFalse(ExtensionState.FROZEN.allowsStructuralMutation());
        assertFalse(ExtensionState.LOADING.allowsStructuralMutation());
        assertFalse(ExtensionState.READY.allowsStructuralMutation());
        assertFalse(ExtensionState.FAILED.allowsStructuralMutation());
    }
    
    @Test
    @DisplayName("Should identify states allowing runtime execution")
    void testAllowsRuntimeExecution() {
        assertTrue(ExtensionState.LOADING.allowsRuntimeExecution());
        assertTrue(ExtensionState.READY.allowsRuntimeExecution());
        
        assertFalse(ExtensionState.DISCOVERED.allowsRuntimeExecution());
        assertFalse(ExtensionState.VALIDATED.allowsRuntimeExecution());
        assertFalse(ExtensionState.TYPE_INITIALIZED.allowsRuntimeExecution());
        assertFalse(ExtensionState.FROZEN.allowsRuntimeExecution());
        assertFalse(ExtensionState.FAILED.allowsRuntimeExecution());
    }
    
    @ParameterizedTest
    @EnumSource(ExtensionState.class)
    @DisplayName("All enum values should be testable")
    void testAllEnumValues(ExtensionState state) {
        assertNotNull(state);
        assertNotNull(state.name());
        assertTrue(state.name().length() > 0);
        
        // All states should have defined behavior for utility methods
        // These should not throw exceptions
        boolean isOperational = state.isOperational();
        boolean isFailed = state.isFailed();
        boolean allowsStructuralMutation = state.allowsStructuralMutation();
        boolean allowsRuntimeExecution = state.allowsRuntimeExecution();
        
        // At least one property should be true for each non-terminal state
        // FROZEN and FAILED are special cases where most properties are false
        if (state == ExtensionState.FROZEN) {
            // FROZEN state intentionally has all properties false
            assertTrue(!isOperational && !isFailed && !allowsStructuralMutation && !allowsRuntimeExecution);
        } else if (state == ExtensionState.FAILED) {
            // FAILED state has isFailed=true but all other properties false
            assertTrue(!isOperational && isFailed && !allowsStructuralMutation && !allowsRuntimeExecution);
        } else {
            // All other states should have at least one property true
            assertTrue(isOperational || isFailed || allowsStructuralMutation || allowsRuntimeExecution);
        }
    }
    
    @Test
    @DisplayName("Should have correct number of states")
    void testStateCount() {
        ExtensionState[] states = ExtensionState.values();
        assertEquals(7, states.length);
        
        // Verify all expected states are present
        assertTrue(Arrays.asList(states).contains(ExtensionState.DISCOVERED));
        assertTrue(Arrays.asList(states).contains(ExtensionState.VALIDATED));
        assertTrue(Arrays.asList(states).contains(ExtensionState.TYPE_INITIALIZED));
        assertTrue(Arrays.asList(states).contains(ExtensionState.FROZEN));
        assertTrue(Arrays.asList(states).contains(ExtensionState.LOADING));
        assertTrue(Arrays.asList(states).contains(ExtensionState.READY));
        assertTrue(Arrays.asList(states).contains(ExtensionState.FAILED));
    }
}
