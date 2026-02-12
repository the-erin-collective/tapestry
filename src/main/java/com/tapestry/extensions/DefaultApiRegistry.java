package com.tapestry.extensions;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete implementation of ApiRegistry with strict validation and freeze behavior.
 */
public class DefaultApiRegistry implements ApiRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultApiRegistry.class);
    
    private final PhaseController phaseController;
    private final Map<String, TapestryExtensionDescriptor> declaredCapabilities;
    private final Map<String, ProxyExecutable> registeredFunctions;
    private final Map<String, String> capabilityToApiPath;
    private final Map<String, String> apiPathToCapability;
    private volatile boolean frozen = false;
    private ProxyObject apiTree; // Built when frozen
    
    public DefaultApiRegistry(PhaseController phaseController, 
                             Map<String, TapestryExtensionDescriptor> declaredCapabilities) {
        this.phaseController = phaseController;
        this.declaredCapabilities = declaredCapabilities;
        this.registeredFunctions = new ConcurrentHashMap<>();
        this.capabilityToApiPath = new ConcurrentHashMap<>();
        this.apiPathToCapability = new ConcurrentHashMap<>();
    }
    
    @Override
    public void addFunction(String capabilityName, ProxyExecutable fn) 
            throws RegistryFrozenException, UndeclaredCapabilityException, DuplicateApiPathException {
        
        // Phase check
        if (phaseController.getCurrentPhase() != TapestryPhase.REGISTRATION) {
            throw new WrongPhaseException(
                TapestryPhase.REGISTRATION.name(), 
                phaseController.getCurrentPhase().name()
            );
        }
        
        // Freeze check
        if (frozen) {
            throw new RegistryFrozenException();
        }
        
        // Validate capability is declared
        if (!isCapabilityDeclared(capabilityName)) {
            throw new UndeclaredCapabilityException(capabilityName, "unknown");
        }
        
        // Check if capability already registered
        if (registeredFunctions.containsKey(capabilityName)) {
            throw new CapabilityAlreadyRegisteredException(capabilityName, "unknown");
        }
        
        // Get API path from declared capability
        String apiPath = getApiPathForCapability(capabilityName);
        
        // Check for duplicate API path
        if (apiPathToCapability.containsKey(apiPath)) {
            String existingCapability = apiPathToCapability.get(apiPath);
            throw new DuplicateApiPathException(apiPath, existingCapability, "current");
        }
        
        // Register the function
        registeredFunctions.put(capabilityName, fn);
        capabilityToApiPath.put(capabilityName, apiPath);
        apiPathToCapability.put(apiPath, capabilityName);
        
        LOGGER.debug("Registered API function: {} -> {}", capabilityName, apiPath);
    }
    
    @Override
    public boolean exists(String apiPath) {
        return apiPathToCapability.containsKey(apiPath);
    }
    
    @Override
    public void freeze() {
        if (frozen) {
            return; // Already frozen
        }
        
        LOGGER.info("Freezing API registry with {} registered functions", registeredFunctions.size());
        
        // Build the ProxyObject tree for runtime exposure
        this.apiTree = buildApiTree();
        
        this.frozen = true;
        
        LOGGER.info("API registry frozen successfully with {} API paths", apiPathToCapability.size());
    }
    
    /**
     * Gets the registered function for a capability.
     */
    public ProxyExecutable getFunction(String capabilityName) {
        return registeredFunctions.get(capabilityName);
    }
    
    /**
     * Gets the API tree built during freeze().
     * Returns null if not frozen.
     */
    public ProxyObject getApiTree() {
        return apiTree;
    }
    
    /**
     * Builds the ProxyObject tree from registered API functions.
     * Creates nested objects based on API path structure.
     */
    private ProxyObject buildApiTree() {
        Map<String, Object> root = new HashMap<>();
        
        for (var entry : capabilityToApiPath.entrySet()) {
            String capabilityName = entry.getKey();
            String apiPath = entry.getValue();
            ProxyExecutable function = registeredFunctions.get(capabilityName);
            
            if (function != null) {
                // Parse API path like "tapestry.mods.infinite_dimensions.worldgen.resolveBlock"
                String[] pathParts = apiPath.split("\\.");
                
                // Skip "tapestry" prefix as that's the root object
                Map<String, Object> current = root;
                for (int i = 1; i < pathParts.length - 1; i++) {
                    String part = pathParts[i];
                    current = (Map<String, Object>) current.computeIfAbsent(part, k -> new HashMap<>());
                }
                
                // Add the function at the final path
                String functionName = pathParts[pathParts.length - 1];
                current.put(functionName, function);
            }
        }
        
        // Convert all maps to ProxyObjects recursively
        return convertToProxyObject(root);
    }
    
    /**
     * Recursively converts Map structure to ProxyObject structure.
     */
    private ProxyObject convertToProxyObject(Map<String, Object> map) {
        // Convert all nested maps first
        Map<String, Object> converted = new HashMap<>();
        for (var entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                value = convertToProxyObject(nestedMap);
            }
            converted.put(entry.getKey(), value);
        }
        
        return ProxyObject.fromMap(converted);
    }
    
    /**
     * Gets the API path for a capability.
     */
    public String getApiPath(String capabilityName) {
        return capabilityToApiPath.get(capabilityName);
    }
    
    /**
     * Checks if a capability is declared by any enabled extension.
     */
    private boolean isCapabilityDeclared(String capabilityName) {
        for (var descriptor : declaredCapabilities.values()) {
            for (var capability : descriptor.capabilities()) {
                if (capability.name().equals(capabilityName) && capability.type() == CapabilityType.API) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Gets the API path for a declared capability.
     */
    private String getApiPathForCapability(String capabilityName) {
        for (var descriptor : declaredCapabilities.values()) {
            for (var capability : descriptor.capabilities()) {
                if (capability.name().equals(capabilityName) && capability.type() == CapabilityType.API) {
                    if (capability.apiPath() == null) {
                        throw new UndeclaredCapabilityException(
                            "API capability '" + capabilityName + "' missing required apiPath", 
                            descriptor.id()
                        );
                    }
                    return capability.apiPath();
                }
            }
        }
        throw new UndeclaredCapabilityException(capabilityName, "unknown");
    }
}
