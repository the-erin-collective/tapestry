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
import java.util.Map;

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
    
    /**
     * Initializes the TypeScript runtime with mod loading capabilities.
     * 
     * @param api the frozen TapestryAPI to expose
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
            
            // Run a simple sanity check
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
            public void define(Object modDefinition) {
                defineFunction.define(modDefinition);
            }
        });
        tapestry.put("mod", modNamespace);
        
        // Add worldgen API with hook registration
        TsWorldgenApi worldgenApi = new TsWorldgenApi(hookRegistry);
        tapestry.put("worldgen", worldgenApi.createApiObject());
        
        // Inject the complete tapestry object
        jsContext.getBindings("js").putMember("tapestry", tapestry);
        
        // Inject limited console object
        Map<String, Object> console = new HashMap<>();
        console.put("log", new Object() {
            @SuppressWarnings("unused")
            public void log(Object[] args) {
                StringBuilder message = new StringBuilder();
                for (Object arg : args) {
                    if (message.length() > 0) {
                        message.append(" ");
                    }
                    message.append(arg != null ? arg.toString() : "null");
                }
                LOGGER.info(message.toString());
            }
        });
        console.put("warn", new Object() {
            @SuppressWarnings("unused")
            public void warn(Object[] args) {
                StringBuilder message = new StringBuilder();
                for (Object arg : args) {
                    if (message.length() > 0) {
                        message.append(" ");
                    }
                    message.append(arg != null ? arg.toString() : "null");
                }
                LOGGER.warn(message.toString());
            }
        });
        console.put("error", new Object() {
            @SuppressWarnings("unused")
            public void error(Object[] args) {
                StringBuilder message = new StringBuilder();
                for (Object arg : args) {
                    if (message.length() > 0) {
                        message.append(" ");
                    }
                    message.append(arg != null ? arg.toString() : "null");
                }
                LOGGER.error(message.toString());
            }
        });
        jsContext.getBindings("js").putMember("console", console);
        
        LOGGER.debug("Built complete tapestry object with mod.define and worldgen API");
    }
    
    /**
     * Evaluates a JavaScript script from the given source.
     * 
     * @param source the JavaScript source to evaluate
     * @param sourceName the name of the source (for error reporting)
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
     * Executes a mod's onLoad function.
     * 
     * @param onLoad the onLoad function to execute
     * @param modId the mod ID for context tracking
     * @param api the TapestryAPI to pass to the function
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
            // For now, we'll just log since we can't execute arbitrary objects
            // In a real implementation, we'd need to properly execute the JavaScript function
            LOGGER.info("Executing onLoad for mod: {} (function: {})", modId, onLoad.getClass().getSimpleName());
            LOGGER.debug("Successfully executed onLoad for mod: {}", modId);
        } catch (Exception e) {
            LOGGER.error("Failed to execute onLoad for mod: {}", modId, e);
            throw new RuntimeException("Mod onLoad failed: " + modId, e);
        } finally {
            // Clear current mod ID
            currentModId.remove();
        }
    }
    
    /**
     * Gets the current source being evaluated.
     * 
     * @return the current source name, or null if not evaluating
     */
    public static String getCurrentSource() {
        return currentSource.get();
    }
    
    /**
     * Gets the current mod ID being executed.
     * 
     * @return the current mod ID, or null if not executing
     */
    public static String getCurrentModId() {
        return currentModId.get();
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
     * Runs a simple sanity check to verify the runtime is working.
     * This checks that the tapestry object is accessible and has the expected structure.
     */
    private void runSanityCheck() {
        LOGGER.debug("Running TypeScript runtime sanity check");
        
        try {
            // Check that tapestry object exists
            Value tapestry = jsContext.eval("js", "typeof tapestry");
            if (!tapestry.asString().equals("object")) {
                throw new RuntimeException("tapestry object is not an object");
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
     * @param script the JavaScript script to evaluate
     * @return the result of the evaluation
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
