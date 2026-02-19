package com.tapestry.rpc;

import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter for RPC calls per client.
 * Prevents excessive RPC traffic that could overwhelm the server.
 */
public class RateLimiter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiter.class);
    
    // Configuration limits
    private static final int MAX_CONCURRENT_CALLS_PER_CLIENT = 100;
    private static final int MAX_CALLS_PER_SECOND_PER_CLIENT = 10;
    private static final int MAX_CALLS_PER_MINUTE_PER_CLIENT = 100;
    
    // Per-client tracking
    private final Map<UUID, ClientMetrics> clientMetrics = new ConcurrentHashMap<>();
    
    /**
     * Checks if a client can make an RPC call.
     * 
     * @param player The player making the call
     * @return true if allowed, false if rate limited
     */
    public boolean canMakeCall(ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }
        
        UUID playerId = player.getUuid();
        ClientMetrics metrics = clientMetrics.computeIfAbsent(playerId, 
            id -> new ClientMetrics());
        
        long currentTime = System.currentTimeMillis();
        
        // Check concurrent call limit
        if (metrics.concurrentCalls.get() >= MAX_CONCURRENT_CALLS_PER_CLIENT) {
            LOGGER.warn("Rate limiting player {} - too many concurrent calls: {}", 
                        player.getName().getString(), metrics.concurrentCalls.get());
            return false;
        }
        
        // Check per-second limit
        metrics.cleanupOldCalls(currentTime);
        if (metrics.callsInLastSecond >= MAX_CALLS_PER_SECOND_PER_CLIENT) {
            LOGGER.warn("Rate limiting player {} - too many calls per second: {}", 
                        player.getName().getString(), metrics.callsInLastSecond);
            return false;
        }
        
        // Check per-minute limit
        if (metrics.callsInLastMinute >= MAX_CALLS_PER_MINUTE_PER_CLIENT) {
            LOGGER.warn("Rate limiting player {} - too many calls per minute: {}", 
                        player.getName().getString(), metrics.callsInLastMinute);
            return false;
        }
        
        // Record this call
        metrics.recordCall(currentTime);
        return true;
    }
    
    /**
     * Records the completion of an RPC call.
     * 
     * @param player The player who made the call
     */
    public void recordCallCompletion(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUuid();
        ClientMetrics metrics = clientMetrics.get(playerId);
        if (metrics != null) {
            metrics.concurrentCalls.decrementAndGet();
        }
    }
    
    /**
     * Removes all metrics for a player (typically on disconnect).
     * 
     * @param player The player to remove
     */
    public void removePlayer(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        
        ClientMetrics removed = clientMetrics.remove(player.getUuid());
        if (removed != null) {
            LOGGER.debug("Removed rate limiting metrics for player: {}", 
                        player.getName().getString());
        }
    }
    
    /**
     * Gets current metrics for debugging.
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> result = new ConcurrentHashMap<>();
        result.put("totalClients", clientMetrics.size());
        result.put("maxConcurrentPerClient", MAX_CONCURRENT_CALLS_PER_CLIENT);
        result.put("maxPerSecondPerClient", MAX_CALLS_PER_SECOND_PER_CLIENT);
        result.put("maxPerMinutePerClient", MAX_CALLS_PER_MINUTE_PER_CLIENT);
        return result;
    }
    
    /**
     * Tracks per-client RPC call metrics.
     */
    private static class ClientMetrics {
        final AtomicInteger concurrentCalls = new AtomicInteger(0);
        final Set<Long> recentCalls = ConcurrentHashMap.newKeySet();
        int callsInLastSecond = 0;
        int callsInLastMinute = 0;
        
        void recordCall(long timestamp) {
            concurrentCalls.incrementAndGet();
            recentCalls.add(timestamp);
        }
        
        void cleanupOldCalls(long currentTime) {
            long oneSecondAgo = currentTime - 1000;
            long oneMinuteAgo = currentTime - 60000;
            
            // Remove calls older than 1 minute
            recentCalls.removeIf(timestamp -> timestamp < oneMinuteAgo);
            
            // Count calls in last second and minute
            callsInLastSecond = 0;
            callsInLastMinute = 0;
            
            for (Long callTime : recentCalls) {
                if (callTime >= oneMinuteAgo) {
                    callsInLastMinute++;
                }
                if (callTime >= oneSecondAgo) {
                    callsInLastSecond++;
                }
            }
        }
    }
}
