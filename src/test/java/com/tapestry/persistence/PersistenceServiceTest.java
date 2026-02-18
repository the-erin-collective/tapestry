package com.tapestry.persistence;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Phase 9 persistence functionality.
 */
public class PersistenceServiceTest {
    
    private Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Reset phase controller for each test
        PhaseController.reset();
        
        // Reset singleton instance for clean testing
        resetPersistenceServiceSingleton();
        
        // Advance through all phases to PERSISTENCE_READY
        PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
        PhaseController.getInstance().advanceTo(TapestryPhase.VALIDATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
        PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_REGISTER);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_ACTIVATE);
        PhaseController.getInstance().advanceTo(TapestryPhase.TS_READY);
        PhaseController.getInstance().advanceTo(TapestryPhase.PERSISTENCE_READY);
        
        // Create temporary directory for testing
        try {
            tempDir = Files.createTempDirectory("tapestry-test");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temp directory", e);
        }
    }
    
    @AfterEach
    void tearDown() {
        // Clean up singleton instance
        resetPersistenceServiceSingleton();
    }
    
    /**
     * Resets the PersistenceService singleton for clean testing.
     * Uses reflection to access the private instance field.
     */
    private void resetPersistenceServiceSingleton() {
        try {
            Field instanceField = PersistenceService.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset PersistenceService singleton", e);
        }
    }
    
    @Test
    void testPersistenceServiceInitialization() {
        // Test that persistence service can be initialized
        assertDoesNotThrow(() -> {
            PersistenceService.initialize(tempDir);
        });
        
        assertTrue(PersistenceService.getInstance().isInitialized());
        assertEquals(0, PersistenceService.getInstance().getLoadedModCount());
    }
    
    @Test
    void testModStateStoreBasicOperations() {
        // Initialize persistence service
        PersistenceService.initialize(tempDir);
        
        // Get a state store for a test mod
        ModStateStore store = PersistenceService.getInstance().getModStateStore("test-mod");
        
        // Test basic operations
        assertNull(store.get("nonexistent"));
        
        store.set("key1", "value1");
        assertEquals("value1", store.get("key1"));
        
        store.set("key2", 42);
        assertEquals(42, store.get("key2"));
        
        store.set("key3", true);
        assertEquals(true, store.get("key3"));
        
        assertEquals(3, store.size());
        assertTrue(store.hasKey("key1"));
        assertFalse(store.hasKey("nonexistent"));
        
        // Test deletion
        store.delete("key2");
        assertNull(store.get("key2"));
        assertEquals(2, store.size());
        assertFalse(store.hasKey("key2"));
    }
    
    @Test
    void testPersistenceAcrossModIsolation() {
        // Initialize persistence service
        PersistenceService.initialize(tempDir);
        
        // Get state stores for two different mods
        ModStateStore store1 = PersistenceService.getInstance().getModStateStore("mod1");
        ModStateStore store2 = PersistenceService.getInstance().getModStateStore("mod2");
        
        // Set values in each mod
        store1.set("shared", "value1");
        store2.set("shared", "value2");
        
        // Verify isolation
        assertEquals("value1", store1.get("shared"));
        assertEquals("value2", store2.get("shared"));
        
        // Verify mods can't access each other's data
        assertNotEquals(store1.get("shared"), store2.get("shared"));
        assertNotEquals(store2.get("shared"), store1.get("shared"));
    }
    
    @Test
    void testPhaseAdvancement() {
        // Test that phases advance correctly with persistence
        PhaseController.reset(); // Starts at BOOTSTRAP
        
        // Should be able to advance through all phases including PERSISTENCE_READY
        assertDoesNotThrow(() -> {
            PhaseController.getInstance().advanceTo(TapestryPhase.DISCOVERY);
            PhaseController.getInstance().advanceTo(TapestryPhase.VALIDATION);
            PhaseController.getInstance().advanceTo(TapestryPhase.REGISTRATION);
            PhaseController.getInstance().advanceTo(TapestryPhase.FREEZE);
            PhaseController.getInstance().advanceTo(TapestryPhase.TS_LOAD);
            PhaseController.getInstance().advanceTo(TapestryPhase.TS_REGISTER);
            PhaseController.getInstance().advanceTo(TapestryPhase.TS_ACTIVATE);
            PhaseController.getInstance().advanceTo(TapestryPhase.TS_READY);
            PhaseController.getInstance().advanceTo(TapestryPhase.PERSISTENCE_READY);
            PhaseController.getInstance().advanceTo(TapestryPhase.EVENT);
            PhaseController.getInstance().advanceTo(TapestryPhase.RUNTIME);
        });
        
        assertEquals(TapestryPhase.RUNTIME, PhaseController.getInstance().getCurrentPhase());
    }
}
