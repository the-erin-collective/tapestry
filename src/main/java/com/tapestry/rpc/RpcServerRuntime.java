package com.tapestry.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Core RPC server runtime authority.
 * Enforces handshake completion, rate limiting, and delegates to dispatcher.
 */
public class RpcServerRuntime {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServerRuntime.class);
    
    private final RpcDispatcher dispatcher;
    private final HandshakeRegistry handshakeRegistry;
    private final RateLimiter rateLimiter;
    
    public RpcServerRuntime(RpcDispatcher dispatcher, HandshakeRegistry handshakeRegistry) {
        this.dispatcher = dispatcher;
        this.handshakeRegistry = handshakeRegistry;
        this.rateLimiter = new RateLimiter();
    }
    
    /**
     * Handles incoming RPC call from client.
     */
    public void handleCall(ServerPlayerEntity player, JsonObject packet) {
        // Validate handshake complete
        if (!handshakeRegistry.isReady(player.getUuid())) {
            LOGGER.warn("Received RPC call from player {} before handshake completion", 
                       player.getName().getString());
            disconnect(player);
            return;
        }
        
        // Check rate limits
        if (!rateLimiter.canMakeCall(player)) {
            LOGGER.warn("Rate limited RPC call from player: {}", player.getName().getString());
            // Don't disconnect, just ignore the call
            return;
        }
        
        try {
            String id = packet.get("id").getAsString();
            String method = packet.get("method").getAsString();
            JsonElement args = packet.has("args") ? packet.get("args") : null;
            
            LOGGER.debug("RPC call: player={}, method={}, id={}", 
                       player.getName().getString(), method, id);
            
            // Dispatch asynchronously to avoid blocking network thread
            CompletableFuture.runAsync(() -> {
                try {
                    dispatcher.dispatch(player, id, method, args);
                } finally {
                    // Record completion for rate limiting
                    rateLimiter.recordCallCompletion(player);
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Error processing RPC call from player: {}", player.getName().getString(), e);
            
            // Record completion even on error
            rateLimiter.recordCallCompletion(player);
            
            // Send error response if we can extract call ID
            if (packet.has("id")) {
                RpcResponseSender.sendError(player, packet.get("id").getAsString(), 
                                        "INTERNAL_ERROR", "Server processing error");
            }
        }
    }
    
    /**
     * Disconnects a player for protocol violation.
     */
    private void disconnect(ServerPlayerEntity player) {
        player.networkHandler.disconnect(net.minecraft.text.Text.literal("RPC protocol violation"));
    }
    
    /**
     * Gets rate limiting metrics for debugging.
     */
    public Map<String, Object> getRateLimitMetrics() {
        return rateLimiter.getMetrics();
    }
    
    /**
     * Removes a player from rate limiting (typically on disconnect).
     */
    public void removePlayer(ServerPlayerEntity player) {
        rateLimiter.removePlayer(player);
    }
}
