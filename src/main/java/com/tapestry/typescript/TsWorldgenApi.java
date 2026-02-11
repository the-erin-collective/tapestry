package com.tapestry.typescript;

import com.tapestry.hooks.HookRegistry;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * TypeScript API for worldgen hook registration.
 * 
 * This class provides the tapestry.worldgen namespace that TypeScript
 * mods can use to register hooks during TS_READY phase.
 */
public class TsWorldgenApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TsWorldgenApi.class);
    private static final String ON_RESOLVE_BLOCK_HOOK = "worldgen.onResolveBlock";
    
    private final HookRegistry hookRegistry;
    
    public TsWorldgenApi(HookRegistry hookRegistry) {
        this.hookRegistry = hookRegistry;
    }
    
    /**
     * Registers a hook for worldgen.onResolveBlock.
     * 
     * @param handler the JavaScript function to call
     * @throws IllegalStateException if not in TS_READY phase
     * @throws IllegalArgumentException if handler is null or not executable
     */
    public void onResolveBlock(Object handler) {
        // Phase enforcement: only allowed during TS_READY
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_READY);
        
        if (handler == null) {
            throw new IllegalArgumentException("Hook handler cannot be null");
        }
        
        // Get the current mod ID from execution context
        String modId = TypeScriptRuntime.getCurrentModId();
        if (modId == null) {
            throw new IllegalStateException("No mod ID set in current context");
        }
        
        // Convert handler to Value for registration
        Value handlerValue = Value.asValue(handler);
        
        // Verify it's executable
        if (!handlerValue.canExecute()) {
            throw new IllegalArgumentException("Hook handler must be an executable function");
        }
        
        // Register the hook with the proper signature
        hookRegistry.registerHook("worldgen.onResolveBlock", handlerValue, modId);
        
        LOGGER.info("Registered worldgen.onResolveBlock hook from mod '{}'", modId);
    }
    
    /**
     * Creates the worldgen API object to expose to JavaScript.
     * 
     * @return map containing worldgen API functions
     */
    public Map<String, Object> createApiObject() {
        Map<String, Object> api = new HashMap<>();
        api.put("onResolveBlock", new Object() {
            @SuppressWarnings("unused")
            public Object onResolveBlock(Object handler) {
                try {
                    TsWorldgenApi.this.onResolveBlock(handler);
                } catch (Exception e) {
                    throw new RuntimeException("Hook registration failed", e);
                }
                return null;
            }
        });
        return api;
    }
}
