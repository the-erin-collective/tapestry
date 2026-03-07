package com.tapestry.gameplay.traits;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TraitSystem.register() function.
 * 
 * Tests Requirements: 1.1, 1.2, 1.3, 1.5, 9.1, 9.2
 */
class TraitSystemTest {
    
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
    
    // ===== Successful Registration Tests =====
    
    @Test
    void testRegisterTrait_WithCustomTag() {
        // Requirement 1.1, 1.2: Register trait with custom tag during TS_REGISTER phase
        TraitConfig config = new TraitConfig("minecraft:cat_food");
        
        traitSystem.register("fish_food", config);
        
        TraitDefinition trait = traitSystem.getTrait("fish_food");
        assertNotNull(trait);
        assertEquals("fish_food", trait.getName());
        assertEquals("minecraft:cat_food", trait.getTag());
    }
    
    @Test
    void testRegisterTrait_WithDefaultTag() {
        // Requirement 9.2: Generate default tag name using pattern "tapestry:{trait_name}_items"
        traitSystem.register("fish_food", null);
        
        TraitDefinition trait = traitSystem.getTrait("fish_food");
        assertNotNull(trait);
        assertEquals("fish_food", trait.getName());
        assertEquals("tapestry:fish_food_items", trait.getTag());
    }
    
    @Test
    void testRegisterTrait_WithEmptyConfig() {
        // Requirement 9.2: Generate default tag when config has null tag
        TraitConfig config = new TraitConfig();
        
        traitSystem.register("milk_like", config);
        
        TraitDefinition trait = traitSystem.getTrait("milk_like");
        assertNotNull(trait);
        assertEquals("milk_like", trait.getName());
        assertEquals("tapestry:milk_like_items", trait.getTag());
    }
    
    @Test
    void testRegisterMultipleTraits() {
        // Requirement 1.1: Register multiple different traits
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traitSystem.register("milk_like", new TraitConfig("tapestry:milk_items"));
        traitSystem.register("egg_like", new TraitConfig("tapestry:egg_items"));
        
        assertEquals(3, traitSystem.getAllTraits().size());
        assertNotNull(traitSystem.getTrait("fish_food"));
        assertNotNull(traitSystem.getTrait("milk_like"));
        assertNotNull(traitSystem.getTrait("egg_like"));
    }
    
    @Test
    void testRegisterTrait_ValidTagFormats() {
        // Requirement 9.3: Validate tag names follow Minecraft namespace:path format
        assertDoesNotThrow(() -> traitSystem.register("test1", new TraitConfig("tapestry:fish_items")));
        assertDoesNotThrow(() -> traitSystem.register("test2", new TraitConfig("minecraft:cat_food")));
        assertDoesNotThrow(() -> traitSystem.register("test3", new TraitConfig("mymod:custom_tag")));
        assertDoesNotThrow(() -> traitSystem.register("test4", new TraitConfig("mod-name:tag-name")));
        assertDoesNotThrow(() -> traitSystem.register("test5", new TraitConfig("mod.name:tag.name")));
        assertDoesNotThrow(() -> traitSystem.register("test6", new TraitConfig("tapestry:items/fish")));
    }
    
    // ===== Duplicate Trait Name Tests =====
    
