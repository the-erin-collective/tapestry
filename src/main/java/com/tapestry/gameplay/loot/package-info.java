/**
 * Loot modification system for the Gameplay Patch Engine.
 * 
 * <p>This package provides the infrastructure for modifying loot tables
 * through the unified patch engine. It includes:
 * <ul>
 *   <li>Loot-specific patch operations (add pool, remove pool, add entry, remove entry)</li>
 *   <li>Structured filters for targeting specific loot pools and entries</li>
 *   <li>Builder API for constructing loot modifications</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * LootAPI.modify(new Identifier("minecraft:chests/simple_dungeon"), builder -> {
 *     builder.removePool(filter -> filter.name("main"));
 *     builder.addPool(pool -> pool.rolls(1, 3).entry(entry -> entry.item("minecraft:diamond")));
 * });
 * }</pre>
 * 
 * @see com.tapestry.gameplay.patch.PatchEngine
 * @see com.tapestry.gameplay.loot.operations
 * @see com.tapestry.gameplay.loot.filter
 */
package com.tapestry.gameplay.loot;
