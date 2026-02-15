package com.tapestry.typescript;

import com.tapestry.events.EventBus;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.typescript.TsModRegistry;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * TypeScript API for event operations.
 * 
 * Provides event registration with deterministic ordering
 * and fail-fast error handling.
 */
public class TsEventsApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TsEventsApi.class);
    
    private final EventBus eventBus;
    private final TsModRegistry modRegistry;
    
    public TsEventsApi(EventBus eventBus, TsModRegistry modRegistry) {
        this.eventBus = eventBus;
        this.modRegistry = modRegistry;
    }
    
    /**
     * Creates the events namespace object.
     * 
     * @return ProxyObject with event functions
     */
    public ProxyObject createNamespace() {
        Map<String, Object> events = new HashMap<>();
        
        // Generic on function
        events.put("on", (ProxyExecutable) args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException("events.on requires exactly 2 arguments: (eventName, callback)");
            }
            
            String eventName = args[0].asString();
            Value callback = args[1];
            
            if (eventName == null || eventName.isBlank()) {
                throw new IllegalArgumentException("Event name must be a non-empty string");
            }
            
            if (callback == null || !callback.canExecute()) {
                throw new IllegalArgumentException("Second argument must be an executable function");
            }
            
            // Enforce event registration only during ON_LOAD execution
            TypeScriptRuntime.ExecutionContext context = TypeScriptRuntime.getCurrentContext();
            if (context.mode() != TypeScriptRuntime.ExecutionContextMode.ON_LOAD) {
                throw new IllegalStateException(
                    String.format("Event registration only allowed during onLoad execution. " +
                        "Tried to register '%s' from mod '%s' in mode '%s'", 
                        eventName, context.modId(), context.mode())
                );
            }
            
            String modId = context.modId();
            if (modId == null) {
                throw new IllegalStateException("No mod ID set in current context");
            }
            
            // Get source file from mod registry
            String source = "unknown";
            var mod = modRegistry.getMod(modId);
            if (mod != null) {
                source = mod.source();
            }
            
            eventBus.subscribe(modId, eventName, callback);
            return null;
        });
        
        // Player events namespace
        Map<String, Object> playerEvents = new HashMap<>();
        playerEvents.put("onJoin", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("events.player.onJoin requires exactly 1 argument: (callback)");
            }
            
            Value callback = args[0];
            validateEventCallback(callback);
            
            // Enforce event registration only during ON_LOAD execution
            TypeScriptRuntime.ExecutionContext context = TypeScriptRuntime.getCurrentContext();
            if (context.mode() != TypeScriptRuntime.ExecutionContextMode.ON_LOAD) {
                throw new IllegalStateException(
                    String.format("Event registration only allowed during onLoad execution. " +
                        "Tried to register 'playerJoin' from mod '%s' in mode '%s'", 
                        context.modId(), context.mode())
                );
            }
            
            String modId = context.modId();
            String source = getModSource(modId);
            
            eventBus.subscribe(modId, "playerJoin", callback);
            return null;
        });
        
        playerEvents.put("onQuit", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("events.player.onQuit requires exactly 1 argument: (callback)");
            }
            
            Value callback = args[0];
            validateEventCallback(callback);
            
            // Enforce event registration only during ON_LOAD execution
            TypeScriptRuntime.ExecutionContext context = TypeScriptRuntime.getCurrentContext();
            if (context.mode() != TypeScriptRuntime.ExecutionContextMode.ON_LOAD) {
                throw new IllegalStateException(
                    String.format("Event registration only allowed during onLoad execution. " +
                        "Tried to register 'playerQuit' from mod '%s' in mode '%s'", 
                        context.modId(), context.mode())
                );
            }
            
            String modId = context.modId();
            String source = getModSource(modId);
            
            eventBus.subscribe(modId, "playerQuit", callback);
            return null;
        });
        
        playerEvents.put("onChat", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("events.player.onChat requires exactly 1 argument: (callback)");
            }
            
            Value callback = args[0];
            validateEventCallback(callback);
            
            // Enforce event registration only during ON_LOAD execution
            TypeScriptRuntime.ExecutionContext context = TypeScriptRuntime.getCurrentContext();
            if (context.mode() != TypeScriptRuntime.ExecutionContextMode.ON_LOAD) {
                throw new IllegalStateException(
                    String.format("Event registration only allowed during onLoad execution. " +
                        "Tried to register 'playerChat' from mod '%s' in mode '%s'", 
                        context.modId(), context.mode())
                );
            }
            
            String modId = context.modId();
            String source = getModSource(modId);
            
            eventBus.subscribe(modId, "playerChat", callback);
            return null;
        });
        
        events.put("player", ProxyObject.fromMap(playerEvents));
        
        // Block events namespace
        Map<String, Object> blockEvents = new HashMap<>();
        blockEvents.put("onBreak", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("events.block.onBreak requires exactly 1 argument: (callback)");
            }
            
            Value callback = args[0];
            validateEventCallback(callback);
            
            // Enforce event registration only during ON_LOAD execution
            TypeScriptRuntime.ExecutionContext context = TypeScriptRuntime.getCurrentContext();
            if (context.mode() != TypeScriptRuntime.ExecutionContextMode.ON_LOAD) {
                throw new IllegalStateException(
                    String.format("Event registration only allowed during onLoad execution. " +
                        "Tried to register 'blockBreak' from mod '%s' in mode '%s'", 
                        context.modId(), context.mode())
                );
            }
            
            String modId = context.modId();
            String source = getModSource(modId);
            
            eventBus.subscribe(modId, "blockBreak", callback);
            return null;
        });
        
        blockEvents.put("onPlace", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("events.block.onPlace requires exactly 1 argument: (callback)");
            }
            
            Value callback = args[0];
            validateEventCallback(callback);
            
            // Enforce event registration only during ON_LOAD execution
            TypeScriptRuntime.ExecutionContext context = TypeScriptRuntime.getCurrentContext();
            if (context.mode() != TypeScriptRuntime.ExecutionContextMode.ON_LOAD) {
                throw new IllegalStateException(
                    String.format("Event registration only allowed during onLoad execution. " +
                        "Tried to register 'blockPlace' from mod '%s' in mode '%s'", 
                        context.modId(), context.mode())
                );
            }
            
            String modId = context.modId();
            String source = getModSource(modId);
            
            eventBus.subscribe(modId, "blockPlace", callback);
            return null;
        });
        
        events.put("block", ProxyObject.fromMap(blockEvents));
        
        // Server events namespace
        Map<String, Object> serverEvents = new HashMap<>();
        serverEvents.put("onStarted", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("events.server.onStarted requires exactly 1 argument: (callback)");
            }
            
            Value callback = args[0];
            validateEventCallback(callback);
            
            // Enforce event registration only during ON_LOAD execution
            TypeScriptRuntime.ExecutionContext context = TypeScriptRuntime.getCurrentContext();
            if (context.mode() != TypeScriptRuntime.ExecutionContextMode.ON_LOAD) {
                throw new IllegalStateException(
                    String.format("Event registration only allowed during onLoad execution. " +
                        "Tried to register 'serverStarted' from mod '%s' in mode '%s'", 
                        context.modId(), context.mode())
                );
            }
            
            String modId = context.modId();
            String source = getModSource(modId);
            
            eventBus.subscribe(modId, "serverStarted", callback);
            return null;
        });
        
        serverEvents.put("onStopping", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("events.server.onStopping requires exactly 1 argument: (callback)");
            }
            
            Value callback = args[0];
            validateEventCallback(callback);
            
            // Enforce event registration only during ON_LOAD execution
            TypeScriptRuntime.ExecutionContext context = TypeScriptRuntime.getCurrentContext();
            if (context.mode() != TypeScriptRuntime.ExecutionContextMode.ON_LOAD) {
                throw new IllegalStateException(
                    String.format("Event registration only allowed during onLoad execution. " +
                        "Tried to register 'serverStopping' from mod '%s' in mode '%s'", 
                        context.modId(), context.mode())
                );
            }
            
            String modId = context.modId();
            String source = getModSource(modId);
            
            eventBus.subscribe(modId, "serverStopping", callback);
            return null;
        });
        
        events.put("server", ProxyObject.fromMap(serverEvents));
        
        return ProxyObject.fromMap(events);
    }
    
    /**
     * Validates that a value is an executable function.
     */
    private void validateEventCallback(Value callback) {
        if (callback == null || !callback.canExecute()) {
            throw new IllegalArgumentException("Argument must be an executable function");
        }
    }
    
    /**
     * Gets the source file for a mod.
     */
    private String getModSource(String modId) {
        var mod = modRegistry.getMod(modId);
        return mod != null ? mod.source() : "unknown";
    }
}
