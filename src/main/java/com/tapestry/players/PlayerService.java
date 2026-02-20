package com.tapestry.players;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.registry.Registries;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * Backend service for player operations.
 * Integrates with Fabric API and vanilla server systems.
 * Provides snapshot extraction and messaging methods.
 */
public class PlayerService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerService.class);
    
    private MinecraftServer server;
    
    public PlayerService(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Guard method to ensure server is available.
     * Fails fast with clear error message if server is not ready.
     * 
     * @return the server instance
     * @throws IllegalStateException if server is not available
     */
    private MinecraftServer requireServer() {
        if (server == null) {
            throw new IllegalStateException("PlayerService not ready: server not available yet");
        }
        return server;
    }
    
    /**
     * Gets the Minecraft server instance.
     * 
     * @return the server instance
     */
    public MinecraftServer getServer() {
        return requireServer();
    }
    
    /**
     * Updates the server instance (for when server becomes available).
     * 
     * @param server the Minecraft server instance
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Gets all online players.
     * 
     * @return array of player snapshots
     */
    public Object[] listPlayers() {
        PlayerManager pm = requireServer().getPlayerManager();
        Collection<ServerPlayerEntity> players = pm.getPlayerList();
        
        return players.stream()
            .map(this::createPlayerSnapshot)
            .toArray();
    }
    
    /**
     * Gets a player by UUID.
     * 
     * @param uuidString player UUID string
     * @return player snapshot or null if not found
     */
    public Object getPlayer(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            PlayerManager pm = requireServer().getPlayerManager();
            ServerPlayerEntity player = pm.getPlayer(uuid);
            
            if (player == null) {
                return null;
            }
            
            return createPlayerSnapshot(player);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuidString, e);
        }
    }
    
    /**
     * Finds a player by name.
     * 
     * @param name player name
     * @return player snapshot or null if not found
     */
    public Object findPlayerByName(String name) {
        PlayerManager pm = requireServer().getPlayerManager();
        ServerPlayerEntity player = pm.getPlayer(name);
        
        if (player == null) {
            return null;
        }
        
        return createPlayerSnapshot(player);
    }
    
    /**
     * Sends a chat message to a player.
     * 
     * @param uuidString player UUID string
     * @param message message to send
     */
    public void sendChat(String uuidString, String message) {
        ServerPlayerEntity player = getPlayerEntity(uuidString);
        player.sendMessage(Text.literal(message), false);
    }
    
    /**
     * Sends an action bar message to a player.
     * 
     * @param uuidString player UUID string
     * @param message message to send
     */
    public void sendActionBar(String uuidString, String message) {
        ServerPlayerEntity player = getPlayerEntity(uuidString);
        player.sendMessage(Text.literal(message), true);
    }
    
    /**
     * Sends a title to a player.
     * 
     * @param uuidString player UUID string
     * @param title title text
     * @param subtitle subtitle text (can be null)
     * @param fadeIn fade in time
     * @param stay display time
     * @param fadeOut fade out time
     */
    public void sendTitle(String uuidString, String title, String subtitle, 
                      int fadeIn, int stay, int fadeOut) {
        ServerPlayerEntity player = getPlayerEntity(uuidString);
        
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(title)));
        
        if (subtitle != null) {
            player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(subtitle)));
        }
        
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
    }
    
    /**
     * Gets player position and world.
     * 
     * @param uuidString player UUID string
     * @return position snapshot
     */
    public Object getPlayerPosition(String uuidString) {
        ServerPlayerEntity player = getPlayerEntity(uuidString);
        Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
        // TODO: Fix ServerPlayerEntity world access
        // World world = player.level();
        
        Map<String, Object> result = new HashMap<>();
        result.put("x", pos.x);
        result.put("y", pos.y);
        result.put("z", pos.z);
        // TODO: Fix mapping issues - temporarily disabled
        return null;
    }
    
    /**
     * Gets player look direction.
     * 
     * @param uuidString player UUID string
     * @return look snapshot
     */
    public Object getPlayerLook(String uuidString) {
        ServerPlayerEntity player = getPlayerEntity(uuidString);
        Vec3d look = player.getRotationVec(1.0F);
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        
        Map<String, Object> result = new HashMap<>();
        result.put("yaw", yaw);
        result.put("pitch", pitch);
        
        Map<String, Object> dir = new HashMap<>();
        dir.put("x", look.x);
        dir.put("y", look.y);
        dir.put("z", look.z);
        result.put("dir", dir);
        
        return result;
    }
    
    /**
     * Performs a raycast for block targeting.
     * 
     * @param uuidString player UUID string
     * @param maxDistance maximum raycast distance
     * @param includeFluids whether to include fluids
     * @return hit result map with block info, or null map if missed
     */
    public Object raycastBlock(String uuidString, double maxDistance, boolean includeFluids) {
        // Enforce safety limits
        if (maxDistance <= 0 || maxDistance > 32.0) {
            throw new IllegalArgumentException("maxDistance must be between 0 and 32");
        }
        
        ServerPlayerEntity player = getPlayerEntity(uuidString);
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(maxDistance));
        
        RaycastContext context = new RaycastContext(
            start,
            end,
            RaycastContext.ShapeType.OUTLINE,
            includeFluids 
                ? RaycastContext.FluidHandling.ANY 
                : RaycastContext.FluidHandling.NONE,
            player
        );
        
        // Get the player's world and execute raycast
        // TODO: Use player's actual dimension instead of overworld
        ServerWorld world = server.getOverworld();
        HitResult hitResult = world.raycast(context);
        
        Map<String, Object> result = new HashMap<>();
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Identifier blockId = Registries.BLOCK.getId(state.getBlock());
            
            result.put("hit", true);
            
            Map<String, Object> blockPos = new HashMap<>();
            blockPos.put("x", pos.getX());
            blockPos.put("y", pos.getY());
            blockPos.put("z", pos.getZ());
            result.put("blockPos", blockPos);
            
            result.put("blockId", blockId.toString());
            result.put("blockName", state.getBlock().getName().getString());
            
            Direction side = blockHit.getSide();
            result.put("side", side.asString());
        } else {
            result.put("hit", false);
        }
        
        return result;
    }
    
    /**
     * Creates a TS-safe player snapshot.
     * 
     * @param player the player entity
     * @return player snapshot map
     */
    private Map<String, Object> createPlayerSnapshot(ServerPlayerEntity player) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("uuid", player.getUuidAsString());
        snapshot.put("name", player.getName().getString());
        return snapshot;
    }
    
    /**
     * @return game mode string
     */
    public String getPlayerGameMode(String uuidString) {
        ServerPlayerEntity player = getPlayerEntity(uuidString);
        GameMode gameMode = player.interactionManager.getGameMode();
        return gameMode.asString();
    }
    
    /**
     * Gets a player entity, throwing if not found.
     * 
     * @param uuidString player UUID string
     * @return player entity
     * @throws IllegalStateException if player not found
     */
    private ServerPlayerEntity getPlayerEntity(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            PlayerManager pm = requireServer().getPlayerManager();
            ServerPlayerEntity player = pm.getPlayer(uuid);
            
            if (player == null) {
                throw new IllegalStateException("Player not online: " + uuidString);
            }
            
            return player;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuidString, e);
        }
    }
}
