package com.tapestry.extensions.types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simplified GraalVM integration for Phase 14 type resolution.
 * 
 * Provides type resolution without complex FileSystem abstraction.
 * Focuses on core functionality: type contract validation and resolution.
 */
public class GraalVMTypeIntegration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GraalVMTypeIntegration.class);
    
    private final TapestryTypeResolver typeResolver;
    private final ExtensionTypeRegistry typeRegistry;
    
    public GraalVMTypeIntegration(ExtensionTypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
        this.typeResolver = new TapestryTypeResolver(typeRegistry);
    }
    
    /**
     * Gets type resolver for use by other components.
     */
    public TapestryTypeResolver getTypeResolver() {
        return typeResolver;
    }
    
    /**
     * Gets type registry for type contract management.
     */
    public ExtensionTypeRegistry getTypeRegistry() {
        return typeRegistry;
    }
    
    /**
     * Sets current extension context for type resolution.
     */
    public void setCurrentExtension(String extensionId) {
        typeResolver.setCurrentExtension(extensionId);
    }
    
    /**
     * Initializes integration.
     */
    public void initialize() {
        LOGGER.info("Phase 14 GraalVM type integration initialized (simplified)");
    }
    
    /**
     * Validates type import at runtime.
     * Simplified approach without FileSystem interception.
     */
    public boolean validateTypeImport(String importingExtension, String targetExtension, String typeName) {
        try {
            // Check if target extension exists and exports types
            if (!typeRegistry.hasTypeExports(targetExtension)) {
                LOGGER.warn("Extension '{}' imports types from '{}' but '{}' does not export types", 
                    importingExtension, targetExtension, targetExtension);
                return false;
            }
            
            // Check if target extension is a required dependency
            if (!typeRegistry.isTypeDependency(importingExtension, targetExtension)) {
                LOGGER.warn("Extension '{}' imports types from '{}' but it's not a declared dependency", 
                    importingExtension, targetExtension);
                return false;
            }
            
            // Check if type exists in target's exports
            if (!typeRegistry.hasTypeExport(targetExtension, typeName)) {
                LOGGER.warn("Extension '{}' imports type '{}' from '{}' but type is not exported", 
                    importingExtension, typeName, targetExtension);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to validate type import from '{}' to '{}' for type '{}'", 
                importingExtension, targetExtension, typeName, e);
            return false;
        }
    }
    
    /**
     * Gets type definition for IDE support.
     * Returns synthetic TypeScript definition content.
     */
    public String getTypeDefinition(String extensionId, String typeName) {
        try {
            return typeResolver.resolveModule("@tapestry/" + extensionId, "type");
        } catch (TapestryTypeResolver.TapestryTypeResolutionException e) {
            LOGGER.error("Failed to resolve type definition for '{}' in extension '{}'", 
                typeName, extensionId, e);
            return null;
        }
    }
    
    /**
     * Gets all type exports for an extension.
     */
    public Map<String, String> getTypeExports(String extensionId) {
        try {
            Map<String, String> exports = new HashMap<>();
            
            // Get type definition content
            String typeContent = typeResolver.resolveModule("@tapestry/" + extensionId, "type");
            if (typeContent != null) {
                exports.put("types", typeContent);
            }
            
            return exports;
        } catch (Exception e) {
            LOGGER.error("Failed to get type exports for extension '{}'", extensionId, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Checks if integration is ready for use.
     */
    public boolean isReady() {
        return typeRegistry != null && typeResolver != null;
    }
}
