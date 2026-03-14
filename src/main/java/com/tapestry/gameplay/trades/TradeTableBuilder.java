package com.tapestry.gameplay.trades;

import com.tapestry.gameplay.patch.PatchOperation;
import com.tapestry.gameplay.patch.PatchSet;
import com.tapestry.gameplay.patch.PatchTarget;
import com.tapestry.gameplay.trades.filter.TradeEntry;
import com.tapestry.gameplay.trades.filter.TradeFilter;
import com.tapestry.gameplay.trades.model.BasicTradeEntry;
import com.tapestry.gameplay.trades.operations.AddTradeOperation;
import com.tapestry.gameplay.trades.operations.RemoveTradeOperation;
import com.tapestry.gameplay.trades.operations.ReplaceTradeInputOperation;
import com.tapestry.gameplay.trades.operations.ReplaceTradeOutputOperation;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A fluent builder for constructing trade table modifications.
 * 
 * <p>The TradeTableBuilder provides a convenient API for creating patch operations
 * that modify villager trade tables. It accumulates operations internally and produces
 * a {@link PatchSet} when {@link #build(Identifier, PatchTarget, int)} is called.</p>
 * 
 * <p>All builder methods return {@code this} to enable method chaining:</p>
 * <pre>{@code
 * TradeTableBuilder builder = new TradeTableBuilder();
 * PatchSet<TradeTable> patchSet = builder
 *     .remove(Map.of("input", "minecraft:cod", "level", 1))
 *     .replaceInput(
 *         Map.of("input", "minecraft:cod"),
 *         Identifier.of("minecraft:salmon")
 *     )
 *     .add(Map.of(
 *         "input", "minecraft:nautilus_shell",
 *         "output", "minecraft:emerald",
 *         "level", 2
 *     ))
 *     .build(
 *         Identifier.of("mymod:fisherman_trades"),
 *         new PatchTarget<>(
 *             Identifier.of("minecraft:villager/fisherman"),
 *             TradeTable.class
 *         ),
 *         0
 *     );
 * }</pre>
 * 
 * @see PatchSet
 * @see TradeTable
 * @see TradeFilter
 */
public class TradeTableBuilder {
    private final List<PatchOperation<TradeTable>> operations;
    
    /**
     * Creates a new TradeTableBuilder with an empty operations list.
     */
    public TradeTableBuilder() {
        this.operations = new ArrayList<>();
    }
    
    /**
     * Adds a remove operation that removes trades matching the filter specification.
     * 
     * <p>The filter specification is a map containing optional criteria:
     * <ul>
     *   <li>{@code "inputItem"} or {@code "input"} - String (item identifier)</li>
     *   <li>{@code "inputTag"} - String (tag identifier)</li>
     *   <li>{@code "outputItem"} or {@code "output"} - String (item identifier)</li>
     *   <li>{@code "outputTag"} - String (tag identifier)</li>
     *   <li>{@code "level"} - Integer (villager level)</li>
     *   <li>{@code "maxUses"} - Integer (maximum uses)</li>
     * </ul>
     * </p>
     * 
     * <p>All specified criteria are combined using logical AND. Trades matching
     * all criteria will be removed.</p>
     * 
     * @param filterSpec The filter specification map
     * @return This builder for method chaining
     * @throws IllegalArgumentException if filterSpec is null or contains invalid values
     */
    public TradeTableBuilder remove(Map<String, Object> filterSpec) {
        Objects.requireNonNull(filterSpec, "Filter specification cannot be null");
        TradeFilter filter = TradeFilter.fromSpec(filterSpec);
        operations.add(new RemoveTradeOperation(filter));
        return this;
    }
    
    /**
     * Adds a replace input operation that replaces the input item in trades matching the filter.
     * 
     * <p>The filter specification uses the same format as {@link #remove(Map)}.</p>
     * 
     * @param filterSpec The filter specification map
     * @param newInput The new input item identifier
     * @return This builder for method chaining
     * @throws IllegalArgumentException if filterSpec is null or contains invalid values
     * @throws NullPointerException if newInput is null
     */
    public TradeTableBuilder replaceInput(Map<String, Object> filterSpec, Identifier newInput) {
        Objects.requireNonNull(filterSpec, "Filter specification cannot be null");
        Objects.requireNonNull(newInput, "New input identifier cannot be null");
        TradeFilter filter = TradeFilter.fromSpec(filterSpec);
        operations.add(new ReplaceTradeInputOperation(filter, newInput));
        return this;
    }
    
    /**
     * Adds a replace output operation that replaces the output item in trades matching the filter.
     * 
     * <p>The filter specification uses the same format as {@link #remove(Map)}.</p>
     * 
     * @param filterSpec The filter specification map
     * @param newOutput The new output item identifier
     * @return This builder for method chaining
     * @throws IllegalArgumentException if filterSpec is null or contains invalid values
     * @throws NullPointerException if newOutput is null
     */
    public TradeTableBuilder replaceOutput(Map<String, Object> filterSpec, Identifier newOutput) {
        Objects.requireNonNull(filterSpec, "Filter specification cannot be null");
        Objects.requireNonNull(newOutput, "New output identifier cannot be null");
        TradeFilter filter = TradeFilter.fromSpec(filterSpec);
        operations.add(new ReplaceTradeOutputOperation(filter, newOutput));
        return this;
    }
    
    /**
     * Adds an add operation that adds a new trade to the trade table.
     * 
     * <p>The trade specification is a map that must contain the trade properties.
     * Supported keys:
     * <ul>
     *   <li>{@code "buy"} - TradeItem, Identifier, or String (primary input)</li>
     *   <li>{@code "buy2"} - TradeItem, Identifier, or String (secondary input, optional)</li>
     *   <li>{@code "sell"} - TradeItem, Identifier, or String (output)</li>
     *   <li>{@code "level"} - Integer (villager level, required)</li>
     *   <li>{@code "maxUses"} - Integer (default: 16)</li>
     *   <li>{@code "experience"} - Integer (default: 0)</li>
     *   <li>{@code "priceMultiplier"} - Float (default: 1.0)</li>
     * </ul>
     * </p>
     * 
     * @param tradeSpec The trade specification map
     * @return This builder for method chaining
     * @throws IllegalArgumentException if tradeSpec is null or contains invalid values
     */
    public TradeTableBuilder add(Map<String, Object> tradeSpec) {
        Objects.requireNonNull(tradeSpec, "Trade specification cannot be null");
        
        TradeEntry entry = BasicTradeEntry.fromSpec(tradeSpec);
        operations.add(new AddTradeOperation(entry));
        return this;
    }
    
    /**
     * Builds a PatchSet containing all accumulated operations.
     * 
     * <p>This method creates an immutable PatchSet with the specified mod identifier,
     * target, and priority. The operations list is copied to prevent external modification.</p>
     * 
     * <p>After calling this method, the builder can be reused to create additional
     * patch sets, but the returned PatchSet is independent and immutable.</p>
     * 
     * @param modId The identifier of the mod registering this patch set
     * @param target The target trade table to modify
     * @param priority The priority value for ordering (lower values apply first)
     * @return A new PatchSet containing all accumulated operations
     * @throws NullPointerException if modId or target is null
     */
    public PatchSet<TradeTable> build(Identifier modId, PatchTarget<TradeTable> target, int priority) {
        Objects.requireNonNull(modId, "Mod identifier cannot be null");
        Objects.requireNonNull(target, "Patch target cannot be null");
        
        return new PatchSet<>(
            modId,
            target,
            priority,
            new ArrayList<>(operations), // Create a copy to prevent external modification
            Optional.empty() // No condition by default
        );
    }
}
