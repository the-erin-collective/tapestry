package com.tapestry.gameplay.trades.model;

import com.tapestry.gameplay.trades.filter.TradeEntry;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A concrete implementation of TradeEntry that wraps Minecraft's trade data.
 * 
 * <p>This implementation provides a complete TradeEntry with all the required
 * methods for filtering, modification, and tag expansion. It uses TradeItem
 * objects internally to represent items with quantities.</p>
 * 
 * <p>This class is designed to be used as a wrapper around Minecraft's
 * TradeOffer objects, providing the Tapestry API interface.</p>
 */
public class BasicTradeEntry implements TradeEntry {
    private TradeItem buy;
    private TradeItem buy2;
    private TradeItem sell;
    private int level;
    private int maxUses;
    private int experience;
    private float priceMultiplier;
    
    /**
     * Creates a new BasicTradeEntry with the specified parameters.
     * 
     * @param buy Primary input item (cannot be null)
     * @param buy2 Optional secondary input item (can be null)
     * @param sell Output item (cannot be null)
     * @param level Villager level (1-5)
     * @param maxUses Maximum uses before locking
     * @param experience Experience reward
     * @param priceMultiplier Price multiplier
     */
    public BasicTradeEntry(
        TradeItem buy,
        TradeItem buy2,
        TradeItem sell,
        int level,
        int maxUses,
        int experience,
        float priceMultiplier
    ) {
        this.buy = Objects.requireNonNull(buy, "Primary input cannot be null");
        this.buy2 = buy2;
        this.sell = Objects.requireNonNull(sell, "Output cannot be null");
        this.level = level;
        this.maxUses = maxUses;
        this.experience = experience;
        this.priceMultiplier = priceMultiplier;
    }
    
    /**
     * Creates a BasicTradeEntry from a specification map.
     * 
     * <p>Supported specification keys:</p>
     * <ul>
     *   <li>{@code "buy"} - TradeItem or Identifier (primary input)</li>
     *   <li>{@code "buy2"} - TradeItem or Identifier (secondary input, optional)</li>
     *   <li>{@code "sell"} - TradeItem or Identifier (output)</li>
     *   <li>{@code "level"} - Integer (villager level, required)</li>
     *   <li>{@code "maxUses"} - Integer (default: 16)</li>
     *   <li>{@code "experience"} - Integer (default: 0)</li>
     *   <li>{@code "priceMultiplier"} - Float (default: 1.0)</li>
     * </ul>
     * 
     * @param spec The specification map
     * @return A new BasicTradeEntry
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    public static BasicTradeEntry fromSpec(Map<String, Object> spec) {
        Objects.requireNonNull(spec, "Specification cannot be null");
        
        // Parse buy (required)
        TradeItem buy = parseTradeItem(spec.get("buy"), "buy");
        
        // Parse buy2 (optional)
        TradeItem buy2 = null;
        if (spec.containsKey("buy2")) {
            buy2 = parseTradeItem(spec.get("buy2"), "buy2");
        }
        
        // Parse sell (required)
        TradeItem sell = parseTradeItem(spec.get("sell"), "sell");
        
        // Parse level (required)
        Integer level = (Integer) spec.get("level");
        if (level == null) {
            throw new IllegalArgumentException("Missing required field: level");
        }
        if (level < 1 || level > 5) {
            throw new IllegalArgumentException("Level must be between 1 and 5, got: " + level);
        }
        
        // Parse optional fields with defaults
        Integer maxUses = Optional.ofNullable((Integer) spec.get("maxUses")).orElse(16);
        Integer experience = Optional.ofNullable((Integer) spec.get("experience")).orElse(0);
        Float priceMultiplier = Optional.ofNullable((Float) spec.get("priceMultiplier")).orElse(1.0f);
        
        return new BasicTradeEntry(buy, buy2, sell, level, maxUses, experience, priceMultiplier);
    }
    
    /**
     * Parses a TradeItem from various input formats.
     */
    private static TradeItem parseTradeItem(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }
        
        if (value instanceof TradeItem) {
            return (TradeItem) value;
        }
        
        if (value instanceof Identifier) {
            return TradeItem.of((Identifier) value);
        }
        
        if (value instanceof String) {
            return TradeItem.of(Identifier.of((String) value));
        }
        
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            Object itemObj = map.get("item");
            Object countObj = map.get("count");
            
            if (itemObj == null) {
                throw new IllegalArgumentException("TradeItem spec missing 'item' field in " + fieldName);
            }
            
