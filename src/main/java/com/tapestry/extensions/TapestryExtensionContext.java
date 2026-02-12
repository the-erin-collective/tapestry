package com.tapestry.extensions;

import com.tapestry.api.TapestryAPI;
import com.tapestry.lifecycle.TapestryPhase;

/**
 * Context for Tapestry extensions providing access to API and phase information.
 */
public class TapestryExtensionContext {
    
    private final TapestryAPI api;
    private final TapestryPhase phase;
    
    public TapestryExtensionContext(TapestryAPI api, TapestryPhase phase) {
        this.api = api;
        this.phase = phase;
    }
    
    /**
     * @return the Tapestry API instance
     */
    public TapestryAPI api() {
        return api;
    }
    
    /**
     * @return the current phase
     */
    public TapestryPhase phase() {
        return phase;
    }
}
