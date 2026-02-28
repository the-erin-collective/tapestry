package com.tapestry.rpc;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.tapestry.networking.RpcPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for sending server events to specific clients.
 */
public class ServerEventSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerEventSender.class);
    
    /**
     * Send an event to a specific player.
     */
    public static void sendToPlayer(ServerPlayerEntity player, String eventName, JsonElement payload) {
        try {
            JsonObject packet = new JsonObject();
            packet.addProperty("type", "event");
            packet.addProperty("channel", eventName);
            packet.add("data", payload);
            
            String packetData = packet.toString();
            
            // Send using modern payload system
            ServerPlayNetworking.send(player, new RpcPayload(packetData));
            
        } catch (Exception e) {
            LOGGER.error("Failed to send server event to player: " + player.getName().getString(), e);
        }
    }
    
    /**
     * Validates event name format.
     * Events must be namespaced: modId:eventName
     */
    public static boolean isValidEventName(String eventName) {
        if (eventName == null || eventName.isEmpty()) {
            return false;
        }
        
        int colonIndex = eventName.indexOf(':');
        if (colonIndex <= 0 || colonIndex >= eventName.length() - 1) {
            return false;
        }
        
        String modId = eventName.substring(0, colonIndex);
        String eventPart = eventName.substring(colonIndex + 1);
        
        // Basic validation - mod ID and event name should be reasonable
        return modId.matches("[a-z0-9_-]+") && eventPart.matches("[a-zA-Z0-9_-]+");
    }
}
