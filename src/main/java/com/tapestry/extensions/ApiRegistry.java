package com.tapestry.extensions;

/**
 * Registry for API functions during capability registration.
 * Provides strict validation and deterministic behavior.
 */
public interface ApiRegistry {
    
    /**
     * Registers a function for the given capability.
     * 
     * @param capabilityName the capability name being registered
     * @param fn the function implementation
     * @throws RegistryFrozenException if registry is frozen
     * @throws UndeclaredCapabilityException if capability wasn't declared
     * @throws DuplicateApiPathException if API path already exists
     */
    void addFunction(String capabilityName, ProxyExecutable fn) 
        throws RegistryFrozenException, UndeclaredCapabilityException, DuplicateApiPathException;
    
    /**
     * Checks if an API path is already registered.
     * 
     * @param apiPath the API path to check
     * @return true if already registered
     */
    boolean exists(String apiPath);
    
    /**
     * Freezes the registry - no further registrations allowed.
     * Builds the ProxyObject tree for runtime exposure.
     */
    void freeze();
}
