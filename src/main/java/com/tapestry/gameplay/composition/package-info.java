/**
 * COMPOSITION phase implementation for Tapestry Gameplay API.
 * 
 * <p>This package contains the core components that execute during the COMPOSITION phase:
 * 
 * <h2>Components</h2>
 * <ul>
 *   <li>{@link com.tapestry.gameplay.composition.CompositionOrchestrator} - Orchestrates the entire COMPOSITION phase</li>
 *   <li>{@link com.tapestry.gameplay.composition.TraitResolver} - Resolves trait-to-item mappings</li>
 *   <li>{@link com.tapestry.gameplay.composition.BehaviorTagGenerator} - Generates Minecraft tag files from trait definitions</li>
 * </ul>
 * 
 * <h2>COMPOSITION Phase Flow</h2>
 * <ol>
 *   <li><b>Trait Resolution</b>: Collect all registered traits and items, then build mappings between traits and items that possess them</li>
 *   <li><b>Tag Generation</b>: Generate Minecraft tag JSON files for each trait, containing all items with that trait</li>
 *   <li><b>Registry Freezing</b>: Freeze all trait registries to make them immutable for the remainder of execution</li>
 * </ol>
 * 
 * <h2>Behavior Tags</h2>
 * <p>Behavior tags are Minecraft item tags automatically generated from trait definitions. They enable
 * vanilla compatibility by allowing vanilla entities and other mods to recognize modded items through
 * standard Minecraft tag checking mechanisms.
 * 
 * <p>Example: A trait "fish_food" mapped to tag "tapestry:fish_items" will generate a tag file at
 * {@code data/tapestry/tags/items/fish_items.json} containing all items that possess the "fish_food" trait.
 * 
 * <h2>Tag Format</h2>
 * <p>Generated tags follow standard Minecraft tag format:
 * <pre>{@code
 * {
 *   "replace": false,
 *   "values": [
 *     "minecraft:cod",
 *     "minecraft:salmon",
 *     "animalfriendly:nori"
 *   ]
 * }
 * }</pre>
 * 
 * <p>The {@code replace: false} setting allows datapack merging, so manual tag additions
 * from datapacks will be merged with generated tags.
 * 
 * @see com.tapestry.gameplay.traits.TraitSystem
 * @see com.tapestry.gameplay.items.ItemRegistration
 */
package com.tapestry.gameplay.composition;
