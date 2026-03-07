package com.tapestry.gameplay.loot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Fabric integration layer for loot table modification.
 * 
 * Hooks into Fabric's LootTableEvents.MODIFY to apply registered modifications
 * during datapack reload events.
 */
public class FabricLootRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricLootRegistry.class);
    private static final Gson GSON = new Gson();
    
    private final com.tapestry.gameplay.loot.LootModifier lootModifier;
    private boolean initialized = false;
    
    /**
     * Creates a Fabric loot registry.
     * 
     * @param lootModifier the loot modifier containing registered modifications
     */
    public FabricLootRegistry(com.tapestry.gameplay.loot.LootModifier lootModifier) {
        this.lootModifier = lootModifier;
    }
    
    /**
     * Initializes the Fabric loot table event hooks.
     * 
     * Should be called during INITIALIZATION phase.
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("FabricLootRegistry already initialized");
            return;
        }
        
        LOGGER.info("Initializing Fabric loot table event hooks");
        
        // Register loot table modification event
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            // Get the loot table identifier
            Identifier tableId = key.getValue();
            String tableIdString = tableId.toString();
            
            // Find matching modifications
            List<LootModification> modifications = lootModifier.getAllModifications();
            for (LootModification modification : modifications) {
                if (modification.getTableId().equals(tableIdString)) {
                    try {
                        // Create a simple wrapper that modifies the builder directly
                        FabricLootTableWrapper wrapper = new FabricLootTableWrapper(tableBuilder);
                        modification.getModifier().accept(wrapper);
                        
                        LOGGER.info("Applied loot modification to table: {}", tableIdString);
                    } catch (Exception e) {
                        LOGGER.error("Failed to apply loot modification to table: {}", tableIdString, e);
                    }
                }
            }
        });
        
        initialized = true;
        LOGGER.info("Fabric loot table event hooks initialized with {} modifications", 
                    lootModifier.getAllModifications().size());
    }
    
    /**
     * Wrapper that modifies Fabric loot table builder directly.
     */
    private static class FabricLootTableWrapper implements com.tapestry.gameplay.loot.LootTable {
        private final LootTable.Builder builder;
        
        public FabricLootTableWrapper(LootTable.Builder builder) {
            this.builder = builder;
        }
        
        @Override
        public void replace(String oldItem, String newItem) {
            if (oldItem == null || oldItem.isEmpty()) {
                throw new IllegalArgumentException("Old item identifier cannot be null or empty");
            }
            
            if (newItem == null || newItem.isEmpty()) {
                throw new IllegalArgumentException("New item identifier cannot be null or empty");
            }
            
            LOGGER.debug("Replacing {} with {} in loot table builder", oldItem, newItem);
            
            try {
                // Build the current table to access its pools
                LootTable table = builder.build();
                
                // Access pools using reflection (Fabric doesn't expose this directly)
                Field poolsField = LootTable.class.getDeclaredField("pools");
                poolsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<LootPool> pools = (List<LootPool>) poolsField.get(table);
                
                // Parse old and new item identifiers
                Identifier oldItemId = Identifier.of(oldItem);
                Identifier newItemId = Identifier.of(newItem);
                
                Item oldItemObj = Registries.ITEM.get(oldItemId);
                Item newItemObj = Registries.ITEM.get(newItemId);
                
                if (newItemObj == Items.AIR) {
                    LOGGER.warn("New item {} not found in registry, skipping replacement", newItem);
                    return;
                }
                
                // Walk through pools and entries to find and replace items
                for (LootPool pool : pools) {
                    replaceInPool(pool, oldItemObj, newItemObj, oldItem, newItem);
                }
                
            } catch (Exception e) {
                LOGGER.error("Failed to replace items in loot table", e);
            }
        }
        
        /**
         * Replaces items in a loot pool using reflection.
         */
        private void replaceInPool(LootPool pool, Item oldItem, Item newItem, 
                                   String oldItemStr, String newItemStr) {
            try {
                // Access entries using reflection
                Field entriesField = LootPool.class.getDeclaredField("entries");
                entriesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<LootPoolEntry> entries = (List<LootPoolEntry>) entriesField.get(pool);
                
                // Walk through entries
                for (LootPoolEntry entry : entries) {
                    replaceInEntry(entry, oldItem, newItem, oldItemStr, newItemStr);
                }
                
            } catch (Exception e) {
                LOGGER.error("Failed to access pool entries", e);
            }
        }
        
        /**
         * Replaces items in a loot entry using reflection.
         */
        private void replaceInEntry(LootPoolEntry entry, Item oldItem, Item newItem,
                                    String oldItemStr, String newItemStr) {
            // Check if this is an ItemEntry
            if (entry instanceof ItemEntry) {
                try {
                    // Access the item field
                    Field itemField = ItemEntry.class.getDeclaredField("item");
                    itemField.setAccessible(true);
                    Item entryItem = (Item) itemField.get(entry);
                    
                    // Replace if it matches
                    if (entryItem == oldItem) {
                        itemField.set(entry, newItem);
                        LOGGER.debug("Replaced item {} with {} in entry", oldItemStr, newItemStr);
                    }
                    
                } catch (Exception e) {
                    LOGGER.error("Failed to access item entry field", e);
                }
            }
            
            // TODO: Handle AlternativeEntry and GroupEntry for nested structures
            // This would require additional reflection to access children arrays
        }
    }
}
