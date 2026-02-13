package com.tapestry.typescript;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.players.PlayerService;
import com.tapestry.typescript.TypeScriptRuntime;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

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
}
