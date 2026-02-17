package com.tapestry.extensions.types;

import com.tapestry.extensions.TapestryExtensionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Validates type-related aspects of extensions for Phase 14.
 */
public class TypeValidator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeValidator.class);
    
    // Phase 14 invariant: Maximum .d.ts file size is 1MB
    private static final long MAX_TYPE_FILE_SIZE = 1024 * 1024; // 1MB
    
    // Pattern for detecting ambient declarations
    private static final Pattern AMBIENT_GLOBAL_PATTERN = Pattern.compile("\\bdeclare\\s+global\\b");
    private static final Pattern AMBIENT_MODULE_PATTERN = Pattern.compile("\\bdeclare\\s+module\\b");
    
    /**
     * Validate type-related aspects of an extension descriptor.
     * 
     * @param descriptor The extension descriptor to validate
     * @param extensionRoot The extension's root directory
     * @param allDescriptors All extension descriptors for cross-validation
     * @return List of validation errors
     */
    public List<TypeValidationResult> validateDescriptor(
            TapestryExtensionDescriptor descriptor,
            Path extensionRoot,
            Map<String, TapestryExtensionDescriptor> allDescriptors) {
        
        List<TypeValidationResult> errors = new ArrayList<>();
        
        // Rule 1: Subset constraint - typeImports must be subset of requiredDependencies
        validateSubsetConstraint(descriptor, errors);
        
        // Rule 2: Export existence - validate typeExportEntry if present
        validateTypeExportEntry(descriptor, extensionRoot, errors);
        
        // Rule 5: No ambient pollution - validate .d.ts content
        validateNoAmbientPollution(descriptor, extensionRoot, errors);
        
        // Rule 6: File size limits
        validateFileSizeLimits(descriptor, extensionRoot, errors);
        
        return errors;
    }
    
    /**
     * Validate cross-extension type import relationships.
     * 
     * @param descriptors All extension descriptors
     * @return List of validation errors
     */
    public List<TypeValidationResult> validateCrossExtensionRelations(
            Map<String, TapestryExtensionDescriptor> descriptors) {
        
        List<TypeValidationResult> errors = new ArrayList<>();
        
        // Rule 2: Export existence - verify all typeImport targets export types
        validateTypeImportTargets(descriptors, errors);
        
        return errors;
    }
    
    /**
     * Validate that type imports are a subset of required dependencies.
     * Rule 1: Subset Constraint
     */
    private void validateSubsetConstraint(
            TapestryExtensionDescriptor descriptor,
            List<TypeValidationResult> errors) {
        
        Set<String> requiredDeps = new HashSet<>(descriptor.requires());
        Set<String> typeImports = new HashSet<>(descriptor.typeImports());
        
        // Check if any type import is not in required dependencies
        Set<String> invalidImports = new HashSet<>(typeImports);
        invalidImports.removeAll(requiredDeps);
        
        if (!invalidImports.isEmpty()) {
            errors.add(new TypeValidationResult(
                TypeValidationError.TYPE_IMPORT_NOT_REQUIRED_DEPENDENCY,
                descriptor.id(),
                String.format(
                    "Extension '%s' imports types from [%s] but does not list them as required dependencies. " +
                    "Add these to requiredDependencies: %s",
                    descriptor.id(),
                    String.join(", ", invalidImports),
                    String.join(", ", invalidImports)
                )
            ));
        }
    }
    
    /**
     * Validate type export entry file existence and accessibility.
     * Rule 2: Export Existence (partial)
     */
    private void validateTypeExportEntry(
            TapestryExtensionDescriptor descriptor,
            Path extensionRoot,
            List<TypeValidationResult> errors) {
        
        if (descriptor.typeExportEntry().isEmpty()) {
            return; // No type export is valid
        }
        
        String typeExportPath = descriptor.typeExportEntry().get();
        Path typeFile = extensionRoot.resolve(typeExportPath);
        
        // Check if file exists
        if (!Files.exists(typeFile)) {
            errors.add(new TypeValidationResult(
                TypeValidationError.TYPE_EXPORT_FILE_NOT_FOUND,
                descriptor.id(),
                String.format(
                    "Type export file not found: %s (resolved to %s)",
                    typeExportPath,
                    typeFile
                )
            ));
            return;
        }
        
        // Check if it's a file (not directory)
        if (!Files.isRegularFile(typeFile)) {
            errors.add(new TypeValidationResult(
                TypeValidationError.TYPE_EXPORT_FILE_NOT_FOUND,
                descriptor.id(),
                String.format(
                    "Type export path is not a file: %s",
                    typeFile
                )
            ));
        }
    }
    
    /**
     * Validate that exported .d.ts files contain no ambient declarations.
     * Rule 5: No Ambient Pollution
     */
    private void validateNoAmbientPollution(
            TapestryExtensionDescriptor descriptor,
            Path extensionRoot,
            List<TypeValidationResult> errors) {
        
        if (descriptor.typeExportEntry().isEmpty()) {
            return;
        }
        
        Path typeFile = extensionRoot.resolve(descriptor.typeExportEntry().get());
        
        try {
            String content = Files.readString(typeFile);
            
            // Check for ambient declarations
            if (AMBIENT_GLOBAL_PATTERN.matcher(content).find()) {
                errors.add(new TypeValidationResult(
                    TypeValidationError.AMBIENT_DECLARATION_FORBIDDEN,
                    descriptor.id(),
                    "Type export file contains 'declare global' which is forbidden"
                ));
            }
            
            if (AMBIENT_MODULE_PATTERN.matcher(content).find()) {
                errors.add(new TypeValidationResult(
                    TypeValidationError.AMBIENT_DECLARATION_FORBIDDEN,
                    descriptor.id(),
                    "Type export file contains 'declare module' which is forbidden"
                ));
            }
            
            // Check that file contains at least one export
            if (!content.contains("export ")) {
                errors.add(new TypeValidationResult(
                    TypeValidationError.AMBIENT_DECLARATION_FORBIDDEN,
                    descriptor.id(),
                    "Type export file must contain at least one 'export' statement"
                ));
            }
            
        } catch (IOException e) {
            errors.add(new TypeValidationResult(
                TypeValidationError.TYPE_EXPORT_FILE_NOT_FOUND,
                descriptor.id(),
                String.format("Failed to read type export file: %s", e.getMessage())
            ));
        }
    }
    
    /**
     * Validate file size limits.
     * Rule 6: File Size Limits
     */
    private void validateFileSizeLimits(
            TapestryExtensionDescriptor descriptor,
            Path extensionRoot,
            List<TypeValidationResult> errors) {
        
        if (descriptor.typeExportEntry().isEmpty()) {
            return;
        }
        
        Path typeFile = extensionRoot.resolve(descriptor.typeExportEntry().get());
        
        try {
            long fileSize = Files.size(typeFile);
            
            if (fileSize > MAX_TYPE_FILE_SIZE) {
                errors.add(new TypeValidationResult(
                    TypeValidationError.TYPE_EXPORT_FILE_TOO_LARGE,
                    descriptor.id(),
                    String.format(
                        "Type export file size (%d bytes) exceeds maximum allowed size (%d bytes)",
                        fileSize,
                        MAX_TYPE_FILE_SIZE
                    )
                ));
            }
            
        } catch (IOException e) {
            errors.add(new TypeValidationResult(
                TypeValidationError.TYPE_EXPORT_FILE_NOT_FOUND,
                descriptor.id(),
                String.format("Failed to check file size: %s", e.getMessage())
            ));
        }
    }
    
    /**
     * Validate that all type import targets actually export types.
     * Rule 2: Export Existence (cross-extension)
     */
    private void validateTypeImportTargets(
            Map<String, TapestryExtensionDescriptor> descriptors,
            List<TypeValidationResult> errors) {
        
        for (TapestryExtensionDescriptor descriptor : descriptors.values()) {
            for (String typeImport : descriptor.typeImports()) {
                
                // Check if target extension exists
                TapestryExtensionDescriptor target = descriptors.get(typeImport);
                if (target == null) {
                    errors.add(new TypeValidationResult(
                        TypeValidationError.DEPENDENCY_NOT_FOUND,
                        descriptor.id(),
                        String.format(
                            "Type import target '%s' does not exist",
                            typeImport
                        )
                    ));
                    continue;
                }
                
                // Check if target exports types
                if (target.typeExportEntry().isEmpty()) {
                    errors.add(new TypeValidationResult(
                        TypeValidationError.TARGET_DOES_NOT_EXPORT_TYPES,
                        descriptor.id(),
                        String.format(
                            "Extension '%s' does not export any types but is imported by '%s'. " +
                            "Target extension must declare typeExportEntry.",
                            typeImport,
                            descriptor.id()
                        )
                    ));
                }
            }
        }
    }
    
    /**
     * Result of a type validation operation.
     */
    public static record TypeValidationResult(
        TypeValidationError error,
        String extensionId,
        String message
    ) {}
}
