package com.tapestry.rpc.client;

import java.util.Map;

/**
 * Phase 17: Secure facade interface for RPC client access.
 * Provides minimal surface area for SafeTapestryBridge.
 * Prevents exposure of internal RpcClientRuntime methods.
 */
public interface RpcClientFacade {
    
    /**
     * Makes an RPC call to server.
     * 
     * @param methodId Fully qualified method ID (mod:method)
     * @param args Pre-sanitized arguments
     * @return Sanitized response from server
     * @throws RuntimeException if RPC call fails
     */
    Object call(String methodId, Map<String, Object> args);
    
    /**
     * Subscribes to server events.
     * 
     * @param eventId Event ID to subscribe to
     */
    void subscribe(String eventId);
    
    /**
     * Unsubscribes from server events.
     * 
     * @param eventId Event ID to unsubscribe from
     */
    void unsubscribe(String eventId);
}
