package com.tapestry.gameplay.patches.mixins;

import com.tapestry.gameplay.patches.TapestryTags;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to patch CatEntity to use behavior tags instead of hardcoded fish items.
 * Replaces the check for COD and SALMON with a check for FISH_ITEMS tag.
 */
@Mixin(CatEntity.class)
public class CatEntityMixin {

    /**
     * Injects into the isBreedingItem method to check for fish items using behavior tags.
     * This allows modded fish items to work with cats for breeding/taming.
     */
    @Inject(method = "isBreedingItem", at = @At("HEAD"), cancellable = true)
    private void onIsBreedingItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Determine default result based on tags
        boolean defaultAllow = false;
        if (stack.isIn(TapestryTags.FISH_ITEMS)) {
            defaultAllow = true;
        } else if (stack.isIn(TapestryTags.CAT_FOODS)) {
            defaultAllow = true;
        }

        // Build context and invoke hooks
        // obtain item identifier via registry
        String itemId = stack.getItem().toString();
        com.tapestry.gameplay.entities.FeedAttemptContext.ItemInfo itemInfo =
                new com.tapestry.gameplay.entities.FeedAttemptContext.ItemInfo(itemId, stack.getCount());
        // entity type is always cat for this mixin
        com.tapestry.gameplay.entities.FeedAttemptContext.EntityInfo entityInfo =
                new com.tapestry.gameplay.entities.FeedAttemptContext.EntityInfo("minecraft:cat", ((CatEntity)(Object)this).getUuidAsString());
        com.tapestry.gameplay.entities.FeedAttemptContext context =
                new com.tapestry.gameplay.entities.FeedAttemptContext(itemInfo, entityInfo);

        com.tapestry.gameplay.entities.EntityInteractionHookRegistry.getInstance()
                .invokeFeedAttemptHandlers("minecraft:cat", context);

        if (context.getResult() == com.tapestry.gameplay.entities.FeedAttemptContext.FeedResult.ACCEPTED) {
            cir.setReturnValue(true);
            return;
        }
        if (context.getResult() == com.tapestry.gameplay.entities.FeedAttemptContext.FeedResult.REJECTED) {
            cir.setReturnValue(false);
            return;
        }

        // no decision from hooks, fall back to default
        if (defaultAllow) {
            cir.setReturnValue(true);
        }
    }
}