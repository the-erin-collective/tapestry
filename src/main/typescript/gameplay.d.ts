/**
 * TypeScript type definitions for the Tapestry Gameplay Patch Engine.
 * 
 * These types provide a type-safe interface for modifying gameplay data
 * (trades, loot tables, etc.) from TypeScript mods.
 * 
 * @module tapestry/gameplay
 */

/**
 * A Minecraft resource identifier in the format "namespace:path".
 * Examples: "minecraft:diamond", "mymod:custom_item"
 */
export type Identifier = string;

/**
 * Priority constants for controlling patch application order.
 * Lower values are applied first.
 */
export const PatchPriority = {
  /** Priority -1000: Applied very early */
  VERY_EARLY: -1000,
  /** Priority -500: Applied early */
  EARLY: -500,
  /** Priority 0: Default priority */
  NORMAL: 0,
  /** Priority 500: Applied late */
  LATE: 500,
  /** Priority 1000: Applied very late */
  VERY_LATE: 1000,
} as const;

/**
 * Filter specification for targeting specific trades in a villager's trade table.
 * All specified criteria are combined using logical AND.
 */
export interface TradeFilterSpec {
  /** The item identifier for the trade's input item */
  inputItem?: Identifier;
  /** Alias for inputItem */
  input?: Identifier;
  /** The tag identifier for the trade's input item (e.g., "#minecraft:logs") */
  inputTag?: string;
  /** The item identifier for the trade's output item */
  outputItem?: Identifier;
  /** Alias for outputItem */
  output?: Identifier;
  /** The tag identifier for the trade's output item */
  outputTag?: string;
  /** The villager level required for this trade (1-5) */
  level?: number;
  /** The maximum number of times this trade can be used */
  maxUses?: number;
}

/**
 * Specification for creating a new trade.
 */
export interface TradeSpec {
  /** The input item identifier */
  input: Identifier;
  /** The output item identifier */
  output: Identifier;
  /** The villager level for this trade (1-5) */
  level: number;
  /** Optional: The maximum number of times this trade can be used */
  maxUses?: number;
  /** Optional: The input item count */
  inputCount?: number;
  /** Optional: The output item count */
  outputCount?: number;
}

/**
 * Builder interface for modifying villager trade tables.
 * 
 * Provides methods for adding, removing, and modifying trades.
 * All operations are accumulated and applied when the builder completes.
 * 
 * @example
 * ```typescript
 * gameplay.trades.modify("minecraft:villager/fisherman", (trades) => {
 *   // Remove all cod trades at level 1
 *   trades.remove({ input: "minecraft:cod", level: 1 });
 *   
 *   // Replace cod with salmon in all trades
 *   trades.replaceInput(
 *     { input: "minecraft:cod" },
 *     "minecraft:salmon"
 *   );
 *   
 *   // Add a new trade
 *   trades.add({
 *     input: "minecraft:nautilus_shell",
 *     output: "minecraft:emerald",
 *     level: 2
 *   });
 * });
 * ```
 */
export interface TradeModificationBuilder {
  /**
   * Removes trades matching the filter specification.
   * 
   * All specified criteria are combined using logical AND.
   * Trades matching all criteria will be removed.
   * 
   * @param filterSpec The filter specification for targeting trades
   * @returns This builder for method chaining
   * 
   * @example
   * ```typescript
   * // Remove all cod trades at level 1
   * trades.remove({ input: "minecraft:cod", level: 1 });
   * 
   * // Remove all trades with emerald output
   * trades.remove({ output: "minecraft:emerald" });
   * ```
   */
  remove(filterSpec: TradeFilterSpec): TradeModificationBuilder;

  /**
   * Replaces the input item in trades matching the filter.
   * 
   * @param filterSpec The filter specification for targeting trades
   * @param newInput The new input item identifier
   * @returns This builder for method chaining
   * 
   * @example
   * ```typescript
   * // Replace cod with salmon in all trades
   * trades.replaceInput(
   *   { input: "minecraft:cod" },
   *   "minecraft:salmon"
   * );
   * 
   * // Replace all fish items with a tag
   * trades.replaceInput(
   *   { inputTag: "#minecraft:fishes" },
   *   "minecraft:cooked_cod"
   * );
   * ```
   */
  replaceInput(filterSpec: TradeFilterSpec, newInput: Identifier): TradeModificationBuilder;

  /**
   * Replaces the output item in trades matching the filter.
   * 
   * @param filterSpec The filter specification for targeting trades
   * @param newOutput The new output item identifier
   * @returns This builder for method chaining
   * 
   * @example
   * ```typescript
   * // Replace emerald output with diamond
   * trades.replaceOutput(
   *   { output: "minecraft:emerald" },
   *   "minecraft:diamond"
   * );
   * ```
   */
  replaceOutput(filterSpec: TradeFilterSpec, newOutput: Identifier): TradeModificationBuilder;

