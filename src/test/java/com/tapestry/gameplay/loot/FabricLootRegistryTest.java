package com.tapestry.gameplay.loot;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FabricLootRegistry initialization and integration.
 */
class FabricLootRegistryTest {
    
    private LootModifier lootModifier;
    private FabricLootRegistry fabricRegistry;
    
    @BeforeEach
    void setUp() {
        // Advance through phases to TS_REGISTER
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.VALIDATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_REGISTER);
        
        lootModifier = new LootModifier();
        fabricRegistry = new FabricLootRegistry(lootModifier);
    }
    
    @AfterEach
    void tearDown() {
        PhaseController.reset();
    }
    
    @Test
    void testConstructorWithValidLootModifier() {
        assertNotNull(fabricRegistry);
    }
    
    @Test
    void testInitializeOnce() {
        // First initialization should succeed
        assertDoesNotThrow(() -> fabricRegistry.initialize());
    }
    
    @Test
    void testInitializeTwice() {
        // First initialization
        fabricRegistry.initialize();
        
        // Second initialization should log warning but not fail
        assertDoesNotThrow(() -> fabricRegistry.initialize());
    }
    
    @Test
    void testInitializeWithNoModifications() {
        // Should initialize successfully even with no modifications
        assertDoesNotThrow(() -> fabricRegistry.initialize());
    }
    
    @Test
    void testInitializeWithModifications() {
        // Register some modifications
        lootModifier.modify("minecraft:chests/simple_dungeon", table -> {
            table.replace("minecraft:bread", "minecraft:nori");
        });
        
        lootModifier.modify("minecraft:fishing/fish", table -> {
            table.replace("minecraft:cod", "minecraft:nori");
        });
        
        // Should initialize successfully with modifications
        assertDoesNotThrow(() -> fabricRegistry.initialize());
    }
}
