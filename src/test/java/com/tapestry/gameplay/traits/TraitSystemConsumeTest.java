package com.tapestry.gameplay.traits;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TraitSystem.consume() function.
 * 
 * Tests Requirements: 2.1, 2.2
 */
class TraitSystemConsumeTest {
    
    private TraitSystem traitSystem;
    private PhaseController phaseController;
    
    @BeforeEach
    void setUp() {
        // Reset phase controller for each test
        PhaseController.reset();
        phaseController = PhaseController.getInstance();
        
        // Advance to TS_REGISTER phase
        phaseController.advanceTo(TapestryPhase.DISCOVERY);
        phaseController.advanceTo(TapestryPhase.VALIDATION);
        phaseController.advanceTo(TapestryPhase.REGISTRATION);
        phaseController.advanceTo(TapestryPhase.FREEZE);
        phaseController.advanceTo(TapestryPhase.TS_LOAD);
        phaseController.advanceTo(TapestryPhase.TS_REGISTER);
        
        traitSystem = new TraitSystem();
    }
    
    @AfterEach
    void tearDown() {
        PhaseController.reset();
    }
    
    // ===== Successful Consumption Tests =====
    
    @Test
    void testConsume_BasicConsumption() {
        // Requirement 2.1: Accept trait name and ConsumptionConfig
        ConsumptionConfig config = new ConsumptionConfig("minecraft:cat", "food");
        
        assertDoesNotThrow(() -> traitSystem.consume("fish_food", config));
        
        // Requirement 2.2: Store consumption relationship
        assertEquals(1, traitSystem.getConsumptions().size());
        Consumption consumption = traitSystem.getConsumptions().get(0);
        assertEquals("fish_food", consumption.getTraitName());
        assertEquals("minecraft:cat", consumption.getEntity());
        assertEquals("food", consumption.getBehavior());
    }
    
    @Test
    void testConsume_MultipleConsumptions() {
        // Requirement 2.2: Store multiple consumption relationships
        traitSystem.consume("fish_food", new ConsumptionConfig("minecraft:cat", "food"));
        traitSystem.consume("fish_food", new ConsumptionConfig("minecraft:dolphin", "food"));
        traitSystem.consume("milk_like", new ConsumptionConfig("minecraft:cow", "breeding"));
        
        assertEquals(3, traitSystem.getConsumptions().size());
    }
    
    @Test
    void testConsume_SameTraitMultipleEntities() {
        // Multiple entities can consume the same trait
        traitSystem.consume("fish_food", new ConsumptionConfig("minecraft:cat", "food"));
        traitSystem.consume("fish_food", new ConsumptionConfig("minecraft:dolphin", "food"));
        traitSystem.consume("fish_food", new ConsumptionConfig("minecraft:axolotl", "food"));
        
        assertEquals(3, traitSystem.getConsumptions().size());
        
        // All should reference the same trait
        assertTrue(traitSystem.getConsumptions().stream()
            .allMatch(c -> c.getTraitName().equals("fish_food")));
    }
    
    @Test
    void testConsume_SameEntityMultipleTraits() {
        // Same entity can consume multiple traits for different behaviors
        traitSystem.consume("fish_food", new ConsumptionConfig("minecraft:cat", "food"));
        traitSystem.consume("milk_like", new ConsumptionConfig("minecraft:cat", "healing"));
        
        assertEquals(2, traitSystem.getConsumptions().size());
        
        // Both should reference the same entity
        assertTrue(traitSystem.getConsumptions().stream()
            .allMatch(c -> c.getEntity().equals("minecraft:cat")));
    }
    
    @Test
    void testConsume_ValidEntityIdentifiers() {
        // Test various valid entity identifier formats
        assertDoesNotThrow(() -> traitSystem.consume("trait1", 
            new ConsumptionConfig("minecraft:cat", "food")));
        assertDoesNotThrow(() -> traitSystem.consume("trait2", 
            new ConsumptionConfig("mymod:custom_entity", "food")));
        assertDoesNotThrow(() -> traitSystem.consume("trait3", 
            new ConsumptionConfig("mod-name:entity-name", "food")));
    }
    
    @Test
    void testConsume_ValidBehaviorTypes() {
        // Test various valid behavior types
        assertDoesNotThrow(() -> traitSystem.consume("trait1", 
            new ConsumptionConfig("minecraft:cat", "food")));
        assertDoesNotThrow(() -> traitSystem.consume("trait2", 
            new ConsumptionConfig("minecraft:cow", "breeding")));
        assertDoesNotThrow(() -> traitSystem.consume("trait3", 
            new ConsumptionConfig("minecraft:villager", "trading")));
        assertDoesNotThrow(() -> traitSystem.consume("trait4", 
            new ConsumptionConfig("minecraft:wolf", "healing")));
    }
    
    // ===== Phase Violation Tests =====
    
