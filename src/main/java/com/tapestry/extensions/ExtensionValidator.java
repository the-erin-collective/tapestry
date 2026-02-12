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
        TreeMap<String, RejectedExtension> rejected = new TreeMap<>();
        ArrayList<ValidationMessage> warnings = new ArrayList<>();
        
        try {
            // Extract descriptors for validation
            var descriptors = ExtensionDiscovery.extractDescriptors(providers);
            
            // Sort capabilities within each descriptor for deterministic validation
            for (var provider : providers) {
                var descriptor = provider.descriptor();
                var sortedCapabilities = descriptor.capabilities().stream()
                    .sorted(Comparator.comparing(CapabilityDecl::name))
                    .collect(Collectors.toList());
                
                // Create new descriptor with sorted capabilities
                var sortedDescriptor = new TapestryExtensionDescriptor(
                    descriptor.id(),
                    descriptor.displayName(),
                    descriptor.version(),
                    descriptor.minTapestry(),
                    sortedCapabilities,
                    descriptor.requires(),
                    descriptor.optional()
                );
                
                var sortedProvider = new DiscoveredExtensionProvider(
                    provider.provider(),
                    provider.sourceMod(),
                    sortedDescriptor
                );
                
                // Validate individual descriptor
                var result = validateSingleDescriptor(sortedProvider, descriptors);
                
                if (result.isValid()) {
                    enabled.put(sortedDescriptor.id(), result.validated());
                } else {
                    rejected.put(sortedDescriptor.id(), result.rejected());
                }
                
                warnings.addAll(result.warnings());
            }
            
            // Global validation (conflicts, cycles)
            performGlobalValidation(enabled, rejected, warnings);
            
            // Handle failFast policy
            if (policy.failFast() && rejected.values().stream()
                    .anyMatch(r -> r.errors().stream()
                            .anyMatch(e -> e.severity() == Severity.ERROR))) {
                // Find first error and throw to abort
                var firstError = rejected.values().stream()
                    .flatMap(r -> r.errors().stream())
                    .filter(e -> e.severity() == Severity.ERROR)
                    .findFirst()
                    .orElseThrow();
                
                throw new RuntimeException("Validation failed fast: " + firstError.message());
            }
            
            LOGGER.info("Extension validation complete: {} enabled, {} rejected, {} warnings",
                enabled.size(), rejected.size(), warnings.size());
            
            return new ExtensionValidationResult(enabled, rejected, warnings);
            
        } finally {
            // No explicit cleanup needed - phase enforcement is enough
        }
    }
    
    /**
     * Validates a single extension descriptor.
     */
    private ValidationResult validateSingleDescriptor(
            DiscoveredExtensionProvider provider,
            Map<String, TapestryExtensionDescriptor> allDescriptors) {
        
        var descriptor = provider.descriptor();
        ArrayList<ValidationMessage> errors = new ArrayList<>();
        ArrayList<ValidationMessage> warnings = new ArrayList<>();
        String modId = provider.sourceMod().getMetadata().getId();
        
        // Rule Group A: Descriptor shape
        validateDescriptorShape(descriptor, errors, modId);
        
        // Rule Group C: Tapestry version compatibility
        validateVersionCompatibility(descriptor, errors, modId);
        
        // Rule Group D: Capability well-formedness
        validateCapabilityWellFormedness(descriptor, errors, modId);
        
        if (errors.isEmpty()) {
            // Only resolve dependencies if descriptor is otherwise valid
            var resolvedDeps = resolveDependencies(descriptor, allDescriptors, warnings, modId);
            
            var validated = new ValidatedExtension(
                descriptor,
                provider.sourceMod(),
                descriptor.capabilities(),
                resolvedDeps
            );
            
            return new ValidationResult(validated, null, warnings);
        } else {
            var rejected = new RejectedExtension(descriptor, provider.sourceMod(), errors);
            return new ValidationResult(null, rejected, warnings);
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
        
        for (var capability : descriptor.capabilities()) {
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
                } else {
                    // This will be handled by global validation
                    warnings.add(new ValidationMessage(
                        Severity.WARN, "MISSING_REQUIRED_DEPENDENCY", 
                        "Required dependency '" + dep + "' not found", descriptor.id()));
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
            TreeMap<String, RejectedExtension> rejected,
            List<ValidationMessage> warnings) {
        
        // Rule Group B: Unique extension IDs
        validateUniqueExtensionIds(enabled, rejected);
        
        // Rule Group E: Capability conflicts
        validateCapabilityConflicts(enabled, rejected);
        
        // Rule Group F: Dependency cycles
        validateDependencyCycles(enabled, rejected);
    }
    
    /**
     * Rule Group B: Check for duplicate extension IDs.
     */
    private void validateUniqueExtensionIds(
            TreeMap<String, ValidatedExtension> enabled,
            TreeMap<String, RejectedExtension> rejected) {
        
        Map<String, List<DiscoveredExtensionProvider>> idToProviders = new HashMap<>();
        
        // Collect all providers by ID
        for (var validated : enabled.values()) {
            idToProviders.computeIfAbsent(validated.descriptor().id(), k -> new ArrayList<>())
                .add(new DiscoveredExtensionProvider(null, validated.sourceMod(), validated.descriptor()));
        }
        
        for (var rejectedExt : rejected.values()) {
            idToProviders.computeIfAbsent(rejectedExt.descriptor().id(), k -> new ArrayList<>())
                .add(new DiscoveredExtensionProvider(null, rejectedExt.sourceMod(), rejectedExt.descriptor()));
        }
        
        // Find duplicates
        for (var entry : idToProviders.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Default policy: reject ALL duplicates
                for (var provider : entry.getValue()) {
                    var existingRejected = rejected.get(provider.descriptor().id());
                    if (existingRejected != null) {
                        // Already rejected, add error
                        var errors = new ArrayList<>(existingRejected.errors());
                        errors.add(new ValidationMessage(
                            Severity.ERROR, "DUPLICATE_ID", 
                            "Extension ID '" + entry.getKey() + "' is duplicated", provider.descriptor().id()));
                        
                        rejected.put(provider.descriptor().id(), 
                            new RejectedExtension(provider.descriptor(), provider.sourceMod(), errors));
                    } else {
                        // Was enabled, now reject
                        var errors = List.of(new ValidationMessage(
                            Severity.ERROR, "DUPLICATE_ID", 
                            "Extension ID '" + entry.getKey() + "' is duplicated", provider.descriptor().id()));
                        
                        rejected.put(provider.descriptor().id(), 
                            new RejectedExtension(provider.descriptor(), provider.sourceMod(), errors));
                        
                        // Remove from enabled
                        enabled.remove(provider.descriptor().id());
                    }
                }
            }
        }
    }
    
    /**
     * Rule Group E: Check for capability conflicts.
     */
    private void validateCapabilityConflicts(
            TreeMap<String, ValidatedExtension> enabled,
            TreeMap<String, RejectedExtension> rejected) {
        
        Map<String, List<CapabilityDecl>> capabilityToProviders = new HashMap<>();
        
        // Collect all capabilities by name
        for (var validated : enabled.values()) {
            for (var capability : validated.capabilitiesResolved()) {
                // Apply default exclusivity if flag is omitted
                boolean isExclusive = capability.exclusive() || 
                    (capability.type() != CapabilityType.HOOK);
                
                if (isExclusive) {
                    capabilityToProviders.computeIfAbsent(capability.name(), k -> new ArrayList<>())
                        .add(capability);
                }
            }
        }
        
        // Find conflicts
        for (var entry : capabilityToProviders.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Conflict found - reject all conflicting extensions
                for (var capability : entry.getValue()) {
                    // Find the extension that owns this capability
                    var conflictingExtension = enabled.values().stream()
                        .filter(e -> e.capabilitiesResolved().contains(capability))
                        .findFirst()
                        .orElse(null);
                    
                    if (conflictingExtension != null) {
                        var errors = new ArrayList<>(rejected.getOrDefault(
                            conflictingExtension.descriptor().id(), 
                            new RejectedExtension(conflictingExtension.descriptor(), 
                                conflictingExtension.sourceMod(), new ArrayList<>())
                        ).errors());
                        
                        errors.add(new ValidationMessage(
                            Severity.ERROR, "CAPABILITY_CONFLICT", 
                            "Capability '" + entry.getKey() + "' is claimed by multiple extensions", 
                            conflictingExtension.descriptor().id()));
                        
                        rejected.put(conflictingExtension.descriptor().id(), 
                            new RejectedExtension(conflictingExtension.descriptor(), 
                                conflictingExtension.sourceMod(), errors));
                        
                        // Remove from enabled
                        enabled.remove(conflictingExtension.descriptor().id());
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
            TreeMap<String, RejectedExtension> rejected) {
        
        // Build dependency graph from enabled extensions
        Map<String, List<String>> graph = new HashMap<>();
        for (var validated : enabled.values()) {
            graph.put(validated.descriptor().id(), validated.resolvedDependencies());
        }
        
        // Detect cycles using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String extensionId : graph.keySet()) {
            if (hasCycle(extensionId, graph, visited, recursionStack)) {
                // Find all nodes in the cycle
                List<String> cycle = findCycle(extensionId, graph, recursionStack);
                
                // Reject all extensions in the cycle
                for (String cycleId : cycle) {
                    var cycleExtension = enabled.get(cycleId);
                    if (cycleExtension != null) {
                        var errors = List.of(new ValidationMessage(
                            Severity.ERROR, "DEPENDENCY_CYCLE", 
                            "Dependency cycle detected: " + String.join(" -> ", cycle) + " -> " + cycleId, 
                            cycleId));
                        
                        rejected.put(cycleId, 
                            new RejectedExtension(cycleExtension.descriptor(), 
                                cycleExtension.sourceMod(), errors));
                        
                        // Remove from enabled
                        enabled.remove(cycleId);
                    }
                }
            }
        }
    }
    
    /**
     * Detects if there's a cycle starting from the given node.
     */
    private boolean hasCycle(
            String node, 
            Map<String, List<String>> graph, 
            Set<String> visited, 
            Set<String> recursionStack) {
        
        if (recursionStack.contains(node)) {
            return true; // Cycle detected
        }
        
        if (visited.contains(node)) {
            return false; // Already processed, no cycle from this path
        }
        
        visited.add(node);
        recursionStack.add(node);
        
        List<String> dependencies = graph.getOrDefault(node, Collections.emptyList());
        for (String dep : dependencies) {
            if (hasCycle(dep, graph, visited, recursionStack)) {
                return true;
            }
        }
        
        recursionStack.remove(node);
        return false;
    }
    
    /**
     * Finds the actual cycle path for reporting.
     */
    private List<String> findCycle(
            String startNode, 
            Map<String, List<String>> graph, 
            Set<String> recursionStack) {
        
        List<String> cycle = new ArrayList<>();
        String current = startNode;
        
        // Find where the cycle starts
        int startIndex = -1;
        List<String> path = new ArrayList<>(recursionStack);
        path.add(current);
        
        for (int i = 0; i < path.size(); i++) {
            if (path.get(i).equals(current)) {
                startIndex = i;
                break;
            }
        }
        
        if (startIndex >= 0) {
            cycle.addAll(path.subList(startIndex, path.size()));
        }
        
        return cycle;
    }
    
    /**
     * Result of validating a single descriptor.
     */
    private record ValidationResult(
            ValidatedExtension validated,
            RejectedExtension rejected,
            List<ValidationMessage> warnings
    ) {
        boolean isValid() {
            return validated != null;
        }
    }
}
