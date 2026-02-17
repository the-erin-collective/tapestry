package com.tapestry.extensions;

import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.mod.ModRegistry;
import com.tapestry.mod.ModDescriptor;
import com.tapestry.extensions.LifecycleViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 15: Extension lifecycle manager.
 * 
 * Wraps and constrains existing ModRegistry to enforce
 * formal runtime contracts and state machine validation.
 * 
 * This is infrastructure-only - no domain logic contamination.
 */
public class ExtensionLifecycleManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionLifecycleManager.class);
    
    // Core components
    private final ModRegistry modRegistry;
    private final PhaseController phaseController;
    
    // Per-extension state tracking
    private final Map<String, ExtensionState> extensionStates = new ConcurrentHashMap<>();
    private final Map<String, String> failureReasons = new ConcurrentHashMap<>();
    
    private ExtensionLifecycleManager(ModRegistry modRegistry) {
        this.modRegistry = modRegistry;
        this.phaseController = PhaseController.getInstance();
    }
    
    /**
     * Creates a new lifecycle manager wrapping the given mod registry.
     * 
     * @param modRegistry existing mod registry to wrap
     * @return new lifecycle manager instance
     */
    public static ExtensionLifecycleManager create(ModRegistry modRegistry) {
        return new ExtensionLifecycleManager(modRegistry);
    }
    
    /**
     * Gets the current state of an extension.
     * 
     * @param extensionId the extension identifier
     * @return current state, or DISCOVERED if unknown
     */
    public ExtensionState getExtensionState(String extensionId) {
        return extensionStates.getOrDefault(extensionId, ExtensionState.DISCOVERED);
    }
    
    /**
     * Validates and performs a state transition for an extension.
     * 
     * @param extensionId the extension identifier
     * @param newState the desired new state
     * @throws LifecycleViolationException if transition is invalid
     */
    public void transitionState(String extensionId, ExtensionState newState) throws LifecycleViolationException {
        ExtensionState currentState = getExtensionState(extensionId);
        
        if (!isValidTransition(currentState, newState)) {
            throw new LifecycleViolationException(
                String.format(
                    "Invalid state transition for extension '%s': %s → %s",
                    extensionId, currentState, newState
                )
            );
        }
        
        // Log state transition
        LOGGER.debug(
            "Extension '{}' transitioning from {} to {}",
            extensionId, currentState, newState
        );
        
        extensionStates.put(extensionId, newState);
        
        // Handle special transition logic
        handleTransitionLogic(extensionId, currentState, newState);
    }
    
    /**
     * Checks if a transition between states is valid.
     * 
     * @param from current state
     * @param to desired state
     * @return true if transition is allowed
     */
    private boolean isValidTransition(ExtensionState from, ExtensionState to) {
        // ANY → FAILED is always allowed
        if (to == ExtensionState.FAILED) {
            return true;
        }
        
        // Terminal states cannot transition
        if (from == ExtensionState.FAILED || from == ExtensionState.READY) {
            return false;
        }
        
        // Valid sequential transitions
        return switch (from) {
            case DISCOVERED -> to == ExtensionState.VALIDATED;
            case VALIDATED -> to == ExtensionState.TYPE_INITIALIZED;
            case TYPE_INITIALIZED -> to == ExtensionState.FROZEN;
            case FROZEN -> to == ExtensionState.LOADING;
            case LOADING -> to == ExtensionState.READY;
            default -> false;
        };
    }
    
    /**
     * Handles special logic for specific state transitions.
     * 
     * @param extensionId the extension identifier
     * @param from previous state
     * @param to new state
     */
    private void handleTransitionLogic(String extensionId, ExtensionState from, ExtensionState to) {
        switch (to) {
            case FAILED:
                handleExtensionFailure(extensionId, from);
                break;
                
            case READY:
                LOGGER.info("Extension '{}' successfully loaded and ready", extensionId);
                break;
                
            case LOADING:
                // Verify dependencies are ready before allowing loading
                if (!areDependenciesReady(extensionId)) {
                    try {
                        transitionState(extensionId, ExtensionState.FAILED);
                        failureReasons.put(extensionId, "Dependencies not ready");
                    } catch (LifecycleViolationException e) {
                        LOGGER.error("Failed to transition extension '{}' to FAILED: {}", extensionId, e.getMessage());
                    }
                }
                break;
                
            default:
                // No special handling required
                break;
        }
    }
    
    /**
     * Handles extension failure and propagates to dependents.
     * 
     * @param extensionId the failed extension
     * @param failedState the state when failure occurred
     */
    private void handleExtensionFailure(String extensionId, ExtensionState failedState) {
        LOGGER.error(
            "Extension '{}' failed during {} state",
            extensionId, failedState
        );
        
        // Mark all dependents as failed
        Set<String> dependents = getDependents(extensionId);
        for (String dependent : dependents) {
            try {
                transitionState(dependent, ExtensionState.FAILED);
                failureReasons.put(dependent, 
                    String.format("Dependency '%s' failed", extensionId));
            } catch (LifecycleViolationException e) {
                // This should not happen - FAILED is always allowed
                LOGGER.error("Failed to mark dependent '{}' as failed: {}", 
                    dependent, e.getMessage());
            }
        }
    }
    
    /**
     * Checks if all dependencies of an extension are in READY state.
     * 
     * @param extensionId the extension to check
     * @return true if all dependencies are ready
     */
    private boolean areDependenciesReady(String extensionId) {
        ModDescriptor descriptor = modRegistry.getModDescriptor(extensionId);
        if (descriptor == null || descriptor.dependencies().isEmpty()) {
            return true; // No dependencies to check
        }
        
        for (String dependencyId : descriptor.dependencies()) {
            ExtensionState depState = getExtensionState(dependencyId);
            if (depState != ExtensionState.READY) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets all extensions that depend on the given extension.
     * 
     * @param extensionId the extension to find dependents for
     * @return set of dependent extension IDs
     */
    private Set<String> getDependents(String extensionId) {
        Set<String> dependents = new HashSet<>();
        
        for (ModDescriptor descriptor : modRegistry.getAllModDescriptors()) {
            if (descriptor.dependencies().contains(extensionId)) {
                dependents.add(descriptor.getId());
            }
        }
        
        return dependents;
    }
    
    /**
     * Sets the failure reason for an extension.
     * 
     * @param extensionId the extension identifier
     * @param reason the failure reason
     */
    public void setFailureReason(String extensionId, String reason) {
        failureReasons.put(extensionId, reason);
    }
    
    /**
     * Gets the failure reason for an extension.
     * 
     * @param extensionId the extension identifier
     * @return failure reason, or null if not failed
     */
    public String getFailureReason(String extensionId) {
        return failureReasons.get(extensionId);
    }
    
    /**
     * Gets all extensions in a given state.
     * 
     * @param state the state to filter by
     * @return set of extension IDs in the given state
     */
    public Set<String> getExtensionsInState(ExtensionState state) {
        Set<String> result = new HashSet<>();
        
        for (Map.Entry<String, ExtensionState> entry : extensionStates.entrySet()) {
            if (entry.getValue() == state) {
                result.add(entry.getKey());
            }
        }
        
        return result;
    }
    
    /**
     * Initializes all discovered extensions to DISCOVERED state.
     * Should be called during DISCOVERY phase.
     * 
     * @param extensionIds all discovered extension IDs
     */
    public void initializeDiscoveredExtensions(Set<String> extensionIds) {
        LOGGER.info("Initializing {} discovered extensions", extensionIds.size());
        
        for (String extensionId : extensionIds) {
            extensionStates.put(extensionId, ExtensionState.DISCOVERED);
        }
        
        LOGGER.debug("All extensions initialized to DISCOVERED state");
    }
    
    /**
     * Gets diagnostic information about all extensions.
     * For internal host use only - not exposed to extensions.
     * 
     * @return diagnostic summary
     */
    public LifecycleDiagnostics getDiagnostics() {
        Map<ExtensionState, Integer> stateCounts = new HashMap<>();
        
        for (ExtensionState state : ExtensionState.values()) {
            stateCounts.put(state, 0);
        }
        
        for (ExtensionState state : extensionStates.values()) {
            stateCounts.put(state, stateCounts.get(state) + 1);
        }
        
        return new LifecycleDiagnostics(stateCounts, failureReasons);
    }
    
    /**
     * Internal diagnostic data structure.
     * For host internal use only.
     */
    public static class LifecycleDiagnostics {
        private final Map<ExtensionState, Integer> stateCounts;
        private final Map<String, String> failureReasons;
        
        public LifecycleDiagnostics(Map<ExtensionState, Integer> stateCounts, 
                                 Map<String, String> failureReasons) {
            this.stateCounts = new HashMap<>(stateCounts);
            this.failureReasons = new HashMap<>(failureReasons);
        }
        
        public Map<ExtensionState, Integer> getStateCounts() {
            return new HashMap<>(stateCounts);
        }
        
        public Map<String, String> getFailureReasons() {
            return new HashMap<>(failureReasons);
        }
    }
}
