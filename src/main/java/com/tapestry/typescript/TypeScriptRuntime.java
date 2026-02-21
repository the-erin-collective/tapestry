package com.tapestry.typescript;

import com.tapestry.extensions.Version;
import com.tapestry.config.ConfigService;
import com.tapestry.events.EventBus;
import com.tapestry.extensions.types.GraalVMTypeIntegration;
import com.tapestry.extensions.types.ExtensionTypeRegistry;
import com.tapestry.extensions.ExtensionLifecycleManager;
import com.tapestry.extensions.ExtensionState;
import com.tapestry.extensions.LifecycleViolationException;
import com.tapestry.hooks.HookRegistry;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.persistence.PersistenceService;
import com.tapestry.persistence.ModStateStore;
import com.tapestry.persistence.PersistenceApi;
import com.tapestry.scheduler.SchedulerService;
import com.tapestry.state.ModStateService;
import com.tapestry.overlay.OverlayRegistry;
import com.tapestry.overlay.OverlayApi;
import com.tapestry.mod.ModRegistry;
import com.tapestry.mod.ModDescriptor;
import com.tapestry.mod.ModDiscovery;
import com.tapestry.mod.ModActivationException;
import com.tapestry.performance.PerformanceMonitor;
import com.tapestry.rpc.SafeTapestryBridge;
import com.tapestry.rpc.client.RpcClientRuntime;
import com.tapestry.rpc.client.RpcClientFacade;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TypeScript runtime using GraalVM Polyglot for JavaScript execution.
 * 
 * This class manages the JavaScript execution context, API injection,
 * and script evaluation for TypeScript mods. In Phase 2, it provides
 * a secure sandboxed environment for mod loading and hook registration.
 */
