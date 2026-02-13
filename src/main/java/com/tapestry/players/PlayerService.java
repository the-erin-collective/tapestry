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
    
    private final MinecraftServer server;
    
    public PlayerService(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Gets the Minecraft server instance.
     * 
     * @return the server instance
     */
    public MinecraftServer getServer() {
        return server;
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
        PlayerManager pm = server.getPlayerManager();
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
            PlayerManager pm = server.getPlayerManager();
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
        PlayerManager pm = server.getPlayerManager();
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
        Vec3d pos = player.getPos();
        World world = player.getWorld();
        
        Map<String, Object> result = new HashMap<>();
        result.put("x", pos.x);
        result.put("y", pos.y);
        result.put("z", pos.z);
        result.put("worldId", world.getRegistryKey().getValue().toString());
        
        return result;
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
     * @return hit result or null if missed
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
        
        BlockHitResult hit = player.getWorld().raycast(context);
        
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        
        BlockPos pos = hit.getBlockPos();
        BlockState state = player.getWorld().getBlockState(pos);
        Identifier blockId = Registries.BLOCK.getId(state.getBlock());
        Direction face = hit.getSide();
        double distance = hit.getPos().distanceTo(start);
        
        Map<String, Object> result = new HashMap<>();
        result.put("blockId", blockId.toString());
        
        Map<String, Object> hitPos = new HashMap<>();
        hitPos.put("x", pos.getX());
        hitPos.put("y", pos.getY());
        hitPos.put("z", pos.getZ());
        result.put("pos", hitPos);
        
        result.put("face", face.getName());
        result.put("distance", distance);
        
        return result;
    }
    
    /**
     * Gets player game mode.
     * 
     * @param uuidString player UUID string
     * @return game mode string
     */
    public String getPlayerGameMode(String uuidString) {
        ServerPlayerEntity player = getPlayerEntity(uuidString);
        GameMode gameMode = player.interactionManager.getGameMode();
        return gameMode.getName();
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
     * Gets a player entity, throwing if not found.
     * 
     * @param uuidString player UUID string
     * @return player entity
     * @throws IllegalStateException if player not found
     */
    private ServerPlayerEntity getPlayerEntity(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            PlayerManager pm = server.getPlayerManager();
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
