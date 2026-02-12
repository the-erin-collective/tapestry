package com.tapestry.typescript;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * TypeScript API for runtime operations.
 * 
 * Provides structured logging and other runtime utilities
 * with mod context and fail-fast error handling.
 */
public class TsRuntimeApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TsRuntimeApi.class);
    
    /**
     * Creates the runtime namespace object.
     * 
     * @return ProxyObject with runtime functions
     */
    public ProxyObject createNamespace() {
        Map<String, Object> runtime = new HashMap<>();
        
        // Logging namespace
        Map<String, Object> log = new HashMap<>();
        
        log.put("info", (ProxyExecutable) args -> {
            if (args.length < 1) {
                throw new IllegalArgumentException("runtime.log.info requires at least 1 argument: (message, [context])");
            }
            
            String message = args[0].asString();
            if (message == null) {
                throw new IllegalArgumentException("Message must be a string");
            }
            
            TypeScriptRuntime.ExecutionContext context = TypeScriptRuntime.getCurrentContext();
            String modId = context.modId();
            String source = context.source();
            
            // Extract context if provided
            Map<String, Object> logContext = new HashMap<>();
            if (args.length > 1 && args[1] != null) {
                // For now, we'll just stringify the context object
                logContext.put("data", args[1].toString());
            }
            
            logContext.put("modId", modId);
            if (source != null) {
                logContext.put("source", source);
            }
            
            LOGGER.info("[TS RUNTIME] [{}] {} - {}", modId, message, logContext);
            return null;
        });
        
        log.put("warn", (ProxyExecutable) args -> {
            if (args.length < 1) {
                throw new IllegalArgumentException("runtime.log.warn requires at least 1 argument: (message, [context])");
            }
            
            String message = args[0].asString();
            if (message == null) {
                throw new IllegalArgumentException("Message must be a string");
            }
            
            TypeScriptRuntime.ExecutionContext context = TypeScriptRuntime.getCurrentContext();
            String modId = context.modId();
            String source = context.source();
            
            // Extract context if provided
            Map<String, Object> logContext = new HashMap<>();
            if (args.length > 1 && args[1] != null) {
                // For now, we'll just stringify the context object
                logContext.put("data", args[1].toString());
            }
            
            logContext.put("modId", modId);
            if (source != null) {
                logContext.put("source", source);
            }
            
            LOGGER.warn("[TS RUNTIME] [{}] {} - {}", modId, message, logContext);
            return null;
        });
        
        log.put("error", (ProxyExecutable) args -> {
            if (args.length < 1) {
                throw new IllegalArgumentException("runtime.log.error requires at least 1 argument: (message, [context])");
            }
            
            String message = args[0].asString();
            if (message == null) {
                throw new IllegalArgumentException("Message must be a string");
            }
            
            TypeScriptRuntime.ExecutionContext context = TypeScriptRuntime.getCurrentContext();
            String modId = context.modId();
            String source = context.source();
            
            // Extract context if provided
            Map<String, Object> logContext = new HashMap<>();
            if (args.length > 1 && args[1] != null) {
                // For now, we'll just stringify the context object
                logContext.put("data", args[1].toString());
            }
            
            logContext.put("modId", modId);
            if (source != null) {
                logContext.put("source", source);
            }
            
            LOGGER.error("[TS RUNTIME] [{}] {} - {}", modId, message, logContext);
            return null;
        });
        
        runtime.put("log", ProxyObject.fromMap(log));
        
        return ProxyObject.fromMap(runtime);
    }
}
