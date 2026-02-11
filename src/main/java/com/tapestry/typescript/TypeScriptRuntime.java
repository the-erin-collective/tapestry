package com.tapestry.typescript;

import com.tapestry.api.TapestryAPI;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * TypeScript runtime using GraalVM Polyglot JavaScript engine.
 * 
 * In Phase 1, this only initializes the runtime and injects the frozen API object.
 * No user TypeScript code is executed - this is purely infrastructure setup.
 */
public class TypeScriptRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeScriptRuntime.class);
    
    private Context jsContext;
    private boolean initialized = false;
    
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
            
            // Inject the frozen API object
            injectAPIObject(api);
            
            // Inject tapestry.mod.define function
            injectModDefineFunction(modRegistry);
            
            // Inject worldgen API with hook registration
            injectWorldgenApi(hookRegistry);
            
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
     * Injects tapestry.mod.define function into JavaScript context.
     * 
     * @param modRegistry the mod registry to register mods with
     */
    private void injectModDefineFunction(TsModRegistry modRegistry) {
        TsModDefineFunction defineFunction = new TsModDefineFunction(modRegistry);
        jsContext.getBindings("js").putMember("tapestry", 
            jsContext.asValue(
                bindings -> bindings.putMember("mod", 
                    jsContext.asValue(
                        modBindings -> modBindings.putMember("define", defineFunction::define)
                    )
                )
            )
        );
        
        LOGGER.debug("Injected tapestry.mod.define function");
    }
    
    /**
     * Injects worldgen API with hook registration into JavaScript context.
     * 
     * @param hookRegistry the hook registry for hook registration
     */
    private void injectWorldgenApi(HookRegistry hookRegistry) {
        TsWorldgenApi worldgenApi = new TsWorldgenApi(hookRegistry);
        Map<String, Object> worldgenApiObject = worldgenApi.createApiObject();
        
        // Inject worldgen API into tapestry object
        jsContext.eval("js", "tapestry.worldgen = Object.from(arguments[0])", "injectWorldgen", 
            jsContext.asValue(worldgenApiObject));
        
        LOGGER.debug("Injected tapestry.worldgen API");
    }
    
    /**
     * Injects the frozen API object and console into the JavaScript context.
     * 
     * @param api the frozen TapestryAPI to inject
     */
    private void injectAPIObject(TapestryAPI api) {
        // Inject the frozen API object
        jsContext.getBindings("js").putMember("tapestry", api);
        
        // Inject limited console object (log, warn, error only)
        Map<String, Object> console = new HashMap<>();
        console.put("log", createConsoleFunction("INFO"));
        console.put("warn", createConsoleFunction("WARN"));
        console.put("error", createConsoleFunction("ERROR"));
        jsContext.getBindings("js").putMember("console", console);
        
        LOGGER.debug("Injected tapestry API and console into JavaScript context");
    }
    
    /**
     * Creates a console function that logs at the specified level.
     * 
     * @param level the log level to use
     * @return console function
     */
    private Object createConsoleFunction(String level) {
        return (Object[] args) -> {
            StringBuilder message = new StringBuilder();
            for (Object arg : args) {
                if (message.length() > 0) {
                    message.append(" ");
                }
                message.append(arg != null ? arg.toString() : "null");
            }
            
            switch (level) {
                case "INFO" -> LOGGER.info(message.toString());
                case "WARN" -> LOGGER.warn(message.toString());
                case "ERROR" -> LOGGER.error(message.toString());
                default -> LOGGER.info(message.toString());
            }
        };
    }
    
    /**
     * Creates a JavaScript proxy object that represents the TapestryAPI structure.
     * This converts the Java Map-based API to a JavaScript object structure.
     * 
     * @param api the frozen API
     * @return a JavaScript Value representing the API
     */
    private Value createAPIProxy(TapestryAPI api) {
        // For Phase 1, we'll create a simple object structure
        // In later phases, this could be more sophisticated with proper proxy handling
        
        // Create the main tapestry object
        Map<String, Object> tapestryObject = Map.of(
            "worlds", api.getWorlds(),
            "worldgen", api.getWorldgen(),
            "events", api.getEvents(),
            "mods", api.getMods(),
            "core", api.getCore()
        );
        
        // Convert to a JavaScript value
        return jsContext.asValue(tapestryObject);
    }
    
    /**
     * Evaluates a JavaScript file from the given source.
     * 
     * @param source the JavaScript source to evaluate
     * @param sourceName the name of the source (for error reporting)
     * @throws RuntimeException if evaluation fails
     */
    public void evaluateScript(String source, String sourceName) {
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
        try {
            jsContext.eval("js", source, sourceName);
            LOGGER.debug("Successfully evaluated script: {}", sourceName);
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate script: {}", sourceName, e);
            throw new RuntimeException("Script evaluation failed: " + sourceName, e);
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
            Value tapestry = jsContext.eval("js", "typeof tapestry");
            if (!tapestry.asString().equals("object")) {
                throw new RuntimeException("Sanity check failed: tapestry is not an object");
            }
            
            // Check that core domains exist
            String checkScript = """
                tapestry.worlds !== null &&
                tapestry.worldgen !== null &&
                tapestry.events !== null &&
                tapestry.mods !== null &&
                tapestry.core !== null
                """;
            
            Value domainsExist = jsContext.eval("js", checkScript);
            if (!domainsExist.asBoolean()) {
                throw new RuntimeException("Sanity check failed: core domains are missing");
            }
            
            // Check that core contains phases
            Value phasesExist = jsContext.eval("js", "Array.isArray(tapestry.core.phases)");
            if (!phasesExist.asBoolean()) {
                throw new RuntimeException("Sanity check failed: core.phases is not an array");
            }
            
            LOGGER.info("TypeScript runtime sanity check passed");
            
        } catch (Exception e) {
            LOGGER.error("TypeScript runtime sanity check failed", e);
            throw new RuntimeException("Sanity check failed", e);
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
