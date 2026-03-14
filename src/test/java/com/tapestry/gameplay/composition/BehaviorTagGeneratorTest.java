package com.tapestry.gameplay.composition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tapestry.behavior.VanillaTagMergeRegistry;
import com.tapestry.gameplay.items.ItemOptions;
import com.tapestry.gameplay.items.ItemRegistration;
import com.tapestry.gameplay.traits.TraitConfig;
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
 * Unit tests for BehaviorTagGenerator.
 */
class BehaviorTagGeneratorTest {
    
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
        itemRegistration.setTraitSystem(traitSystem); // Wire trait system for validation
        resolver = new TraitResolver(traitSystem, itemRegistration);
        generator = new BehaviorTagGenerator(traitSystem, tempDir);
    }
    
    @AfterEach
    void tearDown() {
        PhaseController.reset();
    }
    
    @Test
    void testGenerateTagsWithNoTraits() throws IOException {
        // Generate tags with no traits registered
        int tagCount = generator.generateTags();
        
        assertEquals(0, tagCount);
        
        // Verify no files were created
        assertTrue(Files.list(tempDir).findAny().isEmpty());
    }
    
    @Test
    void testGenerateTagsWithTraitButNoItems() throws IOException {
        // Register trait with no items
        traitSystem.register("fish_food", new TraitConfig());
        
        // Generate tags
        int tagCount = generator.generateTags();
        
        // Should skip traits with no items
        assertEquals(0, tagCount);
    }
    
    @Test
    void testGenerateSingleTag() throws IOException {
        // Register trait
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        
        // Register items with trait
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        itemRegistration.register("test:cod", new ItemOptions().traits("fish_food"));
        
        // Resolve traits
        resolver.resolve();
        
        // Generate tags
        int tagCount = generator.generateTags();
        
        assertEquals(1, tagCount);
        
        // Verify tag file was created
        Path tagFile = tempDir.resolve("fish_items.json");
        assertTrue(Files.exists(tagFile));
        
        // Verify tag content
        String content = Files.readString(tagFile);
        JsonObject tagJson = JsonParser.parseString(content).getAsJsonObject();
        
        assertFalse(tagJson.get("replace").getAsBoolean());
        
        JsonArray values = tagJson.getAsJsonArray("values");
        assertEquals(2, values.size());
        
        // Values should be sorted
        assertTrue(values.toString().contains("test:cod"));
        assertTrue(values.toString().contains("test:nori"));

        // generation should also register a merge entry for this tag
        String vanilla = VanillaTagMergeRegistry.getMerges().get("tapestry:fish_items");
        assertEquals("minecraft:fish", vanilla);
    }
    
    @Test
    void testGenerateMultipleTags() throws IOException {
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
        
        // Resolve traits
        resolver.resolve();
        
        // Generate tags
        int tagCount = generator.generateTags();
        
        assertEquals(3, tagCount);
        
        // Verify all tag files were created
        assertTrue(Files.exists(tempDir.resolve("fish_items.json")));
        assertTrue(Files.exists(tempDir.resolve("plant_fibers.json")));
        assertTrue(Files.exists(tempDir.resolve("sea_vegetables.json")));
        
        // Verify fish_items tag
        String fishContent = Files.readString(tempDir.resolve("fish_items.json"));
        JsonObject fishJson = JsonParser.parseString(fishContent).getAsJsonObject();
        JsonArray fishValues = fishJson.getAsJsonArray("values");
        assertEquals(2, fishValues.size());
        
        // Verify plant_fibers tag
        String plantContent = Files.readString(tempDir.resolve("plant_fibers.json"));
        JsonObject plantJson = JsonParser.parseString(plantContent).getAsJsonObject();
        JsonArray plantValues = plantJson.getAsJsonArray("values");
        assertEquals(2, plantValues.size());
        
        // Verify sea_vegetables tag
        String seaContent = Files.readString(tempDir.resolve("sea_vegetables.json"));
        JsonObject seaJson = JsonParser.parseString(seaContent).getAsJsonObject();
        JsonArray seaValues = seaJson.getAsJsonArray("values");
        assertEquals(2, seaValues.size());
    }
    
    @Test
    void testTagFormatIsCorrect() throws IOException {
        // Register trait and item
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items"));
        itemRegistration.register("minecraft:cod", new ItemOptions().traits("fish_food"));
        
        // Resolve and generate
        resolver.resolve();
        generator.generateTags();
        
        // Read tag file
        Path tagFile = tempDir.resolve("fish_items.json");
        String content = Files.readString(tagFile);
        JsonObject tagJson = JsonParser.parseString(content).getAsJsonObject();
        
        // Verify structure
        assertTrue(tagJson.has("replace"));
        assertTrue(tagJson.has("values"));
        assertFalse(tagJson.get("replace").getAsBoolean());
        assertTrue(tagJson.get("values").isJsonArray());
    }
    
    @Test
    void testTagValuesAreSorted() throws IOException {
        // Register trait with multiple items
        traitSystem.register("test_trait", new TraitConfig("tapestry:test_items"));
        itemRegistration.register("z:item", new ItemOptions().traits("test_trait"));
        itemRegistration.register("a:item", new ItemOptions().traits("test_trait"));
        itemRegistration.register("m:item", new ItemOptions().traits("test_trait"));
        
        // Resolve and generate
        resolver.resolve();
        generator.generateTags();
        
        // Read tag file
        Path tagFile = tempDir.resolve("test_items.json");
        String content = Files.readString(tagFile);
        JsonObject tagJson = JsonParser.parseString(content).getAsJsonObject();
        JsonArray values = tagJson.getAsJsonArray("values");
        
        // Verify sorted order
        assertEquals("a:item", values.get(0).getAsString());
        assertEquals("m:item", values.get(1).getAsString());
        assertEquals("z:item", values.get(2).getAsString());
    }
    
    @Test
    void testGenerateTagsWithInheritance() throws IOException {
        // Parent and child trait setup
        traitSystem.register("food", new TraitConfig("tapestry:food_items"));
        traitSystem.register("fish_food", new TraitConfig("tapestry:fish_items", "food"));
        
        // Item only declares the child trait
        itemRegistration.register("test:nori", new ItemOptions().traits("fish_food"));
        
        // Resolve to propagate items
        resolver.resolve();
        
        // Generate tags
        int tagCount = generator.generateTags();
        assertEquals(2, tagCount);
        
        // Both files should exist
        assertTrue(Files.exists(tempDir.resolve("food_items.json")));
        assertTrue(Files.exists(tempDir.resolve("fish_items.json")));
        
        // Parent file must list the child item
        String parentJsonStr = Files.readString(tempDir.resolve("food_items.json"));
        JsonArray parentValues = JsonParser.parseString(parentJsonStr)
            .getAsJsonObject().getAsJsonArray("values");
        assertEquals(1, parentValues.size());
        assertEquals("test:nori", parentValues.get(0).getAsString());
    }

    @Test
    void testDefaultTagNameGeneration() throws IOException {
        // Register trait without explicit tag (should use default pattern)
        traitSystem.register("custom_trait", new TraitConfig());
        itemRegistration.register("test:item", new ItemOptions().traits("custom_trait"));
        
        // Resolve and generate
        resolver.resolve();
        generator.generateTags();
        
        // Verify tag file was created with default name
        Path tagFile = tempDir.resolve("custom_trait_items.json");
        assertTrue(Files.exists(tagFile));
    }
}
