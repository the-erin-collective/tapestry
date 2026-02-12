package com.tapestry.extensions;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete implementation of ServiceRegistry with strict validation and freeze behavior.
 */
public class DefaultServiceRegistry implements ServiceRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultServiceRegistry.class);
    
    private final PhaseController phaseController;
    private final Map<String, TapestryExtensionDescriptor> declaredCapabilities;
    private final Map<String, ServiceEntry> registeredServices;
    private volatile boolean frozen = false;
    
    public DefaultServiceRegistry(PhaseController phaseController, 
                                 Map<String, TapestryExtensionDescriptor> declaredCapabilities) {
        this.phaseController = phaseController;
        this.declaredCapabilities = declaredCapabilities;
        this.registeredServices = new ConcurrentHashMap<>();
    }
    
    @Override
    public void addService(String serviceName, Class<?> serviceClass, Object instance) 
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
        if (!isCapabilityDeclared(serviceName)) {
            throw new UndeclaredCapabilityException(serviceName, "unknown");
        }
        
        // Check if service already registered
        if (registeredServices.containsKey(serviceName)) {
            throw new CapabilityAlreadyRegisteredException(serviceName, "unknown");
        }
        
        // Validate instance type
        if (!serviceClass.isInstance(instance)) {
            throw new ExtensionRegistrationException(
                "Service instance " + instance.getClass().getName() + 
                " is not an instance of " + serviceClass.getName()
            );
        }
        
        // Register the service
        registeredServices.put(serviceName, new ServiceEntry(serviceClass, instance));
        
        LOGGER.debug("Registered service: {} ({})", serviceName, serviceClass.getSimpleName());
    }
    
    @Override
    public void freeze() {
        if (frozen) {
            return; // Already frozen
        }
        
        LOGGER.info("Freezing service registry with {} registered services", registeredServices.size());
        this.frozen = true;
        
        // TODO: Initialize service access infrastructure
        // This would involve setting up service lookup mechanisms
    }
    
    /**
     * Gets a registered service by name.
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(String serviceName, Class<T> serviceClass) {
        ServiceEntry entry = registeredServices.get(serviceName);
        if (entry != null && serviceClass.isAssignableFrom(entry.serviceClass)) {
            return (T) entry.instance;
        }
        return null;
    }
    
    /**
     * Gets a registered service entry.
     */
    public ServiceEntry getServiceEntry(String serviceName) {
        return registeredServices.get(serviceName);
    }
    
    /**
     * Checks if a capability is declared by any enabled extension.
     */
    private boolean isCapabilityDeclared(String serviceName) {
        for (var descriptor : declaredCapabilities.values()) {
            for (var capability : descriptor.capabilities()) {
                if (capability.name().equals(serviceName) && capability.type() == CapabilityType.SERVICE) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Internal record for service entries.
     */
    public record ServiceEntry(Class<?> serviceClass, Object instance) {}
}
