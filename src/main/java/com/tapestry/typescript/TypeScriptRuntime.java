package com.tapestry.typescript;

import com.tapestry.api.TapestryAPI;
import com.tapestry.hooks.HookRegistry;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
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
    
    private static Context jsContext;
    private static boolean initialized = false;
    
    // ThreadLocal context for tracking current script and mod
    private static final ThreadLocal<String> currentSource = new ThreadLocal<>();
    private static final ThreadLocal<String> currentModId = new ThreadLocal<>();
    
    // Track sources that have already defined a mod (one-define-per-file rule)
    private static final Set<String> sourcesWithModDefine = new HashSet<>();
    
    // Define function instance
    private static TsModDefineFunction defineFunction;
    
    /**
     * Initializes the TypeScript runtime with mod loading capabilities.
     * 
     * @param api frozen TapestryAPI to expose
     * @param modRegistry the mod registry for registration
     * @param hookRegistry the hook registry for hook registration
     */
    public void initializeForModLoading(TapestryAPI api, TsModRegistry modRegistry, HookRegistry hookRegistry) {
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
            
            // Build minimal tapestry object for TS_LOAD phase
            Object bindings = buildTapestryObjectForLoad();
            
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
     * @param api frozen TapestryAPI to expose
     * @param hookRegistry the hook registry for hook registration
     */
    public void extendForReadyPhase(TapestryAPI api, HookRegistry hookRegistry) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_READY);
        
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            // Extend the tapestry object with full API
            extendTapestryObjectForReady(api, hookRegistry);
            
            LOGGER.info("TypeScript runtime extended for TS_READY phase");
            
        } catch (Exception e) {
            LOGGER.error("Failed to extend TypeScript runtime for TS_READY", e);
            throw new RuntimeException("Failed to extend TypeScript runtime", e);
        }
    }
    
    /**
     * Builds the minimal tapestry object for TS_LOAD phase using proper GraalVM proxies.
     * Only exposes tapestry.mod.define and console functions.
     */
    private Object buildTapestryObjectForLoad() {
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
            
            Object modDefinition = args[0];
            defineFunction.define(modDefinition);
            return null;
        });
        
        // Build minimal tapestry object
        Map<String, Object> tapestry = new HashMap<>();
        tapestry.put("mod", ProxyObject.fromMap(mod));
        
        // Return the complete bindings structure
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("tapestry", ProxyObject.fromMap(tapestry));
        bindings.put("console", ProxyObject.fromMap(console));
        
        return bindings;
    }
    
    /**
     * Extends the tapestry object for TS_READY phase using proper GraalVM proxies.
     * 
     * @param api frozen API to expose
     * @param hookRegistry the hook registry for hook registration
     */
    private void extendTapestryObjectForReady(TapestryAPI api, HookRegistry hookRegistry) {
        // Create worldgen API instance
        TsWorldgenApi worldgenApi = new TsWorldgenApi(hookRegistry);
        
        // Get the existing tapestry object
        Value tapestryValue = jsContext.getBindings("js").getMember("tapestry");
        
        // Create worldgen namespace with ProxyExecutable functions
        Map<String, Object> worldgen = new HashMap<>();
        worldgen.put("onResolveBlock", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("tapestry.worldgen.onResolveBlock requires exactly one argument");
            }
            
            Object handler = args[0];
            worldgenApi.onResolveBlock(handler);
            return null;
        });
        
        // Extend the existing tapestry object with minimal Phase 2 domains
        tapestryValue.putMember("worldgen", ProxyObject.fromMap(worldgen));
        
        LOGGER.debug("Extended tapestry object for TS_READY phase");
    }
    
    /**
     * Evaluates a JavaScript script from the given source.
     * 
     * @param source JavaScript source code
     * @param sourceName name of the source (for error reporting)
     * @throws RuntimeException if evaluation fails
     */
    public void evaluateScript(String source, String sourceName) {
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        // Set current source for context tracking
        currentSource.set(sourceName);
        
        try {
            Source src = Source.newBuilder("js", source, sourceName).build();
            jsContext.eval(src);
            LOGGER.debug("Successfully evaluated script: {}", sourceName);
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate script: {}", sourceName, e);
            throw new RuntimeException("Script evaluation failed: " + sourceName, e);
        } finally {
            // Clear current source
            currentSource.remove();
        }
    }
    
    /**
     * Executes the onLoad function for a mod.
     * 
     * @param onLoad onLoad function to execute
     * @param modId mod ID for context tracking
     * @param api TapestryAPI to pass to the function (not used - we pass JS tapestry object instead)
     */
    public void executeOnLoad(Object onLoad, String modId, TapestryAPI api) {
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        if (onLoad == null) {
            LOGGER.warn("Mod {} has no onLoad function to execute", modId);
            return;
        }
        
        // Set current mod ID for context tracking
        currentModId.set(modId);
        
        try {
            // Convert Object to Value for execution using the JS context
            Value onLoadFunction = jsContext.asValue(onLoad);
            
            // Verify it's executable
            if (!onLoadFunction.canExecute()) {
                throw new IllegalArgumentException("onLoad must be an executable function");
            }
            
            // Get the JS tapestry object to pass to onLoad function
            // This is the same object exposed to JavaScript
            Value tapestryObject = jsContext.getBindings("js").getMember("tapestry");
            
            // Execute onLoad function with the JS tapestry object
            onLoadFunction.executeVoid(tapestryObject);
            
            LOGGER.info("Successfully executed onLoad for mod: {}", modId);
        } catch (Exception e) {
            LOGGER.error("Failed to execute onLoad for mod: {}", modId, e);
            throw new RuntimeException("Failed to execute onLoad for mod: " + modId, e);
        } finally {
            // Clear current mod ID
            currentModId.set(null);
        }
    }
    
    /**
     * Runs a simple sanity check to verify the runtime is working.
     * This checks that the tapestry object is accessible and has the expected structure.
     */
    private void runSanityCheck() {
        LOGGER.debug("Running TypeScript runtime sanity check");
        
        try {
            // Check that tapestry object exists
            Value tapestryObj = jsContext.eval("js", "typeof tapestry");
            if (!tapestryObj.asString().equals("object")) {
                throw new RuntimeException("tapestry object is not accessible");
            }
            
            // Check that mod.define exists
            Value modDefine = jsContext.eval("js", "typeof tapestry.mod.define");
            if (!modDefine.asString().equals("function")) {
                throw new RuntimeException("tapestry.mod.define is not a function");
            }
            
            // Check that worldgen.onResolveBlock exists
            Value worldgenHook = jsContext.eval("js", "typeof tapestry.worldgen.onResolveBlock");
            if (!worldgenHook.asString().equals("function")) {
                throw new RuntimeException("tapestry.worldgen.onResolveBlock is not a function");
            }
            
            LOGGER.debug("Sanity check passed");
            
        } catch (Exception e) {
            LOGGER.error("TypeScript runtime sanity check failed", e);
            throw new RuntimeException("TypeScript runtime sanity check failed", e);
        }
    }
    
    /**
     * Evaluates a JavaScript expression in the runtime context.
     * For Phase 1, this should only be used for internal validation.
     * Requires TS_READY phase or later.
     * 
     * @param script JavaScript script to evaluate
     * @return result of evaluation
     * @throws IllegalStateException if the runtime is not initialized or wrong phase
     */
    public Value evaluate(String script) {
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        PhaseController.getInstance().requireAtLeast(TapestryPhase.TS_READY);
        
        return jsContext.eval("js", script);
    }
    
    /**
     * Gets the current source being evaluated.
     * 
     * @return current source name, or null if not evaluating
     */
    public static String getCurrentSource() {
        return currentSource.get();
    }
    
    /**
     * Sets the current source being evaluated.
     * 
     * @param source the source name
     */
    public static void setCurrentSource(String source) {
        currentSource.set(source);
    }
    
    /**
     * Gets the current mod ID being executed.
     * 
     * @return current mod ID, or null if not executing
     */
    public static String getCurrentModId() {
        return currentModId.get();
    }
    
    /**
     * Sets the current mod ID being executed.
     * 
     * @param modId the mod ID
     */
    public static void setCurrentModId(String modId) {
        currentModId.set(modId);
    }
    
    /**
     * Checks if a mod has already been defined in the given source.
     * 
     * @param source the source to check
     * @return true if a mod was already defined in this source
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
    
    /**
     * Gets the JavaScript context (for testing purposes).
     * 
     * @return the JavaScript context
     */
    public static Context getJsContext() {
        return jsContext;
    }
    
    /**
     * Checks if the runtime is initialized.
     * 
     * @return true if the runtime is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Closes the runtime and releases resources.
     * Should be called during shutdown.
     */
    public void close() {
        if (jsContext != null) {
            try {
                jsContext.close();
                LOGGER.info("TypeScript runtime closed");
            } catch (Exception e) {
                LOGGER.warn("Error closing TypeScript runtime", e);
            } finally {
                jsContext = null;
                initialized = false;
            }
        }
    }
}
