package com.tapestry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
        LOGGER.info("Initializing server-specific components");
        
        // Register server lifecycle events for additional server-specific handling
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Server starting - performing server-specific setup");
            // Additional server-side setup can be added here
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping - cleaning up server-specific resources");
            // Server cleanup can be added here
        });
        
        // Initialize server-specific services that might not be covered in TapestryMod
        initializeServerSpecificServices();
        
        // Register server-specific event handlers
        registerServerEventHandlers();
        
        LOGGER.info("Server components initialized");
    }
    
    /**
     * Initialize server-specific services.
     */
    private void initializeServerSpecificServices() {
        // This method can be used for services that are specifically server-only
        // and not handled in the main TapestryMod initialization
        
        LOGGER.info("Server-specific services initialized");
        
        // Examples of what could be added:
        // - Server-wide configuration management
        // - Server analytics or metrics
        // - Server-side caching systems
        // - Cross-world data synchronization
    }
    
    /**
     * Register server-specific event handlers.
     */
    private void registerServerEventHandlers() {
        LOGGER.info("Registering server-specific event handlers");
        
        // Register server tick events for server-specific periodic tasks
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started - registering server-specific tick handlers");
            
            // Register server tick handler for server-specific periodic tasks
            ServerTickEvents.END_SERVER_TICK.register(tickingServer -> {
                // Server-specific periodic tasks can be added here
                // Examples: cleanup tasks, metrics collection, etc.
            });
        });
        
        // Register server-specific world events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Register world load/unload events if needed
            LOGGER.info("Server world event handlers registered");
        });
        
        LOGGER.info("Server event handlers implemented");
    }
}
