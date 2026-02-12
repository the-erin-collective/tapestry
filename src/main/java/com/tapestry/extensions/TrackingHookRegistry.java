package com.tapestry.extensions;

import java.util.Map;
import java.util.Set;

/**
 * HookRegistry implementation that tracks registered capabilities for validation.
 */
public class TrackingHookRegistry implements HookRegistry {
    
    private final String extensionId;
    private final HookRegistry delegate;
    private final Map<String, Set<String>> registeredCapabilities;
    
    public TrackingHookRegistry(String extensionId, 
                                HookRegistry delegate, 
                                Map<String, Set<String>> registeredCapabilities) {
        this.extensionId = extensionId;
        this.delegate = delegate;
        this.registeredCapabilities = registeredCapabilities;
    }
    
    @Override
    public void registerHook(String capabilityName, HookType hookType) 
            throws RegistryFrozenException, UndeclaredCapabilityException, CapabilityAlreadyRegisteredException {
        
        // Track the capability registration
        registeredCapabilities.get(extensionId).add(capabilityName);
        
        // Delegate to actual registry
        delegate.registerHook(capabilityName, hookType);
    }
    
    @Override
    public void freeze() {
        delegate.freeze();
    }
}
