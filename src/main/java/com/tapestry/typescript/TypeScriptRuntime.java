package com.tapestry.typescript;

import com.tapestry.config.ConfigService;
import com.tapestry.events.EventBus;
import com.tapestry.hooks.HookRegistry;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.persistence.PersistenceService;
import com.tapestry.persistence.ModStateStore;
import com.tapestry.persistence.PersistenceApi;
import com.tapestry.scheduler.SchedulerService;
import com.tapestry.state.ModStateService;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TypeScript runtime using GraalVM Polyglot for JavaScript execution.
 * 
 * This class manages the JavaScript execution context, API injection,
 * and script evaluation for TypeScript mods. In Phase 2, it provides
 * a secure sandboxed environment for mod loading and hook registration.
 */
public class TypeScriptRuntime {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeScriptRuntime.class);
    
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
    private static final ThreadLocal<ExecutionContext> currentContext = ThreadLocal.withInitial(() -> 
        new ExecutionContext(null, ExecutionContextMode.NONE, null)
    );

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
     * 
     * @param apiTree frozen ProxyObject tree to expose as tapestry object
     * @param modRegistry the mod registry for registration
     * @param hookRegistry the hook registry for hook registration
     */
    public void initializeForModLoading(ProxyObject apiTree, TsModRegistry modRegistry, HookRegistry hookRegistry) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_LOAD);
        
        if (initialized) {
            throw new IllegalStateException("TypeScript runtime already initialized");
        }
        
        try {
            // Clear source tracking for fresh runtime initialization
            clearSourceTracking();
            
            // Initialize the define function
            defineFunction = new TsModDefineFunction(modRegistry);
            
            // Create JavaScript context with HostAccess.NONE for security
            jsContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.NONE)
                .allowHostClassLookup(s -> false)
                .allowIO(false)
                .build();
            
            // Build tapestry object with mod.define + extension APIs
            Object bindings = buildTapestryObjectWithExtensions(apiTree);
            
            // Inject bindings into the context
            Map<String, Object> bindingsMap = (Map<String, Object>) bindings;
            for (Map.Entry<String, Object> entry : bindingsMap.entrySet()) {
                jsContext.getBindings("js").putMember(entry.getKey(), entry.getValue());
            }
            
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
     * Extends the tapestry object for TS_READY phase with hook APIs.
     * 
     * @param hookRegistry the hook registry for hook registration
     * @param modRegistry the mod registry for source information
     */
    private void extendTapestryObjectForReady(HookRegistry hookRegistry, TsModRegistry modRegistry) {
        // Get the existing tapestry object
        Value tapestryValue = jsContext.getBindings("js").getMember("tapestry");
        
        // Create hook API instance for TS_READY phase (hook registration)
        TsEventsApi eventsApi = new TsEventsApi(null, modRegistry); // No EventBus during TS_READY
        
        // Add events namespace
        tapestryValue.putMember("events", eventsApi.createNamespace());
        
        LOGGER.debug("Extended tapestry object for TS_READY phase with hook APIs");
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
        TsEventsApi eventsApi = new TsEventsApi(eventBus, modRegistry);
        TsConfigApi configApi = new TsConfigApi(configService);
        TsStateApi stateApi = new TsStateApi(stateService);
        TsRuntimeApi runtimeApi = new TsRuntimeApi();
        
        // Create Phase 9 persistence API
        PersistenceApi persistenceApi = null;
        if (PersistenceService.getInstance().isInitialized()) {
            // Note: This will be per-mod, so we'll create it lazily when mods request it
            // For now, we'll inject a factory that creates per-mod instances
            persistenceApi = createPersistenceApiFactory();
        }
        
        // Add Phase 6 namespaces
        tapestryValue.putMember("scheduler", schedulerApi.createNamespace());
        tapestryValue.putMember("events", eventsApi.createNamespace());
        tapestryValue.putMember("config", configApi.createNamespace());
        tapestryValue.putMember("state", stateApi.createNamespace());
        tapestryValue.putMember("runtime", runtimeApi.createNamespace());
        
        // Add Phase 9 persistence namespace if available
        if (persistenceApi != null) {
            tapestryValue.putMember("persistence", persistenceApi);
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
        
        // Phase enforcement: only allowed during TS_LOAD
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_LOAD);
        
        // Set current source for context tracking
        setExecutionContext(null, ExecutionContextMode.NONE, sourceName);
        
        try {
            Source src = Source.newBuilder("js", source, sourceName).build();
            jsContext.eval(src);
            LOGGER.debug("Successfully evaluated script: {}", sourceName);
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate script: {}", sourceName, e);
            throw new RuntimeException("Script evaluation failed: " + sourceName, e);
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
            
            // Check that mod.define exists (available in all phases)
            Value modDefine = jsContext.eval("js", "typeof tapestry.mod.define");
            if (!modDefine.asString().equals("function")) {
                throw new RuntimeException("tapestry.mod.define is not a function");
            }
            
            // Check that console functions exist (available in all phases)
            Value consoleLog = jsContext.eval("js", "typeof console.log");
            if (!consoleLog.asString().equals("function")) {
                throw new RuntimeException("console.log is not a function");
            }
            
            // Phase-specific checks
            if (phase == TapestryPhase.TS_READY || phase == TapestryPhase.RUNTIME) {
                // Check that worldgen.onResolveBlock exists (only available from TS_READY onwards)
                Value worldgenHook = jsContext.eval("js", "typeof tapestry.worldgen.onResolveBlock");
                if (!worldgenHook.asString().equals("function")) {
                    throw new RuntimeException("tapestry.worldgen.onResolveBlock is not a function");
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
