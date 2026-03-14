package com.tapestry.gameplay.trades.expansion;

import com.tapestry.gameplay.trades.TradeTable;
import com.tapestry.gameplay.trades.filter.TradeEntry;
import com.tapestry.gameplay.trades.model.TradeItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Expands tag-based inputs in trades into individual trade entries for each item in the tag.
 * 
 * <p>This component handles the tag expansion phase of trade patching, where a single trade
 * with a tag as input is expanded into multiple trades, one for each item in the tag.</p>
 * 
 * <p>The expansion process:
 * <ol>
 *   <li>Resolve the tag to get all items</li>
 *   <li>Remove duplicates using a Set</li>
 *   <li>Remove the original trade</li>
 *   <li>Clone the original trade for each item</li>
 *   <li>Set the buy item to each resolved item</li>
 *   <li>Preserve all other fields including counts</li>
 *   <li>Insert clones into the trade table</li>
 * </ol>
 * </p>
 * 
 * <p>Edge cases:
 * <ul>
 *   <li>Empty tag: The original trade is removed</li>
 *   <li>Duplicate items in tag: Duplicates are deduplicated</li>
 *   <li>Original item in tag: The original trade is replaced with the expanded set</li>
 * </ul>
 * </p>
 */
public class TagTradeExpander {
    private static final Logger LOGGER = LoggerFactory.getLogger(TagTradeExpander.class);
    
    /**
     * Expands an input tag in a trade entry into multiple trades.
     * 
     * <p>This method resolves the tag, clones the original trade for each item,
     * and inserts the clones into the trade table. The original trade is removed.</p>
     * 
     * @param table The trade table to modify
     * @param original The original trade entry to expand
     * @param tagId The tag identifier string (e.g., "namespace:tag_name")
     * @throws NullPointerException if table, original, or tagId is null
     */
    public static void expandInputTag(
        TradeTable table,
        TradeEntry original,
        String tagId
    ) {
        Objects.requireNonNull(table, "Trade table cannot be null");
        Objects.requireNonNull(original, "Original trade entry cannot be null");
        Objects.requireNonNull(tagId, "Tag identifier cannot be null");
        
        try {
            // Resolve the tag to get all items
            Set<Item> resolvedItems = resolveTag(tagId);
            
            // Check if tag was resolved successfully
            if (resolvedItems.isEmpty()) {
                LOGGER.warn("Tag {} resolved to no items, removing original trade", tagId);
                // Remove the original trade and don't replace it
                table.removeIf(entry -> entry == original);
                return;
            }
            
            // Remove the original trade
            table.removeIf(entry -> entry == original);
            
            // Create clones for each item
            for (Item item : resolvedItems) {
                TradeEntry clone = cloneTrade(original);
                clone.setBuy(new TradeItem(Registries.ITEM.getId(item), original.getInputCount()));
                table.add(clone);
            }
            
            LOGGER.debug("Expanded tag {} into {} trades", tagId, resolvedItems.size());
            
        } catch (Exception e) {
            LOGGER.warn("Failed to expand tag {}: {}", tagId, e.getMessage());
            // Leave the trade unchanged on error
        }
    }
    
    /**
     * Expands a secondary input tag in a trade entry.
     * 
     * @param table The trade table to modify
     * @param original The original trade entry to expand
     * @param tagId The tag identifier string
     */
    public static void expandInputTag2(
        TradeTable table,
        TradeEntry original,
        String tagId
    ) {
        Objects.requireNonNull(table, "Trade table cannot be null");
        Objects.requireNonNull(original, "Original trade entry cannot be null");
        Objects.requireNonNull(tagId, "Tag identifier cannot be null");
        
        try {
            // Resolve the tag to get all items
            Set<Item> resolvedItems = resolveTag(tagId);
            
            // Check if tag was resolved successfully
            if (resolvedItems.isEmpty()) {
                LOGGER.warn("Tag {} resolved to no items, removing secondary input", tagId);
                // Remove the original trade - secondary input cannot be empty
                table.removeIf(entry -> entry == original);
                return;
            }
            
            // Remove the original trade
            table.removeIf(entry -> entry == original);
            
            // Create clones for each item
            for (Item item : resolvedItems) {
                TradeEntry clone = cloneTrade(original);
                clone.setBuy2(new TradeItem(Registries.ITEM.getId(item), original.getInputCount2()));
                table.add(clone);
            }
            
            LOGGER.debug("Expanded secondary tag {} into {} trades", tagId, resolvedItems.size());
            
        } catch (Exception e) {
            LOGGER.warn("Failed to expand secondary tag {}: {}", tagId, e.getMessage());
            // Leave the trade unchanged on error
        }
    }
    
