package com.tapestry.overlay;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the OverlayRegistry class.
 */
public class OverlayRegistryTest {
    
    private OverlayRegistry registry;
    
    @BeforeEach
    void setUp() {
        PhaseController.reset();
        
        // Advance to CLIENT_PRESENTATION_READY phase
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.VALIDATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_READY);
        PhaseController.getInstance().advanceTo(TapestryPhase.PERSISTENCE_READY);
        PhaseController.getInstance().advanceTo(TapestryPhase.RUNTIME);
        PhaseController.getInstance().advanceTo(TapestryPhase.CLIENT_PRESENTATION_READY);
        
        // Get singleton instance and clear it for testing
        registry = OverlayRegistry.getInstance();
        registry.clear();
    }
    
    @Test
    void testRegisterOverlay() {
        // Arrange
        String modId = "test-mod";
        String overlayId = "test-overlay";
        
        // Create a simple render function
        Value mockRenderFunction = Value.asValue((ProxyExecutable) args -> {
            return "test result";
        });
        
        OverlayRegistry.OverlayDefinition definition = new OverlayRegistry.OverlayDefinition(
            "TOP_LEFT", 50, mockRenderFunction
        );
        
        // Act
        registry.registerOverlay(modId, overlayId, definition);
        
        // Assert
        assertEquals(1, registry.getOverlayCount());
        assertEquals(1, registry.getActiveOverlayCount());
        assertTrue(registry.getModOverlayIds(modId).contains(overlayId));
        
        OverlayRegistry.OverlayDefinition retrieved = registry.getOverlay(modId + ":" + overlayId);
        assertNotNull(retrieved);
        assertEquals("TOP_LEFT", retrieved.getAnchor());
        assertEquals(50, retrieved.getZIndex());
        assertTrue(retrieved.isVisible());
    }
    
    @Test
    void testRegisterOverlayDuplicateId() {
        // Arrange
        String modId = "test-mod";
        String overlayId = "test-overlay";
        
        Value mockRenderFunction = Value.asValue((ProxyExecutable) args -> null);
        
        OverlayRegistry.OverlayDefinition definition1 = new OverlayRegistry.OverlayDefinition(
            "TOP_LEFT", 50, mockRenderFunction
        );
        OverlayRegistry.OverlayDefinition definition2 = new OverlayRegistry.OverlayDefinition(
            "TOP_RIGHT", 100, mockRenderFunction
        );
        
        // Act & Assert
        registry.registerOverlay(modId, overlayId, definition1);
        
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerOverlay(modId, overlayId, definition2);
        });
    }
    
    @Test
    void testRegisterOverlayInvalidAnchor() {
        // Arrange
        String modId = "test-mod";
        String overlayId = "test-overlay";
        
        Value mockRenderFunction = Value.asValue((ProxyExecutable) args -> null);
        
        OverlayRegistry.OverlayDefinition definition = new OverlayRegistry.OverlayDefinition(
            "INVALID_ANCHOR", 50, mockRenderFunction
        );
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerOverlay(modId, overlayId, definition);
        });
    }
    
    @Test
    void testRegisterOverlayInvalidZIndex() {
        // Arrange
        String modId = "test-mod";
        String overlayId = "test-overlay";
        
        Value mockRenderFunction = Value.asValue((ProxyExecutable) args -> null);
        
        OverlayRegistry.OverlayDefinition definition = new OverlayRegistry.OverlayDefinition(
            "TOP_LEFT", -1, mockRenderFunction
        );
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerOverlay(modId, overlayId, definition);
        });
    }
    
    @Test
    void testOverlayVisibility() {
        // Arrange
        String modId = "test-mod";
        String overlayId = "test-overlay";
        
        Value mockRenderFunction = Value.asValue((ProxyExecutable) args -> null);
        
        OverlayRegistry.OverlayDefinition definition = new OverlayRegistry.OverlayDefinition(
            "TOP_LEFT", 50, mockRenderFunction
        );
        
        registry.registerOverlay(modId, overlayId, definition);
        
        // Act
        registry.setOverlayVisibility(modId, overlayId, false);
        
        // Assert
        assertEquals(1, registry.getOverlayCount()); // Still registered
        assertEquals(0, registry.getActiveOverlayCount()); // Not active
        
        OverlayRegistry.OverlayDefinition retrieved = registry.getOverlay(modId + ":" + overlayId);
        assertFalse(retrieved.isVisible());
    }
    
    @Test
    void testOverlayRenderOrder() {
        // Arrange
        String modId = "test-mod";
        
        Value mockRenderFunction = Value.asValue((ProxyExecutable) args -> null);
        
        OverlayRegistry.OverlayDefinition definition1 = new OverlayRegistry.OverlayDefinition(
            "TOP_LEFT", 100, mockRenderFunction
        );
        OverlayRegistry.OverlayDefinition definition2 = new OverlayRegistry.OverlayDefinition(
            "TOP_LEFT", 50, mockRenderFunction
        );
        OverlayRegistry.OverlayDefinition definition3 = new OverlayRegistry.OverlayDefinition(
            "TOP_LEFT", 75, mockRenderFunction
        );
        
        // Act
        registry.registerOverlay(modId, "overlay1", definition1);
        registry.registerOverlay(modId, "overlay2", definition2);
        registry.registerOverlay(modId, "overlay3", definition3);
        
        var overlays = registry.getOverlaysInRenderOrder();
        
        // Assert - Should be sorted by z-index (50, 75, 100)
        assertEquals(3, overlays.size());
        assertEquals(50, overlays.get(0).definition().getZIndex());
        assertEquals(75, overlays.get(1).definition().getZIndex());
        assertEquals(100, overlays.get(2).definition().getZIndex());
    }
    
    @Test
    void testDisableOverlay() {
        // Arrange
        String modId = "test-mod";
        String overlayId = "test-overlay";
        
        Value mockRenderFunction = Value.asValue((ProxyExecutable) args -> null);
        
        OverlayRegistry.OverlayDefinition definition = new OverlayRegistry.OverlayDefinition(
            "TOP_LEFT", 50, mockRenderFunction
        );
        
        registry.registerOverlay(modId, overlayId, definition);
        
        // Act
        registry.disableOverlay(modId + ":" + overlayId);
        
        // Assert
        assertEquals(1, registry.getOverlayCount()); // Still registered
        assertEquals(0, registry.getActiveOverlayCount()); // Not active due to being disabled
        
        var overlays = registry.getOverlaysInRenderOrder();
        assertEquals(0, overlays.size()); // Not included in render list
    }
    
    @Test
    void testClear() {
        // Arrange
        String modId = "test-mod";
        
        Value mockRenderFunction = Value.asValue((ProxyExecutable) args -> null);
        
        OverlayRegistry.OverlayDefinition definition = new OverlayRegistry.OverlayDefinition(
            "TOP_LEFT", 50, mockRenderFunction
        );
        
        registry.registerOverlay(modId, "overlay1", definition);
        registry.registerOverlay(modId, "overlay2", definition);
        
        // Act
        registry.clear();
        
        // Assert
        assertEquals(0, registry.getOverlayCount());
        assertEquals(0, registry.getActiveOverlayCount());
        assertTrue(registry.getModOverlayIds(modId).isEmpty());
    }
}
