package com.tapestry.players;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerDataStoreTest {
    private ServerPlayerEntity player;
    private UUID uuid;
    private NbtCompound root;

    @BeforeEach
    void setup() {
        player = mock(ServerPlayerEntity.class);
        uuid = UUID.randomUUID();
        when(player.getUuid()).thenReturn(uuid);
        root = new NbtCompound();
        when(player.getPersistentData()).thenReturn(root);
        // ensure server is set to avoid null checks in store
        PlayerDataStore.setServer(mock(net.minecraft.server.MinecraftServer.class));
    }

    @Test
    void loadEmptyCreatesEmptyCache() {
        // root contains no tapestry data
        PlayerDataStore.load(player);
        assertNull(PlayerDataStore.get(uuid, "nonexistent"));
    }

    @Test
    void setAndSavePersistsToNbt() {
        PlayerDataStore.set(uuid, "foo", 123);
        PlayerDataStore.save(player);

        assertTrue(root.contains("tapestry"));
        NbtCompound tapestry = root.getCompound("tapestry");
        NbtCompound modData = tapestry.getCompound("mod_data");
        assertEquals(123, modData.getInt("foo"));
    }

    @Test
    void loadRestoresValues() {
        // manually construct persistent data
        NbtCompound tapestry = new NbtCompound();
        NbtCompound modData = new NbtCompound();
        modData.putInt("hello", 42);
        tapestry.put("mod_data", modData);
        root.put("tapestry", tapestry);

        PlayerDataStore.load(player);
        Object val = PlayerDataStore.get(uuid, "hello");
        assertEquals(42, val);
    }

    @Test
    void setBatchMergesAtomically() {
        Map<String, Object> batch = Map.of("a", 1, "b", 2);
        PlayerDataStore.setBatch(uuid, batch);
        assertEquals(1, PlayerDataStore.get(uuid, "a"));
        assertEquals(2, PlayerDataStore.get(uuid, "b"));
    }

    @Test
    void offThreadOperationsAreDeferred() {
        net.minecraft.server.MinecraftServer fake = mock(net.minecraft.server.MinecraftServer.class);
        when(fake.isOnThread()).thenReturn(false);
        // execute should run the runnable immediately for test
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(fake).execute(any(Runnable.class));

        PlayerDataStore.setServer(fake);
        PlayerDataStore.set(uuid, "z", "deferred");
        assertEquals("deferred", PlayerDataStore.get(uuid, "z"));
    }
}
