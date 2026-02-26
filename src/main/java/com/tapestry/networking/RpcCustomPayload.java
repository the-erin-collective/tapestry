package com.tapestry.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record RpcCustomPayload(String json) implements CustomPayload {

    public static final Id<CustomPayload> ID = new Id<>(Identifier.of("tapestry", "rpc"));
    public static final PacketCodec<ByteBuf,RpcCustomPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, RpcCustomPayload::json,
        RpcCustomPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
