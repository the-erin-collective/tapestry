/**
 * Structured filters for targeting specific loot pools and entries in the Gameplay Patch Engine.
 * 
 * <p>This package provides structured filter implementations for identifying
 * specific loot pools and entries within a loot table. Filters use named criteria
 * fields rather than arbitrary predicate functions, enabling serialization
 * from TypeScript and validation at compile time.
 * 
 * <p>The primary filter is {@code LootPoolFilter}, which supports criteria including:
 * <ul>
 *   <li>Pool name</li>
 *   <li>Number of rolls</li>
 *   <li>Bonus rolls</li>
 * </ul>
 * 
 * <p>Multiple criteria are combined using logical AND, so a loot pool must match
 * all specified criteria to be selected by the filter.
 * 
 * <p>Example usage:
 * <pre>{@code
 * LootPoolFilter filter = new LootPoolFilter(
 *     Optional.of("main"),      // name
 *     Optional.empty(),         // rolls
 *     Optional.empty()          // bonusRolls
 * );
 * Predicate<LootPool> predicate = filter.toPredicate();
 * }</pre>
 * 
 * @see com.tapestry.gameplay.patch.filter.StructuredFilter
 * @see com.tapestry.gameplay.loot.operations
 */
package com.tapestry.gameplay.loot.filter;