public class TypeScriptRuntime {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeScriptRuntime.class);
    
    // Phase 14: Type integration for cross-mod type contracts
    private static GraalVMTypeIntegration typeIntegration;
    private static ExtensionTypeRegistry typeRegistry;
    
    // Phase 15: Extension lifecycle management
    private static ExtensionLifecycleManager lifecycleManager;
    
    private static Context jsContext;
    private static boolean initialized = false;
    
    /**
     * Thread-safe execution context tracking for TypeScript runtime.
     */
    public enum ExecutionContextMode {
        NONE,
        ON_LOAD,
        RUNTIME
    }

    /**
     * Thread-local execution context for tracking current mod and execution mode.
     */
    private static final ThreadLocal<ExecutionContext> currentContext = new ThreadLocal<>();
    private static final Set<String> definedMods = ConcurrentHashMap.newKeySet();

    /**
     * Represents the current execution context.
     */
    public static class ExecutionContext {
        private final String modId;
        private final ExecutionContextMode mode;
        private final String source;
        
        public ExecutionContext(String modId, ExecutionContextMode mode, String source) {
            this.modId = modId;
            this.mode = mode;
            this.source = source;
        }
        
        public String modId() { return modId; }
        public ExecutionContextMode mode() { return mode; }
        public String source() { return source; }
    }

    /**
     * Sets the current execution context.
     */
    public static void setExecutionContext(String modId, ExecutionContextMode mode, String source) {
        currentContext.set(new ExecutionContext(modId, mode, source));
    }

    /**
     * Gets the current execution context.
     */
    public static ExecutionContext getCurrentContext() {
        return currentContext.get();
    }

    /**
     * Clears the execution context.
     */
    public static void clearExecutionContext() {
        currentContext.set(new ExecutionContext(null, ExecutionContextMode.NONE, null));
    }
    
    /**
     * Gets the current mod ID from the execution context.
     * 
     * @return the current mod ID or null if not in a mod context
     */
    public static String getCurrentModId() {
        return getCurrentContext().modId();
    }
    
    /**
     * Evaluates a JavaScript expression in the runtime context.
     * 
     * @param expression JavaScript expression to evaluate
     * @return result of the expression
     */
    public static Object evalExpression(String expression) {
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            return jsContext.eval("js", expression);
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate expression: {}", expression, e);
            throw new RuntimeException("Expression evaluation failed: " + expression, e);
        }
    }
    
    /**
     * Gets the current source file from the execution context.
     * 
     * @return the current source file or null if not in a mod context
     */
    public static String getCurrentSource() {
        return getCurrentContext().source();
    }
    
    /**
     * Checks if a mod has been defined in the given source.
     * 
     * @param source the source file
     * @return true if a mod has been defined, false otherwise
     */
    public static boolean hasModDefinedInSource(String source) {
        synchronized (sourcesWithModDefine) {
            return sourcesWithModDefine.contains(source);
        }
    }
    
    /**
     * Marks that a mod was defined in the given source.
     * 
     * @param source the source where the mod was defined
     */
    public static void markModDefinedInSource(String source) {
        synchronized (sourcesWithModDefine) {
            sourcesWithModDefine.add(source);
        }
    }
    
    /**
     * Clears source tracking (for fresh runtime initialization).
     */
    public static void clearSourceTracking() {
        synchronized (sourcesWithModDefine) {
            sourcesWithModDefine.clear();
        }
    }
    
    // Track sources that have already defined a mod (one-define-per-file rule)
    private static final Set<String> sourcesWithModDefine = new HashSet<>();
    
    // Define function instance
    private static TsModDefineFunction defineFunction;
    
    /**
     * Initializes the TypeScript runtime with mod loading capabilities.
     * Phase 14: Accepts type registry for cross-mod type contracts.
     * 
     * @param apiTree frozen ProxyObject tree to expose as tapestry object
     * @param modRegistry the mod registry for registration
     * @param hookRegistry the hook registry for hook registration
     * @param typeRegistry the type registry for Phase 14 (optional)
     */
    public void initializeForModLoading(ProxyObject apiTree, TsModRegistry modRegistry, 
                                       HookRegistry hookRegistry, ExtensionTypeRegistry typeRegistry) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_LOAD);
        
        if (initialized) {
            throw new IllegalStateException("TypeScript runtime already initialized");
        }
        
        try {
            // Clear source tracking for fresh runtime initialization
            clearSourceTracking();
            
            // Store type registry for Phase 14
            TypeScriptRuntime.typeRegistry = typeRegistry;
            
            // Initialize Phase 14 type integration if available
            if (typeRegistry != null && typeIntegration == null) {
                typeIntegration = new GraalVMTypeIntegration(typeRegistry);
                typeIntegration.initialize();
                LOGGER.info("Phase 14 GraalVM type integration initialized");
            }
            
            // Initialize the define function
            defineFunction = new TsModDefineFunction(modRegistry);
            
            // Create JavaScript context with Phase 17 hardening
            jsContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.NONE)
                .allowHostClassLookup(className -> false)
                .allowIO(false)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .allowEnvironmentAccess(EnvironmentAccess.NONE)
                .allowExperimentalOptions(true)
                .option("js.console", "true")
                .option("js.print", "true")
                .build();
            
            // Phase 17: Inject SafeTapestryBridge instead of full API
            RpcClientFacade rpcFacade = getRpcClientFacade();
            SafeTapestryBridge bridge = new SafeTapestryBridge(rpcFacade);
            
            // Create mod object with define function
            Value modObject = jsContext.eval("js", "({ define: function() {} })");
            Value defineFunctionValue = jsContext.asValue((ProxyExecutable) args -> {
                if (args.length != 1) {
                    throw new IllegalArgumentException("tapestry.mod.define requires exactly one argument");
                }
                defineFunction.define(args[0]);
                return null;
            });
            modObject.putMember("define", defineFunctionValue);
            
            // Create tapestry object with both bridge and mod
            Value tapestryObject = jsContext.eval("js", "({})");
            tapestryObject.putMember("bridge", bridge);
            tapestryObject.putMember("mod", modObject);
            
            // Only expose the tapestry object to JavaScript
            LOGGER.info("=== DIAGNOSTIC: Bridge injection context identity: {}", System.identityHashCode(jsContext));
            jsContext.getBindings("js").putMember("tapestry", tapestryObject);
            
            LOGGER.info("=== DIAGNOSTIC: Bindings after injection: {}", jsContext.getBindings("js").getMemberKeys());
            
            // Store API and hookRegistry for later extension in TS_READY
            // We'll extend the tapestry object when we reach TS_READY phase
            // For now, just run a sanity check
            runSanityCheck();
            
            initialized = true;
            LOGGER.info("TypeScript runtime initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize TypeScript runtime", e);
            throw new RuntimeException("TypeScript runtime initialization failed", e);
        }
    }
    
    /**
     * Phase 17: Gets RPC client facade for secure bridge.
     * Returns null if not available (e.g., server-side).
     */
    private RpcClientFacade getRpcClientFacade() {
        // Try to get existing RPC client runtime
        try {
            RpcClientRuntime clientRuntime = RpcClientRuntime.getInstance();
            if (clientRuntime != null) {
                return clientRuntime; // Implements RpcClientFacade
            }
        } catch (Exception e) {
            LOGGER.debug("No RPC client runtime available: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Extends the tapestry object for TS_READY phase.
     * This should be called when transitioning to TS_READY phase.
     * 
     * @param hookRegistry the hook registry for hook registration
     * @param modRegistry the mod registry for source information
     */
    public void extendForReadyPhase(HookRegistry hookRegistry, TsModRegistry modRegistry) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_READY);
        
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            // Extend the tapestry object with hook APIs
            extendTapestryObjectForReady(hookRegistry, modRegistry);
            
            // Run TS_READY phase sanity check
            runSanityCheckForPhase(TapestryPhase.TS_READY);
            
            LOGGER.info("TypeScript runtime extended for TS_READY phase");
            
        } catch (Exception e) {
            LOGGER.error("Failed to extend TypeScript runtime for TS_READY", e);
            throw new RuntimeException("Failed to extend TypeScript runtime for TS_READY", e);
        }
    }
    
    /**
     * Builds the tapestry object combining mod.define with extension APIs.
     * 
     * @param apiTree the frozen ProxyObject tree from extension registration
     * @return complete bindings map for JavaScript context
     */
    private Object buildTapestryObjectWithExtensions(ProxyObject apiTree) {
        // Create console namespace with ProxyExecutable functions
        Map<String, Object> console = new HashMap<>();
        console.put("log", (ProxyExecutable) args -> {
            if (args.length > 0) {
                LOGGER.info("[JS LOG] {}", args[0]);
            }
            return null;
        });
        console.put("warn", (ProxyExecutable) args -> {
            if (args.length > 0) {
                LOGGER.warn("[JS WARN] {}", args[0]);
            }
            return null;
        });
        console.put("error", (ProxyExecutable) args -> {
            if (args.length > 0) {
                LOGGER.error("[JS ERROR] {}", args[0]);
            }
            return null;
        });
        
        // Create tapestry.mod namespace with define function
        Map<String, Object> mod = new HashMap<>();
        mod.put("define", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("tapestry.mod.define requires exactly one argument");
            }
            
            Value modDefinition = args[0];
            defineFunction.define(modDefinition);
            return null;
        });
        
        // Build complete tapestry object
        Map<String, Object> tapestry = new HashMap<>();
        tapestry.put("mod", ProxyObject.fromMap(mod));
        
        // Merge extension APIs into the tapestry object
        if (apiTree != null) {
            // Copy all members from the extension API tree
            Object keys = apiTree.getMemberKeys();
            if (keys instanceof Object[]) {
                Object[] keyArray = (Object[]) keys;
                for (Object keyObj : keyArray) {
                    String key = keyObj.toString();
                    tapestry.put(key, apiTree.getMember(key));
                }
            }
        }
        
        // Return the complete bindings structure
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("tapestry", ProxyObject.fromMap(tapestry));
        bindings.put("console", ProxyObject.fromMap(console));
        
        return bindings;
    }
    
    /**
     * Creates the worldgen.onResolveBlock() function for JavaScript.
     */
    private ProxyExecutable createWorldgenOnResolveBlockFunction(TsWorldgenApi worldgenApi) {
        return args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("worldgen.onResolveBlock() requires exactly one argument (handler function)");
            }
            
            Value handler = args[0];
            if (!handler.canExecute()) {
                throw new IllegalArgumentException("worldgen.onResolveBlock() requires a function as argument");
            }
            
            // Get current mod ID for registration
            String currentModId = getCurrentModId();
            if (currentModId == null) {
                throw new IllegalStateException("worldgen.onResolveBlock() must be called from within a mod context");
            }
            
            // Register the hook
            worldgenApi.onResolveBlock(handler);
            
            return null;
        };
    }
    
    /**
     * Extends the tapestry object for TS_READY phase with hook APIs.
     * 
     * @param hookRegistry the hook registry for hook registration
     * @param modRegistry the mod registry for source information
     */
    private void extendTapestryObjectForReady(HookRegistry hookRegistry, TsModRegistry modRegistry) {
        // Get the existing tapestry object
        Value tapestryValue = jsContext.getBindings("js").getMember("tapestry");
        
        // Create worldgen API instance for TS_READY phase
        TsWorldgenApi worldgenApi = new TsWorldgenApi(hookRegistry, modRegistry);
        Map<String, Object> worldgenNamespace = new HashMap<>();
        worldgenNamespace.put("onResolveBlock", createWorldgenOnResolveBlockFunction(worldgenApi));
        
        // Add worldgen namespace to tapestry object
        tapestryValue.putMember("worldgen", ProxyObject.fromMap(worldgenNamespace));
        
        LOGGER.debug("Extended tapestry object for TS_READY phase with worldgen API");
    }
    
    /**
     * Extends the tapestry object for RUNTIME phase with Phase 6 APIs.
     * This should be called when transitioning to RUNTIME phase.
     * 
     * @param schedulerService the scheduler service
     * @param eventBus the event bus
     * @param configService the config service
     * @param stateService the state service
     * @param modRegistry the mod registry for source information
     */
    public void extendForRuntime(SchedulerService schedulerService, EventBus eventBus, 
                                 ConfigService configService, ModStateService stateService, 
                                 TsModRegistry modRegistry) {
        PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
        
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            // Extend the tapestry object with Phase 6 APIs
            extendTapestryObjectForRuntime(schedulerService, eventBus, configService, stateService, modRegistry);
            
            // Run RUNTIME phase sanity check
            runSanityCheckForPhase(TapestryPhase.RUNTIME);
            
            LOGGER.info("TypeScript runtime extended for RUNTIME phase");
            
        } catch (Exception e) {
            LOGGER.error("Failed to extend TypeScript runtime for RUNTIME", e);
            throw new RuntimeException("Failed to extend TypeScript runtime for RUNTIME", e);
        }
    }
    
    /**
     * Extends the tapestry object for RUNTIME phase with Phase 6 APIs.
     * 
     * @param schedulerService the scheduler service
     * @param eventBus the event bus
     * @param configService the config service
     * @param stateService the state service
     * @param modRegistry the mod registry for source information
     */
    private void extendTapestryObjectForRuntime(SchedulerService schedulerService, EventBus eventBus, 
                                                 ConfigService configService, ModStateService stateService,
                                                 TsModRegistry modRegistry) {
        // Get the existing tapestry object
        Value tapestryValue = jsContext.getBindings("js").getMember("tapestry");
        
        // Create Phase 6 API instances
        TsSchedulerApi schedulerApi = new TsSchedulerApi(schedulerService);
        // Note: TsEventsApi removed as it's replaced by Phase 11 ModEventApi
        TsConfigApi configApi = new TsConfigApi(configService);
        TsStateApi stateApi = new TsStateApi(stateService);
        TsRuntimeApi runtimeApi = new TsRuntimeApi();
        
        // Create Phase 9 persistence API
        PersistenceApi persistenceApi = null;
        try {
            if (PersistenceService.getInstance().isInitialized()) {
                // Note: This will be per-mod, so we'll create it lazily when mods request it
                // For now, we'll inject a factory that creates per-mod instances
                persistenceApi = createPersistenceApiFactory();
            }
        } catch (IllegalStateException e) {
            // PersistenceService not initialized yet - this is expected during client startup
            LOGGER.debug("PersistenceService not yet initialized - skipping persistence API");
        }
        
        // Add Phase 6 namespaces
        tapestryValue.putMember("scheduler", schedulerApi.createNamespace());
        // Note: Old events namespace removed - replaced by mod.on/emit/off API
        tapestryValue.putMember("config", configApi.createNamespace());
        tapestryValue.putMember("state", stateApi.createNamespace());
        tapestryValue.putMember("runtime", runtimeApi.createNamespace());
        
        // Add Phase 9 persistence namespace if available
        if (persistenceApi != null) {
            tapestryValue.putMember("persistence", persistenceApi);
        }
        
        // Add Phase 11 event API to mod namespace during RUNTIME phase
        Value modValue = tapestryValue.getMember("mod");
        if (modValue != null) {
            ModEventApi modEventApi = new ModEventApi(eventBus);
            modValue.putMember("on", modEventApi.createModEventApi().getMember("on"));
            modValue.putMember("emit", modEventApi.createModEventApi().getMember("emit"));
            modValue.putMember("off", modEventApi.createModEventApi().getMember("off"));
        }
        
        // Add Phase 12 state API to mod namespace
        if (modValue != null) {
            com.tapestry.typescript.StateFactory stateFactory = new com.tapestry.typescript.StateFactory(eventBus);
            modValue.putMember("state", stateFactory.createStateNamespace());
        }
        
        // Add Phase 13 runtime capability API to mod namespace
        if (modValue != null) {
            com.tapestry.typescript.CapabilityApi capabilityApi = new com.tapestry.typescript.CapabilityApi();
            modValue.putMember("capability", capabilityApi.createRuntimeCapabilityNamespace());
        }
        
        LOGGER.debug("Extended tapestry object for RUNTIME phase with Phase 6-9 APIs");
    }
    
    /**
     * Creates a persistence API factory that provides per-mod persistence access.
     * 
     * @return a ProxyObject that creates per-mod persistence instances
     */
    private PersistenceApi createPersistenceApiFactory() {
        return new PersistenceApi() {
            @Override
            public Object getMember(String key) {
                if ("getModStore".equals(key)) {
                    return new ProxyExecutable() {
                        @Override
                        public Object execute(Value... arguments) {
                            if (arguments.length != 1) {
                                throw new IllegalArgumentException("getModStore() requires exactly 1 argument (modId)");
                            }
                            
                            String modId = arguments[0].asString();
                            String currentModId = getCurrentModId();
                            
                            if (!modId.equals(currentModId)) {
                                throw new IllegalArgumentException(
                                    "Mod '" + currentModId + "' cannot access persistence for mod '" + modId + "'");
                            }
                            
                            ModStateStore store = PersistenceService.getInstance().getModStateStore(modId);
                            return toHostValue(store);
                        }
                    };
                }
                
                return null; // Factory mode - only support specific methods
            }
            
            @Override
            public Object getMemberKeys() {
                return new String[]{"getModStore"};
            }
            
            @Override
            public boolean hasMember(String key) {
                return "getModStore".equals(key);
            }
        };
    }
    
    /**
     * Evaluates a JavaScript script from given source.
     * 
     * @param source JavaScript source code
     * @param sourceName name of source (for error reporting)
     * @throws RuntimeException if evaluation fails
     */
    public void evaluateScript(String source, String sourceName) {
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        // Set current source for context tracking
        setExecutionContext(null, ExecutionContextMode.NONE, sourceName);
        
        try {
            LOGGER.info("=== DIAGNOSTIC: About to evaluate script: {}", sourceName);
            LOGGER.info("=== DIAGNOSTIC: Script length: {}", source.length());
            LOGGER.info("=== DIAGNOSTIC: Context identity: {}", System.identityHashCode(jsContext));
            LOGGER.info("=== DIAGNOSTIC: Bindings before eval: {}", jsContext.getBindings("js").getMemberKeys());
            LOGGER.info("=== DIAGNOSTIC: Available languages: {}", jsContext.getEngine().getLanguages().keySet());
            
            // Test basic JS execution
            Value basicTest = jsContext.eval("js", "1 + 1");
            LOGGER.info("=== DIAGNOSTIC: Basic test (1+1) result: {}", basicTest);
            
            // Test console availability
            Value consoleTest = jsContext.eval("js", "typeof console");
            LOGGER.info("=== DIAGNOSTIC: typeof console: {}", consoleTest);
            
            // Test print function
            Value printTest = jsContext.eval("js", "typeof print");
            LOGGER.info("=== DIAGNOSTIC: typeof print: {}", printTest);
            
            // Test console output with alternative
            jsContext.eval("js", "if (typeof console !== 'undefined') { console.log('=== DIAGNOSTIC: Console test works! ==='); } else if (typeof print !== 'undefined') { print('=== DIAGNOSTIC: Print test works! ==='); } else { throw new Error('No console or print available'); }");
            
            // Test tapestry bridge visibility
            Value tapestryTest = jsContext.eval("js", "typeof tapestry");
            LOGGER.info("=== DIAGNOSTIC: typeof tapestry: {}", tapestryTest);
            
            Source src = Source.newBuilder("js", source, sourceName).build();
            Value result = jsContext.eval(src);
            
            LOGGER.info("=== DIAGNOSTIC: Eval completed successfully");
            LOGGER.info("=== DIAGNOSTIC: Script result: {}", result);
            
            // Test registry mutation after eval
            LOGGER.info("=== DIAGNOSTIC: Checking registry mutation...");
            
            // Check if tapestry bridge was called
            Value bindings = jsContext.getBindings("js");
            LOGGER.info("=== DIAGNOSTIC: Has tapestry: {}", bindings.hasMember("tapestry"));
            if (bindings.hasMember("tapestry")) {
                Value tapestryObj = bindings.getMember("tapestry");
                LOGGER.info("=== DIAGNOSTIC: tapestry.mod: {}", tapestryObj.getMember("mod"));
            }
            
            // Test direct bridge call
            LOGGER.info("=== DIAGNOSTIC: Testing direct bridge call...");
            try {
                Value directTest = jsContext.eval("js", "tapestry.mod.define({id: 'direct-test', version: '1.0.0'});");
                LOGGER.info("=== DIAGNOSTIC: Direct bridge call result: {}", directTest);
            } catch (Exception e) {
                LOGGER.error("=== DIAGNOSTIC: Direct bridge call failed", e);
            }
            
            LOGGER.debug("Successfully evaluated script: {}", sourceName);
        } catch (Throwable t) {
            System.err.println("=== DIAGNOSTIC: JS EVALUATION FAILED HARD ===");
            t.printStackTrace();
            LOGGER.error("Failed to evaluate script: {}", sourceName, t);
            throw new RuntimeException("Script evaluation failed: " + sourceName, t);
        } finally {
            // Clear current source
            clearExecutionContext();
        }
    }
    
    /**
     * Executes a onLoad function for a given mod.
     * 
     * @param onLoad Value function to execute
     * @param modId mod ID for context tracking
     */
    public void executeOnLoad(Value onLoad, String modId) {
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        // Phase enforcement: only allowed during TS_READY
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_READY);
        
        if (onLoad == null || onLoad.isNull()) {
            LOGGER.warn("Mod {} has no onLoad function to execute", modId);
            return;
        }
        
        // Verify it's executable
        if (!onLoad.canExecute()) {
            throw new IllegalArgumentException("onLoad must be an executable function");
        }
        
        // Set current mod ID and source for context tracking
        String source = "unknown"; // Default fallback
        setExecutionContext(modId, ExecutionContextMode.ON_LOAD, source);
        
        try {
            // Get the JS tapestry object to pass to onLoad
            Value tapestryObject = jsContext.getBindings("js").getMember("tapestry");
            
            // Execute onLoad function with JS tapestry object
            onLoad.executeVoid(tapestryObject);
            
            LOGGER.info("Successfully executed onLoad for mod: {}", modId);
        } catch (Exception e) {
            LOGGER.error("Failed to execute onLoad for mod: {}", modId, e);
            throw new RuntimeException("Failed to execute onLoad for mod: " + modId, e);
        } finally {
            // Clear current mod ID
            clearExecutionContext();
        }
    }
    
    /**
     * Runs a simple sanity check to verify the runtime is working.
     * This checks that the tapestry object is accessible and has the expected structure.
     */
    private void runSanityCheck() {
        runSanityCheckForPhase(TapestryPhase.TS_LOAD);
    }
    
    /**
     * Runs a phase-specific sanity check.
     * 
     * @param phase the phase to check against
     */
    private void runSanityCheckForPhase(TapestryPhase phase) {
        LOGGER.debug("Running TypeScript runtime sanity check for phase: {}", phase);
        
        try {
            // Check that tapestry object exists
            Value tapestryObj = jsContext.eval("js", "typeof tapestry");
            if (!tapestryObj.asString().equals("object")) {
                throw new RuntimeException("tapestry object is not accessible");
            }
            
            // Check that console functions exist (available in all phases)
            Value consoleLog = jsContext.eval("js", "typeof console.log");
            if (!consoleLog.asString().equals("function")) {
                throw new RuntimeException("console.log is not a function");
            }
            
            // Only check mod.define in phases where it's available (TS_REGISTER and later)
            if (phase.ordinal() >= TapestryPhase.TS_REGISTER.ordinal()) {
                Value modDefine = jsContext.eval("js", "typeof tapestry.mod.define");
                if (!modDefine.asString().equals("function")) {
                    throw new RuntimeException("tapestry.mod.define is not a function");
                }
            }
            
            // Phase-specific checks
            if (phase == TapestryPhase.TS_READY || phase == TapestryPhase.RUNTIME) {
                // Check that worldgen.onResolveBlock exists (only available from TS_READY onwards)
                Value worldgenHook = jsContext.eval("js", "typeof tapestry.worldgen.onResolveBlock");
                if (!worldgenHook.asString().equals("function")) {
                    throw new RuntimeException("tapestry.worldgen.onResolveBlock is not a function");
                }
            }
            
            if (phase == TapestryPhase.CLIENT_PRESENTATION_READY) {
                // Check that client.overlay.register exists (only available from CLIENT_PRESENTATION_READY onwards)
                Value overlayRegister = jsContext.eval("js", "typeof tapestry.client.overlay.register");
                if (!overlayRegister.asString().equals("function")) {
                    throw new RuntimeException("tapestry.client.overlay.register is not a function");
                }
            }
            
            if (phase == TapestryPhase.CLIENT_PRESENTATION_READY) {
                // Check that tapestry.utils.mikel exists (only available from CLIENT_PRESENTATION_READY onwards)
                Value mikel = jsContext.eval("js", "typeof tapestry.utils.mikel");
                if (!mikel.asString().equals("function")) {
                    throw new RuntimeException("tapestry.utils.mikel is not a function");
                }
            }
            
            LOGGER.debug("Sanity check passed for phase: {}", phase);
            
        } catch (Exception e) {
            LOGGER.error("TypeScript runtime sanity check failed for phase: {}", phase, e);
            throw new RuntimeException("TypeScript runtime sanity check failed for phase: " + phase, e);
        }
    }
    
    /**
     * Gets the JavaScript context (for testing purposes).
     * 
     * @return the JavaScript context
     */
    public static Context getJsContext() {
        return jsContext;
    }
    
    /**
     * Checks if runtime is initialized.
     * 
     * @return true if runtime is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Extends the tapestry object for CLIENT_PRESENTATION_READY phase.
     * This should be called when transitioning to CLIENT_PRESENTATION_READY phase.
     */
    public void extendForClientPresentation() {
        PhaseController.getInstance().requirePhase(TapestryPhase.CLIENT_PRESENTATION_READY);
        
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            // Load Mikel templating library from embedded resources
            loadMikelLibrary();
            
            // Initialize overlay system
            OverlayRegistry overlayRegistry = OverlayRegistry.getInstance();
            OverlayApi overlayApi = new OverlayApi(overlayRegistry);
            
            // Get the existing tapestry object
            Value tapestryValue = jsContext.getBindings("js").getMember("tapestry");
            
            // Create client namespace if it doesn't exist
            Value clientValue;
            if (tapestryValue.hasMember("client")) {
                clientValue = tapestryValue.getMember("client");
            } else {
                clientValue = jsContext.asValue(ProxyObject.fromMap(new HashMap<>()));
                tapestryValue.putMember("client", clientValue);
            }
            
            // Create mod namespace in client if it doesn't exist
            Value modValue;
            if (clientValue.hasMember("mod")) {
                modValue = clientValue.getMember("mod");
            } else {
                modValue = jsContext.asValue(ProxyObject.fromMap(new HashMap<>()));
                clientValue.putMember("mod", modValue);
            }
            
            // Add client-side players API
            ClientPlayersApi clientPlayersApi = new ClientPlayersApi();
            clientValue.putMember("players", clientPlayersApi.createNamespace());
            
            // Add Phase 11 event API to mod namespace
            EventBus eventBus = com.tapestry.TapestryMod.getEventBus();
            if (eventBus != null) {
                ModEventApi modEventApi = new ModEventApi(eventBus);
                modValue.putMember("on", modEventApi.createModEventApi().getMember("on"));
                modValue.putMember("emit", modEventApi.createModEventApi().getMember("emit"));
                modValue.putMember("off", modEventApi.createModEventApi().getMember("off"));
            }
            
            // Add Phase 12 state API to mod namespace
            if (eventBus != null) {
                com.tapestry.typescript.StateFactory stateFactory = new com.tapestry.typescript.StateFactory(eventBus);
                modValue.putMember("state", stateFactory.createStateNamespace());
            }
            
            // Add Phase 13 capability API to mod namespace
            if (eventBus != null) {
                com.tapestry.typescript.CapabilityApi capabilityApi = new com.tapestry.typescript.CapabilityApi();
                modValue.putMember("capability", capabilityApi.createRuntimeCapabilityNamespace());
            }
            
            // Initialize overlay renderer
            com.tapestry.overlay.OverlayRenderer.getInstance(overlayRegistry);
            
            // Run CLIENT_PRESENTATION_READY phase sanity check
            runSanityCheckForPhase(TapestryPhase.CLIENT_PRESENTATION_READY);
            
            LOGGER.info("TypeScript runtime extended for CLIENT_PRESENTATION_READY phase");
            
        } catch (Exception e) {
            LOGGER.error("Failed to extend TypeScript runtime for CLIENT_PRESENTATION_READY", e);
            throw new RuntimeException("Failed to extend TypeScript runtime for CLIENT_PRESENTATION_READY", e);
        }
    }
    
    /**
     * Loads the Mikel templating library from embedded resources.
     * 
     * @throws RuntimeException if loading fails
     */
    private void loadMikelLibrary() {
        final String MIKEL_RESOURCE_PATH = "/tapestry/mikel/mikel.js";
        final String MIKEL_VERSION = "v0.32.0";
        
        try {
            LOGGER.info("Loading Mikel templating library version {} from embedded resources...", MIKEL_VERSION);
            
            // Load the library from embedded resources
            String mikelSource;
            try (InputStream inputStream = TypeScriptRuntime.class.getResourceAsStream(MIKEL_RESOURCE_PATH)) {
                if (inputStream == null) {
                    throw new IOException("Mikel library not found at resource path: " + MIKEL_RESOURCE_PATH);
                }
                byte[] bytes = inputStream.readAllBytes();
                mikelSource = new String(bytes, StandardCharsets.UTF_8);
            }
            
            // Load the library into the JavaScript context
            evaluateScript(mikelSource, "mikel");
            
            // Expose the mikel function as tapestry.utils.mikel
            String setupScript = """
                // Create tapestry.utils namespace if it doesn't exist
                if (typeof tapestry.utils === 'undefined') {
                    tapestry.utils = {};
                }
                
                // Expose mikel function
                tapestry.utils.mikel = mikel;
                
                // Log that mikel is available
                console.log('Mikel templating library is now available as tapestry.utils.mikel');
                """;
            
            evaluateScript(setupScript, "mikel-setup");
            
            LOGGER.info("Successfully loaded Mikel library version {}", MIKEL_VERSION);
            
        } catch (Exception e) {
            LOGGER.error("Failed to load Mikel library", e);
            throw new RuntimeException("Mikel library loading failed", e);
        }
    }
    
    /**
     * Extends the tapestry object for TS_REGISTER phase.
     * This should be called when transitioning to TS_REGISTER phase.
     */
    public void extendForRegistration() {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            LOGGER.info("Extending TypeScript runtime for TS_REGISTER phase");
            
            // Initialize mod registry
            ModRegistry modRegistry = ModRegistry.getInstance();
            modRegistry.beginRegistration();
            
            // Get the existing tapestry object
            Value tapestryValue = jsContext.getBindings("js").getMember("tapestry");
            
            // Create mod namespace with define API
            Map<String, Object> modNamespace = new HashMap<>();
            modNamespace.put("define", createModDefineFunction(modRegistry));
            
            // Add Phase 13 capability API to mod namespace during REGISTRATION only
            EventBus eventBus = com.tapestry.TapestryMod.getEventBus();
            if (eventBus != null) {
                com.tapestry.typescript.CapabilityApi capabilityApi = new com.tapestry.typescript.CapabilityApi();
                modNamespace.put("capability", capabilityApi.createCapabilityNamespace());
            }
            
            tapestryValue.putMember("mod", ProxyObject.fromMap(modNamespace));
            
            // Run TS_REGISTER phase sanity check
            runSanityCheckForPhase(TapestryPhase.TS_REGISTER);
            
            LOGGER.info("TypeScript runtime extended for TS_REGISTER phase");
            
        } catch (Exception e) {
            LOGGER.error("Failed to extend TypeScript runtime for TS_REGISTER", e);
            throw new RuntimeException("Failed to extend TypeScript runtime for TS_REGISTER", e);
        }
    }
    
    /**
     * Extends the tapestry object for TS_REGISTER phase with capability registration.
     * This should be called when transitioning to TS_REGISTER phase.
     * Phase 14: Also initializes GraalVM type integration.
     */
    public void extendForCapabilityRegistration() {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            LOGGER.info("Extending TypeScript runtime for TS_REGISTER phase (capability registration)");
            
            // Phase 14: Initialize type integration if available
            if (typeRegistry != null && typeIntegration == null) {
                typeIntegration = new GraalVMTypeIntegration(typeRegistry);
                typeIntegration.initialize();
                LOGGER.info("Phase 14 GraalVM type integration initialized");
            }
            
            // Replace SafeTapestryBridge with writable object at TS_REGISTER phase
            // SafeTapestryBridge is too restrictive for mod registration
            Map<String, Object> writableTapestry = new HashMap<>();
            
            // Create mod namespace with define API
            Map<String, Object> modNamespace = new HashMap<>();
            modNamespace.put("define", createModDefineFunction(ModRegistry.getInstance()));
            
            // Add Phase 13 capability API to mod namespace during TS_REGISTER only
            com.tapestry.typescript.CapabilityApi capabilityApi = new com.tapestry.typescript.CapabilityApi();
            modNamespace.put("capability", capabilityApi.createCapabilityNamespace());
            
            // Add runtime capability access API (will be validated by CapabilityRegistry.isFrozen())
            modNamespace.put("getCapability", capabilityApi.createRuntimeCapabilityNamespace());
            
            // Create writable tapestry object with mod namespace
            writableTapestry.put("mod", ProxyObject.fromMap(modNamespace));
            
            // Replace SafeTapestryBridge with writable object
            jsContext.getBindings("js").putMember("tapestry", ProxyObject.fromMap(writableTapestry));
            
            // Run TS_REGISTER phase sanity check
            runSanityCheckForPhase(TapestryPhase.TS_REGISTER);
            
            LOGGER.info("TypeScript runtime extended for TS_REGISTER phase (capability registration)");
            
        } catch (Exception e) {
            LOGGER.error("Failed to extend TypeScript runtime for TS_REGISTER", e);
            throw new RuntimeException("Failed to extend TypeScript runtime for TS_REGISTER", e);
        }
    }
    
    /**
     * Extends the tapestry object for TS_ACTIVATE phase.
     * This should be called when transitioning to TS_ACTIVATE phase.
     */
    public void extendForActivation() {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_ACTIVATE);
        
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            LOGGER.info("Extending TypeScript runtime for TS_ACTIVATE phase");
            
            // Get mod registry and create lifecycle manager
            ModRegistry modRegistry = ModRegistry.getInstance();
            
            // Phase 15: Create lifecycle manager to wrap mod registry
            if (lifecycleManager == null) {
                lifecycleManager = ExtensionLifecycleManager.create(modRegistry);
                LOGGER.info("Phase 15: ExtensionLifecycleManager created");
            }
            
            modRegistry.validateDependencies();
            List<ModDescriptor> activationOrder = modRegistry.buildActivationOrder();
            modRegistry.beginActivation();
            
            // Phase 15: Initialize all extensions to DISCOVERED state
            Set<String> extensionIds = new HashSet<>();
            for (ModDescriptor mod : activationOrder) {
                extensionIds.add(mod.getId());
            }
            lifecycleManager.initializeDiscoveredExtensions(extensionIds);
            
            // Phase 14: Authorize type imports for all mods
            if (typeIntegration != null) {
                for (ModDescriptor mod : activationOrder) {
                    // Get extension descriptor for this mod
                    // Note: This would need to be wired up with actual extension data
                    // For now, we'll authorize based on mod dependencies
                    var typeResolver = typeIntegration.getTypeResolver();
                    // TODO: Wire with actual extension typeImports data
                    LOGGER.debug("Phase 14: Would authorize type imports for mod: {}", mod.getId());
                }
            }
            
            // Get the existing tapestry object
            Value tapestryValue = jsContext.getBindings("js").getMember("tapestry");
            Value modValue = tapestryValue.getMember("mod");
            
            // Add export and require APIs to mod namespace
            modValue.putMember("export", createModExportFunction(modRegistry));
            modValue.putMember("require", createModRequireFunction(modRegistry));
            
            // Add Phase 11 event API to mod namespace
            // Note: EventBus will be available through TapestryMod.getInstance().getEventBus()
            EventBus eventBus = com.tapestry.TapestryMod.getEventBus();
            if (eventBus != null) {
                ModEventApi modEventApi = new ModEventApi(eventBus);
                modValue.putMember("on", modEventApi.createModEventApi().getMember("on"));
                modValue.putMember("emit", modEventApi.createModEventApi().getMember("emit"));
                modValue.putMember("off", modEventApi.createModEventApi().getMember("off"));
            }
            
            // Add Phase 12 state API to mod namespace
            if (eventBus != null) {
                com.tapestry.typescript.StateFactory stateFactory = new com.tapestry.typescript.StateFactory(eventBus);
                modValue.putMember("state", stateFactory.createStateNamespace());
            }
            
            // Activate all mods in dependency order
            activateMods(activationOrder);
            
            // Run TS_ACTIVATE phase sanity check
            runSanityCheckForPhase(TapestryPhase.TS_ACTIVATE);
            
            LOGGER.info("TypeScript runtime extended for TS_ACTIVATE phase");
            
        } catch (Exception e) {
            LOGGER.error("Failed to extend TypeScript runtime for TS_ACTIVATE", e);
            throw new RuntimeException("Failed to extend TypeScript runtime for TS_ACTIVATE", e);
        }
    }
    
    /**
     * Creates the mod.define() function for JavaScript.
     */
    private ProxyExecutable createModDefineFunction(ModRegistry modRegistry) {
        return args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("mod.define() requires exactly one argument");
            }
            
            Value definition = args[0];
            if (!definition.hasMember("id") || !definition.hasMember("version")) {
                throw new IllegalArgumentException("mod.define() requires 'id' and 'version' fields");
            }
            
            String id = definition.getMember("id").asString();
            String version = definition.getMember("version").asString();
            
            // Guard against duplicate define() calls from same mod context
            String currentModId = getCurrentModId();
            if (currentModId != null && definedMods.contains(currentModId)) {
                throw new IllegalStateException(
                    String.format("mod.define() already called for mod '%s'. Each mod may call define() exactly once.", currentModId));
            }
            
            // Parse dependencies
            List<String> dependsOn = new ArrayList<>();
            if (definition.hasMember("dependsOn")) {
                Value depsValue = definition.getMember("dependsOn");
                if (depsValue.hasArrayElements()) {
                    for (int i = 0; i < depsValue.getArraySize(); i++) {
                        dependsOn.add(depsValue.getArrayElement(i).asString());
                    }
                }
            }
            
            // Create mod descriptor
            ModDescriptor descriptor = new ModDescriptor(id, version, dependsOn, null, getCurrentSource());
            
            // Store lifecycle functions if present
            if (definition.hasMember("activate")) {
                descriptor.setActivateFunction(definition.getMember("activate"));
            }
            if (definition.hasMember("deactivate")) {
                descriptor.setDeactivateFunction(definition.getMember("deactivate"));
            }
            
            // Register the mod
            modRegistry.registerMod(descriptor);
            
            // Mark this mod as defined to prevent duplicate calls
            if (currentModId != null) {
                definedMods.add(currentModId);
            }
            
            return null;
        };
    }
    
    /**
     * Creates the mod.export() function for JavaScript.
     */
    private ProxyExecutable createModExportFunction(ModRegistry modRegistry) {
        return args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException("mod.export() requires exactly 2 arguments (key, value)");
            }
            
            String key = args[0].asString();
            Object value = fromValue(args[1]);
            String currentModId = getCurrentModId();
            
            if (currentModId == null) {
                throw new IllegalStateException("mod.export() must be called from within a mod context");
            }
            
            modRegistry.registerExport(currentModId, key, value);
            
            return null;
        };
    }
    
    /**
     * Creates the mod.require() function for JavaScript.
     */
    private ProxyExecutable createModRequireFunction(ModRegistry modRegistry) {
        return args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("mod.require() requires exactly 1 argument (modId)");
            }
            
            String modId = args[0].asString();
            String currentModId = getCurrentModId();
            
            if (currentModId == null) {
                throw new IllegalStateException("mod.require() must be called from within a mod context");
            }
            
            // Check if dependency is declared
            ModDescriptor currentMod = modRegistry.getMod(currentModId);
            if (currentMod == null || !currentMod.hasDependency(modId)) {
                throw new IllegalArgumentException("mod.require('" + modId + "') not declared in dependsOn for mod '" + currentModId + "'");
            }
            
            Object export = modRegistry.requireExport(modId, "default");
            return toHostValue(export);
        };
    }
    
    /**
     * Activates all mods in the specified order with Phase 15 lifecycle management.
     */
    private void activateMods(List<ModDescriptor> activationOrder) {
        LOGGER.info("Activating {} mods in dependency order with Phase 15 lifecycle", activationOrder.size());
        
        PerformanceMonitor performanceMonitor = PerformanceMonitor.getInstance();
        
        for (ModDescriptor mod : activationOrder) {
            String modId = mod.getId();
            PerformanceMonitor.ActivationTimer timer = performanceMonitor.startModActivationTiming(modId);
            
            try {
                LOGGER.debug("Phase 15: Activating mod: {}", modId);
                
                // Phase 15: Transition to VALIDATED state (already validated, just formalizing)
                lifecycleManager.transitionState(modId, ExtensionState.VALIDATED);
                
                // Phase 15: Transition to TYPE_INITIALIZED state (Phase 14 already done)
                lifecycleManager.transitionState(modId, ExtensionState.TYPE_INITIALIZED);
                
                // Phase 15: Transition to FROZEN state (registries sealed)
                lifecycleManager.transitionState(modId, ExtensionState.FROZEN);
                
                // Phase 15: Transition to LOADING state (runtime execution)
                lifecycleManager.transitionState(modId, ExtensionState.LOADING);
                
                // Existing mod activation logic
                mod.setState(ModDescriptor.ModState.ACTIVATING);
                
                if (mod.hasActivateFunction()) {
                    setExecutionContext(modId, ExecutionContextMode.ON_LOAD, mod.getSourcePath());
                    try {
                        // Phase 15: Execute with exception handling
                        mod.getActivateFunction().executeVoid();
                    } finally {
                        clearExecutionContext();
                    }
                }
                
                mod.setState(ModDescriptor.ModState.ACTIVE);
                
                // Phase 15: Transition to READY state (successful execution)
                lifecycleManager.transitionState(modId, ExtensionState.READY);
                
                timer.stop(); // This will check performance limits
                
                LOGGER.debug("Phase 15: Successfully activated mod: {}", modId);
                
            } catch (LifecycleViolationException e) {
                timer.stop();
                LOGGER.error("Phase 15: Lifecycle violation for mod {}: {}", modId, e.getMessage());
                mod.setState(ModDescriptor.ModState.FAILED);
                lifecycleManager.setFailureReason(modId, e.getMessage());
                throw new ModActivationException(modId, e);
                
            } catch (Exception e) {
                timer.stop(); // Still record the time even if failed
                
                // Phase 15: Handle any exception during LOADING
                LOGGER.error("Phase 15: Exception during activation of mod: {}", modId, e);
                
                try {
                    // Transition to FAILED state
                    lifecycleManager.transitionState(modId, ExtensionState.FAILED);
                    lifecycleManager.setFailureReason(modId, e.getClass().getSimpleName() + ": " + e.getMessage());
                } catch (LifecycleViolationException le) {
                    // This should not happen - FAILED is always allowed
                    LOGGER.error("Failed to transition mod {} to FAILED state: {}", modId, le.getMessage());
                }
                
                mod.setState(ModDescriptor.ModState.FAILED);
                throw new ModActivationException(modId, e);
            }
        }
        
        LOGGER.info("Phase 15: All mods activated with lifecycle management");
        
        // Phase 15: Log lifecycle diagnostics
        var diagnostics = lifecycleManager.getDiagnostics();
        LOGGER.info("Phase 15: Lifecycle diagnostics - Ready: {}, Failed: {}", 
            diagnostics.getStateCounts().get(ExtensionState.READY),
            diagnostics.getStateCounts().get(ExtensionState.FAILED));
    }
    
    /**
     * Evaluates a platform script (internal only).
     * 
     * @param source the script source
     * @param name the script name for debugging
     */
    public void evaluatePlatformScript(String source, String name) {
        PhaseController.getInstance().requirePhase(TapestryPhase.BOOTSTRAP);
        
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            evaluateScript(source, name);
            LOGGER.debug("Platform script evaluated: {}", name);
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate platform script: {}", name, e);
            throw new RuntimeException("Platform script evaluation failed: " + name, e);
        }
    }
    
    /**
     * Evaluates a mod script (internal only).
     * 
     * @param source the script source
     * @param name the script name for debugging
     */
    public void evaluateModScript(String source, String name) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            setExecutionContext(null, ExecutionContextMode.ON_LOAD, name);
            try {
                evaluateScript(source, name);
            } finally {
                clearExecutionContext();
            }
            LOGGER.debug("Mod script evaluated: {}", name);
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate mod script: {}", name, e);
            throw new RuntimeException("Mod script evaluation failed: " + name, e);
        }
    }
    
    /**
     * Extends tapestry object for Phase 16 RPC system.
     * This should be called when RPC system is initialized.
     */
    public void extendForRpcPhase() {
        PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
        
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            // Create RPC namespace using jsContext instead of Context.getCurrent()
            Value rpcNamespace = jsContext.asValue(RpcApi.createNamespace());
            // Create env namespace for side awareness using jsContext instead of Context.getCurrent()
            Value envNamespace = jsContext.asValue(EnvApi.createNamespace());
            // Inject into global tapestry object
            Value tapestry = jsContext.getBindings("js").getMember("tapestry");
            tapestry.putMember("rpc", rpcNamespace);
            tapestry.putMember("env", envNamespace);
            
            LOGGER.info("Phase 16 RPC API extensions loaded");
        } catch (Exception e) {
            LOGGER.error("Failed to extend TypeScript runtime for Phase 16", e);
            throw new RuntimeException("Phase 16 extension failed", e);
        }
    }
    
    /**
     * Extends tapestry object for EVENT phase.
     * This should be called when transitioning to EVENT phase.
     */
    public void extendForEventPhase() {
        PhaseController.getInstance().requirePhase(TapestryPhase.EVENT);
        
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            LOGGER.info("Extending TypeScript runtime for EVENT phase");
            
            // Get existing tapestry object
            Value tapestryValue = jsContext.getBindings("js").getMember("tapestry");
            
            // Get existing mod object
            Value modValue = tapestryValue.getMember("mod");
            
            // Add event API to mod namespace
            com.tapestry.events.EventBus eventBus = new com.tapestry.events.EventBus();
            ModEventApi modEventApi = new ModEventApi(eventBus);
            modValue.putMember("on", modEventApi.createModEventApi().getMember("on"));
            modValue.putMember("off", modEventApi.createModEventApi().getMember("off"));
            modValue.putMember("emit", modEventApi.createModEventApi().getMember("emit"));
            
            LOGGER.info("TypeScript runtime extended for EVENT phase");
            
        } catch (Exception e) {
            LOGGER.error("Failed to extend TypeScript runtime for EVENT phase", e);
            throw new RuntimeException("Failed to extend TypeScript runtime for EVENT phase", e);
        }
    }
    
    /**
     * Closes runtime and releases resources.
     */
    public void close() {
        if (jsContext != null) {
            jsContext.close();
            jsContext = null;
        }
        initialized = false;
        LOGGER.info("TypeScript runtime closed");
    }
    
    /**
     * Converts a Java object to a JavaScript value.
     * 
     * @param obj the Java object to convert
     * @return the JavaScript value
     */
    public static Value toHostValue(Object obj) {
        if (obj == null) {
            return jsContext.asValue(null);
        }
        return jsContext.asValue(obj);
    }
    
    /**
     * Converts a JavaScript value to a Java object.
     * 
     * @param value the JavaScript value to convert
     * @return the Java object
     */
    public static Object fromValue(Value value) {
        if (value.isNull()) {
            return null;
        }
        return value.as(Object.class);
    }
}
