package com.tapestry.rpc;

import com.google.gson.JsonElement;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Secure RPC dispatcher for executing server API methods.
 * Uses ServerApiRegistry for method allowlist and validation.
 * Enforces namespace ownership and argument sanitization.
 */
public class RpcDispatcher {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcDispatcher.class);
    
    // Client installed mods for namespace validation
    private Set<String> clientInstalledMods;
    
    public RpcDispatcher() {
        this.clientInstalledMods = Set.of(); // TODO: Get from handshake
        LOGGER.info("Secure RPC Dispatcher initialized");
    }
    
    /**
     * Sets client installed mods for namespace validation.
     * Called during handshake completion.
     */
    public void setClientInstalledMods(Set<String> mods) {
        this.clientInstalledMods = Set.copyOf(mods);
        LOGGER.info("Client installed mods: {}", mods);
    }
    
    /**
     * Dispatches an RPC call with security validation.
     */
    public void dispatch(ServerPlayerEntity player, String id, String method, JsonElement args) {
        try {
            // Security: Validate method is registered in allowlist
            if (!ServerApiRegistry.hasMethod(method)) {
                LOGGER.warn("RPC method not registered: {} from player: {}", method, player.getName().getString());
                RpcResponseSender.sendError(player, id, "METHOD_NOT_FOUND", method);
                return;
            }
            
            // Security: Enforce namespace ownership (Option A - Strict Isolation)
            String methodOwner = ServerApiRegistry.getMethodOwner(method);
            if (methodOwner != null && !clientInstalledMods.contains(methodOwner)) {
                LOGGER.warn("RPC method access denied: {} from player {} - mod {} not installed", 
                           method, player.getName().getString(), methodOwner);
                RpcResponseSender.sendError(player, id, "NAMESPACE_ACCESS_DENIED", 
                                       "Method owner not installed: " + methodOwner);
                return;
            }
            
            ServerApiMethod target = ServerApiRegistry.getMethod(method);
            if (target == null) {
                // This should not happen if hasMethod() returned true, but defensive programming
                LOGGER.error("Registry inconsistency for method: {}", method);
                RpcResponseSender.sendError(player, id, "INTERNAL_ERROR", "Registry inconsistency");
                return;
            }
            
            // Security: Sanitize arguments
            Object sanitizedArgs;
            try {
                sanitizedArgs = RpcSanitizer.sanitize(args);
            } catch (RpcValidationException e) {
                LOGGER.warn("RPC argument validation failed for method {} from player {}: {}", 
                           method, player.getName().getString(), e.getMessage());
                RpcResponseSender.sendError(player, id, "INVALID_ARGUMENT", e.getMessage());
                return;
            }
            
            execute(player, id, target, sanitizedArgs);
            
        } catch (Exception e) {
            LOGGER.error("Unexpected error in RPC dispatch for method {} from player {}", 
                       method, player.getName().getString(), e);
            RpcResponseSender.sendError(player, id, "INTERNAL_ERROR", "Dispatch failed");
        }
    }
    
    /**
     * Executes target method with proper error handling and security.
     */
    private void execute(ServerPlayerEntity player, String id, ServerApiMethod method, Object sanitizedArgs) {
        try {
            // TODO: Fix mapping issues - temporarily disabled
            // ServerContext ctx = new ServerContext(player, player.server, player.getUuid());
            
            // TODO: Re-enable when mapping issues are fixed
            // CompletableFuture<Object> future = method.invoke(ctx, sanitizedArgs);
            
            // TODO: Re-enable when mapping issues are fixed
            // future.whenComplete((result, throwable) -> {
            //     if (throwable != null) {
            //         LOGGER.error("RPC method {} threw exception for player: {}", 
            //                    method.getModId() + ":" + method.getMethodName(), 
            //                    player.getName().getString(), throwable);
            //         RpcResponseSender.sendError(player, id, "USER_ERROR", throwable.getMessage());
            //     } else {
            //         // Security: Sanitize return value
            //         Object sanitizedResult;
            //         try {
            //             sanitizedResult = RpcSanitizer.sanitize(result);
            //             RpcResponseSender.sendSuccess(player, id, sanitizedResult);
            //         } catch (RpcValidationException e) {
            //             LOGGER.error("RPC method {} returned invalid data for player {}: {}", 
            //                        method.getModId() + ":" + method.getMethodName(), 
            //                        player.getName().getString(), e.getMessage());
            //             RpcResponseSender.sendError(player, id, "INVALID_RETURN", "Method returned invalid data");
            //         }
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
