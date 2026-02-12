package com.tapestry.extensions;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete implementation of HookRegistry with strict validation and freeze behavior.
 */
public class DefaultHookRegistry implements HookRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHookRegistry.class);
    
    private final PhaseController phaseController;
    private final Map<String, TapestryExtensionDescriptor> declaredCapabilities;
    private final Map<String, HookType> registeredHooks;
    private volatile boolean frozen = false;
    
    public DefaultHookRegistry(PhaseController phaseController, 
                               Map<String, TapestryExtensionDescriptor> declaredCapabilities) {
        this.phaseController = phaseController;
        this.declaredCapabilities = declaredCapabilities;
        this.registeredHooks = new ConcurrentHashMap<>();
    }
    
    @Override
    public void registerHook(String capabilityName, HookType hookType) 
            throws RegistryFrozenException, UndeclaredCapabilityException, CapabilityAlreadyRegisteredException {
        
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
        if (registeredHooks.containsKey(capabilityName)) {
            throw new CapabilityAlreadyRegisteredException(capabilityName, "unknown");
        }
        
        // Register the hook
        registeredHooks.put(capabilityName, hookType);
        
        LOGGER.debug("Registered hook bridge: {} -> {}", capabilityName, hookType);
    }
    
    @Override
    public void freeze() {
        if (frozen) {
            return; // Already frozen
        }
        
        LOGGER.info("Freezing hook registry with {} registered hooks", registeredHooks.size());
        this.frozen = true;
        
        // TODO: Initialize hook bridge infrastructure
        // This would involve setting up the actual hook mechanism for TypeScript handlers
    }
    
    /**
     * Gets the registered hook type for a capability.
     */
    public HookType getHookType(String capabilityName) {
        return registeredHooks.get(capabilityName);
    }
    
    /**
     * Checks if a capability is declared by any enabled extension.
     */
    private boolean isCapabilityDeclared(String capabilityName) {
        for (var descriptor : declaredCapabilities.values()) {
            for (var capability : descriptor.capabilities()) {
                if (capability.name().equals(capabilityName) && capability.type() == CapabilityType.HOOK) {
                    return true;
                }
            }
        }
        return false;
    }
}
