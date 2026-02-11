package com.tapestry;

import com.tapestry.api.TapestryAPI;
import com.tapestry.extension.ExtensionRegistry;
import com.tapestry.hooks.HookRegistry;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.typescript.TsModDiscovery;
import com.tapestry.typescript.TsModRegistry;
import com.tapestry.typescript.TypeScriptRuntime;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Main Tapestry mod implementing Fabric's ModInitializer.
 * 
 * This class orchestrates the complete Phase 2 lifecycle including
 * TypeScript mod discovery, loading, and hook registration.
 */
public class TapestryMod implements ModInitializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryMod.class);
    
    private static TapestryAPI api;
    private static ExtensionRegistry extensionRegistry;
    private static TsModRegistry modRegistry;
    private static HookRegistry hookRegistry;
    private static TypeScriptRuntime tsRuntime;
    private static TsModDiscovery modDiscovery;
    
    @Override
    public void onInitialize() {
        LOGGER.info("Starting Tapestry Phase 2 initialization");
        
        try {
            // Phase 1: Bootstrap framework
            bootstrapFramework();
            
            // Phase 2: TypeScript mod loading
            loadTypeScriptMods();
            
            LOGGER.info("Tapestry Phase 2 initialization completed successfully");
            
        } catch (Exception e) {
            LOGGER.error("Tapestry initialization failed", e);
            throw new RuntimeException("Failed to initialize Tapestry", e);
        }
    }
    
    /**
     * Phase 1: Bootstrap the core framework.
     */
    private static void bootstrapFramework() {
        LOGGER.info("=== BOOTSTRAP PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.BOOTSTRAP);
        
        // Initialize core components
        api = new TapestryAPI();
        extensionRegistry = new ExtensionRegistry(api);
        modRegistry = new TsModRegistry();
        hookRegistry = new HookRegistry();
        tsRuntime = new TypeScriptRuntime();
        modDiscovery = new TsModDiscovery();
        
        LOGGER.info("Core framework components initialized");
        
        // DISCOVERY: Discover Java extensions
        LOGGER.info("=== DISCOVERY PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        extensionRegistry.discoverExtensions();
        
        // REGISTRATION: Register Java extensions
        LOGGER.info("=== REGISTRATION PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        extensionRegistry.registerExtensions();
        
        // FREEZE: Lock API surface
        LOGGER.info("=== FREEZE PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        api.freeze();
        
        LOGGER.info("Framework bootstrap complete");
        LOGGER.info("API surface frozen - no further modifications allowed");
        
        // Advance to TS_LOAD
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        LOGGER.info("Advanced to TS_LOAD phase");
    }
    
    /**
     * Executes the TS_LOAD phase.
     * Initializes the TypeScript runtime with the frozen API.
     */
    private void executeTSLoadPhase() {
        LOGGER.info("=== TS_LOAD PHASE ===");
        
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_LOAD);
        
        // Initialize TypeScript runtime
        typeScriptRuntime.initialize(api);
        
        LOGGER.info("TypeScript runtime initialized with frozen API");
        
        // Advance to TS_READY
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_READY);
        LOGGER.info("Advanced to TS_READY phase");
    }
    
    /**
     * Executes the TS_READY phase.
     * In Phase 1, this is a no-op since we don't execute user TS code yet.
     */
    private void executeTSReadyPhase() {
        LOGGER.info("=== TS_READY PHASE ===");
        
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_READY);
        
        // In Phase 1, we don't execute any user TS code
        // This phase exists to establish the lifecycle pattern
        LOGGER.info("TS_READY phase completed (no user code execution in Phase 1)");
        
        // Note: We don't advance to RUNTIME in Phase 1
        // RUNTIME will be reached when the server actually starts
        LOGGER.info("Phase 1 complete - waiting for server start to reach RUNTIME");
    }
    
    /**
     * Gets the TapestryAPI instance.
     * This is primarily for testing and internal use.
     * 
     * @return the TapestryAPI instance
     */
    public TapestryAPI getAPI() {
        return api;
    }
    
    /**
     * Gets the ExtensionRegistry instance.
     * This is primarily for testing and internal use.
     * 
     * @return the ExtensionRegistry instance
     */
    public ExtensionRegistry getExtensionRegistry() {
        return extensionRegistry;
    }
    
    /**
     * Gets the TypeScriptRuntime instance.
     * This is primarily for testing and internal use.
     * 
     * @return the TypeScriptRuntime instance
     */
    public TypeScriptRuntime getTypeScriptRuntime() {
        return typeScriptRuntime;
    }
    
    /**
     * Cleanup method for graceful shutdown.
     * This should be called during server shutdown.
     */
    public void shutdown() {
        LOGGER.info("Shutting down Tapestry");
        
        if (typeScriptRuntime != null) {
            typeScriptRuntime.close();
        }
        
        LOGGER.info("Tapestry shutdown complete");
    }
}
