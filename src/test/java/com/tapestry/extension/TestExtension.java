package com.tapestry.extension;

import java.util.List;
import java.util.Map;

/**
 * Test extension for unit testing the extension system.
 * This extension demonstrates how to properly implement TapestryExtensionProvider.
 */
public class TestExtension implements TapestryExtensionProvider {
    
    @Override
    public TapestryExtensionDescriptor describe() {
        return new TapestryExtensionDescriptor(
            "testExtension",
            List.of("worlds.fog", "worldgen.custom")
        );
    }
    
    @Override
    public void register(TapestryExtensionContext context) {
        // Extend core domains
        context.extendDomain("worlds", "fog", Map.of(
            "density", 0.5,
            "color", "gray"
        ));
        
        context.extendDomain("worldgen", "custom", Map.of(
            "enabled", true,
            "rarity", 0.1
        ));
        
        // Register mod-owned APIs
        context.registerModAPI("config", Map.of(
            "debug", false,
            "version", "1.0.0"
        ));
        
        context.registerModAPI("utils", Map.of(
            "helper", "testHelper"
        ));
    }
}
