package com.tapestry.mixin;

import com.tapestry.behavior.TapestryTagInjector;
import net.minecraft.resource.ResourceManager;
import net.minecraft.tag.TagManagerLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Mixin applied to {@link TagManagerLoader} in order to hook the end of
 * the datapack tag load process.  Allows Tapestry to add its own tags into
 * vanilla tag builders before they are finalized.
 */
@Mixin(TagManagerLoader.class)
public class TagLoaderMixin {

    @Inject(method = "apply", at = @At("TAIL"))
    private void tapestry$injectTags(
        Map<ResourceLocation, Tag.Builder> tags,
        ResourceManager resourceManager,
        Profiler profiler,
        CallbackInfo ci
    ) {
        TapestryTagInjector.inject(tags);
    }
}