    @Test
    void testRegisterTrait_DuplicateName() {
        // Requirement 1.5: Throw fatal error if trait name already registered
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("tapestry:other_tag"))
        );
        
        assertTrue(exception.getMessage().contains("already registered"));
        assertTrue(exception.getMessage().contains("fish_food"));
        assertTrue(exception.getMessage().contains("Duplicate trait names are not allowed"));
    }
    
    @Test
    void testRegisterTrait_DuplicateNameDifferentTag() {
        // Requirement 1.5: Duplicate names not allowed even with different tags
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("minecraft:cat_food"))
        );
    }
    
    // ===== Phase Violation Tests =====
    
    @Test
    void testRegisterTrait_OutsideRegisterPhase_BeforePhase() {
        // Requirement 1.3: Throw fatal error if called outside TS_REGISTER phase
        PhaseController.reset();
        PhaseController controller = PhaseController.getInstance();
        controller.advanceTo(TapestryPhase.DISCOVERY);
        
        TraitSystem system = new TraitSystem();
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> system.register("fish_food", null)
        );
        
        assertTrue(exception.getMessage().contains("TS_REGISTER"));
        assertTrue(exception.getMessage().contains("DISCOVERY"));
    }
    
    @Test
    void testRegisterTrait_OutsideRegisterPhase_AfterPhase() {
        // Requirement 1.3: Throw fatal error if called after TS_REGISTER phase
        phaseController.advanceTo(TapestryPhase.TS_ACTIVATE);
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> traitSystem.register("fish_food", null)
        );
        
        assertTrue(exception.getMessage().contains("TS_REGISTER"));
        assertTrue(exception.getMessage().contains("TS_ACTIVATE"));
    }
    
    @Test
    void testRegisterTrait_InRuntimePhase() {
        // Requirement 1.3: Cannot register during RUNTIME phase
        phaseController.advanceTo(TapestryPhase.TS_ACTIVATE);
        phaseController.advanceTo(TapestryPhase.TS_READY);
        phaseController.advanceTo(TapestryPhase.PERSISTENCE_READY);
        phaseController.advanceTo(TapestryPhase.EVENT);
        phaseController.advanceTo(TapestryPhase.RUNTIME);
        
        assertThrows(
            IllegalStateException.class,
            () -> traitSystem.register("fish_food", null)
        );
    }
    
    // ===== Frozen Registry Tests =====
    
    @Test
    void testRegisterTrait_AfterFreeze() {
        // Requirement 2.4, 2.5: Cannot register after COMPOSITION phase (frozen)
        traitSystem.register("fish_food", null);
        traitSystem.freeze();
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> traitSystem.register("milk_like", null)
        );
        
        assertTrue(exception.getMessage().contains("Cannot register trait"));
        assertTrue(exception.getMessage().contains("after COMPOSITION phase"));
        assertTrue(exception.getMessage().contains("frozen"));
    }
    
    // ===== Invalid Tag Format Tests =====
    
    @Test
    void testRegisterTrait_InvalidTag_NoColon() {
        // Requirement 9.3: Tag must follow namespace:path format
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("tapestry_fish_items"))
        );
        
        assertTrue(exception.getMessage().contains("Invalid tag format"));
        assertTrue(exception.getMessage().contains("namespace:path"));
    }
    
    @Test
    void testRegisterTrait_InvalidTag_Uppercase() {
        // Requirement 9.3: Tag must be lowercase
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("Tapestry:fish_items"))
        );
        
        assertTrue(exception.getMessage().contains("Invalid tag format"));
    }
    
    @Test
    void testRegisterTrait_InvalidTag_Space() {
        // Requirement 9.3: Tag cannot contain spaces
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("tapestry:fish items"))
        );
        
        assertTrue(exception.getMessage().contains("Invalid tag format"));
    }
    
    @Test
    void testRegisterTrait_InvalidTag_EmptyNamespace() {
        // Requirement 9.3: Namespace cannot be empty
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig(":fish_items"))
        );
        
        assertTrue(exception.getMessage().contains("Invalid tag format"));
    }
    
    @Test
    void testRegisterTrait_InvalidTag_EmptyPath() {
        // Requirement 9.3: Path cannot be empty
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("tapestry:"))
        );
        
        assertTrue(exception.getMessage().contains("Invalid tag format"));
    }
    
    // ===== Integration Tests =====
    
    @Test
    void testGetAllTraits_ReturnsUnmodifiableMap() {
        // Verify that getAllTraits returns an immutable view
        traitSystem.register("fish_food", null);
        
        assertThrows(UnsupportedOperationException.class, () -> {
            traitSystem.getAllTraits().put("new_trait", new TraitDefinition("new_trait", "tapestry:new_items"));
        });
    }
    
    @Test
    void testGetTrait_NonExistent() {
        // Verify that getting a non-existent trait returns null
        assertNull(traitSystem.getTrait("non_existent"));
    }
    
    @Test
    void testIsFrozen_InitiallyFalse() {
        // Verify initial state
        assertFalse(traitSystem.isFrozen());
    }
    
    @Test
    void testIsFrozen_AfterFreeze() {
        // Verify frozen state
        traitSystem.freeze();
        assertTrue(traitSystem.isFrozen());
    }
}
