package com.tapestry.gameplay.items;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps Fabric Registry.register() for item registration.
 * 
 * Provides a stable abstraction layer over Fabric APIs, translating ItemOptions
 * to Fabric Item instances and handling registration errors gracefully.
 * 
 * This class isolates Fabric API dependencies so that changes to Fabric don't
 * break the public Tapestry API.
 */
public class FabricItemRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricItemRegistry.class);
    
    /**
     * Registers an item with Fabric's registry system.
     * 
     * Translates ItemOptions properties to Fabric Item.Settings:
     * - stackSize → maxCount
     * - durability → maxDamage
     * - food → FoodComponent
     * - recipeRemainder → recraftingRemainder
     * 
     * @param id the item identifier (namespace:path format)
     * @param options the item configuration options
     * @return the registered Fabric Item instance
     * @throws IllegalArgumentException if registration fails
     */
    public static Item registerItem(String id, ItemOptions options) {
        try {
            // Parse the identifier
            Identifier identifier = parseIdentifier(id);
            
            // Create Fabric Item.Settings from ItemOptions
            Item.Settings settings = createItemSettings(options);
            
            // Create the Fabric Item instance
            Item fabricItem = createFabricItem(settings, options);
            
            // Register with Fabric's registry
            Item registeredItem = Registry.register(Registries.ITEM, identifier, fabricItem);
            
            LOGGER.debug("Successfully registered item '{}' with Fabric registry", id);
            
            return registeredItem;
            
        } catch (Exception e) {
            // Translate Fabric errors to descriptive TypeScript-friendly errors
            String errorMessage = String.format(
                "Failed to register item '%s' with Fabric: %s",
                id,
                translateFabricError(e)
            );
            LOGGER.error(errorMessage, e);
            throw new IllegalArgumentException(errorMessage, e);
        }
    }
    
    /**
     * Parses an item identifier string into a Fabric Identifier.
     * 
     * @param id the identifier string (namespace:path format)
     * @return the Fabric Identifier
     * @throws IllegalArgumentException if the identifier format is invalid
     */
    private static Identifier parseIdentifier(String id) {
        try {
            String[] parts = id.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Identifier must contain exactly one colon");
            }
            return Identifier.of(parts[0], parts[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                String.format("Invalid item identifier '%s': must be in namespace:path format", id),
                e
            );
        }
    }
    
    /**
     * Creates Fabric Item.Settings from ItemOptions.
     * 
     * Translates Tapestry properties to Fabric properties:
     * - stackSize → maxCount
     * - durability → maxDamage
     * - recipeRemainder → recraftingRemainder
     * 
     * @param options the item options
     * @return the Fabric Item.Settings
     */
    private static Item.Settings createItemSettings(ItemOptions options) {
        Item.Settings settings = new Item.Settings();
        
        if (options == null) {
            return settings;
        }
        
        // Translate stackSize to maxCount
        if (options.getStackSize() != null) {
            settings.maxCount(options.getStackSize());
        }
        
        // Translate durability to maxDamage
        if (options.getDurability() != null) {
            settings.maxDamage(options.getDurability());
        }
        
        // Translate food component
        if (options.getFood() != null) {
            // Note: Food component translation is complex in Minecraft 1.21+
            // For now, we'll skip this and handle it in a future update
            // The food component API changed significantly in 1.21
            LOGGER.warn("Food component translation not yet implemented for Minecraft 1.21+");
        }
        
        // Translate recipeRemainder to recraftingRemainder
        if (options.getRecipeRemainder() != null) {
            try {
                String[] parts = options.getRecipeRemainder().split(":", 2);
                if (parts.length == 2) {
                    Identifier remainderIdentifier = Identifier.of(parts[0], parts[1]);
                    Item remainderItem = Registries.ITEM.get(remainderIdentifier);
                    if (remainderItem != null) {
                        settings.recipeRemainder(remainderItem);
                    } else {
                        LOGGER.warn("Recipe remainder item '{}' not found in registry", options.getRecipeRemainder());
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to set recipe remainder '{}': {}", options.getRecipeRemainder(), e.getMessage());
            }
        }
        
        return settings;
    }
    
    /**
     * Creates a Fabric Item instance.
     * 
     * For now, creates a basic Item. In the future, this could create
     * specialized item types based on options (e.g., ToolItem, ArmorItem).
     * 
     * @param settings the Fabric Item.Settings
     * @param options the item options (for future use)
     * @return the Fabric Item instance
     */
    private static Item createFabricItem(Item.Settings settings, ItemOptions options) {
        // For now, create a basic Item
        // In the future, we could create specialized items based on options:
        // - If durability is set, could create a ToolItem
        // - If armor properties are added, could create an ArmorItem
        // - etc.
        
        if (options != null && options.getUseHandler() != null) {
            // Create a custom item with use handler
            return new TapestryCustomItem(settings, options.getUseHandler());
        }
        
        return new Item(settings);
    }
    
    /**
     * Translates Fabric exceptions into descriptive error messages.
     * 
     * @param e the exception from Fabric
     * @return a descriptive error message
     */
    private static String translateFabricError(Exception e) {
        String message = e.getMessage();
        
        if (message == null) {
            return "Unknown Fabric error: " + e.getClass().getSimpleName();
        }
        
        // Translate common Fabric errors to user-friendly messages
        if (message.contains("duplicate")) {
            return "Item ID already registered (duplicate registration)";
        }
        
        if (message.contains("invalid") || message.contains("illegal")) {
            return "Invalid item configuration: " + message;
        }
        
        if (message.contains("namespace")) {
            return "Invalid namespace in item ID";
        }
        
        if (message.contains("path")) {
            return "Invalid path in item ID";
        }
        
        // Return the original message if no specific translation applies
        return message;
    }
}
