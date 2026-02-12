package com.tapestry.typescript;

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
     */
    public void extendForReadyPhase(HookRegistry hookRegistry) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_READY);
        
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            // Extend the tapestry object with hook APIs
            extendTapestryObjectForReady(hookRegistry);
            
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
            for (String key : apiTree.getMemberKeys()) {
                tapestry.put(key, apiTree.getMember(key));
            }
        }
        
        // Return the complete bindings structure
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("tapestry", ProxyObject.fromMap(tapestry));
        bindings.put("console", ProxyObject.fromMap(console));
        
        return bindings;
    }
    /**
     * Extends the tapestry object for TS_READY phase using proper GraalVM proxies.
     * 
     * @param hookRegistry the hook registry for hook registration
     */
    private void extendTapestryObjectForReady(HookRegistry hookRegistry) {
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
            
            Value handler = args[0];
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
     * Executes a onLoad function for a given mod.
     * 
     * @param onLoad Value function to execute
     * @param modId mod ID for context tracking
     */
    public void executeOnLoad(Value onLoad, String modId) {
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        if (onLoad == null || onLoad.isNull()) {
            LOGGER.warn("Mod {} has no onLoad function to execute", modId);
            return;
        }
        
        // Verify it's executable
        if (!onLoad.canExecute()) {
            throw new IllegalArgumentException("onLoad must be an executable function");
        }
        
        // Set current mod ID for context tracking
        currentModId.set(modId);
        
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
            currentModId.remove();
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
