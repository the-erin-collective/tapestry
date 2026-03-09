package com.tapestry.gameplay.trades.operations;

import com.tapestry.gameplay.patch.PatchOperation;
import com.tapestry.gameplay.trades.TradeTable;
import com.tapestry.gameplay.trades.filter.TradeFilter;

import java.util.Objects;
import java.util.Optional;

/**
 * A patch operation that removes trade entries matching a filter from a villager's trade table.
 * 
 * <p>This operation uses a {@link TradeFilter} to identify which trades should be removed.
 * All trades matching the filter criteria will be removed from the table.</p>
 * 
 * <p>This operation is stateless and deterministic - applying the same filter to the same
 * trade table will always remove the same trades.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * TradeFilter filter = new TradeFilter(
 *     Optional.of(Identifier.of("minecraft:emerald")),
 *     Optional.empty(),
 *     Optional.empty(),
 *     Optional.empty(),
 *     Optional.of(1),
 *     Optional.empty()
 * );
 * RemoveTradeOperation operation = new RemoveTradeOperation(filter);
 * operation.apply(tradeTable);
 * }</pre>
 * 
 * @see PatchOperation
 * @see TradeTable
 * @see TradeFilter
 */
public record RemoveTradeOperation(TradeFilter filter) implements PatchOperation<TradeTable> {
    
    /**
     * Compact constructor that validates the filter is not null.
     * 
     * @throws NullPointerException if filter is null
     */
    public RemoveTradeOperation {
        Objects.requireNonNull(filter, "Trade filter cannot be null");
    }
    
    /**
     * Applies this operation by removing all trades matching the filter from the target trade table.
     * 
     * @param target The trade table to modify
     * @throws NullPointerException if target is null
     */
    @Override
    public void apply(TradeTable target) {
        Objects.requireNonNull(target, "Trade table cannot be null");
        target.removeIf(filter.toPredicate());
    }
    
    /**
     * Returns a debug identifier for this operation.
     * 
     * @return A debug string describing the filter criteria
     */
    @Override
    public Optional<String> getDebugId() {
        StringBuilder criteria = new StringBuilder("RemoveTrade[");
        boolean first = true;
        
        if (filter.inputItem().isPresent()) {
            criteria.append("input=").append(filter.inputItem().get());
            first = false;
        }
        if (filter.inputTag().isPresent()) {
            if (!first) criteria.append(", ");
            criteria.append("inputTag=").append(filter.inputTag().get());
            first = false;
        }
        if (filter.outputItem().isPresent()) {
            if (!first) criteria.append(", ");
            criteria.append("output=").append(filter.outputItem().get());
            first = false;
        }
        if (filter.outputTag().isPresent()) {
            if (!first) criteria.append(", ");
            criteria.append("outputTag=").append(filter.outputTag().get());
            first = false;
        }
        if (filter.level().isPresent()) {
            if (!first) criteria.append(", ");
            criteria.append("level=").append(filter.level().get());
            first = false;
        }
        if (filter.maxUses().isPresent()) {
            if (!first) criteria.append(", ");
            criteria.append("maxUses=").append(filter.maxUses().get());
        }
        
        criteria.append("]");
        return Optional.of(criteria.toString());
    }
}
