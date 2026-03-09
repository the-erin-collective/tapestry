/**
 * Trade modification system for the Gameplay Patch Engine.
 * 
 * <p>This package provides the infrastructure for modifying villager trades
 * through the unified patch engine. It includes:
 * <ul>
 *   <li>Trade-specific patch operations (add, remove, replace)</li>
 *   <li>Structured filters for targeting specific trades</li>
 *   <li>Builder API for constructing trade modifications</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * TradeAPI.modify(VillagerProfession.FARMER, builder -> {
 *     builder.remove(filter -> filter.outputItem("minecraft:wheat"));
 *     builder.add(trade -> trade.input("minecraft:emerald").output("minecraft:diamond"));
 * });
 * }</pre>
 * 
 * @see com.tapestry.gameplay.patch.PatchEngine
 * @see com.tapestry.gameplay.trades.operations
 * @see com.tapestry.gameplay.trades.filter
 */
package com.tapestry.gameplay.trades;
