package com.tapestry.typescript;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
     * Discovers TypeScript mods from Fabric mod containers (JARs or directories).
     * 
     * @return list of discovered classpath mods
     * @throws IOException if scanning fails
     */
    private static List<DiscoveredMod> discoverClasspathMods() throws IOException {
        List<DiscoveredMod> discovered = new ArrayList<>();
        
        var fabricLoader = FabricLoader.getInstance();
        if (fabricLoader == null) {
            LOGGER.debug("FabricLoader not available in test environment");
            return discovered;
        }
        
        for (var container : fabricLoader.getAllMods()) {
            String modId = container.getMetadata().getId();
            
            for (Path root : container.getRootPaths()) {
                Path tapestryPath = root.resolve("assets/tapestry/mods");
                
                if (!Files.exists(tapestryPath)) {
                    continue;
                }
                
                try (Stream<Path> walk = Files.walk(tapestryPath)) {
                    walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".js"))
                        .forEach(path -> {
                            String relative = tapestryPath.relativize(path).toString().replace("\\", "/");
                            String sourceName = "classpath:" + modId + "/" + relative;
                            
                            discovered.add(new DiscoveredMod(
                                path,
                                sourceName,
                                true  // mark as classpath
                            ));
                        });
                }
            }
        }
        
        // Deterministic ordering
        discovered.sort(Comparator.comparing(DiscoveredMod::sourceName));
        
        return discovered;
    }
    
    /**
     * Discovers TypeScript mod files from filesystem and classpath resources.
     * 
     * @return list of discovered mods
     * @throws IOException if scanning fails
     */
    public static List<DiscoveredMod> discoverMods() throws IOException {
        List<DiscoveredMod> all = new ArrayList<>();
        
        all.addAll(scanFilesystemMods());
        all.addAll(discoverClasspathMods());
        
        // Final deterministic order across both sources
        all.sort(Comparator.comparing(DiscoveredMod::sourceName));
        
        LOGGER.info("Discovered {} TypeScript mod files ({} filesystem, {} classpath)", 
            all.size(), 
            all.stream().mapToInt(m -> m.classpath() ? 0 : 1).sum(),
            all.stream().mapToInt(m -> m.classpath() ? 1 : 0).sum());
        
        return all;
    }
    
    /**
     * Scans filesystem for mod files.
     * Uses Fabric's config directory for discovery.
     * 
     * @return list of discovered filesystem mods
     * @throws IOException if scanning fails
     */
    private static List<DiscoveredMod> scanFilesystemMods() throws IOException {
        List<DiscoveredMod> modFiles = new ArrayList<>();
        
        try {
            // Use Fabric's config directory
            Path configDir;
            try {
                var fabricLoader = FabricLoader.getInstance();
                if (fabricLoader == null) {
                    LOGGER.debug("FabricLoader not available in test environment");
                    return modFiles;
                }
                
                Path fabricConfigDir = fabricLoader.getConfigDir();
                if (fabricConfigDir == null) {
                    LOGGER.debug("Config directory not available in test environment");
                    return modFiles;
                }
                
                configDir = fabricConfigDir.resolve("tapestry/mods");
            } catch (Exception e) {
                LOGGER.debug("FabricLoader not available in test environment: {}", e.getMessage());
                return modFiles;
            }
            
            if (!Files.exists(configDir)) {
                LOGGER.debug("Config directory does not exist: {}", configDir);
                return modFiles;
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
                    String relative = configDir.relativize(file).toString().replace("\\", "/");
                    String sourceName = "filesystem:" + relative;
                    
                    modFiles.add(new DiscoveredMod(
                        file,
                        sourceName,
                        false  // mark as filesystem
                    ));
                    LOGGER.debug("Found filesystem mod: {}", sourceName);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to scan filesystem mods directory", e);
            throw new RuntimeException("Failed to scan filesystem mods", e);
        }
        
        return modFiles;
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
