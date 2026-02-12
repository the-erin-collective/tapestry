package com.tapestry.extensions;

/**
 * Registry for hook bridges during capability registration.
 * Hooks are backend concepts - actual handlers come from TypeScript.
 */
public interface HookRegistry {
    
    /**
     * Registers a hook bridge for the given capability.
     * 
     * @param capabilityName the capability name being registered
     * @param hookType the type of hook
     * @throws RegistryFrozenException if registry is frozen
     * @throws UndeclaredCapabilityException if capability wasn't declared
     * @throws CapabilityAlreadyRegisteredException if hook already registered
     */
    void registerHook(String capabilityName, HookType hookType) 
        throws RegistryFrozenException, UndeclaredCapabilityException, CapabilityAlreadyRegisteredException;
    
    /**
     * Freezes the registry - no further registrations allowed.
     */
    void freeze();
}
