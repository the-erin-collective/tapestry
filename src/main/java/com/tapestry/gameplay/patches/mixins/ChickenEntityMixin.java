package com.tapestry.gameplay.patches.mixins;

import com.tapestry.gameplay.patches.TapestryTags;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to patch ChickenEntity to use behavior tags instead of hardcoded seeds.
 * Replaces the check for various seeds with a check for CHICKEN_BREEDING_ITEMS tag.
 */
@Mixin(ChickenEntity.class)
public class ChickenEntityMixin {

    /**
     * Injects into the isBreedingItem method to check for chicken breeding items using behavior tags.
     * This allows modded seeds and items to work with chickens for breeding.
     */
    @Inject(method = "isBreedingItem", at = @At("HEAD"), cancellable = true)
    private void onIsBreedingItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        boolean defaultAllow = stack.isIn(TapestryTags.CHICKEN_BREEDING_ITEMS);

        String itemId = stack.getItem().toString();
        com.tapestry.gameplay.entities.FeedAttemptContext.ItemInfo itemInfo =
                new com.tapestry.gameplay.entities.FeedAttemptContext.ItemInfo(itemId, stack.getCount());
        com.tapestry.gameplay.entities.FeedAttemptContext.EntityInfo entityInfo =
                new com.tapestry.gameplay.entities.FeedAttemptContext.EntityInfo("minecraft:chicken", ((ChickenEntity)(Object)this).getUuidAsString());
        com.tapestry.gameplay.entities.FeedAttemptContext context =
                new com.tapestry.gameplay.entities.FeedAttemptContext(itemInfo, entityInfo);

        com.tapestry.gameplay.entities.EntityInteractionHookRegistry.getInstance()
                .invokeFeedAttemptHandlers("minecraft:chicken", context);

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