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
     * Initializes the TypeScript runtime.
     * Must be called during TS_LOAD phase after the API is frozen.
     * 
     * @param api the frozen TapestryAPI to inject
     * @throws IllegalStateException if called outside TS_LOAD phase or if already initialized
     */
    public void initialize(TapestryAPI api) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_LOAD);
        
        if (initialized) {
            LOGGER.warn("TypeScript runtime already initialized");
            return;
        }
        
        if (!api.isFrozen()) {
            throw new IllegalStateException("API must be frozen before initializing TypeScript runtime");
        }
        
        LOGGER.info("Initializing TypeScript runtime with GraalVM Polyglot");
        
        try {
            // Create JavaScript context with host access enabled
            jsContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL) // Allow Java objects to be accessed from JS
                .allowHostClassLookup(s -> true) // Allow class loading
                .build();
            
            // Inject the frozen API object
            injectAPIObject(api);
            
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
     * Injects the frozen TapestryAPI object into the JavaScript context.
     * The API is exposed as a global 'tapestry' object.
     * 
     * @param api the frozen API to inject
     */
    private void injectAPIObject(TapestryAPI api) {
        LOGGER.debug("Injecting frozen API object into JavaScript context");
        
        // Create a proxy object that represents the API structure
        Value tapestryProxy = createAPIProxy(api);
        
        // Set it as a global variable in the JS context
        jsContext.getBindings("js").putMember("tapestry", tapestryProxy);
        
        LOGGER.debug("API object injected as global 'tapestry' variable");
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
     * 
     * @param script the JavaScript script to evaluate
     * @return the result of the evaluation
     * @throws IllegalStateException if the runtime is not initialized
     */
    public Value evaluate(String script) {
        if (!initialized) {
            throw new IllegalStateException("TypeScript runtime not initialized");
        }
        
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
