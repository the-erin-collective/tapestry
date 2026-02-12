package com.tapestry.extensions;

import java.util.Map;
import java.util.Set;

/**
 * ApiRegistry implementation that tracks registered capabilities for validation.
 */
public class TrackingApiRegistry implements ApiRegistry {
    
    private final String extensionId;
    private final ApiRegistry delegate;
    private final Map<String, Set<String>> registeredCapabilities;
    
    public TrackingApiRegistry(String extensionId, 
                               ApiRegistry delegate, 
                               Map<String, Set<String>> registeredCapabilities) {
        this.extensionId = extensionId;
        this.delegate = delegate;
        this.registeredCapabilities = registeredCapabilities;
    }
    
    @Override
    public void addFunction(String capabilityName, ProxyExecutable fn) 
            throws RegistryFrozenException, UndeclaredCapabilityException, DuplicateApiPathException {
        
        // Track the capability registration
        registeredCapabilities.get(extensionId).add(capabilityName);
        
        // Delegate to actual registry
        delegate.addFunction(capabilityName, fn);
    }
    
    @Override
    public boolean exists(String apiPath) {
        return delegate.exists(apiPath);
    }
    
    @Override
    public void freeze() {
        delegate.freeze();
    }
}
