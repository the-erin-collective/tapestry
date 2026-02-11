package com.tapestry.typescript;

/**
 * Represents a TypeScript mod definition registered via tapestry.mod.define().
 * 
 * This record stores the metadata and callbacks for a single mod.
 */
public record TsModDefinition(
    String id,
    Object onLoad,
    Object onEnable,
    String source
) {
    
    /**
     * Creates a new TsModDefinition.
     * 
     * @param id the mod identifier (validated format)
     * @param onLoad the onLoad function (required)
     * @param onEnable the onEnable function (optional, may be null)
     * @param source the source file/resource path
     */
    public TsModDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Mod ID cannot be null or blank");
        }
        if (onLoad == null) {
            throw new IllegalArgumentException("onLoad function cannot be null");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Source cannot be null or blank");
        }
    }
    
    /**
     * Checks if this mod has an onEnable function.
     * 
     * @return true if onEnable is not null
     */
    public boolean hasOnEnable() {
        return onEnable != null;
    }
    
    /**
     * Gets the mod ID.
     * 
     * @return the mod identifier
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the onLoad function.
     * 
     * @return the onLoad function
     */
    public Object getOnLoad() {
        return onLoad;
    }
    
    /**
     * Gets the onEnable function.
     * 
     * @return the onEnable function, or null if not provided
     */
    public Object getOnEnable() {
        return onEnable;
    }
    
    /**
     * Gets the source location.
     * 
     * @return the source file/resource path
     */
    public String getSource() {
        return source;
    }
}
