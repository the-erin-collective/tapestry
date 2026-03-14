package com.tapestry.gameplay.trades.model;

import net.minecraft.util.Identifier;

import java.util.Objects;

/**
 * Represents an item with a quantity in a trade.
 * 
 * <p>This model encapsulates an item identifier and its count, providing
 * a complete representation of an ItemStack in the trade system.</p>
 * 
 * <p>Trade items have a default count of 1 if not explicitly specified.</p>
 * 
 * @param item The item identifier
 * @param count The quantity of the item (default: 1)
 */
public record TradeItem(Identifier item, int count) {
    
    /**
     * Creates a new TradeItem with the specified item and quantity.
     * 
     * @param item The item identifier (cannot be null)
     * @param count The quantity (must be positive)
     * @throws NullPointerException if item is null
     * @throws IllegalArgumentException if count is less than 1
     */
    public TradeItem {
        Objects.requireNonNull(item, "Item identifier cannot be null");
        if (count < 1) {
            throw new IllegalArgumentException("Item count must be at least 1, got " + count);
        }
    }
    
    /**
     * Creates a new TradeItem with the specified item and a count of 1.
     * 
     * @param item The item identifier
     * @return A TradeItem with count 1
     */
    public static TradeItem of(Identifier item) {
        return new TradeItem(item, 1);
    }
    
    /**
     * Creates a new TradeItem with the specified item and count.
     * 
     * @param item The item identifier
     * @param count The quantity
     * @return A TradeItem with the specified count
     */
    public static TradeItem of(Identifier item, int count) {
        return new TradeItem(item, count);
    }
    
    /**
     * Creates a copy of this TradeItem with a new item identifier.
     * The count is preserved.
     * 
     * @param newItem The new item identifier
     * @return A new TradeItem with the new item and same count
     */
    public TradeItem withItem(Identifier newItem) {
        return new TradeItem(newItem, count);
    }
    
    /**
     * Creates a copy of this TradeItem with a new count.
     * The item is preserved.
     * 
     * @param newCount The new count
     * @return A new TradeItem with the same item and new count
     */
    public TradeItem withCount(int newCount) {
        return new TradeItem(item, newCount);
    }
}
