package com.tapestry.gameplay.patch;

import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Provides environment information for patch condition evaluation.
 * 
 * <p>The context allows conditions to query the runtime environment,
 * including mod presence, registry contents, and trait existence. This
 * enables conditional patch application based on the actual game state.</p>
 * 
 * <p>Context is provided by the patch engine during patch application,
 * after datapack content has been loaded. This ensures that registry-dependent
 * conditions can access datapack data.</p>
 */
public interface PatchContext {
    /**
     * Checks if a mod with the specified identifier is loaded.
     * 
     * @param modId The mod identifier to check
     * @return true if the mod is loaded, false otherwise
     */
    boolean isModLoaded(String modId);
    
    /**
     * Checks if a registry contains an entry with the specified identifier.
     * 
     * <p>This method is evaluated after datapack load, ensuring that
     * datapack-provided registry entries are available for checking.</p>
     * 
     * @param id The identifier to check in the registry
     * @return true if any registry contains the entry, false otherwise
     */
    boolean registryContains(Identifier id);
    
    /**
     * Checks if a trait with the specified identifier exists.
     * 
     * @param traitId The trait identifier to check
     * @return true if the trait exists, false otherwise
     */
    boolean traitExists(Identifier traitId);
    
    /**
     * Retrieves a registry by its identifier.
     * 
     * <p>This method provides access to specific registries for advanced
     * condition evaluation.</p>
     * 
     * @param registryId The registry identifier
     * @return The registry, or null if not found
     */
    Registry<?> getRegistry(Identifier registryId);
}
