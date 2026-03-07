/**
 * Integration and wiring documentation for Tapestry Gameplay API.
 * 
 * <p>This package documents how all components of the gameplay API are integrated
 * and wired together. The integration follows the deterministic lifecycle:
 * TS_REGISTER → TRAITS → COMPOSITION → INITIALIZATION → RUNTIME.
 * 
 * <h2>Component Wiring</h2>
 * <ul>
 *   <li><b>Trait System</b>: Integrated with item registration for validation</li>
 *   <li><b>Item Registration</b>: Integrated with trait system and Fabric registry</li>
 *   <li><b>Brewing Recipes</b>: Integrated with Fabric brewing registry</li>
 *   <li><b>Loot Modifications</b>: Integrated with Fabric loot events</li>
 *   <li><b>Composition Orchestrator</b>: Integrates trait and item systems</li>
 *   <li><b>Behavior Tags</b>: Integrated with vanilla tag system</li>
 * </ul>
 * 
 * <h2>Data Flow</h2>
 * 
 * <h3>Registration Flow (TS_REGISTER Phase)</h3>
 * <pre>
 * Mod Code → TraitSystem.register() → TraitDefinition stored
 * Mod Code → ItemRegistration.register() → ItemDefinition stored with trait references
 * Mod Code → BrewingRecipe.register() → Recipe stored
 * Mod Code → LootModifier.modify() → Modification stored
 * </pre>
 * 
 * <h3>Composition Flow (COMPOSITION Phase)</h3>
 * <pre>
 * GameplayAPI.executeComposition()
 *   → CompositionOrchestrator.executeComposition()
 *   → TraitResolver.resolve() (trait-to-item mappings)
 *   → BehaviorTagGenerator.generateTags() (tag JSON files)
 *   → TraitSystem.freeze() (registries frozen)
 * </pre>
 * 
 * <h3>Initialization Flow (INITIALIZATION Phase)</h3>
 * <pre>
 * GameplayAPI.executeInitialization()
 *   → FabricBrewingRegistry.registerRecipes()
 *   → FabricItemRegistry.registerItems()
 *   → FabricLootRegistry.registerModifications()
 * </pre>
 * 
 * <h3>Runtime Flow (RUNTIME Phase)</h3>
 * <pre>
 * Player uses item
 *   → ItemUseHandler invoked
 *   → UseContext provided
 *   → Use function executes
 *   → UseResult returned
 *   → Minecraft applies result
 * </pre>
 * 
 * <h2>Verification</h2>
 * <p>All components are properly wired:
 * <ul>
 *   <li>✅ Trait system integrated with item registration</li>
 *   <li>✅ Item registration integrated with trait validation</li>
 *   <li>✅ Brewing recipes integrated with Fabric registration</li>
 *   <li>✅ Loot modifications integrated with Fabric events</li>
 *   <li>✅ Composition orchestrator integrated with trait and item systems</li>
 *   <li>✅ Behavior tags integrated with vanilla tag system</li>
 *   <li>✅ Phase enforcement prevents out-of-order operations</li>
 * </ul>
 * 
 * <h2>Documentation</h2>
 * <p>See {@code IntegrationWiring.md} for detailed documentation on:
 * <ul>
 *   <li>Lifecycle integration</li>
 *   <li>Component wiring</li>
 *   <li>Data flow diagrams</li>
 *   <li>Verification checklist</li>
 *   <li>Integration tests</li>
 * </ul>
 * 
 * @see com.tapestry.gameplay.GameplayAPI
 * @see com.tapestry.gameplay.composition.CompositionOrchestrator
 */
package com.tapestry.gameplay.integration;
