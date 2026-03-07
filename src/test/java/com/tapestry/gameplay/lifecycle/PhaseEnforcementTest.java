package com.tapestry.gameplay.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PhaseEnforcement utilities.
 */
class PhaseEnforcementTest {
    
    private LifecycleState lifecycleState;
    
    @BeforeEach
    void setUp() {
        lifecycleState = new LifecycleState();
    }
    
    @Test
    void testAssertPhase_Success() {
        // Should not throw when phase matches
        assertDoesNotThrow(() -> 
            PhaseEnforcement.assertPhase(lifecycleState, LifecyclePhase.TS_REGISTER)
        );
    }
    
    @Test
    void testAssertPhase_Failure() {
        // Should throw when phase doesn't match
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> PhaseEnforcement.assertPhase(lifecycleState, LifecyclePhase.RUNTIME)
        );
        
        // Verify error message contains expected and actual phases
        String message = exception.getMessage();
        assertTrue(message.contains("Phase violation"));
        assertTrue(message.contains("Expected phase RUNTIME"));
        assertTrue(message.contains("current phase is TS_REGISTER"));
    }
    
    @Test
    void testAssertPhase_MultipleAllowed_Success() {
        // Should not throw when current phase is in the list
        assertDoesNotThrow(() -> 
            PhaseEnforcement.assertPhase(
                lifecycleState, 
                LifecyclePhase.TS_REGISTER, 
                LifecyclePhase.TRAITS
            )
        );
    }
    
    @Test
    void testAssertPhase_MultipleAllowed_Failure() {
        // Should throw when current phase is not in the list
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> PhaseEnforcement.assertPhase(
                lifecycleState,
                LifecyclePhase.COMPOSITION,
                LifecyclePhase.RUNTIME
            )
        );
        
        // Verify error message contains allowed phases and actual phase
        String message = exception.getMessage();
        assertTrue(message.contains("Phase violation"));
        assertTrue(message.contains("Expected one of phases"));
        assertTrue(message.contains("COMPOSITION"));
        assertTrue(message.contains("RUNTIME"));
        assertTrue(message.contains("current phase is TS_REGISTER"));
    }
    
    @Test
    void testAssertPhase_AfterPhaseTransition() {
        // Advance to TRAITS phase
        lifecycleState.advanceTo(LifecyclePhase.TRAITS);
        
        // Should succeed for TRAITS
        assertDoesNotThrow(() -> 
            PhaseEnforcement.assertPhase(lifecycleState, LifecyclePhase.TRAITS)
        );
        
        // Should fail for TS_REGISTER
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> PhaseEnforcement.assertPhase(lifecycleState, LifecyclePhase.TS_REGISTER)
        );
        
        assertTrue(exception.getMessage().contains("Expected phase TS_REGISTER"));
        assertTrue(exception.getMessage().contains("current phase is TRAITS"));
    }
    
    @Test
    void testFormatPhaseViolationError_SinglePhase() {
        String errorMsg = PhaseEnforcement.formatPhaseViolationError(
            LifecyclePhase.COMPOSITION,
            LifecyclePhase.TS_REGISTER
        );
        
        assertTrue(errorMsg.contains("Phase violation"));
        assertTrue(errorMsg.contains("Expected phase COMPOSITION"));
        assertTrue(errorMsg.contains("current phase is TS_REGISTER"));
        assertTrue(errorMsg.contains("can only be performed during the COMPOSITION phase"));
    }
    
    @Test
    void testFormatPhaseViolationError_MultiplePhases() {
        LifecyclePhase[] allowedPhases = {
            LifecyclePhase.TS_REGISTER,
            LifecyclePhase.TRAITS,
            LifecyclePhase.COMPOSITION
        };
        
        String errorMsg = PhaseEnforcement.formatPhaseViolationError(
            allowedPhases,
            LifecyclePhase.RUNTIME
        );
        
        assertTrue(errorMsg.contains("Phase violation"));
        assertTrue(errorMsg.contains("Expected one of phases"));
        assertTrue(errorMsg.contains("TS_REGISTER"));
        assertTrue(errorMsg.contains("TRAITS"));
        assertTrue(errorMsg.contains("COMPOSITION"));
        assertTrue(errorMsg.contains("current phase is RUNTIME"));
    }
    
    @Test
    void testFormatPhaseViolationError_TwoPhases() {
        LifecyclePhase[] allowedPhases = {
            LifecyclePhase.TS_REGISTER,
            LifecyclePhase.TRAITS
        };
        
        String errorMsg = PhaseEnforcement.formatPhaseViolationError(
            allowedPhases,
            LifecyclePhase.RUNTIME
        );
        
        // Should use "or" for two phases
        assertTrue(errorMsg.contains("TS_REGISTER or TRAITS"));
    }
    
    @Test
    void testAssertPhase_ThroughAllPhases() {
        // Test assertion through all phase transitions
        PhaseEnforcement.assertPhase(lifecycleState, LifecyclePhase.TS_REGISTER);
        
        lifecycleState.advanceTo(LifecyclePhase.TRAITS);
        PhaseEnforcement.assertPhase(lifecycleState, LifecyclePhase.TRAITS);
        
        lifecycleState.advanceTo(LifecyclePhase.COMPOSITION);
        PhaseEnforcement.assertPhase(lifecycleState, LifecyclePhase.COMPOSITION);
        
        lifecycleState.advanceTo(LifecyclePhase.INITIALIZATION);
        PhaseEnforcement.assertPhase(lifecycleState, LifecyclePhase.INITIALIZATION);
        
        lifecycleState.advanceTo(LifecyclePhase.RUNTIME);
        PhaseEnforcement.assertPhase(lifecycleState, LifecyclePhase.RUNTIME);
    }
}
