package com.tapestry.gameplay.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages lifecycle state for gameplay API.
 * 
 * Tracks current phase, phase history, and registry freeze status.
 * Enforces valid phase transitions and provides phase validation utilities.
 */
public class LifecycleState {
    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleState.class);
    
    private LifecyclePhase currentPhase;
    private final List<LifecyclePhase> phaseHistory;
    private boolean registriesFrozen;
    
    /**
     * Creates a new lifecycle state.
     */
    public LifecycleState() {
        this.currentPhase = LifecyclePhase.TS_REGISTER;
        this.phaseHistory = new ArrayList<>();
        this.phaseHistory.add(LifecyclePhase.TS_REGISTER);
        this.registriesFrozen = false;
        LOGGER.info("LifecycleState initialized in TS_REGISTER phase");
    }
    
    /**
     * Gets the current phase.
     * 
     * @return the current phase
     */
    public LifecyclePhase getCurrentPhase() {
        return currentPhase;
    }
    
    /**
     * Advances to the next phase with validation.
     * Phases must advance in order: TS_REGISTER -> TRAITS -> COMPOSITION -> INITIALIZATION -> RUNTIME
     * 
     * @param nextPhase the phase to advance to
     * @throws IllegalStateException if the transition is invalid
     */
    public void advanceTo(LifecyclePhase nextPhase) {
        if (nextPhase == currentPhase) {
            LOGGER.warn("Attempted to advance to the same phase: {}", nextPhase);
            return;
        }
        
        if (!isValidTransition(currentPhase, nextPhase)) {
            String errorMsg = String.format(
                "Invalid phase transition: %s -> %s. Phases must advance in order: TS_REGISTER -> TRAITS -> COMPOSITION -> INITIALIZATION -> RUNTIME",
                currentPhase, nextPhase
            );
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        LOGGER.info("Phase transition: {} -> {}", currentPhase, nextPhase);
        this.currentPhase = nextPhase;
        this.phaseHistory.add(nextPhase);
        
        // Freeze registries after COMPOSITION phase completes
        if (nextPhase == LifecyclePhase.INITIALIZATION && !registriesFrozen) {
            freezeRegistries();
            LOGGER.info("Registries frozen after COMPOSITION phase");
        }
    }
    
    /**
     * Validates that a phase transition is allowed.
     * Transitions must follow the exact order defined in LifecyclePhase enum.
     * 
     * @param from the current phase
     * @param to the target phase
     * @return true if the transition is valid
     */
    private boolean isValidTransition(LifecyclePhase from, LifecyclePhase to) {
        // Phases must advance in exact order
        return to.ordinal() == from.ordinal() + 1;
    }
    
    /**
     * Requires that the current phase matches the expected phase.
     * 
     * @param expectedPhase the expected phase
     * @throws IllegalStateException if the current phase is not the expected phase
     */
    public void requirePhase(LifecyclePhase expectedPhase) {
        if (currentPhase != expectedPhase) {
            String errorMsg = String.format(
                "Operation requires phase %s, but current phase is %s",
                expectedPhase, currentPhase
            );
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }
    
    /**
     * Requires that the current phase is one of the allowed phases.
     * 
     * @param allowedPhases the allowed phases
     * @throws IllegalStateException if the current phase is not in the allowed list
     */
    public void requirePhase(LifecyclePhase... allowedPhases) {
        for (LifecyclePhase phase : allowedPhases) {
            if (currentPhase == phase) {
                return;
            }
        }
        
        String errorMsg = String.format(
            "Operation requires one of phases %s, but current phase is %s",
            java.util.Arrays.toString(allowedPhases), currentPhase
        );
        LOGGER.error(errorMsg);
        throw new IllegalStateException(errorMsg);
    }
    
    /**
     * Checks if the current phase is before the given phase.
     * 
     * @param phase the phase to compare against
     * @return true if current phase is before the given phase
     */
    public boolean isBefore(LifecyclePhase phase) {
        return currentPhase.ordinal() < phase.ordinal();
    }
    
    /**
     * Checks if the current phase is after the given phase.
     * 
     * @param phase the phase to compare against
     * @return true if current phase is after the given phase
     */
    public boolean isAfter(LifecyclePhase phase) {
        return currentPhase.ordinal() > phase.ordinal();
    }
    
    /**
     * Checks if the current phase is at or before the given phase.
     * 
     * @param phase the phase to compare against
     * @return true if current phase is at or before the given phase
     */
    public boolean isAtOrBefore(LifecyclePhase phase) {
        return currentPhase.ordinal() <= phase.ordinal();
    }
    
    /**
     * Checks if the current phase is at or after the given phase.
     * 
     * @param phase the phase to compare against
     * @return true if current phase is at or after the given phase
     */
    public boolean isAtOrAfter(LifecyclePhase phase) {
        return currentPhase.ordinal() >= phase.ordinal();
    }
    
    /**
     * Gets the phase history.
     * 
     * @return unmodifiable list of phases
     */
    public List<LifecyclePhase> getPhaseHistory() {
        return Collections.unmodifiableList(phaseHistory);
    }
    
    /**
     * Checks if registries are frozen.
     * 
     * @return true if frozen
     */
    public boolean areRegistriesFrozen() {
        return registriesFrozen;
    }
    
    /**
     * Freezes all registries.
     * This is called automatically after COMPOSITION phase completes.
     */
    public void freezeRegistries() {
        this.registriesFrozen = true;
    }
}
