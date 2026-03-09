/**
 * Trade-specific patch operations for the Gameplay Patch Engine.
 * 
 * <p>This package contains concrete implementations of {@link com.tapestry.gameplay.patch.PatchOperation}
 * for modifying villager trade tables. Each operation represents a specific type of modification
 * that can be applied to a {@code TradeTable}.
 * 
 * <p>Available operations include:
 * <ul>
 *   <li>{@code AddTradeOperation} - Adds a new trade to a villager profession</li>
 *   <li>{@code RemoveTradeOperation} - Removes trades matching a filter</li>
 *   <li>{@code ReplaceTradeInputOperation} - Replaces input items in matching trades</li>
 *   <li>{@code ReplaceTradeOutputOperation} - Replaces output items in matching trades</li>
 * </ul>
 * 
 * <p>All operations are stateless and deterministic, supporting datapack reload
 * by producing the same result when applied to fresh data.
 * 
 * @see com.tapestry.gameplay.patch.PatchOperation
 * @see com.tapestry.gameplay.trades.filter.TradeFilter
 */
package com.tapestry.gameplay.trades.operations;
