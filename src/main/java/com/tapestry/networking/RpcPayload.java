package com.tapestry.networking;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Universal RPC payload for all Tapestry communication.
 * 
 * This replaces the old packet system with modern Minecraft 1.20.5+ approach.
 * Minecraft only sees a JSON string - Tapestry handles routing internally.
 */
public record RpcPayload(String json) implements CustomPayload {

    public static final Id<RpcPayload> ID =
        new Id<>(Identifier.of("tapestry", "rpc"));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