    @Test
    void testConsume_OutsideRegisterPhase_BeforePhase() {
        // Requirement 2.1: Throw fatal error if called outside TS_REGISTER phase
        PhaseController.reset();
        PhaseController controller = PhaseController.getInstance();
        controller.advanceTo(TapestryPhase.DISCOVERY);
        
        TraitSystem system = new TraitSystem();
        ConsumptionConfig config = new ConsumptionConfig("minecraft:cat", "food");
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> system.consume("fish_food", config)
        );
        
        assertTrue(exception.getMessage().contains("TS_REGISTER"));
        assertTrue(exception.getMessage().contains("DISCOVERY"));
    }
    
    @Test
    void testConsume_OutsideRegisterPhase_AfterPhase() {
        // Requirement 2.1: Throw fatal error if called after TS_REGISTER phase
        phaseController.advanceTo(TapestryPhase.TS_ACTIVATE);
        
        ConsumptionConfig config = new ConsumptionConfig("minecraft:cat", "food");
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> traitSystem.consume("fish_food", config)
        );
        
        assertTrue(exception.getMessage().contains("TS_REGISTER"));
        assertTrue(exception.getMessage().contains("TS_ACTIVATE"));
    }
    
    @Test
    void testConsume_InRuntimePhase() {
        // Cannot consume during RUNTIME phase
        phaseController.advanceTo(TapestryPhase.TS_ACTIVATE);
        phaseController.advanceTo(TapestryPhase.TS_READY);
        phaseController.advanceTo(TapestryPhase.PERSISTENCE_READY);
        phaseController.advanceTo(TapestryPhase.EVENT);
        phaseController.advanceTo(TapestryPhase.RUNTIME);
        
        ConsumptionConfig config = new ConsumptionConfig("minecraft:cat", "food");
        
        assertThrows(
            IllegalStateException.class,
            () -> traitSystem.consume("fish_food", config)
        );
    }
    
    // ===== Frozen Registry Tests =====
    
    @Test
    void testConsume_AfterFreeze() {
        // Requirement 2.5: Cannot consume after COMPOSITION phase (frozen)
        traitSystem.consume("fish_food", new ConsumptionConfig("minecraft:cat", "food"));
        traitSystem.freeze();
        
        ConsumptionConfig config = new ConsumptionConfig("minecraft:dolphin", "food");
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> traitSystem.consume("fish_food", config)
        );
        
        assertTrue(exception.getMessage().contains("Cannot consume trait"));
        assertTrue(exception.getMessage().contains("after COMPOSITION phase"));
        assertTrue(exception.getMessage().contains("frozen"));
    }
    
    // ===== Invalid Configuration Tests =====
    
    @Test
    void testConsume_NullEntity() {
        // ConsumptionConfig should validate entity is not null
        assertThrows(
            IllegalArgumentException.class,
            () -> new ConsumptionConfig(null, "food")
        );
    }
    
    @Test
    void testConsume_EmptyEntity() {
        // ConsumptionConfig should validate entity is not empty
        assertThrows(
            IllegalArgumentException.class,
            () -> new ConsumptionConfig("", "food")
        );
    }
    
    @Test
    void testConsume_NullBehavior() {
        // ConsumptionConfig should validate behavior is not null
        assertThrows(
            IllegalArgumentException.class,
            () -> new ConsumptionConfig("minecraft:cat", null)
        );
    }
    
    @Test
    void testConsume_EmptyBehavior() {
        // ConsumptionConfig should validate behavior is not empty
        assertThrows(
            IllegalArgumentException.class,
            () -> new ConsumptionConfig("minecraft:cat", "")
        );
    }
    
    // ===== Integration Tests =====
    
    @Test
    void testGetConsumptions_ReturnsUnmodifiableList() {
        // Verify that getConsumptions returns an immutable view
        traitSystem.consume("fish_food", new ConsumptionConfig("minecraft:cat", "food"));
        
        assertThrows(UnsupportedOperationException.class, () -> {
            traitSystem.getConsumptions().add(
                new Consumption("new_trait", "minecraft:pig", "breeding")
            );
        });
    }
    
    @Test
    void testGetConsumptions_EmptyInitially() {
        // Verify initial state
        assertEquals(0, traitSystem.getConsumptions().size());
    }
    
    @Test
    void testConsume_WithoutTraitRegistration() {
        // Consumption can be declared even if trait is not registered yet
        // (validation happens during COMPOSITION phase)
        assertDoesNotThrow(() -> 
            traitSystem.consume("unregistered_trait", 
                new ConsumptionConfig("minecraft:cat", "food"))
        );
        
        assertEquals(1, traitSystem.getConsumptions().size());
    }
    
    @Test
    void testConsume_OrderPreserved() {
        // Verify consumption order is preserved
        traitSystem.consume("trait1", new ConsumptionConfig("entity1", "behavior1"));
        traitSystem.consume("trait2", new ConsumptionConfig("entity2", "behavior2"));
        traitSystem.consume("trait3", new ConsumptionConfig("entity3", "behavior3"));
        
        var consumptions = traitSystem.getConsumptions();
        assertEquals("trait1", consumptions.get(0).getTraitName());
        assertEquals("trait2", consumptions.get(1).getTraitName());
        assertEquals("trait3", consumptions.get(2).getTraitName());
    }
}
