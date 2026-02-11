package com.tapestry.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhaseControllerTest {
    
    private PhaseController controller;
    
    @BeforeEach
    void setUp() {
        // Reset singleton for each test
        controller = PhaseController.getInstance();
        // Reset to initial state
        while (controller.getCurrentPhase() != TapestryPhase.BOOTSTRAP) {
            // This is a bit of a hack for testing - in real code we'd never go backwards
            try {
                controller.advanceTo(TapestryPhase.BOOTSTRAP);
            } catch (IllegalStateException e) {
                // Expected, break the loop
                break;
            }
        }
    }
    
    @Test
    void testInitialState() {
        assertEquals(TapestryPhase.BOOTSTRAP, controller.getCurrentPhase());
    }
    
    @Test
    void testValidPhaseTransitions() {
        // Test all valid forward transitions
        controller.advanceTo(TapestryPhase.DISCOVERY);
        assertEquals(TapestryPhase.DISCOVERY, controller.getCurrentPhase());
        
        controller.advanceTo(TapestryPhase.REGISTRATION);
        assertEquals(TapestryPhase.REGISTRATION, controller.getCurrentPhase());
        
        controller.advanceTo(TapestryPhase.FREEZE);
        assertEquals(TapestryPhase.FREEZE, controller.getCurrentPhase());
        
        controller.advanceTo(TapestryPhase.TS_LOAD);
        assertEquals(TapestryPhase.TS_LOAD, controller.getCurrentPhase());
        
        controller.advanceTo(TapestryPhase.TS_READY);
        assertEquals(TapestryPhase.TS_READY, controller.getCurrentPhase());
        
        controller.advanceTo(TapestryPhase.RUNTIME);
        assertEquals(TapestryPhase.RUNTIME, controller.getCurrentPhase());
    }
    
    @Test
    void testInvalidPhaseTransitions() {
        // Test backward transitions
        controller.advanceTo(TapestryPhase.DISCOVERY);
        
        assertThrows(IllegalStateException.class, () -> {
            controller.advanceTo(TapestryPhase.BOOTSTRAP);
        });
        
        // Test skipping phases - this should now be forbidden
        controller.advanceTo(TapestryPhase.REGISTRATION);
        
        assertThrows(IllegalStateException.class, () -> {
            controller.advanceTo(TapestryPhase.RUNTIME); // Skipping phases
        });
        
        // Test exact next phase requirement
        assertThrows(IllegalStateException.class, () -> {
            controller.advanceTo(TapestryPhase.FREEZE); // Skipping TS_LOAD
        });
    }
    
    @Test
    void testSamePhaseTransition() {
        // Advancing to the same phase should not throw but should log a warning
        assertDoesNotThrow(() -> {
            controller.advanceTo(TapestryPhase.BOOTSTRAP);
        });
        assertEquals(TapestryPhase.BOOTSTRAP, controller.getCurrentPhase());
    }
    
    @Test
    void testRequirePhase() {
        controller.advanceTo(TapestryPhase.REGISTRATION);
        
        // Should not throw
        assertDoesNotThrow(() -> {
            controller.requirePhase(TapestryPhase.REGISTRATION);
        });
        
        // Should throw
        assertThrows(IllegalStateException.class, () -> {
            controller.requirePhase(TapestryPhase.DISCOVERY);
        });
    }
    
    @Test
    void testRequireAtLeast() {
        controller.advanceTo(TapestryPhase.REGISTRATION);
        
        // Should not throw - REGISTRATION is at or after DISCOVERY
        assertDoesNotThrow(() -> {
            controller.requireAtLeast(TapestryPhase.DISCOVERY);
        });
        
        // Should not throw - REGISTRATION is at or after REGISTRATION
        assertDoesNotThrow(() -> {
            controller.requireAtLeast(TapestryPhase.REGISTRATION);
        });
        
        // Should throw - REGISTRATION is before FREEZE
        assertThrows(IllegalStateException.class, () -> {
            controller.requireAtLeast(TapestryPhase.FREEZE);
        });
    }
    
    @Test
    void testRequireAtMost() {
        controller.advanceTo(TapestryPhase.REGISTRATION);
        
        // Should not throw - REGISTRATION is at or before FREEZE
        assertDoesNotThrow(() -> {
            controller.requireAtMost(TapestryPhase.FREEZE);
        });
        
        // Should not throw - REGISTRATION is at or before REGISTRATION
        assertDoesNotThrow(() -> {
            controller.requireAtMost(TapestryPhase.REGISTRATION);
        });
        
        // Should throw - REGISTRATION is after DISCOVERY
        assertThrows(IllegalStateException.class, () -> {
            controller.requireAtMost(TapestryPhase.DISCOVERY);
        });
    }
    
    @Test
    void testIsPhase() {
        controller.advanceTo(TapestryPhase.REGISTRATION);
        
        assertTrue(controller.isPhase(TapestryPhase.REGISTRATION));
        assertFalse(controller.isPhase(TapestryPhase.DISCOVERY));
        assertFalse(controller.isPhase(TapestryPhase.FREEZE));
        
        // Test multiple phases
        assertTrue(controller.isPhase(TapestryPhase.DISCOVERY, TapestryPhase.REGISTRATION));
        assertTrue(controller.isPhase(TapestryPhase.REGISTRATION, TapestryPhase.FREEZE));
        assertFalse(controller.isPhase(TapestryPhase.DISCOVERY, TapestryPhase.FREEZE));
    }
    
    @Test
    void testPhaseTransitionTime() {
        var initialTime = controller.getPhaseTransitionTime();
        
        // Small delay to ensure different timestamp
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        controller.advanceTo(TapestryPhase.DISCOVERY);
        var newTime = controller.getPhaseTransitionTime();
        
        assertTrue(newTime.isAfter(initialTime));
    }
    
    @Test
    void testTapestryPhaseEnumMethods() {
        // Test isAfter
        assertTrue(TapestryPhase.REGISTRATION.isAfter(TapestryPhase.DISCOVERY));
        assertFalse(TapestryPhase.DISCOVERY.isAfter(TapestryPhase.REGISTRATION));
        assertFalse(TapestryPhase.REGISTRATION.isAfter(TapestryPhase.REGISTRATION));
        
        // Test isBefore
        assertTrue(TapestryPhase.DISCOVERY.isBefore(TapestryPhase.REGISTRATION));
        assertFalse(TapestryPhase.REGISTRATION.isBefore(TapestryPhase.DISCOVERY));
        assertFalse(TapestryPhase.REGISTRATION.isBefore(TapestryPhase.REGISTRATION));
        
        // Test isAtOrAfter
        assertTrue(TapestryPhase.REGISTRATION.isAtOrAfter(TapestryPhase.DISCOVERY));
        assertTrue(TapestryPhase.REGISTRATION.isAtOrAfter(TapestryPhase.REGISTRATION));
        assertFalse(TapestryPhase.DISCOVERY.isAtOrAfter(TapestryPhase.REGISTRATION));
        
        // Test isAtOrBefore
        assertTrue(TapestryPhase.DISCOVERY.isAtOrBefore(TapestryPhase.REGISTRATION));
        assertTrue(TapestryPhase.REGISTRATION.isAtOrBefore(TapestryPhase.REGISTRATION));
        assertFalse(TapestryPhase.REGISTRATION.isAtOrBefore(TapestryPhase.DISCOVERY));
    }
}
