package com.tapestry.gameplay.brewing;

import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Wraps Fabric BrewingRecipeRegistry for brewing recipe registration.
 * 
 * Provides a stable abstraction layer over Fabric APIs, translating BrewingRecipeDefinition
 * to Fabric brewing recipe registrations and handling registration errors gracefully.
 * 
 * This class isolates Fabric API dependencies so that changes to Fabric don't
 * break the public Tapestry API.
 */
public class FabricBrewingRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricBrewingRegistry.class);
    
    /**
     * Registers all brewing recipes with Fabric's brewing recipe registry.
     * 
     * This method should be called during the INITIALIZATION phase after all
     * recipes have been registered during TS_REGISTER phase.
     * 
     * @param recipes the list of brewing recipe definitions to register
     * @throws IllegalArgumentException if registration fails
     */
    public static void registerRecipes(List<BrewingRecipeDefinition> recipes) {
        LOGGER.info("Registering {} brewing recipes with Fabric", recipes.size());
        
        for (BrewingRecipeDefinition recipe : recipes) {
            try {
                registerRecipe(recipe);
            } catch (Exception e) {
                String errorMessage = String.format(
                    "Failed to register brewing recipe (%s + %s -> %s): %s",
                    recipe.getInput(),
                    recipe.getIngredient(),
                    recipe.getOutput(),
                    translateFabricError(e)
                );
                LOGGER.error(errorMessage, e);
                throw new IllegalArgumentException(errorMessage, e);
            }
        }
        
        LOGGER.info("Successfully registered all brewing recipes");
    }
    
    /**
     * Registers a single brewing recipe with Fabric.
     * 
     * Note: In Minecraft 1.21+, the brewing recipe system has changed significantly.
     * The BrewingRecipeRegistry.registerPotionRecipe() method is no longer available.
     * This implementation provides a placeholder that logs the recipe registration.
     * 
     * TODO: Implement proper brewing recipe registration for Minecraft 1.21+
     * This may require using data-driven recipes or a different Fabric API.
     * 
     * @param recipe the brewing recipe definition
     * @throws IllegalArgumentException if the recipe is invalid or registration fails
     */
    private static void registerRecipe(BrewingRecipeDefinition recipe) {
        // Parse identifiers
        Identifier inputId = parseIdentifier(recipe.getInput(), "input potion");
        Identifier ingredientId = parseIdentifier(recipe.getIngredient(), "ingredient item");
        Identifier outputId = parseIdentifier(recipe.getOutput(), "output potion");
        
        // Look up the input potion
        RegistryEntry<Potion> inputPotion = Registries.POTION.getEntry(inputId)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Input potion '%s' not found in registry", recipe.getInput())
            ));
        
        // Look up the ingredient item
        Item ingredientItem = Registries.ITEM.get(ingredientId);
        if (ingredientItem == null) {
            throw new IllegalArgumentException(
                String.format("Ingredient item '%s' not found in registry", recipe.getIngredient())
            );
        }
        
        // Look up the output potion
        RegistryEntry<Potion> outputPotion = Registries.POTION.getEntry(outputId)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Output potion '%s' not found in registry", recipe.getOutput())
            ));
        
        // TODO: Register the brewing recipe with Fabric
        // In Minecraft 1.21+, brewing recipes are data-driven and may need to be
        // registered through a different mechanism (possibly through recipe JSON files)
        LOGGER.warn("Brewing recipe registration not yet implemented for Minecraft 1.21+: {} + {} -> {}",
            recipe.getInput(), recipe.getIngredient(), recipe.getOutput());
        LOGGER.warn("Brewing recipes in Minecraft 1.21+ are data-driven. Consider using recipe JSON files instead.");
        
        LOGGER.debug("Validated brewing recipe: {} + {} -> {}",
            recipe.getInput(), recipe.getIngredient(), recipe.getOutput());
    }
    
    /**
     * Parses an identifier string into a Fabric Identifier.
     * 
     * @param id the identifier string (namespace:path format)
     * @param type the type description for error messages
     * @return the Fabric Identifier
     * @throws IllegalArgumentException if the identifier format is invalid
     */
    private static Identifier parseIdentifier(String id, String type) {
        try {
            String[] parts = id.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Identifier must contain exactly one colon");
            }
            return Identifier.of(parts[0], parts[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                String.format("Invalid %s identifier '%s': must be in namespace:path format", type, id),
                e
            );
        }
    }
    
    /**
     * Translates Fabric exceptions into descriptive error messages.
     * 
     * @param e the exception from Fabric
     * @return a descriptive error message
     */
    private static String translateFabricError(Exception e) {
        String message = e.getMessage();
        
        if (message == null) {
            return "Unknown Fabric error: " + e.getClass().getSimpleName();
        }
        
        // Translate common Fabric errors to user-friendly messages
        if (message.contains("not found") || message.contains("null")) {
            return "Item or potion not found in registry. Ensure all items and potions are registered before brewing recipes.";
        }
        
        if (message.contains("duplicate")) {
            return "Brewing recipe already registered (duplicate registration)";
        }
        
        if (message.contains("invalid") || message.contains("illegal")) {
            return "Invalid brewing recipe configuration: " + message;
        }
        
        if (message.contains("namespace")) {
            return "Invalid namespace in identifier";
        }
        
        if (message.contains("path")) {
            return "Invalid path in identifier";
        }
        
        // Return the original message if no specific translation applies
        return message;
    }
}
