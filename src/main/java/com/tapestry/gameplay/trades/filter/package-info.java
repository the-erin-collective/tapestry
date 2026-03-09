/**
 * Structured filters for targeting specific trades in the Gameplay Patch Engine.
 * 
 * <p>This package provides structured filter implementations for identifying
 * specific trades within a villager's trade table. Filters use named criteria
 * fields rather than arbitrary predicate functions, enabling serialization
 * from TypeScript and validation at compile time.
 * 
 * <p>The primary filter is {@code TradeFilter}, which supports criteria including:
 * <ul>
 *   <li>Input item identifier</li>
 *   <li>Input item tag</li>
 *   <li>Output item identifier</li>
 *   <li>Output item tag</li>
 *   <li>Villager level</li>
 *   <li>Maximum uses</li>
 * </ul>
 * 
 * <p>Multiple criteria are combined using logical AND, so a trade must match
 * all specified criteria to be selected by the filter.
 * 
 * <p>Example usage:
 * <pre>{@code
 * TradeFilter filter = new TradeFilter(
 *     Optional.of(new Identifier("minecraft:emerald")),  // inputItem
 *     Optional.empty(),                                   // inputTag
 *     Optional.of(new Identifier("minecraft:wheat")),     // outputItem
 *     Optional.empty(),                                   // outputTag
 *     Optional.of(1),                                     // level
 *     Optional.empty()                                    // maxUses
 * );
 * Predicate<TradeEntry> predicate = filter.toPredicate();
 * }</pre>
 * 
 * @see com.tapestry.gameplay.patch.filter.StructuredFilter
 * @see com.tapestry.gameplay.trades.operations
 */
package com.tapestry.gameplay.trades.filter;
