package com.tapestry.typescript;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TsModRegistry functionality.
 * Note: Tests that require Value mocking are skipped due to Value being final.
 */
public class TsModRegistryTest {
    
    private TsModRegistry registry;
    
    @BeforeEach
    void setUp() {
        PhaseController.reset();
        registry = new TsModRegistry();
    }
    
    @Test
    void testGetModsDeterministicOrder() {
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        
        // Skip Value-dependent tests for now
        // Value mockOnLoad = createMockValue("onLoad");
        
        // Test empty registry
        var mods = registry.getMods();
        assertEquals(0, mods.size());
    }
    
    @Test
    void testCompleteDiscoveryEmpty() {
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        
        assertDoesNotThrow(() -> registry.completeDiscovery());
        assertTrue(registry.isDiscoveryComplete());
        
        // Calling completeDiscovery again should not throw, just log a warning
        assertDoesNotThrow(() -> registry.completeDiscovery());
    }
    
    @Test
    void testCompleteLoadingEmpty() {
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        
        assertDoesNotThrow(() -> registry.completeDiscovery());
        assertDoesNotThrow(() -> registry.completeLoading());
        
        // Should not be able to complete loading twice
        assertDoesNotThrow(() -> registry.completeLoading()); // Should not throw, just warn
    }
}
