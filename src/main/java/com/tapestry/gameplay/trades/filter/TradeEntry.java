package com.tapestry.gameplay.trades.filter;

import net.minecraft.util.Identifier;

/**
 * Represents a single trade entry in a villager's trade table.
 * <p>
 * This interface provides access to trade properties for filtering and modification
 * by the Gameplay Patch Engine. Implementations wrap Minecraft's trade data structures.
 * </p>
 */
public interface TradeEntry {
    /**
     * Gets the identifier of the item required as input for this trade.
     * 
     * @return The input item identifier
     */
    Identifier getInputItem();
    
    /**
     * Checks if the input item has the specified tag.
     * 
     * @param tag The tag identifier to check
     * @return true if the input item has the tag, false otherwise
     */
    boolean hasInputTag(String tag);
    
    /**
     * Gets the identifier of the item provided as output for this trade.
     * 
     * @return The output item identifier
     */
    Identifier getOutputItem();
    
    /**
     * Checks if the output item has the specified tag.
     * 
     * @param tag The tag identifier to check
     * @return true if the output item has the tag, false otherwise
     */
    boolean hasOutputTag(String tag);
    
    /**
     * Gets the villager level required for this trade to be available.
     * 
     * @return The required villager level (1-5)
     */
    int getLevel();
    
    /**
     * Gets the maximum number of times this trade can be used before it locks.
     * 
     * @return The maximum uses count
     */
    int getMaxUses();
    
    /**
     * Sets the input item for this trade.
     * 
     * @param inputItem The new input item identifier
     */
    void setInputItem(Identifier inputItem);
    
    /**
     * Sets the output item for this trade.
     * 
     * @param outputItem The new output item identifier
     */
    void setOutputItem(Identifier outputItem);
}
