/**
 * Tapestry Gameplay API Expansion.
 * 
 * <p>This package provides a minimal, stable gameplay API layer that enables TypeScript-first
 * Minecraft mod development with emergent cross-mod compatibility.</p>
 * 
 * <h2>Core Features</h2>
 * <ul>
 *   <li><b>Trait System</b> - Emergent cross-mod compatibility through behavioral classifications</li>
 *   <li><b>Item Registration</b> - Stable API for registering custom items with trait support</li>
 *   <li><b>Brewing Recipes</b> - Register custom potion brewing recipes</li>
 *   <li><b>Loot Modification</b> - Modify loot tables without file replacement</li>
 * </ul>
 * 
 * <h2>Design Philosophy</h2>
 * <ul>
 *   <li>Stable abstraction layer over Fabric APIs</li>
 *   <li>Data-driven design compatible with vanilla datapacks</li>
 *   <li>Deterministic lifecycle with dedicated COMPOSITION phase</li>
 *   <li>Minimal API surface for ease of use</li>
 * </ul>
 * 
 * <h2>Lifecycle Phases</h2>
 * <ol>
 *   <li><b>TS_REGISTER</b> - Items, traits, recipes registered</li>
 *   <li><b>TRAITS</b> - Trait definitions validated</li>
 *   <li><b>COMPOSITION</b> - Trait resolution, tag generation, registry freezing</li>
 *   <li><b>INITIALIZATION</b> - Capability resolution, vanilla patches applied</li>
 *   <li><b>RUNTIME</b> - Normal gameplay</li>
 * </ol>
 * 
 * @see com.tapestry.gameplay.GameplayAPI
 * @see com.tapestry.gameplay.traits.TraitSystem
 * @see com.tapestry.gameplay.items.ItemRegistration
 * @see com.tapestry.gameplay.brewing.BrewingRecipe
 * @see com.tapestry.gameplay.loot.LootModifier
 */
package com.tapestry.gameplay;
