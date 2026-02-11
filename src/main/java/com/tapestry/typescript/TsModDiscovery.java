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
     * Discovers all TypeScript mod files from both filesystem and classpath.
     * 
     * @return list of discovered mod file paths in deterministic order
     */
    public List<String> discoverMods() {
        List<String> modFiles = new ArrayList<>();
        
        // First, scan filesystem (config/tapestry/mods)
        scanFilesystemMods(modFiles);
        
        // Then, scan classpath resources (assets/tapestry/mods)
        scanClasspathMods(modFiles);
        
        LOGGER.info("Discovered {} TypeScript mod files", modFiles.size());
        return Collections.unmodifiableList(modFiles);
    }
    
    /**
     * Scans the filesystem for mod files.
     * 
     * @param modFiles list to add discovered files to
     */
    private void scanFilesystemMods(List<String> modFiles) {
        Path configPath = Path.of(CONFIG_DIR);
        
        if (!Files.exists(configPath)) {
            LOGGER.debug("Config directory does not exist: {}", CONFIG_DIR);
            return;
        }
        
        try {
            try (Stream<Path> paths = Files.walk(configPath)) {
                paths.filter(path -> !Files.isDirectory(path))
                     .filter(path -> !isHidden(path))
                     .filter(path -> path.toString().endsWith(".js"))
                     .sorted() // Alphabetical order for determinism
                     .forEach(path -> {
                         String relativePath = configPath.relativize(path).toString().replace('\\', '/');
                         modFiles.add("file:" + relativePath);
                         LOGGER.debug("Found filesystem mod: {}", relativePath);
                     });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to scan filesystem mods directory: {}", CONFIG_DIR, e);
            throw new RuntimeException("Failed to scan mods directory", e);
        }
    }
    
    /**
     * Scans classpath resources for mod files.
     * 
     * @param modFiles list to add discovered files to
     */
    private void scanClasspathMods(List<String> modFiles) {
        try {
            // Get all resources in assets/tapestry/mods
            var resources = FabricLoader.getInstance()
                .getModContainer("tapestry")
                .getRootPaths()
                .stream()
                .flatMap(root -> findResourcesInPath(root, ASSETS_DIR))
                .filter(path -> path.endsWith(".js"))
                .sorted() // Alphabetical order for determinism
                .toList();
            
            for (String resource : resources) {
                modFiles.add("classpath:" + resource);
                LOGGER.debug("Found classpath mod: {}", resource);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to scan classpath mods directory: {}", ASSETS_DIR, e);
            throw new RuntimeException("Failed to scan classpath mods", e);
        }
    }
    
    /**
     * Finds resources in a given path within a root directory.
     * 
     * @param root the root path to search in
     * @param targetPath the target path within the root
     * @return stream of resource paths
     */
    private Stream<String> findResourcesInPath(Path root, String targetPath) {
        try {
            Path target = root.resolve(targetPath);
            if (!Files.exists(target)) {
                return Stream.empty();
            }
            
            return Files.walk(target)
                .filter(path -> !Files.isDirectory(path))
                .filter(path -> !isHidden(path))
                .filter(path -> path.toString().endsWith(".js"))
                .map(path -> targetPath + "/" + target.relativize(path).toString().replace('\\', '/'));
                
        } catch (IOException e) {
            LOGGER.warn("Failed to scan path {}: {}", targetPath, e.getMessage());
            return Stream.empty();
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
