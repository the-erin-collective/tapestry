package com.tapestry.rpc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players have completed the RPC handshake.
 * Thread-safe for concurrent access.
 */
public class HandshakeRegistry {
    
    private final Set<UUID> readyConnections = new HashSet<>();
    
    /**
     * Marks a player connection as handshake-complete.
     */
    public synchronized void markReady(UUID playerId) {
        readyConnections.add(playerId);
    }
    
    /**
     * Checks if a player has completed handshake.
     */
    public synchronized boolean isReady(UUID playerId) {
        return readyConnections.contains(playerId);
    }
    
    /**
     * Checks if a player has completed handshake.
     * Convenience method for ServerPlayerEntity.
     */
    public boolean isReady(net.minecraft.server.network.ServerPlayerEntity player) {
        return isReady(player.getUuid());
    }
    
    /**
     * Removes a player from the registry (typically on disconnect).
     */
    public synchronized void remove(UUID playerId) {
        readyConnections.remove(playerId);
    }
    
    /**
     * Gets the count of ready connections.
     */
    public synchronized int getReadyCount() {
        return readyConnections.size();
    }
}
