package com.tapestry.gameplay.traits;

/**
 * Configuration for trait registration.
 * 
 * Specifies optional tag mapping for the trait.
 */
public class TraitConfig {
    private final String tag;
    
    /**
     * Creates a trait configuration with default tag.
     */
    public TraitConfig() {
        this.tag = null;
    }
    
    /**
     * Creates a trait configuration with custom tag.
     * 
     * @param tag the Minecraft tag this trait maps to
     */
    public TraitConfig(String tag) {
        this.tag = tag;
    }
    
    /**
     * Gets the tag mapping.
     * 
     * @return the tag, or null to use default
     */
    public String getTag() {
        return tag;
    }
}
