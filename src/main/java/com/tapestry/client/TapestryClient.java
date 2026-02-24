package com.tapestry.client;

import net.fabricmc.api.ClientModInitializer;
import com.tapestry.lifecycle.TapestryPhase;
import com.tapestry.lifecycle.PhaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Client-side Tapestry initialization.
 * Handles CLIENT_PRESENTATION_READY phase transition via Fabric entrypoint.
 */
public class TapestryClient implements ClientModInitializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryClient.class);
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Client-side initialization started");
        
        try {
            // Check if phase is already CLIENT_PRESENTATION_READY before advancing
            var currentPhase = PhaseController.getInstance().getCurrentPhase();
            if (currentPhase != TapestryPhase.CLIENT_PRESENTATION_READY) {
                // Authoritative phase orchestration via Fabric entrypoint
                PhaseController.getInstance().advanceTo(TapestryPhase.CLIENT_PRESENTATION_READY);
                LOGGER.info("Client-side initialization completed - CLIENT_PRESENTATION_READY phase triggered");
            } else {
                LOGGER.info("CLIENT_PRESENTATION_READY phase already reached - skipping transition");
            }
            
            // Note: engine:runtimeStart event is emitted from server-side initialization
            // Client-side mods like TWILA will receive it there
            
        } catch (IllegalStateException e) {
            LOGGER.warn("CLIENT_PRESENTATION_READY phase transition failed: {}", e.getMessage());
            LOGGER.info("PhaseController not yet initialized - client will wait for server initialization");
        } catch (Exception e) {
            LOGGER.error("Unexpected error during client initialization", e);
        }
    }
}
