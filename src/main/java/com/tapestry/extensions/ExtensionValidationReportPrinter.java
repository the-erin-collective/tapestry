package com.tapestry.extensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Prints formatted validation reports for extensions.
 */
public class ExtensionValidationReportPrinter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionValidationReportPrinter.class);
    
    /**
     * Prints a complete validation report.
     * 
     * @param result validation result to print
     */
    public static void printReport(ExtensionValidationResult result) {
        LOGGER.info("=== Extension Validation Report ===");
        
        printEnabledExtensions(result.enabled());
        printRejectedExtensions(result.rejected());
        printWarnings(result.warnings());
        
        LOGGER.info("=== Validation Complete ===");
    }
    
    /**
     * Prints successfully validated extensions.
     */
    private static void printEnabledExtensions(java.util.Map<String, ValidatedExtension> enabled) {
        if (enabled.isEmpty()) {
            LOGGER.info("No extensions enabled.");
            return;
        }
        
        LOGGER.info("Enabled Extensions ({}):", enabled.size());
        for (var entry : enabled.entrySet()) {
            var extension = entry.getValue();
            String modId = extension.sourceMod().getMetadata().getId();
            
            LOGGER.info("  ✓ {} (from {})", extension.descriptor().id(), modId);
            
            // Print capabilities
            if (!extension.capabilitiesResolved().isEmpty()) {
                LOGGER.info("    Capabilities:");
                for (var capability : extension.capabilitiesResolved()) {
                    String exclusiveFlag = capability.exclusive() ? " [exclusive]" : "";
                    LOGGER.info("      - {} ({}){}", 
                        capability.name(), capability.type(), exclusiveFlag);
                }
            }
            
            // Print dependencies
            if (!extension.resolvedDependencies().isEmpty()) {
                LOGGER.info("    Dependencies: {}", String.join(", ", extension.resolvedDependencies()));
            }
        }
    }
    
    /**
     * Prints rejected extensions with errors.
     */
    private static void printRejectedExtensions(List<RejectedExtension> rejected) {
        if (rejected.isEmpty()) {
            return;
        }
        
        LOGGER.error("Rejected Extensions ({}):", rejected.size());
        for (var extension : rejected) {
            String modId = extension.sourceMod().getMetadata().getId();
            
            LOGGER.error("  ✗ {} (from {})", extension.descriptor().id(), modId);
            
            // Print errors
            for (var error : extension.errors()) {
                String prefix = error.severity() == Severity.ERROR ? "ERROR" : 
                               error.severity() == Severity.WARN ? "WARN" : "INFO";
                LOGGER.error("    {} [{}]: {}", prefix, error.code(), error.message());
            }
        }
    }
    
    /**
     * Prints global validation warnings.
     */
    private static void printWarnings(List<ValidationMessage> warnings) {
        if (warnings.isEmpty()) {
            return;
        }
        
        LOGGER.warn("Global Warnings ({}):", warnings.size());
        for (var warning : warnings) {
            String prefix = warning.severity() == Severity.ERROR ? "ERROR" : 
                           warning.severity() == Severity.WARN ? "WARN" : "INFO";
            String extensionInfo = warning.extensionId() != null ? 
                " [" + warning.extensionId() + "]" : "";
            
            LOGGER.warn("  {} [{}]{}: {}", prefix, warning.code(), extensionInfo, warning.message());
        }
    }
    
    /**
     * Prints a summary of validation results.
     * 
     * @param result validation result
     */
    public static void printSummary(ExtensionValidationResult result) {
        int totalExtensions = result.enabled().size() + result.rejected().size();
        int errorCount = result.rejected().stream()
            .mapToInt(r -> r.errors().size())
            .sum();
        int warningCount = result.warnings().size() + 
            result.rejected().stream()
                .flatMap(r -> r.errors().stream())
                .mapToInt(e -> e.severity() == Severity.WARN ? 1 : 0)
                .sum();
        
        LOGGER.info("Validation Summary:");
        LOGGER.info("  Total extensions: {}", totalExtensions);
        LOGGER.info("  Enabled: {}", result.enabled().size());
        LOGGER.info("  Rejected: {}", result.rejected().size());
        LOGGER.info("  Errors: {}", errorCount);
        LOGGER.info("  Warnings: {}", warningCount);
        
        if (result.rejected().isEmpty()) {
            LOGGER.info("✓ All extensions passed validation");
        } else {
            LOGGER.warn("⚠ Some extensions were rejected");
        }
    }
}
