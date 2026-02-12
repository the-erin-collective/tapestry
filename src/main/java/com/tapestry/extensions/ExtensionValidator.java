package com.tapestry.extensions;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates extension descriptors according to Phase 3 rules.
 * Enforces no side effects during validation.
 */
public class ExtensionValidator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionValidator.class);
    
    // Validation patterns
    private static final Pattern EXTENSION_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");
    private static final Pattern CAPABILITY_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$");
    
    private final Version currentTapestryVersion;
    private final ValidationPolicy policy;
    
    public ExtensionValidator(Version currentTapestryVersion, ValidationPolicy policy) {
        this.currentTapestryVersion = currentTapestryVersion;
        this.policy = policy;
    }
    
    /**
     * Validates all discovered extension providers.
     * 
     * @param providers list of discovered providers
     * @return validation result
     */
    public ExtensionValidationResult validate(List<DiscoveredExtensionProvider> providers) {
        // Enforce no side effects
        PhaseController.getInstance().requirePhase(TapestryPhase.VALIDATION);
        
        TreeMap<String, ValidatedExtension> enabled = new TreeMap<>();
        ArrayList<RejectedExtension> rejected = new ArrayList<>();
        ArrayList<ValidationMessage> warnings = new ArrayList<>();
        
        try {
            // Step 1: Group providers by ID for duplicate detection
            Map<String, List<DiscoveredExtensionProvider>> providersById = groupProvidersById(providers);
            
            // Step 2: Reject all duplicates
            rejectDuplicateIds(providersById, rejected);
            
            // Step 3: Validate individual descriptors (non-duplicates only)
            Map<String, TapestryExtensionDescriptor> descriptors = new HashMap<>();
            for (var entry : providersById.entrySet()) {
                if (entry.getValue().size() == 1) {
                    var provider = entry.getValue().get(0);
                    var descriptor = provider.descriptor();
                    descriptors.put(descriptor.id(), descriptor);
                }
            }
            
            // Now resolve dependencies for all valid descriptors
            Map<String, List<String>> resolvedDepsByExtension = new HashMap<>();
            for (var entry : providersById.entrySet()) {
                if (entry.getValue().size() == 1) {
                    var provider = entry.getValue().get(0);
                    var descriptor = provider.descriptor();
                    ArrayList<ValidationMessage> errors = new ArrayList<>();
                    String modId = provider.sourceMod().getMetadata().getId();
                    
                    // Rule Group A: Descriptor shape
                    validateDescriptorShape(descriptor, errors, modId);
                    
                    // Rule Group C: Tapestry version compatibility
                    validateVersionCompatibility(descriptor, errors, modId);
                    
                    // Rule Group D: Capability well-formedness
                    validateCapabilityWellFormedness(descriptor, errors, modId);
                    
                    if (errors.isEmpty()) {
                        // Valid descriptor - resolve dependencies using complete descriptors map
                        var resolvedDeps = resolveDependencies(descriptor, descriptors, warnings, modId);
                        resolvedDepsByExtension.put(descriptor.id(), resolvedDeps);
                        
                        var validated = new ValidatedExtension(
                            descriptor,
                            provider.sourceMod(),
                            descriptor.capabilities(),
                            resolvedDepsByExtension.get(descriptor.id())
                        );
                        enabled.put(descriptor.id(), validated);
                    } else {
                        // Invalid descriptor
                        var rejectedExt = new RejectedExtension(descriptor, provider.sourceMod(), errors);
                        rejected.add(rejectedExt);
                    }
                }
            }
            
            // Step 4: Global validation (conflicts, cycles)
            performGlobalValidation(enabled, rejected, warnings);
            
            // Step 5: Required dependency validation (must be enabled)
            validateRequiredDependenciesEnabled(enabled, rejected);
            
            // Step 6: Handle policy
            handlePolicy(enabled, rejected, warnings);
            
            LOGGER.info("Extension validation complete: {} enabled, {} rejected, {} warnings",
                enabled.size(), rejected.size(), warnings.size());
            
            return new ExtensionValidationResult(enabled, rejected, warnings);
            
        } finally {
            // No explicit cleanup needed - phase enforcement is enough
        }
    }
    
    /**
     * Groups providers by extension ID for duplicate detection.
     */
    private Map<String, List<DiscoveredExtensionProvider>> groupProvidersById(List<DiscoveredExtensionProvider> providers) {
        Map<String, List<DiscoveredExtensionProvider>> byId = new TreeMap<>();
        
        for (var provider : providers) {
            var descriptor = provider.descriptor();
            if (descriptor != null && descriptor.id() != null) {
                byId.computeIfAbsent(descriptor.id(), k -> new ArrayList<>()).add(provider);
            }
        }
        
        return byId;
    }
    
    /**
     * Rejects all providers with duplicate IDs.
     */
    private void rejectDuplicateIds(
            Map<String, List<DiscoveredExtensionProvider>> providersById,
            List<RejectedExtension> rejected) {
        
        for (var entry : providersById.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Reject ALL providers with this ID
                for (var provider : entry.getValue()) {
                    var errors = List.of(new ValidationMessage(
                        Severity.ERROR, "DUPLICATE_ID", 
                        "Extension ID '" + entry.getKey() + "' is duplicated", 
                        provider.descriptor().id()));
                    
                    var rejectedExt = new RejectedExtension(
                        provider.descriptor(), 
                        provider.sourceMod(), 
                        errors);
                    rejected.add(rejectedExt);
                }
            }
        }
    }
    
    /**
     * Validates that required dependencies are enabled extensions.
     */
    private void validateRequiredDependenciesEnabled(
            TreeMap<String, ValidatedExtension> enabled,
            List<RejectedExtension> rejected) {
        
        // Create set of enabled extension IDs
        Set<String> enabledIds = enabled.keySet();
        
        // Check each enabled extension's required dependencies
        List<ValidatedExtension> toReject = new ArrayList<>();
        
        for (var validated : enabled.values()) {
            List<String> missingDeps = new ArrayList<>();
            
            if (validated.descriptor().requires() != null) {
                for (String requiredDep : validated.descriptor().requires()) {
                    if (!enabledIds.contains(requiredDep)) {
                        missingDeps.add(requiredDep);
                    }
                }
            }
            
            if (!missingDeps.isEmpty()) {
                // Mark for rejection
                toReject.add(validated);
                
                var errors = List.of(new ValidationMessage(
                    Severity.ERROR, "MISSING_REQUIRED_DEPENDENCY", 
                    "Required dependencies not enabled: " + String.join(", ", missingDeps), 
                    validated.descriptor().id()));
                
                var rejectedExt = new RejectedExtension(
                    validated.descriptor(), 
                    validated.sourceMod(), 
                    errors);
                rejected.add(rejectedExt);
            }
        }
        
        // Remove rejected extensions from enabled
        for (var toRemove : toReject) {
            enabled.remove(toRemove.descriptor().id());
        }
    }
    
    /**
     * Handles failFast and disableInvalid policies.
     */
    private void handlePolicy(
            TreeMap<String, ValidatedExtension> enabled,
            List<RejectedExtension> rejected,
            List<ValidationMessage> warnings) {
        
        // Handle failFast policy
        if (policy.failFast() && rejected.stream()
                .anyMatch(r -> r.errors().stream()
                        .anyMatch(e -> e.severity() == Severity.ERROR))) {
            // Find first error and throw to abort
            var firstError = rejected.stream()
                .flatMap(r -> r.errors().stream())
                .filter(e -> e.severity() == Severity.ERROR)
                .findFirst()
                .orElseThrow();
            
            throw new RuntimeException("Validation failed fast: " + firstError.message());
        }
        
        // Handle disableInvalid policy
        if (!policy.disableInvalid()) {
            // If we're not disabling invalid extensions, any ERROR should fail startup
            var hasAnyErrors = rejected.stream()
                .anyMatch(r -> r.errors().stream()
                        .anyMatch(e -> e.severity() == Severity.ERROR));
            
            if (hasAnyErrors) {
                var firstError = rejected.stream()
                    .flatMap(r -> r.errors().stream())
                    .filter(e -> e.severity() == Severity.ERROR)
                    .findFirst()
                    .orElseThrow();
                
                throw new RuntimeException("Validation failed: " + firstError.message());
            }
        }
    }
    
    /**
     * Rule Group A: Descriptor shape validation.
     */
    private void validateDescriptorShape(
            TapestryExtensionDescriptor descriptor,
            List<ValidationMessage> errors,
            String modId) {
        
        // ID validation
        if (descriptor.id() == null || descriptor.id().trim().isEmpty()) {
            errors.add(new ValidationMessage(
                Severity.ERROR, "MISSING_ID", 
                "Extension ID is required", descriptor.id()));
            return;
        }
        
        if (!EXTENSION_ID_PATTERN.matcher(descriptor.id()).matches()) {
            errors.add(new ValidationMessage(
                Severity.ERROR, "INVALID_ID", 
                "Extension ID must match pattern: " + EXTENSION_ID_PATTERN.pattern(), 
                descriptor.id()));
        }
        
        // Capabilities validation
        if (descriptor.capabilities() == null || descriptor.capabilities().isEmpty()) {
            errors.add(new ValidationMessage(
                Severity.ERROR, "MISSING_CAPABILITIES", 
                "Extension must declare at least one capability", descriptor.id()));
        }
    }
    
    /**
     * Rule Group C: Tapestry version compatibility.
     */
    private void validateVersionCompatibility(
            TapestryExtensionDescriptor descriptor,
            List<ValidationMessage> errors,
            String modId) {
        
        if (descriptor.minTapestry() == null || descriptor.minTapestry().trim().isEmpty()) {
            errors.add(new ValidationMessage(
                Severity.ERROR, "MISSING_MIN_VERSION", 
                "Extension must specify minimum Tapestry version", descriptor.id()));
            return;
        }
        
        try {
            Version minVersion = Version.parse(descriptor.minTapestry());
            if (!currentTapestryVersion.isAtLeast(minVersion)) {
                errors.add(new ValidationMessage(
                    Severity.ERROR, "VERSION_TOO_LOW", 
                    String.format("Extension requires Tapestry %s but current is %s", 
                        minVersion, currentTapestryVersion), descriptor.id()));
            }
        } catch (IllegalArgumentException e) {
            errors.add(new ValidationMessage(
                Severity.ERROR, "INVALID_VERSION_FORMAT", 
                "Invalid minimum version format: " + e.getMessage(), descriptor.id()));
        }
    }
    
    /**
     * Rule Group D: Capability well-formedness.
     */
    private void validateCapabilityWellFormedness(
            TapestryExtensionDescriptor descriptor,
            List<ValidationMessage> errors,
            String modId) {
        
        // Sort capabilities by name for deterministic error reporting
        List<CapabilityDecl> sortedCapabilities = new ArrayList<>(descriptor.capabilities());
        sortedCapabilities.sort((a, b) -> a.name().compareTo(b.name()));
        
        for (var capability : sortedCapabilities) {
            // Name validation
            if (capability.name() == null || capability.name().trim().isEmpty()) {
                errors.add(new ValidationMessage(
                    Severity.ERROR, "MISSING_CAPABILITY_NAME", 
                    "Capability name is required", descriptor.id()));
                continue;
            }
            
            if (!CAPABILITY_NAME_PATTERN.matcher(capability.name()).matches()) {
                errors.add(new ValidationMessage(
                    Severity.ERROR, "INVALID_CAPABILITY_NAME", 
                    "Capability name must match pattern: " + CAPABILITY_NAME_PATTERN.pattern(), 
                    descriptor.id()));
            }
            
            // Type validation
            if (capability.type() == null) {
                errors.add(new ValidationMessage(
                    Severity.ERROR, "MISSING_CAPABILITY_TYPE", 
                    "Capability type is required", descriptor.id()));
            }
            
            // Meta validation (permissive)
            validateCapabilityMeta(capability, errors, descriptor.id());
        }
    }
    
    /**
     * Validates capability metadata fields.
     */
    private void validateCapabilityMeta(
            CapabilityDecl capability,
            List<ValidationMessage> errors,
            String extensionId) {
        
        if (capability.meta() == null) return;
        
        for (var entry : capability.meta().entrySet()) {
            // Check for null keys
            if (entry.getKey() == null) {
                errors.add(new ValidationMessage(
                    Severity.ERROR, "NULL_META_KEY", 
                    "Capability meta keys cannot be null", extensionId));
                continue;
            }
            
            // Check for non-serializable values
            Object value = entry.getValue();
            if (value != null && !isSerializableType(value)) {
                errors.add(new ValidationMessage(
                    Severity.ERROR, "INVALID_META_VALUE_TYPE", 
                    "Capability meta values must be serializable (String, Number, Boolean, List, Map)", 
                    extensionId));
            }
        }
    }
    
    /**
     * Checks if a value is a serializable type for meta validation.
     */
    private boolean isSerializableType(Object value) {
        return value instanceof String ||
               value instanceof Number ||
               value instanceof Boolean ||
               (value instanceof List && isSerializableList((List<?>) value)) ||
               (value instanceof Map && isSerializableMap((Map<?, ?>) value));
    }
    
    @SuppressWarnings("unchecked")
    private boolean isSerializableList(List<?> list) {
        return list.stream().allMatch(this::isSerializableType);
    }
    
    @SuppressWarnings("unchecked")
    private boolean isSerializableMap(Map<?, ?> map) {
        return map.keySet().stream().allMatch(k -> k instanceof String) &&
               map.values().stream().allMatch(this::isSerializableType);
    }
    
    /**
     * Resolves extension dependencies and validates them.
     */
    private List<String> resolveDependencies(
            TapestryExtensionDescriptor descriptor,
            Map<String, TapestryExtensionDescriptor> allDescriptors,
            List<ValidationMessage> warnings,
            String modId) {
        
        ArrayList<String> resolved = new ArrayList<>();
        
        // Resolve required dependencies
        if (descriptor.requires() != null) {
            for (String dep : descriptor.requires()) {
                if (allDescriptors.containsKey(dep)) {
                    resolved.add(dep);
                }
            }
        }
        
        // Resolve optional dependencies
        if (descriptor.optional() != null && policy.warnOnOptionalMissing()) {
            for (String dep : descriptor.optional()) {
                if (allDescriptors.containsKey(dep)) {
                    resolved.add(dep);
                } else {
                    warnings.add(new ValidationMessage(
                        Severity.WARN, "MISSING_OPTIONAL_DEPENDENCY", 
                        "Optional dependency '" + dep + "' not found", descriptor.id()));
                }
            }
        }
        
        return resolved;
    }
    
    /**
     * Performs global validation across all extensions.
     */
    private void performGlobalValidation(
            TreeMap<String, ValidatedExtension> enabled,
            List<RejectedExtension> rejected,
            List<ValidationMessage> warnings) {
        
        // Rule Group E: Capability conflicts
        validateCapabilityConflicts(enabled, rejected);
        
        // Rule Group F: Dependency cycles
        validateDependencyCycles(enabled, rejected);
    }
    
    /**
     * Rule Group E: Check for capability conflicts.
     */
    private void validateCapabilityConflicts(
            TreeMap<String, ValidatedExtension> enabled,
            List<RejectedExtension> rejected) {
        
        TreeMap<String, List<String>> capabilityToExtensions = new TreeMap<>();
        
        // Collect all capabilities by name and track which extensions claim them
        for (var validated : enabled.values()) {
            for (var capability : validated.capabilitiesResolved()) {
                // Apply default exclusivity if flag is omitted
                boolean isExclusive = capability.exclusive() || 
                    (capability.type() != CapabilityType.HOOK);
                
                if (isExclusive) {
                    capabilityToExtensions.computeIfAbsent(capability.name(), k -> new ArrayList<>())
                        .add(validated.descriptor().id());
                }
            }
        }
        
        // Sort extension ID lists for deterministic processing
        for (var entry : capabilityToExtensions.entrySet()) {
            entry.getValue().sort(String::compareTo);
        }
        
        // Find conflicts
        for (var entry : capabilityToExtensions.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Conflict found - reject all conflicting extensions
                for (String extensionId : entry.getValue()) {
                    var conflictingExtension = enabled.get(extensionId);
                    if (conflictingExtension != null) {
                        var errors = List.of(new ValidationMessage(
                            Severity.ERROR, "CAPABILITY_CONFLICT", 
                            "Capability '" + entry.getKey() + "' is claimed by multiple extensions", 
                            extensionId));
                        
                        var rejectedExt = new RejectedExtension(
                            conflictingExtension.descriptor(), 
                            conflictingExtension.sourceMod(), 
                            errors);
                        rejected.add(rejectedExt);
                        
                        // Remove from enabled
                        enabled.remove(extensionId);
                    }
                }
            }
        }
    }
    
    /**
     * Rule Group F: Check for dependency cycles.
     */
    private void validateDependencyCycles(
            TreeMap<String, ValidatedExtension> enabled,
            List<RejectedExtension> rejected) {
        
        // Build dependency graph from enabled extensions using TreeMap for deterministic order
        TreeMap<String, List<String>> graph = new TreeMap<>();
        for (var validated : enabled.values()) {
            List<String> deps = new ArrayList<>(validated.resolvedDependencies());
            deps.sort(String::compareTo); // Sort dependencies for deterministic processing
            graph.put(validated.descriptor().id(), deps);
        }
        
        // Detect cycles using DFS with ordered path tracking
        Set<String> visited = new HashSet<>();
        Deque<String> path = new ArrayDeque<>();
        
        for (String extensionId : graph.keySet()) { // TreeMap provides deterministic iteration order
            // Clear path for fresh detection
            path.clear();
            if (hasCycleOrdered(extensionId, graph, visited, path)) {
                // Extract cycle path from current path
                List<String> cycle = new ArrayList<>();
                boolean foundStart = false;
                
                // Build cycle from path - find start node and collect from there
                for (String pathElement : path) {
                    if (foundStart) {
                        cycle.add(pathElement);
                    }
                    if (pathElement.equals(extensionId)) {
                        foundStart = true;
                    }
                }
                
                // Add start node again to complete the cycle
                if (!cycle.isEmpty()) {
                    cycle.add(extensionId);
                }
                
                // Reject all extensions in cycle
                for (String cycleId : cycle) {
                    var cycleExtension = enabled.get(cycleId);
                    if (cycleExtension != null) {
                        var errors = List.of(new ValidationMessage(
                            Severity.ERROR, "DEPENDENCY_CYCLE", 
                            "Dependency cycle detected: " + String.join(" -> ", cycle), 
                            cycleId));
                        
                        var rejectedExt = new RejectedExtension(
                            cycleExtension.descriptor(), 
                            cycleExtension.sourceMod(), 
                            errors);
                        rejected.add(rejectedExt);
                        
                        // Remove from enabled
                        enabled.remove(cycleId);
                    }
                }
            }
        }
    }
    
    /**
     * Detects if there's a cycle starting from given node using ordered path tracking.
     */
    private boolean hasCycleOrdered(
            String node, 
            Map<String, List<String>> graph, 
            Set<String> visited, 
            Deque<String> path) {
        
        if (path.contains(node)) {
            return true; // Cycle detected
        }
        
        if (visited.contains(node)) {
            return false; // Already processed, no cycle from this path
        }
        
        visited.add(node);
        path.addLast(node);
        
        List<String> dependencies = graph.getOrDefault(node, Collections.emptyList());
        for (String dep : dependencies) {
            if (hasCycleOrdered(dep, graph, visited, path)) {
                return true; // Keep cycle in path
            }
        }
        
        path.removeLast(); // Remove current node when backtracking
        return false;
    }
}
