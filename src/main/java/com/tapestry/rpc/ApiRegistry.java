package com.tapestry.rpc;

import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for server API methods during the registration phase.
 * Mutable during registration, then frozen into an immutable RpcDispatcher.
 */
public class ApiRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRegistry.class);
    
    private final Map<String, ServerApiMethod> mutableTable = new ConcurrentHashMap<>();
    private boolean frozen = false;
    
    /**
     * Registers a server API method.
     * 
     * @param modId The mod ID registering the method
     * @param methodName The method name
     * @param function The Graal function to execute
     * @param context The Graal context the function belongs to
     */
    public void register(String modId, String methodName, Value function, org.graalvm.polyglot.Context context) {
        if (frozen) {
            throw new IllegalStateException("ApiRegistry is frozen - no further registration allowed");
        }
        
        String qualifiedName = modId + ":" + methodName;
        
        if (mutableTable.containsKey(qualifiedName)) {
            throw new IllegalArgumentException("Server API method already registered: " + qualifiedName);
        }
        
        ServerApiMethod method = new ServerApiMethod(function, context, modId, methodName);
        mutableTable.put(qualifiedName, method);
        
        LOGGER.debug("Registered server API method: {}", qualifiedName);
    }
    
    /**
     * Freezes the registry and creates an immutable RpcDispatcher.
     * After this call, no further registration is allowed.
     */
    public RpcDispatcher freeze() {
        if (frozen) {
            throw new IllegalStateException("ApiRegistry is already frozen");
        }
        
        frozen = true;
        
        LOGGER.info("Freezing ApiRegistry with {} methods", mutableTable.size());
        
        // Create immutable dispatcher
        return new RpcDispatcher(Map.copyOf(mutableTable));
    }
    
    /**
     * Checks if the registry is frozen.
     */
    public boolean isFrozen() {
        return frozen;
    }
    
    /**
     * Gets the current number of registered methods.
     */
    public int size() {
        return mutableTable.size();
    }
    
    /**
     * Checks if a method is registered.
     */
    public boolean hasMethod(String modId, String methodName) {
        return mutableTable.containsKey(modId + ":" + methodName);
    }
}
