package com.tapestry.gameplay.traits;

/**
 * Configuration for trait registration.
 * 
 * Specifies optional tag mapping for the trait.
 */
public class TraitConfig {
    private final String tag;
    private final String extendsTrait;
    
    /**
     * Creates a trait configuration with default tag and no parent.
     */
    public TraitConfig() {
        this(null, null);
    }
    
    /**
     * Creates a trait configuration with custom tag and no parent.
     * 
     * @param tag the Minecraft tag this trait maps to
     */
    public TraitConfig(String tag) {
        this(tag, null);
    }
    
    /**
     * Creates a trait configuration with custom tag and an optional parent trait.
     * 
     * @param tag the Minecraft tag this trait maps to
     * @param extendsTrait name of another trait this one extends (may be null)
     */
    public TraitConfig(String tag, String extendsTrait) {
        this.tag = tag;
        this.extendsTrait = extendsTrait;
    }
    
    /**
     * Gets the tag mapping.
     * 
     * @return the tag, or null to use default
     */
    public String getTag() {
        return tag;
    }
    
    /**
     * Gets the name of the parent trait this trait extends.
     * 
     * @return the parent trait name, or null if none
     */
    public String getExtendsTrait() {
        return extendsTrait;
    }
}
