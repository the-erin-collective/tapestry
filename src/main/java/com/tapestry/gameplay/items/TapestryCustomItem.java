package com.tapestry.gameplay.items;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Fabric Item implementation that supports Tapestry use handlers.
 * 
 * This class extends Fabric's Item class to provide custom use behavior
 * defined by mod developers through the UseHandler interface.
 * 
 * Requirements:
 * - 4.1: Provides UseContext with player, world, stack, hand, blockPos, entityTarget
 * - 4.2: Handles item replacement when use function returns item identifier
 * - 4.3: Catches and logs errors from use function, prevents item use on error
 * - 4.4: Invokes use functions only during RUNTIME phase
 */
public class TapestryCustomItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryCustomItem.class);
    private final UseHandler useHandler;
    
    /**
     * Creates a new custom item with a use handler.
     * 
     * @param settings the Fabric item settings
     * @param useHandler the custom use behavior handler
     */
    public TapestryCustomItem(Settings settings, UseHandler useHandler) {
        super(settings);
        this.useHandler = useHandler;
    }
    
    /**
     * Called when the item is used (right-clicked).
     * 
     * Invokes the custom use handler during RUNTIME phase only and processes the result.
     * Handles item replacement and error conditions according to requirements.
     * 
     * @param world the world
     * @param user the player using the item
     * @param hand the hand holding the item
     * @return the action result
     */
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        // Requirement 4.4: Invoke use functions only during RUNTIME phase
        try {
            PhaseController.getInstance().requirePhase(TapestryPhase.RUNTIME);
        } catch (IllegalStateException e) {
            LOGGER.error("Item use handler called outside RUNTIME phase: {}", e.getMessage());
            return ActionResult.FAIL;
        }
        
        ItemStack stack = user.getStackInHand(hand);
        
        try {
            // Requirement 4.1: Create UseContext with player, world, stack, hand, blockPos, entityTarget
            // blockPos and entityTarget are null for basic item use (not targeting a block or entity)
            UseContext context = new UseContext(user, world, stack, hand, null, null);
            
            // Invoke the use handler
            UseResult result = useHandler.use(context);
            
            // Process the result
            if (result != null && result.isSuccess()) {
                // Requirement 4.2: Handle item replacement when use function returns item identifier
                if (result.getItem() != null) {
                    handleItemReplacement(user, hand, stack, result.getItem());
                }
                
                return ActionResult.SUCCESS;
            } else {
                return ActionResult.PASS;
            }
            
        } catch (Exception e) {
            // Requirement 4.3: Catch and log errors from use function, prevent item use on error
            LOGGER.error("Error in custom item use handler for item '{}': {}", 
                Registries.ITEM.getId(this), e.getMessage(), e);
            return ActionResult.FAIL;
        }
    }
    
    /**
     * Handles item replacement after successful use.
     * 
     * Replaces the used item with the specified replacement item.
     * If the replacement item doesn't exist, logs a warning and keeps the original item.
     * 
     * @param player the player using the item
     * @param hand the hand holding the item
     * @param originalStack the original item stack
     * @param replacementId the replacement item identifier (namespace:path format)
     */
    private void handleItemReplacement(PlayerEntity player, Hand hand, ItemStack originalStack, String replacementId) {
        try {
            // Parse the replacement item identifier
            String[] parts = replacementId.split(":", 2);
            if (parts.length != 2) {
                LOGGER.warn("Invalid replacement item identifier '{}': must be in namespace:path format", replacementId);
                return;
            }
            
            Identifier identifier = Identifier.of(parts[0], parts[1]);
            
            // Check if the item exists in the registry
            if (!Registries.ITEM.containsId(identifier)) {
                LOGGER.warn("Replacement item '{}' not found in registry", replacementId);
                return;
            }
            
            Item replacementItem = Registries.ITEM.get(identifier);
            
            // Decrement the original stack
            originalStack.decrement(1);
            
            // If the stack is now empty, replace it with the new item
            if (originalStack.isEmpty()) {
                player.setStackInHand(hand, new ItemStack(replacementItem));
            } else {
                // If the stack still has items, try to add the replacement to inventory
                ItemStack replacementStack = new ItemStack(replacementItem);
                if (!player.getInventory().insertStack(replacementStack)) {
                    // If inventory is full, drop the item
                    player.dropItem(replacementStack, false);
                }
            }
            
            LOGGER.debug("Replaced item with '{}'", replacementId);
            
        } catch (Exception e) {
            LOGGER.error("Failed to replace item with '{}': {}", replacementId, e.getMessage(), e);
        }
    }
}
