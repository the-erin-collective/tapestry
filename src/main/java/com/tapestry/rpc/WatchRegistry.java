package com.tapestry.rpc;

import com.google.gson.JsonElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for server-side watch system.
 * Tracks which players are watching which state keys.
 * Thin helper over emitTo system.
 */
public class WatchRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchRegistry.class);
    
    // Server instance for player lookup
    private MinecraftServer server;
    
    // Maps: watchKey -> Set of player UUIDs
    private final Map<String, Set<java.util.UUID>> watchers = new ConcurrentHashMap<>();
    
    /**
     * Creates a new WatchRegistry.
     */
    public WatchRegistry(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Updates the server instance (called when server becomes available).
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Registers a player's interest in a watch key.
     * 
     * @param player The player to register
     * @param watchKey The key to watch (e.g., "machineProgress:machineId")
     */
    public void watch(ServerPlayerEntity player, String watchKey) {
        watchers.computeIfAbsent(watchKey, k -> ConcurrentHashMap.newKeySet())
               .add(player.getUuid());
        
        LOGGER.debug("Player {} is now watching: {}", player.getName().getString(), watchKey);
    }
    
    /**
     * Removes a player's interest in a watch key.
     */
    public void unwatch(ServerPlayerEntity player, String watchKey) {
        Set<java.util.UUID> playerSet = watchers.get(watchKey);
        if (playerSet != null) {
            playerSet.remove(player.getUuid());
            if (playerSet.isEmpty()) {
                watchers.remove(watchKey);
            }
        }
        
        LOGGER.debug("Player {} stopped watching: {}", player.getName().getString(), watchKey);
    }
    
    /**
     * Removes all watches for a player (typically on disconnect).
     */
    public void removeAllWatches(ServerPlayerEntity player) {
        java.util.UUID playerId = player.getUuid();
        watchers.entrySet().removeIf(entry -> {
            entry.getValue().remove(playerId);
            return entry.getValue().isEmpty();
        });
        
        LOGGER.debug("Removed all watches for player: {}", player.getName().getString());
    }
    
    /**
     * Emits a watched value to all interested players.
     * 
     * @param watchKey The watch key
     * @param payload The data to send
     */
    public void emitWatched(String watchKey, JsonElement payload) {
        Set<java.util.UUID> interestedPlayers = watchers.get(watchKey);
        if (interestedPlayers == null || interestedPlayers.isEmpty()) {
            return;
        }
        
        // Send to each interested player
        for (java.util.UUID playerId : interestedPlayers) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player != null && player.networkHandler != null) {
                // Create event name from watch key
                String eventName = "watch:" + watchKey;
                ServerEventSender.sendToPlayer(player, eventName, payload);
            }
        }
        
        LOGGER.debug("Emitted watched value for {} to {} players", watchKey, interestedPlayers.size());
    }
    
    /**
     * Gets the number of watchers for a key.
     */
    public int getWatcherCount(String watchKey) {
        Set<java.util.UUID> playerSet = watchers.get(watchKey);
        return playerSet != null ? playerSet.size() : 0;
    }
    
    /**
     * Gets all watch keys.
     */
    public Set<String> getAllWatchKeys() {
        return watchers.keySet();
    }
    
    /**
     * Clears all watches (for shutdown).
     */
    public void clear() {
        watchers.clear();
        LOGGER.debug("Watch registry cleared");
    }
}
