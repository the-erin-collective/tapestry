package com.tapestry.gameplay.trades.operations;

import com.tapestry.gameplay.patch.PatchOperation;
import com.tapestry.gameplay.patch.PatchContext;
import com.tapestry.gameplay.trades.TradeTable;
import com.tapestry.gameplay.trades.expansion.TagTradeExpander;
import com.tapestry.gameplay.trades.filter.TradeFilter;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Optional;

/**
 * A patch operation that replaces the input item in trades matching a filter.
 * 
 * <p>This operation uses a {@link TradeFilter} to identify which trades should be modified,
 * then replaces the input item in all matching trades with the specified new input item.</p>
 * 
 * <p>If the new input is a tag identifier (starts with "#"), the operation will expand
 * the tag into individual trades for each item in the tag using {@link TagTradeExpander}.</p>
 * 
 * <p>This operation is stateless and deterministic - applying the same filter and replacement
 * to the same trade table will always produce the same result.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * TradeFilter filter = new TradeFilter(
 *     Optional.of(Identifier.of("minecraft:cod")),
 *     Optional.empty(),
 *     Optional.empty(),
 *     Optional.empty(),
 *     Optional.empty(),
 *     Optional.empty()
 * );
 * ReplaceTradeInputOperation operation = new ReplaceTradeInputOperation(
 *     filter,
 *     Identifier.of("minecraft:salmon")
 * );
 * operation.apply(tradeTable);
 * }</pre>
 * 
 * @see PatchOperation
 * @see TradeTable
 * @see TradeFilter
 * @see TagTradeExpander
 */
public record ReplaceTradeInputOperation(
    TradeFilter filter,
    Identifier newInput
) implements PatchOperation<TradeTable> {
    
    /**
     * Compact constructor that validates parameters are not null.
     * 
     * @throws NullPointerException if filter or newInput is null
     */
    public ReplaceTradeInputOperation {
        Objects.requireNonNull(filter, "Trade filter cannot be null");
        Objects.requireNonNull(newInput, "New input item cannot be null");
    }
    
    /**
     * Applies this operation by replacing the input item in all trades matching the filter.
     * 
     * <p>If the new input is a tag identifier (starts with "#"), delegates to TagTradeExpander
     * to expand the tag into individual trades. Otherwise, performs a simple replacement.</p>
     * 
     * @param target The trade table to modify
     * @throws NullPointerException if target is null
     */
    @Override
    public void apply(TradeTable target) {
        Objects.requireNonNull(target, "Trade table cannot be null");
        
        // Check if newInput is a tag identifier
        if (newInput.toString().startsWith("#")) {
            // Tag expansion: find matching trades and expand each one
            target.stream()
                .filter(filter.toPredicate())
                .forEach(entry -> TagTradeExpander.expandInputTag(target, entry, newInput.toString()));
        } else {
            // Simple replacement: replace input item in matching trades
            target.stream()
                .filter(filter.toPredicate())
                .forEach(entry -> entry.setInputItem(newInput));
        }
    }
    
    /**
     * Returns a debug identifier for this operation.
     * 
     * @return A debug string describing the filter and replacement
     */
    @Override
    public void validate(PatchContext context) throws Exception {
        filter.validate(context);
        
        // If it's a tag, we don't validate it exists (tags are resolved at runtime)
        // If it's an item, validate it exists
        if (!newInput.toString().startsWith("#") && !context.registryContains(newInput)) {
            throw new IllegalArgumentException("Unknown new input item: " + newInput);
        }
    }

    @Override
    public Optional<String> getDebugId() {
        StringBuilder criteria = new StringBuilder("ReplaceTradeInput[");
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
        if (filter.level().isPresent()) {
            if (!first) criteria.append(", ");
            criteria.append("level=").append(filter.level().get());
            first = false;
        }
        
        criteria.append(" -> newInput=").append(newInput).append("]");
        return Optional.of(criteria.toString());
    }
}
