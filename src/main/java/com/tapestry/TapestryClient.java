package com.tapestry;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side Tapestry initialization.
 * Handles client-specific lifecycle and services.
 */
public class TapestryClient implements ClientModInitializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryClient.class);
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Tapestry client initialization started");
        
        // Only run client-specific initialization
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            try {
                // Initialize client-side components
                initializeClientComponents();
                
                LOGGER.info("Tapestry client initialization completed");
            } catch (Exception e) {
                LOGGER.error("Tapestry client initialization failed", e);
                throw new RuntimeException("Failed to initialize Tapestry client", e);
            }
        } else {
            LOGGER.warn("Tapestry client entrypoint called on server - skipping");
        }
    }
    
    /**
     * Initialize client-specific components.
     */
    private void initializeClientComponents() {
        // TODO: Move client-specific initialization here
        // This will include:
        // - Client lifecycle event registration
        // - Client-side service initialization
        // - Client-specific API setup
        // - Phase transitions to CLIENT_PRESENTATION_READY
        // - Event emission for client-side mods (engine:runtimeStart)
        LOGGER.info("Client components initialized");
    }
}
