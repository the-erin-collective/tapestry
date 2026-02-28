package com.tapestry.networking;

/**
 * Simple data class for RPC packet data.
 * The actual packet handling is done through Fabric's networking API.
 */
public record RpcCustomPayload(String json) {
    
    public RpcCustomPayload {
        // Validate json is not null
        if (json == null) {
            throw new IllegalArgumentException("JSON data cannot be null");
        }
    }
}
