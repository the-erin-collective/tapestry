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
        
    }
    
    /**
     * Phase 2: Load TypeScript mods.
     */
    private static void loadTypeScriptMods() {
        // TS_LOAD: Initialize runtime and discover mods
        LOGGER.info("=== TS_LOAD PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        
        // Initialize TypeScript runtime with mod loading capabilities
        tsRuntime.initializeForModLoading(api, modRegistry, hookRegistry);
        
        // Discover all TypeScript mod files
        List<String> modFiles = modDiscovery.discoverMods();
        
        // Evaluate all mod scripts (mod definition only)
        for (String modFile : modFiles) {
            try {
                String source = readModSource(modFile);
                String sourceName = extractSourceName(modFile);
                tsRuntime.evaluateScript(source, sourceName);
                LOGGER.debug("Loaded mod script: {}", sourceName);
            } catch (Exception e) {
                LOGGER.error("Failed to load mod: {}", modFile, e);
                throw new RuntimeException("Mod loading failed: " + modFile, e);
            }
        }
        
        // Complete discovery phase
        modRegistry.completeDiscovery();
        
        // TS_READY: Execute onLoad functions and allow hook registration
        LOGGER.info("=== TS_READY PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_READY);
        
        // Extend the tapestry object with full API for TS_READY phase
        tsRuntime.extendForReadyPhase(api, hookRegistry);
        
        // Allow hook registration
        hookRegistry.allowRegistration();
        
        // Execute onLoad for all mods in deterministic order
        for (var mod : modRegistry.getMods()) {
            try {
                LOGGER.info("Executing onLoad for mod: {}", mod.id());
                tsRuntime.executeOnLoad(mod.getOnLoad(), mod.id(), api);
                LOGGER.debug("Completed onLoad for mod: {}", mod.id());
            } catch (Exception e) {
                LOGGER.error("Mod {} threw exception in onLoad", mod.id(), e);
                throw new RuntimeException("Mod onLoad failed: " + mod.id(), e);
            }
        }
        
        // Complete loading phase
        modRegistry.completeLoading();
        
        // Disallow further hook registration
        hookRegistry.disallowRegistration();
        
        LOGGER.info("TypeScript mod loading complete");
    }
    
    /**
     * Reads the contents of a mod file.
     * 
     * @param modFile path to mod file
     * @return file contents
     */
    private static String readModSource(String modFile) {
        try {
            return java.nio.file.Files.readString(java.nio.file.Paths.get(modFile));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read mod file: " + modFile, e);
        }
    }
    
    /**
     * Extracts the source name from a file path.
     * 
     * @param modFile full path to mod file
     * @return source name
     */
    private static String extractSourceName(String modFile) {
        return java.nio.file.Paths.get(modFile).getFileName().toString();
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
        return tsRuntime;
    }
    
    /**
     * Cleanup method for graceful shutdown.
     * This should be called during server shutdown.
     */
    public void shutdown() {
        LOGGER.info("Shutting down Tapestry");
        
        if (tsRuntime != null) {
            // Note: TypeScriptRuntime doesn't have a close method in current implementation
            // This can be added later if needed
        }
        
        LOGGER.info("Tapestry shutdown complete");
    }
}
