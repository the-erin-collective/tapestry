package com.tapestry.rpc;

import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tapestry.networking.RpcCustomPayload;
import net.minecraft.network.PacketByteBuf;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Handles the RPC handshake process between client and server.
 */
public class HandshakeHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HandshakeHandler.class);
    private static final int PROTOCOL_VERSION = 1;
    
    private final HandshakeRegistry handshakeRegistry;
    private final RpcDispatcher dispatcher;
    private final List<String> serverMods;
    
    public HandshakeHandler(HandshakeRegistry handshakeRegistry, RpcDispatcher dispatcher, List<String> serverMods) {
        this.handshakeRegistry = handshakeRegistry;
        this.dispatcher = dispatcher;
        this.serverMods = serverMods;
    }
    
    /**
     * Handles client hello packet.
     */
    public void handleHello(ServerPlayerEntity player, JsonObject packet) {
        try {
            JsonObject client = packet.getAsJsonObject("client");
            
            String tapestryVersion = client.get("tapestryVersion").getAsString();
            LOGGER.debug("Handshake from {}: Tapestry version {}", 
                       player.getName().getString(), tapestryVersion);
            
            // Validate protocol version (already done in packet handler)
            
            // TODO: Validate required mods based on server configuration
            // For now, accept any client
            
            // Compute API hash from dispatcher
            String apiHash = computeApiHash();
            
            // Send hello_ack
            sendHelloAck(player, apiHash);
            
            // Mark as ready
            handshakeRegistry.markReady(player.getUuid());
            
            LOGGER.info("RPC handshake completed for player: {}", player.getName().getString());
            
        } catch (Exception e) {
            LOGGER.error("Error during handshake with player: {}", player.getName().getString(), e);
            sendHandshakeFail(player, "Handshake processing error");
        }
    }
    
    /**
     * Sends hello_ack response to client.
     */
    private void sendHelloAck(ServerPlayerEntity player, String apiHash) {
        JsonObject response = new JsonObject();
        response.addProperty("protocol", PROTOCOL_VERSION);
        response.addProperty("type", "hello_ack");
        
        JsonObject server = new JsonObject();
        server.addProperty("tapestryVersion", "0.16.0"); // TODO: Get from build config
        server.addProperty("apiHash", apiHash);
        
        // Add supported features
        server.add("features", createFeaturesArray());
        
        response.add("server", server);
        
        sendPacket(player, response);
    }
    
    /**
     * Sends handshake failure to client.
     */
    public void sendHandshakeFail(ServerPlayerEntity player, String reason) {
        JsonObject response = new JsonObject();
        response.addProperty("protocol", PROTOCOL_VERSION);
        response.addProperty("type", "handshake_fail");
        response.addProperty("reason", reason);
        
        sendPacket(player, response);
        
        LOGGER.warn("Handshake failed for player {}: {}", player.getName().getString(), reason);
    }
    
    /**
     * Creates features array for hello_ack.
     */
    private com.google.gson.JsonArray createFeaturesArray() {
        com.google.gson.JsonArray features = new com.google.gson.JsonArray();
        features.add("rpc");
        features.add("emit");
        features.add("watch");
        return features;
    }
    
    /**
     * Computes deterministic hash of all registered API methods.
     */
    private String computeApiHash() {
        try {
            // Get all method names from dispatcher
            SortedSet<String> methodNames = new TreeSet<>();
            
            // This would need to be exposed by RpcDispatcher
            // For now, use a placeholder
            methodNames.add("dimension_crafter:createDimension");
            methodNames.add("dimension_crafter:linkPortal");
            
            // Create concatenated string
            StringBuilder sb = new StringBuilder();
            for (String methodName : methodNames) {
                sb.append(methodName).append("\n");
            }
            
            // Compute SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes("UTF-8"));
            
            // Return first 10 characters as hex
            return HexFormat.of().formatHex(hash).substring(0, 10);
            
        } catch (Exception e) {
            LOGGER.error("Failed to compute API hash", e);
            return "unknown";
        }
    }
    
    /**
     * Sends a packet to a player.
     */
    private void sendPacket(ServerPlayerEntity player, JsonObject packet) {
        try {
            String packetData = packet.toString();
            
            // TODO: Re-enable networking after fixing API compatibility
            // Send using simple buffer approach
            // PacketByteBuf buf = PacketByteBufs.create();
            // buf.writeString(packet.toString());
            // player.networkHandler.sendPacket(new CustomPayloadS2CPacket(RpcCustomPayload.ID, buf));
                
        } catch (Exception e) {
            LOGGER.error("Failed to send handshake packet to player: {}", player.getName().getString(), e);
        }
    }
}