  /**
   * Adds a new trade to the trade table.
   * 
   * Note: This method is not yet fully implemented in the Java backend.
   * 
   * @param tradeSpec The specification for the new trade
   * @returns This builder for method chaining
   * 
   * @example
   * ```typescript
   * trades.add({
   *   input: "minecraft:nautilus_shell",
   *   output: "minecraft:emerald",
   *   level: 2,
   *   maxUses: 12
   * });
   * ```
   */
  add(tradeSpec: TradeSpec): TradeModificationBuilder;
}

/**
 * Filter specification for targeting specific loot pools in a loot table.
 * All specified criteria are combined using logical AND.
 */
export interface LootPoolFilterSpec {
  /** The name identifier for the loot pool */
  name?: string;
  /** The number of rolls for the loot pool */
  rolls?: number;
  /** The number of bonus rolls for the loot pool */
  bonusRolls?: number;
}

/**
 * Filter specification for targeting specific loot entries in a loot pool.
 * All specified criteria are combined using logical AND.
 */
export interface LootEntryFilterSpec {
  /** The item identifier for the loot entry */
  item?: Identifier;
  /** The type of loot entry (e.g., "minecraft:item", "minecraft:tag") */
  type?: string;
}

/**
 * Specification for creating a new loot pool.
 * 
 * Note: This is a simplified specification. The full LootPool structure
 * may require additional fields depending on the Java implementation.
 */
export interface LootPoolSpec {
  /** The loot pool object (if passing a pre-constructed pool) */
  pool?: any;
  /** The name of the pool */
  name?: string;
  /** The number of rolls */
  rolls?: number;
  /** The number of bonus rolls */
  bonusRolls?: number;
  /** The loot entries in this pool */
  entries?: LootEntrySpec[];
}

/**
 * Specification for creating a new loot entry.
 * 
 * Note: This is a simplified specification. The full LootPoolEntry structure
 * may require additional fields depending on the Java implementation.
 */
export interface LootEntrySpec {
  /** The loot entry object (if passing a pre-constructed entry) */
  entry?: any;
  /** The item identifier for this entry */
  item?: Identifier;
  /** The weight of this entry */
  weight?: number;
  /** The quality modifier */
  quality?: number;
}

/**
 * Builder interface for modifying loot tables.
 * 
 * Provides methods for adding, removing, and modifying loot pools and entries.
 * All operations are accumulated and applied when the builder completes.
 * 
 * @example
 * ```typescript
 * gameplay.loot.modify("minecraft:chests/simple_dungeon", (loot) => {
 *   // Remove the main pool
 *   loot.removePool({ name: "main" });
 *   
 *   // Add a new entry to a pool
 *   loot.addEntry(
 *     { name: "main" },
 *     { item: "minecraft:diamond", weight: 1 }
 *   );
 *   
 *   // Remove diamond entries from all pools
 *   loot.removeEntry(
 *     {},  // Match all pools
 *     { item: "minecraft:diamond" }
 *   );
 * });
 * ```
 */
export interface LootModificationBuilder {
  /**
   * Adds a new loot pool to the loot table.
   * 
   * @param poolSpec The specification for the new pool
   * @returns This builder for method chaining
   * 
   * @example
   * ```typescript
   * // Add a pool with a pre-constructed LootPool object
   * loot.addPool({ pool: myLootPool });
   * ```
   */
  addPool(poolSpec: LootPoolSpec): LootModificationBuilder;

  /**
   * Removes loot pools matching the filter specification.
   * 
   * All specified criteria are combined using logical AND.
   * Pools matching all criteria will be removed.
   * 
   * @param filterSpec The filter specification for targeting pools
   * @returns This builder for method chaining
   * 
   * @example
   * ```typescript
   * // Remove the main pool
   * loot.removePool({ name: "main" });
   * 
   * // Remove all pools with 1 roll
   * loot.removePool({ rolls: 1 });
   * ```
   */
  removePool(filterSpec: LootPoolFilterSpec): LootModificationBuilder;

  /**
   * Adds a new entry to loot pools matching the filter.
   * 
   * @param poolFilterSpec The filter specification for targeting pools
   * @param entrySpec The specification for the new entry
   * @returns This builder for method chaining
   * 
   * @example
   * ```typescript
   * // Add a diamond entry to the main pool
   * loot.addEntry(
   *   { name: "main" },
   *   { item: "minecraft:diamond", weight: 1 }
   * );
   * ```
   */
  addEntry(poolFilterSpec: LootPoolFilterSpec, entrySpec: LootEntrySpec): LootModificationBuilder;

  /**
   * Removes entries from loot pools matching the filters.
   * 
   * @param poolFilterSpec The filter specification for targeting pools
   * @param entryFilterSpec The filter specification for targeting entries
   * @returns This builder for method chaining
   * 
   * @example
   * ```typescript
   * // Remove all diamond entries from the main pool
   * loot.removeEntry(
   *   { name: "main" },
   *   { item: "minecraft:diamond" }
   * );
   * 
   * // Remove all item entries from all pools
   * loot.removeEntry(
   *   {},
   *   { type: "minecraft:item" }
   * );
   * ```
   */
  removeEntry(poolFilterSpec: LootPoolFilterSpec, entryFilterSpec: LootEntryFilterSpec): LootModificationBuilder;
}

