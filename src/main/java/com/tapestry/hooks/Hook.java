package com.tapestry.hooks;

import org.graalvm.polyglot.Value;

/**
 * Generic hook interface for TypeScript mod hook registration.
 * 
 * This interface represents a registered hook callback that can be invoked
 * with a context and return a result.
 * 
 * @param <TContext> the type of context passed to the hook
 * @param <TResult> the type of result returned by the hook
 */
public interface Hook<TContext, TResult> {
    
    /**
     * The callback function from the TypeScript mod.
     */
    Value callback();
    
    /**
     * The ID of the mod that registered this hook.
     */
    String modId();
    
    /**
     * The registration order (for deterministic invocation).
     */
    int registrationOrder();
    
    /**
     * Creates a new hook instance.
     * 
     * @param callback the JavaScript function to call
     * @param modId the mod ID that registered this hook
     * @param registrationOrder the order of registration
     */
    static <TContext, TResult> Hook<TContext, TResult> of(
            Value callback, 
            String modId, 
            int registrationOrder
    ) {
        return new HookImpl<>(callback, modId, registrationOrder);
    }
    
    /**
     * Implementation of the Hook interface.
     */
    class HookImpl<TContext, TResult> implements Hook<TContext, TResult> {
        private final Value callback;
        private final String modId;
        private final int registrationOrder;
        
        HookImpl(Value callback, String modId, int registrationOrder) {
            this.callback = callback;
            this.modId = modId;
            this.registrationOrder = registrationOrder;
        }
        
        @Override
        public Value callback() {
            return callback;
        }
        
        @Override
        public String modId() {
            return modId;
        }
        
        @Override
        public int registrationOrder() {
            return registrationOrder;
        }
    }
}
