package com.tapestry.typescript;

import com.tapestry.api.TapestryAPI;
import com.tapestry.hooks.HookRegistry;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
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
    
    private Context jsContext;
    private boolean initialized = false;
    
    // ThreadLocal context for tracking current script and mod
    private static final ThreadLocal<String> currentSource = new ThreadLocal<>();
    private static final ThreadLocal<String> currentModId = new ThreadLocal<>();
    
    // Track sources that have already defined a mod (one-define-per-file rule)
    private static final Set<String> sourcesWithModDefine = new HashSet<>();
    
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
        
        if (!api.isFrozen()) {
            throw new IllegalStateException("API must be frozen before initializing TypeScript runtime");
        }
        
        LOGGER.info("Initializing TypeScript runtime with GraalVM Polyglot");
        
        try {
            // Create JavaScript context with tightened security for Phase 2
            // SECURITY NOTE: HostAccess.NONE is now enforced for user mod execution
            // Only tapestry API and console are exposed - no arbitrary Java access
            jsContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.NONE) // RESTRICTED - no direct Java access
                .allowHostClassLookup(s -> false) // RESTRICTED - no class loading
                .allowIO(false) // RESTRICTED - no file system access
                .build();
            
            // Build the complete tapestry object
            buildTapestryObject(api, modRegistry, hookRegistry);
            
            // Run a sanity check to ensure the runtime is working
            runSanityCheck();
            
            initialized = true;
            LOGGER.info("TypeScript runtime initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize TypeScript runtime", e);
            throw new RuntimeException("TypeScript runtime initialization failed", e);
        }
    }
    
    /**
     * Builds the complete tapestry object with all sub-objects.
     * 
     * @param api the frozen API
     * @param modRegistry the mod registry
     * @param hookRegistry the hook registry
     */
    private void buildTapestryObject(TapestryAPI api, TsModRegistry modRegistry, HookRegistry hookRegistry) {
        // Create the main tapestry object
        Map<String, Object> tapestry = new HashMap<>();
        
        // Add core domains
        tapestry.put("worlds", api.getWorlds());
        tapestry.put("worldgen", api.getWorldgen());
        tapestry.put("events", api.getEvents());
        tapestry.put("core", api.getCore());
        tapestry.put("mods", api.getMods());
        
        // Add mod.define function
        Map<String, Object> modNamespace = new HashMap<>();
        TsModDefineFunction defineFunction = new TsModDefineFunction(modRegistry);
        modNamespace.put("define", new Object() {
            @SuppressWarnings("unused")
            public Object define(Object modDefinition) {
                defineFunction.define(modDefinition);
                return null;
            }
        });
        tapestry.put("mod", modNamespace);
        
        // Add worldgen API with hook registration
        TsWorldgenApi worldgenApi = new TsWorldgenApi(hookRegistry);
        tapestry.put("worldgen", worldgenApi.createApiObject());
        
        // Inject the complete tapestry object
        jsContext.getBindings("js").putMember("tapestry", tapestry);
        
        // Inject console object
        Map<String, Object> console = new HashMap<>();
        console.put("log", new Object() {
            @SuppressWarnings("unused")
            public Object log(Object[] args) {
                StringBuilder message = new StringBuilder();
                for (Object arg : args) {
                    if (message.length() > 0) {
                        message.append(" ");
                    }
                    message.append(arg != null ? arg.toString() : "null");
                }
                LOGGER.info(message.toString());
                return null;
            }
        });
        console.put("warn", new Object() {
            @SuppressWarnings("unused")
            public Object warn(Object[] args) {
                StringBuilder message = new StringBuilder();
                for (Object arg : args) {
                    if (message.length() > 0) {
                        message.append(" ");
                    }
                    message.append(arg != null ? arg.toString() : "null");
                }
                LOGGER.warn(message.toString());
                return null;
            }
        });
        console.put("error", new Object() {
            @SuppressWarnings("unused")
            public Object error(Object[] args) {
                StringBuilder message = new StringBuilder();
                for (Object arg : args) {
                    if (message.length() > 0) {
                        message.append(" ");
                    }
                    message.append(arg != null ? arg.toString() : "null");
                }
                LOGGER.error(message.toString());
                return null;
            }
        });
        jsContext.getBindings("js").putMember("console", console);
        
        LOGGER.debug("Built complete tapestry object with JS-compatible API");
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
     * @param api TapestryAPI to pass to the function
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
            // Convert Object to Value for execution
            Value onLoadFunction = Value.asValue(onLoad);
            
            // Verify it's executable
            if (!onLoadFunction.canExecute()) {
                throw new IllegalArgumentException("onLoad must be an executable function");
            }
            
            // Execute onLoad function with the API object
            // The API object should be the same one exposed to JavaScript
            onLoadFunction.executeVoid(api);
            
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
     * Marks that a mod has been defined in the given source.
     * 
     * @param source the source where the mod was defined
     */
    public static void markModDefinedInSource(String source) {
        synchronized (sourcesWithModDefine) {
            sourcesWithModDefine.add(source);
        }
    }
    
    /**
     * Gets the JavaScript context (for testing purposes).
     * 
     * @return the JavaScript context
     */
    public Context getJsContext() {
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
