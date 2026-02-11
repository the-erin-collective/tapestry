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
    public void onResolveBlock(Object handler) {
        // Phase enforcement: only allowed during TS_READY
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_READY);
        
        if (handler == null) {
            throw new IllegalArgumentException("Hook handler cannot be null");
        }
        
        // Get the current mod ID from the execution context
        String modId = getCurrentModId();
        
        // For now, we'll just log the registration
        // In a real implementation, we'd convert the handler to a Value and register it
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
            public void onResolveBlock(Object handler) {
                try {
                    TsWorldgenApi.this.onResolveBlock(handler);
                } catch (Exception e) {
                    throw new RuntimeException("Hook registration failed", e);
                }
            }
        });
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
