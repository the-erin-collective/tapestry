/**
 * Loot-specific patch operations for the Gameplay Patch Engine.
 * 
 * <p>This package contains concrete implementations of {@link com.tapestry.gameplay.patch.PatchOperation}
 * for modifying loot tables. Each operation represents a specific type of modification
 * that can be applied to a {@code LootTable}.
 * 
 * <p>Available operations include:
 * <ul>
 *   <li>{@code AddPoolOperation} - Adds a new loot pool to a loot table</li>
 *   <li>{@code RemovePoolOperation} - Removes loot pools matching a filter</li>
 *   <li>{@code AddEntryOperation} - Adds a new entry to pools matching a filter</li>
 *   <li>{@code RemoveEntryOperation} - Removes entries from pools matching filters</li>
 * </ul>
 * 
 * <p>All operations are stateless and deterministic, supporting datapack reload
 * by producing the same result when applied to fresh data.
 * 
 * @see com.tapestry.gameplay.patch.PatchOperation
 * @see com.tapestry.gameplay.loot.filter
 */
package com.tapestry.gameplay.loot.operations;
