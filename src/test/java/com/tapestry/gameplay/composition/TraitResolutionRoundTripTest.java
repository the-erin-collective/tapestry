package com.tapestry.gameplay.composition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property test for trait resolution round-trip consistency.
 * 
 * **Validates: Requirements 2.6**
 * 
 * This test validates that FOR ALL consumed traits, parsing the generated 
 * behavior tag then resolving items then parsing again SHALL produce an 
 * equivalent item set (round-trip property).
 * 
 * The round-trip process:
 * 1. Register traits and items with trait assignments
 * 2. Resolve traits to behavior tags (COMPOSITION phase)
 * 3. Parse behavior tags back to item sets
 * 4. Verify item sets are equivalent to original assignments
 */
class TraitResolutionRoundTripTest {
    
    @TempDir
    Path tempDir;
    
    private TraitSystem traitSystem;
    private ItemRegistration itemRegistration;
    private TraitResolver resolver;
    private BehaviorTagGenerator generator;
    
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
        resolver = new TraitResolver(traitSystem, itemRegistration);
        generator = new BehaviorTagGenerator(traitSystem, tempDir);
    }
    
    @AfterEach
    void tearDown() {
        PhaseController.reset();
    }
    
    /**
     * Property 1: Round trip consistency
     * 
     * Tests that trait resolution is bijective - no information is lost
     * when converting from trait assignments to behavior tags and back.
     */
    @Test
    void testRoundTripConsistencyWithSingleTraitAndItem() throws IOException {
        // Step 1: Register trait and item with trait assignment
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        
        // Step 2: Resolve traits to behavior tags
        resolver.resolve();
        generator.generateTags();
        
        // Step 3: Parse behavior tags back to item sets
        Map<String, Set<String>> parsedItemSets = parseBehaviorTags();
        
        // Step 4: Verify item sets are equivalent
        assertRoundTripEquivalence("fish_food", Set.of("test:nori"), parsedItemSets);
    }
    
    @Test
    void testRoundTripConsistencyWithMultipleItemsPerTrait() throws IOException {
        // Step 1: Register trait with multiple items
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        itemRegistration.register("test:cod", new ItemOptions().traits("fish_food"));
        itemRegistration.register("test:salmon", new ItemOptions().traits("fish_food"));
        
        // Step 2: Resolve traits to behavior tags
        resolver.resolve();
        generator.generateTags();
        
        // Step 3: Parse behavior tags back to item sets
        Map<String, Set<String>> parsedItemSets = parseBehaviorTags();
        
        // Step 4: Verify item sets are equivalent
        assertRoundTripEquivalence("fish_food", 
            Set.of("test:nori", "test:cod", "test:salmon"), 
            parsedItemSets);
    }
    
    @Test
    void testRoundTripConsistencyWithMultipleTraits() throws IOException {
        // Step 1: Register multiple traits with items
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traitSystem.register("plant_fiber", new TraitConfig("tapestry:plant_fibers"));
        traitSystem.register("sea_vegetable", new TraitConfig("tapestry:sea_vegetables"));
        
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food", "plant_fiber", "sea_vegetable"));
        itemRegistration.register("test:kelp", new ItemOptions().traits("plant_fiber", "sea_vegetable"));
        itemRegistration.register("test:cod", new ItemOptions().traits("fish_food"));
        
        // Step 2: Resolve traits to behavior tags
        resolver.resolve();
        generator.generateTags();
        
        // Step 3: Parse behavior tags back to item sets
        Map<String, Set<String>> parsedItemSets = parseBehaviorTags();
        
        // Step 4: Verify item sets are equivalent for each trait
        assertRoundTripEquivalence("fish_food", 
            Set.of("test:nori", "test:cod"), 
            parsedItemSets);
        assertRoundTripEquivalence("plant_fiber", 
            Set.of("test:nori", "test:kelp"), 
            parsedItemSets);
        assertRoundTripEquivalence("sea_vegetable", 
            Set.of("test:nori", "test:kelp"), 
            parsedItemSets);
    }
    
    @Test
    void testRoundTripConsistencyWithItemsHavingMultipleTraits() throws IOException {
        // Step 1: Register traits and items where items have multiple traits
        traitSystem.register("edible", new TraitConfig("tapestry:edible_items"));
        traitSystem.register("craftable", new TraitConfig("tapestry:craftable_items"));
        traitSystem.register("stackable", new TraitConfig("tapestry:stackable_items"));
        
        itemRegistration.register("test:bread", new ItemOptions().traits("edible", "craftable", "stackable"));
        itemRegistration.register("test:sword", new ItemOptions().traits("craftable"));
        itemRegistration.register("test:apple", new ItemOptions().traits("edible", "stackable"));
        
        // Step 2: Resolve traits to behavior tags
        resolver.resolve();
        generator.generateTags();
        
        // Step 3: Parse behavior tags back to item sets
        Map<String, Set<String>> parsedItemSets = parseBehaviorTags();
        
        // Step 4: Verify item sets are equivalent for each trait
        assertRoundTripEquivalence("edible", 
            Set.of("test:bread", "test:apple"), 
            parsedItemSets);
        assertRoundTripEquivalence("craftable", 
            Set.of("test:bread", "test:sword"), 
            parsedItemSets);
        assertRoundTripEquivalence("stackable", 
            Set.of("test:bread", "test:apple"), 
            parsedItemSets);
    }
    
    @Test
    void testRoundTripConsistencyWithEmptyTraits() throws IOException {
        // Step 1: Register traits with no items
        traitSystem.register("unused_trait", new TraitConfig("tapestry:unused_items"));
        
        // Step 2: Resolve traits to behavior tags
        resolver.resolve();
        generator.generateTags();
        
        // Step 3: Parse behavior tags back to item sets
        Map<String, Set<String>> parsedItemSets = parseBehaviorTags();
        
        // Step 4: Verify empty trait has no tag file (generator skips empty traits)
        assertFalse(parsedItemSets.containsKey("unused_trait"));
    }
    
    @Test
    void testRoundTripConsistencyWithLargeItemSet() throws IOException {
        // Step 1: Register trait with many items (stress test)
        traitSystem.register("common_trait", new TraitConfig("tapestry:common_items"));
        
        Set<String> expectedItems = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            String itemId = "test:item_" + i;
            itemRegistration.register(itemId, new ItemOptions().traits("common_trait"));
            expectedItems.add(itemId);
        }
        
        // Step 2: Resolve traits to behavior tags
        resolver.resolve();
        generator.generateTags();
        
        // Step 3: Parse behavior tags back to item sets
        Map<String, Set<String>> parsedItemSets = parseBehaviorTags();
        
        // Step 4: Verify all items are preserved
        assertRoundTripEquivalence("common_trait", expectedItems, parsedItemSets);
    }
    
    @Test
    void testRoundTripConsistencyWithSpecialCharactersInItemIds() throws IOException {
        // Step 1: Register items with special characters in IDs
        traitSystem.register("special_trait", new TraitConfig("tapestry:special_items"));
        
        itemRegistration.register("test:item_with_underscore", new ItemOptions().traits("special_trait"));
        itemRegistration.register("test:item-with-dash", new ItemOptions().traits("special_trait"));
        itemRegistration.register("test:item123", new ItemOptions().traits("special_trait"));
        
        // Step 2: Resolve traits to behavior tags
        resolver.resolve();
        generator.generateTags();
        
        // Step 3: Parse behavior tags back to item sets
        Map<String, Set<String>> parsedItemSets = parseBehaviorTags();
        
        // Step 4: Verify special characters are preserved
        assertRoundTripEquivalence("special_trait", 
            Set.of("test:item_with_underscore", "test:item-with-dash", "test:item123"), 
            parsedItemSets);
    }
    
    @Test
    void testRoundTripConsistencyPreservesItemOrder() throws IOException {
        // Step 1: Register items in specific order
        traitSystem.register("ordered_trait", new TraitConfig("tapestry:ordered_items"));
        
        itemRegistration.register("z:last", new ItemOptions().traits("ordered_trait"));
        itemRegistration.register("a:first", new ItemOptions().traits("ordered_trait"));
        itemRegistration.register("m:middle", new ItemOptions().traits("ordered_trait"));
        
        // Step 2: Resolve traits to behavior tags
        resolver.resolve();
        generator.generateTags();
        
        // Step 3: Parse behavior tags back to item sets
        Map<String, Set<String>> parsedItemSets = parseBehaviorTags();
        
        // Step 4: Verify all items are present (order doesn't matter for sets)
        assertRoundTripEquivalence("ordered_trait", 
            Set.of("z:last", "a:first", "m:middle"), 
            parsedItemSets);
    }
    
    /**
     * Parses all behavior tag files in the temp directory back to item sets.
     * 
     * @return map of tag path to set of item identifiers
     * @throws IOException if reading tag files fails
     */
    private Map<String, Set<String>> parseBehaviorTags() throws IOException {
        Map<String, Set<String>> itemSets = new HashMap<>();
        
        // Read all JSON files in the temp directory
        Files.list(tempDir)
            .filter(path -> path.toString().endsWith(".json"))
            .forEach(tagFile -> {
                try {
                    // Read tag file
                    String content = Files.readString(tagFile);
                    JsonObject tagJson = JsonParser.parseString(content).getAsJsonObject();
                    
                    // Extract items from values array
                    JsonArray values = tagJson.getAsJsonArray("values");
                    Set<String> items = new HashSet<>();
                    values.forEach(element -> items.add(element.getAsString()));
                    
                    // Use file name (without .json) as the tag path
                    String fileName = tagFile.getFileName().toString();
                    String tagPath = fileName.substring(0, fileName.length() - 5); // Remove .json
                    
                    itemSets.put(tagPath, items);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse tag file: " + tagFile, e);
                }
            });
        
        return itemSets;
    }
    
    /**
     * Asserts that the round-trip conversion preserved the item set for a trait.
     * 
     * @param traitName the trait name
     * @param expectedItems the expected item set
     * @param parsedItemSets the parsed item sets from behavior tags
     */
    private void assertRoundTripEquivalence(String traitName, Set<String> expectedItems, 
                                           Map<String, Set<String>> parsedItemSets) {
        // Get the trait definition to find the actual tag name
        TraitDefinition trait = traitSystem.getTrait(traitName);
        assertNotNull(trait, "Trait not found: " + traitName);
        
        // Extract the path part from the tag (e.g., "tapestry:fish_items" -> "fish_items")
        String tagPath = trait.getTag().split(":", 2)[1];
        
        // Get parsed items for this tag
        Set<String> parsedItems = parsedItemSets.get(tagPath);
        
        // Verify items are equivalent
        assertNotNull(parsedItems, "No parsed items found for trait: " + traitName + " (tag: " + tagPath + ")");
        assertEquals(expectedItems.size(), parsedItems.size(), 
            "Item count mismatch for trait: " + traitName);
        assertEquals(expectedItems, parsedItems, 
            "Item sets are not equivalent for trait: " + traitName);
    }
}
