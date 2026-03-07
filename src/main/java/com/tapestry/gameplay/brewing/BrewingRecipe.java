package com.tapestry.gameplay.brewing;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides stable API for registering potion brewing recipes.
 * 
 * Brewing recipes define transformations from input potion + ingredient to output potion.
 * All registration must occur during TS_REGISTER phase.
 */
public class BrewingRecipe {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrewingRecipe.class);
    
    private final List<BrewingRecipeDefinition> recipes = new ArrayList<>();
    
    /**
     * Registers a new brewing recipe.
     * 
     * @param config the recipe configuration
     * @throws IllegalStateException if called outside TS_REGISTER phase
     * @throws IllegalArgumentException if validation fails
     */
    public void register(BrewingRecipeConfig config) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        validateConfig(config);
        
        BrewingRecipeDefinition definition = new BrewingRecipeDefinition(
            config.getInput(),
            config.getIngredient(),
            config.getOutput()
        );
        
        recipes.add(definition);
        
        LOGGER.info("Registered brewing recipe: {} + {} -> {}",
            config.getInput(), config.getIngredient(), config.getOutput());
    }
    
    /**
     * Gets all registered brewing recipes.
     * 
     * @return unmodifiable list of recipes
     */
    public List<BrewingRecipeDefinition> getAllRecipes() {
        return Collections.unmodifiableList(recipes);
    }
    
    /**
     * Validates brewing recipe configuration.
     * 
     * @param config the configuration to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateConfig(BrewingRecipeConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Brewing recipe configuration cannot be null");
        }
        
        validateIdentifier(config.getInput(), "input potion");
        validateIdentifier(config.getIngredient(), "ingredient item");
        validateIdentifier(config.getOutput(), "output potion");
    }
    
    /**
     * Validates an identifier format.
     * 
     * @param identifier the identifier to validate
     * @param type the type description for error messages
     * @throws IllegalArgumentException if format is invalid
     */
    private void validateIdentifier(String identifier, String type) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException(type + " cannot be null or empty");
        }
        
        if (!identifier.matches("^[a-z0-9_.-]+:[a-z0-9_.-]+(/[a-z0-9_.-]+)*$")) {
            throw new IllegalArgumentException(
                "Invalid " + type + " format: '" + identifier + "'. Must follow namespace:path format."
            );
        }
    }
}
