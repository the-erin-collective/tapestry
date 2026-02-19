package com.tapestry.rpc.client;

import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Registry for pending RPC calls on the client side.
 * Handles timeouts and promise resolution.
 */
public class PendingCallRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PendingCallRegistry.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    
    private final Map<String, PendingCall> pending = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * Registers a pending RPC call.
     */
    public void register(String id, CompletableFuture<JsonElement> future) {
        PendingCall call = new PendingCall(future, System.currentTimeMillis());
        pending.put(id, call);
        
        // Schedule timeout
        timeoutExecutor.schedule(() -> {
            PendingCall expiredCall = pending.remove(id);
            if (expiredCall != null) {
                expiredCall.future.completeExceptionally(
                    new RpcTimeoutException("RPC call timed out after " + DEFAULT_TIMEOUT_SECONDS + " seconds"));
                LOGGER.debug("RPC call timed out: {}", id);
            }
        }, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        LOGGER.debug("Registered pending RPC call: {}", id);
    }
    
    /**
     * Resolves a pending call with a successful result.
     */
    public void resolve(String id, JsonElement result) {
        PendingCall call = pending.remove(id);
        if (call != null) {
            call.future.complete(result);
            LOGGER.debug("Resolved RPC call: {}", id);
        } else {
            LOGGER.warn("Received response for unknown RPC call: {}", id);
        }
    }
    
    /**
     * Rejects a pending call with an error.
     */
    public void reject(String id, RpcException error) {
        PendingCall call = pending.remove(id);
        if (call != null) {
            call.future.completeExceptionally(error);
            LOGGER.debug("Rejected RPC call: {}, error: {}", id, error.getMessage());
        } else {
            LOGGER.warn("Received error for unknown RPC call: {}", id);
        }
    }
    
    /**
     * Gets the number of pending calls.
     */
    public int getPendingCount() {
        return pending.size();
    }
    
    /**
     * Clears all pending calls (for shutdown).
     */
    public void clear() {
        pending.clear();
        timeoutExecutor.shutdown();
    }
    
    /**
     * Represents a pending RPC call.
     */
    private static class PendingCall {
        final CompletableFuture<JsonElement> future;
        final long timestamp;
        
        PendingCall(CompletableFuture<JsonElement> future, long timestamp) {
            this.future = future;
            this.timestamp = timestamp;
        }
    }
}
