package com.tapestry.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
 * Handles incoming RPC packets on the server side.
 * This is a single entry point for all RPC traffic.
 */
public class RpcPacketHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcPacketHandler.class);
    private static final int PROTOCOL_VERSION = 1;
    
    private final RpcServerRuntime rpcServerRuntime;
    private final HandshakeHandler handshakeHandler;
    
    public RpcPacketHandler(RpcServerRuntime rpcServerRuntime, HandshakeHandler handshakeHandler) {
        this.rpcServerRuntime = rpcServerRuntime;
        this.handshakeHandler = handshakeHandler;
    }
    
    /**
     * Handle RPC packet from JSON string (used by new registration pattern).
     */
    public void handle(ServerPlayerEntity player, String json) {
        try {
            // Decode packet data
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();
            
            // Validate protocol version
            if (!data.has("protocol") || data.get("protocol").getAsInt() != PROTOCOL_VERSION) {
                LOGGER.warn("Received packet with invalid protocol version from player: {}", player.getName().getString());
                handshakeHandler.sendHandshakeFail(player, "Protocol mismatch");
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
