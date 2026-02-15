package com.tapestry.events;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global synchronous EventBus for Phase 11.
 * 
 * Provides deterministic, lifecycle-safe event dispatch with namespace enforcement.
 * No async, no priority, no wildcards - simple and reliable.
 */
public class EventBus {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBus.class);
    
    // Performance limits from Phase 11 spec
    private static final int MAX_LISTENERS_PER_EVENT = 500;
    private static final int MAX_TOTAL_LISTENERS = 5000;
    private static final int WARN_DISPATCH_DEPTH = 50;
    
    // Event storage: eventName -> set of listeners
    private final Map<String, Set<Listener>> eventListeners = new ConcurrentHashMap<>();
    
    // Mod tracking: modId -> set of subscribed event names (for lifecycle cleanup)
    private final Map<String, Set<String>> modSubscriptions = new ConcurrentHashMap<>();
    
    // Dispatch tracking for recursion warning
    private final ThreadLocal<Integer> dispatchDepth = ThreadLocal.withInitial(() -> 0);
    
    /**
     * Represents an event listener.
     */
    private static class Listener {
        final String modId;
        final Value handler;
        
        Listener(String modId, Value handler) {
            this.modId = modId;
            this.handler = handler;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Listener other = (Listener) obj;
            return Objects.equals(handler, other.handler) && Objects.equals(modId, other.modId);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(handler, modId);
        }
    }
    
    /**
     * Subscribes to an event with namespace validation.
     * 
     * @param modId the mod ID subscribing
     * @param eventName the event name to listen for
     * @param handler the event handler function
     * @throws IllegalArgumentException if namespace rules are violated
     */
    public void subscribe(String modId, String eventName, Value handler) {
        validateNamespace(modId, eventName, "subscribe");
        
        Listener listener = new Listener(modId, handler);
        
        // Add to event listeners
        eventListeners.computeIfAbsent(eventName, k -> ConcurrentHashMap.newKeySet()).add(listener);
        
        // Track mod subscription
        modSubscriptions.computeIfAbsent(modId, k -> ConcurrentHashMap.newKeySet()).add(eventName);
        
        // Check performance limits
        checkPerformanceLimits(eventName);
        
        LOGGER.debug("Mod '{}' subscribed to event '{}'", modId, eventName);
    }
    
    /**
     * Unsubscribes a specific handler from an event.
     * 
     * @param modId the mod ID unsubscribing
     * @param eventName the event name
     * @param handler the handler to remove
     */
    public void unsubscribe(String modId, String eventName, Value handler) {
        Set<Listener> listeners = eventListeners.get(eventName);
        if (listeners != null) {
            Listener listenerToRemove = new Listener(modId, handler);
            listeners.remove(listenerToRemove);
            
            if (listeners.isEmpty()) {
                eventListeners.remove(eventName);
            }
        }
        
        LOGGER.debug("Mod '{}' unsubscribed from event '{}'", modId, eventName);
    }
    
    /**
     * Emits an event with namespace validation.
     * 
     * @param emitterModId the mod ID emitting the event
     * @param eventName the event name to emit
     * @param payload the event payload data
     * @throws IllegalArgumentException if namespace rules are violated
     */
    public void emit(String emitterModId, String eventName, Object payload) {
        validateNamespace(emitterModId, eventName, "emit");
        
        Set<Listener> listeners = eventListeners.get(eventName);
        if (listeners == null || listeners.isEmpty()) {
            // No listeners - this is valid per spec (allows future-proof decoupling)
            return;
        }
        
        // Create event object
        TapestryEvent event = new TapestryEvent(eventName, extractNamespace(eventName), payload, emitterModId);
        
        // Increment dispatch depth for recursion warning
        int currentDepth = dispatchDepth.get();
        int newDepth = currentDepth + 1;
        dispatchDepth.set(newDepth);
        
        // Log recursion warning when entering deep recursion (once per chain)
        if (newDepth == WARN_DISPATCH_DEPTH + 1) {
            LOGGER.warn("Deep event dispatch detected: depth {} for event '{}'", 
                       newDepth, eventName);
        }
        
        try {
            // Snapshot listeners to prevent ConcurrentModificationException
            // This preserves determinism even if listeners modify the set during dispatch
            List<Listener> listenersSnapshot = new ArrayList<>(listeners);
            
            // Execute listeners in registration order
            for (Listener listener : listenersSnapshot) {
                try {
                    listener.handler.executeVoid(event);
                } catch (Exception e) {
                    LOGGER.error("Event handler error for mod '{}' on event '{}': {}", 
                               listener.modId, eventName, e.getMessage(), e);
                    // Continue dispatching remaining listeners
                }
            }
        } finally {
            // Decrement dispatch depth
            dispatchDepth.set(currentDepth);
        }
        
        LOGGER.debug("Event '{}' emitted by '{}' to {} listeners", eventName, emitterModId, listeners.size());
    }
    
    /**
     * Removes all listeners for a specific mod (lifecycle cleanup).
     * 
     * @param modId the mod ID to clean up
     */
    public void removeAllListenersForMod(String modId) {
        // Remove from all event listeners
        for (Map.Entry<String, Set<Listener>> entry : eventListeners.entrySet()) {
            Set<Listener> listeners = entry.getValue();
            listeners.removeIf(listener -> listener.modId.equals(modId));
            
            // Clean up empty event sets
            if (listeners.isEmpty()) {
                eventListeners.remove(entry.getKey());
            }
        }
        
        // Clear mod subscription tracking
        modSubscriptions.remove(modId);
        
        LOGGER.debug("Removed all event listeners for mod '{}'", modId);
    }
    
    /**
     * Validates namespace rules for subscribe/emit operations.
     * 
     * @param modId the mod ID performing the operation
     * @param eventName the event name
     * @param operation the operation (subscribe/emit) for error messages
     * @throws IllegalArgumentException if namespace rules are violated
     */
    private void validateNamespace(String modId, String eventName, String operation) {
        String namespace = extractNamespace(eventName);
        
        switch (namespace) {
            case "engine":
                if ("emit".equals(operation)) {
                    // Mods cannot emit engine events, only platform can
                    throw new IllegalArgumentException(
                        String.format("Mod '%s' cannot emit engine events (attempted: %s)", modId, eventName));
                }
                // Mods can subscribe to engine events - this is allowed
                break;
                
            case "ui":
                if ("emit".equals(operation)) {
                    // Mods should not emit UI events directly
                    LOGGER.warn("Mod '{}' emitting UI event '{}' - This should be reserved for UI system", modId, eventName);
                }
                break;
                
            default:
                if (namespace.startsWith("mod:")) {
                    String targetModId = namespace.substring(4); // Remove "mod:" prefix
                    if (!targetModId.equals(modId) && "emit".equals(operation)) {
                        throw new IllegalArgumentException(
                            String.format("Mod '%s' cannot emit events for mod '%s' (attempted: %s)", modId, targetModId, eventName));
                    }
                }
                break;
        }
    }
    
    /**
     * Extracts namespace from event name.
     * 
     * @param eventName the full event name
     * @return the namespace part (before first colon) or the full name if no colon
     */
    private String extractNamespace(String eventName) {
        int colonIndex = eventName.indexOf(':');
        return colonIndex > 0 ? eventName.substring(0, colonIndex) : eventName;
    }
    
    /**
     * Checks performance limits and logs warnings if exceeded.
     * 
     * @param eventName the event name to check
     */
    private void checkPerformanceLimits(String eventName) {
        Set<Listener> listeners = eventListeners.get(eventName);
        if (listeners != null) {
            if (listeners.size() > MAX_LISTENERS_PER_EVENT) {
                LOGGER.warn("Event '{}' has {} listeners (exceeds recommended limit of {})", 
                           eventName, listeners.size(), MAX_LISTENERS_PER_EVENT);
            }
        }
        
        int totalListeners = eventListeners.values().stream().mapToInt(Set::size).sum();
        if (totalListeners > MAX_TOTAL_LISTENERS) {
            LOGGER.warn("Total event listeners: {} (exceeds recommended limit of {})", 
                       totalListeners, MAX_TOTAL_LISTENERS);
        }
    }
    
    /**
     * Gets the current dispatch depth for Phase 12 compatibility.
     * 
     * @return current dispatch depth (0 if not in dispatch)
     */
    public int getDispatchDepth() {
        return dispatchDepth.get();
    }
    
    /**
     * Gets performance statistics for monitoring.
     * 
     * @return performance statistics
     */
    public EventStats getStats() {
        int totalListeners = eventListeners.values().stream().mapToInt(Set::size).sum();
        int totalEvents = eventListeners.size();
        
        return new EventStats(totalListeners, totalEvents, modSubscriptions.size());
    }
    
    /**
     * Performance statistics for the event system.
     */
    public record EventStats(int totalListeners, int totalEvents, int activeMods) {}
}
