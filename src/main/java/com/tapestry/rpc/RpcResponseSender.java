package com.tapestry.rpc;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.tapestry.networking.RpcPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for sending RPC responses from server to client.
 */
public class RpcResponseSender {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcResponseSender.class);
    private static final int PROTOCOL_VERSION = 1;
    
    /**
     * Sends a successful RPC response.
     */
    public static void sendSuccess(ServerPlayerEntity player, String id, JsonElement result) {
        JsonObject response = new JsonObject();
        response.addProperty("protocol", PROTOCOL_VERSION);
        response.addProperty("type", "response");
        response.addProperty("requestId", id);
        response.addProperty("success", true);
        
        if (result != null) {
            response.add("result", result);
        } else {
            response.add("result", null);
        }
        
        sendPacket(player, response);
        
        LOGGER.debug("RPC success sent: player={}, id={}", player.getName().getString(), id);
    }
    
    /**
     * Sends an error RPC response.
     */
    public static void sendError(ServerPlayerEntity player, String id, String code, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("protocol", PROTOCOL_VERSION);
        response.addProperty("type", "response");
        response.addProperty("requestId", id);
        response.addProperty("success", false);
        
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        response.add("error", error);
        
        sendPacket(player, response);
        
        LOGGER.debug("RPC error sent: player={}, id={}, code={}", 
                   player.getName().getString(), id, code);
    }
    
    /**
     * Sends a server event to a specific client.
     */
    public static void sendServerEvent(ServerPlayerEntity player, String eventName, JsonElement payload) {
        JsonObject response = new JsonObject();
        response.addProperty("protocol", PROTOCOL_VERSION);
        response.addProperty("type", "event");
        response.addProperty("channel", eventName);
        
        if (payload != null) {
            response.add("data", payload);
        } else {
            response.add("data", null);
        }
        
        sendPacket(player, response);
        
        LOGGER.debug("Server event sent: player={}, channel={}", player.getName().getString(), eventName);
    }
    
    /**
     * Sends a packet to a player.
     */
    private static void sendPacket(ServerPlayerEntity player, JsonObject packet) {
        try {
            String packetData = packet.toString();
            
            // Send using modern payload system
            ServerPlayNetworking.send(player, new RpcPayload(packetData));
                
        } catch (Exception e) {
            LOGGER.error("Failed to send RPC packet to player: {}", player.getName().getString(), e);
        }
    }
}
