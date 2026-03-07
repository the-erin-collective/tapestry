package com.tapestry.gameplay.items;

/**
 * Defines food properties for an item.
 * 
 * Specifies hunger restoration, saturation, and eating behavior.
 */
public class FoodComponent {
    private final int hunger;
    private final float saturation;
    private final boolean alwaysEdible;
    private final boolean snack;
    
    /**
     * Creates a food component.
     * 
     * @param hunger hunger points restored (0-20)
     * @param saturation saturation modifier (0.0-1.0)
     */
    public FoodComponent(int hunger, float saturation) {
        this(hunger, saturation, false, false);
    }
    
    /**
     * Creates a food component with all options.
     * 
     * @param hunger hunger points restored (0-20)
     * @param saturation saturation modifier (0.0-1.0)
     * @param alwaysEdible can eat when not hungry
     * @param snack fast eating animation
     */
    public FoodComponent(int hunger, float saturation, boolean alwaysEdible, boolean snack) {
        this.hunger = hunger;
        this.saturation = saturation;
        this.alwaysEdible = alwaysEdible;
        this.snack = snack;
    }
    
    /**
     * Gets the hunger points restored.
     * 
     * @return hunger points (0-20)
     */
    public int getHunger() {
        return hunger;
    }
    
    /**
     * Gets the saturation modifier.
     * 
     * @return saturation (0.0-1.0)
     */
    public float getSaturation() {
        return saturation;
    }
    
    /**
     * Checks if this food can be eaten when not hungry.
     * 
     * @return true if always edible
     */
    public boolean isAlwaysEdible() {
        return alwaysEdible;
    }
    
    /**
     * Checks if this food uses fast eating animation.
     * 
     * @return true if snack
     */
    public boolean isSnack() {
        return snack;
    }
}
