package com.tapestry.typescript;

import com.tapestry.hooks.HookRegistry;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final TsModRegistry modRegistry;
    
    public TsWorldgenApi(HookRegistry hookRegistry, TsModRegistry modRegistry) {
        this.hookRegistry = hookRegistry;
        this.modRegistry = modRegistry;
    }
    
    /**
     * Registers a hook for worldgen.onResolveBlock.
     * 
     * @param handler JavaScript Value function to call
     * @throws IllegalStateException if not in TS_READY phase or not during onLoad execution
     * @throws IllegalArgumentException if handler is null or not executable
     */
    public void onResolveBlock(Value handler) {
        // Phase enforcement: only allowed during TS_READY
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_READY);
        
        if (handler == null || handler.isNull()) {
            throw new IllegalArgumentException("Hook handler cannot be null");
        }
        
        // Verify it's executable
        if (!handler.canExecute()) {
            throw new IllegalArgumentException("Hook handler must be an executable function");
        }
        
        // Get the current mod ID from execution context
        String modId = TypeScriptRuntime.getCurrentModId();
        if (modId == null) {
            throw new IllegalStateException(
                "Hooks may only be registered during onLoad() execution - no mod ID context found"
            );
        }
        
        // Get the source file from the mod registry for better error reporting
        String source = "unknown";
        if (modRegistry != null) {
            var mod = modRegistry.getMod(modId);
            if (mod != null) {
                source = mod.source();
            }
        }
        
        // Additional guard: ensure we're actually inside onLoad execution
        // This prevents async or delayed registration
        String currentSource = TypeScriptRuntime.getCurrentSource();
        if (currentSource != null) {
            throw new IllegalStateException(
                String.format("Hooks may only be registered during onLoad() execution, not during script evaluation. " +
                    "Current source: %s, Mod: %s", currentSource, modId)
            );
        }
        
        // Register hook with the proper signature
        hookRegistry.registerHook("worldgen.onResolveBlock", handler, modId, source);
        
        LOGGER.info("Registered worldgen.onResolveBlock hook from mod '{}'", modId);
    }
}
