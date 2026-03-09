package com.tapestry.gameplay.trades.operations;

import com.tapestry.gameplay.patch.PatchOperation;
import com.tapestry.gameplay.patch.PatchContext;
import com.tapestry.gameplay.trades.TradeTable;
import com.tapestry.gameplay.trades.filter.TradeFilter;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Optional;

/**
 * A patch operation that replaces the output item in trades matching a filter.
 * 
 * <p>This operation uses a {@link TradeFilter} to identify which trades should be modified,
 * then replaces the output item in all matching trades with the specified new output item.</p>
 * 
 * <p>This operation is stateless and deterministic - applying the same filter and replacement
 * to the same trade table will always produce the same result.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * TradeFilter filter = new TradeFilter(
 *     Optional.empty(),
 *     Optional.empty(),
 *     Optional.of(Identifier.of("minecraft:emerald")),
 *     Optional.empty(),
 *     Optional.of(1),
 *     Optional.empty()
 * );
 * ReplaceTradeOutputOperation operation = new ReplaceTradeOutputOperation(
 *     filter,
 *     Identifier.of("minecraft:diamond")
 * );
 * operation.apply(tradeTable);
 * }</pre>
 * 
 * @see PatchOperation
 * @see TradeTable
 * @see TradeFilter
 */
public record ReplaceTradeOutputOperation(
    TradeFilter filter,
    Identifier newOutput
) implements PatchOperation<TradeTable> {
    
    /**
     * Compact constructor that validates parameters are not null.
     * 
     * @throws NullPointerException if filter or newOutput is null
     */
    public ReplaceTradeOutputOperation {
        Objects.requireNonNull(filter, "Trade filter cannot be null");
        Objects.requireNonNull(newOutput, "New output item cannot be null");
    }
    
    /**
     * Applies this operation by replacing the output item in all trades matching the filter.
     * 
     * @param target The trade table to modify
     * @throws NullPointerException if target is null
     */
    @Override
    public void apply(TradeTable target) {
        Objects.requireNonNull(target, "Trade table cannot be null");
        target.stream()
            .filter(filter.toPredicate())
            .forEach(entry -> entry.setOutputItem(newOutput));
    }
    
    /**
     * Returns a debug identifier for this operation.
     * 
     * @return A debug string describing the filter and replacement
     */
    @Override
    public void validate(PatchContext context) throws Exception {
        filter.validate(context);
        if (!context.registryContains(newOutput)) {
            throw new IllegalArgumentException("Unknown new output item: " + newOutput);
        }
    }

    @Override
    public Optional<String> getDebugId() {
        StringBuilder criteria = new StringBuilder("ReplaceTradeOutput[");
        boolean first = true;
        
        if (filter.inputItem().isPresent()) {
            criteria.append("input=").append(filter.inputItem().get());
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
        
        criteria.append(" -> newOutput=").append(newOutput).append("]");
        return Optional.of(criteria.toString());
    }
}
