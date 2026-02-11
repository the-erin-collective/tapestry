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
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.REGISTRATION));
        assertEquals(TapestryPhase.REGISTRATION, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.FREEZE));
        assertEquals(TapestryPhase.FREEZE, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.TS_LOAD));
        assertEquals(TapestryPhase.TS_LOAD, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.TS_READY));
        assertEquals(TapestryPhase.TS_READY, controller.getCurrentPhase());
        
        assertDoesNotThrow(() -> controller.advanceTo(TapestryPhase.RUNTIME));
        assertEquals(TapestryPhase.RUNTIME, controller.getCurrentPhase());
    }
    
    @Test
    void testInvalidPhaseTransitions() {
        // Test skipping phases - should fail
        controller.advanceTo(TapestryPhase.DISCOVERY);
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
        controller.advanceTo(TapestryPhase.REGISTRATION);
        
        // Should work for current phase (exact match)
        assertDoesNotThrow(() -> {
            controller.requirePhase(TapestryPhase.REGISTRATION);
        });
        
        // Should fail for earlier phases (not exact match)
        assertThrows(IllegalStateException.class, () -> {
            controller.requirePhase(TapestryPhase.DISCOVERY);
        });
        
        // Should fail for later phases (not exact match)
        assertThrows(IllegalStateException.class, () -> {
            controller.requirePhase(TapestryPhase.TS_LOAD);
        });
    }
    
    @Test
    void testRequireAtLeast() {
        controller.advanceTo(TapestryPhase.DISCOVERY);
        controller.advanceTo(TapestryPhase.REGISTRATION);
        
        // Should work for current phase (>= current)
        assertDoesNotThrow(() -> {
            controller.requireAtLeast(TapestryPhase.REGISTRATION);
        });
        
        // Should work for earlier phases (>= earlier)
        assertDoesNotThrow(() -> {
            controller.requireAtLeast(TapestryPhase.DISCOVERY);
        });
        
        // Should work for much earlier phases (>= much earlier)
        assertDoesNotThrow(() -> {
            controller.requireAtLeast(TapestryPhase.BOOTSTRAP);
        });
        
        // Should fail for later phases (current < later)
        assertThrows(IllegalStateException.class, () -> {
            controller.requireAtLeast(TapestryPhase.TS_LOAD);
        });
    }
    
    @Test
    void testRequireAtMost() {
        controller.advanceTo(TapestryPhase.DISCOVERY);
        controller.advanceTo(TapestryPhase.REGISTRATION);
        
        // Should work for current phase (<= current)
        assertDoesNotThrow(() -> {
            controller.requireAtMost(TapestryPhase.REGISTRATION);
        });
        
        // Should work for later phases (<= later)
        assertDoesNotThrow(() -> {
            controller.requireAtMost(TapestryPhase.TS_LOAD);
        });
        
        // Should work for much later phases (<= much later)
        assertDoesNotThrow(() -> {
            controller.requireAtMost(TapestryPhase.RUNTIME);
        });
        
        // Should fail for earlier phases (current > earlier)
        assertThrows(IllegalStateException.class, () -> {
            controller.requireAtMost(TapestryPhase.DISCOVERY);
        });
    }
    
    @Test
    void testIsPhase() {
        controller.advanceTo(TapestryPhase.DISCOVERY);
        controller.advanceTo(TapestryPhase.REGISTRATION);
        
        assertTrue(controller.isPhase(TapestryPhase.REGISTRATION));
        assertTrue(controller.isPhase(TapestryPhase.DISCOVERY, TapestryPhase.REGISTRATION));
        assertFalse(controller.isPhase(TapestryPhase.TS_LOAD));
        assertFalse(controller.isPhase(TapestryPhase.TS_LOAD, TapestryPhase.TS_READY));
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
