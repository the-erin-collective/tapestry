package com.tapestry.gameplay.trades.operations;

import com.tapestry.gameplay.patch.PatchOperation;
import com.tapestry.gameplay.patch.PatchContext;
import com.tapestry.gameplay.trades.TradeTable;
import com.tapestry.gameplay.trades.filter.TradeEntry;

import java.util.Objects;
import java.util.Optional;

/**
 * A patch operation that adds a new trade entry to a villager's trade table.
 * 
 * <p>This operation is stateless and deterministic - adding the same trade entry
 * to the same trade table will always produce the same result.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * TradeEntry entry = // ... create trade entry
 * AddTradeOperation operation = new AddTradeOperation(entry);
 * operation.apply(tradeTable);
 * }</pre>
 * 
 * @see PatchOperation
 * @see TradeTable
 * @see TradeEntry
 */
public record AddTradeOperation(TradeEntry entry) implements PatchOperation<TradeTable> {
    
    /**
     * Compact constructor that validates the trade entry is not null.
     * 
     * @throws NullPointerException if entry is null
     */
    public AddTradeOperation {
        Objects.requireNonNull(entry, "Trade entry cannot be null");
    }
    
    /**
     * Applies this operation by adding the trade entry to the target trade table.
     * 
     * @param target The trade table to modify
     * @throws NullPointerException if target is null
     */
    @Override
    public void apply(TradeTable target) {
        Objects.requireNonNull(target, "Trade table cannot be null");
        target.add(entry);
    }
    
    /**
     * Returns a debug identifier for this operation.
     * 
     * @return A debug string describing the trade being added
     */
    @Override
    public void validate(PatchContext context) throws Exception {
        // ensure items referenced by the entry exist if non-null
        if (entry.getInputItem() != null && !context.registryContains(entry.getInputItem())) {
            throw new IllegalArgumentException("Unknown input item: " + entry.getInputItem());
        }
        if (entry.getOutputItem() != null && !context.registryContains(entry.getOutputItem())) {
            throw new IllegalArgumentException("Unknown output item: " + entry.getOutputItem());
        }
    }

    @Override
    public Optional<String> getDebugId() {
        return Optional.of(String.format(
            "AddTrade[input=%s, output=%s, level=%d]",
            entry.getInputItem(),
            entry.getOutputItem(),
            entry.getLevel()
        ));
    }
}
