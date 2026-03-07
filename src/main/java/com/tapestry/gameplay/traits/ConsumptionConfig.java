package com.tapestry.gameplay.traits;

/**
 * Configuration for trait consumption.
 * 
 * Declares that an entity consumes a specific trait for a behavior.
 */
public class ConsumptionConfig {
    private final String entity;
    private final String behavior;
    
    /**
     * Creates a consumption configuration.
     * 
     * @param entity the entity identifier (e.g., "minecraft:cat")
     * @param behavior the behavior type (e.g., "food", "breeding")
     */
    public ConsumptionConfig(String entity, String behavior) {
        if (entity == null || entity.isEmpty()) {
            throw new IllegalArgumentException("Entity identifier cannot be null or empty");
        }
        if (behavior == null || behavior.isEmpty()) {
            throw new IllegalArgumentException("Behavior type cannot be null or empty");
        }
        
        this.entity = entity;
        this.behavior = behavior;
    }
    
    /**
     * Gets the entity identifier.
     * 
     * @return the entity identifier
     */
    public String getEntity() {
        return entity;
    }
    
    /**
     * Gets the behavior type.
     * 
     * @return the behavior type
     */
    public String getBehavior() {
        return behavior;
    }
}
