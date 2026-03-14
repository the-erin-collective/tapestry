package com.tapestry.typescript;

import net.minecraft.server.MinecraftServer;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import com.tapestry.ServerContext;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.UUID;

/**
 * TypeScript API for advancement granting.
 * 
 * Provides tapestry.advancements namespace for granting advancements
 * to players during gameplay.
 */
public class AdvancementsApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancementsApi.class);
    
    // Minecraft identifier format: namespace:path
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_/.-]+$");
    
    // UUID format validation
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Creates the advancements namespace for TypeScript.
     * 
     * @return ProxyObject containing advancement APIs
     */
    public ProxyObject createNamespace() {
        Map<String, Object> advancements = new HashMap<>();
        
        advancements.put("grant", createGrantFunction());
        
        return ProxyObject.fromMap(advancements);
    }
    
    /**
     * Creates the grant function.
     */
    private ProxyExecutable createGrantFunction() {
        return args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException("advancements.grant requires exactly 2 arguments: (playerId, advancementId)");
            }
            
            String playerId = args[0].asString();
            String advancementId = args[1].asString();
            
            // Validate playerId is a valid UUID
            if (playerId == null || playerId.isBlank()) {
                throw new IllegalArgumentException("playerId must be a non-empty string");
            }
            if (!UUID_PATTERN.matcher(playerId).matches()) {
                throw new IllegalArgumentException(
                    String.format("playerId '%s' is not a valid UUID", playerId)
                );
            }
            
            // Validate advancement identifier format
            if (advancementId == null || advancementId.isBlank()) {
                throw new IllegalArgumentException("advancementId must be a non-empty string");
            }
            if (!IDENTIFIER_PATTERN.matcher(advancementId).matches()) {
                throw new IllegalArgumentException(
                    String.format("Invalid advancement identifier '%s'. Must follow format 'namespace:path'", advancementId)
                );
            }
            
            // Grant the advancement
            grantAdvancements(playerId, advancementId);
            
            LOGGER.debug("Granted advancement {} to player {}", advancementId, playerId);
            
            return null;
        };
    }
    
    /**
     * Internal method to grant an advancement to a player.
     * 
     * This method validates the advancement and grants all remaining criteria.
     * 
     * Implementation note: The actual server-side logic would:
     * 1. Look up the server's advancement loader
     * 2. Get the Advancement object by ID
     * 3. Get the player's advancement progress
     * 4. Grant all unobtained criteria
     * 
     * @param playerId player UUID as string
     * @param advancementId the advancement identifier
     */
    private void grantAdvancements(String playerId, String advancementId) {
        try {
            MinecraftServer server = ServerContext.getCurrentServer();
            
            if (server == null) {
                LOGGER.warn("Cannot grant advancement - server not available");
                return;
            }
            
            // Ensure we're on the server thread
            if (!server.isOnThread()) {
                server.execute(() -> grantAdvancements(playerId, advancementId));
                return;
            }
            
            Identifier id = Identifier.tryParse(advancementId);
            if (id == null) {
                LOGGER.warn("Advancement identifier '{}' is invalid", advancementId);
                return;
            }

            AdvancementEntry advancement = server.getAdvancementLoader().get(id);
            
            if (advancement == null) {
                LOGGER.warn("Advancement '{}' not found", id);
                return;
            }
            
            UUID uuid;
            try {
                uuid = UUID.fromString(playerId);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid player UUID '{}'", playerId);
                return;
            }
            
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            
            if (player == null) {
                LOGGER.warn("Player '{}' not found", playerId);
                return;
            }
            
            AdvancementProgress progress =
                player.getAdvancementTracker().getProgress(advancement);
            
            for (String criterion : progress.getUnobtainedCriteria()) {
                player.getAdvancementTracker().grantCriterion(advancement, criterion);
            }
            
            LOGGER.info("Granted advancement '{}' to player '{}'", advancementId, playerId);
            
        } catch (Exception e) {
            LOGGER.error("Failed to grant advancement {} to player {}: {}", 
                        advancementId, playerId, e.getMessage(), e);
            throw new RuntimeException(
                String.format("Failed to grant advancement '%s' to player '%s': %s", 
                             advancementId, playerId, e.getMessage()), e
            );
        }
    }
}
