package com.tapestry.gameplay.entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registry and event handler for entity behavior hooks.
 * 
 * Manages registration and invocation of entity interaction hooks like
 * onFeedAttempt. This system integrates with the gameplay lifecycle to
 * ensure hooks are registered during the appropriate phase.
 */
public class EntityInteractionHookRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityInteractionHookRegistry.class);
    private static EntityInteractionHookRegistry instance;

    // Store feed attempt handlers by entity type
    private final Map<String, List<FeedAttemptHandler>> feedAttemptHandlers = new HashMap<>();
    private boolean registrationAllowed = false;

    /**
     * Gets or creates the singleton instance.
     */
    public static synchronized EntityInteractionHookRegistry getInstance() {
        if (instance == null) {
            instance = new EntityInteractionHookRegistry();
        }
        return instance;
    }

    /**
     * Allows hooks to be registered (during TS_REGISTER phase).
     */
    public void allowRegistration() {
        registrationAllowed = true;
        LOGGER.debug("Entity interaction hook registration allowed");
    }

    /**
     * Disallows hook registration (after TS_REGISTER phase).
     */
    public void disallowRegistration() {
        registrationAllowed = false;
        LOGGER.debug("Entity interaction hook registration disallowed");
    }

    /**
     * Registers a feed attempt handler for a specific entity type.
     * 
     * @param entityType the entity type (e.g., "minecraft:cat")
     * @param handler the handler function
     * @throws IllegalStateException if registration is not allowed
     */
    public void registerFeedAttemptHandler(String entityType, FeedAttemptHandler handler) {
        if (!registrationAllowed) {
            throw new IllegalStateException(
                "Entity interaction hooks can only be registered during TS_REGISTER phase. " +
                "Attempted to register for entity type: " + entityType
            );
        }

        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        feedAttemptHandlers.computeIfAbsent(entityType, k -> new ArrayList<>()).add(handler);
        LOGGER.info("Registered feed attempt handler for entity type: {}", entityType);
    }

    /**
     * Invokes all feed attempt handlers for a given entity type.
     * 
     * @param entityType the entity type
     * @param context the feed attempt context
     */
    public void invokeFeedAttemptHandlers(String entityType, FeedAttemptContext context) {
        List<FeedAttemptHandler> handlers = feedAttemptHandlers.get(entityType);
        if (handlers == null || handlers.isEmpty()) {
            return;
        }

        LOGGER.debug("Invoking {} feed attempt handlers for entity type: {}", handlers.size(), entityType);

        for (FeedAttemptHandler handler : handlers) {
            try {
                handler.handle(context);
                
                // Early exit if handler accepted or rejected
                if (context.getResult() != FeedAttemptContext.FeedResult.UNKNOWN) {
                    break;
                }
            } catch (Exception e) {
                LOGGER.error("Error invoking feed attempt handler for entity type: {}", entityType, e);
            }
        }
    }

    /**
     * Gets all feed attempt handlers for a given entity type.
     * 
     * @param entityType the entity type
     * @return list of handlers, or empty list if none exist
     */
    public List<FeedAttemptHandler> getFeedAttemptHandlers(String entityType) {
        return new ArrayList<>(feedAttemptHandlers.getOrDefault(entityType, new ArrayList<>()));
    }

    /**
     * Clears all registered handlers.
     */
    public void clear() {
        feedAttemptHandlers.clear();
        LOGGER.info("Cleared all entity interaction hook handlers");
    }

    /**
     * Functional interface for feed attempt handlers.
     */
    @FunctionalInterface
    public interface FeedAttemptHandler {
        void handle(FeedAttemptContext context);
    }
}
