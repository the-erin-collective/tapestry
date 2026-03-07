package com.tapestry.gameplay.composition;

import com.tapestry.gameplay.items.ItemOptions;
import com.tapestry.gameplay.items.ItemRegistration;
import com.tapestry.gameplay.traits.TraitConfig;
import com.tapestry.gameplay.traits.TraitSystem;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CompositionOrchestrator.
 */
class CompositionOrchestratorTest {
    
    private TraitSystem traitSystem;
    private ItemRegistration itemRegistration;
    private CompositionOrchestrator orchestrator;
    
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
        orchestrator = new CompositionOrchestrator(traitSystem, itemRegistration);
    }
    
    @AfterEach
    void tearDown() {
        PhaseController.reset();
    }
    
    @Test
    void testExecuteCompositionWithNoData() throws IOException {
        // Execute composition with no traits or items
        assertDoesNotThrow(() -> orchestrator.executeComposition());
        
        // Verify trait system is frozen
        assertTrue(traitSystem.isFrozen());
    }
    
    @Test
    void testExecuteCompositionWithTraitsAndItems() throws IOException {
        // Register traits
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traitSystem.register("plant_fiber", new TraitConfig("tapestry:plant_fibers"));
        
        // Register items
        itemRegistration.register("test:nori", 
            new ItemOptions().traits("fish_food", "plant_fiber"));
        itemRegistration.register("test:cod", 
            new ItemOptions().traits("fish_food"));
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify trait system is frozen
        assertTrue(traitSystem.isFrozen());
        
        // Verify trait mappings were resolved
        assertEquals(2, traitSystem.getTrait("fish_food").getItems().size());
        assertEquals(1, traitSystem.getTrait("plant_fiber").getItems().size());
    }
    
    @Test
    void testExecuteCompositionFreezesRegistries() throws IOException {
        // Register trait and item
        traitSystem.register("test_trait", new TraitConfig());
        itemRegistration.register("test:item", new ItemOptions().traits("test_trait"));
        
        // Verify not frozen before composition
        assertFalse(traitSystem.isFrozen());
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify frozen after composition
        assertTrue(traitSystem.isFrozen());
        
        // Verify cannot register new traits after composition
        assertThrows(IllegalStateException.class, 
            () -> traitSystem.register("new_trait", new TraitConfig()));
    }
    
    @Test
    void testExecuteCompositionWithUndefinedTraitFails() {
        // Create ItemRegistration without TraitSystem to defer validation
        ItemRegistration deferredValidation = new ItemRegistration();
        CompositionOrchestrator deferredOrchestrator = new CompositionOrchestrator(traitSystem, deferredValidation);
        
        // Register item with undefined trait
        deferredValidation.register("test:item", new ItemOptions().traits("undefined_trait"));
        
        // Execute composition should fail
        assertThrows(IllegalStateException.class, 
            () -> deferredOrchestrator.executeComposition());
    }
    
    @Test
    void testGetTraitResolver() {
        assertNotNull(orchestrator.getTraitResolver());
    }
    
    @Test
    void testGetTagGenerator() {
        assertNotNull(orchestrator.getTagGenerator());
    }
}
