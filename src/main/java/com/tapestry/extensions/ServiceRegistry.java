package com.tapestry.extensions;

/**
 * Registry for Java services during capability registration.
 * Services are backend objects accessed via Java (for now).
 */
public interface ServiceRegistry {
    
    /**
     * Registers a service instance.
     * 
     * @param serviceName the service name (typically the capability name)
     * @param serviceClass the service class type
     * @param instance the service instance
     * @throws RegistryFrozenException if registry is frozen
     * @throws UndeclaredCapabilityException if capability wasn't declared
     * @throws CapabilityAlreadyRegisteredException if service already registered
     */
    void addService(String serviceName, Class<?> serviceClass, Object instance) 
        throws RegistryFrozenException, UndeclaredCapabilityException, CapabilityAlreadyRegisteredException;
    
    /**
     * Freezes the registry - no further registrations allowed.
     */
    void freeze();
}
