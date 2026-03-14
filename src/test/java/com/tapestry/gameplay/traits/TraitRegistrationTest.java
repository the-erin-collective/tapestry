package com.tapestry.gameplay.traits;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for trait registration functionality.
 * 
 * Tests Requirements:
 * - 1.1: Trait registration during TS_REGISTER phase only
 * - 1.3: Fatal error when duplicate trait names are registered
 * - 1.5: Fatal error when registration attempted outside TS_REGISTER phase
 * - 1.6: Tag mapping validation follows Minecraft namespace:path format
 */
class TraitRegistrationTest {
    
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
    
    // ===== Test Case 1: Successful trait registration with valid name and config =====
    
    @Test
    void testSuccessfulRegistration_WithCustomTag() {
        // Requirement 1.1: Register trait with valid name and custom tag during TS_REGISTER phase
        TraitConfig config = new TraitConfig("minecraft:cat_food");
        
        assertDoesNotThrow(() -> traitSystem.register("fish_food", config));
        
        TraitDefinition trait = traitSystem.getTrait("fish_food");
        assertNotNull(trait, "Trait should be registered");
        assertEquals("fish_food", trait.getName());
        assertEquals("minecraft:cat_food", trait.getTag());
    }
    
    @Test
    void testSuccessfulRegistration_WithDefaultTag() {
        // Requirement 1.1: Register trait with default tag generation
        assertDoesNotThrow(() -> traitSystem.register("fish_food", null));
        
        TraitDefinition trait = traitSystem.getTrait("fish_food");
        assertNotNull(trait, "Trait should be registered");
        assertEquals("fish_food", trait.getName());
        assertEquals("tapestry:fish_food_items", trait.getTag());
    }
    
    @Test
    void testSuccessfulRegistration_WithEmptyConfig() {
        // Requirement 1.1: Register trait with empty config (null tag)
        TraitConfig config = new TraitConfig();
        
        assertDoesNotThrow(() -> traitSystem.register("milk_like", config));
        
        TraitDefinition trait = traitSystem.getTrait("milk_like");
        assertNotNull(trait, "Trait should be registered");
        assertEquals("tapestry:milk_like_items", trait.getTag());
    }
    
    @Test
    void testSuccessfulRegistration_MultipleTraits() {
        // Requirement 1.1: Register multiple different traits successfully
        assertDoesNotThrow(() -> {
            traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
            traitSystem.register("milk_like", new TraitConfig("tapestry:milk_items"));
            traitSystem.register("egg_like", new TraitConfig("tapestry:egg_items"));
            traitSystem.register("honey_like", new TraitConfig("tapestry:honey_items"));
            traitSystem.register("plant_fiber", new TraitConfig("tapestry:plant_fibers"));
        });
        
        assertEquals(5, traitSystem.getAllTraits().size());
        assertNotNull(traitSystem.getTrait("fish_food"));
        assertNotNull(traitSystem.getTrait("milk_like"));
        assertNotNull(traitSystem.getTrait("egg_like"));
        assertNotNull(traitSystem.getTrait("honey_like"));
        assertNotNull(traitSystem.getTrait("plant_fiber"));
    }
    
    // ===== Test Case 2: Duplicate trait name error =====
    
