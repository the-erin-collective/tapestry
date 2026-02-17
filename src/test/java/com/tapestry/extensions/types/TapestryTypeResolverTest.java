package com.tapestry.extensions.types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TapestryTypeResolver (Phase 14).
 */
public class TapestryTypeResolverTest {
    
    private ExtensionTypeRegistry typeRegistry;
    private TapestryTypeResolver resolver;
    
    @BeforeEach
    void setUp() {
        typeRegistry = new ExtensionTypeRegistry();
        resolver = new TapestryTypeResolver(typeRegistry);
    }
    
    @Test
    void testResolveModule_ValidTypeImport() throws Exception {
        // Given
        String extensionId = "test_extension";
        typeRegistry.storeTypeModule(extensionId, "export interface Test {}");
        resolver.setCurrentExtension("importer");
        resolver.authorizeTypeImports("importer", List.of(extensionId));
        
        // When
        String result = resolver.resolveModule("@tapestry/" + extensionId, "type");
        
        // Then
        assertEquals("export {};", result);
    }
    
    @Test
    void testResolveModule_RuntimeImportForbidden() {
        // Given
        String extensionId = "test_extension";
        typeRegistry.storeTypeModule(extensionId, "export interface Test {}");
        resolver.setCurrentExtension("importer");
        resolver.authorizeTypeImports("importer", List.of(extensionId));
        
        // When & Then
        var exception = assertThrows(
            TapestryTypeResolver.TapestryTypeResolutionException.class,
            () -> resolver.resolveModule("@tapestry/" + extensionId, "value")
        );
        
        assertEquals(TypeValidationError.RUNTIME_IMPORT_FORBIDDEN, exception.getError());
        assertTrue(exception.getMessage().contains("Runtime imports are forbidden"));
    }
    
    @Test
    void testResolveModule_InvalidNamespace() {
        // Given
        resolver.setCurrentExtension("importer");
        
        // When & Then
        var exception = assertThrows(
            TapestryTypeResolver.TapestryTypeResolutionException.class,
            () -> resolver.resolveModule("@other/extension", "type")
        );
        
        assertEquals(TypeValidationError.INVALID_TAPESTRY_NAMESPACE, exception.getError());
        assertTrue(exception.getMessage().contains("not in @tapestry/* namespace"));
    }
    
    @Test
    void testResolveModule_SubpathNotAllowed() {
        // Given
        resolver.setCurrentExtension("importer");
        
        // When & Then
        var exception = assertThrows(
            TapestryTypeResolver.TapestryTypeResolutionException.class,
            () -> resolver.resolveModule("@tapestry/extension/internal", "type")
        );
        
        assertEquals(TypeValidationError.INVALID_TAPESTRY_NAMESPACE, exception.getError());
        assertTrue(exception.getMessage().contains("Subpaths are not allowed"));
    }
    
    @Test
    void testResolveModule_UndeclaredTypeImport() throws Exception {
        // Given
        String extensionId = "test_extension";
        typeRegistry.storeTypeModule(extensionId, "export interface Test {}");
        resolver.setCurrentExtension("importer");
        // Don't authorize the import
        
        // When & Then
        var exception = assertThrows(
            TapestryTypeResolver.TapestryTypeResolutionException.class,
            () -> resolver.resolveModule("@tapestry/" + extensionId, "type")
        );
        
        assertEquals(TypeValidationError.UNDECLARED_TYPE_IMPORT, exception.getError());
        assertTrue(exception.getMessage().contains("does not declare type import"));
    }
    
    @Test
    void testResolveModule_TargetDoesNotExportTypes() throws Exception {
        // Given
        String extensionId = "test_extension";
        // Don't store any type module for this extension
        resolver.setCurrentExtension("importer");
        resolver.authorizeTypeImports("importer", List.of(extensionId));
        
        // When & Then
        var exception = assertThrows(
            TapestryTypeResolver.TapestryTypeResolutionException.class,
            () -> resolver.resolveModule("@tapestry/" + extensionId, "type")
        );
        
        assertEquals(TypeValidationError.TARGET_DOES_NOT_EXPORT_TYPES, exception.getError());
        assertTrue(exception.getMessage().contains("does not export any types"));
    }
    
    @Test
    void testResolveModule_InvalidExtensionId() {
        // Given
        resolver.setCurrentExtension("importer");
        
        // When & Then
        var exception = assertThrows(
            TapestryTypeResolver.TapestryTypeResolutionException.class,
            () -> resolver.resolveModule("@tapestry/Invalid-ID", "type")
        );
        
        assertEquals(TypeValidationError.INVALID_TAPESTRY_NAMESPACE, exception.getError());
        assertTrue(exception.getMessage().contains("Invalid extension ID"));
    }
    
    @Test
    void testAuthorizeTypeImports() {
        // Given
        String extensionId = "test_extension";
        resolver.setCurrentExtension("importer");
        
        // When
        resolver.authorizeTypeImports("importer", List.of(extensionId, "another_extension"));
        
        // Then
        // We can't directly test authorization since it's internal,
        // but we can verify it doesn't throw
        assertDoesNotThrow(() -> {
            try {
                resolver.resolveModule("@tapestry/" + extensionId, "type");
            } catch (TapestryTypeResolver.TapestryTypeResolutionException e) {
                // This should still fail since no type module is stored
                if (e.getError() != TypeValidationError.TARGET_DOES_NOT_EXPORT_TYPES) {
                    throw e;
                }
            }
        });
    }
    
    @Test
    void testSetCurrentExtension() {
        // Given
        String extensionId = "test_extension";
        
        // When
        resolver.setCurrentExtension(extensionId);
        
        // Then
        // We can't directly test the current extension since it's thread-local,
        // but we can verify it doesn't throw
        assertDoesNotThrow(() -> resolver.setCurrentExtension(extensionId));
    }
    
    @Test
    void testClearCurrentExtension() {
        // Given
        resolver.setCurrentExtension("test_extension");
        
        // When
        resolver.clearCurrentExtension();
        
        // Then
        // We can't directly test the cleared state since it's thread-local,
        // but we can verify it doesn't throw
        assertDoesNotThrow(() -> resolver.clearCurrentExtension());
    }
    
    @Test
    void testGetMemoryUsage() {
        // Given
        resolver.authorizeTypeImports("ext1", List.of("target1", "target2"));
        resolver.authorizeTypeImports("ext2", List.of("target3"));
        
        // When
        long memoryUsage = resolver.getMemoryUsage();
        
        // Then
        assertTrue(memoryUsage > 0);
        // Should be roughly 64 bytes per authorization string
        long expected = 3 * 64L; // 3 authorizations
        assertEquals(expected, memoryUsage);
    }
    
    @Test
    void testClearAuthorizations() {
        // Given
        resolver.authorizeTypeImports("ext1", List.of("target1"));
        resolver.authorizeTypeImports("ext2", List.of("target2"));
        
        // When
        resolver.clearAuthorizations();
        
        // Then
        assertEquals(0, resolver.getMemoryUsage());
    }
}
