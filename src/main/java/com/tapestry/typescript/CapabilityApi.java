package com.tapestry.typescript;

import com.tapestry.extensions.CapabilityRegistry;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Phase 13 Capability API for mods.
 * 
 * Provides provideCapability and requireCapability functions during registration phase.
 */
public class CapabilityApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CapabilityApi.class);
    
    // Temporary storage for capability declarations during registration
    private static final Map<String, Object> providedCapabilities = new HashMap<>();
    private static final Map<String, String> requiredCapabilities = new HashMap<>();
    
    /**
     * Creates the capability namespace object for JavaScript mods.
     * 
     * @return ProxyObject with provideCapability and requireCapability functions
     */
    public ProxyObject createCapabilityNamespace() {
        Map<String, Object> capabilityApi = new HashMap<>();
        
        // provideCapability(name, implementation) function
        capabilityApi.put("provideCapability", (ProxyExecutable) args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException("provideCapability requires exactly 2 arguments: (name, implementation)");
            }
            
            String capabilityName = args[0].asString();
            Object implementation = args[1].asHostObject();
            
            validatePhase("provideCapability");
            validateCapabilityName(capabilityName);
            
            // Store for later resolution
            providedCapabilities.put(capabilityName, implementation);
            
            LOGGER.debug("Mod '{}' provided capability '{}'", getCurrentModId(), capabilityName);
            
            return null;
        });
        
        // requireCapability(name) function
        capabilityApi.put("requireCapability", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("requireCapability requires exactly 1 argument: (name)");
            }
            
            String capabilityName = args[0].asString();
            
            validatePhase("requireCapability");
            validateCapabilityName(capabilityName);
            
            // Store requirement for later validation
            requiredCapabilities.put(capabilityName, getCurrentModId());
            
            LOGGER.debug("Mod '{}' requires capability '{}'", getCurrentModId(), capabilityName);
            
            return null;
        });
        
        return ProxyObject.fromMap(capabilityApi);
    }
    
    /**
     * Creates the runtime capability access object for JavaScript mods.
     * 
     * @return ProxyObject with getCapability function
     */
    public ProxyObject createRuntimeCapabilityNamespace() {
        Map<String, Object> runtimeApi = new HashMap<>();
        
        // getCapability(name) function
        runtimeApi.put("getCapability", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("getCapability requires exactly 1 argument: (name)");
            }
            
            String capabilityName = args[0].asString();
            
            validateRuntimeAccess();
            
            Object capability = CapabilityRegistry.getCapability(capabilityName);
            LOGGER.debug("Mod '{}' accessed capability '{}'", getCurrentModId(), capabilityName);
            
            return capability;
        });
        
        return ProxyObject.fromMap(runtimeApi);
    }
    
    /**
     * Gets all provided capabilities for this mod.
     * 
     * @return map of capability name to implementation
     */
    public static Map<String, Object> getProvidedCapabilities() {
        return Map.copyOf(providedCapabilities);
    }
    
    /**
     * Gets all required capabilities for this mod.
     * 
     * @return map of capability name to requiring mod ID
     */
    public static Map<String, String> getRequiredCapabilities() {
        return Map.copyOf(requiredCapabilities);
    }
    
    /**
     * Clears temporary capability storage (called after validation).
     */
    public static void clearTemporaryStorage() {
        providedCapabilities.clear();
        requiredCapabilities.clear();
        LOGGER.debug("Cleared temporary capability storage");
    }
    
    /**
     * Validates that capability operations are allowed in current phase.
     */
    private void validatePhase(String operation) {
        TapestryPhase currentPhase = PhaseController.getInstance().getCurrentPhase();
        if (currentPhase != TapestryPhase.REGISTRATION) {
            throw new IllegalStateException(
                String.format("%s is only available during REGISTRATION phase. Current phase: %s", 
                             operation, currentPhase));
        }
    }
    
    /**
     * Validates capability name format.
     */
    private void validateCapabilityName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Capability name cannot be null or empty");
        }
        
        // Basic validation - could be enhanced with regex
        if (!name.matches("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$")) {
            throw new IllegalArgumentException("Invalid capability name format: " + name);
        }
    }
    
    /**
     * Validates that runtime access is allowed.
     */
    private void validateRuntimeAccess() {
        if (!CapabilityRegistry.isFrozen()) {
            throw new IllegalStateException("getCapability() only available after capability resolution completes");
        }
    }
    
    /**
     * Gets the current mod ID from execution context.
     */
    private String getCurrentModId() {
        return com.tapestry.typescript.TypeScriptRuntime.getCurrentModId();
    }
}
