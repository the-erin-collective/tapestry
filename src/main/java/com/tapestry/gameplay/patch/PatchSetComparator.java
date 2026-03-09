package com.tapestry.gameplay.patch;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Implements deterministic three-level sorting for patch sets.
 * 
 * <p>This comparator ensures that patch sets are applied in a predictable order
 * by comparing them using three levels of criteria:</p>
 * <ol>
 *   <li><b>Priority</b>: Lower priority values are applied first (ascending order)</li>
 *   <li><b>Mod Load Order</b>: If priorities are equal, mods are ordered by their
 *       dependency-resolved load order</li>
 *   <li><b>Registration Order</b>: If both priority and mod load order are equal,
 *       patches are ordered by the sequence in which they were registered</li>
 * </ol>
 * 
 * <p>Registration order is tracked internally using an identity-based map that assigns
 * each patch set instance a unique sequence number on first access. This ensures that
 * patches registered by the same mod are applied in the order they were registered,
 * even if they have identical content.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * ModLoadOrder modLoadOrder = // ... obtain from mod loader
 * PatchSetComparator comparator = new PatchSetComparator(modLoadOrder);
 * 
 * List<PatchSet<?>> patches = // ... get patches from registry
 * patches.sort(comparator);
 * }</pre>
 * 
 * <p>This comparator is used by {@link PatchPlan} during compilation to pre-sort
 * patches for efficient application during bootstrap and datapack reload.</p>
 */
class PatchSetComparator implements Comparator<PatchSet<?>> {
    private final ModLoadOrder modLoadOrder;
    private final Map<PatchSet<?>, Integer> registrationOrder;
    
    /**
     * Creates a new patch set comparator with the given mod load order.
     * 
     * @param modLoadOrder The mod load order provider for comparing mod identifiers
     * @throws NullPointerException if modLoadOrder is null
     */
    PatchSetComparator(ModLoadOrder modLoadOrder) {
        if (modLoadOrder == null) {
            throw new NullPointerException("ModLoadOrder cannot be null");
        }
        this.modLoadOrder = modLoadOrder;
        // Use IdentityHashMap to track registration order by object identity,
        // not by equals/hashCode, so that two PatchSets with identical content
        // are still treated as separate instances
        this.registrationOrder = new IdentityHashMap<>();
    }
    
    /**
     * Compares two patch sets using three-level ordering.
     * 
     * <p>The comparison follows this algorithm:</p>
     * <ol>
     *   <li>Compare by priority (ascending)</li>
     *   <li>If priorities are equal, compare by mod load order</li>
     *   <li>If mod load orders are equal, compare by registration order</li>
     * </ol>
     * 
     * @param a The first patch set
     * @param b The second patch set
     * @return A negative integer, zero, or a positive integer as the first
     *         patch set should be applied before, at the same time as, or
     *         after the second patch set
     */
    @Override
    public int compare(PatchSet<?> a, PatchSet<?> b) {
        // Level 1: Priority (ascending order - lower values first)
        int priorityCompare = Integer.compare(a.priority(), b.priority());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        
        // Level 2: Mod load order
        int modOrderCompare = modLoadOrder.compare(a.modId(), b.modId());
        if (modOrderCompare != 0) {
            return modOrderCompare;
        }
        
        // Level 3: Registration order
        // Use computeIfAbsent to assign a unique sequence number on first access
        int aOrder = registrationOrder.computeIfAbsent(a, k -> registrationOrder.size());
        int bOrder = registrationOrder.computeIfAbsent(b, k -> registrationOrder.size());
        return Integer.compare(aOrder, bOrder);
    }
}
