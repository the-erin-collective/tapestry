package com.tapestry.events;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Event bus for TypeScript mod event registration and dispatch.
 * 
 * Provides deterministic event handling with fail-fast error handling
 * and strict phase enforcement.
 */
public class EventBus {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBus.class);
    
    // Maps event names to lists of handlers
    private final Map<String, List<EventHandler>> handlers = new ConcurrentHashMap<>();
    private final AtomicLong nextRegistrationOrder = new AtomicLong(0);
    private boolean registrationAllowed = true;
    
    /**
     * Registers an event handler for the given event name.
     * 
     * @param eventName the event name (e.g., "playerJoin")
     * @param callback JavaScript function to call
     * @param modId mod ID registering the handler
     * @param source source file where handler is registered
     */
    public void on(String eventName, Value callback, String modId, String source) {
        if (!registrationAllowed) {
            throw new IllegalStateException(
                String.format("Event registration not allowed after RUNTIME phase. " +
                    "Tried to register '%s' from mod '%s' in source '%s'", 
                    eventName, modId, source)
            );
        }
        
        if (callback == null || !callback.canExecute()) {
            throw new IllegalArgumentException("Event callback must be an executable function");
        }
        
        if (eventName == null || eventName.isBlank()) {
            throw new IllegalArgumentException("Event name cannot be null or empty");
        }
        
        EventHandler handler = new EventHandler(
            callback, modId, source, (int) nextRegistrationOrder.incrementAndGet()
        );
        
        handlers.computeIfAbsent(eventName, k -> new ArrayList<>()).add(handler);
        
        LOGGER.info("Registered event handler for '{}' from mod '{}' (source: {}, order: {})", 
            eventName, modId, source, handler.registrationOrder());
    }
    
    /**
     * Emits an event to all registered handlers.
     * 
     * @param eventName the event name
     * @param context the event context object (read-only)
     */
    public void emit(String eventName, Map<String, Object> context) {
        List<EventHandler> eventHandlers = handlers.get(eventName);
        if (eventHandlers == null || eventHandlers.isEmpty()) {
            return;
        }
        
        // Sort for deterministic execution: modId ascending, then registration order
        List<EventHandler> sortedHandlers = new ArrayList<>(eventHandlers);
        sortedHandlers.sort(Comparator
            .comparing(EventHandler::modId)
            .thenComparing(EventHandler::registrationOrder));
        
        for (EventHandler handler : sortedHandlers) {
            try {
                // Execute handler with context
                handler.callback().executeVoid(context);
                
                LOGGER.debug("Executed event handler for '{}' from mod '{}' (source: {})", 
                    eventName, handler.modId(), handler.source());
                
            } catch (Exception e) {
                LOGGER.error("Event handler for '{}' from mod '{}' (source: {}) threw an exception", 
                    eventName, handler.modId(), handler.source(), e);
                throw new RuntimeException(
                    String.format("Event handler for '%s' from mod '%s' (source: %s) failed", 
                        eventName, handler.modId(), handler.source()), e
                );
            }
        }
    }
    
    /**
     * Gets the number of handlers for an event.
     * 
     * @param eventName the event name
     * @return number of registered handlers
     */
    public int getHandlerCount(String eventName) {
        List<EventHandler> eventHandlers = handlers.get(eventName);
        return eventHandlers != null ? eventHandlers.size() : 0;
    }
    
    /**
     * Gets all registered event names.
     * 
     * @return set of event names
     */
    public Set<String> getEventNames() {
        return Collections.unmodifiableSet(handlers.keySet());
    }
    
    /**
     * Disallows further event registration (called when RUNTIME phase begins).
     */
    public void disallowRegistration() {
        registrationAllowed = false;
        LOGGER.info("Event registration disallowed - runtime phase active");
    }
    
    /**
     * Clears all event handlers (for testing/shutdown).
     */
    public void clear() {
        handlers.clear();
        registrationAllowed = true;
        nextRegistrationOrder.set(0);
        LOGGER.debug("Event bus cleared");
    }
    
    /**
     * Represents an event handler.
     */
    private static class EventHandler {
        private final Value callback;
        private final String modId;
        private final String source;
        private final int registrationOrder;
        
        EventHandler(Value callback, String modId, String source, int registrationOrder) {
            this.callback = callback;
            this.modId = modId;
            this.source = source;
            this.registrationOrder = registrationOrder;
        }
        
        Value callback() { return callback; }
        String modId() { return modId; }
        String source() { return source; }
        int registrationOrder() { return registrationOrder; }
    }
}