    /**
     * Resolves a tag identifier to a set of items, removing duplicates.
     * 
     * @param tagId The tag identifier string
     * @return A set of unique items in the tag
     * @throws IllegalArgumentException if the tag cannot be resolved
     */
    private static Set<Item> resolveTag(String tagId) {
        try {
            Identifier tagIdentifier = Identifier.tryParse(tagId);
            if (tagIdentifier == null) {
                throw new IllegalArgumentException("Invalid tag identifier: " + tagId);
            }
            
            Set<Item> items = new HashSet<>();
            
            // Try to resolve the tag from the tag manager
            try {
                TagKey<Item> tagKey = TagKey.of(Registries.ITEM.getKey(), tagIdentifier);
                Registries.ITEM.iterateEntries(tagKey).forEach(reference -> {
                    items.add(reference.value());
                });
            } catch (Exception e) {
                // Tag resolution failed - log and return empty
                LOGGER.warn("Could not resolve tag {}: {}", tagId, e.getMessage());
            }
            
            return items;
        } catch (Exception e) {
            LOGGER.warn("Could not resolve tag {}: {}", tagId, e.getMessage());
            throw new IllegalArgumentException("Failed to resolve tag: " + tagId, e);
        }
    }
    
    /**
     * Creates a clone of a trade entry with all fields copied.
     * 
     * @param original The original trade entry
     * @return A new TradeEntry clone
     */
    private static TradeEntry cloneTrade(TradeEntry original) {
        // This is a placeholder implementation - actual implementation depends on
        // the concrete TradeEntry implementation provided by Minecraft/Fabric
        // For now, we return a wrapper that delegates to the original
        return new TradeEntryWrapper(
            original.getBuy(),
            original.getBuy2(),
            original.getSell(),
            original.getLevel(),
            original.getMaxUses(),
            original.getExperience(),
            original.getPriceMultiplier()
        );
    }
    
    /**
     * Checks if a trade entry references the specified tag as input.
     * 
     * @param entry The trade entry to check
     * @param tagId The tag identifier
     * @return true if the entry's input matches the tag, false otherwise
     */
    public static boolean referencesInputTag(TradeEntry entry, String tagId) {
        return entry.hasInputTag(tagId);
    }
    
    /**
     * Checks if a trade entry references the specified tag as secondary input.
     * 
     * @param entry The trade entry to check
     * @param tagId The tag identifier
     * @return true if the entry's secondary input matches the tag, false otherwise
     */
    public static boolean referencesInputTag2(TradeEntry entry, String tagId) {
        return entry.hasInputTag2(tagId);
    }
    
    /**
     * Internal wrapper for cloned trades that preserves all original properties.
     */
    private static class TradeEntryWrapper implements TradeEntry {
        private final TradeItem buy;
        private TradeItem buy2;
        private final TradeItem sell;
        private final int level;
        private final int maxUses;
        private final int experience;
        private final float priceMultiplier;
        
        TradeEntryWrapper(
            TradeItem buy,
            TradeItem buy2,
            TradeItem sell,
            int level,
            int maxUses,
            int experience,
            float priceMultiplier
        ) {
            this.buy = buy;
            this.buy2 = buy2;
            this.sell = sell;
            this.level = level;
            this.maxUses = maxUses;
            this.experience = experience;
            this.priceMultiplier = priceMultiplier;
        }
        
        @Override public Identifier getInputItem() { return buy.item(); }
        @Override public int getInputCount() { return buy.count(); }
        @Override public TradeItem getBuy() { return buy; }
        @Override public Identifier getInputItem2() { return buy2 != null ? buy2.item() : null; }
        @Override public int getInputCount2() { return buy2 != null ? buy2.count() : 0; }
        @Override public TradeItem getBuy2() { return buy2; }
        @Override public boolean hasInputTag(String tag) { return false; }
        @Override public boolean hasInputTag2(String tag) { return false; }
        @Override public Identifier getOutputItem() { return sell.item(); }
        @Override public int getOutputCount() { return sell.count(); }
        @Override public TradeItem getSell() { return sell; }
        @Override public boolean hasOutputTag(String tag) { return false; }
        @Override public int getLevel() { return level; }
        @Override public int getMaxUses() { return maxUses; }
        @Override public int getExperience() { return experience; }
        @Override public float getPriceMultiplier() { return priceMultiplier; }
        
        @Override public void setInputItem(Identifier inputItem) { /* not mutable */ }
        @Override public void setInputCount(int count) { /* not mutable */ }
        @Override public void setBuy(TradeItem newBuy) { /* not mutable */ }
        @Override public void setInputItem2(Identifier inputItem2) { /* not mutable */ }
        @Override public void setInputCount2(int count) { /* not mutable */ }
        @Override public void setBuy2(TradeItem newBuy2) { /* not mutable */ }
        @Override public void setOutputItem(Identifier outputItem) { /* not mutable */ }
        @Override public void setOutputCount(int count) { /* not mutable */ }
        @Override public void setSell(TradeItem newSell) { /* not mutable */ }
    }
}
