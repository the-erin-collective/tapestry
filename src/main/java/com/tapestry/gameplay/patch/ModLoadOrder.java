package com.tapestry.gameplay.patch;

import net.minecraft.util.Identifier;

/**
 * Provides mod load order comparison for deterministic patch ordering.
 * 
 * <p>This interface abstracts the mod loading system's dependency resolution,
 * allowing the patch engine to sort patches by mod load order without directly
 * depending on the mod loader implementation.</p>
 * 
 * <p>Implementations should return:</p>
 * <ul>
 *   <li>Negative value if modA loads before modB</li>
 *   <li>Zero if modA and modB are the same mod</li>
 *   <li>Positive value if modA loads after modB</li>
 * </ul>
 * 
 * <p>The comparison must be consistent with the actual mod loading order
 * determined by the dependency resolution system.</p>
 */
public interface ModLoadOrder {
    /**
     * Compares two mod identifiers based on their load order.
     * 
     * @param modA The first mod identifier
     * @param modB The second mod identifier
     * @return A negative integer, zero, or a positive integer as modA loads
     *         before, at the same time as, or after modB
     */
    int compare(Identifier modA, Identifier modB);
}
