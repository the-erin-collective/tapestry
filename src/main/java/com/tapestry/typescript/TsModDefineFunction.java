package com.tapestry.typescript;

import com.tapestry.lifecycle.PhaseController;
import com.tapestry.lifecycle.TapestryPhase;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Implementation of tapestry.mod.define function for TypeScript mod registration.
 * 
 * This class provides the bridge between JavaScript mod definitions
 * and the Java-side TsModRegistry.
 */
public class TsModDefineFunction {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TsModDefineFunction.class);
    
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
        
        // For now, we'll assume modDefinition is a Map-like object
        // In a real implementation, we'd need to properly extract values from the JS object
        String id = "unknown"; // Would extract from modDefinition
        Value onLoad = null; // Would extract from modDefinition
        Value onEnable = null; // Would extract from modDefinition
        
        // Create source information using current source tracking
        String source = TypeScriptRuntime.getCurrentSource();
        if (source == null) {
            source = "unknown";
        }
        
        // Create and register the mod definition
        TsModDefinition mod = new TsModDefinition(id, onLoad, onEnable, source);
        registry.registerMod(mod);
        
        LOGGER.info("Registered mod '{}' from source '{}'", id, source);
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
