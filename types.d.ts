/**
 * Tapestry Gameplay API Type Definitions
 * 
 * This file provides TypeScript type definitions for the Tapestry Gameplay API,
 * enabling type-safe mod development with full IDE support.
 */

/**
 * Gameplay API namespace containing all gameplay-related APIs.
 */
declare namespace gameplay {
  // ============================================================================
  // Trait System
  // ============================================================================
  
  /**
   * Trait system for emergent cross-mod compatibility.
   * 
   * Traits are behavioral classifications that items can possess, enabling
   * automatic compatibility with entities and systems without explicit integration.
   * 
   * @example
   * ```typescript
   * // Register a custom trait
   * gameplay.traits.register("sea_vegetable", {
   *   tag: "tapestry:sea_vegetables"
   * });
   * 
   * // Declare trait consumption
   * gameplay.traits.consume("fish_food", {
   *   entity: "minecraft:cat",
   *   behavior: "food"
   * });
   * ```
   */
  namespace traits {
    /**
     * Registers a new gameplay trait.
     * 
     * Must be called during TS_REGISTER phase only.
     * 
     * @param name - Unique trait identifier (lowercase, underscores only)
     * @param config - Optional configuration (tag mapping)
     * @throws {Error} If called outside TS_REGISTER phase
     * @throws {Error} If trait name already registered
     * @throws {Error} If tag format is invalid
     */
    function register(name: string, config?: TraitConfig): void;
    
    /**
     * Declares that an entity consumes a specific trait.
     * 
     * Must be called during TS_REGISTER phase only.
     * 
     * @param name - Trait name to consume
     * @param config - Consumption configuration (entity, behavior)
     * @throws {Error} If called outside TS_REGISTER phase
     * @throws {Error} If trait does not exist
     */
    function consume(name: string, config: ConsumptionConfig): void;
  }
  
  /**
   * Configuration for trait registration.
   */
  interface TraitConfig {
    /**
     * Minecraft tag this trait maps to.
     * 
     * Default: "tapestry:{trait_name}_items"
     * Format: "namespace:path"
     * 
     * @example "tapestry:fish_items"
     */
    tag?: string;
  }
  
  /**
   * Configuration for trait consumption.
   */
  interface ConsumptionConfig {
    /**
     * Entity identifier that consumes this trait.
     * 
     * @example "minecraft:cat"
     */
    entity: string;
    
    /**
     * Behavior type for this consumption.
     * 
     * @example "food", "breeding", "taming"
     */
    behavior: string;
  }
  
  // ============================================================================
  // Item Registration
  // ============================================================================
  
  /**
   * Item registration API for creating custom items with trait support.
   * 
   * @example
   * ```typescript
   * gameplay.items.register("mymod:nori", {
   *   stackSize: 64,
   *   traits: ["fish_food", "plant_fiber"],
   *   food: {
   *     hunger: 2,
   *     saturation: 0.3,
   *     alwaysEdible: true
   *   },
   *   use(ctx) {
   *     ctx.player.clearStatusEffects();
   *     return { success: true };
   *   }
   * });
   * ```
   */
  namespace items {
    /**
     * Registers a new custom item.
     * 
     * Must be called during TS_REGISTER phase only.
     * 
     * @param id - Item identifier in format "namespace:path"
     * @param options - Item configuration options
     * @throws {Error} If called outside TS_REGISTER phase
     * @throws {Error} If item ID already registered
     * @throws {Error} If validation fails
     */
    function register(id: string, options?: ItemOptions): void;
  }
  
  /**
   * Configuration options for item registration.
   */
  interface ItemOptions {
    /**
     * Maximum stack size (1-64).
     * 
     * @default 64
     */
    stackSize?: number;
    
    /**
     * Gameplay traits this item possesses.
     * 
     * @example ["fish_food", "plant_fiber"]
     */
    traits?: string[];
    
    /**
     * Food component for edible items.
     */
    food?: FoodComponent;
    
    /**
     * Durability for tools and armor (≥0).
     */
    durability?: number;
    
    /**
     * Item returned after use (e.g., bucket from milk).
     * 
     * @example "minecraft:bucket"
     */
    recipeRemainder?: string;
    
    /**
     * Custom use behavior handler.
     * 
     * Called when the item is used (right-click).
     * 
     * @param ctx - Use context with player, world, stack, etc.
     * @returns Use result (item replacement, success status)
     */
    use?(ctx: UseContext): UseResult;
  }
  
  /**
   * Food component for edible items.
   */
  interface FoodComponent {
    /**
     * Hunger points restored (0-20).
     */
    hunger: number;
    
    /**
     * Saturation modifier (0-1).
     */
    saturation: number;
    
    /**
     * Can be eaten when not hungry.
     * 
     * @default false
     */
    alwaysEdible?: boolean;
    
    /**
     * Fast eating animation (like dried kelp).
     * 
     * @default false
     */
    snack?: boolean;
  }
  
  /**
   * Context provided to item use handlers.
   */
  interface UseContext {
    /**
     * Player using the item.
     */
    player: Player;
    
    /**
     * World where the item is being used.
     */
    world: World;
    
