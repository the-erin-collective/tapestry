package com.tapestry.extensions;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.TreeMap;

/**
 * Orchestrates extension registration with deterministic ordering.
 * Enforces strict validation and fail-fast behavior.
 */
public class ExtensionRegistrationOrchestrator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionRegistrationOrchestrator.class);
    
    private final PhaseController phaseController;
    private final ApiRegistry apiRegistry;
    private final HookRegistry hookRegistry;
    private final ServiceRegistry serviceRegistry;
    
    public ExtensionRegistrationOrchestrator(
            PhaseController phaseController,
            ApiRegistry apiRegistry,
            HookRegistry hookRegistry,
            ServiceRegistry serviceRegistry) {
        this.phaseController = phaseController;
        this.apiRegistry = apiRegistry;
        this.hookRegistry = hookRegistry;
        this.serviceRegistry = serviceRegistry;
    }
    
    /**
     * Registers all enabled extensions in deterministic order.
     * 
     * @param enabledExtensions the validated extensions from Phase 3
     * @throws ExtensionRegistrationException if registration fails
     */
    public void registerExtensions(Map<String, ValidatedExtension> enabledExtensions) 
            throws ExtensionRegistrationException {
        
        // Phase check
        if (phaseController.getCurrentPhase() != TapestryPhase.REGISTRATION) {
            throw new WrongPhaseException(
                TapestryPhase.REGISTRATION.name(), 
                phaseController.getCurrentPhase().name()
            );
        }
        
        LOGGER.info("Starting extension registration for {} extensions", enabledExtensions.size());
        
        try {
            // 1. Determine deterministic registration order
            List<String> registrationOrder = determineRegistrationOrder(enabledExtensions);
            LOGGER.debug("Registration order: {}", String.join(" -> ", registrationOrder));
            
            // 2. Register each extension in order
            Map<String, Set<String>> registeredCapabilities = new TreeMap<>();
            
            for (String extensionId : registrationOrder) {
                var validatedExtension = enabledExtensions.get(extensionId);
                registerExtension(validatedExtension, registeredCapabilities);
            }
            
            // 3. Enforce registration model (registered ⊆ declared)
            enforceRegistrationModel(enabledExtensions, registeredCapabilities);
            
            // 5. Complete phase
            phaseController.complete(TapestryPhase.REGISTRATION);
            
            LOGGER.info("Extension registration completed successfully");
            
        } catch (ExtensionRegistrationException e) {
            LOGGER.error("Extension registration failed: {}", e.getMessage());
            // Do NOT freeze registries on failure
            throw e;
        }
    }
    
    /**
     * Determines deterministic registration order: topological sort by requires, tie-break by extension ID.
     */
    private List<String> determineRegistrationOrder(Map<String, ValidatedExtension> enabledExtensions) {
        // Build dependency graph
        Map<String, List<String>> graph = new TreeMap<>();
        for (var entry : enabledExtensions.entrySet()) {
            String extensionId = entry.getKey();
            var validated = entry.getValue();
            
            List<String> deps = new ArrayList<>(validated.descriptor().requires());
            deps.sort(String::compareTo); // Sort for deterministic processing
            graph.put(extensionId, deps);
        }
        
        // Topological sort
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (String extensionId : graph.keySet()) {
            if (!visited.contains(extensionId)) {
                topologicalSort(extensionId, graph, visited, visiting, result);
            }
        }
        
        return result;
    }
    
    private void topologicalSort(String extensionId, Map<String, List<String>> graph, 
                                 Set<String> visited, Set<String> visiting, List<String> result) {
        if (visiting.contains(extensionId)) {
            throw new IllegalStateException("Dependency cycle detected during registration: " + extensionId);
        }
        
        if (visited.contains(extensionId)) {
            return;
        }
        
        visiting.add(extensionId);
        
        for (String dep : graph.getOrDefault(extensionId, Collections.emptyList())) {
            topologicalSort(dep, graph, visited, visiting, result);
        }
        
        visiting.remove(extensionId);
        visited.add(extensionId);
        result.add(extensionId);
    }
    
    /**
     * Registers a single extension with strict validation.
     */
    private void registerExtension(ValidatedExtension validatedExtension, 
                                   Map<String, Set<String>> registeredCapabilities) {
        
        String extensionId = validatedExtension.descriptor().id();
        LOGGER.debug("Registering extension: {}", extensionId);
        
        // Create context for this extension
        TapestryExtensionContext context = new ExtensionRegistrationContext(
            extensionId, 
            apiRegistry, 
            hookRegistry, 
            serviceRegistry,
            registeredCapabilities
        );
        
        // Get the extension instance
        TapestryExtension extension = getExtensionInstance(validatedExtension);
        
        try {
            // Register the extension
            extension.register(context);
            
            LOGGER.debug("Extension {} registered successfully", extensionId);
            
        } catch (ExtensionRegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new ExtensionRegistrationException(
                "Extension registration failed for " + extensionId + ": " + e.getMessage(), 
                extensionId
            );
        }
    }
    
    /**
     * Gets the extension instance from the validated extension.
     */
    private TapestryExtension getExtensionInstance(ValidatedExtension validatedExtension) {
        try {
            TapestryExtensionProvider provider = validatedExtension.provider();
            return provider.create();
        } catch (Exception e) {
            throw new ExtensionRegistrationException(
                "Failed to create extension instance for '" + 
                validatedExtension.descriptor().id() + "': " + e.getMessage(), 
                validatedExtension.descriptor().id()
            );
        }
    }
    
    /**
     * Enforces that registered capabilities are a subset of declared capabilities.
     */
    private void enforceRegistrationModel(Map<String, ValidatedExtension> enabledExtensions,
                                          Map<String, Set<String>> registeredCapabilities) {
        
        for (var entry : enabledExtensions.entrySet()) {
            String extensionId = entry.getKey();
            var validated = entry.getValue();
            
            Set<String> declared = validated.descriptor().capabilities().stream()
                .map(CapabilityDecl::name)
                .collect(Collectors.toSet());
            
            Set<String> registered = registeredCapabilities.getOrDefault(extensionId, Collections.emptySet());
            
            // Check: registered ⊆ declared
            Set<String> undeclared = new HashSet<>(registered);
            undeclared.removeAll(declared);
            
            if (!undeclared.isEmpty()) {
                throw new ExtensionRegistrationException(
                    "Extension '" + extensionId + "' registered undeclared capabilities: " + 
                    String.join(", ", undeclared)
                );
            }
        }
    }
}
