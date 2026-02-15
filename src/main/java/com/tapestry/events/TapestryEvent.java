package com.tapestry.events;

/**
 * Represents a structured event in the Phase 11 event system.
 * 
 * This is a primitive, clean event object with no cancellation,
 * no propagation control, and no validation complexity.
 */
public class TapestryEvent {
    
    private final String name;
    private final String namespace;
    private final Object payload;
    private final String emitterModId;
    private final long timestamp;
    
    public TapestryEvent(String name, String namespace, Object payload, String emitterModId) {
        this.name = name;
        this.namespace = namespace;
        this.payload = payload;
        this.emitterModId = emitterModId;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Gets the full event name (e.g., "mod:inventory:update").
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the namespace (engine, ui, or mod).
     */
    public String getNamespace() {
        return namespace;
    }
    
    /**
     * Gets the event payload data.
     */
    public Object getPayload() {
        return payload;
    }
    
    /**
     * Gets the mod ID that emitted this event.
     * Null for engine events.
     */
    public String getEmitterModId() {
        return emitterModId;
    }
    
    /**
     * Gets the timestamp when this event was created.
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("TapestryEvent{name='%s', namespace='%s', emitter='%s', timestamp=%d}", 
                              name, namespace, emitterModId, timestamp);
    }
}
