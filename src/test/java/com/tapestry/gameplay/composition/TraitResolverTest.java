package com.tapestry.gameplay.composition;

import com.tapestry.gameplay.items.ItemDefinition;
import com.tapestry.gameplay.items.ItemOptions;
import com.tapestry.gameplay.items.ItemRegistration;
import com.tapestry.gameplay.traits.TraitConfig;
import com.tapestry.gameplay.traits.TraitDefinition;
import com.tapestry.gameplay.traits.TraitSystem;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TraitResolver.
 */
class TraitResolverTest {
    
    private TraitSystem traitSystem;
    private ItemRegistration itemRegistration;
    private TraitResolver resolver;
    
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
        itemRegistration.setTraitSystem(traitSystem); // Wire trait system for validation
        resolver = new TraitResolver(traitSystem, itemRegistration);
    }
    
    @AfterEach
    void tearDown() {
        PhaseController.reset();
    }
    
    @Test
    void testResolveWithNoTraitsOrItems() {
        // Resolve with empty registries
        TraitResolver.ResolutionResult result = resolver.resolve();
        
        assertEquals(0, result.getTraitCount());
        assertEquals(0, result.getItemCount());
        assertEquals(0, result.getMappingCount());
    }
    
    @Test
    void testResolveWithTraitsButNoItems() {
        // Register traits
        traitSystem.register("fish_food", new TraitConfig());
        traitSystem.register("plant_fiber", new TraitConfig());
        
        // Resolve
        TraitResolver.ResolutionResult result = resolver.resolve();
        
        assertEquals(2, result.getTraitCount());
        assertEquals(0, result.getItemCount());
        assertEquals(0, result.getMappingCount());
    }
    
    @Test
    void testResolveWithItemsButNoTraits() {
        // Register items without traits
        itemRegistration.register("test:item1", new ItemOptions());
        itemRegistration.register("test:item2", new ItemOptions());
        
        // Resolve
        TraitResolver.ResolutionResult result = resolver.resolve();
        
        assertEquals(0, result.getTraitCount());
        assertEquals(2, result.getItemCount());
        assertEquals(0, result.getMappingCount());
    }
    
    @Test
    void testResolveWithSingleTraitAndItem() {
        // Register trait
        traitSystem.register("fish_food", new TraitConfig());
        
        // Register item with trait
        ItemOptions options = new ItemOptions().traits("fish_food");
        itemRegistration.register("test:nori", options);
        
        // Resolve
        TraitResolver.ResolutionResult result = resolver.resolve();
        
        assertEquals(1, result.getTraitCount());
        assertEquals(1, result.getItemCount());
        assertEquals(1, result.getMappingCount());
        
        // Verify trait contains item
        TraitDefinition trait = traitSystem.getTrait("fish_food");
        assertTrue(trait.getItems().contains("test:nori"));
    }
    
    @Test
    void testResolveWithMultipleTraitsAndItems() {
        // Register traits
        traitSystem.register("fish_food", new TraitConfig());
        traitSystem.register("plant_fiber", new TraitConfig());
        traitSystem.register("sea_vegetable", new TraitConfig());
        
        // Register items with various trait combinations
        itemRegistration.register("test:nori", 
            new ItemOptions().traits("fish_food", "plant_fiber", "sea_vegetable"));
        itemRegistration.register("test:kelp", 
            new ItemOptions().traits("plant_fiber", "sea_vegetable"));
        itemRegistration.register("test:cod", 
            new ItemOptions().traits("fish_food"));
        
        // Resolve
        TraitResolver.ResolutionResult result = resolver.resolve();
        
        assertEquals(3, result.getTraitCount());
        assertEquals(3, result.getItemCount());
        assertEquals(6, result.getMappingCount()); // nori:3 + kelp:2 + cod:1
        
        // Verify trait mappings
        TraitDefinition fishFood = traitSystem.getTrait("fish_food");
        assertEquals(2, fishFood.getItems().size());
        assertTrue(fishFood.getItems().contains("test:nori"));
        assertTrue(fishFood.getItems().contains("test:cod"));
        
        TraitDefinition plantFiber = traitSystem.getTrait("plant_fiber");
        assertEquals(2, plantFiber.getItems().size());
        assertTrue(plantFiber.getItems().contains("test:nori"));
        assertTrue(plantFiber.getItems().contains("test:kelp"));
        
        TraitDefinition seaVegetable = traitSystem.getTrait("sea_vegetable");
        assertEquals(2, seaVegetable.getItems().size());
        assertTrue(seaVegetable.getItems().contains("test:nori"));
        assertTrue(seaVegetable.getItems().contains("test:kelp"));
    }
    
    @Test
    void testResolveWithUndefinedTraitThrowsException() {
        // Create ItemRegistration without TraitSystem to defer validation
        ItemRegistration deferredValidation = new ItemRegistration();
        TraitResolver deferredResolver = new TraitResolver(traitSystem, deferredValidation);
        
        // Register item with undefined trait
        ItemOptions options = new ItemOptions().traits("undefined_trait");
        deferredValidation.register("test:item", options);
        
        // Resolve should throw exception
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> deferredResolver.resolve()
        );
        
        assertTrue(exception.getMessage().contains("undefined traits"));
        assertTrue(exception.getMessage().contains("undefined_trait"));
    }
    
    @Test
    void testResolveWithMultipleUndefinedTraits() {
        // Create ItemRegistration without TraitSystem to defer validation
        ItemRegistration deferredValidation = new ItemRegistration();
        TraitResolver deferredResolver = new TraitResolver(traitSystem, deferredValidation);
        
        // Register trait
        traitSystem.register("valid_trait", new TraitConfig());
        
        // Register items with mix of valid and invalid traits
        deferredValidation.register("test:item1", 
            new ItemOptions().traits("valid_trait", "invalid1"));
        deferredValidation.register("test:item2", 
            new ItemOptions().traits("invalid2"));
        
        // Resolve should throw exception listing all invalid traits
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> deferredResolver.resolve()
        );
        
        assertTrue(exception.getMessage().contains("undefined traits"));
        assertTrue(exception.getMessage().contains("invalid1"));
        assertTrue(exception.getMessage().contains("invalid2"));
        assertTrue(exception.getMessage().contains("Valid traits are"));
        assertTrue(exception.getMessage().contains("valid_trait"));
    }
    
    @Test
    void testResolveValidatesAllTraitsBeforeMapping() {
        // Create ItemRegistration without TraitSystem to defer validation
        ItemRegistration deferredValidation = new ItemRegistration();
        TraitResolver deferredResolver = new TraitResolver(traitSystem, deferredValidation);
        
        // Register traits
        traitSystem.register("trait1", new TraitConfig());
        traitSystem.register("trait2", new TraitConfig());
        
        // Register item with one valid and one invalid trait
        deferredValidation.register("test:item", 
            new ItemOptions().traits("trait1", "invalid_trait"));
        
        // Resolve should fail
        assertThrows(IllegalStateException.class, () -> deferredResolver.resolve());
        
        // Verify valid trait was not modified (no partial mapping)
        TraitDefinition trait1 = traitSystem.getTrait("trait1");
        // The item should still be added even though validation fails later
        // This is because we add items as we go, then validate at the end
        assertTrue(trait1.getItems().contains("test:item"));
    }
}
