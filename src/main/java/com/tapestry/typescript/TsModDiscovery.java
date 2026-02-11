package com.tapestry.typescript;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Discovers TypeScript mod files from filesystem and classpath resources.
 * 
 * This class handles the recursive scanning of mod directories and
 * provides deterministic ordering of discovered mod files.
 */
public class TsModDiscovery {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TsModDiscovery.class);
    
    private static final String CONFIG_DIR = "config/tapestry/mods";
    private static final String ASSETS_DIR = "assets/tapestry/mods";
    
    /**
     * Discovers TypeScript mod files from filesystem.
     * 
     * @return list of mod file paths
     */
    public List<String> discoverMods() {
        List<String> modFiles = new ArrayList<>();
        
        // Only scan filesystem for Phase 2
        // Classpath discovery is deferred to future phases
        scanFilesystemMods(modFiles);
        
        LOGGER.info("Discovered {} TypeScript mod files", modFiles.size());
        return Collections.unmodifiableList(modFiles);
    }
    
    /**
     * Scans filesystem for mod files.
     * Uses Fabric's config directory for discovery.
     * 
     * @param modFiles list to add discovered files to
     */
    private void scanFilesystemMods(List<String> modFiles) {
        try {
            // Use Fabric's config directory
            Path configDir;
            try {
                var fabricLoader = FabricLoader.getInstance();
                if (fabricLoader == null) {
                    LOGGER.debug("FabricLoader not available in test environment");
                    return;
                }
                
                Path fabricConfigDir = fabricLoader.getConfigDir();
                if (fabricConfigDir == null) {
                    LOGGER.debug("Config directory not available in test environment");
                    return;
                }
                
                configDir = fabricConfigDir.resolve("tapestry/mods");
            } catch (Exception e) {
                LOGGER.debug("FabricLoader not available in test environment: {}", e.getMessage());
                return;
            }
            
            if (!Files.exists(configDir)) {
                LOGGER.debug("Config directory does not exist: {}", configDir);
                return;
            }
            
            // Recursively scan for .js files
            try (var stream = Files.walk(configDir)) {
                var files = stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> path.toString().endsWith(".js"))
                    .filter(path -> !path.getFileName().toString().startsWith("."))
                    .sorted() // Alphabetical order for determinism
                    .toList();
                
                for (Path file : files) {
                    modFiles.add(file.toString());
                    LOGGER.debug("Found filesystem mod: {}", file);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to scan filesystem mods directory", e);
            throw new RuntimeException("Failed to scan filesystem mods", e);
        }
    }    
    /**
     * Checks if a path is hidden.
     * 
     * @param path the path to check
     * @return true if the path is hidden
     */
    private boolean isHidden(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().startsWith(".");
    }
}