            Identifier item;
            if (itemObj instanceof Identifier) {
                item = (Identifier) itemObj;
            } else if (itemObj instanceof String) {
                item = Identifier.of((String) itemObj);
            } else {
                throw new IllegalArgumentException("Invalid item type in " + fieldName + ": " + itemObj.getClass());
            }
            
            int count = 1;
            if (countObj != null) {
                if (countObj instanceof Integer) {
                    count = (Integer) countObj;
                } else if (countObj instanceof Number) {
                    count = ((Number) countObj).intValue();
                } else {
                    throw new IllegalArgumentException("Invalid count type in " + fieldName + ": " + countObj.getClass());
                }
            }
            
            return TradeItem.of(item, count);
        }
        
        throw new IllegalArgumentException("Invalid " + fieldName + " type: " + value.getClass());
    }
    
    // Implementation of TradeEntry interface
    
    @Override
    public Identifier getInputItem() {
        return buy.item();
    }
    
    @Override
    public int getInputCount() {
        return buy.count();
    }
    
    @Override
    public TradeItem getBuy() {
        return buy;
    }
    
    @Override
    public Identifier getInputItem2() {
        return buy2 != null ? buy2.item() : null;
    }
    
    @Override
    public int getInputCount2() {
        return buy2 != null ? buy2.count() : 0;
    }
    
    @Override
    public TradeItem getBuy2() {
        return buy2;
    }
    
    @Override
    public boolean hasInputTag(String tag) {
        return getInputItem().toString().equals(tag);
    }
    
    @Override
    public boolean hasInputTag2(String tag) {
        return buy2 != null && buy2.item().toString().equals(tag);
    }
    
    @Override
    public Identifier getOutputItem() {
        return sell.item();
    }
    
    @Override
    public int getOutputCount() {
        return sell.count();
    }
    
    @Override
    public TradeItem getSell() {
        return sell;
    }
    
    @Override
    public boolean hasOutputTag(String tag) {
        return sell.item().toString().equals(tag);
    }
    
    @Override
    public int getLevel() {
        return level;
    }
    
    @Override
    public int getMaxUses() {
        return maxUses;
    }
    
    @Override
    public int getExperience() {
        return experience;
    }
    
    @Override
    public float getPriceMultiplier() {
        return priceMultiplier;
    }
    
    @Override
    public void setInputItem(Identifier inputItem) {
        this.buy = TradeItem.of(inputItem, buy.count());
    }
    
    @Override
    public void setInputCount(int count) {
        this.buy = buy.withCount(count);
    }
    
    @Override
    public void setBuy(TradeItem buy) {
        this.buy = buy;
    }
    
    @Override
    public void setInputItem2(Identifier inputItem2) {
        if (inputItem2 == null) {
            this.buy2 = null;
        } else {
            int count = buy2 != null ? buy2.count() : 1;
            this.buy2 = TradeItem.of(inputItem2, count);
        }
    }
    
    @Override
    public void setInputCount2(int count) {
        if (buy2 != null) {
            this.buy2 = buy2.withCount(count);
        }
    }
    
    @Override
    public void setBuy2(TradeItem buy2) {
        this.buy2 = buy2;
    }
    
    @Override
    public void setOutputItem(Identifier outputItem) {
        this.sell = TradeItem.of(outputItem, sell.count());
    }
    
    @Override
    public void setOutputCount(int count) {
        this.sell = sell.withCount(count);
    }
    
    @Override
    public void setSell(TradeItem sell) {
        this.sell = sell;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BasicTradeEntry{");
        sb.append("buy=").append(buy);
        if (buy2 != null) {
            sb.append(", buy2=").append(buy2);
        }
        sb.append(", sell=").append(sell);
        sb.append(", level=").append(level);
        sb.append(", maxUses=").append(maxUses);
        sb.append(", experience=").append(experience);
        sb.append(", priceMultiplier=").append(priceMultiplier);
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BasicTradeEntry)) return false;
        BasicTradeEntry other = (BasicTradeEntry) obj;
        return Objects.equals(buy, other.buy) &&
               Objects.equals(buy2, other.buy2) &&
               Objects.equals(sell, other.sell) &&
               level == other.level &&
               maxUses == other.maxUses &&
               experience == other.experience &&
               Float.compare(priceMultiplier, other.priceMultiplier) == 0;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(buy, buy2, sell, level, maxUses, experience, priceMultiplier);
    }
}
