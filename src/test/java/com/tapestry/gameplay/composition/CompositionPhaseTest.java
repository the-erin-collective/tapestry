package com.tapestry.gameplay.composition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tapestry.gameplay.items.ItemOptions;
import com.tapestry.gameplay.items.ItemRegistration;
import com.tapestry.gameplay.traits.ConsumptionConfig;
import com.tapestry.gameplay.traits.TraitConfig;
import com.tapestry.gameplay.traits.TraitDefinition;
import com.tapestry.gameplay.traits.TraitSystem;
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
 * Comprehensive unit tests for COMPOSITION phase.
 * 
 * Tests cover:
 * - Trait-to-item mapping resolution (Requirement 2.3, 8.2)
 * - Behavior tag generation format (Requirement 8.3, 8.4)
 * - Registry freezing behavior (Requirement 2.4, 8.6)
 * - Post-freeze modification errors (Requirement 2.5)
 */
class CompositionPhaseTest {
    
    @TempDir
    Path tempDir;
    
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
        itemRegistration.setTraitSystem(traitSystem);
        
        // Create orchestrator with temp directory for tag generation
        orchestrator = new CompositionOrchestrator(traitSystem, itemRegistration);
    }
    
    @AfterEach
    void tearDown() {
        PhaseController.reset();
    }
    
    // ========================================
    // Trait-to-Item Mapping Resolution Tests
    // Requirements: 2.3, 8.2
    // ========================================
    
    @Test
    void testTraitToItemMappingResolution_SingleTraitSingleItem() throws IOException {
        // Register trait
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        // Register item with trait
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify mapping was resolved
        TraitDefinition trait = traitSystem.getTrait("fish_food");
        assertNotNull(trait);
        assertEquals(1, trait.getItems().size());
        assertTrue(trait.getItems().contains("test:nori"));
    }
    
    @Test
    void testTraitToItemMappingResolution_SingleTraitMultipleItems() throws IOException {
        // Register trait
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        // Register multiple items with same trait
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        itemRegistration.register("test:cod", new ItemOptions().traits("fish_food"));
        itemRegistration.register("test:salmon", new ItemOptions().traits("fish_food"));
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify all items mapped to trait
        TraitDefinition trait = traitSystem.getTrait("fish_food");
        assertEquals(3, trait.getItems().size());
        assertTrue(trait.getItems().contains("test:nori"));
        assertTrue(trait.getItems().contains("test:cod"));
        assertTrue(trait.getItems().contains("test:salmon"));
    }
    
    @Test
    void testTraitToItemMappingResolution_MultipleTraitsSingleItem() throws IOException {
        // Register multiple traits
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traitSystem.register("plant_fiber", new TraitConfig("tapestry:plant_fibers"));
        traitSystem.register("sea_vegetable", new TraitConfig("tapestry:sea_vegetables"));
        
        // Register item with multiple traits
        itemRegistration.register("test:nori", 
            new ItemOptions().traits("fish_food", "plant_fiber", "sea_vegetable"));
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify item mapped to all traits
        assertTrue(traitSystem.getTrait("fish_food").getItems().contains("test:nori"));
        assertTrue(traitSystem.getTrait("plant_fiber").getItems().contains("test:nori"));
        assertTrue(traitSystem.getTrait("sea_vegetable").getItems().contains("test:nori"));
    }
    
    @Test
    void testTraitToItemMappingResolution_ComplexManyToMany() throws IOException {
        // Register traits
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traitSystem.register("plant_fiber", new TraitConfig("tapestry:plant_fibers"));
        traitSystem.register("sea_vegetable", new TraitConfig("tapestry:sea_vegetables"));
        
        // Register items with various trait combinations
        itemRegistration.register("test:nori", 
            new ItemOptions().traits("fish_food", "plant_fiber", "sea_vegetable"));
        itemRegistration.register("test:kelp", 
            new ItemOptions().traits("plant_fiber", "sea_vegetable"));
        itemRegistration.register("test:cod", 
            new ItemOptions().traits("fish_food"));
        itemRegistration.register("test:seaweed", 
            new ItemOptions().traits("sea_vegetable"));
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify fish_food trait
        TraitDefinition fishFood = traitSystem.getTrait("fish_food");
        assertEquals(2, fishFood.getItems().size());
        assertTrue(fishFood.getItems().contains("test:nori"));
        assertTrue(fishFood.getItems().contains("test:cod"));
        
        // Verify plant_fiber trait
        TraitDefinition plantFiber = traitSystem.getTrait("plant_fiber");
        assertEquals(2, plantFiber.getItems().size());
        assertTrue(plantFiber.getItems().contains("test:nori"));
        assertTrue(plantFiber.getItems().contains("test:kelp"));
        
        // Verify sea_vegetable trait
        TraitDefinition seaVegetable = traitSystem.getTrait("sea_vegetable");
        assertEquals(3, seaVegetable.getItems().size());
        assertTrue(seaVegetable.getItems().contains("test:nori"));
        assertTrue(seaVegetable.getItems().contains("test:kelp"));
        assertTrue(seaVegetable.getItems().contains("test:seaweed"));
    }
    
    @Test
    void testTraitToItemMappingResolution_ItemsWithoutTraits() throws IOException {
        // Register trait
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        // Register items - some with traits, some without
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        itemRegistration.register("test:stick", new ItemOptions()); // No traits
        itemRegistration.register("test:stone", new ItemOptions()); // No traits
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify only items with traits are mapped
        TraitDefinition trait = traitSystem.getTrait("fish_food");
        assertEquals(1, trait.getItems().size());
        assertTrue(trait.getItems().contains("test:nori"));
        assertFalse(trait.getItems().contains("test:stick"));
        assertFalse(trait.getItems().contains("test:stone"));
    }
    
    @Test
    void testTraitToItemMappingResolution_TraitsWithoutItems() throws IOException {
        // Register traits
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traitSystem.register("unused_trait", new TraitConfig("tapestry:unused_items"));
        
        // Register item with only one trait
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify fish_food has items
        assertEquals(1, traitSystem.getTrait("fish_food").getItems().size());
        
        // Verify unused_trait has no items
        assertEquals(0, traitSystem.getTrait("unused_trait").getItems().size());
    }
    
    // ========================================
    // Behavior Tag Generation Format Tests
    // Requirements: 8.3, 8.4
    // ========================================
    
    @Test
    void testBehaviorTagGenerationFormat_BasicStructure() throws IOException {
        // Register trait and item
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        
        // Create generator with temp directory
        BehaviorTagGenerator generator = new BehaviorTagGenerator(traitSystem, tempDir);
        TraitResolver resolver = new TraitResolver(traitSystem, itemRegistration);
        
        // Resolve and generate
        resolver.resolve();
        generator.generateTags();
        
        // Read generated tag file
        Path tagFile = tempDir.resolve("fish_items.json");
        assertTrue(Files.exists(tagFile), "Tag file should be created");
        
        String content = Files.readString(tagFile);
        JsonObject tagJson = JsonParser.parseString(content).getAsJsonObject();
        
        // Verify required fields exist
        assertTrue(tagJson.has("replace"), "Tag must have 'replace' field");
        assertTrue(tagJson.has("values"), "Tag must have 'values' field");
        
        // Verify replace is false (Requirement 8.3)
        assertFalse(tagJson.get("replace").getAsBoolean(), 
            "Tag 'replace' must be false to allow datapack merging");
        
        // Verify values is an array
        assertTrue(tagJson.get("values").isJsonArray(), 
            "Tag 'values' must be a JSON array");
    }
    
    @Test
    void testBehaviorTagGenerationFormat_ValuesArray() throws IOException {
        // Register trait with multiple items
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        itemRegistration.register("test:cod", new ItemOptions().traits("fish_food"));
        itemRegistration.register("test:salmon", new ItemOptions().traits("fish_food"));
        
        // Create generator and resolver
        BehaviorTagGenerator generator = new BehaviorTagGenerator(traitSystem, tempDir);
        TraitResolver resolver = new TraitResolver(traitSystem, itemRegistration);
        
        // Resolve and generate
        resolver.resolve();
        generator.generateTags();
        
        // Read generated tag file
        Path tagFile = tempDir.resolve("fish_items.json");
        String content = Files.readString(tagFile);
        JsonObject tagJson = JsonParser.parseString(content).getAsJsonObject();
        JsonArray values = tagJson.getAsJsonArray("values");
        
        // Verify all items are in values array
        assertEquals(3, values.size(), "Values array should contain all items");
        
        // Convert to set for easier checking
        java.util.Set<String> valueSet = new java.util.HashSet<>();
        values.forEach(element -> valueSet.add(element.getAsString()));
        
        assertTrue(valueSet.contains("test:nori"));
        assertTrue(valueSet.contains("test:cod"));
        assertTrue(valueSet.contains("test:salmon"));
    }
    
    @Test
    void testBehaviorTagGenerationFormat_ValuesSorted() throws IOException {
        // Register trait with items in non-alphabetical order
        traitSystem.register("test_trait", new TraitConfig("tapestry:test_items"));
        itemRegistration.register("z:item", new ItemOptions().traits("test_trait"));
        itemRegistration.register("a:item", new ItemOptions().traits("test_trait"));
        itemRegistration.register("m:item", new ItemOptions().traits("test_trait"));
        
        // Create generator and resolver
        BehaviorTagGenerator generator = new BehaviorTagGenerator(traitSystem, tempDir);
        TraitResolver resolver = new TraitResolver(traitSystem, itemRegistration);
        
        // Resolve and generate
        resolver.resolve();
        generator.generateTags();
        
        // Read generated tag file
        Path tagFile = tempDir.resolve("test_items.json");
        String content = Files.readString(tagFile);
        JsonObject tagJson = JsonParser.parseString(content).getAsJsonObject();
        JsonArray values = tagJson.getAsJsonArray("values");
        
        // Verify values are sorted alphabetically
        assertEquals("a:item", values.get(0).getAsString());
        assertEquals("m:item", values.get(1).getAsString());
        assertEquals("z:item", values.get(2).getAsString());
    }
    
    @Test
    void testBehaviorTagGenerationFormat_MultipleTagsGenerated() throws IOException {
        // Register multiple traits with items
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traitSystem.register("plant_fiber", new TraitConfig("tapestry:plant_fibers"));
        traitSystem.register("sea_vegetable", new TraitConfig("tapestry:sea_vegetables"));
        
        itemRegistration.register("test:nori", 
            new ItemOptions().traits("fish_food", "plant_fiber", "sea_vegetable"));
        itemRegistration.register("test:kelp", 
            new ItemOptions().traits("plant_fiber", "sea_vegetable"));
        itemRegistration.register("test:cod", 
            new ItemOptions().traits("fish_food"));
        
        // Create generator and resolver
        BehaviorTagGenerator generator = new BehaviorTagGenerator(traitSystem, tempDir);
        TraitResolver resolver = new TraitResolver(traitSystem, itemRegistration);
        
        // Resolve and generate
        resolver.resolve();
        int tagCount = generator.generateTags();
        
        // Verify all tags were generated
        assertEquals(3, tagCount, "Should generate one tag per trait");
        
        // Verify all tag files exist
        assertTrue(Files.exists(tempDir.resolve("fish_items.json")));
        assertTrue(Files.exists(tempDir.resolve("plant_fibers.json")));
        assertTrue(Files.exists(tempDir.resolve("sea_vegetables.json")));
        
        // Verify each tag has correct format
        for (String tagFile : new String[]{"fish_items.json", "plant_fibers.json", "sea_vegetables.json"}) {
            String content = Files.readString(tempDir.resolve(tagFile));
            JsonObject tagJson = JsonParser.parseString(content).getAsJsonObject();
            
            assertFalse(tagJson.get("replace").getAsBoolean());
            assertTrue(tagJson.get("values").isJsonArray());
        }
    }
    
    @Test
    void testBehaviorTagGenerationFormat_EmptyTraitsSkipped() throws IOException {
        // Register traits
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traitSystem.register("empty_trait", new TraitConfig("tapestry:empty_items"));
        
        // Register item with only one trait
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        
        // Create generator and resolver
        BehaviorTagGenerator generator = new BehaviorTagGenerator(traitSystem, tempDir);
        TraitResolver resolver = new TraitResolver(traitSystem, itemRegistration);
        
        // Resolve and generate
        resolver.resolve();
        int tagCount = generator.generateTags();
        
        // Verify only non-empty trait generated a tag
        assertEquals(1, tagCount, "Should only generate tags for traits with items");
        assertTrue(Files.exists(tempDir.resolve("fish_items.json")));
        assertFalse(Files.exists(tempDir.resolve("empty_items.json")));
    }
    
    // ========================================
    // Registry Freezing Behavior Tests
    // Requirements: 2.4, 8.6
    // ========================================
    
    @Test
    void testRegistryFreezingBehavior_NotFrozenBeforeComposition() {
        // Register trait
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        // Verify not frozen before composition
        assertFalse(traitSystem.isFrozen(), 
            "Trait system should not be frozen before COMPOSITION phase");
    }
    
    @Test
    void testRegistryFreezingBehavior_FrozenAfterComposition() throws IOException {
        // Register trait and item
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        
        // Verify not frozen before
        assertFalse(traitSystem.isFrozen());
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify frozen after (Requirement 2.4, 8.6)
        assertTrue(traitSystem.isFrozen(), 
            "Trait system must be frozen after COMPOSITION phase completes");
    }
    
    @Test
    void testRegistryFreezingBehavior_TraitDefinitionsFrozen() throws IOException {
        // Register trait and item
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify trait definition is frozen
        TraitDefinition trait = traitSystem.getTrait("fish_food");
        assertTrue(trait.isFrozen(), 
            "Individual trait definitions must be frozen after COMPOSITION");
    }
    
    @Test
    void testRegistryFreezingBehavior_AllTraitsFrozen() throws IOException {
        // Register multiple traits
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traitSystem.register("plant_fiber", new TraitConfig("tapestry:plant_fibers"));
        traitSystem.register("sea_vegetable", new TraitConfig("tapestry:sea_vegetables"));
        
        // Register items
        itemRegistration.register("test:nori", 
            new ItemOptions().traits("fish_food", "plant_fiber"));
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify all traits are frozen
        assertTrue(traitSystem.getTrait("fish_food").isFrozen());
        assertTrue(traitSystem.getTrait("plant_fiber").isFrozen());
        assertTrue(traitSystem.getTrait("sea_vegetable").isFrozen());
    }
    
    @Test
    void testRegistryFreezingBehavior_FreezeIsImmutable() throws IOException {
        // Register trait
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify frozen
        assertTrue(traitSystem.isFrozen());
        
        // Attempt to unfreeze should not be possible
        // (TraitSystem doesn't expose unfreeze method, so this is guaranteed by design)
        // Verify frozen state persists
        assertTrue(traitSystem.isFrozen());
    }
    
    // ========================================
    // Post-Freeze Modification Error Tests
    // Requirement: 2.5
    // ========================================
    
    @Test
    void testPostFreezeModificationError_CannotRegisterTraitAfterFreeze() throws IOException {
        // Register initial trait
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        // Execute composition (freezes registries)
        orchestrator.executeComposition();
        
        // Attempt to register new trait after freeze (Requirement 2.5)
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> traitSystem.register("new_trait", new TraitConfig()),
            "Should throw IllegalStateException when registering trait after COMPOSITION"
        );
        
        assertTrue(exception.getMessage().contains("frozen"),
            "Error message should mention registry is frozen");
        assertTrue(exception.getMessage().contains("COMPOSITION"),
            "Error message should mention COMPOSITION phase");
    }
    
    @Test
    void testPostFreezeModificationError_CannotConsumeTraitAfterFreeze() throws IOException {
        // Register trait
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        // Execute composition (freezes registries)
        orchestrator.executeComposition();
        
        // Attempt to consume trait after freeze (Requirement 2.5)
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> traitSystem.consume("fish_food", 
                new ConsumptionConfig("minecraft:cat", "food")),
            "Should throw IllegalStateException when consuming trait after COMPOSITION"
        );
        
        assertTrue(exception.getMessage().contains("frozen"),
            "Error message should mention registry is frozen");
    }
    
    @Test
    void testPostFreezeModificationError_CannotAddItemsToTraitAfterFreeze() throws IOException {
        // Register trait and item
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        
        // Execute composition (freezes registries)
        orchestrator.executeComposition();
        
        // Verify trait is frozen
        TraitDefinition trait = traitSystem.getTrait("fish_food");
        assertTrue(trait.isFrozen());
        
        // Attempt to add item to frozen trait (Requirement 2.5)
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> trait.addItem("test:new_item"),
            "Should throw IllegalStateException when adding item to frozen trait"
        );
        
        assertTrue(exception.getMessage().contains("frozen"),
            "Error message should mention trait is frozen");
    }
    
    @Test
    void testPostFreezeModificationError_MultipleModificationAttempts() throws IOException {
        // Register trait
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Attempt multiple modifications - all should fail
        assertThrows(IllegalStateException.class,
            () -> traitSystem.register("new_trait1", new TraitConfig()));
        
        assertThrows(IllegalStateException.class,
            () -> traitSystem.register("new_trait2", new TraitConfig()));
        
        assertThrows(IllegalStateException.class,
            () -> traitSystem.consume("fish_food", 
                new ConsumptionConfig("minecraft:cat", "food")));
        
        // Verify system remains frozen
        assertTrue(traitSystem.isFrozen());
    }
    
    @Test
    void testPostFreezeModificationError_ErrorMessageDescriptive() throws IOException {
        // Register trait
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Attempt to register trait and verify error message quality
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> traitSystem.register("new_trait", new TraitConfig())
        );
        
        String message = exception.getMessage();
        
        // Verify error message contains key information
        assertTrue(message.contains("new_trait"), 
            "Error should mention the trait name being registered");
        assertTrue(message.contains("frozen") || message.contains("COMPOSITION"),
            "Error should explain why registration failed");
    }
    
    // ========================================
    // Integration Tests
    // ========================================
    
    @Test
    void testCompositionPhaseIntegration_CompleteWorkflow() throws IOException {
        // Register traits
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traitSystem.register("plant_fiber", new TraitConfig("tapestry:plant_fibers"));
        
        // Register items
        itemRegistration.register("test:nori", 
            new ItemOptions().traits("fish_food", "plant_fiber"));
        itemRegistration.register("test:cod", 
            new ItemOptions().traits("fish_food"));
        
        // Register consumption
        traitSystem.consume("fish_food", 
            new ConsumptionConfig("minecraft:cat", "food"));
        
        // Verify initial state
        assertFalse(traitSystem.isFrozen());
        
        // Execute composition
        orchestrator.executeComposition();
        
        // Verify trait-to-item mappings resolved
        assertEquals(2, traitSystem.getTrait("fish_food").getItems().size());
        assertEquals(1, traitSystem.getTrait("plant_fiber").getItems().size());
        
        // Verify registries frozen
        assertTrue(traitSystem.isFrozen());
        assertTrue(traitSystem.getTrait("fish_food").isFrozen());
        assertTrue(traitSystem.getTrait("plant_fiber").isFrozen());
        
        // Verify post-freeze modifications fail
        assertThrows(IllegalStateException.class,
            () -> traitSystem.register("new_trait", new TraitConfig()));
    }
    
    @Test
    void testCompositionPhaseIntegration_EmptyRegistries() throws IOException {
        // Execute composition with no data
        assertDoesNotThrow(() -> orchestrator.executeComposition(),
            "Composition should succeed even with no traits or items");
        
        // Verify system is frozen
        assertTrue(traitSystem.isFrozen());
    }
}
