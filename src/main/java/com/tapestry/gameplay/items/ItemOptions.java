package com.tapestry.gameplay.items;

/**
 * Configuration options for item registration.
 * 
 * Defines item properties like stack size, traits, food values, durability, etc.
 */
public class ItemOptions {
    private Integer stackSize;
    private String[] traits;
    private FoodComponent food;
    private Integer durability;
    private String recipeRemainder;
    private UseHandler useHandler;
    
    /**
     * Creates default item options.
     */
    public ItemOptions() {
        this.stackSize = 64;
    }
    
    /**
     * Sets the maximum stack size (1-64).
     * 
     * @param stackSize the stack size
     * @return this options object for chaining
     */
    public ItemOptions stackSize(int stackSize) {
        this.stackSize = stackSize;
        return this;
    }
    
    /**
     * Sets the gameplay traits this item possesses.
     * 
     * @param traits array of trait identifiers
     * @return this options object for chaining
     */
    public ItemOptions traits(String... traits) {
        this.traits = traits;
        return this;
    }
    
    /**
     * Sets the food component.
     * 
     * @param food the food properties
     * @return this options object for chaining
     */
    public ItemOptions food(FoodComponent food) {
        this.food = food;
        return this;
    }
    
    /**
     * Sets the durability for tools/armor.
     * 
     * @param durability the durability value
     * @return this options object for chaining
     */
    public ItemOptions durability(int durability) {
        this.durability = durability;
        return this;
    }
    
    /**
     * Sets the item returned after use (e.g., bucket from milk).
     * 
     * @param recipeRemainder the remainder item identifier
     * @return this options object for chaining
     */
    public ItemOptions recipeRemainder(String recipeRemainder) {
        this.recipeRemainder = recipeRemainder;
        return this;
    }
    
    /**
     * Sets the custom use behavior handler.
     * 
     * @param useHandler the use handler
     * @return this options object for chaining
     */
    public ItemOptions use(UseHandler useHandler) {
        this.useHandler = useHandler;
        return this;
    }
    
    // Getters
    
    public Integer getStackSize() {
        return stackSize;
    }
    
    public String[] getTraits() {
        return traits;
    }
    
    public FoodComponent getFood() {
        return food;
    }
    
    public Integer getDurability() {
        return durability;
    }
    
    public String getRecipeRemainder() {
        return recipeRemainder;
    }
    
    public UseHandler getUseHandler() {
        return useHandler;
    }
}
