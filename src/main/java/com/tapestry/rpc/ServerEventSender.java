package com.tapestry.rpc;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tapestry.networking.RpcCustomPayload;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Utility for sending server events to specific clients.
 * Handles the SERVER_EVENT packet type.
 */
public class ServerEventSender {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerEventSender.class);
    private static final int PROTOCOL_VERSION = 1;
    
    /**
     * Sends a server event to a specific player.
     * 
     * @param player The target player
     * @param eventName The fully-qualified event name (modId:eventName)
     * @param payload The JSON payload (can be null)
     */
    public static void sendToPlayer(ServerPlayerEntity player, String eventName, JsonElement payload) {
        if (player == null || player.networkHandler == null) {
            LOGGER.warn("Cannot send event '{}' to null player or disconnected player", eventName);
            return;
        }
        
        try {
            LOGGER.debug("Sending server event '{}' to player: {}", eventName, player.getName().getString());
            
            // Create packet data
            JsonObject packet = new JsonObject();
            packet.addProperty("type", "server_event");
            packet.addProperty("event", eventName);
            packet.add("payload", payload);
            packet.addProperty("protocol", PROTOCOL_VERSION);
            
            String packetData = packet.toString();
            
            // TODO: Re-enable networking after fixing API compatibility
            // Send using simple buffer approach
            // PacketByteBuf buf = PacketByteBufs.create();
            // buf.writeString(packet.toString());
            // player.networkHandler.sendPacket(new CustomPayloadS2CPacket(RpcCustomPayload.ID, buf));
            
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
