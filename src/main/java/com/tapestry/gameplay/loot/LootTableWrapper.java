package com.tapestry.gameplay.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for loot table modification with AST visitor pattern.
 * 
 * Parses loot table JSON structure and provides methods to walk and modify
 * the loot table AST recursively, handling nested structures like alternatives,
 * groups, and conditions.
 */
public class LootTableWrapper implements LootTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(LootTableWrapper.class);
    
    private final JsonObject ast;
    
    /**
     * Creates a loot table wrapper from a JSON object.
     * 
     * @param ast the loot table JSON structure
     */
    public LootTableWrapper(JsonObject ast) {
        if (ast == null) {
            throw new IllegalArgumentException("Loot table AST cannot be null");
        }
        this.ast = ast;
    }
    
    /**
     * Gets the underlying JSON AST.
     * 
     * @return the loot table JSON structure
     */
    public JsonObject getAst() {
        return ast;
    }
    
    @Override
    public void replace(String oldItem, String newItem) {
        if (oldItem == null || oldItem.isEmpty()) {
            throw new IllegalArgumentException("Old item identifier cannot be null or empty");
        }
        
        if (newItem == null || newItem.isEmpty()) {
            throw new IllegalArgumentException("New item identifier cannot be null or empty");
        }
        
        LOGGER.debug("Replacing {} with {} in loot table", oldItem, newItem);
        
        // Walk the loot table AST recursively
        visitLootTable(ast, oldItem, newItem);
    }
    
    /**
     * Visits the root loot table structure.
     * 
     * @param table the loot table JSON object
     * @param oldItem the item to replace
     * @param newItem the replacement item
     */
    private void visitLootTable(JsonObject table, String oldItem, String newItem) {
        // Visit pools array
        if (table.has("pools") && table.get("pools").isJsonArray()) {
            JsonArray pools = table.getAsJsonArray("pools");
            for (JsonElement poolElement : pools) {
                if (poolElement.isJsonObject()) {
                    visitPool(poolElement.getAsJsonObject(), oldItem, newItem);
                }
            }
        }
    }
    
    /**
     * Visits a loot pool.
     * 
     * @param pool the pool JSON object
     * @param oldItem the item to replace
     * @param newItem the replacement item
     */
    private void visitPool(JsonObject pool, String oldItem, String newItem) {
        // Visit entries array
        if (pool.has("entries") && pool.get("entries").isJsonArray()) {
            JsonArray entries = pool.getAsJsonArray("entries");
            for (JsonElement entryElement : entries) {
                if (entryElement.isJsonObject()) {
                    visitEntry(entryElement.getAsJsonObject(), oldItem, newItem);
                }
            }
        }
    }
    
    /**
     * Visits a loot entry.
     * 
     * Handles different entry types:
     * - item: direct item entry
     * - alternatives: array of alternative entries
     * - group: array of grouped entries
     * - tag: item tag entry (not modified)
     * 
     * @param entry the entry JSON object
     * @param oldItem the item to replace
     * @param newItem the replacement item
     */
    private void visitEntry(JsonObject entry, String oldItem, String newItem) {
        // Get entry type
        String type = entry.has("type") ? entry.get("type").getAsString() : "";
        
        switch (type) {
            case "minecraft:item":
                // Direct item entry - check and replace name
                if (entry.has("name")) {
                    String itemName = entry.get("name").getAsString();
                    if (itemName.equals(oldItem)) {
                        entry.addProperty("name", newItem);
                        LOGGER.debug("Replaced item {} with {} in entry", oldItem, newItem);
                    }
                }
                break;
                
            case "minecraft:alternatives":
                // Alternatives - visit children array
                if (entry.has("children") && entry.get("children").isJsonArray()) {
                    JsonArray children = entry.getAsJsonArray("children");
                    for (JsonElement childElement : children) {
                        if (childElement.isJsonObject()) {
                            visitEntry(childElement.getAsJsonObject(), oldItem, newItem);
                        }
                    }
                }
                break;
                
            case "minecraft:group":
                // Group - visit children array
                if (entry.has("children") && entry.get("children").isJsonArray()) {
                    JsonArray children = entry.getAsJsonArray("children");
                    for (JsonElement childElement : children) {
                        if (childElement.isJsonObject()) {
                            visitEntry(childElement.getAsJsonObject(), oldItem, newItem);
                        }
                    }
                }
                break;
                
            case "minecraft:tag":
                // Tag entry - don't modify tags, only direct items
                LOGGER.debug("Skipping tag entry: {}", entry.get("name"));
                break;
                
            default:
                // Unknown type - log warning but don't fail
                LOGGER.warn("Unknown loot entry type: {}", type);
                break;
        }
        
        // Note: Conditions are preserved automatically since we only modify
        // the "name" property of item entries, leaving all other properties intact
    }
}
