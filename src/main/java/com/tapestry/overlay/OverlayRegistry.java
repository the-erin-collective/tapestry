package com.tapestry.overlay;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for client-side overlays.
 * 
 * Responsibilities:
 * - Track registered overlays
 * - Enforce unique IDs per mod
 * - Maintain deterministic render order
 * - Handle overlay lifecycle (registration, visibility, removal)
 */
public class OverlayRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OverlayRegistry.class);
    private static OverlayRegistry instance;
    
    // Thread-safe storage for overlays
    private final Map<String, OverlayDefinition> overlays = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> modToOverlayIds = new ConcurrentHashMap<>();
    
    // Track registration order for deterministic sorting
    private final Map<String, Integer> registrationOrder = new ConcurrentHashMap<>();
    private int nextRegistrationOrder = 0;
    
    // Track disabled overlays (crashed during render)
    private final Set<String> disabledOverlays = new HashSet<>();
    
    private OverlayRegistry() {}
    
    /**
     * Gets the singleton instance.
     * 
     * @return the overlay registry instance
     */
    public static synchronized OverlayRegistry getInstance() {
        if (instance == null) {
            instance = new OverlayRegistry();
        }
        return instance;
    }
    
    /**
     * Registers a new overlay.
     * 
     * @param modId the mod ID registering the overlay
     * @param overlayId the unique overlay ID within the mod
     * @param definition the overlay definition
     * @throws IllegalStateException if registration is not allowed
     * @throws IllegalArgumentException if overlay ID is not unique
     */
    public void registerOverlay(String modId, String overlayId, OverlayDefinition definition) {
        // Phase enforcement: only allow registration during CLIENT_PRESENTATION_READY
        PhaseController.getInstance().requireAtLeast(TapestryPhase.CLIENT_PRESENTATION_READY);
        
        String fullId = createFullId(modId, overlayId);
        
        // Check for duplicate overlay ID
        if (overlays.containsKey(fullId)) {
            throw new IllegalArgumentException(
                String.format("Overlay ID '%s' is already registered by mod '%s'", 
                    overlayId, modId)
            );
        }
        
        // Validate the overlay definition
        validateOverlayDefinition(definition);
        
        // Store the overlay
        overlays.put(fullId, definition);
        modToOverlayIds.computeIfAbsent(modId, k -> new HashSet<>()).add(overlayId);
        registrationOrder.put(fullId, nextRegistrationOrder++);
        
        LOGGER.info("Registered overlay '{}' for mod '{}'", overlayId, modId);
    }
    
    /**
     * Gets all overlays in render order (sorted by z-index, then registration order).
     * 
     * @return list of overlays in render order
     */
    public List<OverlayEntry> getOverlaysInRenderOrder() {
        List<OverlayEntry> result = new ArrayList<>();
        
        for (Map.Entry<String, OverlayDefinition> entry : overlays.entrySet()) {
            String fullId = entry.getKey();
            OverlayDefinition definition = entry.getValue();
            
            // Skip disabled overlays
            if (disabledOverlays.contains(fullId)) {
                continue;
            }
            
            // Skip invisible overlays
            if (!definition.isVisible()) {
                continue;
            }
            
            Integer regOrder = registrationOrder.get(fullId);
            result.add(new OverlayEntry(fullId, definition, regOrder));
        }
        
        // Sort by z-index (ascending), then registration order (ascending)
        result.sort((a, b) -> {
            int zIndexCompare = Integer.compare(a.definition().getZIndex(), b.definition().getZIndex());
            if (zIndexCompare != 0) {
                return zIndexCompare;
            }
            return Integer.compare(a.registrationOrder(), b.registrationOrder());
        });
        
        return result;
    }
    
    /**
     * Gets an overlay by its full ID.
     * 
     * @param fullId the full overlay ID (modId:overlayId)
     * @return the overlay definition, or null if not found
     */
    public OverlayDefinition getOverlay(String fullId) {
        return overlays.get(fullId);
    }
    
    /**
     * Gets all overlays registered by a specific mod.
     * 
     * @param modId the mod ID
     * @return set of overlay IDs for the mod
     */
    public Set<String> getModOverlayIds(String modId) {
        return modToOverlayIds.getOrDefault(modId, Collections.emptySet());
    }
    
    /**
     * Sets the visibility of an overlay.
     * 
     * @param modId the mod ID
     * @param overlayId the overlay ID
     * @param visible the visibility state
     */
    public void setOverlayVisibility(String modId, String overlayId, boolean visible) {
        String fullId = createFullId(modId, overlayId);
        OverlayDefinition definition = overlays.get(fullId);
        
        if (definition != null) {
            definition.setVisible(visible);
            LOGGER.debug("Set overlay '{}' visibility to {}", overlayId, visible);
        } else {
            LOGGER.warn("Attempted to set visibility for unknown overlay '{}'", fullId);
        }
    }
    
    /**
     * Disables an overlay permanently (due to crash).
     * 
     * @param fullId the full overlay ID
     */
    public void disableOverlay(String fullId) {
        disabledOverlays.add(fullId);
        LOGGER.error("Permanently disabled overlay '{}' due to rendering error", fullId);
    }
    
    /**
     * Gets the count of registered overlays.
     * 
     * @return the number of registered overlays
     */
    public int getOverlayCount() {
        return overlays.size();
    }
    
    /**
     * Gets the count of active (not disabled) overlays.
     * 
     * @return the number of active overlays
     */
    public int getActiveOverlayCount() {
        return (int) overlays.entrySet().stream()
            .filter(entry -> !disabledOverlays.contains(entry.getKey()))
            .filter(entry -> entry.getValue().isVisible())
            .count();
    }
    
    /**
     * Clears all overlays (for testing purposes).
     */
    public static void clear() {
        if (instance != null) {
            instance.overlays.clear();
            instance.modToOverlayIds.clear();
            instance.registrationOrder.clear();
            instance.disabledOverlays.clear();
            instance.nextRegistrationOrder = 0;
            LOGGER.info("Cleared all overlays from registry");
        }
    }
    
    /**
     * Creates a full overlay ID from mod ID and overlay ID.
     */
    private String createFullId(String modId, String overlayId) {
        return modId + ":" + overlayId;
    }
    
    /**
     * Validates an overlay definition.
     */
    private void validateOverlayDefinition(OverlayDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Overlay definition cannot be null");
        }
        
        if (definition.getRenderFunction() == null || !definition.getRenderFunction().canExecute()) {
            throw new IllegalArgumentException("Overlay must have a valid render function");
        }
        
        if (definition.getAnchor() == null) {
            throw new IllegalArgumentException("Overlay must have a valid anchor");
        }
        
        if (definition.getZIndex() < 0 || definition.getZIndex() > 1000) {
            throw new IllegalArgumentException("Overlay z-index must be between 0 and 1000");
        }
    }
    
    /**
     * Represents an overlay entry with registration information.
     */
    public record OverlayEntry(String fullId, OverlayDefinition definition, int registrationOrder) {}
    
    /**
     * Represents an overlay definition.
     */
    public static class OverlayDefinition {
        private final String anchor;
        private final int zIndex;
        private final Value renderFunction;
        private volatile boolean visible;
        
        public OverlayDefinition(String anchor, int zIndex, Value renderFunction) {
            this.anchor = anchor;
            this.zIndex = zIndex;
            this.renderFunction = renderFunction;
            this.visible = true; // Default to visible
        }
        
        public String getAnchor() { return anchor; }
        public int getZIndex() { return zIndex; }
        public Value getRenderFunction() { return renderFunction; }
        public boolean isVisible() { return visible; }
        
        public void setVisible(boolean visible) {
            this.visible = visible;
        }
    }
}
