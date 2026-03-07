package com.tapestry.gameplay.items;

/**
 * Validation utilities for item properties.
 * 
 * Provides reusable validation functions for stack size, durability, food values, and item IDs.
 */
public class ItemValidation {
    
    /**
     * Validates stack size is within valid range (1-64).
     * 
     * @param stackSize the stack size to validate
     * @throws IllegalArgumentException if stack size is invalid
     */
    public static void validateStackSize(int stackSize) {
        if (stackSize < 1 || stackSize > 64) {
            throw new IllegalArgumentException(
                "Stack size must be between 1 and 64, got: " + stackSize
            );
        }
    }
    
    /**
     * Validates durability is non-negative.
     * 
     * @param durability the durability to validate
     * @throws IllegalArgumentException if durability is invalid
     */
    public static void validateDurability(int durability) {
        if (durability < 0) {
            throw new IllegalArgumentException(
                "Durability must be >= 0, got: " + durability
            );
        }
    }
    
    /**
     * Validates food component values are within valid ranges.
     * 
     * @param food the food component to validate
     * @throws IllegalArgumentException if food values are invalid
     */
    public static void validateFood(FoodComponent food) {
        if (food == null) {
            return;
        }
        
        if (food.getHunger() < 0 || food.getHunger() > 20) {
            throw new IllegalArgumentException(
                "Food hunger must be between 0 and 20, got: " + food.getHunger()
            );
        }
        
        if (food.getSaturation() < 0.0f || food.getSaturation() > 1.0f) {
            throw new IllegalArgumentException(
                "Food saturation must be between 0.0 and 1.0, got: " + food.getSaturation()
            );
        }
    }
    
    /**
     * Validates item identifier format (namespace:path).
     * 
     * @param id the item identifier to validate
     * @throws IllegalArgumentException if format is invalid
     */
    public static void validateItemId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty");
        }
        
        if (!id.matches("^[a-z0-9_.-]+:[a-z0-9_.-]+(/[a-z0-9_.-]+)*$")) {
            throw new IllegalArgumentException(
                "Invalid item ID format: '" + id + "'. Must follow namespace:path format."
            );
        }
    }
    
    /**
     * Validates item ID uniqueness against existing registrations.
     * 
     * @param id the item identifier to check
     * @param existingIds set of already registered item IDs
     * @throws IllegalArgumentException if ID is already registered
     */
    public static void validateItemIdUniqueness(String id, java.util.Set<String> existingIds) {
        if (existingIds.contains(id)) {
            throw new IllegalArgumentException(
                "Item '" + id + "' is already registered. Duplicate item IDs are not allowed."
            );
        }
    }
    
    /**
     * Validates trait references against registered traits.
     * 
     * @param traits the trait identifiers to validate
     * @param traitSystem the trait system to check against
     * @throws IllegalArgumentException if any trait is not registered
     */
    public static void validateTraits(String[] traits, com.tapestry.gameplay.traits.TraitSystem traitSystem) {
        if (traits == null || traits.length == 0) {
            return;
        }
        
        for (String trait : traits) {
            if (trait == null || trait.isEmpty()) {
                throw new IllegalArgumentException("Trait identifier cannot be null or empty");
            }
            
            if (traitSystem.getTrait(trait) == null) {
                // Build list of valid traits for error message
                java.util.Set<String> validTraits = traitSystem.getAllTraits().keySet();
                String validTraitsList = validTraits.isEmpty() 
                    ? "none" 
                    : String.join(", ", validTraits);
                
                throw new IllegalArgumentException(
                    String.format(
                        "Undefined trait '%s'. Item references trait that has not been registered. " +
                        "Valid traits: %s",
                        trait, validTraitsList
                    )
                );
            }
        }
    }
    
    /**
     * Validates all item options.
     * 
     * @param options the options to validate
     * @throws IllegalArgumentException if any validation fails
     */
    public static void validateItemOptions(ItemOptions options) {
        if (options == null) {
            return;
        }
        
        if (options.getStackSize() != null) {
            validateStackSize(options.getStackSize());
        }
        
        if (options.getDurability() != null) {
            validateDurability(options.getDurability());
        }
        
        if (options.getFood() != null) {
            validateFood(options.getFood());
        }
    }
}
