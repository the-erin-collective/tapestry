package com.tapestry.networking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record RpcCustomPayload(String json) {

    public static final Identifier ID = Identifier.of("tapestry", "rpc");

    public void write(PacketByteBuf buf) {
        buf.writeString(json);
    }
}
