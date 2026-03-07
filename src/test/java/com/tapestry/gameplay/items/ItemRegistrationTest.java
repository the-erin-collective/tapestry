package com.tapestry.gameplay.items;

import com.tapestry.gameplay.traits.TraitConfig;
import com.tapestry.gameplay.traits.TraitSystem;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ItemRegistration.register() function.
 * 
 * Validates:
 * - Item registration during TS_REGISTER phase
 * - Item property validation (stackSize, durability, food, traits)
 * - Phase enforcement
 * - Trait reference validation
 * - Descriptive error messages
 */
class ItemRegistrationTest {
    
    private ItemRegistration itemRegistration;
    private TraitSystem traitSystem;
    
    @BeforeEach
    void setUp() {
        // Reset phase controller and advance to TS_REGISTER
        PhaseController.reset();
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.VALIDATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_REGISTER);
        
        traitSystem = new TraitSystem();
        itemRegistration = new ItemRegistration();
        itemRegistration.setTraitSystem(traitSystem);
    }
    
    @AfterEach
    void tearDown() {
        PhaseController.reset();
    }
    
    @Test
    void testRegisterItem_Success() {
        ItemOptions options = new ItemOptions()
            .stackSize(32)
            .durability(100);
        
        assertDoesNotThrow(() -> itemRegistration.register("test:item", options));
        
        ItemDefinition item = itemRegistration.getItem("test:item");
        assertNotNull(item);
        assertEquals("test:item", item.getId());
        assertEquals(32, item.getStackSize());
        assertEquals(100, item.getDurability());
    }
    
    @Test
    void testRegisterItem_WithTraits_Success() {
        // Register traits first
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traitSystem.register("plant_fiber", new TraitConfig("tapestry:plant_fibers"));
        
        ItemOptions options = new ItemOptions()
            .traits("fish_food", "plant_fiber");
        
        assertDoesNotThrow(() -> itemRegistration.register("test:nori", options));
        
        ItemDefinition item = itemRegistration.getItem("test:nori");
        assertNotNull(item);
        assertArrayEquals(new String[]{"fish_food", "plant_fiber"}, item.getTraits());
    }
    
    @Test
    void testRegisterItem_NullOptions() {
        assertDoesNotThrow(() -> itemRegistration.register("test:item", null));
        
        ItemDefinition item = itemRegistration.getItem("test:item");
        assertNotNull(item);
        assertEquals(64, item.getStackSize()); // Default stack size
    }
    
    @Test
    void testRegisterItem_InvalidStackSize() {
        ItemOptions options = new ItemOptions().stackSize(100);
        
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> itemRegistration.register("test:item", options)
        );
        assertTrue(ex.getMessage().contains("Stack size"));
        assertTrue(ex.getMessage().contains("between 1 and 64"));
    }
    
    @Test
    void testRegisterItem_InvalidDurability() {
        ItemOptions options = new ItemOptions().durability(-10);
        
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> itemRegistration.register("test:item", options)
        );
        assertTrue(ex.getMessage().contains("Durability"));
        assertTrue(ex.getMessage().contains(">= 0"));
    }
    
    @Test
    void testRegisterItem_InvalidFood() {
        FoodComponent food = new FoodComponent(25, 0.5f); // Hunger too high
        ItemOptions options = new ItemOptions().food(food);
        
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> itemRegistration.register("test:item", options)
        );
        assertTrue(ex.getMessage().contains("hunger"));
        assertTrue(ex.getMessage().contains("between 0 and 20"));
    }
    
    @Test
    void testRegisterItem_InvalidItemId() {
        ItemOptions options = new ItemOptions();
        
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> itemRegistration.register("invalid_id_no_colon", options)
        );
        assertTrue(ex.getMessage().contains("namespace:path format"));
    }
    
    @Test
    void testRegisterItem_DuplicateId() {
        ItemOptions options = new ItemOptions();
        
        itemRegistration.register("test:item", options);
        
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> itemRegistration.register("test:item", options)
        );
        assertTrue(ex.getMessage().contains("already registered"));
        assertTrue(ex.getMessage().contains("test:item"));
    }
    
    @Test
    void testRegisterItem_UndefinedTrait() {
        // Register one trait but reference a different one
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        ItemOptions options = new ItemOptions()
            .traits("undefined_trait");
        
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> itemRegistration.register("test:item", options)
        );
        assertTrue(ex.getMessage().contains("Undefined trait"));
        assertTrue(ex.getMessage().contains("undefined_trait"));
        assertTrue(ex.getMessage().contains("Valid traits"));
    }
    
    @Test
    void testRegisterItem_MultipleTraits_OneUndefined() {
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        ItemOptions options = new ItemOptions()
            .traits("fish_food", "undefined_trait");
        
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> itemRegistration.register("test:item", options)
        );
        assertTrue(ex.getMessage().contains("Undefined trait"));
        assertTrue(ex.getMessage().contains("undefined_trait"));
    }
    
    @Test
    void testRegisterItem_OutsideRegisterPhase() {
        // Advance to TS_ACTIVATE phase (after TS_REGISTER)
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_ACTIVATE);
        
        ItemOptions options = new ItemOptions();
        
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> itemRegistration.register("test:item", options)
        );
        assertTrue(ex.getMessage().contains("TS_REGISTER"));
        assertTrue(ex.getMessage().contains("TS_ACTIVATE"));
    }
    
    @Test
    void testRegisterItem_WithoutTraitSystem() {
        // Create a new ItemRegistration without setting TraitSystem
        ItemRegistration newRegistration = new ItemRegistration();
        
        ItemOptions options = new ItemOptions()
            .traits("some_trait");
        
        // Should not throw - validation will happen during COMPOSITION phase
        assertDoesNotThrow(() -> newRegistration.register("test:item", options));
    }
    
    @Test
    void testRegisterItem_ComplexItem() {
        // Register traits
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traitSystem.register("plant_fiber", new TraitConfig("tapestry:plant_fibers"));
        
        // Create complex item with multiple properties
        FoodComponent food = new FoodComponent(4, 0.6f);
        ItemOptions options = new ItemOptions()
            .stackSize(16)
            .traits("fish_food", "plant_fiber")
            .food(food)
            .recipeRemainder("minecraft:bowl");
        
        assertDoesNotThrow(() -> itemRegistration.register("test:complex_item", options));
        
        ItemDefinition item = itemRegistration.getItem("test:complex_item");
        assertNotNull(item);
        assertEquals(16, item.getStackSize());
        assertArrayEquals(new String[]{"fish_food", "plant_fiber"}, item.getTraits());
        assertEquals(4, item.getFood().getHunger());
        assertEquals(0.6f, item.getFood().getSaturation(), 0.001f);
        assertEquals("minecraft:bowl", item.getRecipeRemainder());
    }
    
    @Test
    void testGetAllItems() {
        itemRegistration.register("test:item1", new ItemOptions());
        itemRegistration.register("test:item2", new ItemOptions());
        itemRegistration.register("test:item3", new ItemOptions());
        
        assertEquals(3, itemRegistration.getAllItems().size());
        assertTrue(itemRegistration.getAllItems().containsKey("test:item1"));
        assertTrue(itemRegistration.getAllItems().containsKey("test:item2"));
        assertTrue(itemRegistration.getAllItems().containsKey("test:item3"));
    }
    
    // Note: Fabric registration tests are skipped because Fabric registry
    // is not available in unit test environment. The performFabricRegistration()
    // method will be tested in integration tests with a full Minecraft environment.
}
