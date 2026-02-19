package com.tapestry.rpc;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Immutable context object injected into server API method calls.
 * Contains authoritative server objects that should not cross the network boundary.
 */
public class ServerContext {
    
    private final ServerPlayerEntity player;
    private final MinecraftServer server;
    private final UUID connectionId;
    
    public ServerContext(ServerPlayerEntity player, MinecraftServer server, UUID connectionId) {
        this.player = player;
        this.server = server;
        this.connectionId = connectionId;
    }
    
    public ServerPlayerEntity getPlayer() {
        return player;
    }
    
    public MinecraftServer getServer() {
        return server;
    }
    
    public UUID getConnectionId() {
        return connectionId;
    }
}
