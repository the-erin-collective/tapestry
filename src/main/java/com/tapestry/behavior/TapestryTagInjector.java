package com.tapestry.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Responsible for merging tapestry-generated tags into vanilla tags after
 * the datapack loader has finished.  The mixin in
 * com.tapestry.mixin.TagLoaderMixin invokes {@link #inject} at the end of
 * the tag loading process.
 */
public class TapestryTagInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryTagInjector.class);

    static {
        // register the common merges; external code may add more
        VanillaTagMergeRegistry.register("tapestry:fish_items", "minecraft:fish");
        VanillaTagMergeRegistry.register("tapestry:cat_foods", "minecraft:cat_food");
        VanillaTagMergeRegistry.register("tapestry:axolotl_foods", "minecraft:axolotl_food");
        VanillaTagMergeRegistry.register("tapestry:cow_breeding_items", "minecraft:cow_food");
        // chicken breeding uses a tag but trait registration may add it later
        VanillaTagMergeRegistry.register("tapestry:chicken_breeding_items", "minecraft:chicken_food");
    }

    /**
     * Perform merges on the given tag builders map.
     */
    public static void inject(Map<ResourceLocation, Tag.Builder> tags) {
        // static merges; kept for backwards compatibility
        merge(tags, "minecraft:fish", "tapestry:fish_items");
        merge(tags, "minecraft:cat_food", "tapestry:cat_foods");
        merge(tags, "minecraft:axolotl_food", "tapestry:axolotl_foods");
        merge(tags, "minecraft:cow_food", "tapestry:cow_breeding_items");

        // dynamic registry entries
        for (Map.Entry<String, String> entry : VanillaTagMergeRegistry.getMerges().entrySet()) {
            // registry maps tapestryTag -> vanillaTag
            merge(tags, entry.getValue(), entry.getKey());
        }
    }

    private static void merge(
        Map<ResourceLocation, Tag.Builder> tags,
        String vanillaTag,
        String tapestryTag
    ) {
        ResourceLocation vanilla = new ResourceLocation(vanillaTag);
        Tag.Builder builder = tags.get(vanilla);
        if (builder == null) {
            LOGGER.warn(
                "Tapestry could not merge tag {} into {} (target tag missing)",
                tapestryTag,
                vanillaTag
            );
            return;
        }
        builder.addTag(new ResourceLocation(tapestryTag));
        LOGGER.debug("Merged {} into {}", tapestryTag, vanillaTag);
    }
}
