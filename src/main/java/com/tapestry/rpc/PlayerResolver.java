package com.tapestry.rpc;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for resolving player objects to ServerPlayerEntity instances.
 * Handles different player object representations from JavaScript.
 */
public class PlayerResolver {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerResolver.class);
    
    /**
     * Resolves a JavaScript player value to ServerPlayerEntity.
     * 
     * @param playerValue The JavaScript player object
     * @param server The current Minecraft server
     * @return ServerPlayerEntity or null if not found
     */
    public static ServerPlayerEntity resolvePlayer(Value playerValue, MinecraftServer server) {
        if (playerValue == null || server == null) {
            return null;
        }
        
        try {
            // Case 1: Player object with getUuid() method
            if (playerValue.hasMembers() && playerValue.hasMember("getUuid")) {
                Value getUuidMethod = playerValue.getMember("getUuid");
                if (getUuidMethod.canExecute()) {
                    Value uuidValue = getUuidMethod.execute();
                    String uuidString = uuidValue.asString();
                    java.util.UUID playerUuid = java.util.UUID.fromString(uuidString);
                    return server.getPlayerManager().getPlayer(playerUuid);
                }
            }
            
            // Case 2: Player object with getName() method
            if (playerValue.hasMembers() && playerValue.hasMember("getName")) {
                Value getNameMethod = playerValue.getMember("getName");
                if (getNameMethod.canExecute()) {
                    Value nameValue = getNameMethod.execute();
                    String playerName = nameValue.asString();
                    return server.getPlayerManager().getPlayer(playerName);
                }
            }
            
            // Case 3: String (player name)
            if (playerValue.isString()) {
                String playerName = playerValue.asString();
                return server.getPlayerManager().getPlayer(playerName);
            }
            
            // Case 4: Direct UUID string
            if (playerValue.isString()) {
                try {
                    java.util.UUID playerUuid = java.util.UUID.fromString(playerValue.asString());
                    return server.getPlayerManager().getPlayer(playerUuid);
                } catch (IllegalArgumentException e) {
                    // Not a UUID, treat as player name
                    String playerName = playerValue.asString();
                    return server.getPlayerManager().getPlayer(playerName);
                }
            }
            
            LOGGER.warn("Unable to resolve player from value: {}", playerValue);
            return null;
            
        } catch (Exception e) {
            LOGGER.error("Error resolving player from JavaScript value", e);
            return null;
        }
    }
    
    /**
     * Validates that a player value is in a supported format.
     */
    public static boolean isValidPlayerValue(Value playerValue) {
        if (playerValue == null) {
            return false;
        }
        
        // Check for object with methods
        if (playerValue.hasMembers()) {
            return (playerValue.hasMember("getUuid") && playerValue.getMember("getUuid").canExecute()) ||
                   (playerValue.hasMember("getName") && playerValue.getMember("getName").canExecute());
        }
        
        // Check for string (name or UUID)
        return playerValue.isString();
    }
}
