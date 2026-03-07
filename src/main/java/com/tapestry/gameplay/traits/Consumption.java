package com.tapestry.gameplay.traits;

/**
 * Represents a trait consumption declaration.
 * 
 * Declares that an entity consumes items with a specific trait for a behavior.
 */
public class Consumption {
    private final String traitName;
    private final String entity;
    private final String behavior;
    
    /**
     * Creates a consumption declaration.
     * 
     * @param traitName the trait being consumed
     * @param entity the entity identifier
     * @param behavior the behavior type
     */
    public Consumption(String traitName, String entity, String behavior) {
        this.traitName = traitName;
        this.entity = entity;
        this.behavior = behavior;
    }
    
    /**
     * Gets the trait name.
     * 
     * @return the trait name
     */
    public String getTraitName() {
        return traitName;
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
