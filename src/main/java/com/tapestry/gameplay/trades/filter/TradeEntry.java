package com.tapestry.gameplay.trades.filter;

import com.tapestry.gameplay.trades.model.TradeItem;
import net.minecraft.util.Identifier;

/**
 * Represents a single trade entry in a villager's trade table.
 * <p>
 * This interface provides access to trade properties for filtering and modification
 * by the Gameplay Patch Engine. Implementations wrap Minecraft's trade data structures.
 * </p>
 * 
 * <p>Trades support primary and optional secondary inputs, with complete ItemStack
 * information including quantities.</p>
 */
public interface TradeEntry {
    /**
     * Gets the identifier of the item required as input for this trade.
     * 
     * @return The input item identifier
     */
    Identifier getInputItem();
    
    /**
     * Gets the quantity of the primary input item required for this trade.
     * 
     * @return The input item count (default: 1)
     */
    int getInputCount();
    
    /**
     * Gets the primary input as a TradeItem with both item and count.
     * 
     * @return The primary input TradeItem
     */
    TradeItem getBuy();
    
    /**
     * Gets the identifier of the optional secondary input item for this trade.
     * 
     * @return The secondary input item identifier, or null if not present
     */
    Identifier getInputItem2();
    
    /**
     * Gets the quantity of the optional secondary input item for this trade.
     * 
     * @return The secondary input item count (default: 1), or 0 if not present
     */
    int getInputCount2();
    
    /**
     * Gets the optional secondary input as a TradeItem.
     * 
     * @return The secondary input TradeItem, or null if not present
     */
    TradeItem getBuy2();
    
    /**
     * Checks if the input item has the specified tag.
     * 
     * @param tag The tag identifier to check
     * @return true if the input item has the tag, false otherwise
     */
    boolean hasInputTag(String tag);
    
    /**
     * Checks if the secondary input item has the specified tag.
     * 
     * @param tag The tag identifier to check
     * @return true if the secondary input item has the tag, false otherwise
     */
    boolean hasInputTag2(String tag);
    
    /**
     * Gets the identifier of the item provided as output for this trade.
     * 
     * @return The output item identifier
     */
    Identifier getOutputItem();
    
    /**
     * Gets the quantity of the output item provided for this trade.
     * 
     * @return The output item count (default: 1)
     */
    int getOutputCount();
    
    /**
     * Gets the output as a TradeItem with both item and count.
     * 
     * @return The output TradeItem
     */
    TradeItem getSell();
    
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
     * Gets the experience reward gained by the villager for completing this trade.
     * 
     * @return The experience points
     */
    int getExperience();
    
    /**
     * Gets the price multiplier for this trade affecting demand and restock calculations.
     * 
     * @return The price multiplier as a float
     */
    float getPriceMultiplier();
    
    /**
     * Sets the input item for this trade, preserving the input count.
     * 
     * @param inputItem The new input item identifier
     */
    void setInputItem(Identifier inputItem);
    
    /**
     * Sets the input count for this trade.
     * 
     * @param count The new input count
     */
    void setInputCount(int count);
    
    /**
     * Sets the primary input as a complete TradeItem with item and count.
     * 
     * @param buy The new TradeItem for primary input
     */
    void setBuy(TradeItem buy);
    
    /**
     * Sets the optional secondary input item, preserving the secondary input count.
     * 
     * @param inputItem2 The new secondary input item identifier, or null to clear
     */
    void setInputItem2(Identifier inputItem2);
    
    /**
     * Sets the secondary input count for this trade.
     * 
     * @param count The new secondary input count
     */
    void setInputCount2(int count);
    
    /**
     * Sets the optional secondary input as a complete TradeItem.
     * 
     * @param buy2 The new TradeItem for secondary input, or null to clear
     */
    void setBuy2(TradeItem buy2);
    
    /**
     * Sets the output item for this trade, preserving the output count.
     * 
     * @param outputItem The new output item identifier
     */
    void setOutputItem(Identifier outputItem);
    
    /**
     * Sets the output count for this trade.
     * 
     * @param count The new output count
     */
    void setOutputCount(int count);
    
    /**
     * Sets the output as a complete TradeItem with item and count.
     * 
     * @param sell The new TradeItem for output
     */
    void setSell(TradeItem sell);
}
