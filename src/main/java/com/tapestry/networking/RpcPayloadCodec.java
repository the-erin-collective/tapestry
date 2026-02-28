package com.tapestry.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Codec for RpcPayload serialization.
 * 
 * Minecraft 1.20.5+ uses codecs for automatic serialization.
 * This replaces manual buffer read/write operations.
 */
public class RpcPayloadCodec {
    
    public static final PacketCodec<ByteBuf, RpcPayload> CODEC =
        PacketCodec.tuple(
            PacketCodecs.STRING,
            RpcPayload::json,
            RpcPayload::new
        );
}
