package com.tapestry.hooks;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for managing hooks registered by TypeScript mods.
 * 
 * This class provides a generic system for storing and invoking hooks
 * of different types, with deterministic ordering and error handling.
 */
public class HookRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HookRegistry.class);
    
    // Store hooks by hook type name
    private final Map<String, List<Hook<?, ?>>> hooks = new HashMap<>();
    private boolean registrationAllowed = false;
    private int nextRegistrationOrder = 0;
    
    /**
     * Allows hook registration (called during TS_READY phase).
     */
    public void allowRegistration() {
        registrationAllowed = true;
        LOGGER.debug("Hook registration allowed");
    }
    
    /**
     * Disallows hook registration (called after TS_READY phase).
     */
    public void disallowRegistration() {
        registrationAllowed = false;
        LOGGER.debug("Hook registration disallowed");
    }
    
    /**
     * Explicitly freezes the hook registry to prevent any further changes.
     * This is called after TS_READY phase to ensure runtime stability.
     */
    public void freeze() {
        registrationAllowed = false;
        LOGGER.info("Hook registry frozen - no further modifications allowed");
    }
    
    /**
     * Registers a hook for the given hook type.
     * 
     * @param hookType the type of hook (e.g., "worldgen.onResolveBlock")
     * @param callback the JavaScript function to call
     * @param modId the mod ID registering the hook
     * @param source the source file where the hook is being registered
     * @throws IllegalStateException if registration is not allowed
     */
    public void registerHook(String hookType, Value callback, String modId, String source) {
        if (!registrationAllowed) {
            throw new IllegalStateException(
                String.format("Hook registration not allowed in current phase. Tried to register '%s' from mod '%s' in source '%s'", 
                    hookType, modId, source)
            );
        }
        
        if (callback == null || !callback.canExecute()) {
            throw new IllegalArgumentException("Hook callback must be an executable function");
        }
        
        Hook<?, ?> hook = Hook.of(callback, modId, source, nextRegistrationOrder++);
        
        hooks.computeIfAbsent(hookType, k -> new ArrayList<>()).add(hook);
        
        LOGGER.info("Registered hook '{}' from mod '{}' (source: {}, order: {})", 
            hookType, modId, source, hook.registrationOrder());
    }
    
    /**
     * Invokes all hooks of the given type with the provided context.
     * 
     * @param <TContext> the context type
     * @param <TResult> the result type
     * @param hookType the type of hook to invoke
     * @param context the context object to pass to hooks (should contain modId)
     * @param vanillaBlock the vanilla block value to pass as second argument
     * @return the first non-null result, or null if all hooks return null
     * @throws RuntimeException if any hook throws an exception
     */
    @SuppressWarnings("unchecked")
    public <TContext, TResult> TResult invokeHooks(
            String hookType, 
            TContext context,
            Object vanillaBlock
    ) {
        List<Hook<?, ?>> hookList = hooks.get(hookType);
        if (hookList == null || hookList.isEmpty()) {
            return null;
        }
        
        // Hooks are already in registration order due to sequential registration
        for (Hook<?, ?> hook : hookList) {
            try {
                // Execute hook with proper signature: handler(ctx, vanillaBlock)
                // The context object should contain the modId
                Object result = hook.callback().execute(context, vanillaBlock);
                
                if (result != null) {
                    LOGGER.debug("Hook '{}' from mod '{}' (source: {}) returned non-null result", 
                        hookType, hook.modId(), hook.source());
                    return (TResult) result;
                }
                
            } catch (Exception e) {
                LOGGER.error("Hook '{}' from mod '{}' (source: {}) threw an exception", 
                    hookType, hook.modId(), hook.source(), e);
                throw new RuntimeException(
                    String.format("Hook '%s' from mod '%s' (source: %s) failed", 
                        hookType, hook.modId(), hook.source()), e
                );
            }
        }
        
        return null;
    }
    
    /**
     * Gets the number of registered hooks for a given type.
     * 
     * @param hookType the hook type
     * @return the number of hooks
     */
    public int getHookCount(String hookType) {
        List<Hook<?, ?>> hookList = hooks.get(hookType);
        return hookList != null ? hookList.size() : 0;
    }
    
    /**
     * Gets all registered hook types.
     * 
     * @return set of hook type names
     */
    public java.util.Set<String> getHookTypes() {
        return Collections.unmodifiableSet(hooks.keySet());
    }
    
    /**
     * Gets unmodifiable list of hooks for a given type.
     * 
     * @param <TContext> the context type
     * @param <TResult> the result type
     * @param hookType the hook type
     * @return list of hooks in registration order
     */
    @SuppressWarnings("unchecked")
    public <TContext, TResult> List<Hook<TContext, TResult>> getHooks(String hookType) {
        List<Hook<?, ?>> hookList = hooks.get(hookType);
        if (hookList == null) {
            return Collections.emptyList();
        }
        
        return Collections.unmodifiableList((List<Hook<TContext, TResult>>) (List<?>) hookList);
    }
    
    /**
     * Clears all hooks (for testing purposes).
     */
    public void clear() {
        hooks.clear();
        registrationAllowed = false;
        nextRegistrationOrder = 0;
        LOGGER.debug("Hook registry cleared");
    }
}