    @Test
    void testDuplicateTraitName_ThrowsError() {
        // Requirement 1.3: Fatal error when duplicate trait names are registered
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("tapestry:other_tag")),
            "Registering duplicate trait name should throw IllegalArgumentException"
        );
        
        assertTrue(exception.getMessage().contains("already registered"),
            "Error message should indicate trait is already registered");
        assertTrue(exception.getMessage().contains("fish_food"),
            "Error message should include the trait name");
        assertTrue(exception.getMessage().contains("Duplicate trait names are not allowed"),
            "Error message should explain duplicate names are not allowed");
    }
    
    @Test
    void testDuplicateTraitName_DifferentTags() {
        // Requirement 1.3: Duplicate names not allowed even with different tags
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("minecraft:cat_food")),
            "Duplicate trait name with different tag should still throw error"
        );
        
        assertTrue(exception.getMessage().contains("already registered"));
    }
    
    @Test
    void testDuplicateTraitName_SameTag() {
        // Requirement 1.3: Duplicate names not allowed even with same tag
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items")),
            "Duplicate trait name with same tag should throw error"
        );
    }
    
    // ===== Test Case 2.5: Inheritance support =====
    
    @Test
    void testRegisterTrait_WithParent() {
        // Verify child trait stores parent name correctly
        traitSystem.register("food", new TraitConfig("tapestry:food_items"));
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items", "food"));
        TraitDefinition child = traitSystem.getTrait("fish_food");
        assertNotNull(child);
        assertEquals("food", child.getParentName());
    }

    @Test
    void testRegisterTrait_ParentMayNotExistUntilResolve() {
        // Allow registering a trait that extends a parent which is not yet defined
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items", "food"));
        TraitDefinition child = traitSystem.getTrait("fish_food");
        assertNotNull(child);
        assertEquals("food", child.getParentName());
        // validation of missing parent will occur later during composition
    }

    @Test
    void testRegisterTrait_CannotExtendSelf() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("self", new TraitConfig("tapestry:self_items", "self"))
        );
        assertTrue(ex.getMessage().contains("cannot extend itself"));
    }

    // ===== Test Case 3: Phase violation errors =====
    
    @Test
    void testPhaseViolation_BeforeRegisterPhase() {
        // Requirement 1.5: Fatal error when registration attempted before TS_REGISTER phase
        PhaseController.reset();
        PhaseController controller = PhaseController.getInstance();
        controller.advanceTo(TapestryPhase.DISCOVERY);
        
        TraitSystem system = new TraitSystem();
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> system.register("fish_food", null),
            "Registration before TS_REGISTER phase should throw IllegalStateException"
        );
        
        assertTrue(exception.getMessage().contains("TS_REGISTER"),
            "Error message should mention TS_REGISTER phase");
        assertTrue(exception.getMessage().contains("DISCOVERY"),
            "Error message should mention current phase");
    }
    
    @Test
    void testPhaseViolation_AfterRegisterPhase() {
        // Requirement 1.5: Fatal error when registration attempted after TS_REGISTER phase
        phaseController.advanceTo(TapestryPhase.TS_ACTIVATE);
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> traitSystem.register("fish_food", null),
            "Registration after TS_REGISTER phase should throw IllegalStateException"
        );
        
        assertTrue(exception.getMessage().contains("TS_REGISTER"),
            "Error message should mention TS_REGISTER phase");
        assertTrue(exception.getMessage().contains("TS_ACTIVATE"),
            "Error message should mention current phase");
    }
    
    @Test
    void testPhaseViolation_InRuntimePhase() {
        // Requirement 1.5: Cannot register during RUNTIME phase
        phaseController.advanceTo(TapestryPhase.TS_ACTIVATE);
        phaseController.advanceTo(TapestryPhase.TS_READY);
        phaseController.advanceTo(TapestryPhase.PERSISTENCE_READY);
        phaseController.advanceTo(TapestryPhase.EVENT);
        phaseController.advanceTo(TapestryPhase.RUNTIME);
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> traitSystem.register("fish_food", null),
            "Registration during RUNTIME phase should throw IllegalStateException"
        );
        
        assertTrue(exception.getMessage().contains("TS_REGISTER"));
    }
    
    @Test
    void testPhaseViolation_InValidationPhase() {
        // Requirement 1.5: Cannot register during VALIDATION phase (before TS_REGISTER)
        PhaseController.reset();
        PhaseController controller = PhaseController.getInstance();
        controller.advanceTo(TapestryPhase.DISCOVERY);
        controller.advanceTo(TapestryPhase.VALIDATION);
        
        TraitSystem system = new TraitSystem();
        
        assertThrows(
            IllegalStateException.class,
            () -> system.register("fish_food", null),
            "Registration during VALIDATION phase should throw error"
        );
    }
    
    @Test
    void testPhaseViolation_AfterFreeze() {
        // Requirement 1.5: Cannot register after registry is frozen
        traitSystem.register("fish_food", null);
        traitSystem.freeze();
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> traitSystem.register("milk_like", null),
            "Registration after freeze should throw IllegalStateException"
        );
        
        assertTrue(exception.getMessage().contains("Cannot register trait"),
            "Error message should indicate registration is not allowed");
        assertTrue(exception.getMessage().contains("after COMPOSITION phase"),
            "Error message should mention COMPOSITION phase");
        assertTrue(exception.getMessage().contains("frozen"),
            "Error message should mention registry is frozen");
    }
    
    // ===== Test Case 4: Tag name validation =====
    
    @Test
    void testTagValidation_ValidFormats() {
        // Requirement 1.6: Valid namespace:path formats should be accepted
        assertDoesNotThrow(() -> traitSystem.register("test1", 
            new TraitConfig("tapestry:fish_items")),
            "Standard namespace:path format should be valid");
        
        assertDoesNotThrow(() -> traitSystem.register("test2", 
            new TraitConfig("minecraft:cat_food")),
            "Minecraft namespace should be valid");
        
        assertDoesNotThrow(() -> traitSystem.register("test3", 
            new TraitConfig("mymod:custom_tag")),
            "Custom mod namespace should be valid");
        
        assertDoesNotThrow(() -> traitSystem.register("test4", 
            new TraitConfig("mod-name:tag-name")),
            "Hyphens in namespace and path should be valid");
        
        assertDoesNotThrow(() -> traitSystem.register("test5", 
            new TraitConfig("mod.name:tag.name")),
            "Dots in namespace and path should be valid");
        
        assertDoesNotThrow(() -> traitSystem.register("test6", 
            new TraitConfig("tapestry:items/fish")),
            "Slashes in path for nested tags should be valid");
        
        assertDoesNotThrow(() -> traitSystem.register("test7", 
            new TraitConfig("mod_name:tag_name")),
            "Underscores should be valid");
    }
    
    @Test
    void testTagValidation_InvalidFormat_NoColon() {
        // Requirement 1.6: Tag must follow namespace:path format
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("tapestry_fish_items")),
            "Tag without colon should throw IllegalArgumentException"
        );
        
        assertTrue(exception.getMessage().contains("Invalid tag format"),
            "Error message should indicate invalid tag format");
        assertTrue(exception.getMessage().contains("namespace:path"),
            "Error message should mention namespace:path format");
    }
    
    @Test
    void testTagValidation_InvalidFormat_Uppercase() {
        // Requirement 1.6: Tag must be lowercase
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("Tapestry:fish_items")),
            "Tag with uppercase letters should throw IllegalArgumentException"
        );
        
        assertTrue(exception.getMessage().contains("Invalid tag format"));
    }
    
    @Test
    void testTagValidation_InvalidFormat_UppercasePath() {
        // Requirement 1.6: Path must be lowercase
        assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("tapestry:Fish_Items")),
            "Tag with uppercase in path should throw error"
        );
    }
    
    @Test
    void testTagValidation_InvalidFormat_Space() {
        // Requirement 1.6: Tag cannot contain spaces
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("tapestry:fish items")),
            "Tag with spaces should throw IllegalArgumentException"
        );
        
        assertTrue(exception.getMessage().contains("Invalid tag format"));
    }
    
    @Test
    void testTagValidation_InvalidFormat_SpaceInNamespace() {
        // Requirement 1.6: Namespace cannot contain spaces
        assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("my mod:fish_items")),
            "Tag with space in namespace should throw error"
        );
    }
    
    @Test
    void testTagValidation_InvalidFormat_EmptyNamespace() {
        // Requirement 1.6: Namespace cannot be empty
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig(":fish_items")),
            "Tag with empty namespace should throw IllegalArgumentException"
        );
        
        assertTrue(exception.getMessage().contains("Invalid tag format"));
    }
    
    @Test
    void testTagValidation_InvalidFormat_EmptyPath() {
        // Requirement 1.6: Path cannot be empty
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("tapestry:")),
            "Tag with empty path should throw IllegalArgumentException"
        );
        
        assertTrue(exception.getMessage().contains("Invalid tag format"));
    }
    
    @Test
    void testTagValidation_InvalidFormat_MultipleColons() {
        // Requirement 1.6: Only one colon separator allowed
        assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("tapestry:fish:items")),
            "Tag with multiple colons should throw error"
        );
    }
    
    @Test
    void testTagValidation_InvalidFormat_SpecialCharacters() {
        // Requirement 1.6: Special characters not allowed
        assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food", new TraitConfig("tapestry:fish@items")),
            "Tag with @ symbol should throw error"
        );
        
        assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food2", new TraitConfig("tapestry:fish#items")),
            "Tag with # symbol should throw error"
        );
        
        assertThrows(
            IllegalArgumentException.class,
            () -> traitSystem.register("fish_food3", new TraitConfig("tapestry:fish$items")),
            "Tag with $ symbol should throw error"
        );
    }
    
    // ===== Integration Tests =====
    
    @Test
    void testRegistration_TraitStoredInRegistry() {
        // Verify trait is properly stored and retrievable
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        TraitDefinition trait = traitSystem.getTrait("fish_food");
        assertNotNull(trait);
        assertEquals("fish_food", trait.getName());
        assertEquals("tapestry:fish_items", trait.getTag());
    }
    
    @Test
    void testRegistration_GetAllTraitsReturnsUnmodifiable() {
        // Verify getAllTraits returns immutable view
        traitSystem.register("fish_food", null);
        
        assertThrows(UnsupportedOperationException.class, () -> {
            traitSystem.getAllTraits().put("new_trait", 
                new TraitDefinition("new_trait", "tapestry:new_items"));
        }, "getAllTraits should return unmodifiable map");
    }
    
    @Test
    void testRegistration_NonExistentTrait() {
        // Verify getting non-existent trait returns null
        assertNull(traitSystem.getTrait("non_existent"),
            "Non-existent trait should return null");
    }
    
    @Test
    void testRegistration_FrozenState() {
        // Verify frozen state tracking
        assertFalse(traitSystem.isFrozen(), "Initially should not be frozen");
        
        traitSystem.freeze();
        
        assertTrue(traitSystem.isFrozen(), "Should be frozen after freeze()");
    }
    
    @Test
    void testRegistration_TraitCountTracking() {
        // Verify trait count is tracked correctly
        assertEquals(0, traitSystem.getAllTraits().size());
        
        traitSystem.register("trait1", null);
        assertEquals(1, traitSystem.getAllTraits().size());
        
        traitSystem.register("trait2", null);
        assertEquals(2, traitSystem.getAllTraits().size());
        
        traitSystem.register("trait3", null);
        assertEquals(3, traitSystem.getAllTraits().size());
    }
}
