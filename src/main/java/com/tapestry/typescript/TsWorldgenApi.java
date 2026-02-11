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
     * Registers a hook for block resolution during world generation.
     * 
     * @param handler function that receives (ctx, vanillaBlock) and returns string|null
     * @throws IllegalStateException if called outside TS_READY phase
     */
    public void onResolveBlock(Value handler) {
        // Phase enforcement: only allowed during TS_READY
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_READY);
        
        if (handler == null || !handler.canExecute()) {
            throw new IllegalArgumentException("Hook handler must be an executable function");
        }
        
        // Get the current mod ID from the execution context
        String modId = getCurrentModId();
        
        hookRegistry.registerHook(ON_RESOLVE_BLOCK_HOOK, handler, modId);
        
        LOGGER.info("Registered worldgen.onResolveBlock hook from mod '{}'", modId);
    }
    
    /**
     * Creates the worldgen API object to expose to JavaScript.
     * 
     * @return map containing worldgen API functions
     */
    public Map<String, Object> createApiObject() {
        Map<String, Object> api = new HashMap<>();
        api.put("onResolveBlock", this::onResolveBlock);
        return api;
    }
    
    /**
     * Attempts to get the current mod ID from execution context.
     * This is a simplified approach - in a real implementation,
     * we might need to track this more carefully.
     * 
     * @return current mod ID, or "unknown" if not available
     */
    private String getCurrentModId() {
        // For Phase 2, we'll use a simplified approach
        // In a more complete implementation, we might track this
        // through the execution context or thread-local storage
        return "unknown";
    }
}
