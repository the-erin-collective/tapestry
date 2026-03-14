package com.tapestry.typescript;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import com.tapestry.ServerContext;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.players.PlayerService;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Exposes player services to TypeScript.
 * Enforces RUNTIME phase and validates arguments strictly.
 */
public class PlayersApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayersApi.class);
    
    private PlayerService playerService;
    
    public PlayersApi(PlayerService playerService) {
        this.playerService = playerService;
    }
    
    /**
     * Helper method to check if player service is available.
     * @throws RuntimeException if player service is not available
     */
    private void ensurePlayerServiceAvailable() {
        if (playerService == null) {
            throw new RuntimeException("Player service is not available on client side");
        }
    }
    
    /**
     * Updates the PlayerService instance.
     * 
     * @param playerService the player service
     */
    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }
    
    /**
     * Creates the players namespace for TypeScript.
     * 
     * @return ProxyObject containing all player APIs
     */
    public ProxyObject createNamespace() {
        Map<String, Object> players = new HashMap<>();
        
        // Player identity & discovery
        players.put("list", createListFunction());
        players.put("get", createGetFunction());
        players.put("findByName", createFindByNameFunction());
        
        // Player messaging
        players.put("sendChat", createSendChatFunction());
        players.put("sendActionBar", createSendActionBarFunction());
        players.put("sendTitle", createSendTitleFunction());
        
        // Player queries
        players.put("getPosition", createGetPositionFunction());
        players.put("getLook", createGetLookFunction());
        players.put("getGameMode", createGetGameModeFunction());
        
        // Raycasting
        players.put("raycastBlock", createRaycastBlockFunction());
        
        // Phase 2: Player data persistence
        players.put("getData", createGetDataFunction());
        players.put("setData", createSetDataFunction());
        players.put("setBatch", createSetBatchFunction());
        
        return ProxyObject.fromMap(players);
    }
    
    private ProxyExecutable createListFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 0) {
                throw new IllegalArgumentException("players.list requires no arguments");
            }
            
            try {
                return playerService.listPlayers();
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.list failed for mod: {}", modId, e);
                throw new RuntimeException("players.list failed", e);
            }
        };
    }
    
    private ProxyExecutable createGetFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 1) {
                throw new IllegalArgumentException("players.get requires exactly 1 argument: (uuid)");
            }
            
            String uuid = args[0].asString();
            if (uuid == null || uuid.isBlank()) {
                throw new IllegalArgumentException("UUID must be a non-empty string");
            }
            
            try {
                return playerService.getPlayer(uuid);
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.get failed for mod: {}, uuid: {}", modId, uuid, e);
                throw new RuntimeException("players.get failed", e);
            }
        };
    }
    
    private ProxyExecutable createFindByNameFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 1) {
                throw new IllegalArgumentException("players.findByName requires exactly 1 argument: (name)");
            }
            
            String name = args[0].asString();
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Name must be a non-empty string");
            }
            
            try {
                return playerService.findPlayerByName(name);
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.findByName failed for mod: {}, name: {}", modId, name, e);
                throw new RuntimeException("players.findByName failed", e);
            }
        };
    }
    
    private ProxyExecutable createSendChatFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 2) {
                throw new IllegalArgumentException("players.sendChat requires exactly 2 arguments: (uuid, message)");
            }
            
            String uuid = args[0].asString();
            String message = args[1].asString();
            
            if (uuid == null || uuid.isBlank()) {
                throw new IllegalArgumentException("UUID must be a non-empty string");
            }
            if (message == null) {
                throw new IllegalArgumentException("Message cannot be null");
            }
            
            try {
                playerService.sendChat(uuid, message);
                return null;
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.sendChat failed for mod: {}, uuid: {}", modId, uuid, e);
                throw new RuntimeException("players.sendChat failed", e);
            }
        };
    }
    
    private ProxyExecutable createSendActionBarFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 2) {
                throw new IllegalArgumentException("players.sendActionBar requires exactly 2 arguments: (uuid, message)");
            }
            
            String uuid = args[0].asString();
            String message = args[1].asString();
            
            if (uuid == null || uuid.isBlank()) {
                throw new IllegalArgumentException("UUID must be a non-empty string");
            }
            if (message == null) {
                throw new IllegalArgumentException("Message cannot be null");
            }
            
            try {
                playerService.sendActionBar(uuid, message);
                return null;
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.sendActionBar failed for mod: {}, uuid: {}", modId, uuid, e);
                throw new RuntimeException("players.sendActionBar failed", e);
            }
        };
    }
    
    private ProxyExecutable createSendTitleFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 2 && args.length != 6) {
                throw new IllegalArgumentException("players.sendTitle requires 2 arguments: (uuid, options) or 6 arguments: (uuid, title, subtitle, fadeIn, stay, fadeOut)");
            }
            
            String uuid = args[0].asString();
            if (uuid == null || uuid.isBlank()) {
                throw new IllegalArgumentException("UUID must be a non-empty string");
            }
            
            String title, subtitle;
            int fadeIn = 10, stay = 40, fadeOut = 10;
            
            if (args.length == 2) {
                // Options object form - handle Graal Value
                Value opts = args[1];
                if (opts == null || !opts.hasMembers()) {
                    throw new IllegalArgumentException("Options cannot be null and must be an object");
                }
                
                title = opts.hasMember("title") ? opts.getMember("title").asString() : null;
                subtitle = opts.hasMember("subtitle") ? opts.getMember("subtitle").asString() : null;
                
                if (opts.hasMember("fadeIn")) {
                    fadeIn = opts.getMember("fadeIn").asInt();
                }
                if (opts.hasMember("stay")) {
                    stay = opts.getMember("stay").asInt();
                }
                if (opts.hasMember("fadeOut")) {
                    fadeOut = opts.getMember("fadeOut").asInt();
                }
            } else {
                // Individual arguments form
                title = args[1].asString();
                subtitle = args[2].asString();
                fadeIn = args[3].isNumber() ? args[3].asInt() : 10;
                stay = args[4].isNumber() ? args[4].asInt() : 40;
                fadeOut = args[5].isNumber() ? args[5].asInt() : 10;
            }
            
            if (title == null) {
                throw new IllegalArgumentException("Title cannot be null");
            }
            
            try {
                playerService.sendTitle(uuid, title, subtitle, fadeIn, stay, fadeOut);
                return null;
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.sendTitle failed for mod: {}, uuid: {}", modId, uuid, e);
                throw new RuntimeException("players.sendTitle failed", e);
            }
        };
    }
    
    private ProxyExecutable createGetPositionFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 1) {
                throw new IllegalArgumentException("players.getPosition requires exactly 1 argument: (uuid)");
            }
            
            String uuid = args[0].asString();
            if (uuid == null || uuid.isBlank()) {
                throw new IllegalArgumentException("UUID must be a non-empty string");
            }
            
            try {
                return playerService.getPlayerPosition(uuid);
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.getPosition failed for mod: {}, uuid: {}", modId, uuid, e);
                throw new RuntimeException("players.getPosition failed", e);
            }
        };
    }
    
    private ProxyExecutable createGetLookFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 1) {
                throw new IllegalArgumentException("players.getLook requires exactly 1 argument: (uuid)");
            }
            
            String uuid = args[0].asString();
            if (uuid == null || uuid.isBlank()) {
                throw new IllegalArgumentException("UUID must be a non-empty string");
            }
            
            try {
                return playerService.getPlayerLook(uuid);
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.getLook failed for mod: {}, uuid: {}", modId, uuid, e);
                throw new RuntimeException("players.getLook failed", e);
            }
        };
    }
    
    private ProxyExecutable createGetGameModeFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 1) {
                throw new IllegalArgumentException("players.getGameMode requires exactly 1 argument: (uuid)");
            }
            
            String uuid = args[0].asString();
            if (uuid == null || uuid.isBlank()) {
                throw new IllegalArgumentException("UUID must be a non-empty string");
            }
            
            try {
                return playerService.getPlayerGameMode(uuid);
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.getGameMode failed for mod: {}, uuid: {}", modId, uuid, e);
                throw new RuntimeException("players.getGameMode failed", e);
            }
        };
    }
    
    private ProxyExecutable createRaycastBlockFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 2) {
                throw new IllegalArgumentException("players.raycastBlock requires exactly 2 arguments: (uuid, options)");
            }
            
            String uuid = args[0].asString();
            if (uuid == null || uuid.isBlank()) {
                throw new IllegalArgumentException("UUID must be a non-empty string");
            }
            
            Value opts = args[1];
            if (opts == null || !opts.hasMembers()) {
                throw new IllegalArgumentException("Options cannot be null and must be an object");
            }
            
            if (!opts.hasMember("maxDistance")) {
                throw new IllegalArgumentException("maxDistance is required in options");
            }
            
            double maxDistance = opts.getMember("maxDistance").asDouble();
            boolean includeFluids = opts.hasMember("includeFluids") && opts.getMember("includeFluids").asBoolean();
            
            try {
                return playerService.raycastBlock(uuid, maxDistance, includeFluids);
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.raycastBlock failed for mod: {}, uuid: {}", modId, uuid, e);
                throw new RuntimeException("players.raycastBlock failed", e);
            }
        };
    }
    
    /**
     * Creates the getData function for retrieving player data.
     */
    private ProxyExecutable createGetDataFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 2) {
                throw new IllegalArgumentException("players.getData requires exactly 2 arguments: (playerId, key)");
            }
            
            String playerId = args[0].asString();
            String key = args[1].asString();
            
            if (playerId == null || playerId.isBlank()) {
                throw new IllegalArgumentException("playerId must be a non-empty string (UUID)");
            }
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("key must be a non-empty string");
            }
            
            try {
                // Under the new architecture the PlayerDataStore is backed by
                // NBT; the memory cache is the source used here.  The store
                // itself already handles server-thread dispatch.
                UUID uuid;
                try {
                    uuid = UUID.fromString(playerId);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid player UUID '{}'", playerId);
                    return null;
                }
                
                Object valObj = com.tapestry.players.PlayerDataStore.get(uuid, key);
                return valObj;
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.getData failed for mod: {}, playerId: {}, key: {}", modId, playerId, key, e);
                throw new RuntimeException("players.getData failed", e);
            }
        };
    }
    
    /**
     * Creates the setData function for storing player data.
     */
    private ProxyExecutable createSetDataFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 3) {
                throw new IllegalArgumentException("players.setData requires exactly 3 arguments: (playerId, key, value)");
            }
            
            String playerId = args[0].asString();
            String key = args[1].asString();
            Value valueArg = args[2];
            
            if (playerId == null || playerId.isBlank()) {
                throw new IllegalArgumentException("playerId must be a non-empty string (UUID)");
            }
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("key must be a non-empty string");
            }
            
            try {
                // Convert Value to appropriate Java type
                Object value = valueToJava(valueArg);
                
                UUID uuid;
                try {
                    uuid = UUID.fromString(playerId);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid player UUID '{}'", playerId);
                    return null;
                }
                com.tapestry.players.PlayerDataStore.set(uuid, key, value);
                return value;
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.setData failed for mod: {}, playerId: {}, key: {}", modId, playerId, key, e);
                throw new RuntimeException("players.setData failed", e);
            }
        };
    }
    
    /**
     * Creates the setBatch function for atomic batch player data operations.
     */
    private ProxyExecutable createSetBatchFunction() {
        return args -> {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
            
            if (args.length != 2) {
                throw new IllegalArgumentException("players.setBatch requires exactly 2 arguments: (playerId, dataObject)");
            }
            
            String playerId = args[0].asString();
            Value dataObject = args[1];
            
            if (playerId == null || playerId.isBlank()) {
                throw new IllegalArgumentException("playerId must be a non-empty string (UUID)");
            }
            if (!dataObject.hasMembers()) {
                throw new IllegalArgumentException("dataObject must be an object with key-value pairs");
            }
            
            try {
                // Extract all key-value pairs
                Set<String> keys = dataObject.getMemberKeys();
                Map<String, Object> batchData = new HashMap<>();
                
                if (keys.size() > 1000) {
                    throw new IllegalArgumentException(
                        String.format("Batch size %d exceeds maximum of 1000", keys.size())
                    );
                }
                
                for (String key : keys) {
                    Value val = dataObject.getMember(key);
                    batchData.put(key, valueToJava(val));
                }
                
                MinecraftServer server = ServerContext.getCurrentServer();
                if (server == null) {
                    LOGGER.warn("Cannot set batch player data - server not available");
                    return null;
                }
                
                // Ensure we're on the server thread
                if (!server.isOnThread()) {
                    LOGGER.warn("players.setBatch called from non-server thread - operation not supported");
                    return null;
                }
                
                UUID uuid;
                try {
                    uuid = UUID.fromString(playerId);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid player UUID '{}'", playerId);
                    return null;
                }
                
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                if (player == null) {
                    LOGGER.warn("Player '{}' not found for batch data storage", playerId);
                    return null;
                }
                
                // Atomic batch update in our data store
                com.tapestry.players.PlayerDataStore.setBatch(uuid, batchData);
                return null;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                String modId = TypeScriptRuntime.getCurrentModId();
                LOGGER.error("players.setBatch failed for mod: {}, playerId: {}", modId, playerId, e);
                throw new RuntimeException("players.setBatch failed", e);
            }
        };
    }
    
    /**
     * Converts a Graal Value to a Java object.
     * Supports: null, boolean, number, string, array, object
     */
    private Object valueToJava(Value val) {
        if (val.isNull()) {
            return null;
        }
        if (val.isBoolean()) {
            return val.asBoolean();
        }
        if (val.isNumber()) {
            if (val.fitsInInt()) {
                return val.asInt();
            } else if (val.fitsInLong()) {
                return val.asLong();
            } else {
                return val.asDouble();
            }
        }
        if (val.isString()) {
            return val.asString();
        }
        if (val.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            for (long i = 0; i < val.getArraySize(); i++) {
                list.add(valueToJava(val.getArrayElement(i)));
            }
            return list;
        }
        if (val.hasMembers()) {
            Map<String, Object> map = new HashMap<>();
            Set<String> keys = val.getMemberKeys();
            for (String key : keys) {
                map.put(key, valueToJava(val.getMember(key)));
            }
            return map;
        }
        
        // Fallback: return as string
        return val.asString();
    }
    
}

