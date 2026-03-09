package com.tapestry.gameplay.patch;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Default implementation of {@link PatchContext} used at runtime.
 *
 * <p>This implementation delegates to Fabric/Minecraft infrastructure
 * to answer questions about loaded mods and registry contents.  It is
 * intentionally conservative – unrecognised registry identifiers simply
 * return false or null rather than throwing exceptions.</p>
 */
public class DefaultPatchContext implements PatchContext {
    @Override
    public boolean isModLoaded(String modId) {
        if (modId == null) {
            return false;
        }
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean registryContains(Identifier id) {
        if (id == null) {
            return false;
        }

        try {
            // try to locate the entry in any well-known registry
            if (Registries.ITEM.containsId(id)) return true;
            if (Registries.BLOCK.containsId(id)) return true;
            // note: loot tables are handled via Fabric events rather than direct
            // registry lookup, so we do not check Registries.LOOT_TABLE here.
            if (Registries.VILLAGER_PROFESSION.containsId(id)) return true;
            if (Registries.ENTITY_TYPE.containsId(id)) return true;
            // additional registries may be added as needed
        } catch (Exception e) {
            // ignore - registry might not be present in this environment
        }
        return false;
    }

    @Override
    public boolean traitExists(Identifier traitId) {
        // traits are managed by the trait system; we don't have a direct
        // dependency here, so simply return false by default.  The caller
        // should ensure a TraitContext is provided if trait checks are
        // required.
        return false;
    }

    @Override
    public Registry<?> getRegistry(Identifier registryId) {
        // runtime lookup of arbitrary registries is not required by the
        // current patch engine implementation; mods may provide their own
        // context if they need this functionality.  We simply return null.
        return null;
    }
}
