package com.tapestry.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Emits engine events for Phase 11.
 * 
 * Handles engine:runtimeStart, engine:tick, and engine:runtimeStop events
 * with proper timing and lifecycle integration.
 */
public class EngineEventEmitter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EngineEventEmitter.class);
    
    private final EventBus eventBus;
    private volatile boolean running = false;
    private long startTime;
    
    public EngineEventEmitter(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    /**
     * Starts the engine event emitter.
     * 
     * Called when the runtime starts.
     */
    public void start() {
        if (running) {
            LOGGER.warn("Engine event emitter already started");
            return;
        }
        
        running = true;
        startTime = System.currentTimeMillis();
        
        // Emit engine:runtimeStart
        eventBus.emit(null, "engine:runtimeStart", Map.of(
            "timestamp", startTime
        ));
        
        LOGGER.info("Engine event emitter started");
    }
    
    /**
     * Emits a tick event.
     * 
     * Called each Minecraft tick for engine:tick events.
     * 
     * @param tickCount the current tick count
     */
    public void emitTick(long tickCount) {
        if (!running) {
            LOGGER.warn("Engine event emitter not started - ignoring tick");
            return;
        }
        
        // Emit engine:tick
        eventBus.emit(null, "engine:tick", Map.of(
            "tickCount", tickCount,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Stops the engine event emitter.
     * 
     * Called when the runtime is shutting down.
     */
    public void stop() {
        if (!running) {
            LOGGER.warn("Engine event emitter not started");
            return;
        }
        
        running = false;
        long stopTime = System.currentTimeMillis();
        long uptime = stopTime - startTime;
        
        // Emit engine:runtimeStop
        eventBus.emit(null, "engine:runtimeStop", Map.of(
            "timestamp", stopTime,
            "uptime", uptime
        ));
        
        LOGGER.info("Engine event emitter stopped (uptime: {}ms)", uptime);
    }
    
    /**
     * Checks if the engine event emitter is currently running.
     * 
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
}
