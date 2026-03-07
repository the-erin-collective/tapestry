/**
 * Vanilla behavior patches for Tapestry Gameplay API.
 * 
 * <p>This package contains infrastructure for patching vanilla entity behavior
 * to recognize modded items through behavior tags. The patch system uses Fabric
 * mixins to replace hardcoded item checks with tag-based checks.
 * 
 * <h2>Architecture</h2>
 * <p>During the COMPOSITION phase, behavior tags are generated from trait definitions.
 * These tags are then used by vanilla entities to recognize modded items without
 * requiring explicit integration code.
 * 
 * <h2>Implementation Status</h2>
 * <p>The behavior tag generation system is fully implemented. The mixin infrastructure
 * is documented but not yet implemented. See {@code VanillaBehaviorPatches.md} for
 * detailed implementation guidance.
 * 
 * <h2>Manual Integration</h2>
 * <p>Until mixins are implemented, behavior tags can be used manually:
 * <ul>
 *   <li>Add items to behavior tags through datapacks</li>
 *   <li>Check behavior tags in custom entity code</li>
 *   <li>Use Fabric's entity interaction events</li>
 * </ul>
 * 
 * <h2>Future Work</h2>
 * <p>When implementing mixins, target these vanilla entities:
 * <ul>
 *   <li>CatEntity - fish food recognition</li>
 *   <li>CowEntity - breeding item recognition</li>
 *   <li>ChickenEntity - breeding item recognition</li>
 *   <li>DolphinEntity - fish food recognition</li>
 *   <li>AxolotlEntity - food recognition</li>
 * </ul>
 * 
 * @see com.tapestry.gameplay.composition.BehaviorTagGenerator
 * @see com.tapestry.gameplay.traits.TraitSystem
 */
package com.tapestry.gameplay.patches;
