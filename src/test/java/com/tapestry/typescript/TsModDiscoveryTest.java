package com.tapestry.typescript;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TsModDiscovery.
 */
public class TsModDiscoveryTest {
    
    private TsModDiscovery discovery;
    
    @BeforeEach
    void setUp() {
        PhaseController.reset();
        discovery = new TsModDiscovery();
    }
    
    @Test
    void testDiscoverModsEmptyDirectory() {
        // Test basic functionality - just ensure method doesn't crash
        var mods = discovery.discoverMods();
        assertNotNull(mods);
        // In test environment, this will likely be empty due to no FabricLoader
        // The important thing is that it doesn't throw NullPointerException
    }
    
    @Test
    void testExtractSourceName() {
        // Test extracting source name from full path
        String fullPath = "C:\\Users\\test\\mods\\example_mod.js";
        String expected = "example_mod.js";
        String actual = extractSourceName(fullPath);
        assertEquals(expected, actual);
        
        // Test with relative path
        String relativePath = "./mods/test_mod.js";
        assertEquals("test_mod.js", extractSourceName(relativePath));
        
        // Test with just filename
        String filename = "test_mod.js";
        assertEquals("test_mod.js", extractSourceName(filename));
    }
    
    /**
     * Extracts the source name from a file path.
     * This is a simplified version for testing.
     * 
     * @param modFile full path to mod file
     * @return source name
     */
    private String extractSourceName(String modFile) {
        return Paths.get(modFile).getFileName().toString();
    }
}
