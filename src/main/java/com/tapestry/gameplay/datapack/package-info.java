/**
 * Datapack integration support for Tapestry Gameplay API.
 * 
 * <p>This package documents Tapestry's integration with standard Minecraft datapack
 * formats. The framework is designed to work seamlessly with vanilla datapacks and
 * does not interfere with standard loading mechanisms.
 * 
 * <h2>Supported Formats</h2>
 * <ul>
 *   <li><b>Recipes</b>: Standard JSON recipe formats in {@code data/{namespace}/recipes/}</li>
 *   <li><b>Tags</b>: Standard JSON tag formats in {@code data/{namespace}/tags/items/}</li>
 *   <li><b>Loot Tables</b>: Standard JSON loot table formats in {@code data/{namespace}/loot_tables/}</li>
 *   <li><b>Assets</b>: Standard resource pack formats in {@code assets/{namespace}/}</li>
 * </ul>
 * 
 * <h2>Integration Principles</h2>
 * <ul>
 *   <li><b>Non-Interference</b>: Tapestry does not modify vanilla datapack loading</li>
 *   <li><b>Additive Merging</b>: Generated tags use {@code replace: false} for merging</li>
 *   <li><b>Event-Based Modification</b>: Loot modifications use Fabric events</li>
 *   <li><b>Standard Formats</b>: All generated files use vanilla formats</li>
 * </ul>
 * 
 * <h2>Behavior Tag Merging</h2>
 * <p>Tapestry generates behavior tags during the COMPOSITION phase with
 * {@code replace: false}, allowing manual datapack additions to merge:
 * 
 * <pre>{@code
 * // Generated: data/tapestry/tags/items/fish_items.json
 * {
 *   "replace": false,
 *   "values": ["minecraft:cod", "minecraft:salmon"]
 * }
 * 
 * // Manual: data/mymod/tags/items/fish_items.json
 * {
 *   "replace": false,
 *   "values": ["mymod:custom_fish"]
 * }
 * 
 * // Merged result: [cod, salmon, custom_fish]
 * }</pre>
 * 
 * <h2>Loot Table Modification</h2>
 * <p>Loot modifications are applied during datapack reload events using
 * Fabric's {@code LootTableEvents.MODIFY}, ensuring compatibility with
 * other mods and datapacks.
 * 
 * <h2>Documentation</h2>
 * <p>See {@code DatapackIntegration.md} for detailed documentation on:
 * <ul>
 *   <li>Supported datapack formats</li>
 *   <li>Integration principles</li>
 *   <li>Directory structure</li>
 *   <li>Usage guidelines</li>
 *   <li>Troubleshooting</li>
 * </ul>
 * 
 * @see com.tapestry.gameplay.composition.BehaviorTagGenerator
 * @see com.tapestry.gameplay.loot.LootModifier
 */
package com.tapestry.gameplay.datapack;
