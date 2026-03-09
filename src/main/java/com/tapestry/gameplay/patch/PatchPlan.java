package com.tapestry.gameplay.patch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiled, sorted, and validated collection of patch sets.
 * 
 * <p>A PatchPlan represents the result of compiling all registered patches from
 * a {@link PatchRegistry}. The plan stores patches sorted by priority, mod load
 * order, and registration order, ensuring deterministic application during both
 * initial bootstrap and datapack reload.</p>
 * 
 * <p>The plan is immutable after compilation and can be reused across multiple
 * patch application cycles without recompilation. This improves performance during
 * datapack reload by eliminating the need to re-sort patches.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * PatchRegistry registry = // ... register patches during TS_REGISTER phase
 * ModLoadOrder modLoadOrder = // ... obtain from mod loader
 * 
 * // Compile patches into an optimized plan
 * PatchPlan plan = PatchPlan.compile(registry, modLoadOrder);
 * 
 * // Use the plan to apply patches
 * List<PatchSet<TradeTable>> patches = plan.getPatchesFor(tradeTarget);
 * 
 * // Reuse the same plan during datapack reload
 * List<PatchSet<LootTable>> lootPatches = plan.getPatchesFor(lootTarget);
 * }</pre>
 * 
 * <p>The compilation process:</p>
 * <ol>
 *   <li>Retrieves all patches from the registry</li>
 *   <li>Sorts each target's patches using {@link PatchSetComparator}</li>
 *   <li>Calculates compilation statistics (total targets, total operations)</li>
 *   <li>Returns an immutable PatchPlan</li>
 * </ol>
 */
public class PatchPlan {
    private final Map<PatchTarget<?>, List<PatchSet<?>>> sortedPatches;
    private final PatchStatistics compilationStats;
    
    /**
     * Private constructor accepting sorted patches and statistics.
     * 
     * <p>Use {@link #compile(PatchRegistry, ModLoadOrder)} to create instances.</p>
     * 
     * @param sortedPatches The sorted patches indexed by target
     * @param stats The compilation statistics
     */
    private PatchPlan(Map<PatchTarget<?>, List<PatchSet<?>>> sortedPatches,
                      PatchStatistics stats) {
        this.sortedPatches = Map.copyOf(sortedPatches);
        this.compilationStats = stats;
    }
    
    /**
     * Compiles a patch plan from a registry using the given mod load order.
     * 
     * <p>This method retrieves all patches from the registry, sorts each target's
     * patches using a {@link PatchSetComparator}, and calculates compilation
     * statistics including total targets and total operations.</p>
     * 
     * <p>The sorting ensures deterministic patch application order:</p>
     * <ol>
     *   <li>Patches with lower priority values are applied first</li>
     *   <li>When priorities are equal, patches are ordered by mod load order</li>
     *   <li>When both priority and mod load order are equal, patches are ordered
     *       by registration sequence</li>
     * </ol>
     * 
     * @param registry The patch registry containing all registered patches
     * @param modLoadOrder The mod load order provider for sorting
     * @return A compiled, immutable patch plan
     * @throws NullPointerException if registry or modLoadOrder is null
     */
    /**
     * Convenience overload using the default runtime context for any
     * compilation-time validation.  This preserves backwards compatibility
     * with existing call sites.
     */
    public static PatchPlan compile(PatchRegistry registry,
                                    ModLoadOrder modLoadOrder) {
        return compile(registry, modLoadOrder, new DefaultPatchContext());
    }

