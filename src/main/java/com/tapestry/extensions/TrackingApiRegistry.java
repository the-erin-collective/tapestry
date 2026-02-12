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
    public void addFunction(String extensionId, String capabilityName, ProxyExecutable fn) 
            throws RegistryFrozenException, UndeclaredCapabilityException, DuplicateApiPathException {
        
        // Validate that the extensionId matches this tracker's extension
        if (!this.extensionId.equals(extensionId)) {
            throw new IllegalArgumentException("Extension ID mismatch: expected " + this.extensionId + ", got " + extensionId);
        }
        
        // Delegate to actual registry first - only track if successful
        delegate.addFunction(extensionId, capabilityName, fn);
        
        // Track the capability registration only after successful delegation
        registeredCapabilities.get(extensionId).add(capabilityName);
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
