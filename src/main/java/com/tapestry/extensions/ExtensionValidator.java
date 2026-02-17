package com.tapestry.extensions;

import com.tapestry.extensions.types.ExtensionTypeRegistry;
import com.tapestry.extensions.types.TypeValidator;
import com.tapestry.extensions.types.TypeValidationError;
import com.tapestry.extensions.types.ExtensionTypeRegistry.TypeModule;
import com.tapestry.extensions.types.TypeValidator.TypeValidationResult;
import com.tapestry.extensions.types.TapestryTypeResolver.TapestryTypeResolutionException;
import com.tapestry.extensions.types.GraalVMTypeIntegration;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final ExtensionTypeRegistry typeRegistry;
    private final TypeValidator typeValidator;
    
    public ExtensionValidator(Version currentTapestryVersion, ValidationPolicy policy) {
        this.currentTapestryVersion = currentTapestryVersion;
        this.policy = policy;
        this.typeRegistry = new ExtensionTypeRegistry();
        this.typeValidator = new TypeValidator();
    }
    
    /**
     * Gets the type registry for use by other components.
     */
    public ExtensionTypeRegistry getTypeRegistry() {
        return typeRegistry;
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
            
            // Step 2: Validate descriptors
            validateDescriptors(providersById, enabled, rejected, warnings);
            
            // Step3: Resolve dependencies
            resolveDependencies(enabled, rejected);
            
            // Step 4: Check dependency cycles
            validateDependencyCycles(enabled, rejected);
            
            // Step 5: Handle policy
            handlePolicy(enabled, rejected, warnings);
            
            // Step 6: Post-validation invariant check
            assertNoEnabledDependsOnRejected(enabled, rejected);
            
            // Step 7: Capability validation (Phase 13)
            validateCapabilities(enabled, rejected, warnings);
            
            // Clear temporary capability storage after validation
            com.tapestry.typescript.CapabilityApi.clearTemporaryStorage();
            
            // Step 8: Phase 14 type validation
            validateTypes(providers, enabled, rejected, warnings);
            
            // Step 9: TYPE_INIT sub-step - freeze type registry
            typeRegistry.freeze();
            
            return new ExtensionValidationResult(enabled, rejected, warnings);
            
        } catch (Exception e) {
            LOGGER.error("Critical error during extension validation", e);
            throw new RuntimeException("Extension validation failed", e);
        }
    }
    
    /**
     * Groups providers by extension ID for duplicate detection.
     */
    private Map<String, List<DiscoveredExtensionProvider>> groupProvidersById(List<DiscoveredExtensionProvider> providers) {
        Map<String, List<DiscoveredExtensionProvider>> grouped = new HashMap<>();
        for (var provider : providers) {
            String id = provider.descriptor().id();
            grouped.computeIfAbsent(id, k -> new ArrayList<>()).add(provider);
        }
        return grouped;
    }
    
    /**
     * Validates extension descriptors.
     */
    private void validateDescriptors(
            Map<String, List<DiscoveredExtensionProvider>> providersById,
            TreeMap<String, ValidatedExtension> enabled,
            ArrayList<RejectedExtension> rejected,
            ArrayList<ValidationMessage> warnings) {
        
        for (var entry : providersById.entrySet()) {
            String id = entry.getKey();
            List<DiscoveredExtensionProvider> providers = entry.getValue();
            
            if (providers.size() > 1) {
                // Duplicate extension ID
                for (var provider : providers) {
                    rejected.add(new RejectedExtension(
                        provider.descriptor(),
                        provider.sourceMod(),
                        List.of(new ValidationMessage(
                            Severity.ERROR, "DUPLICATE_EXTENSION_ID",
                            "Extension ID '" + id + "' is provided by multiple mods",
                            id)))
                    );
                }
                continue;
            }
            
            var provider = providers.get(0);
            var descriptor = provider.descriptor();
            var sourceMod = provider.sourceMod();
            
            List<ValidationMessage> errors = validateDescriptor(descriptor);
            if (!errors.isEmpty()) {
                rejected.add(new RejectedExtension(descriptor, sourceMod, errors));
            } else {
                enabled.put(id, new ValidatedExtension(descriptor, provider.provider(), sourceMod, new ArrayList<>(), new ArrayList<>()));
            }
        }
    }
    
    /**
     * Validates a single extension descriptor.
     */
    private List<ValidationMessage> validateDescriptor(TapestryExtensionDescriptor descriptor) {
        List<ValidationMessage> errors = new ArrayList<>();
        String id = descriptor.id();
        
        // Validate extension ID format
        if (!EXTENSION_ID_PATTERN.matcher(id).matches()) {
            errors.add(new ValidationMessage(
                Severity.ERROR, "INVALID_EXTENSION_ID",
                "Extension ID must match pattern: " + EXTENSION_ID_PATTERN.pattern(),
                id));
        }
        
        // Validate version
        try {
            Version.parse(descriptor.version());
        } catch (Exception e) {
            errors.add(new ValidationMessage(
                Severity.ERROR, "INVALID_VERSION",
                "Invalid version format: " + descriptor.version(),
                id));
        }
        
        // Validate Tapestry version requirement
        try {
            if (descriptor.minTapestry() != null) {
                Version minVersion = Version.parse(descriptor.minTapestry());
                if (currentTapestryVersion.compareTo(minVersion) < 0) {
                    errors.add(new ValidationMessage(
                        Severity.ERROR, "INCOMPATIBLE_TAPESTRY_VERSION",
                        "Requires Tapestry " + minVersion + " but current is " + currentTapestryVersion,
                        id));
                }
            }
        } catch (Exception e) {
            errors.add(new ValidationMessage(
                Severity.ERROR, "INVALID_MIN_TAPESTRY_VERSION",
                "Invalid minTapestry version: " + descriptor.minTapestry(),
                id));
        }
        
        // Validate dependencies
        if (descriptor.requires() != null) {
            for (String dep : descriptor.requires()) {
                if (!EXTENSION_ID_PATTERN.matcher(dep).matches()) {
                    errors.add(new ValidationMessage(
                        Severity.ERROR, "INVALID_DEPENDENCY_ID",
                        "Invalid dependency ID: " + dep,
                        id));
                }
            }
        }
        
        return errors;
    }
    
    /**
     * Resolves extension dependencies.
     */
    private void resolveDependencies(
            TreeMap<String, ValidatedExtension> enabled,
            List<RejectedExtension> rejected) {
        
        for (var entry : enabled.entrySet()) {
            String id = entry.getKey();
            var extension = entry.getValue();
            List<String> resolved = new ArrayList<>();
            
            if (extension.descriptor().requires() != null) {
                for (String dep : extension.descriptor().requires()) {
                    if (!enabled.containsKey(dep)) {
                        rejected.add(new RejectedExtension(
                            extension.descriptor(),
                            extension.sourceMod(),
                            List.of(new ValidationMessage(
                                Severity.ERROR, "MISSING_DEPENDENCY",
                                "Missing dependency: " + dep,
                                id))));
                        enabled.remove(id);
                        break;
                    }
                    resolved.add(dep);
                }
            }
            
            if (enabled.containsKey(id)) {
                extension = new ValidatedExtension(
                    extension.descriptor(),
                    extension.provider(),
                    extension.sourceMod(),
                    extension.capabilitiesResolved(),
                    resolved);
                enabled.put(id, extension);
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
    
    /**
     * Post-validation invariant check: No enabled extension may depend on a rejected extension.
     */
    private void assertNoEnabledDependsOnRejected(
            Map<String, ValidatedExtension> enabled,
            List<RejectedExtension> rejected) {
        
        Set<String> rejectedIds = rejected.stream()
            .map(r -> r.descriptor().id())
            .collect(Collectors.toSet());

        for (var ext : enabled.values()) {
            for (String dep : ext.descriptor().requires()) {
                if (rejectedIds.contains(dep)) {
                    throw new IllegalStateException(
                        "Invariant violation: Enabled extension '" +
                        ext.descriptor().id() +
                        "' depends on rejected extension '" +
                        dep + "'"
                    );
                }
            }
        }
    }
    
    /**
     * Validates capabilities for Phase 13.
     * 
     * @param enabled map of enabled extensions
     * @param rejected list of rejected extensions
     * @param warnings list to collect warnings
     */
    private void validateCapabilities(
            TreeMap<String, ValidatedExtension> enabled,
            List<RejectedExtension> rejected,
            List<ValidationMessage> warnings) {
        
        // Build map of extension descriptors
        Map<String, TapestryExtensionDescriptor> descriptors = new HashMap<>();
        for (var validated : enabled.values()) {
            descriptors.put(validated.descriptor().id(), validated.descriptor());
        }
        
        // Create capability validator
        Path configDir = getConfigDirectory();
        var capabilityValidator = new CapabilityValidator(configDir);
        var result = capabilityValidator.validateCapabilities(descriptors);
        
        if (!result.isSuccess()) {
            // Convert capability errors to extension rejections
            for (var error : result.errors()) {
                // Find the mod responsible for this error
                String modId = extractModIdFromCapabilityError(error, enabled);
                if (modId != null) {
                    // Reject the extension responsible
                    var extension = enabled.get(modId);
                    if (extension != null) {
                        rejected.add(new RejectedExtension(
                            extension.descriptor(),
                            extension.sourceMod(),
                            List.of(new ValidationMessage(Severity.ERROR, error.code(), error.message(), modId))
                        ));
                        enabled.remove(modId);
                    }
                }
            }
        } else {
            // Success: initialize and freeze the capability registry
            Map<String, Object> implementations = collectCapabilityImplementations(enabled);
            CapabilityRegistry.initialize(result.capabilityProviders(), implementations);
            CapabilityRegistry.freeze();
        }
        
        // Add capability warnings to extension warnings
        warnings.addAll(result.warnings());
        
        // Final invariant check: no enabled extension requires a capability that is not in resolved registry
        assertNoEnabledExtensionRequiresMissingCapability(enabled, result.capabilityProviders());
    }
    
    /**
     * Final invariant check: No enabled extension requires a capability that is not in resolved registry.
     */
    private void assertNoEnabledExtensionRequiresMissingCapability(
            TreeMap<String, ValidatedExtension> enabled,
            Map<String, String> capabilityProviders) {
        
        for (var extension : enabled.values()) {
            if (extension.descriptor().requiresCapabilities() != null) {
                for (String requiredCap : extension.descriptor().requiresCapabilities()) {
                    if (!capabilityProviders.containsKey(requiredCap)) {
                        throw new IllegalStateException(
                            "Invariant violation: Enabled extension '" +
                            extension.descriptor().id() +
                            "' requires capability '" + requiredCap + "' which is not provided by any extension"
                        );
                    }
                }
            }
        }
    }
    
    /**
     * Extracts mod ID from capability validation error.
     */
    private String extractModIdFromCapabilityError(ValidationMessage error, TreeMap<String, ValidatedExtension> enabled) {
        // For now, this is a simplified implementation
        // In a full implementation, we'd parse the error message to extract the responsible mod
        switch (error.code()) {
            case "DUPLICATE_CAPABILITY_PROVIDER":
                // Parse error message to find mod ID
                String message = error.message();
                for (var entry : enabled.entrySet()) {
                    if (message.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                }
                break;
            case "MISSING_REQUIRED_CAPABILITY":
                // Parse error message to find mod ID
                String missingMessage = error.message();
                for (var entry : enabled.entrySet()) {
                    if (missingMessage.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                }
                break;
            default:
                break;
        }
        return null;
    }
    
    /**
     * Collects capability implementations from enabled extensions.
     */
    private Map<String, Object> collectCapabilityImplementations(TreeMap<String, ValidatedExtension> enabled) {
        Map<String, Object> implementations = new HashMap<>();
        
        for (var entry : enabled.entrySet()) {
            String modId = entry.getKey();
            var extension = entry.getValue();
            
            // Get implementations from CapabilityApi temporary storage
            Map<String, Object> modImplementations = com.tapestry.typescript.CapabilityApi.getProvidedCapabilitiesForMod(modId);
            if (modImplementations != null) {
                implementations.putAll(modImplementations);
            }
        }
        
        return implementations;
    }
    
    /**
     * Gets the actual config directory from the runtime environment.
     */
    private Path getConfigDirectory() {
        // Try to get config directory from Fabric Loader
        try {
            net.fabricmc.loader.api.FabricLoader loader = net.fabricmc.loader.api.FabricLoader.getInstance();
            Path configDir = loader.getConfigDir();
            if (configDir != null) {
                return configDir;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get config directory from Fabric Loader: {}", e.getMessage());
        }
        
        // Fallback to default
        return Path.of("config");
    }
    
    /**
     * Handles validation policy.
     */
    private void handlePolicy(
            TreeMap<String, ValidatedExtension> enabled,
            List<RejectedExtension> rejected,
            List<ValidationMessage> warnings) {
        
        if (policy.disableInvalid()) {
            for (var rejectedExt : rejected) {
                for (var error : rejectedExt.errors()) {
                    if (error.code().equals("MISSING_DEPENDENCY")) {
                        warnings.add(new ValidationMessage(
                            Severity.WARN, error.code(), error.message(), error.extensionId()));
                    }
                }
            }
        }
    }
    
    /**
     * Phase 14: Validates type-related aspects of extensions.
     * 
     * @param providers List of discovered extension providers
     * @param enabled Map of enabled extensions
     * @param rejected List of rejected extensions (will be modified)
     * @param warnings List of warnings (will be modified)
     */
    private void validateTypes(
            List<DiscoveredExtensionProvider> providers,
            TreeMap<String, ValidatedExtension> enabled,
            List<RejectedExtension> rejected,
            List<ValidationMessage> warnings) {
        
        LOGGER.info("Starting Phase 14 type validation");
        
        // Step 1: Load type files into registry during DISCOVERY phase
        loadTypeFiles(providers);
        
        // Step 2: Build map of all descriptors for cross-validation
        Map<String, TapestryExtensionDescriptor> allDescriptors = new HashMap<>();
        for (var provider : providers) {
            allDescriptors.put(provider.descriptor().id(), provider.descriptor());
        }
        
        // Step 3: Validate individual descriptors
        for (var provider : providers) {
            Path extensionRoot = getExtensionRoot(provider.sourceMod());
            var typeErrors = typeValidator.validateDescriptor(
                provider.descriptor(),
                extensionRoot,
                allDescriptors
            );
            
            // Convert type validation errors to extension rejections
            if (!typeErrors.isEmpty()) {
                var extension = enabled.get(provider.descriptor().id());
                if (extension != null) {
                    List<ValidationMessage> validationErrors = typeErrors.stream()
                        .map(te -> new ValidationMessage(
                            Severity.ERROR,
                            te.error().getCode(),
                            te.message(),
                            te.extensionId()
                        ))
                        .toList();
                    
                    rejected.add(new RejectedExtension(
                        provider.descriptor(),
                        provider.sourceMod(),
                        validationErrors
                    ));
                    enabled.remove(provider.descriptor().id());
                }
            }
        }
        
        // Step 4: Validate cross-extension relationships
        var crossValidationErrors = typeValidator.validateCrossExtensionRelations(allDescriptors);
        for (var error : crossValidationErrors) {
            var extension = enabled.get(error.extensionId());
            if (extension != null) {
                List<ValidationMessage> validationErrors = List.of(new ValidationMessage(
                    Severity.ERROR,
                    error.error().getCode(),
                    error.message(),
                    error.extensionId()
                ));
                
                rejected.add(new RejectedExtension(
                    extension.descriptor(),
                    extension.sourceMod(),
                    validationErrors
                ));
                enabled.remove(error.extensionId());
            }
        }
        
        // Step 5: Update dependency graph to include type import edges for cycle detection
        updateDependencyGraphWithTypeImports(enabled);
        
        LOGGER.info("Phase 14 type validation completed. Enabled: {}, Rejected: {}", 
            enabled.size(), rejected.size());
    }
    
    /**
     * Loads type files into the registry during DISCOVERY phase.
     */
    private void loadTypeFiles(List<DiscoveredExtensionProvider> providers) {
        for (var provider : providers) {
            var descriptor = provider.descriptor();
            
            if (descriptor.typeExportEntry().isPresent()) {
                Path extensionRoot = getExtensionRoot(provider.sourceMod());
                Path typeFile = extensionRoot.resolve(descriptor.typeExportEntry().get());
                
                try {
                    String dtsSource = Files.readString(typeFile);
                    typeRegistry.storeTypeModule(descriptor.id(), dtsSource);
                    LOGGER.debug("Loaded type file for extension: {}", descriptor.id());
                } catch (IOException e) {
                    LOGGER.warn("Failed to load type file for extension {}: {}", 
                        descriptor.id(), e.getMessage());
                    // Validation will catch this as TYPE_EXPORT_FILE_NOT_FOUND
                }
            }
        }
    }
    
    /**
     * Updates dependency graph to include type import edges for unified cycle detection.
     * Note: Type imports are validated to be a subset of requiredDependencies,
     * so they're already included in the dependency graph for cycle detection.
     * This method exists for clarity and future extensibility.
     */
    private void updateDependencyGraphWithTypeImports(TreeMap<String, ValidatedExtension> enabled) {
        for (var extension : enabled.values()) {
            List<String> typeImports = extension.descriptor().typeImports();
            if (!typeImports.isEmpty()) {
                LOGGER.debug("Extension {} has type imports: {}", 
                    extension.descriptor().id(), typeImports);
                
                // Verify that all type imports are in required dependencies
                // This should always be true due to prior validation
                for (String typeImport : typeImports) {
                    if (!extension.descriptor().requires().contains(typeImport)) {
                        // This should never happen due to validation, but log for safety
                        LOGGER.error("Invariant violation: type import '{}' not in required dependencies for extension '{}'",
                            typeImport, extension.descriptor().id());
                    }
                }
            }
        }
        
        // The existing cycle detection in validateDependencyCycles() already uses
        // resolvedDependencies(), which includes all required dependencies.
        // Since typeImports ⊆ requiredDependencies, the unified DAG is correct.
    }
    
    /**
     * Gets the extension root directory from a mod container.
     */
    private Path getExtensionRoot(ModContainer sourceMod) {
        try {
            // Try to get the mod's root directory
            Path modPath = sourceMod.getRootPath();
            if (modPath != null) {
                return modPath;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get root path for mod {}: {}", 
                sourceMod.getMetadata().getId(), e.getMessage());
        }
        
        // Fallback to current directory
        return Path.of(".");
    }
}
