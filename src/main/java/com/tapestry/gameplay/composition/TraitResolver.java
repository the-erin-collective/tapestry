package com.tapestry.gameplay.composition;

import com.tapestry.gameplay.items.ItemDefinition;
import com.tapestry.gameplay.items.ItemRegistration;
import com.tapestry.gameplay.traits.TraitDefinition;
import com.tapestry.gameplay.traits.TraitSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Resolves trait-to-item mappings during COMPOSITION phase.
 * 
 * This resolver collects all registered traits and items, then builds
 * the mappings between traits and the items that possess them.
 */
public class TraitResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraitResolver.class);
    
    private final TraitSystem traitSystem;
    private final ItemRegistration itemRegistration;
    
    /**
     * Creates a new trait resolver.
     * 
     * @param traitSystem the trait system
     * @param itemRegistration the item registration system
     */
    public TraitResolver(TraitSystem traitSystem, ItemRegistration itemRegistration) {
        this.traitSystem = traitSystem;
        this.itemRegistration = itemRegistration;
    }
    
    /**
     * Resolves all trait-to-item mappings.
     * 
     * This method:
     * 1. Collects all registered traits and their tag mappings
     * 2. Collects all item trait assignments from item registry
     * 3. Builds trait-to-item mappings by iterating items and their traits
     * 4. Validates all referenced traits exist
     * 
     * @return resolution result containing mapping statistics
     * @throws IllegalStateException if validation fails
     */
    public ResolutionResult resolve() {
        LOGGER.info("Starting trait-to-item mapping resolution...");
        
        // Step 1: Collect all registered traits
        Map<String, TraitDefinition> allTraits = traitSystem.getAllTraits();
        LOGGER.debug("Collected {} registered traits", allTraits.size());
        
        // Step 2: Collect all registered items
        Map<String, ItemDefinition> allItems = itemRegistration.getAllItems();
        LOGGER.debug("Collected {} registered items", allItems.size());
        
        // Step 3: Build trait-to-item mappings
        int mappingCount = 0;
        Set<String> undefinedTraits = new HashSet<>();
        
        for (ItemDefinition item : allItems.values()) {
            String[] itemTraits = item.getOptions().getTraits();
            
            if (itemTraits == null || itemTraits.length == 0) {
                continue;
            }
            
            for (String traitName : itemTraits) {
                // Step 4: Validate trait exists
                TraitDefinition trait = allTraits.get(traitName);
                
                if (trait == null) {
                    undefinedTraits.add(traitName);
                    LOGGER.error("Item '{}' references undefined trait '{}'", 
                        item.getId(), traitName);
                    continue;
                }
                
                // Add item to trait's item set
                trait.addItem(item.getId());
                mappingCount++;
                
                LOGGER.debug("Mapped item '{}' to trait '{}'", item.getId(), traitName);
            }
        }
        
        // Validate no undefined traits were referenced
        if (!undefinedTraits.isEmpty()) {
            List<String> validTraits = new ArrayList<>(allTraits.keySet());
            Collections.sort(validTraits);
            
            throw new IllegalStateException(
                "Items reference undefined traits: " + undefinedTraits + ". " +
                "Valid traits are: " + validTraits
            );
        }
        
        LOGGER.info("Trait resolution complete: {} traits, {} items, {} mappings",
            allTraits.size(), allItems.size(), mappingCount);
        
        return new ResolutionResult(allTraits.size(), allItems.size(), mappingCount);
    }
    
    /**
     * Result of trait resolution.
     */
    public static class ResolutionResult {
        private final int traitCount;
        private final int itemCount;
        private final int mappingCount;
        
        public ResolutionResult(int traitCount, int itemCount, int mappingCount) {
            this.traitCount = traitCount;
            this.itemCount = itemCount;
            this.mappingCount = mappingCount;
        }
        
        public int getTraitCount() {
            return traitCount;
        }
        
        public int getItemCount() {
            return itemCount;
        }
        
        public int getMappingCount() {
            return mappingCount;
        }
    }
}
