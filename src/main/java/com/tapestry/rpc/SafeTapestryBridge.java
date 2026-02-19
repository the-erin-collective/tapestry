package com.tapestry.rpc;

import com.tapestry.rpc.client.RpcClientFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Phase 17: Secure bridge between isolated JS and RPC system.
 * Only exposes minimal, safe methods to JavaScript.
 * Prevents any direct Java access or reflection.
 */
public final class SafeTapestryBridge {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SafeTapestryBridge.class);
    
    private final RpcClientFacade rpcFacade;
    
    public SafeTapestryBridge(RpcClientFacade rpcFacade) {
        this.rpcFacade = rpcFacade;
    }
    
    /**
     * Makes a secure RPC call to server.
     * 
     * @param methodId Fully qualified method ID (mod:method)
     * @param args Arguments object from JavaScript
     * @return Sanitized response from server
     * @throws RuntimeException if RPC call fails
     */
    public Object call(String methodId, Map<String, Object> args) {
        try {
            if (methodId == null || methodId.trim().isEmpty()) {
                throw new IllegalArgumentException("Method ID cannot be null or empty");
            }
            
            LOGGER.debug("Secure bridge call: {}", methodId);
            Object result = rpcFacade.call(methodId, args);
            
            // Result is already sanitized by RPC system
            return result;
            
        } catch (Exception e) {
            // Convert to JS-friendly exception
            String errorMsg = "RPC_CALL_FAILED: " + e.getMessage();
            LOGGER.warn("Bridge call failed for {}: {}", methodId, e.getMessage());
            throw new RuntimeException(errorMsg);
        }
    }
    
    /**
     * Subscribes to server events.
     * 
     * @param eventId Event ID to subscribe to
     */
    public void subscribe(String eventId) {
        try {
            if (eventId == null || eventId.trim().isEmpty()) {
                throw new IllegalArgumentException("Event ID cannot be null or empty");
            }
            
            LOGGER.debug("Secure bridge subscribe: {}", eventId);
            rpcFacade.subscribe(eventId);
            
        } catch (Exception e) {
            String errorMsg = "SUBSCRIBE_FAILED: " + e.getMessage();
            LOGGER.warn("Bridge subscribe failed for {}: {}", eventId, e.getMessage());
            throw new RuntimeException(errorMsg);
        }
    }
    
    /**
     * Unsubscribes from server events.
     * 
     * @param eventId Event ID to unsubscribe from
     */
    public void unsubscribe(String eventId) {
        try {
            if (eventId == null || eventId.trim().isEmpty()) {
                throw new IllegalArgumentException("Event ID cannot be null or empty");
            }
            
            LOGGER.debug("Secure bridge unsubscribe: {}", eventId);
            rpcFacade.unsubscribe(eventId);
            
        } catch (Exception e) {
            String errorMsg = "UNSUBSCRIBE_FAILED: " + e.getMessage();
            LOGGER.warn("Bridge unsubscribe failed for {}: {}", eventId, e.getMessage());
            throw new RuntimeException(errorMsg);
        }
    }
}
