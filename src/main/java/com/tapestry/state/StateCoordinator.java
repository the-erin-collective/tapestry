package com.tapestry.state;

import com.tapestry.events.EventBus;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 12 State Coordinator.
 * 
 * Manages transactional state change batching tied to EventBus dispatch depth.
 * Ensures deterministic ordering of state change emissions.
 */
public class StateCoordinator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StateCoordinator.class);
    
    // Performance limits
    private static final int MAX_PENDING_CHANGES = 1000;
    private static final String STATE_CHANGE_EVENT = "__state_change__";
    
    // Dispatch tracking
    private int dispatchDepth = 0;
    
    // Pending state changes queue
    private final List<StateChangeEvent<?>> pendingStateChanges = new ArrayList<>();
    
    // State subscriptions: stateName -> list of handlers
    private final Map<String, List<Value>> stateSubscriptions = new ConcurrentHashMap<>();
    
    private final EventBus eventBus;
    
    /**
     * Creates a new StateCoordinator.
     * 
     * @param eventBus the EventBus for emitting state change events
     */
    public StateCoordinator(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    /**
     * Gets the current dispatch depth.
     * Used by EventBus to coordinate state change flushing.
     * 
     * @return current dispatch depth (0 if not in dispatch)
     */
    public int getDispatchDepth() {
        return dispatchDepth;
    }
    
    /**
     * Called when event dispatch starts.
     * Increments dispatch depth.
     */
    public void onDispatchStart() {
        dispatchDepth++;
    }
    
    /**
     * Called when event dispatch completes.
     * Decrements dispatch depth and flushes if at outermost level.
     */
    public void onDispatchEnd() {
        dispatchDepth--;
        if (dispatchDepth == 0) {
            flush();
        }
    }
    
    /**
     * Queues a state change for deferred emission.
     * 
     * @param state the State instance that changed
     * @param oldValue the previous value
     * @param newValue the new value
     */
    public void queueChange(State<?> state, Object oldValue, Object newValue) {
        pendingStateChanges.add(new StateChangeEvent<>(state.getStateName(), oldValue, newValue));
        
        // Warn about excessive state churn
        if (pendingStateChanges.size() > MAX_PENDING_CHANGES) {
            LOGGER.warn("High state change activity: {} pending changes for state '{}' (threshold: {})", 
                       pendingStateChanges.size(), state.getStateName(), MAX_PENDING_CHANGES);
        }
    }
    
    /**
     * Subscribes to state change events for a specific state name.
     * 
     * @param stateName name of the state to watch
     * @param handler event handler function
     * @return unsubscribe function
     */
    public ProxyObject subscribeToState(String stateName, Value handler) {
        if (!handler.canExecute()) {
            throw new IllegalArgumentException("Handler must be an executable function");
        }
        
        // Add to subscription map
        stateSubscriptions.computeIfAbsent(stateName, k -> new ArrayList<>()).add(handler);
        
        // Return unsubscribe function
        return ProxyObject.fromMap(Map.of("unsubscribe", (ProxyExecutable) args -> {
            if (args.length != 0) {
                throw new IllegalArgumentException("Unsubscribe requires no arguments");
            }
            
            // Remove from subscription map
            List<Value> handlers = stateSubscriptions.get(stateName);
            if (handlers != null) {
                handlers.remove(handler);
                if (handlers.isEmpty()) {
                    stateSubscriptions.remove(stateName);
                }
            }
            
            return null;
        }));
    }
    
    /**
     * Flushes all pending state changes in order.
     * Called only when dispatch depth returns to 0.
     */
    private void flush() {
        if (pendingStateChanges.isEmpty()) {
            return;
        }
        
        int changeCount = pendingStateChanges.size();
        
        try {
            // Emit all pending state changes as immediate events
            for (StateChangeEvent<?> change : pendingStateChanges) {
                try {
                    // Emit through EventBus with internal event name
                    eventBus.emit(null, STATE_CHANGE_EVENT, change);
                } catch (Exception e) {
                    LOGGER.error("Error emitting state change event for state '{}': {}", 
                               change.getStateName(), e.getMessage(), e);
                    // Continue with remaining changes
                }
            }
            
            // Clear the queue
            pendingStateChanges.clear();
            
            if (changeCount > 0) {
                LOGGER.debug("Flushed {} state changes", changeCount);
            }
        } catch (Exception e) {
            LOGGER.error("Critical error during state change flush: {}", e.getMessage(), e);
            // Clear queue even on error to prevent infinite loops
            pendingStateChanges.clear();
        }
    }
    
    /**
     * Gets performance statistics for debugging.
     * 
     * @return performance statistics
     */
    public StateStats getStats() {
        int totalSubscriptions = stateSubscriptions.values().stream().mapToInt(List::size).sum();
        int pendingChanges = pendingStateChanges.size();
        int stateCount = stateSubscriptions.size();
        
        return new StateStats(totalSubscriptions, pendingChanges, stateCount);
    }
    
    /**
     * Performance statistics for the state system.
     */
    public record StateStats(int totalSubscriptions, int pendingChanges, int stateCount) {}
}
