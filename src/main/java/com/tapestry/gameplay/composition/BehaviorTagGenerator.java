package com.tapestry.gameplay.composition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tapestry.gameplay.traits.TraitDefinition;
import com.tapestry.gameplay.traits.TraitSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * Generates Minecraft behavior tags from trait definitions during COMPOSITION phase.
 * 
 * Behavior tags enable vanilla compatibility by creating standard Minecraft item tags
 * that contain all items possessing a specific trait. These tags can be used by
 * vanilla entities and other mods without requiring explicit integration.
 */
public class BehaviorTagGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BehaviorTagGenerator.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final TraitSystem traitSystem;
    private final Path outputDirectory;
    
    /**
     * Creates a new behavior tag generator.
     * 
     * @param traitSystem the trait system containing trait definitions
     * @param outputDirectory the directory where tag files will be written
     */
    public BehaviorTagGenerator(TraitSystem traitSystem, Path outputDirectory) {
        this.traitSystem = traitSystem;
        this.outputDirectory = outputDirectory;
    }
    
    /**
     * Creates a behavior tag generator with default output directory.
     * Tags will be written to src/main/resources/data/tapestry/tags/items/
     * 
     * @param traitSystem the trait system containing trait definitions
     */
    public BehaviorTagGenerator(TraitSystem traitSystem) {
        this(traitSystem, Paths.get("src/main/resources/data/tapestry/tags/items"));
    }
    
    /**
     * Generates behavior tags for all registered traits.
     * 
     * For each trait:
     * 1. Creates a JSON tag file with the trait's tag mapping
     * 2. Includes all items that possess the trait in the values array
     * 3. Sets replace: false to allow datapack merging
     * 4. Writes the tag file to data/tapestry/tags/items/ directory
     * 
     * @return the number of tag files generated
     * @throws IOException if tag file writing fails
     */
    public int generateTags() throws IOException {
        LOGGER.info("Starting behavior tag generation...");
        
        // Ensure output directory exists
        Files.createDirectories(outputDirectory);
        LOGGER.debug("Output directory: {}", outputDirectory.toAbsolutePath());
        
        Map<String, TraitDefinition> allTraits = traitSystem.getAllTraits();
        int generatedCount = 0;
        
        for (TraitDefinition trait : allTraits.values()) {
            Set<String> items = trait.getItems();
            
            // Skip traits with no items
            if (items.isEmpty()) {
                LOGGER.debug("Skipping trait '{}' - no items assigned", trait.getName());
                continue;
            }
            
            // Generate tag JSON
            JsonObject tagJson = createTagJson(items);
            
            // Determine output file path from tag mapping
            Path tagFile = getTagFilePath(trait.getTag());
            
            // Write tag file
            writeTagFile(tagFile, tagJson);
            
            generatedCount++;
            LOGGER.info("Generated tag '{}' with {} items", trait.getTag(), items.size());
        }
        
        LOGGER.info("Behavior tag generation complete: {} tags generated", generatedCount);
        return generatedCount;
    }
    
    /**
     * Creates a Minecraft tag JSON object.
     * 
     * @param items the item identifiers to include in the tag
     * @return the tag JSON object
     */
    private JsonObject createTagJson(Set<String> items) {
        JsonObject tagJson = new JsonObject();
        
        // Set replace: false to allow datapack merging
        tagJson.addProperty("replace", false);
        
        // Add items to values array
        JsonArray valuesArray = new JsonArray();
        items.stream()
            .sorted() // Sort for deterministic output
            .forEach(valuesArray::add);
        
        tagJson.add("values", valuesArray);
        
        return tagJson;
    }
    
    /**
     * Determines the file path for a tag based on its identifier.
     * 
     * Converts tag format "namespace:path/to/tag" to file path
     * "data/namespace/tags/items/path/to/tag.json"
     * 
     * @param tagId the tag identifier (e.g., "tapestry:fish_items")
     * @return the file path for the tag
     */
    private Path getTagFilePath(String tagId) {
        // Parse namespace and path from tag identifier
        String[] parts = tagId.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid tag identifier: " + tagId);
        }
        
        String namespace = parts[0];
        String path = parts[1];
        
        // For tapestry namespace, use the configured output directory
        if ("tapestry".equals(namespace)) {
            return outputDirectory.resolve(path + ".json");
        }
        
        // For other namespaces, construct full path
        Path baseDir = outputDirectory.getParent().getParent().getParent(); // Go up to data/
        return baseDir.resolve(namespace).resolve("tags/items").resolve(path + ".json");
    }
    
    /**
     * Writes a tag JSON object to a file.
     * 
     * @param tagFile the file path
     * @param tagJson the tag JSON object
     * @throws IOException if writing fails
     */
    private void writeTagFile(Path tagFile, JsonObject tagJson) throws IOException {
        // Ensure parent directories exist
        Files.createDirectories(tagFile.getParent());
        
        // Write JSON to file
        String jsonString = GSON.toJson(tagJson);
        Files.writeString(tagFile, jsonString);
        
        LOGGER.debug("Wrote tag file: {}", tagFile.toAbsolutePath());
    }
}
