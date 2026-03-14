package com.tapestry.gameplay.entities;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.HashMap;
import java.util.Map;

/**
 * TypeScript API for entity behavior hooks.
 * 
 * Provides tapestry.entities namespace for advanced entity interaction control
 * through behavior hooks like onFeedAttempt.
 */
public class EntitiesApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntitiesApi.class);
    private final EntityInteractionHookRegistry hookRegistry;

    public EntitiesApi() {
        this.hookRegistry = EntityInteractionHookRegistry.getInstance();
    }

    /**
     * Creates the entities namespace for TypeScript.
     *
     * @return ProxyObject containing entity API methods
     */
    public ProxyObject createNamespace() {
        Map<String, Object> entities = new HashMap<>();

        entities.put("onFeedAttempt", createOnFeedAttemptFunction());

        return ProxyObject.fromMap(entities);
    }

    /**
     * Creates the onFeedAttempt function for registering feed attempt hooks.
     * 
     * Usage:
     * ```ts
     * tapestry.entities.onFeedAttempt("minecraft:cat", (ctx) => {
     *   if (ctx.item.id === "mymod:special_item") {
     *     ctx.accept();
     *   }
     * });
     * ```
     */
    private ProxyExecutable createOnFeedAttemptFunction() {
        return args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException(
                    "entities.onFeedAttempt requires exactly 2 arguments: (entityType, handler)"
                );
            }

            String entityType = args[0].asString();
            Value handlerValue = args[1];

            if (handlerValue == null || !handlerValue.canExecute()) {
                throw new IllegalArgumentException("Handler must be a function");
            }

            // Register the handler with the registry
            hookRegistry.registerFeedAttemptHandler(entityType, context -> {
                try {
                    handlerValue.executeVoid(context.toProxy());
                } catch (Exception e) {
                    LOGGER.error("Error executing feed attempt handler for entity: {}", entityType, e);
                }
            });

            LOGGER.info("Registered onFeedAttempt hook for entity type: {}", entityType);
            return null;
        };
    }
}
