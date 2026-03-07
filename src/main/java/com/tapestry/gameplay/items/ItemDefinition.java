package com.tapestry.gameplay.items;

/**
 * Internal representation of a registered item.
 * 
 * Stores item properties and references to Fabric item instances.
 * Provides convenient access to all item properties.
 */
public class ItemDefinition {
    private final String id;
    private final ItemOptions options;
    private Object fabricItem; // Will be set during Fabric registration
    
    /**
     * Creates a new item definition.
     * 
     * @param id the item identifier
     * @param options the item options
     */
    public ItemDefinition(String id, ItemOptions options) {
        this.id = id;
        this.options = options != null ? options : new ItemOptions();
    }
    
    /**
     * Gets the item identifier.
     * 
     * @return the item ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the item options.
     * 
     * @return the options
     */
    public ItemOptions getOptions() {
        return options;
    }
    
    /**
     * Gets the maximum stack size.
     * 
     * @return the stack size (1-64)
     */
    public int getStackSize() {
        return options.getStackSize() != null ? options.getStackSize() : 64;
    }
    
    /**
     * Gets the gameplay traits this item possesses.
     * 
     * @return array of trait identifiers, or empty array if none
     */
    public String[] getTraits() {
        return options.getTraits() != null ? options.getTraits() : new String[0];
    }
    
    /**
     * Gets the food component.
     * 
     * @return the food properties, or null if not a food item
     */
    public FoodComponent getFood() {
        return options.getFood();
    }
    
    /**
     * Gets the durability.
     * 
     * @return the durability value, or null if not a durable item
     */
    public Integer getDurability() {
        return options.getDurability();
    }
    
    /**
     * Gets the recipe remainder item.
     * 
     * @return the remainder item identifier, or null if none
     */
    public String getRecipeRemainder() {
        return options.getRecipeRemainder();
    }
    
    /**
     * Gets the use handler.
     * 
     * @return the use handler, or null if no custom behavior
     */
    public UseHandler getUseHandler() {
        return options.getUseHandler();
    }
    
    /**
     * Gets the Fabric item instance.
     * 
     * @return the Fabric item, or null if not yet registered
     */
    public Object getFabricItem() {
        return fabricItem;
    }
    
    /**
     * Sets the Fabric item instance.
     * 
     * @param fabricItem the Fabric item
     */
    public void setFabricItem(Object fabricItem) {
        this.fabricItem = fabricItem;
    }
    
    /**
     * Checks if this item has a food component.
     * 
     * @return true if this is a food item
     */
    public boolean isFood() {
        return options.getFood() != null;
    }
    
    /**
     * Checks if this item is durable (has durability).
     * 
     * @return true if this is a durable item
     */
    public boolean isDurable() {
        return options.getDurability() != null;
    }
    
    /**
     * Checks if this item has a recipe remainder.
     * 
     * @return true if this item has a remainder
     */
    public boolean hasRecipeRemainder() {
        return options.getRecipeRemainder() != null;
    }
    
    /**
     * Checks if this item has custom use behavior.
     * 
     * @return true if this item has a use handler
     */
    public boolean hasUseHandler() {
        return options.getUseHandler() != null;
    }
}
