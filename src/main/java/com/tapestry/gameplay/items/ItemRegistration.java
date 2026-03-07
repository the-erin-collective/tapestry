package com.tapestry.gameplay.items;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides stable TypeScript API for registering custom items with trait support.
 * 
 * Items can be registered with properties like stack size, durability, food values,
 * and gameplay traits. All registration must occur during TS_REGISTER phase.
 */
public class ItemRegistration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemRegistration.class);
    
    private final Map<String, ItemDefinition> items = new ConcurrentHashMap<>();
    private com.tapestry.gameplay.traits.TraitSystem traitSystem;
    
    /**
     * Creates a new item registration instance.
     */
    public ItemRegistration() {
    }
    
    /**
     * Sets the trait system for trait validation.
     * This should be called during initialization before any items are registered.
     * 
     * @param traitSystem the trait system
     */
    public void setTraitSystem(com.tapestry.gameplay.traits.TraitSystem traitSystem) {
        this.traitSystem = traitSystem;
    }
    
    /**
     * Registers a new item.
     * 
     * @param id the unique item identifier (namespace:path format)
     * @param options item configuration options
     * @throws IllegalStateException if called outside TS_REGISTER phase
     * @throws IllegalArgumentException if validation fails
     */
    public void register(String id, ItemOptions options) {
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_REGISTER);
        
        ItemValidation.validateItemId(id);
        ItemValidation.validateItemIdUniqueness(id, items.keySet());
        ItemValidation.validateItemOptions(options);
        
        // Validate trait references if traits are specified AND TraitSystem is available
        // If TraitSystem is not set, trait validation will happen during COMPOSITION phase
        if (options != null && options.getTraits() != null && options.getTraits().length > 0 && traitSystem != null) {
            ItemValidation.validateTraits(options.getTraits(), traitSystem);
        }
        
        ItemDefinition definition = new ItemDefinition(id, options);
        items.put(id, definition);
        
        LOGGER.info("Registered item '{}' with {} traits", id, 
            options != null && options.getTraits() != null ? options.getTraits().length : 0);
    }
    
    /**
     * Gets an item definition by ID.
     * 
     * @param id the item identifier
     * @return the item definition, or null if not found
     */
    public ItemDefinition getItem(String id) {
        return items.get(id);
    }
    
    /**
     * Gets all registered items.
     * 
     * @return unmodifiable map of item definitions
     */
    public Map<String, ItemDefinition> getAllItems() {
        return Map.copyOf(items);
    }
    
    /**
     * Performs Fabric registration for all registered items.
     * 
     * This should be called during COMPOSITION or INITIALIZATION phase after
     * all items have been registered during TS_REGISTER phase.
     * 
     * Translates ItemOptions to Fabric Item instances and registers them
     * with Fabric's registry system.
     * 
     * @throws IllegalArgumentException if Fabric registration fails
     */
    public void performFabricRegistration() {
        LOGGER.info("Starting Fabric registration for {} items", items.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (Map.Entry<String, ItemDefinition> entry : items.entrySet()) {
            String id = entry.getKey();
            ItemDefinition definition = entry.getValue();
            
            try {
                // Register with Fabric
                Object fabricItem = FabricItemRegistry.registerItem(id, definition.getOptions());
                
                // Store the Fabric item reference in the definition
                definition.setFabricItem(fabricItem);
                
                successCount++;
                LOGGER.debug("Registered item '{}' with Fabric", id);
                
            } catch (Exception e) {
                failureCount++;
                LOGGER.error("Failed to register item '{}' with Fabric: {}", id, e.getMessage(), e);
                
                // Re-throw to fail fast
                throw new IllegalArgumentException(
                    String.format("Failed to register item '%s' with Fabric: %s", id, e.getMessage()),
                    e
                );
            }
        }
        
        LOGGER.info("Fabric registration complete: {} succeeded, {} failed", successCount, failureCount);
    }
}
