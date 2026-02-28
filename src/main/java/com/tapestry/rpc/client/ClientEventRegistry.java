package com.tapestry.rpc.client;

import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for client-side event handlers for server-pushed events.
 */
public class ClientEventRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEventRegistry.class);
    
    private final Map<String, List<EventHandler>> handlers = new ConcurrentHashMap<>();
    
    /**
     * Registers an event handler for a specific event name.
     */
    public void on(String eventName, EventHandler handler) {
        handlers.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>()).add(handler);
        LOGGER.debug("Registered event handler for: {}", eventName);
    }
    
    /**
     * Removes an event handler.
     */
    public void off(String eventName, EventHandler handler) {
        List<EventHandler> eventHandlers = handlers.get(eventName);
        if (eventHandlers != null) {
            eventHandlers.remove(handler);
            if (eventHandlers.isEmpty()) {
                handlers.remove(eventName);
            }
            LOGGER.debug("Removed event handler for: {}", eventName);
        }
    }
    
    /**
     * Emits an event to all registered handlers.
     */
    public void emit(String eventName, JsonElement payload) {
        List<EventHandler> eventHandlers = handlers.get(eventName);
        if (eventHandlers != null) {
            for (EventHandler handler : eventHandlers) {
                try {
                    handler.handle(payload);
                } catch (Exception e) {
                    LOGGER.error("Error in event handler for: {}", eventName, e);
                    // Don't let one handler break others
                }
            }
        }
    }
    
    /**
     * Removes all event handlers for a specific event name.
     * 
     * @param eventName The event name to unsubscribe from
     * @return true if handlers were removed, false if no handlers were found
     */
    public boolean unsubscribe(String eventName) {
        List<EventHandler> eventHandlers = handlers.remove(eventName);
        boolean removed = eventHandlers != null && !eventHandlers.isEmpty();
        
        if (removed) {
            LOGGER.debug("Unsubscribed all handlers for event: {} (removed {} handlers)", 
                        eventName, eventHandlers.size());
        } else {
            LOGGER.debug("No handlers found to unsubscribe for event: {}", eventName);
        }
        
        return removed;
    }
    
    /**
     * Gets the number of handlers for an event.
     */
    public int getHandlerCount(String eventName) {
        List<EventHandler> eventHandlers = handlers.get(eventName);
        return eventHandlers != null ? eventHandlers.size() : 0;
    }
    
    /**
     * Functional interface for event handlers.
     */
    @FunctionalInterface
    public interface EventHandler {
        void handle(JsonElement payload);
    }
}
