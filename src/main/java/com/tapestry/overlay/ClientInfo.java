package com.tapestry.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Read-only snapshot of client information provided to overlays.
 * Data is pushed to overlays rather than pulled, ensuring thread safety.
 */
public class ClientInfo {
    
    private final PlayerInfo player;
    private final WorldInfo world;
    private final long tick;
    private final int fps;
    
    public ClientInfo(PlayerInfo player, WorldInfo world, long tick, int fps) {
        this.player = player;
        this.world = world;
        this.tick = tick;
        this.fps = fps;
    }
    
    /**
     * Gets player information snapshot.
     * 
     * @return the player info
     */
    public PlayerInfo getPlayer() {
        return player;
    }
    
    /**
     * Gets world information snapshot.
     * 
     * @return the world info
     */
    public WorldInfo getWorld() {
        return world;
    }
    
    /**
     * Gets the total game ticks elapsed.
     * 
     * @return the tick count
     */
    public long getTick() {
        return tick;
    }
    
    /**
     * Gets the current client FPS.
     * 
     * @return the FPS
     */
    public int getFps() {
        return fps;
    }
    
    /**
     * Creates a ClientInfo snapshot from the current Minecraft client state.
     * 
     * @param client the Minecraft client
     * @return a new ClientInfo snapshot
     */
    public static ClientInfo fromClient(MinecraftClient client) {
        PlayerInfo playerInfo = null;
        WorldInfo worldInfo = null;
        
        // Capture player information
        if (client.player != null) {
            playerInfo = PlayerInfo.fromPlayer(client.player);
        }
        
        // Capture world information
        if (client.world != null) {
            worldInfo = WorldInfo.fromWorld(client.world);
        }
        
        // Get tick and FPS information
        long tick = client.world != null ? client.world.getTime() : 0;
        int fps = 60; // Default FPS - actual FPS tracking would require more complex integration
        
        return new ClientInfo(playerInfo, worldInfo, tick, fps);
    }
    
    /**
     * Player information snapshot.
     */
    public static class PlayerInfo {
        private final Vec3d position;
        private final String dimension;
        private final float health;
        private final int foodLevel;
        private final float armor;
        
        public PlayerInfo(Vec3d position, String dimension, float health, int foodLevel, float armor) {
            this.position = position;
            this.dimension = dimension;
            this.health = health;
            this.foodLevel = foodLevel;
            this.armor = armor;
        }
        
        public Vec3d getPosition() { return position; }
        public String getDimension() { return dimension; }
        public float getHealth() { return health; }
        public int getFoodLevel() { return foodLevel; }
        public float getArmor() { return armor; }
        
        /**
         * Creates a PlayerInfo snapshot from a player entity.
         */
        public static PlayerInfo fromPlayer(PlayerEntity player) {
            Vec3d pos = player.getPos();
            String dimension = player.getWorld().getRegistryKey().getValue().toString();
            float health = player.getHealth();
            int foodLevel = player.getHungerManager().getFoodLevel();
            float armor = player.getArmor();
            
            return new PlayerInfo(pos, dimension, health, foodLevel, armor);
        }
    }
    
    /**
     * World information snapshot.
     */
    public static class WorldInfo {
        private final long time;
        private final boolean isRaining;
        private final String biome;
        
        public WorldInfo(long time, boolean isRaining, String biome) {
            this.time = time;
            this.isRaining = isRaining;
            this.biome = biome;
        }
        
        public long getTime() { return time; }
        public boolean isRaining() { return isRaining; }
        public String getBiome() { return biome; }
        
        /**
         * Creates a WorldInfo snapshot from a client world.
         */
        public static WorldInfo fromWorld(ClientWorld world) {
            long time = world.getTime();
            boolean isRaining = world.isRaining();
            String biome = "unknown";
            
            // Get biome at player position if available
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                biome = world.getBiome(player.getBlockPos()).getKey().toString();
            }
            
            return new WorldInfo(time, isRaining, biome);
        }
    }
}
