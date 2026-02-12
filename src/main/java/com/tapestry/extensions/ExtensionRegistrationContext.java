package com.tapestry.extensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Implementation of TapestryExtensionContext for capability registration.
 * Provides access to registries and logging for extensions.
 */
public class ExtensionRegistrationContext implements TapestryExtensionContext {
    
    private final String extensionId;
    private final ApiRegistry apiRegistry;
    private final HookRegistry hookRegistry;
    private final ServiceRegistry serviceRegistry;
    private final Map<String, Set<String>> registeredCapabilities;
    private final Logger logger;
    
    public ExtensionRegistrationContext(String extensionId,
                                  ApiRegistry apiRegistry,
                                  HookRegistry hookRegistry,
                                  ServiceRegistry serviceRegistry,
                                  Map<String, Set<String>> registeredCapabilities) {
        this.extensionId = extensionId;
        this.apiRegistry = apiRegistry;
        this.hookRegistry = hookRegistry;
        this.serviceRegistry = serviceRegistry;
        this.registeredCapabilities = registeredCapabilities;
        this.logger = LoggerFactory.getLogger("tapestry.extension." + extensionId);
        
        // Initialize capability tracking for this extension
        registeredCapabilities.putIfAbsent(extensionId, new java.util.HashSet<>());
    }
    
    @Override
    public String extensionId() {
        return extensionId;
    }
    
    @Override
    public ApiRegistry api() {
        return new TrackingApiRegistry(extensionId, apiRegistry, registeredCapabilities);
    }
    
    @Override
    public HookRegistry hooks() {
        return new TrackingHookRegistry(extensionId, hookRegistry, registeredCapabilities);
    }
    
    @Override
    public ServiceRegistry services() {
        return new TrackingServiceRegistry(extensionId, serviceRegistry, registeredCapabilities);
    }
    
    @Override
    public Logger log() {
        return logger;
    }
}
