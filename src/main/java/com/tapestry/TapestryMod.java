package com.tapestry;

import com.tapestry.api.TapestryAPI;
import com.tapestry.config.ConfigService;
import com.tapestry.events.EventBus;
import com.tapestry.extensions.*;
import com.tapestry.hooks.HookRegistry;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.scheduler.SchedulerService;
import com.tapestry.state.ModStateService;
import com.tapestry.typescript.DiscoveredMod;
import com.tapestry.typescript.TsModDiscovery;
import com.tapestry.typescript.TsModRegistry;
import com.tapestry.typescript.TypeScriptRuntime;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Main Tapestry mod implementing Fabric's ModInitializer.
 * 
 * This class orchestrates the complete Tapestry lifecycle including
 * extension discovery, validation, TypeScript mod loading, and hook registration.
 */
public class TapestryMod implements ModInitializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryMod.class);
    
    private static TapestryAPI api;
    private static TsModRegistry modRegistry;
    private static com.tapestry.extensions.HookRegistry extensionsHookRegistry; // Phase 4 registry
    private static com.tapestry.hooks.HookRegistry tsHookRegistry; // TS handler registry
    private static TypeScriptRuntime tsRuntime;
    private static TsModDiscovery modDiscovery;
    
    // Phase 3: Extension validation
    private static ExtensionValidationResult validationResult;
    
    @Override
    public void onInitialize() {
        LOGGER.info("Starting Tapestry initialization");
        
        try {
            // Phase 1: Bootstrap framework
            bootstrapFramework();
            
            // Phase 3: Extension validation
            validateExtensions();
            
            // Phase 2: TypeScript mod loading
            loadTypeScriptMods();
            
            LOGGER.info("Tapestry initialization completed successfully");
            
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
        modRegistry = new TsModRegistry();
        extensionsHookRegistry = new com.tapestry.extensions.DefaultHookRegistry(); // Phase 4 registry
        tsHookRegistry = new com.tapestry.hooks.HookRegistry(); // TS handler registry
        tsRuntime = new TypeScriptRuntime();
        modDiscovery = new TsModDiscovery();
        
        LOGGER.info("Core framework components initialized");
        
        // DISCOVERY: Discover extensions
        LOGGER.info("=== DISCOVERY PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        
        LOGGER.info("Framework bootstrap complete");
    }
    
    /**
     * Phase 3: Validate extensions with no side effects.
     */
    private static void validateExtensions() {
        LOGGER.info("=== VALIDATION PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.VALIDATION);
        
        try {
            // Discover extension providers
            var providers = ExtensionDiscovery.discoverProviders();
            LOGGER.info("Discovered {} extension providers", providers.size());
            
            // Set up validation policy
            var policy = new ValidationPolicy(
                false,  // failFast = false
                true,   // disableInvalid = true
                true    // warnOnOptionalMissing = true
            );
            
            // Create validator with current Tapestry version
            var currentVersion = Version.parse("0.3.0"); // TODO: Get from build config
            var validator = new ExtensionValidator(currentVersion, policy);
            
            // Validate extensions
            validationResult = validator.validate(providers);
            
            // Print validation report
            ExtensionValidationReportPrinter.printReport(validationResult);
            ExtensionValidationReportPrinter.printSummary(validationResult);
            
            // Check if any extensions were enabled
            if (validationResult.enabled().isEmpty()) {
                LOGGER.warn("No extensions passed validation - continuing with TypeScript mods only");
            }
            
        } catch (Exception e) {
            LOGGER.error("Extension validation failed", e);
            throw new RuntimeException("Failed to validate extensions", e);
        }
        
        LOGGER.info("Extension validation complete");
    }
    
    /**
     * Phase 2: Load TypeScript mods.
     */
    private static void loadTypeScriptMods() {
        // REGISTRATION: Register validated extensions
        LOGGER.info("=== REGISTRATION PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        
        // Build registries for capability registration
        var phaseController = PhaseController.getInstance();
        var declaredCapabilities = validationResult.enabled().values().stream()
            .collect(java.util.stream.Collectors.toMap(
                ext -> ext.descriptor().id(),
                ValidatedExtension::descriptor
            ));
        
        var apiRegistry = new DefaultApiRegistry(phaseController, declaredCapabilities);
        var hookRegistry = new DefaultHookRegistry(phaseController, declaredCapabilities);
        var serviceRegistry = new DefaultServiceRegistry(phaseController, declaredCapabilities);
        
        // Create orchestrator and register extensions
        var orchestrator = new ExtensionRegistrationOrchestrator(
            phaseController, apiRegistry, hookRegistry, serviceRegistry
        );
        
        try {
            orchestrator.registerExtensions(validationResult.enabled());
            LOGGER.info("Extension registration completed successfully");
        } catch (Exception e) {
            LOGGER.error("Extension registration failed - aborting startup", e);
            throw new RuntimeException("Extension registration failed", e);
        }
        
        // Freeze registries and get the API tree for TypeScript
        apiRegistry.freeze();
        hookRegistry.freeze();
        serviceRegistry.freeze();
        
        var apiTree = apiRegistry.getApiTree();
        LOGGER.info("API surface frozen - no further modifications allowed");
        
        // TS_LOAD: Initialize runtime and discover mods
        LOGGER.info("=== TS_LOAD PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        
        // Initialize TypeScript runtime with mod loading capabilities
        tsRuntime.initializeForModLoading(apiTree, modRegistry, tsHookRegistry);
        
        // Discover all TypeScript mod files
        List<DiscoveredMod> discoveredMods;
        try {
            discoveredMods = modDiscovery.discoverMods();
        } catch (IOException e) {
            throw new RuntimeException("Failed to discover TypeScript mods", e);
        }
        
        // Evaluate all mod scripts (mod definition only)
        for (DiscoveredMod mod : discoveredMods) {
            try {
                String source = readModSource(mod);
                tsRuntime.evaluateScript(source, mod.sourceName());
                LOGGER.debug("Loaded mod script: {}", mod.sourceName());
            } catch (Exception e) {
                LOGGER.error("Failed to load mod: {}", mod.sourceName(), e);
                throw new RuntimeException("Mod loading failed: " + mod.sourceName(), e);
            }
        }
        
        // Complete discovery phase
        modRegistry.completeDiscovery();
        
        // Verify all scripts called tapestry.mod.define (fail-fast enforcement)
        for (DiscoveredMod mod : discoveredMods) {
            if (!TypeScriptRuntime.hasModDefinedInSource(mod.sourceName())) {
                throw new RuntimeException(
                    String.format("Script '%s' never called tapestry.mod.define - startup aborted", mod.sourceName())
                );
            }
        }
        
        // TS_READY: Execute onLoad functions and allow hook registration
        LOGGER.info("=== TS_READY PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_READY);
        
        // Extend the tapestry object with hook APIs for TS_READY phase
        tsRuntime.extendForReadyPhase(tsHookRegistry, modRegistry);
        
        // Allow hook registration
        tsHookRegistry.allowRegistration();
        
        // Execute onLoad for all mods in deterministic order
        for (var mod : modRegistry.getMods()) {
            try {
                LOGGER.info("Executing onLoad for mod: {}", mod.id());
                tsRuntime.executeOnLoad(mod.getOnLoad(), mod.id());
                LOGGER.debug("Completed onLoad for mod: {}", mod.id());
            } catch (Exception e) {
                LOGGER.error("Mod {} threw exception in onLoad", mod.id(), e);
                throw new RuntimeException("Mod onLoad failed: " + mod.id(), e);
            }
        }
        
        // Complete loading phase
        modRegistry.completeLoading();
        
        // Disallow further hook registration and freeze the registry
        tsHookRegistry.disallowRegistration();
        tsHookRegistry.freeze();
        
        LOGGER.info("TypeScript mod loading complete");
        
        // RUNTIME: Initialize Phase 6 services and begin gameplay
        initializeRuntimeServices();
    }
    
    /**
     * Initializes Phase 6 runtime services and advances to RUNTIME phase.
     */
    private void initializeRuntimeServices() {
        LOGGER.info("=== RUNTIME PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.RUNTIME);
        
        try {
            // Initialize Phase 6 services
            SchedulerService schedulerService = new SchedulerService();
            EventBus eventBus = new EventBus();
            ConfigService configService = new ConfigService(java.nio.file.Paths.get("config", "tapestry", "mods"));
            ModStateService stateService = new ModStateService();
            
            // Load configurations for all mods
            for (var mod : modRegistry.getMods()) {
                // TODO: Load config schema from mod definition
                // For now, we'll skip config loading until schema is defined
                LOGGER.debug("Skipping config loading for mod {} (no schema yet)", mod.id());
            }
            
            // Disallow further event registration
            eventBus.disallowRegistration();
            
            // Extend TypeScript runtime with Phase 6 APIs
            tsRuntime.extendForRuntime(schedulerService, eventBus, configService, stateService, modRegistry);
            
            // Store services for runtime use (you might want to store these as fields)
            // For now, we'll just log that they're initialized
            LOGGER.info("Runtime services initialized:");
            LOGGER.info("- SchedulerService: {} pending tasks", schedulerService.getPendingTaskCount());
            LOGGER.info("- EventBus: {} event types", eventBus.getEventNames().size());
            LOGGER.info("- ConfigService: ready");
            LOGGER.info("- ModStateService: ready");
            
            LOGGER.info("Phase 6 runtime initialization complete - server is live!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize runtime services", e);
            throw new RuntimeException("Runtime initialization failed", e);
        }
    }
    
    /**
     * Reads the contents of a mod file.
     * Handles both filesystem and classpath mods.
     * 
     * @param mod discovered mod information
     * @return file contents
     */
    private static String readModSource(DiscoveredMod mod) {
        try {
            if (mod.classpath()) {
                // For classpath mods, read using InputStream (works for JAR files)
                try (var in = java.nio.file.Files.newInputStream(mod.path())) {
                    return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            } else {
                // For filesystem mods, read directly
                return java.nio.file.Files.readString(mod.path());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read mod file: " + mod.sourceName(), e);
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
     * Gets ExtensionValidationResult instance.
     * This is primarily for testing and internal use.
     * 
     * @return ExtensionValidationResult instance
     */
    public ExtensionValidationResult getValidationResult() {
        return validationResult;
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
