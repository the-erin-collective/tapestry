package com.tapestry.typescript;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import java.util.Map;

/**
 * Implementation of tapestry.mod.define function for TypeScript mod registration.
 * 
 * This class provides the bridge between JavaScript mod definitions
 * and the Java-side TsModRegistry.
 */
public class TsModDefineFunction {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TsModDefineFunction.class);
    
    // Mod ID validation pattern: [a-z][a-z0-9_]{0,63}
    private static final Pattern MOD_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");
    
    private final TsModRegistry registry;
    
    public TsModDefineFunction(TsModRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * Defines a TypeScript mod.
     * 
     * @param modDefinition the mod definition object from JavaScript
     * @throws IllegalStateException if called outside TS_LOAD phase
     * @throws IllegalArgumentException if mod definition is invalid
     */
    public void define(Object modDefinition) {
        // Phase enforcement: only allowed during TS_LOAD
        PhaseController.getInstance().requirePhase(TapestryPhase.TS_LOAD);
        
        if (modDefinition == null) {
            throw new IllegalArgumentException("Mod definition must be an object with properties");
        }
        
        // Get the current source for one-define-per-file enforcement
        String currentSource = TypeScriptRuntime.getCurrentSource();
        if (currentSource == null) {
            throw new IllegalStateException("No current source context for mod definition");
        }
        
        // Enforce one-define-per-file rule
        if (TypeScriptRuntime.hasModDefinedInSource(currentSource)) {
            throw new IllegalStateException("Multiple tapestry.mod.define calls in source: " + currentSource);
        }
        
        // Convert to Value for property access
        Value modDefValue = Value.asValue(modDefinition);
        
        // Extract required properties
        String id = extractString(modDefValue, "id", "mod id");
        Value onLoad = extractValue(modDefValue, "onLoad", "onLoad function");
        Value onEnable = extractOptionalValue(modDefValue, "onEnable");
        
        // Validate mod ID
        if (!MOD_ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid mod ID: " + id + ". Must match pattern: [a-z][a-z0-9_]{0,63}");
        }
        
        // Create the mod definition
        TsModDefinition mod = new TsModDefinition(id, onLoad, onEnable, currentSource);
        
        // Register the mod
        registry.registerMod(mod);
        
        // Mark this source as having a mod defined
        TypeScriptRuntime.markModDefinedInSource(currentSource);
        
        LOGGER.info("Defined TypeScript mod '{}' from source '{}'", id, currentSource);
    }
    
    /**
     * Extracts a string value from the mod definition object.
     * 
     * @param obj the object to extract from
     * @param key the key to extract
     * @param fieldName the field name for error messages
     * @return the extracted string value
     * @throws IllegalArgumentException if the value is missing or invalid
     */
    private String extractString(Value obj, String key, String fieldName) {
        Value value = obj.getMember(key);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        
        if (!value.isString()) {
            throw new IllegalArgumentException(fieldName + " must be a string");
        }
        
        String result = value.asString();
        if (result == null || result.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        
        return result;
    }
    
    /**
     * Extracts a required value from the mod definition object.
     * 
     * @param obj the object to extract from
     * @param key the key to extract
     * @param fieldName the field name for error messages
     * @return the extracted value
     * @throws IllegalArgumentException if the value is missing or invalid
     */
    private Value extractValue(Value obj, String key, String fieldName) {
        Value value = obj.getMember(key);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        
        if (!value.canExecute()) {
            throw new IllegalArgumentException(fieldName + " must be a function");
        }
        
        return value;
    }
    
    /**
     * Extracts an optional value from the mod definition object.
     * 
     * @param obj the object to extract from
     * @param key the key to extract
     * @return the extracted value, or null if not present
     */
    private Value extractOptionalValue(Value obj, String key) {
        Value value = obj.getMember(key);
        if (value == null || value.isNull()) {
            return null;
        }
        
        if (!value.canExecute()) {
            throw new IllegalArgumentException(key + " must be a function if provided");
        }
        
        return value;
    }
}
