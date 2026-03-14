package com.tapestry.gameplay.patches.mixins;

import com.tapestry.gameplay.patches.TapestryTags;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to patch DolphinEntity to use behavior tags instead of hardcoded fish items.
 * Replaces the check for COD and SALMON with a check for FISH_ITEMS tag.
 */
@Mixin(DolphinEntity.class)
public class DolphinEntityMixin {

    /**
     * Injects into the canEat method to check for dolphin food items using behavior tags.
     * This allows modded fish items to work with dolphins for feeding.
     */
    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void onCanEat(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        boolean defaultAllow = false;
        if (stack.isIn(TapestryTags.FISH_ITEMS)) {
            defaultAllow = true;
        } else if (stack.isIn(TapestryTags.DOLPHIN_FOODS)) {
            defaultAllow = true;
        }

        String itemId = stack.getItem().toString();
        com.tapestry.gameplay.entities.FeedAttemptContext.ItemInfo itemInfo =
                new com.tapestry.gameplay.entities.FeedAttemptContext.ItemInfo(itemId, stack.getCount());
        com.tapestry.gameplay.entities.FeedAttemptContext.EntityInfo entityInfo =
                new com.tapestry.gameplay.entities.FeedAttemptContext.EntityInfo("minecraft:dolphin", ((DolphinEntity)(Object)this).getUuidAsString());
        com.tapestry.gameplay.entities.FeedAttemptContext context =
                new com.tapestry.gameplay.entities.FeedAttemptContext(itemInfo, entityInfo);

        com.tapestry.gameplay.entities.EntityInteractionHookRegistry.getInstance()
                .invokeFeedAttemptHandlers("minecraft:dolphin", context);

        if (context.getResult() == com.tapestry.gameplay.entities.FeedAttemptContext.FeedResult.ACCEPTED) {
            cir.setReturnValue(true);
            return;
        }
        if (context.getResult() == com.tapestry.gameplay.entities.FeedAttemptContext.FeedResult.REJECTED) {
            cir.setReturnValue(false);
            return;
        }

        if (defaultAllow) {
            cir.setReturnValue(true);
        }
    }
}