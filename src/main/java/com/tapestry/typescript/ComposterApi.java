package com.tapestry.typescript;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.block.ComposterBlock;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * TypeScript API for composter registration.
 * 
 * Provides tapestry.composting namespace for registering items
 * as compostable with specified probability.
 */
public class ComposterApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ComposterApi.class);
    
    // Minecraft identifier format: namespace:path
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_/.-]+$");
    
    // Store registrations for batch processing
    private final List<ComposterEntry> registrations = new ArrayList<>();
    // Cache item lookups to avoid repeated registry hits
    private final Map<String, Item> itemCache = new HashMap<>();
    
    /**
     * Represents a composter registration entry.
     */
    public static class ComposterEntry {
        public final String itemId;
        public final double chance;
        
        public ComposterEntry(String itemId, double chance) {
            this.itemId = itemId;
            this.chance = chance;
        }
    }
    
    /**
     * Creates the composting namespace for TypeScript.
     * 
     * @return ProxyObject containing composter APIs
     */
    public ProxyObject createNamespace() {
        Map<String, Object> composting = new HashMap<>();
        
        composting.put("register", createRegisterFunction());
        composting.put("registerBatch", createRegisterBatchFunction());
        
        return ProxyObject.fromMap(composting);
    }
    
    /**
     * Creates the register function.
     */
    private ProxyExecutable createRegisterFunction() {
        return args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException("composting.register requires exactly 2 arguments: (itemId, chance)");
            }
            
            String itemId = args[0].asString();
            double chance = args[1].asDouble();
            
            // Validate identifier format
            if (itemId == null || itemId.isBlank()) {
                throw new IllegalArgumentException("itemId must be a non-empty string");
            }
            if (!IDENTIFIER_PATTERN.matcher(itemId).matches()) {
                throw new IllegalArgumentException(
                    String.format("Invalid item identifier '%s'. Must follow format 'namespace:path'", itemId)
                );
            }
            
            // Validate chance range
            if (chance < 0.0 || chance > 1.0) {
                throw new IllegalArgumentException(
                    String.format("Chance must be between 0.0 and 1.0, got %f", chance)
                );
            }
            
            // Register the composter entry
            registerCompostable(itemId, (float) chance);
            
            LOGGER.debug("Registered compostable item: {} with chance: {}", itemId, chance);
            
            return null;
        };
    }
    
    /**
     * Creates the registerBatch function.
     */
    private ProxyExecutable createRegisterBatchFunction() {
        return args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("composting.registerBatch requires exactly 1 argument: (items)");
            }
            
            Value itemsArray = args[0];
            
            // Validate it's an array
            if (!itemsArray.hasArrayElements()) {
                throw new IllegalArgumentException("registerBatch argument must be an array");
            }
            
            long length = itemsArray.getArraySize();
            if (length > 1000) {
                throw new IllegalArgumentException(
                    String.format("Batch size %d exceeds maximum of 1000", length)
                );
            }
            
            // Process each entry
            for (long i = 0; i < length; i++) {
                Value entry = itemsArray.getArrayElement(i);
                
                if (!entry.hasMembers()) {
                    throw new IllegalArgumentException(
                        String.format("Batch entry at index %d is not an object", i)
                    );
                }
                
                String itemId = null;
                double chance = 0.0;
                
                // Extract itemId
                if (entry.hasMember("itemId")) {
                    itemId = entry.getMember("itemId").asString();
                } else {
                    throw new IllegalArgumentException(
                        String.format("Batch entry at index %d missing required 'itemId' property", i)
                    );
                }
                
                // Extract chance
                if (entry.hasMember("chance")) {
                    chance = entry.getMember("chance").asDouble();
                } else {
                    throw new IllegalArgumentException(
                        String.format("Batch entry at index %d missing required 'chance' property", i)
                    );
                }
                
                // Validate identifier format
                if (itemId == null || itemId.isBlank()) {
                    throw new IllegalArgumentException(
                        String.format("Batch entry at index %d has invalid itemId", i)
                    );
                }
                if (!IDENTIFIER_PATTERN.matcher(itemId).matches()) {
                    throw new IllegalArgumentException(
                        String.format("Batch entry at index %d has invalid identifier '%s'", i, itemId)
                    );
                }
                
                // Validate chance range
                if (chance < 0.0 || chance > 1.0) {
                    throw new IllegalArgumentException(
                        String.format("Batch entry at index %d has invalid chance %f", i, chance)
                    );
                }
                
                // Register the composter entry
                registerCompostable(itemId, (float) chance);
            }
            
            LOGGER.info("Registered {} compostable items in batch", length);
            
            return null;
        };
    }
    
    /**
     * Internal method to register a compostable item.
     * 
     * This method stores the registration for processing during
     * the appropriate initialization phase.
     * 
     * @param itemId the item identifier
     * @param chance the composting probability (0.0-1.0)
     */
    private synchronized void registerCompostable(String itemId, float chance) {
        registrations.add(new ComposterEntry(itemId, chance));
        LOGGER.debug("Queued compostable registration: {} -> {}", itemId, chance);
    }
    
    /**
     * Gets all queued registrations (for server-side processing).
     * 
     * @return list of registration entries
     */
    public List<ComposterEntry> getRegistrations() {
        return new ArrayList<>(registrations);
    }
    
    /**
     * Applies all queued compostable registrations to the composter registry.
     * This should be called during Tapestry initialization after item registries are loaded.
     */
    public void applyRegistrations() {
        for (ComposterEntry entry : registrations) {
            Item item = itemCache.computeIfAbsent(entry.itemId, key -> {
                Identifier id = Identifier.tryParse(key);
                Item i = Registries.ITEM.get(id);
                if (i == Items.AIR) {
                    LOGGER.warn("Cannot register compostable - item not found: {}", id);
                }
                return i;
            });
            
            if (item == Items.AIR) {
                // skip invalid
                continue;
            }
            
            ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(item, (float) entry.chance);
            LOGGER.info("Registered compostable {} -> {}", entry.itemId, entry.chance);
        }
        
        registrations.clear();
    }
}
