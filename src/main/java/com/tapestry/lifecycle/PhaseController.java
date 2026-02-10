package com.tapestry.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global phase controller that manages Tapestry's lifecycle state.
 * 
 * This is the single source of truth for the current phase.
 * All phase transitions must go through this controller.
 */
public class PhaseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhaseController.class);
    
    private static final PhaseController INSTANCE = new PhaseController();
    
    private final AtomicReference<TapestryPhase> currentPhase;
    private volatile Instant phaseTransitionTime;
    
    private PhaseController() {
        this.currentPhase = new AtomicReference<>(TapestryPhase.BOOTSTRAP);
        this.phaseTransitionTime = Instant.now();
        LOGGER.info("PhaseController initialized in BOOTSTRAP phase");
    }
    
    /**
     * Gets the singleton instance of the PhaseController.
     * 
     * @return the global phase controller
     */
    public static PhaseController getInstance() {
        return INSTANCE;
    }
    
    /**
     * Gets the current phase.
     * 
     * @return the current TapestryPhase
     */
    public TapestryPhase getCurrentPhase() {
        return currentPhase.get();
    }
    
    /**
     * Advances to the next phase if the transition is valid.
     * 
     * @param nextPhase the phase to advance to
     * @throws IllegalStateException if the transition is invalid
     */
    public void advanceTo(TapestryPhase nextPhase) {
        TapestryPhase current = getCurrentPhase();
        
        if (nextPhase == current) {
            LOGGER.warn("Attempted to advance to the same phase: {}", nextPhase);
            return;
        }
        
        if (!isValidTransition(current, nextPhase)) {
            String errorMsg = String.format(
                "Invalid phase transition: %s -> %s. Phases must advance monotonically.",
                current, nextPhase
            );
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        if (currentPhase.compareAndSet(current, nextPhase)) {
            phaseTransitionTime = Instant.now();
            LOGGER.info("Phase transition: {} -> {} at {}", current, nextPhase, phaseTransitionTime);
        } else {
            // CAS failed, meaning another thread changed the phase
            throw new IllegalStateException(
                String.format("Concurrent phase modification detected. Expected %s, but was %s",
                    current, getCurrentPhase())
            );
        }
    }
    
    /**
     * Requires that the current phase matches the expected phase.
     * 
     * @param expectedPhase the expected phase
     * @throws IllegalStateException if the current phase is not the expected phase
     */
    public void requirePhase(TapestryPhase expectedPhase) {
        TapestryPhase current = getCurrentPhase();
        if (current != expectedPhase) {
            String errorMsg = String.format(
                "Operation requires phase %s, but current phase is %s",
                expectedPhase, current
            );
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }
    
    /**
     * Requires that the current phase is at least the given phase.
     * 
     * @param minimumPhase the minimum required phase
     * @throws IllegalStateException if the current phase is before the required phase
     */
    public void requireAtLeast(TapestryPhase minimumPhase) {
        TapestryPhase current = getCurrentPhase();
        if (current.isBefore(minimumPhase)) {
            String errorMsg = String.format(
                "Operation requires phase %s or later, but current phase is %s",
                minimumPhase, current
            );
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }
    
    /**
     * Requires that the current phase is at most the given phase.
     * 
     * @param maximumPhase the maximum allowed phase
     * @throws IllegalStateException if the current phase is after the allowed phase
     */
    public void requireAtMost(TapestryPhase maximumPhase) {
        TapestryPhase current = getCurrentPhase();
        if (current.isAfter(maximumPhase)) {
            String errorMsg = String.format(
                "Operation requires phase %s or earlier, but current phase is %s",
                maximumPhase, current
            );
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }
    
    /**
     * Checks if a phase transition is valid.
     * Valid transitions must advance monotonically (forward only).
     * 
     * @param from the current phase
     * @param to the target phase
     * @return true if the transition is valid
     */
    private boolean isValidTransition(TapestryPhase from, TapestryPhase to) {
        return to.isAfter(from);
    }
    
    /**
     * Gets the time of the last phase transition.
     * 
     * @return the timestamp of the last transition
     */
    public Instant getPhaseTransitionTime() {
        return phaseTransitionTime;
    }
    
    /**
     * Checks if the current phase is one of the specified phases.
     * 
     * @param phases the phases to check against
     * @return true if the current phase is in the list
     */
    public boolean isPhase(TapestryPhase... phases) {
        TapestryPhase current = getCurrentPhase();
        for (TapestryPhase phase : phases) {
            if (current == phase) {
                return true;
            }
        }
        return false;
    }
}
