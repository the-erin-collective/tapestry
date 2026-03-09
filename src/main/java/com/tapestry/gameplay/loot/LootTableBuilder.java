package com.tapestry.gameplay.loot;

import com.tapestry.gameplay.loot.filter.LootEntryFilter;
import com.tapestry.gameplay.loot.filter.LootPoolFilter;
import com.tapestry.gameplay.loot.operations.AddEntryOperation;
import com.tapestry.gameplay.loot.operations.AddPoolOperation;
import com.tapestry.gameplay.loot.operations.RemoveEntryOperation;
import com.tapestry.gameplay.loot.operations.RemovePoolOperation;
import com.tapestry.gameplay.patch.PatchOperation;
import com.tapestry.gameplay.patch.PatchSet;
import com.tapestry.gameplay.patch.PatchTarget;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A fluent builder for constructing loot table modifications.
 * 
 * <p>The LootTableBuilder provides a convenient API for creating patch operations
 * that modify loot tables. It accumulates operations internally and produces
 * a {@link PatchSet} when {@link #build(Identifier, PatchTarget, int)} is called.</p>
 * 
 * <p>All builder methods return {@code this} to enable method chaining:</p>
 * <pre>{@code
 * LootTableBuilder builder = new LootTableBuilder();
 * PatchSet<LootTable> patchSet = builder
 *     .removePool(Map.of("name", "main"))
 *     .addEntry(
 *         Map.of("name", "main"),
 *         myLootEntry
 *     )
 *     .removeEntry(
 *         Map.of("name", "main"),
 *         Map.of("item", "minecraft:diamond")
 *     )
 *     .build(
 *         Identifier.of("mymod:chest_loot"),
 *         new PatchTarget<>(
 *             Identifier.of("minecraft:chests/simple_dungeon"),
 *             LootTable.class
 *         ),
 *         0
 *     );
 * }</pre>
 * 
 * @see PatchSet
 * @see LootTable
 * @see LootPoolFilter
 * @see LootEntryFilter
 */
public class LootTableBuilder {
    private final List<PatchOperation<LootTable>> operations;
    
    /**
     * Creates a new LootTableBuilder with an empty operations list.
     */
    public LootTableBuilder() {
        this.operations = new ArrayList<>();
    }
    
    /**
     * Adds an operation that adds a new loot pool to the loot table.
     * 
     * <p>The pool specification is a map containing the pool properties.
     * The exact format depends on the LootPool implementation. This method
     * currently requires a LootPool object to be provided directly.</p>
     * 
     * @param poolSpec The pool specification (currently expects a LootPool object in "pool" key)
     * @return This builder for method chaining
     * @throws IllegalArgumentException if poolSpec is null or doesn't contain a valid pool
     * @throws UnsupportedOperationException if LootPool creation from spec is not yet implemented
     */
    public LootTableBuilder addPool(Map<String, Object> poolSpec) {
        Objects.requireNonNull(poolSpec, "Pool specification cannot be null");
        
        // Check if a LootPool object is provided directly
        Object poolObj = poolSpec.get("pool");
        if (poolObj instanceof LootPool) {
            operations.add(new AddPoolOperation((LootPool) poolObj));
            return this;
        }
        
        // TODO: Implement LootPool.fromSpec() or a factory method
        // For now, this is a placeholder that will need to be completed when
        // the LootPool creation mechanism is implemented
        throw new UnsupportedOperationException(
            "LootPool creation from specification is not yet implemented. " +
            "A factory method or builder for LootPool needs to be added. " +
            "For now, pass a LootPool object with key 'pool' in the specification map."
        );
    }
    
    /**
     * Adds a remove operation that removes loot pools matching the filter specification.
     * 
     * <p>The filter specification is a map containing optional criteria:
     * <ul>
     *   <li>{@code "name"} - String (pool name identifier)</li>
     *   <li>{@code "rolls"} - Integer (number of rolls)</li>
     *   <li>{@code "bonusRolls"} - Integer (number of bonus rolls)</li>
     * </ul>
     * </p>
     * 
     * <p>All specified criteria are combined using logical AND. Pools matching
     * all criteria will be removed.</p>
     * 
     * @param filterSpec The filter specification map
     * @return This builder for method chaining
     * @throws IllegalArgumentException if filterSpec is null or contains invalid values
     */
    public LootTableBuilder removePool(Map<String, Object> filterSpec) {
        Objects.requireNonNull(filterSpec, "Filter specification cannot be null");
        LootPoolFilter filter = LootPoolFilter.fromSpec(filterSpec);
        operations.add(new RemovePoolOperation(filter));
        return this;
    }
    
    /**
     * Adds an operation that adds a new entry to loot pools matching the filter.
     * 
     * <p>The pool filter specification uses the same format as {@link #removePool(Map)}.</p>
     * 
     * <p>The entry specification is a map containing the entry properties.
     * The exact format depends on the LootPoolEntry implementation. This method
     * currently requires a LootPoolEntry object to be provided directly.</p>
     * 
     * @param poolFilterSpec The pool filter specification map
     * @param entrySpec The entry specification (currently expects a LootPoolEntry object in "entry" key)
     * @return This builder for method chaining
     * @throws IllegalArgumentException if poolFilterSpec or entrySpec is null or contains invalid values
     * @throws UnsupportedOperationException if LootPoolEntry creation from spec is not yet implemented
     */
    public LootTableBuilder addEntry(Map<String, Object> poolFilterSpec, Map<String, Object> entrySpec) {
        Objects.requireNonNull(poolFilterSpec, "Pool filter specification cannot be null");
        Objects.requireNonNull(entrySpec, "Entry specification cannot be null");
        
        LootPoolFilter poolFilter = LootPoolFilter.fromSpec(poolFilterSpec);
        
        // Check if a LootPoolEntry object is provided directly
        Object entryObj = entrySpec.get("entry");
        if (entryObj instanceof LootPoolEntry) {
            operations.add(new AddEntryOperation(poolFilter, (LootPoolEntry) entryObj));
            return this;
        }
        
        // TODO: Implement LootPoolEntry.fromSpec() or a factory method
        // For now, this is a placeholder that will need to be completed when
        // the LootPoolEntry creation mechanism is implemented
        throw new UnsupportedOperationException(
            "LootPoolEntry creation from specification is not yet implemented. " +
            "A factory method or builder for LootPoolEntry needs to be added. " +
            "For now, pass a LootPoolEntry object with key 'entry' in the specification map."
        );
    }
    
    /**
     * Adds a remove operation that removes entries from loot pools matching the filters.
     * 
     * <p>The pool filter specification uses the same format as {@link #removePool(Map)}.</p>
     * 
     * <p>The entry filter specification is a map containing optional criteria:
     * <ul>
     *   <li>{@code "item"} - String (item identifier)</li>
     *   <li>{@code "type"} - String (entry type)</li>
     * </ul>
     * </p>
     * 
     * <p>All specified criteria are combined using logical AND. Entries matching
     * all criteria will be removed from pools matching the pool filter.</p>
     * 
     * @param poolFilterSpec The pool filter specification map
     * @param entryFilterSpec The entry filter specification map
     * @return This builder for method chaining
     * @throws IllegalArgumentException if poolFilterSpec or entryFilterSpec is null or contains invalid values
     */
    public LootTableBuilder removeEntry(Map<String, Object> poolFilterSpec, Map<String, Object> entryFilterSpec) {
        Objects.requireNonNull(poolFilterSpec, "Pool filter specification cannot be null");
        Objects.requireNonNull(entryFilterSpec, "Entry filter specification cannot be null");
        
        LootPoolFilter poolFilter = LootPoolFilter.fromSpec(poolFilterSpec);
        LootEntryFilter entryFilter = LootEntryFilter.fromSpec(entryFilterSpec);
        
        operations.add(new RemoveEntryOperation(poolFilter, entryFilter));
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
     * @param target The target loot table to modify
     * @param priority The priority value for ordering (lower values apply first)
     * @return A new PatchSet containing all accumulated operations
     * @throws NullPointerException if modId or target is null
     */
    public PatchSet<LootTable> build(Identifier modId, PatchTarget<LootTable> target, int priority) {
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
