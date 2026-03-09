package com.tapestry.gameplay.trades;

import com.tapestry.gameplay.trades.filter.TradeEntry;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents a villager's trade table containing multiple trade entries.
 * 
 * <p>This interface provides methods for modifying trade tables through the
 * Gameplay Patch Engine. Implementations wrap Minecraft's villager trade data structures.</p>
 * 
 * <p>Trade tables support operations including:
 * <ul>
 *   <li>Adding new trades</li>
 *   <li>Removing trades matching a predicate</li>
 *   <li>Streaming trades for filtering and modification</li>
 * </ul>
 * </p>
 */
public interface TradeTable {
    /**
     * Adds a new trade entry to this trade table.
     * 
     * @param entry The trade entry to add
     */
    void add(TradeEntry entry);
    
    /**
     * Removes all trade entries matching the given predicate.
     * 
     * @param predicate The predicate to test each trade entry
     * @return true if any trades were removed, false otherwise
     */
    boolean removeIf(Predicate<TradeEntry> predicate);
    
    /**
     * Returns a stream of all trade entries in this table.
     * 
     * <p>The stream can be used to filter and modify trades.</p>
     * 
     * @return A stream of trade entries
     */
    Stream<TradeEntry> stream();
}
