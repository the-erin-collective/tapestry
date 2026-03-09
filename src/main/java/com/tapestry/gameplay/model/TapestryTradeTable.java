package com.tapestry.gameplay.model;

import com.tapestry.gameplay.trades.TradeTable;
import com.tapestry.gameplay.trades.filter.TradeEntry;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Wrapper around {@link TradeTable} providing a simple API for patch operations.
 * Implementations should modify the underlying trade table directly. The
 * wrapper is used by the patch engine when the optional TapestryModel support
 * is enabled.
 */
public class TapestryTradeTable implements TapestryModel<TradeTable> {
    private final TradeTable vanilla;

    public TapestryTradeTable(TradeTable vanilla) {
        if (vanilla == null) {
            throw new NullPointerException("TradeTable cannot be null");
        }
        this.vanilla = vanilla;
    }

    /**
     * Adds a new trade entry to the wrapped table.
     */
    public void addTrade(TradeEntry entry) {
        vanilla.add(entry);
    }

    /**
     * Removes trades matching the predicate.
     */
    public boolean removeIf(Predicate<TradeEntry> predicate) {
        return vanilla.removeIf(predicate);
    }

    /**
     * Returns a stream of all trades.
     */
    public Stream<TradeEntry> stream() {
        return vanilla.stream();
    }

    @Override
    public TradeTable unwrap() {
        return vanilla;
    }
}
