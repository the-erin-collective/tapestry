package com.tapestry.typescript;

import com.tapestry.config.ConfigService;
import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * TypeScript API for configuration operations.
 * 
 * Provides read-only access to mod configuration with
 * fail-fast error handling.
 */
public class TsConfigApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TsConfigApi.class);
    
    private final ConfigService configService;
    
    public TsConfigApi(ConfigService configService) {
        this.configService = configService;
    }
    
    /**
     * Creates the config namespace object.
     * 
     * @return ProxyObject with config functions
     */
    public ProxyObject createNamespace() {
        Map<String, Object> config = new HashMap<>();
        
        // get function for specific mod config
        config.put("get", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("config.get requires exactly 1 argument: (modId)");
            }
            
            String modId = args[0].asString();
            if (modId == null || modId.isBlank()) {
                throw new IllegalArgumentException("Mod ID must be a non-empty string");
            }
            
            return configService.getConfig(modId);
        });
        
        // self function for current mod's config
        config.put("self", (ProxyExecutable) args -> {
            if (args.length != 0) {
                throw new IllegalArgumentException("config.self takes no arguments");
            }
            
            String modId = TypeScriptRuntime.getCurrentModId();
            if (modId == null) {
                throw new IllegalStateException("No mod ID set in current context");
            }
            
            return configService.getSelfConfig(modId);
        });
        
        return ProxyObject.fromMap(config);
    }
}
