package com.tapestry.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tag.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TapestryTagInjectorTest {
    private Map<ResourceLocation, Tag.Builder> tags;
    private Tag.Builder builderMock;

    @BeforeEach
    void setup() {
        tags = new HashMap<>();
        builderMock = mock(Tag.Builder.class);
    }

    @Test
    void injectAddsMergeForKnownTags() {
        ResourceLocation vanillaFish = new ResourceLocation("minecraft", "fish");
        tags.put(vanillaFish, builderMock);

        TapestryTagInjector.inject(tags);

        // verify that addTag was called with tapestry:fish_items
        ArgumentCaptor<ResourceLocation> captor = ArgumentCaptor.forClass(ResourceLocation.class);
        verify(builderMock).addTag(captor.capture());
        assertEquals("tapestry:fish_items", captor.getValue().toString());
    }

    @Test
    void injectSkipsMissingVanillaTag() {
        // no entry for vanilla tag -> should not crash
        TapestryTagInjector.inject(tags);
        // nothing to verify beyond absence of exception
    }

    @Test
    void dynamicRegistryEntriesAreMerged() {
        ResourceLocation vanillaFoo = new ResourceLocation("minecraft", "foo");
        tags.put(vanillaFoo, builderMock);

        VanillaTagMergeRegistry.register("tapestry:bar", "minecraft:foo");
        TapestryTagInjector.inject(tags);

        verify(builderMock).addTag(new ResourceLocation("tapestry:bar"));
    }
}
