package com.tapestry.gameplay.brewing;

/**
 * Internal representation of a registered brewing recipe.
 * 
 * Stores the input potion, ingredient item, and output potion.
 */
public class BrewingRecipeDefinition {
    private final String input;
    private final String ingredient;
    private final String output;
    
    /**
     * Creates a brewing recipe definition.
     * 
     * @param input the input potion identifier
     * @param ingredient the ingredient item identifier
     * @param output the output potion identifier
     */
    public BrewingRecipeDefinition(String input, String ingredient, String output) {
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
