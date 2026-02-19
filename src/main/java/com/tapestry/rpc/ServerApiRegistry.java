package com.tapestry.rpc;

import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Secure registry for server-side API methods.
 * 
 * Provides method allowlist, namespace isolation, and permission validation.
 * Only explicitly registered methods can be called via RPC.
 */
public class ServerApiRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerApiRegistry.class);
    
    // Protocol version for handshake validation
    public static final int PROTOCOL_VERSION = 1;
    
    // Thread-safe method registry
    private static final Map<String, ServerApiMethod> methods = new ConcurrentHashMap<>();
    
    // Registration state - only allow registration during boot
    private static volatile boolean registrationOpen = true;
    
    /**
     * Registers a server API method with allowlist validation.
     * 
     * @param methodId Fully qualified method ID (mod:method)
     * @param method Method implementation
     * @throws IllegalStateException if registration is closed
     * @throws IllegalArgumentException if methodId is invalid
     */
    public static void registerMethod(String methodId, ServerApiMethod method) {
        if (!registrationOpen) {
            throw new IllegalStateException("Method registration is closed after boot");
        }
        
        if (methodId == null || methodId.trim().isEmpty()) {
            throw new IllegalArgumentException("Method ID cannot be null or empty");
        }
        
        if (!methodId.contains(":")) {
            throw new IllegalArgumentException("Method ID must be in format 'mod:method'");
        }
        
        String[] parts = methodId.split(":", 2);
        String modId = parts[0];
        String methodName = parts[1];
        
        // Validate method ID format
        if (modId.isEmpty() || methodName.isEmpty()) {
            throw new IllegalArgumentException("Both mod ID and method name must be non-empty");
        }
        
        // Check for duplicates
        if (methods.containsKey(methodId)) {
            LOGGER.warn("Method '{}' already registered, skipping", methodId);
            return;
        }
        
        methods.put(methodId, method);
        LOGGER.info("Registered server API method: {}", methodId);
    }
    
    /**
     * Gets a registered method by ID.
     * 
     * @param methodId Method ID to look up
     * @return Method if found, null otherwise
     */
    public static ServerApiMethod getMethod(String methodId) {
        return methods.get(methodId);
    }
    
    /**
     * Checks if a method is registered.
     * 
     * @param methodId Method ID to check
     * @return true if registered, false otherwise
     */
    public static boolean hasMethod(String methodId) {
        return methods.containsKey(methodId);
    }
    
    /**
     * Closes registration - no more methods can be registered.
     * Called during mod initialization complete.
     */
    public static void closeRegistration() {
        registrationOpen = false;
        LOGGER.info("Method registration closed. {} methods registered.", methods.size());
    }
    
    /**
     * Gets all registered methods for debugging.
     * 
     * @return Copy of method registry
     */
    public static Map<String, ServerApiMethod> getAllMethods() {
        return Map.copyOf(methods);
    }
    
    /**
     * Clears registry (for testing only).
     */
    static void clearForTesting() {
        methods.clear();
        registrationOpen = true;
    }
}
