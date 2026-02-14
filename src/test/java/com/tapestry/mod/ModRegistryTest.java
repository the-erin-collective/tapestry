package com.tapestry.mod;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ModRegistry Phase 10.5 functionality.
 */
public class ModRegistryTest {
    
    private ModRegistry modRegistry;
    
    @BeforeEach
    void setUp() {
        // Reset phase controller for testing
        PhaseController.reset();
        // Advance through phases to reach TS_REGISTER
        PhaseController.getInstance().advanceTo(TapestryPhase.BOOTSTRAP);
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.VALIDATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_REGISTER);
        
        modRegistry = ModRegistry.getInstance();
        modRegistry.beginRegistration();
    }
    
    @AfterEach
    void tearDown() {
        PhaseController.reset();
    }
    
    @Test
    void testModRegistration() {
        // Create a test mod descriptor
        ModDescriptor mod = new ModDescriptor("test-mod", "1.0.0", List.of(), null, null);
        
        // Register the mod
        modRegistry.registerMod(mod);
        
        // Verify registration
        assertEquals(1, modRegistry.getAllMods().size());
        assertTrue(modRegistry.isModDefined("test-mod"));
        assertNotNull(modRegistry.getMod("test-mod"));
    }
    
    @Test
    void testDuplicateModIdThrowsException() {
        ModDescriptor mod1 = new ModDescriptor("test-mod", "1.0.0", List.of(), null, null);
        ModDescriptor mod2 = new ModDescriptor("test-mod", "2.0.0", List.of(), null, null);
        
        modRegistry.registerMod(mod1);
        
        assertThrows(IllegalArgumentException.class, () -> {
            modRegistry.registerMod(mod2);
        });
    }
    
    @Test
    void testDependencyValidation() {
        // Create mods with dependencies
        ModDescriptor modA = new ModDescriptor("mod-a", "1.0.0", List.of("mod-b"), null, null);
        ModDescriptor modB = new ModDescriptor("mod-b", "1.0.0", List.of(), null, null);
        
        modRegistry.registerMod(modA);
        modRegistry.registerMod(modB);
        
        // Should validate successfully
        assertDoesNotThrow(() -> modRegistry.validateDependencies());
    }
    
    @Test
    void testMissingDependencyThrowsException() {
        ModDescriptor modA = new ModDescriptor("mod-a", "1.0.0", List.of("missing-mod"), null, null);
        
        modRegistry.registerMod(modA);
        
        assertThrows(IllegalStateException.class, () -> {
            modRegistry.validateDependencies();
        });
    }
    
    @Test
    void testCircularDependencyDetection() {
        ModDescriptor modA = new ModDescriptor("mod-a", "1.0.0", List.of("mod-b"), null, null);
        ModDescriptor modB = new ModDescriptor("mod-b", "1.0.0", List.of("mod-a"), null, null);
        
        modRegistry.registerMod(modA);
        modRegistry.registerMod(modB);
        
        assertThrows(IllegalStateException.class, () -> {
            modRegistry.validateDependencies();
        });
    }
    
    @Test
    void testTopologicalSort() {
        // Create dependency chain: A -> B -> C
        ModDescriptor modA = new ModDescriptor("mod-a", "1.0.0", List.of("mod-b"), null, null);
        ModDescriptor modB = new ModDescriptor("mod-b", "1.0.0", List.of("mod-c"), null, null);
        ModDescriptor modC = new ModDescriptor("mod-c", "1.0.0", List.of(), null, null);
        
        modRegistry.registerMod(modA);
        modRegistry.registerMod(modB);
        modRegistry.registerMod(modC);
        
        List<ModDescriptor> activationOrder = modRegistry.buildActivationOrder();
        
        // Should be: C, B, A (dependencies first)
        assertEquals(3, activationOrder.size());
        assertEquals("mod-c", activationOrder.get(0).getId());
        assertEquals("mod-b", activationOrder.get(1).getId());
        assertEquals("mod-a", activationOrder.get(2).getId());
    }
    
    @Test
    void testModRegistryStats() {
        ModDescriptor mod1 = new ModDescriptor("mod1", "1.0.0", List.of(), null, null);
        ModDescriptor mod2 = new ModDescriptor("mod2", "1.0.0", List.of(), null, null);
        
        modRegistry.registerMod(mod1);
        modRegistry.registerMod(mod2);
        
        ModRegistry.ModRegistryStats stats = modRegistry.getStats();
        
        assertEquals(2, stats.registeredCount());
        assertEquals(0, stats.activatedCount()); // Not activated yet
        assertEquals(0, stats.exportCount());
    }
}
