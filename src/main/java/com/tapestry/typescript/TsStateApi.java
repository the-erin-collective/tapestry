package com.tapestry.typescript;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.state.ModStateService;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * TypeScript API for state operations.
 * 
 * Provides per-mod state storage with JSON-serializable
 * value enforcement and persistence-agnostic API.
 */
public class TsStateApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TsStateApi.class);
    
    private final ModStateService stateService;
    
    public TsStateApi(ModStateService stateService) {
        this.stateService = stateService;
    }
    
    /**
     * Creates the state namespace object.
     * 
     * @return ProxyObject with state functions
     */
    public ProxyObject createNamespace() {
        Map<String, Object> state = new HashMap<>();
        
        // set function
        state.put("set", (ProxyExecutable) args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException("state.set requires exactly 2 arguments: (key, value)");
            }
            
            String key = args[0].asString();
            Object value = args[1];
            
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Key must be a non-empty string");
            }
            
            String modId = TypeScriptRuntime.getCurrentModId();
            if (modId == null) {
                throw new IllegalStateException("No mod ID set in current context");
            }
            
            stateService.set(modId, key, value);
            return null;
        });
        
        // get function
        state.put("get", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("state.get requires exactly 1 argument: (key)");
            }
            
            String key = args[0].asString();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Key must be a non-empty string");
            }
            
            String modId = TypeScriptRuntime.getCurrentModId();
            if (modId == null) {
                throw new IllegalStateException("No mod ID set in current context");
            }
            
            return stateService.get(modId, key);
        });
        
        // has function
        state.put("has", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("state.has requires exactly 1 argument: (key)");
            }
            
            String key = args[0].asString();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Key must be a non-empty string");
            }
            
            String modId = TypeScriptRuntime.getCurrentModId();
            if (modId == null) {
                throw new IllegalStateException("No mod ID set in current context");
            }
            
            return stateService.has(modId, key);
        });
        
        // delete function
        state.put("delete", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("state.delete requires exactly 1 argument: (key)");
            }
            
            String key = args[0].asString();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Key must be a non-empty string");
            }
            
            String modId = TypeScriptRuntime.getCurrentModId();
            if (modId == null) {
                throw new IllegalStateException("No mod ID set in current context");
            }
            
            return stateService.delete(modId, key);
        });
        
        // keys function
        state.put("keys", (ProxyExecutable) args -> {
            if (args.length != 0) {
                throw new IllegalArgumentException("state.keys takes no arguments");
            }
            
            String modId = TypeScriptRuntime.getCurrentModId();
            if (modId == null) {
                throw new IllegalStateException("No mod ID set in current context");
            }
            
            return stateService.getKeys(modId);
        });
        
        // clear function
        state.put("clear", (ProxyExecutable) args -> {
            if (args.length != 0) {
                throw new IllegalArgumentException("state.clear takes no arguments");
            }
            
            String modId = TypeScriptRuntime.getCurrentModId();
            if (modId == null) {
                throw new IllegalStateException("No mod ID set in current context");
            }
            
            stateService.clearMod(modId);
            return null;
        });
        
        return ProxyObject.fromMap(state);
    }
}
