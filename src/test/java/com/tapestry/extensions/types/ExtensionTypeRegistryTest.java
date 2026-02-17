package com.tapestry.extensions.types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExtensionTypeRegistry (Phase 14).
 */
public class ExtensionTypeRegistryTest {
    
    private ExtensionTypeRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = new ExtensionTypeRegistry();
    }
    
    @AfterEach
    void tearDown() {
        if (registry != null && !registry.isFrozen()) {
            registry.clear();
        }
    }
    
    @Test
    void testStoreTypeModule() {
        // Given
        String extensionId = "test_extension";
        String dtsSource = "export interface Test {}";
        
        // When
        registry.storeTypeModule(extensionId, dtsSource);
        
        // Then
        assertTrue(registry.hasTypeModule(extensionId));
        
        var module = registry.getTypeModule(extensionId);
        assertNotNull(module);
        assertEquals(extensionId, module.extensionId());
        assertEquals(dtsSource, module.dtsSource());
    }
    
    @Test
    void testStoreTypeModuleAfterFreeze() {
        // Given
        registry.freeze();
        String extensionId = "test_extension";
        String dtsSource = "export interface Test {}";
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            registry.storeTypeModule(extensionId, dtsSource);
        });
    }
    
    @Test
    void testGetTypeModuleNotFound() {
        // Given
        String extensionId = "nonexistent";
        
        // When
        var module = registry.getTypeModule(extensionId);
        
        // Then
        assertNull(module);
        assertFalse(registry.hasTypeModule(extensionId));
    }
    
    @Test
    void testGetAllExtensionIds() {
        // Given
        registry.storeTypeModule("ext1", "export interface A {}");
        registry.storeTypeModule("ext2", "export interface B {}");
        registry.storeTypeModule("ext3", "export interface C {}");
        
        // When
        var extensionIds = registry.getAllExtensionIds();
        
        // Then
        assertEquals(3, extensionIds.size());
        assertTrue(extensionIds.contains("ext1"));
        assertTrue(extensionIds.contains("ext2"));
        assertTrue(extensionIds.contains("ext3"));
    }
    
    @Test
    void testFreeze() {
        // Given
        registry.storeTypeModule("test", "export interface Test {}");
        
        // When
        registry.freeze();
        
        // Then
        assertTrue(registry.isFrozen());
        
        // Should still be able to read
        assertTrue(registry.hasTypeModule("test"));
        
        // Should not be able to write
        assertThrows(IllegalStateException.class, () -> {
            registry.storeTypeModule("test2", "export interface Test2 {}");
        });
    }
    
    @Test
    void testClear() {
        // Given
        registry.storeTypeModule("test", "export interface Test {}");
        registry.storeTypeModule("test2", "export interface Test2 {}");
        
        // When
        registry.clear();
        
        // Then
        assertTrue(registry.getAllExtensionIds().isEmpty());
        assertFalse(registry.hasTypeModule("test"));
        assertFalse(registry.hasTypeModule("test2"));
    }
    
    @Test
    void testClearAfterFreeze() {
        // Given
        registry.storeTypeModule("test", "export interface Test {}");
        registry.freeze();
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            registry.clear();
        });
    }
    
    @Test
    void testGetMemoryFootprint() {
        // Given
        String extensionId = "test_extension";
        String dtsSource = "export interface Test {}";
        registry.storeTypeModule(extensionId, dtsSource);
        
        // When
        long memoryFootprint = registry.getMemoryFootprint();
        
        // Then
        assertTrue(memoryFootprint > 0);
        // Should include both extension ID and source
        long expected = extensionId.getBytes().length + dtsSource.getBytes().length;
        assertEquals(expected, memoryFootprint);
    }
    
    @Test
    void testEmptyRegistry() {
        // Given empty registry
        
        // When & Then
        assertTrue(registry.getAllExtensionIds().isEmpty());
        assertEquals(0, registry.getMemoryFootprint());
        assertFalse(registry.isFrozen());
    }
}
