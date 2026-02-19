package com.tapestry.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Immutable RPC dispatcher for executing server API methods.
 * Contains the dispatch table and handles method execution.
 */
public class RpcDispatcher {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcDispatcher.class);
    
    private final Map<String, ServerApiMethod> dispatchTable;
    
    public RpcDispatcher(Map<String, ServerApiMethod> dispatchTable) {
        this.dispatchTable = Map.copyOf(dispatchTable); // Immutable copy
        LOGGER.info("RPC Dispatcher initialized with {} methods", dispatchTable.size());
    }
    
    /**
     * Dispatches an RPC call to the appropriate method.
     */
    public void dispatch(ServerPlayerEntity player, String id, String method, JsonElement args) {
        ServerApiMethod target = dispatchTable.get(method);
        
        if (target == null) {
            LOGGER.warn("RPC method not found: {} from player: {}", method, player.getName().getString());
            RpcResponseSender.sendError(player, id, "METHOD_NOT_FOUND", method);
            return;
        }
        
        execute(player, id, target, args);
    }
    
    /**
     * Executes the target method with proper error handling.
     */
    private void execute(ServerPlayerEntity player, String id, ServerApiMethod method, JsonElement args) {
        try {
            // Create server context for this call
            // TODO: Fix mapping issues - temporarily disabled
        return;
            
        } catch (Exception e) {
            LOGGER.error("Error executing RPC method {} for player: {}", 
                       method.getModId() + ":" + method.getMethodName(), 
                       player.getName().getString(), e);
            RpcResponseSender.sendError(player, id, "INTERNAL_ERROR", "Method execution failed");
        }
    }
}
