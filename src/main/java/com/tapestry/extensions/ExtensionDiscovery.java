package com.tapestry.extensions;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Discovers Tapestry extension providers from Fabric entrypoints.
 * Collects descriptors without any side effects.
 */
public class ExtensionDiscovery {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionDiscovery.class);
    
    /**
     * Discovers all extension providers from Fabric entrypoints.
     * 
     * @return list of discovered providers, sorted deterministically
     */
    public static List<DiscoveredExtensionProvider> discoverProviders() {
        List<DiscoveredExtensionProvider> providers = new ArrayList<>();
        
        try {
            var fabricLoader = FabricLoader.getInstance();
            if (fabricLoader == null) {
                LOGGER.debug("FabricLoader not available in test environment");
                return providers;
            }
            
            var entrypointContainers = fabricLoader.getEntrypointContainers(
                "tapestry:extensions", 
                TapestryExtensionProvider.class
            );
            
            for (var container : entrypointContainers) {
                try {
                    var provider = container.getEntrypoint();
                    var descriptor = provider.describe();
                    var modContainer = container.getProvider();
                    
                    providers.add(new DiscoveredExtensionProvider(
                        provider, 
                        modContainer, 
                        descriptor
                    ));
                    
                    LOGGER.debug("Discovered extension provider from mod: {}", 
                        modContainer.getMetadata().getId());
                        
                } catch (Exception e) {
                    LOGGER.error("Failed to load extension provider from mod: {}", 
                        container.getProvider().getMetadata().getId(), e);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to discover extension providers", e);
        }
        
        // Sort deterministically by (descriptor.id, sourceModId)
        providers.sort((a, b) -> {
            int idCompare = a.descriptor().id().compareTo(b.descriptor().id());
            if (idCompare != 0) return idCompare;
            
            return a.sourceMod().getMetadata().getId()
                .compareTo(b.sourceMod().getMetadata().getId());
        });
        
        LOGGER.info("Discovered {} extension providers", providers.size());
        return providers;
    }
    
    /**
     * Extracts descriptors from discovered providers.
     * 
     * @param providers list of discovered providers
     * @return map of extension ID to descriptor
     */
    public static TreeMap<String, TapestryExtensionDescriptor> extractDescriptors(
            List<DiscoveredExtensionProvider> providers) {
        
        TreeMap<String, TapestryExtensionDescriptor> descriptors = new TreeMap<>();
        
        for (var provider : providers) {
            var descriptor = provider.descriptor();
            descriptors.put(descriptor.id(), descriptor);
        }
        
        return descriptors;
    }
}
