package com.tapestry.gameplay.brewing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FabricBrewingRegistry.
 * 
 * Note: These tests validate the abstraction layer logic without requiring Minecraft runtime.
 * Full integration tests with Fabric would require a Minecraft test environment.
 * 
 * Since FabricBrewingRegistry accesses Minecraft registries, we can only test
 * the validation logic that happens before registry access.
 */
@DisplayName("FabricBrewingRegistry Tests")
public class FabricBrewingRegistryTest {
    
    @Test
    @DisplayName("Should handle empty recipe list without errors")
    void testEmptyRecipeList() {
        List<BrewingRecipeDefinition> recipes = new ArrayList<>();
        
        // Empty list should not cause any issues
        // Note: This will succeed because no registry access is attempted
        assertDoesNotThrow(() -> FabricBrewingRegistry.registerRecipes(recipes));
    }
    
    @Test
    @DisplayName("Should create valid recipe definitions")
    void testRecipeDefinitionCreation() {
        // Test that recipe definitions can be created with valid identifiers
        BrewingRecipeDefinition recipe = new BrewingRecipeDefinition(
            "minecraft:awkward",
            "minecraft:sugar",
            "minecraft:swiftness"
        );
        
        assertEquals("minecraft:awkward", recipe.getInput());
        assertEquals("minecraft:sugar", recipe.getIngredient());
        assertEquals("minecraft:swiftness", recipe.getOutput());
    }
    
    @Test
    @DisplayName("Should create recipe definitions with paths")
    void testRecipeDefinitionWithPaths() {
        BrewingRecipeDefinition recipe = new BrewingRecipeDefinition(
            "mymod:custom/potion",
            "mymod:ingredient/special",
            "mymod:output/strong"
        );
        
        assertEquals("mymod:custom/potion", recipe.getInput());
        assertEquals("mymod:ingredient/special", recipe.getIngredient());
        assertEquals("mymod:output/strong", recipe.getOutput());
    }
    
    @Test
    @DisplayName("Should store multiple recipe definitions")
    void testMultipleRecipeDefinitions() {
        List<BrewingRecipeDefinition> recipes = new ArrayList<>();
        
        recipes.add(new BrewingRecipeDefinition(
            "minecraft:awkward",
            "minecraft:sugar",
            "minecraft:swiftness"
        ));
        
        recipes.add(new BrewingRecipeDefinition(
            "minecraft:water",
            "minecraft:nether_wart",
            "minecraft:awkward"
        ));
        
        assertEquals(2, recipes.size());
        assertEquals("minecraft:awkward", recipes.get(0).getInput());
        assertEquals("minecraft:water", recipes.get(1).getInput());
    }
}
