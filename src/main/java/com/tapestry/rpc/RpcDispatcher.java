package com.tapestry.rpc;

import com.google.gson.JsonElement;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secure RPC dispatcher for executing server API methods.
 * Uses ServerApiRegistry for method allowlist and validation.
 */
public class RpcDispatcher {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcDispatcher.class);
    
    public RpcDispatcher() {
        LOGGER.info("Secure RPC Dispatcher initialized");
    }
    
    /**
     * Dispatches an RPC call to the appropriate method with security validation.
     */
    public void dispatch(ServerPlayerEntity player, String id, String method, JsonElement args) {
        // Security: Validate method is registered in allowlist
        if (!ServerApiRegistry.hasMethod(method)) {
            LOGGER.warn("RPC method not registered: {} from player: {}", method, player.getName().getString());
            RpcResponseSender.sendError(player, id, "METHOD_NOT_FOUND", method);
            return;
        }
        
        ServerApiMethod target = ServerApiRegistry.getMethod(method);
        if (target == null) {
            // This should not happen if hasMethod() returned true, but defensive programming
            LOGGER.error("Registry inconsistency for method: {}", method);
            RpcResponseSender.sendError(player, id, "INTERNAL_ERROR", "Registry inconsistency");
            return;
        }
        
        execute(player, id, target, args);
    }
    
    /**
     * Executes the target method with proper error handling and security.
     */
    private void execute(ServerPlayerEntity player, String id, ServerApiMethod method, JsonElement args) {
        try {
            // TODO: Fix mapping issues - temporarily disabled
            // ServerContext ctx = new ServerContext(player, player.server, player.getUuid());
            
            // TODO: Re-enable when mapping issues are fixed
            // CompletableFuture<JsonElement> future = method.invoke(ctx, args);
            
            // TODO: Re-enable when mapping issues are fixed
            // future.whenComplete((result, throwable) -> {
            //     if (throwable != null) {
            //         LOGGER.error("RPC method {} threw exception for player: {}", 
            //                    method.getModId() + ":" + method.getMethodName(), 
            //                    player.getName().getString(), throwable);
            //         RpcResponseSender.sendError(player, id, "USER_ERROR", throwable.getMessage());
            //     } else {
            //         RpcResponseSender.sendSuccess(player, id, result);
            //     }
            // });
            
        } catch (Exception e) {
            LOGGER.error("Error executing RPC method {} for player: {}", 
                       method.getModId() + ":" + method.getMethodName(), 
                       player.getName().getString(), e);
            RpcResponseSender.sendError(player, id, "INTERNAL_ERROR", "Method execution failed");
        }
    }
}
