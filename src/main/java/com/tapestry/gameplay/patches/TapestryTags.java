package com.tapestry.gameplay.patches;

import net.minecraft.registry.tag.TagKey;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

/**
 * Tag constants for Tapestry behavior tags.
 * These tags are used by mixins to replace hardcoded item checks in vanilla entities.
 */
public final class TapestryTags {

    // Behavior tags generated from traits
    public static final TagKey<Item> CAT_FOODS = TagKey.of(net.minecraft.registry.RegistryKeys.ITEM, Identifier.of("tapestry", "cat_foods"));
    public static final TagKey<Item> WOLF_FOODS = TagKey.of(net.minecraft.registry.RegistryKeys.ITEM, Identifier.of("tapestry", "wolf_foods"));
    public static final TagKey<Item> AXOLOTL_FOODS = TagKey.of(net.minecraft.registry.RegistryKeys.ITEM, Identifier.of("tapestry", "axolotl_foods"));
    public static final TagKey<Item> DOLPHIN_FOODS = TagKey.of(net.minecraft.registry.RegistryKeys.ITEM, Identifier.of("tapestry", "dolphin_foods"));
    public static final TagKey<Item> FISH_ITEMS = TagKey.of(net.minecraft.registry.RegistryKeys.ITEM, Identifier.of("tapestry", "fish_items"));

    // Additional behavior tags for breeding items
    public static final TagKey<Item> COW_BREEDING_ITEMS = TagKey.of(net.minecraft.registry.RegistryKeys.ITEM, Identifier.of("tapestry", "cow_breeding_items"));
    public static final TagKey<Item> CHICKEN_BREEDING_ITEMS = TagKey.of(net.minecraft.registry.RegistryKeys.ITEM, Identifier.of("tapestry", "chicken_breeding_items"));

    private TapestryTags() {
        // Utility class
    }
}