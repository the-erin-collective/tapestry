package com.tapestry.gameplay;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tapestry.gameplay.items.ItemOptions;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GameplayAPI.
 */
class GameplayAPITest {
    
    @TempDir
    Path tempDir;
    
    private GameplayAPI api;
    
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
        
        api = new GameplayAPI();
    }
    
    @AfterEach
    void tearDown() {
        PhaseController.reset();
    }
    
    @Test
    void testGameplayAPIInitialization() {
        assertNotNull(api.getTraits());
        assertNotNull(api.getItems());
        assertNotNull(api.getBrewing());
        assertNotNull(api.getLoot());
        assertNotNull(api.getCompositionOrchestrator());
    }
    
    @Test
    void testRegisterBuiltInTraits() {
        // Register built-in traits
        api.registerBuiltInTraits();
        
        // Verify all built-in traits are registered
        assertNotNull(api.getTraits().getTrait("fish_food"));
        assertNotNull(api.getTraits().getTrait("milk_like"));
        assertNotNull(api.getTraits().getTrait("egg_like"));
        assertNotNull(api.getTraits().getTrait("honey_like"));
        assertNotNull(api.getTraits().getTrait("plant_fiber"));
        
        // Verify tag mappings
        assertEquals("tapestry:fish_items", api.getTraits().getTrait("fish_food").getTag());
        assertEquals("tapestry:milk_items", api.getTraits().getTrait("milk_like").getTag());
        assertEquals("tapestry:egg_items", api.getTraits().getTrait("egg_like").getTag());
        assertEquals("tapestry:honey_items", api.getTraits().getTrait("honey_like").getTag());
        assertEquals("tapestry:plant_fibers", api.getTraits().getTrait("plant_fiber").getTag());
    }
    
    @Test
    void testExecuteCompositionIntegration() throws IOException {
        // Register built-in traits
        api.registerBuiltInTraits();
        
        // Register some items with traits
        api.getItems().register("test:nori", 
            new ItemOptions().traits("fish_food", "plant_fiber"));
        api.getItems().register("test:cod", 
            new ItemOptions().traits("fish_food"));
        
        // Execute composition
        assertDoesNotThrow(() -> api.executeComposition());
        
        // Verify trait system is frozen
        assertTrue(api.getTraits().isFrozen());
        
        // Verify trait mappings
        assertEquals(2, api.getTraits().getTrait("fish_food").getItems().size());
        assertEquals(1, api.getTraits().getTrait("plant_fiber").getItems().size());
    }
}
