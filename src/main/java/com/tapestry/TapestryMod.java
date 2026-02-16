package com.tapestry;

import com.tapestry.api.TapestryAPI;
import com.tapestry.config.ConfigService;
import com.tapestry.events.EventBus;
import com.tapestry.extensions.*;
import com.tapestry.hooks.HookRegistry;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.persistence.ClientPersistenceService;
import com.tapestry.persistence.PersistenceServiceInterface;
import com.tapestry.persistence.ServerPersistenceService;
import com.tapestry.players.PlayerService;
import com.tapestry.scheduler.SchedulerService;
import com.tapestry.state.ModStateService;
import com.tapestry.typescript.DiscoveredMod;
import com.tapestry.typescript.TsModDiscovery;
import com.tapestry.typescript.TsModRegistry;
import com.tapestry.typescript.TypeScriptRuntime;
import com.tapestry.typescript.PlayersApi;
import com.tapestry.mod.ModRegistry;
import com.tapestry.mod.ModDiscovery;
import com.tapestry.mod.ModDescriptor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
    private static PlayerService playerService;
    private static SchedulerService schedulerService;
    private static EventBus eventBus;
    private static ConfigService configService;
    private static ModStateService stateService;
    private static PlayersApi playersApi;
    private static PersistenceServiceInterface persistenceService;
    
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
        extensionsHookRegistry = new com.tapestry.extensions.DefaultHookRegistry(
            PhaseController.getInstance(), new HashMap<>()
        ); // Phase 4 registry
        tsHookRegistry = new com.tapestry.hooks.HookRegistry(); // TS handler registry
        tsRuntime = new TypeScriptRuntime();
        modDiscovery = new TsModDiscovery();
        
        LOGGER.info("Core framework components initialized");
        
        // DISCOVERY: Discover extensions
        LOGGER.info("=== DISCOVERY PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        
        // Register Fabric server lifecycle hooks
        registerFabricHooks();
        
        LOGGER.info("Framework bootstrap complete");
    }
    
    /**
     * Registers Fabric API hooks for server lifecycle and player events.
     */
    private static void registerFabricHooks() {
        // Server started hook - initialize PlayerService and PersistenceService
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started - initializing services");
            
            // Initialize PlayerService with server instance
            if (playerService != null) {
                playerService.setServer(server);
                LOGGER.info("PlayerService initialized with server instance");
            }
            
            // Initialize Phase 9 persistence service
            if (persistenceService == null) {
                if (server.isDedicated()) {
                    // Server: world-scoped storage
                    java.nio.file.Path worldDir = java.nio.file.Paths.get("world");
                    ServerPersistenceService.initialize(worldDir);
                    persistenceService = ServerPersistenceService.getInstance();
                    LOGGER.info("ServerPersistenceService initialized for dedicated server");
                } else {
                    // Client: instance-scoped storage
                    java.nio.file.Path gameDir = java.nio.file.Paths.get(".");
                    ClientPersistenceService.initialize(gameDir);
                    persistenceService = ClientPersistenceService.getInstance();
                    LOGGER.info("ClientPersistenceService initialized for integrated server");
                }
            }
            
            // Advance to PERSISTENCE_READY phase
            PhaseController.getInstance().advanceTo(TapestryPhase.PERSISTENCE_READY);
            LOGGER.info("PERSISTENCE_READY phase completed");
            
            // Initialize Phase 10.5 mod system
            if (tsRuntime != null) {
                try {
                    initializePhase105ModSystem();
                } catch (Exception e) {
                    LOGGER.error("Failed to initialize Phase 10.5 mod system", e);
                    throw new RuntimeException("Phase 10.5 mod system initialization failed", e);
                }
            }
            
            // Now advance to RUNTIME
            PhaseController.getInstance().advanceTo(TapestryPhase.RUNTIME);
            LOGGER.info("RUNTIME phase completed - Tapestry is ready");
            
            // Initialize client presentation layer (Phase 10)
            if (tsRuntime != null) {
                try {
                    tsRuntime.extendForClientPresentation();
                    PhaseController.getInstance().advanceTo(TapestryPhase.CLIENT_PRESENTATION_READY);
                    
                    // Emit Phase 11 engine:runtimeStart event
                    if (eventBus != null) {
                        try {
                            eventBus.emit(null, "engine:runtimeStart", Map.of(
                                "serverTicks", server.getTicks(),
                                "timestamp", System.currentTimeMillis()
                            ));
                        } catch (Exception e) {
                            LOGGER.error("Error during engine:runtimeStart event emission", e);
                        }
                    }
                    
                    // Extend runtime for CLIENT_PRESENTATION_READY phase
                    tsRuntime.extendForClientPresentation();
                    LOGGER.info("CLIENT_PRESENTATION_READY phase completed");
                } catch (Exception e) {
                    LOGGER.error("Failed to initialize client presentation layer", e);
                    // Don't throw - client presentation is optional
                }
            }
        });
        
        // Server tick hook - for scheduler and other runtime services
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Emit Phase 11 engine:tick event
            if (eventBus != null) {
                try {
                    eventBus.emit(null, "engine:tick", server.getTicks());
                } catch (Exception e) {
                    LOGGER.error("Error during engine:tick event emission", e);
                }
            }
            
            if (schedulerService != null) {
                try {
                    schedulerService.tick(server.getTicks());
                } catch (Exception e) {
                    LOGGER.error("Error during scheduler tick", e);
                }
            }
        });
        
        // Player join/quit hooks - emit events to EventBus
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (eventBus != null) {
                try {
                    // Create player event context using RuntimeContextFactory
                    var player = handler.getPlayer();
                    var worldId = player.getWorld().getRegistryKey().getValue().toString();
                    var tick = server.getTicks();
                    
                    // Emit player join event
                    eventBus.emit("engine", "playerJoin", 
                        (Map<String, Object>) com.tapestry.runtime.RuntimeContextFactory.createPlayerEventContext(
                            "tapestry", 
                            player.getUuidAsString(), 
                            player.getName().getString(), 
                            worldId, 
                            tick, 
                            null
                        )
                    );
                } catch (Exception e) {
                    LOGGER.error("Error during player join event", e);
                }
            }
        });
        
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (eventBus != null) {
                try {
                    // Create player event context using RuntimeContextFactory
                    var player = handler.getPlayer();
                    var worldId = player.getWorld().getRegistryKey().getValue().toString();
                    var tick = server.getTicks();
                    
                    // Emit player quit event
                    eventBus.emit("engine", "playerQuit", 
                        (Map<String, Object>) com.tapestry.runtime.RuntimeContextFactory.createPlayerEventContext(
                            "tapestry", 
                            player.getUuidAsString(), 
                            player.getName().getString(), 
                            worldId, 
                            tick, 
                            null
                        )
                    );
                } catch (Exception e) {
                    LOGGER.error("Error during player quit event", e);
                }
            }
        });
        
        LOGGER.info("Fabric hooks registered");
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
                true    // disableInvalid = true
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
        
        // Register core Phase 7 player capabilities
        registerCoreCapabilities(apiRegistry);
        
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
        
        // Complete loading phase
        modRegistry.completeLoading();
        
        // Disallow further hook registration and freeze hooks registry
        tsHookRegistry.disallowRegistration();
        tsHookRegistry.freeze();
        
        LOGGER.info("TypeScript mod loading complete");
        
        // TS_REGISTER: Capability registration phase
        LOGGER.info("=== TS_REGISTER PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_REGISTER);
        
        try {
            // Extend TypeScript runtime with capability registration APIs
            tsRuntime.extendForCapabilityRegistration();
            
            // Execute onLoad for all mods in deterministic order (capability registration happens here)
            for (var mod : modRegistry.getMods()) {
                try {
                    LOGGER.info("Executing onLoad for mod: {} (capability registration)", mod.id());
                    tsRuntime.executeOnLoad(mod.getOnLoad(), mod.id());
                    LOGGER.debug("Completed onLoad for mod: {}", mod.id());
                } catch (Exception e) {
                    LOGGER.error("Mod {} threw exception in onLoad", mod.id(), e);
                    throw new RuntimeException("Mod onLoad failed: " + mod.id(), e);
                }
            }
            
            // Complete capability registration phase
            modRegistry.completeCapabilityRegistration();
            
            LOGGER.info("TypeScript capability registration complete");
        } catch (Exception e) {
            LOGGER.error("Failed during capability registration phase", e);
            throw new RuntimeException("Capability registration failed", e);
        }
        
        // TS_ACTIVATE: Validate and resolve capabilities
        LOGGER.info("=== TS_ACTIVATE PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_ACTIVATE);
        
        try {
            // Validate capabilities and resolve dependency graph
            var capabilityDescriptors = validationResult.enabled().values().stream()
                .collect(java.util.stream.Collectors.toMap(
                    ext -> ext.descriptor().id(),
                    ValidatedExtension::descriptor
                ));
            
            // Create capability validator
            // Use the same config directory as other services (ConfigService)
            var configDir = java.nio.file.Paths.get("config", "tapestry");
            var capabilityValidator = new com.tapestry.extensions.CapabilityValidator(configDir);
            
            // Validate capabilities (includes JS vs descriptor consistency check)
            var capabilityValidationResult = capabilityValidator.validateCapabilities(capabilityDescriptors);
            
            // Print capability validation report
            com.tapestry.extensions.ExtensionValidationReportPrinter.printCapabilityReport(capabilityValidationResult);
            
            if (capabilityValidationResult.errors().isEmpty()) {
                // Initialize CapabilityRegistry with resolved capabilities
                var allProvidedCapabilities = com.tapestry.typescript.CapabilityApi.getAllProvidedCapabilities();
                var capabilityProviders = capabilityValidationResult.capabilityProviders();
                
                LOGGER.info("Initializing CapabilityRegistry with {} providers", capabilityProviders.size());
                LOGGER.debug("Capability providers: {}", capabilityProviders);
                LOGGER.debug("All provided capabilities: {}", allProvidedCapabilities.keySet());
                
                com.tapestry.extensions.CapabilityRegistry.initialize(capabilityProviders, allProvidedCapabilities);
                com.tapestry.extensions.CapabilityRegistry.freeze();
                
                // Clear temporary capability storage
                com.tapestry.typescript.CapabilityApi.clearTemporaryStorage();
                
                LOGGER.info("Capability system initialized successfully");
            } else {
                LOGGER.error("Capability validation failed with {} errors", capabilityValidationResult.errors().size());
                throw new RuntimeException("Capability validation failed - aborting startup");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed during capability activation phase", e);
            throw new RuntimeException("Capability activation failed", e);
        }
        
        // RUNTIME: Initialize Phase 6 services and begin gameplay
        initializeRuntimeServices();
    }
    
    /**
     * Initializes Phase 6 runtime services and advances to RUNTIME phase.
     */
    private static void initializeRuntimeServices() {
        LOGGER.info("=== RUNTIME PHASE ===");
        PhaseController.getInstance().advanceTo(TapestryPhase.RUNTIME);
        
        try {
            // Initialize Phase 6 services
            schedulerService = new SchedulerService();
            eventBus = new EventBus();
            configService = new ConfigService(java.nio.file.Paths.get("config", "tapestry", "mods"));
            stateService = new ModStateService();
            playerService = new PlayerService(null); // Will be updated with server instance later
            
            // Initialize Phase 9 persistence service
            // Note: Storage directory will be determined when server starts
            persistenceService = null; // Will be initialized in SERVER_STARTED hook
            
            // Update PlayersApi with actual PlayerService
            playersApi.setPlayerService(playerService);
            
            // Load configurations for all mods
            for (var mod : modRegistry.getMods()) {
                // TODO: Load config schema from mod definition
                // For now, we'll skip config loading until schema is defined
                LOGGER.debug("Skipping config loading for mod {} (no schema yet)", mod.id());
            }
            
            // Extend TypeScript runtime with Phase 6 APIs
            tsRuntime.extendForRuntime(schedulerService, eventBus, configService, stateService, modRegistry);
            
            // Store services for runtime use (you might want to store these as fields)
            // For now, we'll just log that they're initialized
            LOGGER.info("Runtime services initialized:");
            LOGGER.info("- SchedulerService: {} pending tasks", schedulerService.getPendingTaskCount());
            LOGGER.info("- EventBus: ready");
            LOGGER.info("- ConfigService: ready");
            LOGGER.info("- ModStateService: ready");
            LOGGER.info("- PlayerService: ready");
            
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
     * Gets the current server instance.
     * This is primarily for testing and internal use.
     * 
     * @return the server instance or null if not available
     */
    public static net.minecraft.server.MinecraftServer getServer() {
        // This would need to be stored when server becomes available
        // For now, return null as server isn't available during initialization
        return null;
    }
    
    /**
     * Gets the global EventBus instance for Phase 11 reactive event system.
     * 
     * @return the EventBus instance or null if not initialized
     */
    public static EventBus getEventBus() {
        return eventBus;
    }
    
    /**
     * Registers core Phase 7 player capabilities.
     * 
     * @param apiRegistry the API registry to register functions to
     */
    private static void registerCoreCapabilities(DefaultApiRegistry apiRegistry) {
        try {
            // Create PlayersApi instance
            playersApi = new PlayersApi(null); // Will be updated later with actual PlayerService
            
            // Create namespace once to avoid creating multiple instances
            org.graalvm.polyglot.proxy.ProxyObject playersNs = playersApi.createNamespace();
            
            // Register player identity & discovery APIs
            apiRegistry.addFunction("tapestry", "players.list", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("list"));
            apiRegistry.addFunction("tapestry", "players.get", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("get"));
            apiRegistry.addFunction("tapestry", "players.findByName", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("findByName"));
            
            // Register player messaging APIs
            apiRegistry.addFunction("tapestry", "players.sendChat", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("sendChat"));
            apiRegistry.addFunction("tapestry", "players.sendActionBar", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("sendActionBar"));
            apiRegistry.addFunction("tapestry", "players.sendTitle", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("sendTitle"));
            
            // Register player query APIs
            apiRegistry.addFunction("tapestry", "players.getPosition", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("getPosition"));
            apiRegistry.addFunction("tapestry", "players.getLook", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("getLook"));
            apiRegistry.addFunction("tapestry", "players.getGameMode", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("getGameMode"));
            
            // Register raycasting API
            apiRegistry.addFunction("tapestry", "players.raycastBlock", (org.graalvm.polyglot.proxy.ProxyExecutable) playersNs.getMember("raycastBlock"));
            
            LOGGER.info("Core Phase 7 player capabilities registered");
        } catch (Exception e) {
            LOGGER.error("Failed to register core player capabilities", e);
            throw new RuntimeException("Core capability registration failed", e);
        }
    }
    
    /**
     * Initializes the Phase 10.5 mod system with two-pass loading.
     */
    private static void initializePhase105ModSystem() {
        LOGGER.info("Initializing Phase 10.5 mod system");
        
        // Advance to TS_REGISTER phase
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_REGISTER);
        LOGGER.info("TS_REGISTER phase started");
        
        try {
            // Extend runtime for registration
            tsRuntime.extendForRegistration();
            
            // Discover and evaluate mods
            ModDiscovery discovery = new ModDiscovery();
            List<ModDiscovery.DiscoveredMod> discoveredMods = discovery.discoverMods();
            
            if (discoveredMods.isEmpty()) {
                LOGGER.info("No mods discovered, skipping to activation phase");
            } else {
                // Evaluate all mod scripts
                for (ModDiscovery.DiscoveredMod discoveredMod : discoveredMods) {
                    try {
                        String modSource = java.nio.file.Files.readString(
                            java.nio.file.Paths.get(discoveredMod.entryPath()));
                        tsRuntime.evaluateModScript(modSource, discoveredMod.id());
                        LOGGER.debug("Evaluated mod script: {}", discoveredMod.id());
                    } catch (Exception e) {
                        LOGGER.error("Failed to evaluate mod script: {}", discoveredMod.id(), e);
                        throw new RuntimeException("Mod script evaluation failed: " + discoveredMod.id(), e);
                    }
                }
                
                LOGGER.info("Evaluated {} mod scripts", discoveredMods.size());
            }
            
            // Advance to TS_ACTIVATE phase
            PhaseController.getInstance().advanceTo(TapestryPhase.TS_ACTIVATE);
            LOGGER.info("TS_ACTIVATE phase started");
            
            // Extend runtime for activation
            tsRuntime.extendForActivation();
            
            // Log mod registry stats
            ModRegistry modRegistry = ModRegistry.getInstance();
            ModRegistry.ModRegistryStats stats = modRegistry.getStats();
            LOGGER.info("Phase 10.5 mod system initialized - Registered: {}, Activated: {}, Exports: {}", 
                       stats.registeredCount(), stats.activatedCount(), stats.exportCount());
            
        } catch (Exception e) {
            LOGGER.error("Phase 10.5 mod system initialization failed", e);
            throw e;
        }
        
        LOGGER.info("Phase 10.5 mod system initialization complete");
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
