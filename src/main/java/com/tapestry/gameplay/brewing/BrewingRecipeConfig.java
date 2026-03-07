package com.tapestry.gameplay.brewing;

/**
 * Configuration for brewing recipe registration.
 * 
 * Defines the input potion, ingredient item, and output potion.
 */
public class BrewingRecipeConfig {
    private final String input;
    private final String ingredient;
    private final String output;
    
    /**
     * Creates a brewing recipe configuration.
     * 
     * @param input the input potion identifier
     * @param ingredient the ingredient item identifier
     * @param output the output potion identifier
     */
    public BrewingRecipeConfig(String input, String ingredient, String output) {
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
    }
    
    /**
     * Gets the input potion identifier.
     * 
     * @return the input potion
     */
    public String getInput() {
        return input;
    }
    
    /**
     * Gets the ingredient item identifier.
     * 
     * @return the ingredient item
     */
    public String getIngredient() {
        return ingredient;
    }
    
    /**
     * Gets the output potion identifier.
     * 
     * @return the output potion
     */
    public String getOutput() {
        return output;
    }
}
