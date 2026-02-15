package com.tapestry.typescript;

import com.tapestry.events.EventBus;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.lifecycle.PhaseController;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Phase 11 Mod Event API.
 * 
 * Provides mod.on, mod.emit, and mod.off methods for the reactive event system.
 * Available during TS_ACTIVATE, CLIENT_PRESENTATION_READY, and RUNTIME phases.
 */
public class ModEventApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ModEventApi.class);
    
    private final EventBus eventBus;
    
    public ModEventApi(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    /**
     * Creates the mod event API object with on/emit/off methods.
     * 
     * @return ProxyObject with mod event functions
     */
    public ProxyObject createModEventApi() {
        Map<String, Object> modEvents = new HashMap<>();
        
        // mod.on(eventName, handler)
        modEvents.put("on", (ProxyExecutable) args -> {
            validatePhase("mod.on");
            
            if (args.length != 2) {
                throw new IllegalArgumentException("mod.on requires exactly 2 arguments: (eventName, handler)");
            }
            
            String eventName = args[0].asString();
            Value handler = args[1];
            
            validateEventName(eventName);
            validateHandler(handler);
            
            String modId = getCurrentModId();
            eventBus.subscribe(modId, eventName, handler);
            
            LOGGER.debug("Mod '{}' subscribed to event '{}'", modId, eventName);
            return null;
        });
        
        // mod.emit(eventName, payload?)
        modEvents.put("emit", (ProxyExecutable) args -> {
            validatePhase("mod.emit");
            
            if (args.length < 1 || args.length > 2) {
                throw new IllegalArgumentException("mod.emit requires 1 or 2 arguments: (eventName, payload?)");
            }
            
            String eventName = args[0].asString();
            Object payload = args.length > 1 ? convertPayload(args[1]) : null;
            
            validateEventName(eventName);
            
            String modId = getCurrentModId();
            eventBus.emit(modId, eventName, payload);
            
            LOGGER.debug("Mod '{}' emitted event '{}'", modId, eventName);
            return null;
        });
        
        // mod.off(eventName, handler)
        modEvents.put("off", (ProxyExecutable) args -> {
            validatePhase("mod.off");
            
            if (args.length != 2) {
                throw new IllegalArgumentException("mod.off requires exactly 2 arguments: (eventName, handler)");
            }
            
            String eventName = args[0].asString();
            Value handler = args[1];
            
            validateEventName(eventName);
            validateHandler(handler);
            
            String modId = getCurrentModId();
            eventBus.unsubscribe(modId, eventName, handler);
            
            LOGGER.debug("Mod '{}' unsubscribed from event '{}'", modId, eventName);
            return null;
        });
        
        return ProxyObject.fromMap(modEvents);
    }
    
    /**
     * Validates that the current phase allows event operations.
     */
    private void validatePhase(String operation) {
        TapestryPhase currentPhase = PhaseController.getInstance().getCurrentPhase();
        
        if (currentPhase != TapestryPhase.TS_ACTIVATE && 
            currentPhase != TapestryPhase.CLIENT_PRESENTATION_READY && 
            currentPhase != TapestryPhase.RUNTIME) {
            throw new IllegalStateException(
                String.format("%s is only available during TS_ACTIVATE, CLIENT_PRESENTATION_READY, or RUNTIME phases. Current phase: %s", 
                             operation, currentPhase));
        }
    }
    
    /**
     * Validates event name format.
     */
    private void validateEventName(String eventName) {
        if (eventName == null || eventName.isBlank()) {
            throw new IllegalArgumentException("Event name must be a non-empty string");
        }
    }
    
    /**
     * Validates that handler is an executable function.
     */
    private void validateHandler(Value handler) {
        if (handler == null || !handler.canExecute()) {
            throw new IllegalArgumentException("Handler must be an executable function");
        }
    }
    
    /**
     * Gets the current mod ID from execution context.
     */
    private String getCurrentModId() {
        String modId = TypeScriptRuntime.getCurrentModId();
        if (modId == null) {
            throw new IllegalStateException("No mod ID set in current execution context");
        }
        return modId;
    }
    
    /**
     * Converts payload from Value to Java Object for EventBus.
     */
    private Object convertPayload(Value value) {
        if (value == null) {
            return null;
        }
        
        // Let GraalVM handle the conversion
        if (value.isHostObject()) {
            return value.asHostObject();
        } else if (value.isString()) {
            return value.asString();
        } else if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            } else if (value.fitsInDouble()) {
                return value.asDouble();
            } else if (value.fitsInLong()) {
                return value.asLong();
            }
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.hasArrayElements()) {
            // Convert arrays
            int size = (int) value.getArraySize();
            Object[] array = new Object[size];
            for (int i = 0; i < size; i++) {
                array[i] = convertPayload(value.getArrayElement(i));
            }
            return array;
        } else if (value.hasMembers()) {
            // Convert objects to Map
            Map<String, Object> map = new HashMap<>();
            for (String key : value.getMemberKeys()) {
                map.put(key, convertPayload(value.getMember(key)));
            }
            return map;
        }
        
        // Fallback: return as is
        return value;
    }
}
