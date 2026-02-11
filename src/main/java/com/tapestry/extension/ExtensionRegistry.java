package com.tapestry.extension;

import com.tapestry.api.TapestryAPI;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry for discovering and managing Tapestry extensions.
 * 
 * This class handles the discovery of extensions via Fabric entrypoints,
 * validation of their descriptors, and registration during the appropriate phase.
 */
public class ExtensionRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionRegistry.class);
    
    private final TapestryAPI api;
    private final List<DiscoveredExtension> discoveredExtensions;
    private final Set<String> registeredIds;
    private final Set<String> registeredCapabilities;
    
    private volatile boolean discoveryComplete = false;
    private volatile boolean registrationComplete = false;
    
    public ExtensionRegistry(TapestryAPI api) {
        this.api = api;
        this.discoveredExtensions = new ArrayList<>();
        this.registeredIds = new HashSet<>();
        this.registeredCapabilities = new HashSet<>();
    }
    
    /**
     * Discovers all Tapestry extensions via Fabric entrypoints.
     * Must be called during DISCOVERY phase.
     * 
     * @throws IllegalStateException if called outside DISCOVERY phase or after FREEZE
     */
    public void discoverExtensions() {
        PhaseController.getInstance().requirePhase(TapestryPhase.DISCOVERY);
        
        // Defensive check: prevent discovery after FREEZE
        PhaseController.getInstance().requireAtMost(TapestryPhase.REGISTRATION);
        
        if (discoveryComplete) {
            LOGGER.warn("Extension discovery already completed");
            return;
        }
        
        LOGGER.info("Starting extension discovery");
        
        // Get all entrypoints for "tapestry:extension"
        List<TapestryExtensionProvider> providers = FabricLoader.getInstance()
            .getEntrypoints("tapestry:extension", TapestryExtensionProvider.class);
        
        LOGGER.info("Found {} Tapestry extension providers", providers.size());
        
        for (TapestryExtensionProvider provider : providers) {
            try {
                // Get descriptor (must be side-effect free)
                TapestryExtensionDescriptor descriptor = provider.describe();
                
                // Validate descriptor
                descriptor.validate();
                
                // Check for duplicate IDs
                if (registeredIds.contains(descriptor.id())) {
                    throw new IllegalStateException(
                        String.format("Duplicate extension ID: %s", descriptor.id())
                    );
                }
                
                // Check for duplicate capabilities
                for (String capability : descriptor.capabilities()) {
                    if (registeredCapabilities.contains(capability)) {
                        throw new IllegalStateException(
                            String.format("Duplicate capability: %s (from extension %s)", 
                                capability, descriptor.id())
                        );
                    }
                    registeredCapabilities.add(capability);
                }
                
                DiscoveredExtension extension = new DiscoveredExtension(provider, descriptor);
                discoveredExtensions.add(extension);
                registeredIds.add(descriptor.id());
                
                LOGGER.info("Discovered extension: {} with capabilities: {}", 
                    descriptor.id(), descriptor.capabilities());
                
            } catch (Exception e) {
                LOGGER.error("Failed to discover extension from provider {}: {}", 
                    provider.getClass().getName(), e.getMessage(), e);
                throw new RuntimeException("Extension discovery failed", e);
            }
        }
        
        // Sort extensions deterministically by ID
        discoveredExtensions.sort(Comparator.comparing(e -> e.descriptor.id()));
        
        discoveryComplete = true;
        LOGGER.info("Extension discovery completed. Found {} extensions: {}", 
            discoveredExtensions.size(),
            discoveredExtensions.stream()
                .map(e -> e.descriptor.id())
                .collect(Collectors.joining(", ")));
    }
    
    /**
     * Registers all discovered extensions.
     * Must be called during REGISTRATION phase.
     * 
     * @throws IllegalStateException if called outside REGISTRATION phase or before discovery
     */
    public void registerExtensions() {
        PhaseController.getInstance().requirePhase(TapestryPhase.REGISTRATION);
        
        // Defensive check: prevent registration after FREEZE
        PhaseController.getInstance().requireAtMost(TapestryPhase.REGISTRATION);
        
        if (!discoveryComplete) {
            throw new IllegalStateException("Must complete discovery before registration");
        }
        
        if (registrationComplete) {
            LOGGER.warn("Extension registration already completed");
            return;
        }
        
        LOGGER.info("Starting extension registration");
        
        for (DiscoveredExtension discovered : discoveredExtensions) {
            try {
                LOGGER.info("Registering extension: {}", discovered.descriptor.id());
                
                TapestryExtensionContext context = 
                    new TapestryExtensionContext(discovered.descriptor.id(), api);
                
                discovered.provider.register(context);
                
                LOGGER.info("Successfully registered extension: {}", discovered.descriptor.id());
                
            } catch (Exception e) {
                LOGGER.error("Failed to register extension {}: {}", 
                    discovered.descriptor.id(), e.getMessage(), e);
                throw new RuntimeException("Extension registration failed", e);
            }
        }
        
        registrationComplete = true;
        LOGGER.info("Extension registration completed for {} extensions", 
            discoveredExtensions.size());
    }
    
    /**
     * Gets the number of discovered extensions.
     * 
     * @return the number of discovered extensions
     */
    public int getExtensionCount() {
        return discoveredExtensions.size();
    }
    
    /**
     * Gets the IDs of all discovered extensions.
     * 
     * @return an unmodifiable set of extension IDs
     */
    public Set<String> getExtensionIds() {
        return Collections.unmodifiableSet(registeredIds);
    }
    
    /**
     * Gets all registered capabilities.
     * 
     * @return an unmodifiable set of capability strings
     */
    public Set<String> getRegisteredCapabilities() {
        return Collections.unmodifiableSet(registeredCapabilities);
    }
    
    /**
     * Checks if discovery is complete.
     * 
     * @return true if discovery has completed
     */
    public boolean isDiscoveryComplete() {
        return discoveryComplete;
    }
    
    /**
     * Checks if registration is complete.
     * 
     * @return true if registration has completed
     */
    public boolean isRegistrationComplete() {
        return registrationComplete;
    }
    
    /**
     * Internal class representing a discovered extension.
     */
    private static class DiscoveredExtension {
        final TapestryExtensionProvider provider;
        final TapestryExtensionDescriptor descriptor;
        
        DiscoveredExtension(TapestryExtensionProvider provider, 
                          TapestryExtensionDescriptor descriptor) {
            this.provider = provider;
            this.descriptor = descriptor;
        }
    }
}
