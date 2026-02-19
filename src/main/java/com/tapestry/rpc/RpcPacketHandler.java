package com.tapestry.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tapestry.networking.RpcCustomPayload;

/**
 * Phase 16.5: Secure RPC packet handler with pre-deserialization limits.
 * Enforces size limits before JSON parsing to prevent memory bombs.
 */
public class RpcPacketHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcPacketHandler.class);
    
    // Pre-deserialization size limits
    private static final int MAX_JSON_BYTES = 65536; // 64KB
    private static final int MAX_JSON_CHARS = 16384; // 16KB characters
    private static final int PROTOCOL_VERSION = 1;
    
    private final RpcServerRuntime rpcServerRuntime;
    private final HandshakeHandler handshakeHandler;
    
    public RpcPacketHandler(RpcServerRuntime rpcServerRuntime, HandshakeHandler handshakeHandler) {
        this.rpcServerRuntime = rpcServerRuntime;
        this.handshakeHandler = handshakeHandler;
    }
    
    /**
     * Handle RPC packet from JSON string with security validation.
     */
    public void handle(ServerPlayerEntity player, String json) {
        try {
            // Security: Pre-deserialization size check
            if (json.length() > MAX_JSON_CHARS) {
                LOGGER.warn("RPC packet too large from player {}: {} chars", 
                           player.getName().getString(), json.length());
                handshakeHandler.sendHandshakeFail(player, "Packet too large");
                return;
            }
            
            // Parse JSON with error handling
            JsonObject data;
            try {
                data = JsonParser.parseString(json).getAsJsonObject();
            } catch (JsonParseException e) {
                LOGGER.warn("Invalid JSON from player {}: {}", 
                           player.getName().getString(), e.getMessage());
                handshakeHandler.sendHandshakeFail(player, "Invalid JSON: " + e.getMessage());
                return;
            }
            
            // Validate protocol version
            if (!data.has("protocol") || data.get("protocol").getAsInt() != PROTOCOL_VERSION) {
                LOGGER.warn("Received packet with invalid protocol version from player: {}", player.getName().getString());
                handshakeHandler.sendHandshakeFail(player, "Protocol mismatch");
                return;
            }
            
            // Security: Post-deserialization sanitization
            Object sanitized;
            try {
                sanitized = RpcSanitizer.sanitize(data);
            } catch (RpcValidationException e) {
                LOGGER.warn("RPC validation failed from player {}: {}", 
                           player.getName().getString(), e.getMessage());
                handshakeHandler.sendHandshakeFail(player, "Validation failed: " + e.getMessage());
                return;
            }
            
            // Route based on packet type
            String packetType = data.get("type").getAsString();
            switch (packetType) {
                case "hello" -> handshakeHandler.handleHello(player, data);
                case "rpc_call" -> rpcServerRuntime.handleCall(player, data);
                default -> {
                    LOGGER.warn("Unknown packet type '{}' from player: {}", packetType, player.getName().getString());
                    // Silently ignore unknown packet types as per spec
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling RPC packet from player: {}", player.getName().getString(), e);
            // Don't crash connection, just log error
        }
    }
    
    /**
     * Gets RPC channel identifier.
     */
    public static net.minecraft.util.Identifier getChannelId() {
        return net.minecraft.util.Identifier.of("tapestry", "rpc");
    }
}
