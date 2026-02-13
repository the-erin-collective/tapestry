package com.tapestry.persistence;

import com.tapestry.typescript.TypeScriptRuntime;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * TypeScript API for persistence operations.
 * 
 * This class is injected into each mod's runtime context and provides
 * the tapestry.persistence namespace with get/set/delete/keys methods.
 */
public class PersistenceApi implements ProxyObject {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceApi.class);
    
    private final String modId;
    private final ModStateStore stateStore;
    
    /**
     * Creates a new persistence API for a mod.
     * 
     * @param modId the mod identifier
     * @param stateStore the mod's state store
     */
    public PersistenceApi(String modId, ModStateStore stateStore) {
        this.modId = modId;
        this.stateStore = stateStore;
    }
    
    /**
     * Creates a persistence API for factory usage.
     * Used when creating persistence instances dynamically.
     */
    public PersistenceApi() {
        this.modId = null; // Will be set per-call
        this.stateStore = null; // Will be set per-call
    }
    
    @Override
    public Object getMember(String key) {
        switch (key) {
            case "get":
                return new GetExecutable();
            case "set":
                return new SetExecutable();
            case "delete":
                return new DeleteExecutable();
            case "keys":
                return new KeysExecutable();
            default:
                return null; // Factory mode - only support specific methods
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return new String[]{"get", "set", "delete", "keys"};
    }
    
    @Override
    public boolean hasMember(String key) {
        return "get".equals(key) || "set".equals(key) || 
               "delete".equals(key) || "keys".equals(key);
    }
    
    @Override
    public void putMember(String key, Value value) {
        // Read-only API - no dynamic member assignment
        throw new UnsupportedOperationException("Persistence API is read-only");
    }
    
    @Override
    public boolean removeMember(String key) {
        // Read-only API - no dynamic member removal
        throw new UnsupportedOperationException("Persistence API is read-only");
    }
    
    /**
     * Implements tapestry.persistence.get(key)
     */
    private class GetExecutable implements ProxyExecutable {
        @Override
        public Object execute(Value... arguments) {
            if (arguments.length != 1) {
                throw new IllegalArgumentException("get() requires exactly 1 argument (key)");
            }
            
            String key = arguments[0].asString();
            ModStateStore store = getCurrentModStore();
            Object result = store.get(key);
            
            LOGGER.debug("Mod {} called persistence.get('{}') = {}", TypeScriptRuntime.getCurrentModId(), key, result);
            return TypeScriptRuntime.toHostValue(result);
        }
    }
    
    /**
     * Implements tapestry.persistence.set(key, value)
     */
    private class SetExecutable implements ProxyExecutable {
        @Override
        public Object execute(Value... arguments) {
            if (arguments.length != 2) {
                throw new IllegalArgumentException("set() requires exactly 2 arguments (key, value)");
            }
            
            String key = arguments[0].asString();
            Object value = TypeScriptRuntime.fromValue(arguments[1]);
            ModStateStore store = getCurrentModStore();
            
            store.set(key, value);
            
            LOGGER.debug("Mod {} called persistence.set('{}', {})", TypeScriptRuntime.getCurrentModId(), key, value);
            return null; // set() returns undefined in JS
        }
    }
    
    /**
     * Implements tapestry.persistence.delete(key)
     */
    private class DeleteExecutable implements ProxyExecutable {
        @Override
        public Object execute(Value... arguments) {
            if (arguments.length != 1) {
                throw new IllegalArgumentException("delete() requires exactly 1 argument (key)");
            }
            
            String key = arguments[0].asString();
            ModStateStore store = getCurrentModStore();
            
            store.delete(key);
            
            LOGGER.debug("Mod {} called persistence.delete('{}')", TypeScriptRuntime.getCurrentModId(), key);
            return null; // delete() returns undefined in JS
        }
    }
    
    /**
     * Implements tapestry.persistence.keys()
     */
    private class KeysExecutable implements ProxyExecutable {
        @Override
        public Object execute(Value... arguments) {
            if (arguments.length != 0) {
                throw new IllegalArgumentException("keys() requires no arguments");
            }
            
            ModStateStore store = getCurrentModStore();
            Set<String> keys = store.keys();
            
            LOGGER.debug("Mod {} called persistence.keys() -> {}", TypeScriptRuntime.getCurrentModId(), keys);
            return TypeScriptRuntime.toHostValue(keys.toArray(new String[0]));
        }
    }
    
    /**
     * Gets the current mod's state store.
     * 
     * @return the current mod's state store
     */
    private ModStateStore getCurrentModStore() {
        if (stateStore != null) {
            return stateStore; // Direct instance
        }
        
        // Factory mode - get from persistence service
        String currentModId = TypeScriptRuntime.getCurrentModId();
        return PersistenceService.getInstance().getModStateStore(currentModId);
    }
}
