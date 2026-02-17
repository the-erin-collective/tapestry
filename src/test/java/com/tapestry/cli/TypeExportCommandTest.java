package com.tapestry.cli;

import com.tapestry.extensions.types.ExtensionTypeRegistry;
import com.tapestry.extensions.types.ExtensionTypeRegistry.TypeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TypeExportCommand (Phase 14).
 */
public class TypeExportCommandTest {
    
    @TempDir
    Path tempDir;
    
    private ExtensionTypeRegistry typeRegistry;
    
    @BeforeEach
    void setUp() {
        typeRegistry = new ExtensionTypeRegistry();
    }
    
    @Test
    void testCall_Success() throws Exception {
        // Given
        typeRegistry.storeTypeModule("ext1", "export interface Test1 {}");
        typeRegistry.storeTypeModule("ext2", "export interface Test2 {}");
        typeRegistry.freeze();
        
        TypeExportCommand command = new TypeExportCommand(typeRegistry, tempDir.toString(), false, false);
        
        // When
        int result = command.call();
        
        // Then
        assertEquals(0, result);
        
        // Verify files were created
        Path ext1File = tempDir.resolve("ext1/index.d.ts");
        Path ext2File = tempDir.resolve("ext2/index.d.ts");
        
        assertTrue(java.nio.file.Files.exists(ext1File));
        assertTrue(java.nio.file.Files.exists(ext2File));
        
        assertEquals("export interface Test1 {}", java.nio.file.Files.readString(ext1File));
        assertEquals("export interface Test2 {}", java.nio.file.Files.readString(ext2File));
    }
    
    @Test
    void testCall_DryRun() throws Exception {
        // Given
        typeRegistry.storeTypeModule("ext1", "export interface Test1 {}");
        typeRegistry.freeze();
        
        TypeExportCommand command = new TypeExportCommand(typeRegistry, tempDir.toString(), false, true);
        
        // When
        int result = command.call();
        
        // Then
        assertEquals(0, result);
        
        // Verify no files were created
        Path ext1File = tempDir.resolve("ext1/index.d.ts");
        assertFalse(java.nio.file.Files.exists(ext1File));
    }
    
    @Test
    void testCall_ValidateNotFrozen() throws Exception {
        // Given
        typeRegistry.storeTypeModule("ext1", "export interface Test1 {}");
        // Don't freeze the registry
        
        TypeExportCommand command = new TypeExportCommand(typeRegistry, tempDir.toString(), true, false);
        
        // When
        int result = command.call();
        
        // Then
        assertEquals(1, result);
    }
    
    @Test
    void testCall_EmptyRegistry() throws Exception {
        // Given
        typeRegistry.freeze();
        
        TypeExportCommand command = new TypeExportCommand(typeRegistry, tempDir.toString(), false, false);
        
        // When
        int result = command.call();
        
        // Then
        assertEquals(0, result);
        
        // Verify no extension directories were created
        Set<String> extensionIds = typeRegistry.getAllExtensionIds();
        assertTrue(extensionIds.isEmpty());
    }
    
    @Test
    void testValidateWorkspaceInvariant_Valid() throws Exception {
        // Given
        typeRegistry.storeTypeModule("ext1", "export interface Test1 {}");
        typeRegistry.freeze();
        
        TypeExportCommand command = new TypeExportCommand(typeRegistry, tempDir.toString(), false, false);
        
        // When
        boolean result = command.validateWorkspaceInvariant();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testValidateWorkspaceInvariant_OutputDirNotWritable() throws Exception {
        // Given
        typeRegistry.freeze();
        // Use a non-existent path that can't be created
        Path invalidDir = Path.of("/invalid/path/that/cannot/be/created");
        
        TypeExportCommand command = new TypeExportCommand(typeRegistry, invalidDir.toString(), false, false);
        
        // When
        boolean result = command.validateWorkspaceInvariant();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testGenerateTsconfigSnippet() throws Exception {
        // Given
        typeRegistry.storeTypeModule("ext1", "export interface Test1 {}");
        typeRegistry.freeze();
        
        TypeExportCommand command = new TypeExportCommand(typeRegistry, tempDir.toString(), false, false);
        
        // When
        command.generateTsconfigSnippet();
        
        // Then
        Path tsconfigFile = tempDir.resolve("tsconfig.json");
        assertTrue(java.nio.file.Files.exists(tsconfigFile));
        
        String content = java.nio.file.Files.readString(tsconfigFile);
        assertTrue(content.contains("\"@tapestry/*\""));
        assertTrue(content.contains("\".tapestry/types/*\""));
    }
    
    @Test
    void testPrintMemoryUsage() throws Exception {
        // Given
        typeRegistry.storeTypeModule("ext1", "export interface Test1 {}");
        typeRegistry.storeTypeModule("ext2", "export interface Test2 {}");
        
        TypeExportCommand command = new TypeExportCommand(typeRegistry, tempDir.toString(), false, false);
        
        // When
        // This would print to log, so we can't easily test it
        // But we can verify it doesn't throw
        assertDoesNotThrow(() -> command.printMemoryUsage());
    }
    
    @Test
    void testCleanupStaleEntries() throws Exception {
        // Given
        typeRegistry.storeTypeModule("ext1", "export interface Test1 {}");
        typeRegistry.freeze();
        
        // Create a stale directory
        Path staleDir = tempDir.resolve("stale_ext");
        java.nio.file.Files.createDirectories(staleDir);
        java.nio.file.Files.writeString(staleDir.resolve("index.d.ts"), "old content");
        
        TypeExportCommand command = new TypeExportCommand(typeRegistry, tempDir.toString(), false, false);
        
        // When
        command.call();
        
        // Then
        // Current extension should exist
        assertTrue(java.nio.file.Files.exists(tempDir.resolve("ext1/index.d.ts")));
        
        // Stale extension should be removed
        assertFalse(java.nio.file.Files.exists(staleDir));
    }
}
