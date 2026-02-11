package com.tapestry.extension;

import com.tapestry.api.TapestryAPI;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Context provided to extensions during the REGISTRATION phase.
 * 
 * This context allows extensions to safely extend domains and register
 * mod-owned APIs. All operations are phase-protected and will fail
 * if called outside the REGISTRATION phase.
 */
public class TapestryExtensionContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryExtensionContext.class);
    
    private final String extensionId;
    private final TapestryAPI api;
    
    public TapestryExtensionContext(TapestryAPI api, TapestryPhase phase) {
        this.extensionId = "unknown";
        this.api = api;
        
        // Ensure we're in the correct phase
        PhaseController.getInstance().requirePhase(phase);
    }
    
    /**
     * Gets the ID of the extension this context belongs to.
     * 
     * @return the extension ID
     */
    public String getExtensionId() {
        return extensionId;
    }
    
    /**
     * Extends a core domain with a new property.
     * This is additive-only - existing properties cannot be overridden.
     * 
     * @param domain the domain to extend (e.g., "worlds", "worldgen", "events")
     * @param key the property key to add
     * @param value the value to set
     * @throws IllegalStateException if called outside REGISTRATION phase
     * @throws IllegalArgumentException if the property already exists
     */
    public void extendDomain(String domain, String key, Object value) {
        PhaseController.getInstance().requirePhase(TapestryPhase.REGISTRATION);
        
        if (!api.getDomains().containsKey(domain)) {
            throw new IllegalArgumentException("Unknown domain: " + domain);
        }
        
        if (api.getDomains().get(domain) instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> domainMap = (Map<String, Object>) api.getDomains().get(domain);
            
            if (domainMap.containsKey(key)) {
                throw new IllegalArgumentException(
                    String.format("Domain %s already contains key '%s'", domain, key)
                );
            }
            
            domainMap.put(key, value);
            LOGGER.debug("Extended domain {} with key {} = {}", domain, key, value);
        }
    }
    
    /**
     * Registers a mod-owned API under the mods namespace.
     * The API will be accessible as tapestry.mods.{extensionId}.{key}
     * 
     * @param key the API key
     * @param value the API object
     * @throws IllegalStateException if called outside REGISTRATION phase
     * @throws IllegalArgumentException if the key already exists for this extension
     */
    public void registerModAPI(String key, Object value) {
        PhaseController.getInstance().requirePhase(TapestryPhase.REGISTRATION);
        
        LOGGER.debug("Extension {} registering mod API with key {}", extensionId, key);
        api.registerModApi(key, value);
    }
}
