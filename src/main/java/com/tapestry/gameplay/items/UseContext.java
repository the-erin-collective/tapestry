package com.tapestry.gameplay.items;

/**
 * Context provided to item use handlers.
 * 
 * Contains information about the player, world, item stack, and target.
 */
public class UseContext {
    private final Object player;
    private final Object world;
    private final Object stack;
    private final Object hand;
    private final Object blockPos;
    private final Object entityTarget;
    
    /**
     * Creates a use context.
     * 
     * @param player the player using the item
     * @param world the world
     * @param stack the item stack
     * @param hand the hand used
     * @param blockPos the target block position (optional)
     * @param entityTarget the target entity (optional)
     */
    public UseContext(Object player, Object world, Object stack, Object hand, 
                     Object blockPos, Object entityTarget) {
        this.player = player;
        this.world = world;
        this.stack = stack;
        this.hand = hand;
        this.blockPos = blockPos;
        this.entityTarget = entityTarget;
    }
    
    /**
     * Gets the player.
     * 
     * @return the player
     */
    public Object getPlayer() {
        return player;
    }
    
    /**
     * Gets the world.
     * 
     * @return the world
     */
    public Object getWorld() {
        return world;
    }
    
    /**
     * Gets the item stack.
     * 
     * @return the stack
     */
    public Object getStack() {
        return stack;
    }
    
    /**
     * Gets the hand used.
     * 
     * @return the hand
     */
    public Object getHand() {
        return hand;
    }
    
    /**
     * Gets the target block position.
     * 
     * @return the block position, or null
     */
    public Object getBlockPos() {
        return blockPos;
    }
    
    /**
     * Gets the target entity.
     * 
     * @return the entity, or null
     */
    public Object getEntityTarget() {
        return entityTarget;
    }
}