/**
 * Trade modification API.
 * 
 * Provides methods for registering trade modifications during the TS_REGISTER phase.
 */
export interface TradeAPI {
  /**
   * Registers a trade modification for the specified villager profession.
   * 
   * This method must be called during the TS_REGISTER phase. The builder function
   * is stored and executed at phase completion to generate patch operations.
   * 
   * @param professionId The identifier of the villager profession to modify
   * @param builderFunction The function that configures trade modifications
   * 
   * @throws Error if called outside the TS_REGISTER phase
   * 
   * @example
   * ```typescript
   * gameplay.trades.modify("minecraft:villager/fisherman", (trades) => {
   *   trades.remove({ input: "minecraft:cod", level: 1 });
   *   trades.replaceInput({ input: "minecraft:cod" }, "minecraft:salmon");
   * });
   * ```
   */
  modify(professionId: Identifier, builderFunction: (builder: TradeModificationBuilder) => void): void;

  /**
   * Registers a trade modification with a custom priority.
   * 
   * This overload allows mods to specify a custom priority value to control
   * the order in which modifications are applied. Lower priority values are
   * applied first.
   * 
   * @param professionId The identifier of the villager profession to modify
   * @param builderFunction The function that configures trade modifications
   * @param priority The priority value for ordering (lower values apply first)
   * 
   * @throws Error if called outside the TS_REGISTER phase
   * 
   * @example
   * ```typescript
   * // Apply this modification early
   * gameplay.trades.modify(
   *   "minecraft:villager/fisherman",
   *   (trades) => {
   *     trades.remove({ input: "minecraft:cod" });
   *   },
   *   PatchPriority.EARLY
   * );
   * ```
   */
  modify(professionId: Identifier, builderFunction: (builder: TradeModificationBuilder) => void, priority: number): void;
}

/**
 * Loot modification API.
 * 
 * Provides methods for registering loot table modifications during the TS_REGISTER phase.
 */
export interface LootAPI {
  /**
   * Registers a loot table modification for the specified loot table.
   * 
   * This method must be called during the TS_REGISTER phase. The builder function
   * is stored and executed at phase completion to generate patch operations.
   * 
   * @param lootTableId The identifier of the loot table to modify
   * @param builderFunction The function that configures loot modifications
   * 
   * @throws Error if called outside the TS_REGISTER phase
   * 
   * @example
   * ```typescript
   * gameplay.loot.modify("minecraft:chests/simple_dungeon", (loot) => {
   *   loot.removePool({ name: "main" });
   *   loot.addEntry({ name: "main" }, { item: "minecraft:diamond", weight: 1 });
   * });
   * ```
   */
  modify(lootTableId: Identifier, builderFunction: (builder: LootModificationBuilder) => void): void;

  /**
   * Registers a loot table modification with a custom priority.
   * 
   * This overload allows mods to specify a custom priority value to control
   * the order in which modifications are applied. Lower priority values are
   * applied first.
   * 
   * @param lootTableId The identifier of the loot table to modify
   * @param builderFunction The function that configures loot modifications
   * @param priority The priority value for ordering (lower values apply first)
   * 
   * @throws Error if called outside the TS_REGISTER phase
   * 
   * @example
   * ```typescript
   * // Apply this modification late
   * gameplay.loot.modify(
   *   "minecraft:chests/simple_dungeon",
   *   (loot) => {
   *     loot.removeEntry({}, { item: "minecraft:diamond" });
   *   },
   *   PatchPriority.LATE
   * );
   * ```
   */
  modify(lootTableId: Identifier, builderFunction: (builder: LootModificationBuilder) => void, priority: number): void;
}

/**
 * Gameplay modification API.
 * 
 * Provides access to trade and loot modification APIs.
 */
export interface GameplayAPI {
  /** Trade modification API */
  trades: TradeAPI;
  /** Loot modification API */
  loot: LootAPI;
}

/**
 * Global gameplay API instance.
 * 
 * This is the main entry point for gameplay modifications in TypeScript mods.
 * 
 * @example
 * ```typescript
 * import { gameplay } from 'tapestry/gameplay';
 * 
 * // Modify villager trades
 * gameplay.trades.modify("minecraft:villager/fisherman", (trades) => {
 *   trades.remove({ input: "minecraft:cod" });
 * });
 * 
 * // Modify loot tables
 * gameplay.loot.modify("minecraft:chests/simple_dungeon", (loot) => {
 *   loot.addEntry({ name: "main" }, { item: "mymod:custom_item", weight: 1 });
 * });
 * ```
 */
export declare const gameplay: GameplayAPI;
