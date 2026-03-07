/**
 * Documentation for service capability and gameplay trait separation.
 * 
 * <p>Tapestry uses two complementary systems with distinct purposes:
 * <ul>
 *   <li><b>Service Capabilities</b>: Platform services, API extensions, explicit dependencies</li>
 *   <li><b>Gameplay Traits</b>: Gameplay composition, emergent compatibility</li>
 * </ul>
 * 
 * <h2>Service Capabilities</h2>
 * <p><b>Purpose</b>: Platform services, API extensions, explicit mod dependencies
 * <p><b>Model</b>: One provider per capability, explicit dependencies
 * <p><b>Resolution</b>: INITIALIZATION phase
 * <p><b>Use Cases</b>: Database services, network APIs, logging, UI overlays
 * <p><b>API</b>:
 * <pre>{@code
 * capabilities.provideCapability("mymod:database", databaseService);
 * DatabaseService db = capabilities.requireCapability("mymod:database");
 * }</pre>
 * 
 * <h2>Gameplay Traits</h2>
 * <p><b>Purpose</b>: Gameplay composition, emergent compatibility
 * <p><b>Model</b>: Many providers, many consumers, implicit compatibility
 * <p><b>Resolution</b>: COMPOSITION phase
 * <p><b>Use Cases</b>: Item behaviors, entity interactions, crafting ingredients
 * <p><b>API</b>:
 * <pre>{@code
 * traits.register("fish_food", new TraitConfig("tapestry:fish_items"));
 * traits.consume("fish_food", new ConsumptionConfig("minecraft:cat", "food"));
 * items.register("mymod:nori", new ItemOptions().traits("fish_food"));
 * }</pre>
 * 
 * <h2>When to Use Each System</h2>
 * 
 * <h3>Use Service Capabilities When:</h3>
 * <ul>
 *   <li>Your mod requires another mod's API (explicit dependencies)</li>
 *   <li>Providing infrastructure for other mods (platform services)</li>
 *   <li>Only one mod should provide this functionality (one provider)</li>
 *   <li>You need strong typing and compile-time checks (type safety)</li>
 *   <li>You need version compatibility checks (versioning)</li>
 * </ul>
 * 
 * <h3>Use Gameplay Traits When:</h3>
 * <ul>
 *   <li>Items should work with systems without explicit integration (emergent compatibility)</li>
 *   <li>Multiple mods contribute items with same behavior (many providers)</li>
 *   <li>Multiple entities/systems use same trait (many consumers)</li>
 *   <li>Items should work with vanilla entities (vanilla integration)</li>
 *   <li>Items from different mods should work together (cross-mod compatibility)</li>
 * </ul>
 * 
 * <h2>Comparison</h2>
 * <table border="1">
 *   <tr>
 *     <th>Aspect</th>
 *     <th>Service Capabilities</th>
 *     <th>Gameplay Traits</th>
 *   </tr>
 *   <tr>
 *     <td>Purpose</td>
 *     <td>Platform services, explicit dependencies</td>
 *     <td>Gameplay composition, emergent compatibility</td>
 *   </tr>
 *   <tr>
 *     <td>Model</td>
 *     <td>One provider per capability</td>
 *     <td>Many providers, many consumers</td>
 *   </tr>
 *   <tr>
 *     <td>Resolution</td>
 *     <td>INITIALIZATION phase</td>
 *     <td>COMPOSITION phase</td>
 *   </tr>
 *   <tr>
 *     <td>Dependencies</td>
 *     <td>Explicit (requireCapability)</td>
 *     <td>Implicit (trait assignments)</td>
 *   </tr>
 *   <tr>
 *     <td>Type Safety</td>
 *     <td>Strong (Java types)</td>
 *     <td>Weak (string identifiers)</td>
 *   </tr>
 *   <tr>
 *     <td>Failure Mode</td>
 *     <td>Fail-fast (missing required capability)</td>
 *     <td>Graceful (missing trait = no compatibility)</td>
 *   </tr>
 * </table>
 * 
 * <h2>Documentation</h2>
 * <p>See {@code ServiceCapabilitySeparation.md} for detailed documentation on:
 * <ul>
 *   <li>Dual system architecture</li>
 *   <li>When to use each system</li>
 *   <li>Comparison table</li>
 *   <li>Code examples</li>
 *   <li>Anti-patterns to avoid</li>
 *   <li>Migration guide</li>
 * </ul>
 * 
 * @see com.tapestry.gameplay.traits.TraitSystem
 */
package com.tapestry.gameplay.capabilities;
