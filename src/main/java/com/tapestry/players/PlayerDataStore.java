package com.tapestry.players;

import com.tapestry.persistence.NbtConverter;
import com.tapestry.TapestryMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hybrid player data store backed by player persistent NBT.  
 * <p>
 * The in‑memory cache is used for runtime lookups; NBT is written on
 * disconnect and read on join.  All public operations are safe to call
 * from any thread – if the caller is off the main server thread the
 * request will be queued on the server.
 * </p>
 *
 * <pre>
 * NBT storage (source of truth)
 *         ↓
 * Memory cache (runtime performance)
 *         ↓
 * TypeScript API
 * </pre>
 */
public class PlayerDataStore {
    // active-player cache only
    private static final Map<UUID, Map<String, Object>> cache = new ConcurrentHashMap<>();

    // current server instance (set during SERVER_STARTED)
    private static volatile MinecraftServer serverInstance;

    private PlayerDataStore() { }

    /**
     * Must be called when the Minecraft server becomes available.
     */
    public static void setServer(MinecraftServer server) {
        serverInstance = server;
    }

    public static Object get(UUID player, String key) {
        ensureServerThread(() -> {});
        Map<String, Object> map = cache.get(player);
        return map != null ? map.get(key) : null;
    }

    public static void set(UUID player, String key, Object value) {
        ensureServerThread(() -> {
            cache
                .computeIfAbsent(player, k -> new ConcurrentHashMap<>())
                .put(key, value);
        });
    }

    public static void setBatch(UUID player, Map<String, Object> updates) {
        ensureServerThread(() -> {
            Map<String, Object> map =
                cache.computeIfAbsent(player, k -> new ConcurrentHashMap<>());
            map.putAll(updates);
        });
    }

    public static void clear(UUID player) {
        cache.remove(player);
    }

    /* ------------------------------------------------------------------ */

    /**
     * Load data from player persistent NBT into the cache.  Called on join.
     */
    public static void load(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        NbtCompound tapestry = player.getPersistentData().getCompound("tapestry");
        NbtCompound modData = tapestry.getCompound("mod_data");

        try {
            Map<String, Object> map = NbtConverter.toJavaMap(modData);
            cache.put(uuid, map);
        } catch (Exception e) {
            TapestryMod.LOGGER.error(
                "Failed to load Tapestry player data for {}", uuid, e);
            cache.put(uuid, new ConcurrentHashMap<>());
        }
    }

    /**
     * Write cached data back to the player's persistent NBT.  Called on
     * disconnect.  If the cache contains no entry for the player nothing
     * happens.
     */
    public static void save(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Map<String, Object> map = cache.get(uuid);
        if (map == null) {
            return;
        }

        NbtCompound tapestry = new NbtCompound();
        tapestry.put("mod_data", NbtConverter.toNbtCompound(map));
        player.getPersistentData().put("tapestry", tapestry);
    }

    /* ------------------------------------------------------------------ */

    private static void ensureServerThread(Runnable action) {
        MinecraftServer srv = serverInstance;
        if (srv == null || srv.isOnThread()) {
            action.run();
        } else {
            srv.execute(action);
        }
    }
}
