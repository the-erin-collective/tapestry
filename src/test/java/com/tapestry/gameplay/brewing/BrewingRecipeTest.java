package com.tapestry.gameplay.brewing;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BrewingRecipe registration.
 */
@DisplayName("BrewingRecipe Tests")
public class BrewingRecipeTest {
    
    private BrewingRecipe brewingRecipe;
    
    @BeforeEach
    void setUp() {
        PhaseController.reset();
        PhaseController controller = PhaseController.getInstance();
        
        // Advance to TS_REGISTER phase
        controller.advanceTo(TapestryPhase.DISCOVERY);
        controller.advanceTo(TapestryPhase.VALIDATION);
        controller.advanceTo(TapestryPhase.REGISTRATION);
        controller.advanceTo(TapestryPhase.FREEZE);
        controller.advanceTo(TapestryPhase.TS_LOAD);
        controller.advanceTo(TapestryPhase.TS_REGISTER);
        
        brewingRecipe = new BrewingRecipe();
    }
    
    @AfterEach
    void tearDown() {
        PhaseController.reset();
    }
    
    @Test
    @DisplayName("Should register valid brewing recipe")
    void testRegisterValidRecipe() {
        BrewingRecipeConfig config = new BrewingRecipeConfig(
            "minecraft:awkward",
            "minecraft:sugar",
            "minecraft:swiftness"
        );
        
        assertDoesNotThrow(() -> brewingRecipe.register(config));
        assertEquals(1, brewingRecipe.getAllRecipes().size());
        
        BrewingRecipeDefinition definition = brewingRecipe.getAllRecipes().get(0);
        assertEquals("minecraft:awkward", definition.getInput());
        assertEquals("minecraft:sugar", definition.getIngredient());
        assertEquals("minecraft:swiftness", definition.getOutput());
    }
    
    @Test
    @DisplayName("Should register multiple brewing recipes")
    void testRegisterMultipleRecipes() {
        BrewingRecipeConfig config1 = new BrewingRecipeConfig(
            "minecraft:awkward",
            "minecraft:sugar",
            "minecraft:swiftness"
        );
        
        BrewingRecipeConfig config2 = new BrewingRecipeConfig(
            "minecraft:water",
            "minecraft:nether_wart",
            "minecraft:awkward"
        );
        
        brewingRecipe.register(config1);
        brewingRecipe.register(config2);
        
        assertEquals(2, brewingRecipe.getAllRecipes().size());
    }
    
    @Test
    @DisplayName("Should reject null configuration")
    void testRejectNullConfig() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> brewingRecipe.register(null)
        );
        
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    @DisplayName("Should reject null input potion")
    void testRejectNullInput() {
        BrewingRecipeConfig config = new BrewingRecipeConfig(
            null,
            "minecraft:sugar",
            "minecraft:swiftness"
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> brewingRecipe.register(config)
        );
        
        assertTrue(exception.getMessage().contains("input potion"));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    @DisplayName("Should reject empty input potion")
    void testRejectEmptyInput() {
        BrewingRecipeConfig config = new BrewingRecipeConfig(
            "",
            "minecraft:sugar",
            "minecraft:swiftness"
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> brewingRecipe.register(config)
        );
        
        assertTrue(exception.getMessage().contains("input potion"));
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }
    
    @Test
    @DisplayName("Should reject invalid input potion format")
    void testRejectInvalidInputFormat() {
        BrewingRecipeConfig config = new BrewingRecipeConfig(
            "invalid_format",
            "minecraft:sugar",
            "minecraft:swiftness"
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> brewingRecipe.register(config)
        );
        
        assertTrue(exception.getMessage().contains("Invalid input potion format"));
        assertTrue(exception.getMessage().contains("namespace:path"));
    }
    
    @Test
    @DisplayName("Should reject null ingredient item")
    void testRejectNullIngredient() {
        BrewingRecipeConfig config = new BrewingRecipeConfig(
            "minecraft:awkward",
            null,
            "minecraft:swiftness"
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> brewingRecipe.register(config)
        );
        
        assertTrue(exception.getMessage().contains("ingredient item"));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    @DisplayName("Should reject invalid ingredient format")
    void testRejectInvalidIngredientFormat() {
        BrewingRecipeConfig config = new BrewingRecipeConfig(
            "minecraft:awkward",
            "no_namespace",
            "minecraft:swiftness"
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> brewingRecipe.register(config)
        );
        
        assertTrue(exception.getMessage().contains("Invalid ingredient item format"));
        assertTrue(exception.getMessage().contains("namespace:path"));
    }
    
    @Test
    @DisplayName("Should reject null output potion")
    void testRejectNullOutput() {
        BrewingRecipeConfig config = new BrewingRecipeConfig(
            "minecraft:awkward",
            "minecraft:sugar",
            null
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> brewingRecipe.register(config)
        );
        
        assertTrue(exception.getMessage().contains("output potion"));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    @DisplayName("Should reject invalid output format")
    void testRejectInvalidOutputFormat() {
        BrewingRecipeConfig config = new BrewingRecipeConfig(
            "minecraft:awkward",
            "minecraft:sugar",
            "INVALID:FORMAT:TOO:MANY:COLONS"
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> brewingRecipe.register(config)
        );
        
        assertTrue(exception.getMessage().contains("Invalid output potion format"));
        assertTrue(exception.getMessage().contains("namespace:path"));
    }
    
    @Test
    @DisplayName("Should accept identifiers with paths")
    void testAcceptIdentifiersWithPaths() {
        BrewingRecipeConfig config = new BrewingRecipeConfig(
            "minecraft:awkward/long",
            "mymod:custom_item/variant",
            "minecraft:swiftness/strong"
        );
        
        assertDoesNotThrow(() -> brewingRecipe.register(config));
    }
    
    @Test
    @DisplayName("Should throw error when registering outside TS_REGISTER phase")
    void testPhaseViolation() {
        // Advance to RUNTIME phase
        PhaseController controller = PhaseController.getInstance();
        controller.advanceTo(TapestryPhase.TS_ACTIVATE);
        controller.advanceTo(TapestryPhase.TS_READY);
        controller.advanceTo(TapestryPhase.PERSISTENCE_READY);
        controller.advanceTo(TapestryPhase.EVENT);
        controller.advanceTo(TapestryPhase.RUNTIME);
        
        BrewingRecipeConfig config = new BrewingRecipeConfig(
            "minecraft:awkward",
            "minecraft:sugar",
            "minecraft:swiftness"
        );
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> brewingRecipe.register(config)
        );
        
        assertTrue(exception.getMessage().contains("TS_REGISTER"));
        assertTrue(exception.getMessage().contains("RUNTIME"));
    }
    
    @Test
    @DisplayName("Should return unmodifiable list of recipes")
    void testUnmodifiableRecipeList() {
        BrewingRecipeConfig config = new BrewingRecipeConfig(
            "minecraft:awkward",
            "minecraft:sugar",
            "minecraft:swiftness"
        );
        
        brewingRecipe.register(config);
        
        assertThrows(
            UnsupportedOperationException.class,
            () -> brewingRecipe.getAllRecipes().clear()
        );
    }
}
