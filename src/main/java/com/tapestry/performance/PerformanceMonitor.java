package com.tapestry.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring system for Phase 10.5.
 * 
 * Enforces performance limits and tracks resource usage according to
 * the Phase 10.5 specifications.
 */
public class PerformanceMonitor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceMonitor.class);
    private static PerformanceMonitor instance;
    
    // Performance limits from Phase 10.5 spec
    private static final int MAX_MODS = 200;
    private static final int MAX_DEPENDENCY_DEPTH = 50;
    private static final long MAX_TEMPLATE_SIZE = 100 * 1024; // 100 KB
    private static final int MAX_TEMPLATE_NODE_COUNT = 1000;
    private static final long MAX_ACTIVATION_TIME_PER_MOD = 5000; // 5 seconds
    
    // Performance tracking
    private final Map<String, PerformanceMetrics> modMetrics = new HashMap<>();
    private final AtomicLong totalTemplateProcessingTime = new AtomicLong(0);
    private final AtomicLong totalTemplateCount = new AtomicLong(0);
    
    private PerformanceMonitor() {}
    
    public static synchronized PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }
    
    /**
     * Checks if the mod count limit would be exceeded.
     * 
     * @param currentCount current number of mods
     * @param additionalMods number of mods to add
     * @throws PerformanceLimitException if limit would be exceeded
     */
    public void checkModCountLimit(int currentCount, int additionalMods) {
        if (currentCount + additionalMods > MAX_MODS) {
            throw new PerformanceLimitException(
                String.format("Mod count limit exceeded: %d + %d > %d", 
                             currentCount, additionalMods, MAX_MODS));
        }
    }
    
    /**
     * Checks dependency depth limit.
     * 
     * @param depth the dependency depth
     * @throws PerformanceLimitException if limit exceeded
     */
    public void checkDependencyDepthLimit(int depth) {
        if (depth > MAX_DEPENDENCY_DEPTH) {
            throw new PerformanceLimitException(
                String.format("Dependency depth limit exceeded: %d > %d", depth, MAX_DEPENDENCY_DEPTH));
        }
    }
    
    /**
     * Checks template size limit.
     * 
     * @param templateSize the template size in bytes
     * @throws PerformanceLimitException if limit exceeded
     */
    public void checkTemplateSizeLimit(long templateSize) {
        if (templateSize > MAX_TEMPLATE_SIZE) {
            throw new PerformanceLimitException(
                String.format("Template size limit exceeded: %d bytes > %d bytes", 
                             templateSize, MAX_TEMPLATE_SIZE));
        }
    }
    
    /**
     * Checks template node count limit.
     * 
     * @param nodeCount the number of nodes
     * @throws PerformanceLimitException if limit exceeded
     */
    public void checkTemplateNodeCountLimit(int nodeCount) {
        if (nodeCount > MAX_TEMPLATE_NODE_COUNT) {
            throw new PerformanceLimitException(
                String.format("Template node count limit exceeded: %d > %d", 
                             nodeCount, MAX_TEMPLATE_NODE_COUNT));
        }
    }
    
    /**
     * Starts timing mod activation.
     * 
     * @param modId the mod ID
     * @return a timer that can be stopped later
     */
    public ActivationTimer startModActivationTiming(String modId) {
        return new ActivationTimer(modId);
    }
    
    /**
     * Records template processing metrics.
     * 
     * @param processingTimeMs processing time in milliseconds
     * @param nodeCount number of nodes processed
     */
    public void recordTemplateProcessing(long processingTimeMs, int nodeCount) {
        totalTemplateProcessingTime.addAndGet(processingTimeMs);
        totalTemplateCount.incrementAndGet();
        
        LOGGER.debug("Template processing: {}ms, {} nodes", processingTimeMs, nodeCount);
    }
    
    /**
     * Gets performance statistics.
     * 
     * @return performance statistics
     */
    public PerformanceStats getStats() {
        long avgTemplateTime = totalTemplateCount.get() > 0 ? 
            totalTemplateProcessingTime.get() / totalTemplateCount.get() : 0;
        
        return new PerformanceStats(
            modMetrics.size(),
            totalTemplateCount.get(),
            avgTemplateTime,
            totalTemplateProcessingTime.get()
        );
    }
    
    /**
     * Gets metrics for a specific mod.
     * 
     * @param modId the mod ID
     * @return the mod's performance metrics
     */
    public PerformanceMetrics getModMetrics(String modId) {
        return modMetrics.get(modId);
    }
    
    /**
     * Resets all performance metrics.
     */
    public void reset() {
        modMetrics.clear();
        totalTemplateProcessingTime.set(0);
        totalTemplateCount.set(0);
        LOGGER.info("Performance metrics reset");
    }
    
    /**
     * Timer for tracking mod activation duration.
     */
    public class ActivationTimer {
        private final String modId;
        private final long startTime;
        
        private ActivationTimer(String modId) {
            this.modId = modId;
            this.startTime = System.currentTimeMillis();
        }
        
        /**
         * Stops the timer and records the activation time.
         * 
         * @throws PerformanceLimitException if activation took too long
         */
        public void stop() {
            long activationTime = System.currentTimeMillis() - startTime;
            
            if (activationTime > MAX_ACTIVATION_TIME_PER_MOD) {
                throw new PerformanceLimitException(
                    String.format("Mod activation time limit exceeded for '%s': %dms > %dms", 
                                 modId, activationTime, MAX_ACTIVATION_TIME_PER_MOD));
            }
            
            // Record metrics
            PerformanceMetrics metrics = modMetrics.computeIfAbsent(modId, k -> new PerformanceMetrics());
            metrics.recordActivation(activationTime);
            
            LOGGER.debug("Mod '{}' activated in {}ms", modId, activationTime);
        }
    }
    
    /**
     * Performance metrics for a mod.
     */
    public static class PerformanceMetrics {
        private long activationTime;
        private int activationCount;
        
        public void recordActivation(long timeMs) {
            this.activationTime = timeMs;
            this.activationCount++;
        }
        
        public long getActivationTime() { return activationTime; }
        public int getActivationCount() { return activationCount; }
        
        @Override
        public String toString() {
            return String.format("PerformanceMetrics{activationTime=%dms, count=%d}", 
                               activationTime, activationCount);
        }
    }
    
    /**
     * Overall performance statistics.
     */
    public record PerformanceStats(
        int modCount,
        long totalTemplateCount,
        long avgTemplateTimeMs,
        long totalTemplateTimeMs
    ) {}
    
    /**
     * Exception thrown when performance limits are exceeded.
     */
    public static class PerformanceLimitException extends RuntimeException {
        public PerformanceLimitException(String message) {
            super(message);
        }
    }
}
