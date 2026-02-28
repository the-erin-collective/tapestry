package com.tapestry.rpc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles client-side packet registration for RPC system.
 * 
 * Note: In this Fabric version, ClientPlayNetworking.registerGlobalReceiver doesn't exist.
 * Instead, we use a static handle() method approach that's called from packet injection points.
 */
public class RpcClientNetworkRegistrar {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientNetworkRegistrar.class);
    
    private static boolean registered = false;

    /**
     * Registers client-side packet handling capability.
     * 
     * Since ClientPlayNetworking.registerGlobalReceiver is not available in this Fabric version,
     * we mark the system as ready and rely on the static RpcClientRuntime.handle() method
     * that gets called from packet injection points.
     */
    public static void register() {
        if (registered) {
            LOGGER.debug("Client-side RPC packet registration already completed");
            return;
        }
        
        try {
            // Mark the system as ready for packet handling
            registered = true;
            
            // Log the registration approach for this Fabric version
            LOGGER.info("Client-side RPC packet registration completed using static handle approach");
            LOGGER.info("RPC packets will be routed through RpcClientRuntime.handle() method");
            
            // Verify that RpcClientRuntime is available
            if (RpcClientRuntime.getInstance() == null) {
                LOGGER.warn("RpcClientRuntime instance not available - packet handling may not work");
            } else {
                LOGGER.debug("RpcClientRuntime instance available for packet handling");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to register client-side RPC packet handling", e);
            registered = false;
        }
    }
    
    /**
     * Checks if client-side packet registration is complete.
     * 
     * @return true if registration is complete
     */
    public static boolean isRegistered() {
        return registered;
    }
    
    /**
     * Simulates receiving a packet for testing purposes.
     * This method can be used to test the packet routing system.
     * 
     * @param json the JSON packet data
     */
    public static void simulatePacketReceive(String json) {
        if (registered && RpcClientRuntime.getInstance() != null) {
            RpcClientRuntime.handle(json);
        } else {
            LOGGER.warn("Cannot simulate packet receive - registration not complete or runtime unavailable");
        }
    }
}
