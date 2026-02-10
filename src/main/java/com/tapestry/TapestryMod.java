package com.tapestry;

import com.tapestry.api.TapestryAPI;
import com.tapestry.extension.ExtensionRegistry;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.typescript.TypeScriptRuntime;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Tapestry mod that orchestrates the Phase 1 lifecycle.
 * 
 * This class implements Fabric's ModInitializer and manages the complete
 * Phase 1 sequence: discovery → registration → freeze → TS runtime initialization.
 */
public class TapestryMod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryMod.class);
    
    private TapestryAPI api;
    private ExtensionRegistry extensionRegistry;
    private TypeScriptRuntime typeScriptRuntime;
    
    @Override
    public void onInitialize() {
        LOGGER.info("Starting Tapestry Phase 1 initialization");
        
        try {
            // Phase 1: Bootstrap (already in BOOTSTRAP from static init)
            executeBootstrapPhase();
            
            // Phase 2: Discovery
            executeDiscoveryPhase();
            
            // Phase 3: Registration
            executeRegistrationPhase();
            
            // Phase 4: Freeze
            executeFreezePhase();
            
            // Phase 5: TS Load
            executeTSLoadPhase();
            
            // Phase 6: TS Ready
            executeTSReadyPhase();
            
            LOGGER.info("Tapestry Phase 1 initialization completed successfully");
            
        } catch (Exception e) {
            LOGGER.error("Tapestry Phase 1 initialization failed", e);
            throw new RuntimeException("Tapestry initialization failed", e);
        }
    }
    
    /**
     * Executes the BOOTSTRAP phase.
     * Core framework initialization.
     */
    private void executeBootstrapPhase() {
        LOGGER.info("=== BOOTSTRAP PHASE ===");
        
        PhaseController phaseController = PhaseController.getInstance();
        phaseController.requirePhase(TapestryPhase.BOOTSTRAP);
        
        // Initialize core components
        api = new TapestryAPI();
        extensionRegistry = new ExtensionRegistry(api);
        typeScriptRuntime = new TypeScriptRuntime();
        
        LOGGER.info("Core components initialized");
        
        // Advance to DISCOVERY
        phaseController.advanceTo(TapestryPhase.DISCOVERY);
        LOGGER.info("Advanced to DISCOVERY phase");
    }
    
    /**
     * Executes the DISCOVERY phase.
     * Discovers all Tapestry extensions via Fabric entrypoints.
     */
    private void executeDiscoveryPhase() {
        LOGGER.info("=== DISCOVERY PHASE ===");
        
        PhaseController.getInstance().requirePhase(TapestryPhase.DISCOVERY);
        
        // Discover extensions
        extensionRegistry.discoverExtensions();
        
        LOGGER.info("Discovered {} extensions", extensionRegistry.getExtensionCount());
        
        // Advance to REGISTRATION
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        LOGGER.info("Advanced to REGISTRATION phase");
    }
    
    /**
     * Executes the REGISTRATION phase.
     * Registers all discovered extensions and allows them to extend the API.
     */
    private void executeRegistrationPhase() {
        LOGGER.info("=== REGISTRATION PHASE ===");
        
        PhaseController.getInstance().requirePhase(TapestryPhase.REGISTRATION);
        
        // Register extensions
        extensionRegistry.registerExtensions();
        
        LOGGER.info("Registered {} extensions with capabilities: {}", 
            extensionRegistry.getExtensionCount(),
            extensionRegistry.getRegisteredCapabilities());
        
        // Advance to FREEZE
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        LOGGER.info("Advanced to FREEZE phase");
    }
    
    /**
     * Executes the FREEZE phase.
     * Permanently freezes the API surface.
     */
    private void executeFreezePhase() {
        LOGGER.info("=== FREEZE PHASE ===");
        
        PhaseController.getInstance().requirePhase(TapestryPhase.FREEZE);
        
        // Freeze the API
        api.freeze();
        
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
