package com.tapestry.extensions;

import java.util.Map;
import java.util.Set;

/**
 * ServiceRegistry implementation that tracks registered capabilities for validation.
 */
public class TrackingServiceRegistry implements ServiceRegistry {
    
    private final String extensionId;
    private final ServiceRegistry delegate;
    private final Map<String, Set<String>> registeredCapabilities;
    
    public TrackingServiceRegistry(String extensionId, 
                                    ServiceRegistry delegate, 
                                    Map<String, Set<String>> registeredCapabilities) {
        this.extensionId = extensionId;
        this.delegate = delegate;
        this.registeredCapabilities = registeredCapabilities;
    }
    
    @Override
    public void addService(String serviceName, Class<?> serviceClass, Object instance) 
            throws RegistryFrozenException, UndeclaredCapabilityException, CapabilityAlreadyRegisteredException {
        
        // Track the capability registration (using service name as capability name)
        registeredCapabilities.get(extensionId).add(serviceName);
        
        // Delegate to actual registry
        delegate.addService(serviceName, serviceClass, instance);
    }
    
    @Override
    public void freeze() {
        delegate.freeze();
    }
}
