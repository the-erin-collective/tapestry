package com.tapestry.gameplay.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for enforcing lifecycle phase requirements.
 * 
 * Provides static methods for validating that operations occur during the correct phase.
 * All methods throw IllegalStateException with descriptive error messages on phase violations.
 */
public class PhaseEnforcement {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhaseEnforcement.class);
    
    /**
     * Asserts that the current phase matches the expected phase.
     * 
     * @param lifecycleState the lifecycle state to check
     * @param expectedPhase the expected phase
     * @throws IllegalStateException if the current phase is not the expected phase
     */
    public static void assertPhase(LifecycleState lifecycleState, LifecyclePhase expectedPhase) {
        LifecyclePhase currentPhase = lifecycleState.getCurrentPhase();
        if (currentPhase != expectedPhase) {
            String errorMsg = formatPhaseViolationError(expectedPhase, currentPhase);
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }
    
    /**
     * Asserts that the current phase is one of the allowed phases.
     * 
     * @param lifecycleState the lifecycle state to check
     * @param allowedPhases the allowed phases
     * @throws IllegalStateException if the current phase is not in the allowed list
     */
    public static void assertPhase(LifecycleState lifecycleState, LifecyclePhase... allowedPhases) {
        LifecyclePhase currentPhase = lifecycleState.getCurrentPhase();
        for (LifecyclePhase phase : allowedPhases) {
            if (currentPhase == phase) {
                return;
            }
        }
        
        String errorMsg = formatPhaseViolationError(allowedPhases, currentPhase);
        LOGGER.error(errorMsg);
        throw new IllegalStateException(errorMsg);
    }
    
    /**
     * Formats a phase violation error message with expected and actual phases.
     * 
     * @param expectedPhase the expected phase
     * @param actualPhase the actual phase
     * @return formatted error message
     */
    public static String formatPhaseViolationError(LifecyclePhase expectedPhase, LifecyclePhase actualPhase) {
        return String.format(
            "Phase violation: Expected phase %s, but current phase is %s. " +
            "This operation can only be performed during the %s phase.",
            expectedPhase, actualPhase, expectedPhase
        );
    }
    
    /**
     * Formats a phase violation error message with multiple allowed phases.
     * 
     * @param allowedPhases the allowed phases
     * @param actualPhase the actual phase
     * @return formatted error message
     */
    public static String formatPhaseViolationError(LifecyclePhase[] allowedPhases, LifecyclePhase actualPhase) {
        StringBuilder phasesStr = new StringBuilder();
        for (int i = 0; i < allowedPhases.length; i++) {
            if (i > 0) {
                if (i == allowedPhases.length - 1) {
                    phasesStr.append(" or ");
                } else {
                    phasesStr.append(", ");
                }
            }
            phasesStr.append(allowedPhases[i]);
        }
        
        return String.format(
            "Phase violation: Expected one of phases [%s], but current phase is %s. " +
            "This operation can only be performed during one of these phases.",
            phasesStr.toString(), actualPhase
        );
    }
}
