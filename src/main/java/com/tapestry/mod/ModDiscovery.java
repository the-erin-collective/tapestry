package com.tapestry.mod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles discovery of mods in the /mods directory structure.
 * 
 * Scans for mod.json metadata files and validates the required structure
 * according to Phase 10.5 specifications.
 */
public class ModDiscovery {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ModDiscovery.class);
    private static final String MODS_DIRECTORY = "mods";
    private static final String METADATA_FILE = "mod.json";
    private static final String DEFAULT_ENTRY_FILE = "dist/index.js";
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path modsPath;
    
    public ModDiscovery() {
        this.modsPath = Path.of(MODS_DIRECTORY);
    }
    
    /**
     * Discovers all mods in the /mods directory.
     * 
     * @return list of discovered mod descriptors
     */
    public List<DiscoveredMod> discoverMods() {
        LOGGER.info("Discovering mods in directory: {}", modsPath.toAbsolutePath());
        
        if (!Files.exists(modsPath)) {
            LOGGER.info("Mods directory does not exist, no mods to discover");
            return new ArrayList<>();
        }
        
        if (!Files.isDirectory(modsPath)) {
            LOGGER.warn("Mods path exists but is not a directory: {}", modsPath);
            return new ArrayList<>();
        }
        
        List<DiscoveredMod> discoveredMods = new ArrayList<>();
        
        try {
            Files.list(modsPath)
                .filter(Files::isDirectory)
                .sorted()
                .forEach(modDir -> {
                    DiscoveredMod mod = discoverModInDirectory(modDir);
                    if (mod != null) {
                        discoveredMods.add(mod);
                    }
                });
        } catch (IOException e) {
            LOGGER.error("Failed to scan mods directory", e);
            return new ArrayList<>();
        }
        
        LOGGER.info("Discovered {} mods", discoveredMods.size());
        return discoveredMods;
    }
    
    /**
     * Discovers a single mod in the given directory.
     * 
     * @param modDir the mod directory
     * @return discovered mod, or null if invalid/skipped
     */
    private DiscoveredMod discoverModInDirectory(Path modDir) {
        String modId = modDir.getFileName().toString();
        
        try {
            // Check for mod.json
            Path metadataPath = modDir.resolve(METADATA_FILE);
            if (!Files.exists(metadataPath)) {
                LOGGER.warn("Mod '{}' skipped: no {} file found", modId, METADATA_FILE);
                return null;
            }
            
            // Parse metadata
            ModMetadata metadata = parseMetadata(metadataPath);
            if (metadata == null) {
                return null;
            }
            
            // Validate metadata matches directory
            if (!modId.equals(metadata.id())) {
                LOGGER.error("Mod '{}' skipped: directory name '{}' does not match metadata id '{}'", 
                           modId, modId, metadata.id());
                return null;
            }
            
            // Check for entry file
            String entryFile = metadata.entry() != null ? metadata.entry() : DEFAULT_ENTRY_FILE;
            Path entryPath = modDir.resolve(entryFile);
            
            if (!Files.exists(entryPath)) {
                LOGGER.warn("Mod '{}' skipped: entry file '{}' not found", modId, entryFile);
                return null;
            }
            
            // Validate entry file is readable
            if (!Files.isRegularFile(entryPath) || !Files.isReadable(entryPath)) {
                LOGGER.warn("Mod '{}' skipped: entry file '{}' is not a readable file", modId, entryFile);
                return null;
            }
            
            LOGGER.debug("Discovered mod: {} (version: {}, entry: {})", 
                        metadata.id(), metadata.version(), entryFile);
            
            return new DiscoveredMod(
                metadata.id(),
                metadata.version(),
                metadata.dependsOn(),
                entryPath.toString(),
                modDir.toString()
            );
            
        } catch (Exception e) {
            LOGGER.error("Mod '{}' skipped: discovery failed", modId, e);
            return null;
        }
    }
    
    /**
     * Parses mod.json metadata file.
     * 
     * @param metadataPath path to mod.json
     * @return parsed metadata, or null if invalid
     */
    private ModMetadata parseMetadata(Path metadataPath) {
        try {
            String content = Files.readString(metadataPath);
            JsonNode json = objectMapper.readTree(content);
            
            // Validate required fields
            if (!json.has("id")) {
                LOGGER.error("Invalid {}: missing required field 'id'", METADATA_FILE);
                return null;
            }
            
            if (!json.has("version")) {
                LOGGER.error("Invalid {}: missing required field 'version'", METADATA_FILE);
                return null;
            }
            
            String id = json.get("id").asText();
            String version = json.get("version").asText();
            
            // Validate ID format
            if (id.isEmpty() || id.contains(" ")) {
                LOGGER.error("Invalid {}: id '{}' must be non-empty and contain no spaces", METADATA_FILE, id);
                return null;
            }
            
            // Parse dependencies
            List<String> dependsOn = new ArrayList<>();
            if (json.has("dependsOn")) {
                JsonNode depsNode = json.get("dependsOn");
                if (depsNode.isArray()) {
                    for (JsonNode dep : depsNode) {
                        dependsOn.add(dep.asText());
                    }
                } else {
                    LOGGER.warn("Invalid {}: 'dependsOn' must be an array, ignoring", METADATA_FILE);
                }
            }
            
            // Parse optional entry file
            String entry = null;
            if (json.has("entry")) {
                entry = json.get("entry").asText();
            }
            
            return new ModMetadata(id, version, dependsOn, entry);
            
        } catch (IOException e) {
            LOGGER.error("Failed to parse {}: {}", METADATA_FILE, e.getMessage());
            return null;
        }
    }
    
    /**
     * Represents discovered mod information.
     */
    public record DiscoveredMod(
        String id,
        String version,
        List<String> dependsOn,
        String entryPath,
        String sourcePath
    ) {}
    
    /**
     * Represents parsed mod.json metadata.
     */
    private record ModMetadata(
        String id,
        String version,
        List<String> dependsOn,
        String entry
    ) {}
}
