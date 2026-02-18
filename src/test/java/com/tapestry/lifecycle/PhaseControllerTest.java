package com.tapestry.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PhaseController.
 */
public class PhaseControllerTest {
    
    private PhaseController controller;
    
    @BeforeEach
    void setUp() {
        // Reset for each test
        PhaseController.reset();
        controller = PhaseController.getInstance();
    }
    
    @Test
    void testInitialState() {
        assertEquals(TapestryPhase.BOOTSTRAP, controller.getCurrentPhase());
    }
    
    @Test
    void testValidPhaseTransitions() {
        // Test all valid transitions in order
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.DISCOVERY));
        assertEquals(TapestryPhase.DISCOVERY, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.VALIDATION));
        assertEquals(TapestryPhase.VALIDATION, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.REGISTRATION));
        assertEquals(TapestryPhase.REGISTRATION, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.FREEZE));
        assertEquals(TapestryPhase.FREEZE, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.TS_LOAD));
        assertEquals(TapestryPhase.TS_LOAD, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.TS_REGISTER));
        assertEquals(TapestryPhase.TS_REGISTER, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.TS_ACTIVATE));
        assertEquals(TapestryPhase.TS_ACTIVATE, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.TS_READY));
        assertEquals(TapestryPhase.TS_READY, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.PERSISTENCE_READY));
        assertEquals(TapestryPhase.PERSISTENCE_READY, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.EVENT));
        assertEquals(TapestryPhase.EVENT, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.RUNTIME));
        assertEquals(TapestryPhase.RUNTIME, controller.getCurrentPhase());
    }
    
    @Test
    void testInvalidPhaseTransitions() {
        // Test skipping phases - should fail
        controller.advanceTo(TapestryPhase.DISCOVERY);
        controller.advanceTo(TapestryPhase.VALIDATION);
        controller.advanceTo(TapestryPhase.REGISTRATION);
        
        assertThrows(IllegalStateException.class, () -> {
            controller.advanceTo(TapestryPhase.TS_LOAD); // Skipping FREEZE
        });
        
        assertThrows(IllegalStateException.class, () -> {
            controller.advanceTo(TapestryPhase.RUNTIME); // Skipping multiple phases
        });
        
        assertThrows(IllegalStateException.class, () -> {
            controller.advanceTo(TapestryPhase.DISCOVERY); // Going backwards
        });
    }
    
    @Test
    void testSamePhaseTransition() {
        // Advancing to the same phase should not throw but should log a warning
        assertDoesNotThrow(() -> {
            controller.advanceTo(TapestryPhase.DISCOVERY);
            controller.advanceTo(TapestryPhase.DISCOVERY); // Same phase
        });
        assertEquals(TapestryPhase.DISCOVERY, controller.getCurrentPhase());
    }
    
    @Test
    void testRequirePhase() {
        controller.advanceTo(TapestryPhase.DISCOVERY);
        controller.advanceTo(TapestryPhase.VALIDATION);
        controller.advanceTo(TapestryPhase.REGISTRATION);
        controller.advanceTo(TapestryPhase.FREEZE);
        
        // Should work for current phase (exact match)
        assertDoesNotThrow(() -> {
            controller.requirePhase(TapestryPhase.FREEZE);
        });
        
        // Should fail for later phases (requirePhase is exact match)
        assertThrows(IllegalStateException.class, () -> {
            controller.requirePhase(TapestryPhase.TS_LOAD);
        });
        
        // Should fail for earlier phases
        assertThrows(IllegalStateException.class, () -> {
            controller.requirePhase(TapestryPhase.DISCOVERY);
        });
    }
    
    @Test
    void testRequireAtLeast() {
        // Test from DISCOVERY phase (early phase)
        controller.advanceTo(TapestryPhase.DISCOVERY);
        
        // Should work for current phase
        assertDoesNotThrow(() -> {
            controller.requireAtLeast(TapestryPhase.DISCOVERY);
        });
        
        // Should fail for later phases when we're in early phase
        assertThrows(IllegalStateException.class, () -> {
            controller.requireAtLeast(TapestryPhase.REGISTRATION);
        });
        
        // Advance to RUNTIME phase for the other tests
        controller.advanceTo(TapestryPhase.VALIDATION);
        controller.advanceTo(TapestryPhase.REGISTRATION);
        controller.advanceTo(TapestryPhase.FREEZE);
        controller.advanceTo(TapestryPhase.TS_LOAD);
        controller.advanceTo(TapestryPhase.TS_REGISTER);
        controller.advanceTo(TapestryPhase.TS_ACTIVATE);
        controller.advanceTo(TapestryPhase.TS_READY);
        controller.advanceTo(TapestryPhase.PERSISTENCE_READY);
        controller.advanceTo(TapestryPhase.EVENT);
        controller.advanceTo(TapestryPhase.RUNTIME);
        
        // Debug: Check current phase
        System.out.println("Current phase after setup: " + controller.getCurrentPhase());
        
        // Should work for current phase (>= current)
        assertDoesNotThrow(() -> {
            System.out.println("Calling requireAtLeast(FREEZE) from phase: " + controller.getCurrentPhase());
            controller.requireAtLeast(TapestryPhase.FREEZE);
        });
        
        // Should work for later phases (we're at RUNTIME, so RUNTIME should work)
        assertDoesNotThrow(() -> {
            controller.requireAtLeast(TapestryPhase.RUNTIME);
        });
        
        // Should work for earlier phases when we're in later phase
        assertDoesNotThrow(() -> {
            controller.requireAtLeast(TapestryPhase.DISCOVERY);
        });
    }
    
    @Test
    void testRequireAtMost() {
        controller.advanceTo(TapestryPhase.DISCOVERY);
        controller.advanceTo(TapestryPhase.VALIDATION);
        controller.advanceTo(TapestryPhase.REGISTRATION);
        
        // Should work for current phase (<= current)
        assertDoesNotThrow(() -> {
            controller.requireAtMost(TapestryPhase.REGISTRATION);
        });
        
        // Should work for later phases (current <= later)
        assertDoesNotThrow(() -> {
            controller.requireAtMost(TapestryPhase.FREEZE);
        });
        
        // Should fail for earlier phases (current > earlier)
        assertThrows(IllegalStateException.class, () -> {
            controller.requireAtMost(TapestryPhase.DISCOVERY);
        });
    }
    
    @Test
    void testIsPhase() {
        controller.advanceTo(TapestryPhase.DISCOVERY);
        controller.advanceTo(TapestryPhase.VALIDATION);
        controller.advanceTo(TapestryPhase.REGISTRATION);
        
        assertTrue(controller.isPhase(TapestryPhase.REGISTRATION));
        assertTrue(controller.isPhase(TapestryPhase.DISCOVERY, TapestryPhase.VALIDATION, TapestryPhase.REGISTRATION));
        assertFalse(controller.isPhase(TapestryPhase.FREEZE));
    }
    
    @Test
    void testPhaseTransitionTime() {
        Instant before = controller.getPhaseTransitionTime();
        
        controller.advanceTo(TapestryPhase.DISCOVERY);
        
        Instant after = controller.getPhaseTransitionTime();
        assertNotNull(after);
        assertTrue(after.isAfter(before) || after.equals(before));
    }
}
