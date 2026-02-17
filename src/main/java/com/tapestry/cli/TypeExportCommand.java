package com.tapestry.cli;

import com.tapestry.extensions.types.ExtensionTypeRegistry;
import com.tapestry.extensions.types.ExtensionTypeRegistry.TypeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * CLI command for exporting type definitions for IDE support.
 * 
 * Phase 14: Generates physical .d.ts files from the type registry
 * for IDE autocompletion and type checking.
 */
public class TypeExportCommand implements Callable<Integer> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeExportCommand.class);
    
    private final ExtensionTypeRegistry typeRegistry;
    private final Path outputDir;
    private final boolean validate;
    private final boolean dryRun;
    
    public TypeExportCommand(
            ExtensionTypeRegistry typeRegistry,
            String outputDir,
            boolean validate,
            boolean dryRun) {
        this.typeRegistry = typeRegistry;
        this.outputDir = Paths.get(outputDir != null ? outputDir : ".tapestry/types");
        this.validate = validate;
        this.dryRun = dryRun;
    }
    
    @Override
    public Integer call() throws Exception {
        LOGGER.info("Starting Phase 14 type export");
        
        try {
            // Validate registry is frozen (should be after TYPE_INIT)
            if (validate && !typeRegistry.isFrozen()) {
                LOGGER.error("Type registry is not frozen. Run validation first.");
                return 1;
            }
            
            // Create output directory
            if (!dryRun) {
                createOutputDirectory();
            }
            
            // Export all type modules
            Set<String> extensionIds = typeRegistry.getAllExtensionIds();
            int exportedCount = 0;
            
            for (String extensionId : extensionIds) {
                TypeModule module = typeRegistry.getTypeModule(extensionId);
                if (module != null) {
                    if (exportTypeModule(module)) {
                        exportedCount++;
                    }
                }
            }
            
            // Clean up stale entries
            if (!dryRun) {
                cleanupStaleEntries(extensionIds);
            }
            
            LOGGER.info("Type export completed. Exported {} extensions to {}", 
                exportedCount, outputDir);
            
            if (dryRun) {
                LOGGER.info("Dry run completed - no files were written");
            }
            
            return 0;
            
        } catch (Exception e) {
            LOGGER.error("Type export failed", e);
            return 1;
        }
    }
    
    /**
     * Exports a single type module to a .d.ts file.
     */
    private boolean exportTypeModule(TypeModule module) {
        try {
            Path extensionDir = outputDir.resolve(module.extensionId());
            Path typeFile = extensionDir.resolve("index.d.ts");
            
            if (dryRun) {
                LOGGER.info("Would write: {} ({} bytes)", 
                    typeFile, module.dtsSource().length());
                return true;
            }
            
            // Create extension directory
            Files.createDirectories(extensionDir);
            
            // Write type file
            Files.writeString(typeFile, module.dtsSource());
            
            LOGGER.debug("Exported types for extension: {} -> {}", 
                module.extensionId(), typeFile);
            
            return true;
            
        } catch (IOException e) {
            LOGGER.error("Failed to export types for extension: {}", 
                module.extensionId(), e);
            return false;
        }
    }
    
    /**
     * Creates the output directory structure.
     */
    private void createOutputDirectory() throws IOException {
        if (Files.exists(outputDir)) {
            if (!Files.isDirectory(outputDir)) {
                throw new IOException("Output path exists but is not a directory: " + outputDir);
            }
        } else {
            Files.createDirectories(outputDir);
            LOGGER.debug("Created output directory: {}", outputDir);
        }
    }
    
    /**
     * Removes stale type files for extensions that no longer exist.
     */
    private void cleanupStaleEntries(Set<String> currentExtensionIds) throws IOException {
        if (!Files.exists(outputDir)) {
            return;
        }
        
        try (var stream = Files.list(outputDir)) {
            stream.filter(Files::isDirectory)
                 .map(Path::getFileName)
                 .map(Path::toString)
                 .filter(dirName -> !currentExtensionIds.contains(dirName))
                 .forEach(dirName -> {
                     try {
                         Path staleDir = outputDir.resolve(dirName);
                         deleteDirectory(staleDir);
                         LOGGER.debug("Removed stale type directory: {}", staleDir);
                     } catch (IOException e) {
                         LOGGER.warn("Failed to remove stale directory: {}", dirName, e);
                     }
                 });
        }
    }
    
    /**
     * Recursively deletes a directory.
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var stream = Files.walk(directory)) {
                stream.sorted((a, b) -> b.compareTo(a)) // Delete files first
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                         } catch (IOException e) {
                             LOGGER.warn("Failed to delete: {}", path, e);
                         }
                     });
            }
        }
    }
    
    /**
     * Generates the required tsconfig snippet for IDE integration.
     */
    public void generateTsconfigSnippet() {
        String snippet = """
            {
              "compilerOptions": {
                "baseUrl": ".",
                "paths": {
                  "@tapestry/*": [".tapestry/types/*"]
                }
              }
            }
            """;
        
        Path tsconfigFile = outputDir.resolve("tsconfig.json");
        
        if (dryRun) {
            LOGGER.info("Would write tsconfig snippet to: {}", tsconfigFile);
            return;
        }
        
        try {
            Files.writeString(tsconfigFile, snippet);
            LOGGER.info("Generated tsconfig snippet: {}", tsconfigFile);
        } catch (IOException e) {
            LOGGER.warn("Failed to generate tsconfig snippet", e);
        }
    }
    
    /**
     * Gets memory usage statistics.
     */
    public void printMemoryUsage() {
        long memoryFootprint = typeRegistry.getMemoryFootprint();
        LOGGER.info("Type registry memory footprint: {} bytes ({} MB)", 
            memoryFootprint, memoryFootprint / (1024.0 * 1024.0));
    }
    
    /**
     * Validates the workspace invariant.
     */
    public boolean validateWorkspaceInvariant() {
        // Check that output directory exists and is writable
        if (!dryRun) {
            try {
                if (!Files.exists(outputDir)) {
                    Files.createDirectories(outputDir);
                }
                
                // Test write permission
                Path testFile = outputDir.resolve(".test");
                Files.writeString(testFile, "test");
                Files.delete(testFile);
                
            } catch (IOException e) {
                LOGGER.error("Output directory is not writable: {}", outputDir, e);
                return false;
            }
        }
        
        // Check that type registry is in a valid state
        if (typeRegistry.getAllExtensionIds().isEmpty()) {
            LOGGER.warn("No extensions with type exports found");
            return true; // Not an error, just warning
        }
        
        return true;
    }
}