    /**
     * Compiles a patch plan with validation using the supplied context.
     *
     * <p>The provided context is used during validation to ensure identifiers
     * referenced by filters, operations, and conditions actually exist in the
     * game registries.  Validation exceptions are wrapped in
     * IllegalArgumentException to surface errors early.</p>
     *
     * @param registry The patch registry containing all registered patches
     * @param modLoadOrder The mod load order provider for sorting
     * @param context The context used for identifier validation
     * @return A compiled, immutable patch plan
     * @throws NullPointerException if registry, modLoadOrder, or context is null
     * @throws IllegalArgumentException if any validation error occurs
     */
    public static PatchPlan compile(PatchRegistry registry,
                                    ModLoadOrder modLoadOrder,
                                    PatchContext context) {
        if (registry == null) {
            throw new NullPointerException("PatchRegistry cannot be null");
        }
        if (modLoadOrder == null) {
            throw new NullPointerException("ModLoadOrder cannot be null");
        }
        if (context == null) {
            throw new NullPointerException("PatchContext cannot be null");
        }
        
        Map<PatchTarget<?>, List<PatchSet<?>>> sorted = new HashMap<>();
        int totalOperations = 0;
        int totalTargets = 0;
        
        // Create comparator for sorting
        PatchSetComparator comparator = new PatchSetComparator(modLoadOrder);
        
        // Sort patches for each target
        for (var entry : registry.getAllPatches().entrySet()) {
            PatchTarget<?> target = entry.getKey();
            List<PatchSet<?>> patches = entry.getValue();
            
            // Sort by priority, then mod load order, then registration order
            List<PatchSet<?>> sortedList = patches.stream()
                .sorted(comparator)
                .toList();
            
            // Perform validation on each patch set and its operations
            for (PatchSet<?> ps : sortedList) {
                try {
                    ps.condition().ifPresent(c -> c.validate(context));
                    for (PatchOperation<?> op : ps.operations()) {
                        op.validate(context);
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "Validation failed for patch set " + ps + ": " + e.getMessage(), e);
                }
            }

            sorted.put(target, sortedList);
            totalTargets++;
            
            // Count total operations across all patch sets for this target
            totalOperations += sortedList.stream()
                .mapToInt(ps -> ps.operations().size())
                .sum();
        }
        
        // Create compilation statistics
        PatchStatistics stats = new PatchStatistics(
            totalTargets, 
            totalOperations, 
            0,  // applied - not yet applied during compilation
            0,  // failed - not yet applied during compilation
            0,  // skipped - not yet applied during compilation
            0,  // noOps - not yet applied during compilation
            0   // missingTargets - not yet applied during compilation
        );
        
        return new PatchPlan(sorted, stats);
    }
    
    /**
     * Retrieves all patch sets for the given target.
     * 
     * <p>Returns an empty list if no patches are registered for the target.
     * The returned patches are sorted by priority, mod load order, and
     * registration order.</p>
     * 
     * @param <T> The type of gameplay data the target represents
     * @param target The target to retrieve patches for
     * @return A sorted list of patch sets for the target, or an empty list if none exist
     */
    public <T> List<PatchSet<T>> getPatchesFor(PatchTarget<T> target) {
        List<PatchSet<?>> rawPatches = sortedPatches.getOrDefault(target, List.of());
        @SuppressWarnings("unchecked")
        List<PatchSet<T>> patches = (List<PatchSet<T>>) (List<?>) rawPatches;
        return patches;
    }
    
    /**
     * Returns the compilation statistics for this patch plan.
     * 
     * <p>The statistics include:</p>
     * <ul>
     *   <li>Total number of targets with registered patches</li>
     *   <li>Total number of operations across all patch sets</li>
     * </ul>
     * 
     * <p>Note that the applied, failed, skipped, noOps, and missingTargets
     * counters are zero at compilation time and are updated during patch
     * application by the {@link PatchEngine}.</p>
     * 
     * @return The compilation statistics
     */
    public PatchStatistics getCompilationStats() {
        return compilationStats;
    }

    /**
     * Returns all targets that have registered patches in this plan.
     *
     * <p>This method is useful for debugging and inspection tools that need to
     * iterate over all targets with patches.</p>
     *
     * @return A map of all targets to their patch sets
     */
    public Map<PatchTarget<?>, List<PatchSet<?>>> getAllTargets() {
        return sortedPatches;
    }

}
