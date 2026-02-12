package com.tapestry.runtime;

import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Factory for creating immutable context objects for runtime callbacks.
 * 
 * Provides deterministic, read-only context objects for events,
 * scheduler callbacks, and other runtime interactions.
 */
public class RuntimeContextFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeContextFactory.class);
    
    /**
     * Creates a scheduler callback context.
     * 
     * @param modId the mod ID
     * @param tick the current tick
     * @param handle the task handle
     * @return immutable context object
     */
    public static ProxyObject createSchedulerContext(String modId, long tick, String handle) {
        Map<String, Object> context = new TreeMap<>();
        context.put("modId", modId);
        context.put("tick", tick);
        context.put("handle", handle);
        
        return ProxyObject.fromMap(context);
    }
    
    /**
     * Creates a player join event context.
     * 
     * @param modId the mod ID
     * @param playerUuid the player UUID
     * @param playerName the player name
     * @param worldId the world ID
     * @param tick the current tick
     * @return immutable context object
     */
    public static ProxyObject createPlayerJoinContext(String modId, String playerUuid, 
                                                      String playerName, String worldId, long tick) {
        Map<String, Object> context = new TreeMap<>();
        context.put("modId", modId);
        context.put("tick", tick);
        context.put("worldId", worldId);
        
        // Player object
        Map<String, Object> player = new TreeMap<>();
        player.put("uuid", playerUuid);
        player.put("name", playerName);
        context.put("player", ProxyObject.fromMap(player));
        
        return ProxyObject.fromMap(context);
    }
    
    /**
     * Creates a player quit event context.
     * 
     * @param modId the mod ID
     * @param playerUuid the player UUID
     * @param playerName the player name
     * @param worldId the world ID
     * @param tick the current tick
     * @return immutable context object
     */
    public static ProxyObject createPlayerQuitContext(String modId, String playerUuid, 
                                                     String playerName, String worldId, long tick) {
        Map<String, Object> context = new TreeMap<>();
        context.put("modId", modId);
        context.put("tick", tick);
        context.put("worldId", worldId);
        
        // Player object
        Map<String, Object> player = new TreeMap<>();
        player.put("uuid", playerUuid);
        player.put("name", playerName);
        context.put("player", ProxyObject.fromMap(player));
        
        return ProxyObject.fromMap(context);
    }
    
    /**
     * Creates a player chat event context.
     * 
     * @param modId the mod ID
     * @param playerUuid the player UUID
     * @param playerName the player name
     * @param message the chat message
     * @param worldId the world ID
     * @param tick the current tick
     * @return immutable context object
     */
    public static ProxyObject createPlayerChatContext(String modId, String playerUuid, 
                                                     String playerName, String message, 
                                                     String worldId, long tick) {
        Map<String, Object> context = new TreeMap<>();
        context.put("modId", modId);
        context.put("tick", tick);
        context.put("worldId", worldId);
        context.put("message", message);
        
        // Player object
        Map<String, Object> player = new TreeMap<>();
        player.put("uuid", playerUuid);
        player.put("name", playerName);
        context.put("player", ProxyObject.fromMap(player));
        
        return ProxyObject.fromMap(context);
    }
    
    /**
     * Creates a block break event context.
     * 
     * @param modId the mod ID
     * @param playerUuid the player UUID
     * @param playerName the player name
     * @param worldId the world ID
     * @param position the block position
     * @param blockId the block ID
     * @param tick the current tick
     * @return immutable context object
     */
    public static ProxyObject createBlockBreakContext(String modId, String playerUuid, 
                                                      String playerName, String worldId,
                                                      Position position, String blockId, long tick) {
        Map<String, Object> context = new TreeMap<>();
        context.put("modId", modId);
        context.put("tick", tick);
        context.put("worldId", worldId);
        context.put("blockId", blockId);
        
        // Player object
        Map<String, Object> player = new TreeMap<>();
        player.put("uuid", playerUuid);
        player.put("name", playerName);
        context.put("player", ProxyObject.fromMap(player));
        
        // Position object
        Map<String, Object> pos = new TreeMap<>();
        pos.put("x", position.x());
        pos.put("y", position.y());
        pos.put("z", position.z());
        context.put("pos", ProxyObject.fromMap(pos));
        
        return ProxyObject.fromMap(context);
    }
    
    /**
     * Creates a block place event context.
     * 
     * @param modId the mod ID
     * @param playerUuid the player UUID
     * @param playerName the player name
     * @param worldId the world ID
     * @param position the block position
     * @param blockId the block ID
     * @param tick the current tick
     * @return immutable context object
     */
    public static ProxyObject createBlockPlaceContext(String modId, String playerUuid, 
                                                      String playerName, String worldId,
                                                      Position position, String blockId, long tick) {
        Map<String, Object> context = new TreeMap<>();
        context.put("modId", modId);
        context.put("tick", tick);
        context.put("worldId", worldId);
        context.put("blockId", blockId);
        
        // Player object
        Map<String, Object> player = new TreeMap<>();
        player.put("uuid", playerUuid);
        player.put("name", playerName);
        context.put("player", ProxyObject.fromMap(player));
        
        // Position object
        Map<String, Object> pos = new TreeMap<>();
        pos.put("x", position.x());
        pos.put("y", position.y());
        pos.put("z", position.z());
        context.put("pos", ProxyObject.fromMap(pos));
        
        return ProxyObject.fromMap(context);
    }
    
    /**
     * Creates a world tick event context.
     * 
     * @param modId the mod ID
     * @param worldId the world ID
     * @param tick the current tick
     * @return immutable context object
     */
    public static ProxyObject createWorldTickContext(String modId, String worldId, long tick) {
        Map<String, Object> context = new TreeMap<>();
        context.put("modId", modId);
        context.put("tick", tick);
        context.put("worldId", worldId);
        
        return ProxyObject.fromMap(context);
    }
    
    /**
     * Creates a generic event context with custom fields.
     * 
     * @param modId the mod ID
     * @param eventType the event type
     * @param tick the current tick
     * @param additionalFields additional context fields
     * @return immutable context object
     */
    public static ProxyObject createGenericContext(String modId, String eventType, long tick,
                                                   Map<String, Object> additionalFields) {
        Map<String, Object> context = new TreeMap<>();
        context.put("modId", modId);
        context.put("tick", tick);
        context.put("eventType", eventType);
        
        if (additionalFields != null) {
            // Sort additional fields for deterministic ordering
            Map<String, Object> sortedFields = new TreeMap<>(additionalFields);
            context.putAll(sortedFields);
        }
        
        return ProxyObject.fromMap(context);
    }
    
    /**
     * Represents a 3D position.
     */
    public static class Position {
        private final double x;
        private final double y;
        private final double z;
        
        public Position(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public double x() { return x; }
        public double y() { return y; }
        public double z() { return z; }
        
        @Override
        public String toString() {
            return String.format("Position{x=%.2f, y=%.2f, z=%.2f}", x, y, z);
        }
    }
}
