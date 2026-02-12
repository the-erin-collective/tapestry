package com.tapestry.extensions;

import org.slf4j.Logger;

/**
 * Context provided to extensions during capability registration.
 * Provides access to registries and logging.
 */
public interface TapestryExtensionContext {
    
    /**
     * @return the ID of the extension being registered
     */
    String extensionId();
    
    /**
     * @return the API registry for registering functions
     */
    ApiRegistry api();
    
    /**
     * @return the hook registry for registering hook bridges
     */
    HookRegistry hooks();
    
    /**
     * @return the service registry for registering Java services
     */
    ServiceRegistry services();
    
    /**
     * @return logger for this extension
     */
    Logger log();
}
