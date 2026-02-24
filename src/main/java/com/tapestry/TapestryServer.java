package com.tapestry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side Tapestry initialization.
 * Handles server-specific lifecycle and services.
 */
public class TapestryServer implements ModInitializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryServer.class);
    
    @Override
    public void onInitialize() {
        LOGGER.info("Tapestry server initialization started");
        
        // Only run server-specific initialization
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            try {
                // Initialize server-side components
                initializeServerComponents();
                
                LOGGER.info("Tapestry server initialization completed");
            } catch (Exception e) {
                LOGGER.error("Tapestry server initialization failed", e);
                throw new RuntimeException("Failed to initialize Tapestry server", e);
            }
        } else {
            LOGGER.warn("Tapestry server entrypoint called on client - skipping");
        }
    }
    
    /**
     * Initialize server-specific components.
     */
    private void initializeServerComponents() {
        // TODO: Move server-specific initialization here
        // This will include:
        // - Server lifecycle event registration
        // - Server-side service initialization  
        // - Server-specific API setup
        LOGGER.info("Server components initialized");
        
        // Register server-specific event handlers
        registerServerEventHandlers();
    }
    
    /**
     * Register server-specific event handlers.
     */
    private void registerServerEventHandlers() {
        // TODO: Implement server event handlers
        LOGGER.info("Server event handlers will be implemented");
    }
}
