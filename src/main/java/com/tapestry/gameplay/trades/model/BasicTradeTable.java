package com.tapestry.gameplay.trades.model;

import com.tapestry.gameplay.trades.TradeTable;
import com.tapestry.gameplay.trades.filter.TradeEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A simple in-memory implementation of TradeTable for testing and development.
 * 
 * <p>This implementation stores trade entries in a list and provides basic
 * operations for adding, removing, and streaming trades. It's designed for
 * use in unit tests and development scenarios where a full Minecraft
 * integration isn't available.</p>
 */
public class BasicTradeTable implements TradeTable {
    private final List<TradeEntry> trades;
    
    /**
     * Creates a new empty BasicTradeTable.
     */
    public BasicTradeTable() {
        this.trades = new ArrayList<>();
    }
    
    /**
     * Creates a new BasicTradeTable with the specified initial trades.
     * 
     * @param initialTrades The initial trade entries
     */
    public BasicTradeTable(List<TradeEntry> initialTrades) {
        this.trades = new ArrayList<>(initialTrades);
    }
    
    @Override
    public void add(TradeEntry entry) {
        trades.add(entry);
    }
    
    @Override
    public boolean removeIf(Predicate<TradeEntry> predicate) {
        return trades.removeIf(predicate);
    }
    
    @Override
    public Stream<TradeEntry> stream() {
        return trades.stream();
    }
    
    /**
     * Returns the number of trades in this table.
     * 
     * @return The trade count
     */
    public int size() {
        return trades.size();
    }
    
    /**
     * Returns a copy of the trades list.
     * 
     * @return A new list containing all trades
     */
    public List<TradeEntry> getTrades() {
        return new ArrayList<>(trades);
    }
    
    /**
     * Clears all trades from this table.
     */
    public void clear() {
        trades.clear();
    }
    
    @Override
    public String toString() {
        return "BasicTradeTable{trades=" + trades + "}";
    }
}
