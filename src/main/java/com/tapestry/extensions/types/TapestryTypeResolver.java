package com.tapestry.extensions.types;

import com.tapestry.extensions.ExtensionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Virtual GraalVM resolver for @tapestry/* namespace.
 * 
 * Phase 14: Intercepts module imports and returns synthetic stubs.
 * Enforces that only import type is allowed for @tapestry/* namespace.
 */
public class TapestryTypeResolver {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryTypeResolver.class);
    
    // The exact stub to return for all @tapestry/* imports
    private static final String SYNTHETIC_STUB = "export {};";
    
    // Cache of authorized type imports per extension
    private final Set<String> authorizedImports = ConcurrentHashMap.newKeySet();
    
    // Reference to type registry
    private final ExtensionTypeRegistry typeRegistry;
    
    // Current extension ID being resolved (for authorization checks)
    private final ThreadLocal<String> currentExtensionId = new ThreadLocal<>();
    
    public TapestryTypeResolver(ExtensionTypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }
    
    /**
     * Sets the current extension context for resolution.
     * Must be called before resolving imports for an extension.
     */
    public void setCurrentExtension(String extensionId) {
        this.currentExtensionId.set(extensionId);
        
        // Initialize authorized imports for this extension if not already done
        if (authorizedImports.isEmpty()) {
            // This would be populated from the extension's typeImports list
            // For now, we'll check against the type registry at runtime
        }
    }
    
    /**
     * Clears the current extension context.
     */
    public void clearCurrentExtension() {
        this.currentExtensionId.remove();
    }
    
    /**
     * Resolves a module import according to Phase 14 rules.
     * 
     * @param modulePath The module path to resolve (e.g., "@tapestry/foo")
     * @param importType The type of import ("type" or "value")
     * @return The resolved module source, or throws exception
     * @throws TapestryTypeResolutionException if resolution fails
     */
    public String resolveModule(String modulePath, String importType) throws TapestryTypeResolutionException {
        LOGGER.debug("Resolving module: {} (import type: {})", modulePath, importType);
        
        // Step 1: Validate namespace
        if (!modulePath.startsWith("@tapestry/")) {
            throw new TapestryTypeResolutionException(
                TypeValidationError.INVALID_TAPESTRY_NAMESPACE,
                String.format("Module '%s' is not in @tapestry/* namespace", modulePath)
            );
        }
        
        // Step 2: Enforce import type restrictions
        if (!"type".equals(importType)) {
            throw new TapestryTypeResolutionException(
                TypeValidationError.RUNTIME_IMPORT_FORBIDDEN,
                String.format(
                    "Runtime imports are forbidden for @tapestry/* namespace. " +
                    "Only 'import type' is allowed. Attempted: %s import of '%s'",
                    importType, modulePath
                )
            );
        }
        
        // Step 3: Parse extension ID
        String extensionId = parseExtensionId(modulePath);
        
        // Step 4: Validate extension ID format
        if (!isValidExtensionId(extensionId)) {
            throw new TapestryTypeResolutionException(
                TypeValidationError.INVALID_TAPESTRY_NAMESPACE,
                String.format("Invalid extension ID in module path: %s", modulePath)
            );
        }
        
        // Step 5: Verify authorization (undeclared type import check)
        if (!isAuthorizedImport(extensionId)) {
            throw new TapestryTypeResolutionException(
                TypeValidationError.UNDECLARED_TYPE_IMPORT,
                String.format(
                    "Extension '%s' does not declare type import from '%s'. " +
                    "Add '%s' to typeImports in descriptor.",
                    getCurrentExtensionId(), extensionId, extensionId
                )
            );
        }
        
        // Step 6: Verify target exports types
        if (!typeRegistry.hasTypeModule(extensionId)) {
            throw new TapestryTypeResolutionException(
                TypeValidationError.TARGET_DOES_NOT_EXPORT_TYPES,
                String.format(
                    "Extension '%s' does not export any types but is imported by '%s'",
                    extensionId, getCurrentExtensionId()
                )
            );
        }
        
        // Step 7: Return synthetic stub
        LOGGER.debug("Returning synthetic stub for @tapestry/{}", extensionId);
        return SYNTHETIC_STUB;
    }
    
    /**
     * Parses extension ID from module path.
     * 
     * @param modulePath The full module path (e.g., "@tapestry/foo")
     * @return The extension ID (e.g., "foo")
     * @throws TapestryTypeResolutionException if path has additional segments
     */
    private String parseExtensionId(String modulePath) throws TapestryTypeResolutionException {
        // Remove @tapestry/ prefix
        String withoutPrefix = modulePath.substring("@tapestry/".length());
        
        // Check for additional path segments (not allowed)
        if (withoutPrefix.contains("/")) {
            throw new TapestryTypeResolutionException(
                TypeValidationError.INVALID_TAPESTRY_NAMESPACE,
                String.format(
                    "Subpaths are not allowed in @tapestry/* imports: %s",
                    modulePath
                )
            );
        }
        
        return withoutPrefix;
    }
    
    /**
     * Validates extension ID format.
     */
    private boolean isValidExtensionId(String extensionId) {
        if (extensionId == null || extensionId.isEmpty()) {
            return false;
        }
        
        // Extension IDs must match: [a-z][a-z0-9_]*
        return extensionId.matches("^[a-z][a-z0-9_]*$");
    }
    
    /**
     * Checks if the current extension is authorized to import from the target extension.
     */
    private boolean isAuthorizedImport(String targetExtensionId) {
        String currentExt = getCurrentExtensionId();
        if (currentExt == null) {
            return false;
        }
        
        // Critical fix: Use explicit authorization instead of just checking if target exports types
        return isExplicitlyAuthorized(currentExt, targetExtensionId);
    }
    
    /**
     * Gets the current extension ID from thread-local context.
     */
    private String getCurrentExtensionId() {
        return currentExtensionId.get();
    }
    
    /**
     * Authorizes type imports for an extension.
     * This should be called during extension activation.
     * 
     * @param extensionId The extension ID
     * @param typeImports List of authorized type import targets
     */
    public void authorizeTypeImports(String extensionId, java.util.List<String> typeImports) {
        for (String targetId : typeImports) {
            authorizedImports.add(extensionId + ":" + targetId);
        }
        LOGGER.debug("Authorized {} type imports for extension: {}", typeImports.size(), extensionId);
    }
    
    /**
     * Checks if a specific import is authorized.
     */
    private boolean isExplicitlyAuthorized(String extensionId, String targetExtensionId) {
        return authorizedImports.contains(extensionId + ":" + targetExtensionId);
    }
    
    /**
     * Gets memory usage statistics.
     */
    public long getMemoryUsage() {
        return authorizedImports.size() * 64L; // Rough estimate per string
    }
    
    /**
     * Clears all authorizations (for testing).
     */
    public void clearAuthorizations() {
        authorizedImports.clear();
    }
    
    /**
     * Exception thrown for type resolution errors.
     */
    public static class TapestryTypeResolutionException extends Exception {
        private final TypeValidationError error;
        
        public TapestryTypeResolutionException(TypeValidationError error, String message) {
            super(message);
            this.error = error;
        }
        
        public TypeValidationError getError() {
            return error;
        }
        
        public String getErrorCode() {
            return error.getCode();
        }
    }
}
