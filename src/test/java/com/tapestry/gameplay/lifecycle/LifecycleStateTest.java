package com.tapestry.gameplay.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LifecycleState.
 */
class LifecycleStateTest {
    
    private LifecycleState lifecycleState;
    
    @BeforeEach
    void setUp() {
        lifecycleState = new LifecycleState();
    }
    
    @Test
    void testInitialPhase() {
        assertEquals(LifecyclePhase.TS_REGISTER, lifecycleState.getCurrentPhase());
        assertFalse(lifecycleState.areRegistriesFrozen());
    }
    
    @Test
    void testValidPhaseTransitions() {
        // TS_REGISTER -> TRAITS
        lifecycleState.advanceTo(LifecyclePhase.TRAITS);
        assertEquals(LifecyclePhase.TRAITS, lifecycleState.getCurrentPhase());
        
        // TRAITS -> COMPOSITION
        lifecycleState.advanceTo(LifecyclePhase.COMPOSITION);
        assertEquals(LifecyclePhase.COMPOSITION, lifecycleState.getCurrentPhase());
        
        // COMPOSITION -> INITIALIZATION (should freeze registries)
        lifecycleState.advanceTo(LifecyclePhase.INITIALIZATION);
        assertEquals(LifecyclePhase.INITIALIZATION, lifecycleState.getCurrentPhase());
        assertTrue(lifecycleState.areRegistriesFrozen());
        
        // INITIALIZATION -> RUNTIME
        lifecycleState.advanceTo(LifecyclePhase.RUNTIME);
        assertEquals(LifecyclePhase.RUNTIME, lifecycleState.getCurrentPhase());
    }
    
    @Test
    void testInvalidPhaseTransition_SkipPhase() {
        // Try to skip from TS_REGISTER to COMPOSITION
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> lifecycleState.advanceTo(LifecyclePhase.COMPOSITION)
        );
        
        assertTrue(exception.getMessage().contains("Invalid phase transition"));
        assertTrue(exception.getMessage().contains("TS_REGISTER"));
        assertTrue(exception.getMessage().contains("COMPOSITION"));
    }
    
    @Test
    void testInvalidPhaseTransition_Backward() {
        // Advance to TRAITS
        lifecycleState.advanceTo(LifecyclePhase.TRAITS);
        
        // Try to go back to TS_REGISTER
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> lifecycleState.advanceTo(LifecyclePhase.TS_REGISTER)
        );
        
        assertTrue(exception.getMessage().contains("Invalid phase transition"));
    }
    
    @Test
    void testAdvanceToSamePhase() {
        // Advancing to the same phase should be a no-op (with warning)
        lifecycleState.advanceTo(LifecyclePhase.TS_REGISTER);
        assertEquals(LifecyclePhase.TS_REGISTER, lifecycleState.getCurrentPhase());
    }
    
    @Test
    void testPhaseHistory() {
        lifecycleState.advanceTo(LifecyclePhase.TRAITS);
        lifecycleState.advanceTo(LifecyclePhase.COMPOSITION);
        
        List<LifecyclePhase> history = lifecycleState.getPhaseHistory();
        assertEquals(3, history.size());
        assertEquals(LifecyclePhase.TS_REGISTER, history.get(0));
        assertEquals(LifecyclePhase.TRAITS, history.get(1));
        assertEquals(LifecyclePhase.COMPOSITION, history.get(2));
    }
    
    @Test
    void testRequirePhase_Success() {
        // Should not throw
        assertDoesNotThrow(() -> lifecycleState.requirePhase(LifecyclePhase.TS_REGISTER));
    }
    
    @Test
    void testRequirePhase_Failure() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> lifecycleState.requirePhase(LifecyclePhase.RUNTIME)
        );
        
        assertTrue(exception.getMessage().contains("Operation requires phase RUNTIME"));
        assertTrue(exception.getMessage().contains("current phase is TS_REGISTER"));
    }
    
    @Test
    void testRequirePhase_MultipleAllowed() {
        // Should not throw when current phase is in the list
        assertDoesNotThrow(() -> 
            lifecycleState.requirePhase(LifecyclePhase.TS_REGISTER, LifecyclePhase.TRAITS)
        );
        
        // Should throw when current phase is not in the list
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> lifecycleState.requirePhase(LifecyclePhase.COMPOSITION, LifecyclePhase.RUNTIME)
        );
        
        assertTrue(exception.getMessage().contains("Operation requires one of phases"));
    }
    
    @Test
    void testPhaseComparison() {
        // Initial phase is TS_REGISTER
        assertFalse(lifecycleState.isBefore(LifecyclePhase.TS_REGISTER));
        assertTrue(lifecycleState.isBefore(LifecyclePhase.TRAITS));
        assertTrue(lifecycleState.isBefore(LifecyclePhase.RUNTIME));
        
        assertFalse(lifecycleState.isAfter(LifecyclePhase.TS_REGISTER));
        assertFalse(lifecycleState.isAfter(LifecyclePhase.TRAITS));
        
        assertTrue(lifecycleState.isAtOrBefore(LifecyclePhase.TS_REGISTER));
        assertTrue(lifecycleState.isAtOrBefore(LifecyclePhase.TRAITS));
        
        assertTrue(lifecycleState.isAtOrAfter(LifecyclePhase.TS_REGISTER));
        assertFalse(lifecycleState.isAtOrAfter(LifecyclePhase.TRAITS));
        
        // Advance to COMPOSITION
        lifecycleState.advanceTo(LifecyclePhase.TRAITS);
        lifecycleState.advanceTo(LifecyclePhase.COMPOSITION);
        
        assertTrue(lifecycleState.isAfter(LifecyclePhase.TS_REGISTER));
        assertTrue(lifecycleState.isAfter(LifecyclePhase.TRAITS));
        assertFalse(lifecycleState.isAfter(LifecyclePhase.COMPOSITION));
        assertFalse(lifecycleState.isAfter(LifecyclePhase.RUNTIME));
        
        assertTrue(lifecycleState.isBefore(LifecyclePhase.RUNTIME));
        assertFalse(lifecycleState.isBefore(LifecyclePhase.COMPOSITION));
    }
    
    @Test
    void testRegistryFreezing() {
        assertFalse(lifecycleState.areRegistriesFrozen());
        
        // Advance through phases
        lifecycleState.advanceTo(LifecyclePhase.TRAITS);
        assertFalse(lifecycleState.areRegistriesFrozen());
        
        lifecycleState.advanceTo(LifecyclePhase.COMPOSITION);
        assertFalse(lifecycleState.areRegistriesFrozen());
        
        // Registries should freeze when entering INITIALIZATION
        lifecycleState.advanceTo(LifecyclePhase.INITIALIZATION);
        assertTrue(lifecycleState.areRegistriesFrozen());
        
        lifecycleState.advanceTo(LifecyclePhase.RUNTIME);
        assertTrue(lifecycleState.areRegistriesFrozen());
    }
    
    @Test
    void testPhaseHistoryIsImmutable() {
        List<LifecyclePhase> history = lifecycleState.getPhaseHistory();
        
        // Should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> {
            history.add(LifecyclePhase.RUNTIME);
        });
    }
}