    /**
     * Item stack being used.
     */
    stack: ItemStack;
    
    /**
     * Hand holding the item.
     */
    hand: Hand;
    
    /**
     * Block position if targeting a block.
     */
    blockPos?: BlockPos;
    
    /**
     * Entity if targeting an entity.
     */
    entityTarget?: Entity;
  }
  
  /**
   * Result of item use.
   */
  interface UseResult {
    /**
     * Item to replace the used item with.
     * 
     * @example "minecraft:glass_bottle"
     */
    item?: string;
    
    /**
     * Whether the action succeeded.
     * 
     * @default true
     */
    success?: boolean;
  }
  
  // ============================================================================
  // Brewing Recipes
  // ============================================================================
  
  /**
   * Brewing recipe API for registering potion transformations.
   * 
   * @example
   * ```typescript
   * gameplay.brewing.register({
   *   input: "minecraft:awkward_potion",
   *   ingredient: "mymod:herbal_tea",
   *   output: "minecraft:leaping_potion"
   * });
   * ```
   */
  namespace brewing {
    /**
     * Registers a new brewing recipe.
     * 
     * Must be called during TS_REGISTER phase only.
     * 
     * @param config - Brewing recipe configuration
     * @throws {Error} If called outside TS_REGISTER phase
     * @throws {Error} If validation fails
     */
    function register(config: BrewingRecipeConfig): void;
  }
  
  /**
   * Configuration for brewing recipes.
   */
  interface BrewingRecipeConfig {
    /**
     * Input potion identifier.
     * 
     * @example "minecraft:awkward_potion"
     */
    input: string;
    
    /**
     * Ingredient item identifier.
     * 
     * @example "minecraft:blaze_powder"
     */
    ingredient: string;
    
    /**
     * Output potion identifier.
     * 
     * @example "minecraft:strength_potion"
     */
    output: string;
  }
  
  // ============================================================================
  // Loot Modification
  // ============================================================================
  
  /**
   * Loot modification API for modifying loot tables without file replacement.
   * 
   * @example
   * ```typescript
   * gameplay.loot.modify("minecraft:fishing/fish", (table) => {
   *   table.replace("minecraft:cod", "mymod:nori");
   *   table.replace("minecraft:salmon", "mymod:nori");
   * });
   * ```
   */
  namespace loot {
    /**
     * Registers a loot table modification.
     * 
     * Must be called during TS_REGISTER phase only.
     * Modifications are applied during datapack reload events.
     * 
     * @param tableId - Loot table identifier
     * @param modifier - Modification function
     * @throws {Error} If called outside TS_REGISTER phase
     */
    function modify(tableId: string, modifier: (table: LootTable) => void): void;
  }
  
  /**
   * Loot table wrapper for modifications.
   */
  interface LootTable {
    /**
     * Replaces all occurrences of an item in the loot table.
     * 
     * Handles nested structures (alternatives, groups, conditions).
     * 
     * @param oldItem - Item identifier to replace
     * @param newItem - Item identifier to replace with
     */
    replace(oldItem: string, newItem: string): void;
  }
  
  // ============================================================================
  // Minecraft Types (Minimal Definitions)
  // ============================================================================
  
  /**
   * Minecraft player entity.
   */
  interface Player {
    clearStatusEffects(): void;
    addStatusEffect(effect: string, duration: number, amplifier: number): void;
  }
  
  /**
   * Minecraft world.
   */
  interface World {
    isClient(): boolean;
  }
  
  /**
   * Minecraft item stack.
   */
  interface ItemStack {
    getItem(): string;
    getCount(): number;
    decrement(amount: number): void;
  }
  
  /**
   * Minecraft hand.
   */
  type Hand = "MAIN_HAND" | "OFF_HAND";
  
  /**
   * Minecraft block position.
   */
  interface BlockPos {
    x: number;
    y: number;
    z: number;
  }
  
  /**
   * Minecraft entity.
   */
  interface Entity {
    getType(): string;
  }
}

/**
 * Built-in traits provided by Tapestry framework.
 */
declare namespace BuiltInTraits {
  /**
   * Fish food trait for items that can be fed to fish-eating entities.
   * 
   * Tag: "tapestry:fish_items"
   * Consumers: cats, dolphins, axolotls
   */
  const FISH_FOOD = "fish_food";
  
  /**
   * Milk-like trait for items that behave like milk.
   * 
   * Tag: "tapestry:milk_items"
   * Consumers: cows (breeding)
   */
  const MILK_LIKE = "milk_like";
  
  /**
   * Egg-like trait for items that behave like eggs.
   * 
   * Tag: "tapestry:egg_items"
   * Consumers: chickens (breeding)
   */
  const EGG_LIKE = "egg_like";
  
  /**
   * Honey-like trait for items that behave like honey.
   * 
   * Tag: "tapestry:honey_items"
   */
  const HONEY_LIKE = "honey_like";
  
  /**
   * Plant fiber trait for plant-based materials.
   * 
   * Tag: "tapestry:plant_fibers"
   */
  const PLANT_FIBER = "plant_fiber";
}

/**
 * Export gameplay API for use in mods.
 */
export { gameplay, BuiltInTraits };