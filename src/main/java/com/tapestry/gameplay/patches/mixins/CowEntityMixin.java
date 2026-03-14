package com.tapestry.gameplay.patches.mixins;

import com.tapestry.gameplay.patches.TapestryTags;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to patch CowEntity to use behavior tags instead of hardcoded wheat.
 * Replaces the check for WHEAT with a check for COW_BREEDING_ITEMS tag.
 */
@Mixin(CowEntity.class)
public class CowEntityMixin {

    /**
     * Injects into the isBreedingItem method to check for cow breeding items using behavior tags.
     * This allows modded items to work with cows for breeding.
     */
    @Inject(method = "isBreedingItem", at = @At("HEAD"), cancellable = true)
    private void onIsBreedingItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        boolean defaultAllow = stack.isIn(TapestryTags.COW_BREEDING_ITEMS);

        // Build context
        String itemId = stack.getItem().toString();
        com.tapestry.gameplay.entities.FeedAttemptContext.ItemInfo itemInfo =
                new com.tapestry.gameplay.entities.FeedAttemptContext.ItemInfo(itemId, stack.getCount());
        com.tapestry.gameplay.entities.FeedAttemptContext.EntityInfo entityInfo =
                new com.tapestry.gameplay.entities.FeedAttemptContext.EntityInfo("minecraft:cow", ((CowEntity)(Object)this).getUuidAsString());
        com.tapestry.gameplay.entities.FeedAttemptContext context =
                new com.tapestry.gameplay.entities.FeedAttemptContext(itemInfo, entityInfo);

        com.tapestry.gameplay.entities.EntityInteractionHookRegistry.getInstance()
                .invokeFeedAttemptHandlers("minecraft:cow", context);

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