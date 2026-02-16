package com.tapestry.extensions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates capability declarations and resolves capability dependencies.
 * 
 * Handles capability masking, duplicate detection, and dependency cycle validation.
 */
public class CapabilityValidator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CapabilityValidator.class);
    
    private final Path configDir;
    
    public CapabilityValidator(Path configDir) {
        this.configDir = configDir;
    }
    
    /**
     * Validates and resolves capabilities for all extensions.
     * 
     * @param extensions map of extension ID to descriptor
     * @return capability validation result
     */
    public CapabilityValidationResult validateCapabilities(Map<String, TapestryExtensionDescriptor> extensions) {
        LOGGER.info("Starting capability validation for {} extensions", extensions.size());
        
        Map<String, CapabilityMask> masks = loadCapabilityMasks(extensions.keySet());
        Map<String, String> capabilityProviders = new HashMap<>();
        Map<String, List<String>> capabilityGraph = new HashMap<>();
        List<ValidationMessage> errors = new ArrayList<>();
        List<ValidationMessage> warnings = new ArrayList<>();
        
        try {
            // Step 1: Validate JS declarations match descriptor declarations (Option A)
            validateJsDeclarationsMatchDescriptors(extensions, errors);
            
            // Step 2: Apply masks to extension capabilities
            Map<String, List<CapabilityDecl>> maskedCapabilities = applyCapabilityMasks(extensions, masks, errors);
            
            // Step 3: Validate unique providers
            validateUniqueProviders(maskedCapabilities, capabilityProviders, errors);
            
            // Step 4: Validate required capabilities exist
            validateRequiredCapabilities(extensions, capabilityProviders, errors);
            
            // Step 5: Build dependency graph and check for cycles
            buildDependencyGraph(extensions, capabilityProviders, capabilityGraph);
            validateDependencyCycles(capabilityGraph, errors);
            
            // Step 6: Final invariant check
            assertNoExtensionDependsOnRejectedCapability(extensions, capabilityProviders, errors);
            
            if (errors.isEmpty()) {
                LOGGER.info("Capability validation successful: {} capabilities resolved", capabilityProviders.size());
                return new CapabilityValidationResult(capabilityProviders, capabilityGraph, Collections.emptyList(), warnings);
            } else {
                LOGGER.error("Capability validation failed with {} errors", errors.size());
                return new CapabilityValidationResult(Collections.emptyMap(), Collections.emptyMap(), errors, warnings);
            }
            
        } catch (Exception e) {
            LOGGER.error("Critical error during capability validation", e);
            errors.add(new ValidationMessage(
                Severity.ERROR, "VALIDATION_ERROR", 
                "Critical error during capability validation: " + e.getMessage(), 
                "system"));
            return new CapabilityValidationResult(Collections.emptyMap(), Collections.emptyMap(), errors, warnings);
        }
    }
    
    /**
     * Validates that JS declarations match descriptor declarations (Option A).
     */
    private void validateJsDeclarationsMatchDescriptors(
            Map<String, TapestryExtensionDescriptor> extensions,
            List<ValidationMessage> errors) {
        
        for (var entry : extensions.entrySet()) {
            String modId = entry.getKey();
            var descriptor = entry.getValue();
            
            // Get JS-provided capabilities for this mod
            Map<String, Object> jsProvided = com.tapestry.typescript.CapabilityApi.getProvidedCapabilitiesForMod(modId);
            Map<String, String> jsRequired = com.tapestry.typescript.CapabilityApi.getRequiredCapabilitiesForMod(modId);
            
            // Validate provided capabilities match descriptor
            Set<String> descriptorProvided = new HashSet<>();
            if (descriptor.capabilities() != null) {
                descriptorProvided.addAll(descriptor.capabilities().stream()
                    .map(CapabilityDecl::name)
                    .collect(Collectors.toSet()));
            }
            
            // Check for JS-provided capabilities not in descriptor
            for (String jsCap : jsProvided.keySet()) {
                if (!descriptorProvided.contains(jsCap)) {
                    errors.add(new ValidationMessage(
                        Severity.ERROR, "UNDECLARED_CAPABILITY",
                        "Mod '" + modId + "' provides capability '" + jsCap + "' not declared in descriptor",
                        modId));
                }
            }
            
            // Check for descriptor-declared capabilities not provided by JS
            for (String descCap : descriptorProvided) {
                if (!jsProvided.containsKey(descCap)) {
                    errors.add(new ValidationMessage(
                        Severity.ERROR, "MISSING_CAPABILITY_IMPLEMENTATION",
                        "Mod '" + modId + "' declares capability '" + descCap + "' but does not provide implementation",
                        modId));
                }
            }
            
            // Validate required capabilities match descriptor
            Set<String> descriptorRequired = new HashSet<>();
            if (descriptor.requiresCapabilities() != null) {
                descriptorRequired.addAll(descriptor.requiresCapabilities());
            }
            
            // Check for JS-required capabilities not in descriptor
            for (String jsReq : jsRequired.keySet()) {
                if (!descriptorRequired.contains(jsReq)) {
                    errors.add(new ValidationMessage(
                        Severity.ERROR, "UNDECLARED_REQUIREMENT",
                        "Mod '" + modId + "' requires capability '" + jsReq + "' not declared in descriptor",
                        modId));
                }
            }
            
            // Check for descriptor-required capabilities not required by JS
            for (String descReq : descriptorRequired) {
                if (!jsRequired.containsKey(descReq)) {
                    errors.add(new ValidationMessage(
                        Severity.ERROR, "MISSING_REQUIREMENT_DECLARATION",
                        "Mod '" + modId + "' declares requirement '" + descReq + "' but does not call requireCapability()",
                        modId));
                }
            }
        }
    }
    
    /**
     * Loads capability mask files for all mods.
     */
    private Map<String, CapabilityMask> loadCapabilityMasks(Set<String> modIds) {
        Map<String, CapabilityMask> masks = new HashMap<>();
        Path capabilitiesDir = configDir.resolve("tapestry/capabilities");
        
        if (!Files.exists(capabilitiesDir)) {
            LOGGER.debug("Capabilities directory not found: {}", capabilitiesDir);
            return masks;
        }
        
        ObjectMapper objectMapper = new ObjectMapper();
        
        for (String modId : modIds) {
            Path maskFile = capabilitiesDir.resolve(modId + ".json");
            if (Files.exists(maskFile)) {
                try {
                    String content = Files.readString(maskFile);
                    JsonNode jsonNode = objectMapper.readTree(content);
                    
                    // Parse disable array
                    List<String> disableList = new ArrayList<>();
                    JsonNode disableNode = jsonNode.get("disable");
                    if (disableNode != null && disableNode.isArray()) {
                        for (JsonNode item : disableNode) {
                            if (item.isTextual()) {
                                disableList.add(item.asText());
                            }
                        }
                    }
                    
                    // Parse meta (optional)
                    Map<String, Object> meta = new HashMap<>();
                    JsonNode metaNode = jsonNode.get("meta");
                    if (metaNode != null && metaNode.isObject()) {
                        Iterator<Map.Entry<String, JsonNode>> fields = metaNode.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> field = fields.next();
                            meta.put(field.getKey(), field.getValue().asText());
                        }
                    }
                    
                    masks.put(modId, new CapabilityMask(disableList, meta));
                    LOGGER.debug("Loaded capability mask for mod '{}': {} disabled capabilities", modId, disableList.size());
                    
                } catch (IOException e) {
                    LOGGER.error("Failed to parse capability mask for mod '{}': {}", modId, e.getMessage());
                    // For parsing errors, we'll create an empty mask but log the error
                    masks.put(modId, new CapabilityMask(Collections.emptyList(), Collections.emptyMap()));
                } catch (Exception e) {
                    LOGGER.error("Unexpected error loading capability mask for mod '{}': {}", modId, e.getMessage());
                    masks.put(modId, new CapabilityMask(Collections.emptyList(), Collections.emptyMap()));
                }
            }
        }
        
        return masks;
    }
    
    /**
     * Applies capability masks to extension capability declarations.
     */
    private Map<String, List<CapabilityDecl>> applyCapabilityMasks(
            Map<String, TapestryExtensionDescriptor> extensions,
            Map<String, CapabilityMask> masks,
            List<ValidationMessage> errors) {
        
        Map<String, List<CapabilityDecl>> maskedCapabilities = new HashMap<>();
        
        for (var entry : extensions.entrySet()) {
            String modId = entry.getKey();
            var descriptor = entry.getValue();
            var mask = masks.get(modId);
            
            List<CapabilityDecl> capabilities = new ArrayList<>();
            if (descriptor.capabilities() != null) {
                capabilities.addAll(descriptor.capabilities());
            }
            
            if (mask != null) {
                // Apply mask - remove disabled capabilities
                capabilities.removeIf(cap -> mask.disable().contains(cap.name()));
                
                // Validate mask entries
                for (String disabledCap : mask.disable()) {
                    boolean wasDeclared = descriptor.capabilities() != null && 
                        descriptor.capabilities().stream().anyMatch(c -> c.name().equals(disabledCap));
                    
                    if (!wasDeclared) {
                        errors.add(new ValidationMessage(
                            Severity.ERROR, "INVALID_MASK_ENTRY", 
                            "Mask disables capability '" + disabledCap + "' not declared by mod '" + modId + "'", 
                            modId));
                    }
                }
            }
            
            maskedCapabilities.put(modId, capabilities);
        }
        
        return maskedCapabilities;
    }
    
    /**
     * Validates that each capability has exactly one provider.
     */
    private void validateUniqueProviders(
            Map<String, List<CapabilityDecl>> maskedCapabilities,
            Map<String, String> capabilityProviders,
            List<ValidationMessage> errors) {
        
        Map<String, List<String>> providerMap = new HashMap<>();
        
        for (var entry : maskedCapabilities.entrySet()) {
            String modId = entry.getKey();
            for (var capability : entry.getValue()) {
                providerMap.computeIfAbsent(capability.name(), k -> new ArrayList<>()).add(modId);
            }
        }
        
        // Check for duplicates
        for (var entry : providerMap.entrySet()) {
            String capabilityName = entry.getKey();
            List<String> providers = entry.getValue();
            
            if (providers.size() > 1) {
                // Duplicate providers found
                for (String providerMod : providers) {
                    errors.add(new ValidationMessage(
                        Severity.ERROR, "DUPLICATE_CAPABILITY_PROVIDER", 
                        "Capability '" + capabilityName + "' is provided by multiple mods: " + String.join(", ", providers), 
                        providerMod));
                }
            } else if (providers.size() == 1) {
                // Unique provider - record it
                capabilityProviders.put(capabilityName, providers.get(0));
            }
        }
    }
    
    /**
     * Validates that all required capabilities have providers.
     */
    private void validateRequiredCapabilities(
            Map<String, TapestryExtensionDescriptor> extensions,
            Map<String, String> capabilityProviders,
            List<ValidationMessage> errors) {
        
        for (var entry : extensions.entrySet()) {
            String modId = entry.getKey();
            var descriptor = entry.getValue();
            
            if (descriptor.requiresCapabilities() != null) {
                for (String requiredCap : descriptor.requiresCapabilities()) {
                    if (!capabilityProviders.containsKey(requiredCap)) {
                        errors.add(new ValidationMessage(
                            Severity.ERROR, "MISSING_REQUIRED_CAPABILITY", 
                            "Mod '" + modId + "' requires capability '" + requiredCap + "' which is not provided by any mod", 
                            modId));
                    }
                }
            }
        }
    }
    
    /**
     * Builds capability dependency graph for cycle detection.
     */
    private void buildDependencyGraph(
            Map<String, TapestryExtensionDescriptor> extensions,
            Map<String, String> capabilityProviders,
            Map<String, List<String>> capabilityGraph) {
        
        for (var entry : extensions.entrySet()) {
            String modId = entry.getKey();
            var descriptor = entry.getValue();
            
            List<String> dependencies = new ArrayList<>();
            if (descriptor.requiresCapabilities() != null) {
                for (String requiredCap : descriptor.requiresCapabilities()) {
                    String provider = capabilityProviders.get(requiredCap);
                    if (provider != null) {
                        dependencies.add(provider);
                    }
                }
            }
            
            // Always include the mod in the graph, even if it has no dependencies
            capabilityGraph.put(modId, dependencies);
        }
    }
    
    /**
     * Validates that capability dependency graph has no cycles.
     */
    private void validateDependencyCycles(
            Map<String, List<String>> capabilityGraph,
            List<ValidationMessage> errors) {
        
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        Deque<String> path = new ArrayDeque<>();
        
        for (String modId : capabilityGraph.keySet()) {
            if (hasCapabilityCycle(modId, capabilityGraph, visited, recursionStack, path)) {
                // Extract cycle path
                List<String> cycle = new ArrayList<>();
                boolean foundStart = false;
                
                for (String pathElement : path) {
                    if (foundStart) {
                        cycle.add(pathElement);
                    }
                    if (pathElement.equals(modId)) {
                        foundStart = true;
                    }
                }
                
                if (!cycle.isEmpty()) {
                    cycle.add(modId);
                }
                
                String cyclePath = String.join(" -> ", cycle);
                for (String cycleMod : cycle) {
                    errors.add(new ValidationMessage(
                        Severity.ERROR, "CAPABILITY_DEPENDENCY_CYCLE", 
                        "Capability dependency cycle detected: " + cyclePath, 
                        cycleMod));
                }
            }
        }
    }
    
    /**
     * Detects cycles in capability dependency graph using proper DFS with recursion stack.
     */
    private boolean hasCapabilityCycle(
            String node, 
            Map<String, List<String>> graph, 
            Set<String> visited, 
            Set<String> recursionStack,
            Deque<String> path) {
        
        if (recursionStack.contains(node)) {
            return true; // Cycle detected
        }
        
        if (visited.contains(node)) {
            return false; // Already processed, no cycle from this path
        }
        
        visited.add(node);
        recursionStack.add(node);
        path.addLast(node);
        
        List<String> dependencies = graph.getOrDefault(node, Collections.emptyList());
        for (String dep : dependencies) {
            if (hasCapabilityCycle(dep, graph, visited, recursionStack, path)) {
                return true; // Keep cycle in path
            }
        }
        
        recursionStack.remove(node);
        path.removeLast(); // Remove current node when backtracking
        return false;
    }
    
    /**
     * Final invariant check: no extension depends on a rejected capability.
     */
    private void assertNoExtensionDependsOnRejectedCapability(
            Map<String, TapestryExtensionDescriptor> extensions,
            Map<String, String> capabilityProviders,
            List<ValidationMessage> errors) {
        
        // This is a placeholder - in practice, this would be integrated
        // with the main ExtensionValidator's invariant check
        
        for (var entry : extensions.entrySet()) {
            String modId = entry.getKey();
            var descriptor = entry.getValue();
            
            if (descriptor.requiresCapabilities() != null) {
                for (String requiredCap : descriptor.requiresCapabilities()) {
                    if (!capabilityProviders.containsKey(requiredCap)) {
                        // This should have been caught earlier, but we double-check
                        errors.add(new ValidationMessage(
                            Severity.ERROR, "INVARIANT_VIOLATION", 
                            "Extension '" + modId + "' depends on unresolved capability '" + requiredCap + "'", 
                            modId));
                    }
                }
            }
        }
    }
}
