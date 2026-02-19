package com.tapestry.rpc.client;

/**
 * Handles client-side packet registration for RPC system.
 */
public class RpcClientNetworkRegistrar {

    public static void register() {
        // TODO: Re-enable networking after fixing API compatibility
        // ClientPlayNetworking.registerGlobalReceiver(RpcCustomPayload.ID,
        // (client, handler, buf, responseSender) -> {
        //     String json = buf.readString(32767);
        //     client.execute(() -> {
        //         RpcClientRuntime.handle(json);
        //     });
        // });
    }
}
