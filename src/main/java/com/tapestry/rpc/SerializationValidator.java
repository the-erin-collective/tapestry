package com.tapestry.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates JSON payloads against Phase 16 serialization constraints.
 * Enforces size limits, type restrictions, and structural rules.
 */
public class SerializationValidator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationValidator.class);
    
    // Configuration limits
    private static final int MAX_PAYLOAD_SIZE = 64 * 1024; // 64KB
    private static final int MAX_NESTING_DEPTH = 20;
    private static final int MAX_ARRAY_LENGTH = 10_000;
    private static final int MAX_OBJECT_KEYS = 1_000;
    
    /**
     * Validates a JSON element for RPC usage.
     * Throws SerializationException if validation fails.
     */
    public static void validate(JsonElement json) throws SerializationException {
        validate(json, 0, 0);
    }
    
    /**
     * Internal recursive validation method.
     */
    private static void validate(JsonElement json, int depth, int totalElements) throws SerializationException {
        if (depth > MAX_NESTING_DEPTH) {
            throw new SerializationException("MAX_NESTING_DEPTH", 
                "JSON nesting depth exceeds limit of " + MAX_NESTING_DEPTH);
        }
        
        if (totalElements > MAX_ARRAY_LENGTH) {
            throw new SerializationException("MAX_ARRAY_LENGTH", 
                "Array length exceeds limit of " + MAX_ARRAY_LENGTH);
        }
        
        if (json == null || json.isJsonNull()) {
            // Null is allowed
            return;
        }
        
        if (json.isJsonPrimitive()) {
            validatePrimitive(json.getAsJsonPrimitive());
        } else if (json.isJsonArray()) {
            validateArray(json.getAsJsonArray(), depth, totalElements);
        } else if (json.isJsonObject()) {
            validateObject(json.getAsJsonObject(), depth, totalElements);
        } else {
            throw new SerializationException("UNSUPPORTED_TYPE", 
                "Unsupported JSON type: " + json.getClass().getSimpleName());
        }
    }
    
    /**
     * Validates primitive values.
     */
    private static void validatePrimitive(JsonPrimitive primitive) throws SerializationException {
        // All primitive types are allowed: string, number, boolean
        // No additional validation needed for primitives
    }
    
    /**
     * Validates JSON arrays.
     */
    private static void validateArray(JsonArray array, int depth, int totalElements) throws SerializationException {
        if (array.size() > MAX_ARRAY_LENGTH) {
            throw new SerializationException("MAX_ARRAY_LENGTH", 
                "Array length " + array.size() + " exceeds limit of " + MAX_ARRAY_LENGTH);
        }
        
        for (int i = 0; i < array.size(); i++) {
            validate(array.get(i), depth + 1, totalElements + 1);
        }
    }
    
    /**
     * Validates JSON objects.
     */
    private static void validateObject(JsonObject object, int depth, int totalElements) throws SerializationException {
        if (object.size() > MAX_OBJECT_KEYS) {
            throw new SerializationException("MAX_OBJECT_KEYS", 
                "Object has " + object.size() + " keys, exceeding limit of " + MAX_OBJECT_KEYS);
        }
        
        // Validate all keys are strings (Gson guarantees this)
        for (var entry : object.entrySet()) {
            String key = entry.getKey();
            
            // Check for forbidden key patterns
            if (key.startsWith("__") || key.startsWith("$")) {
                throw new SerializationException("FORBIDDEN_KEY", 
                    "Object key '" + key + "' starts with forbidden prefix");
            }
            
            // Check key length
            if (key.length() > 256) {
                throw new SerializationException("KEY_TOO_LONG", 
                    "Object key '" + key + "' exceeds maximum length of 256 characters");
            }
            
            validate(entry.getValue(), depth + 1, totalElements + 1);
        }
    }
    
    /**
     * Validates payload size by checking string representation.
     */
    public static void validateSize(String jsonString) throws SerializationException {
        if (jsonString.length() > MAX_PAYLOAD_SIZE) {
            throw new SerializationException("MAX_PAYLOAD_SIZE", 
                "JSON payload size " + jsonString.length() + " exceeds limit of " + MAX_PAYLOAD_SIZE + " bytes");
        }
    }
    
    /**
     * Checks if a value is serializable according to Phase 16 rules.
     * This is used for server-side return value validation.
     */
    public static boolean isSerializable(Object value) {
        if (value == null) {
            return true;
        }
        
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return true;
        }
        
        if (value instanceof Object[] array) {
            if (array.length > MAX_ARRAY_LENGTH) {
                return false;
            }
            for (Object item : array) {
                if (!isSerializable(item)) {
                    return false;
                }
            }
            return true;
        }
        
        if (value instanceof java.util.Map<?, ?> map) {
            if (map.size() > MAX_OBJECT_KEYS) {
                return false;
            }
            for (var entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    return false;
                }
                if (!isSerializable(entry.getValue())) {
                    return false;
                }
            }
            return true;
        }
        
        // All other types are forbidden
        return false;
    }
    
    /**
     * Sanitizes error messages for client transmission.
     */
    public static String sanitizeErrorMessage(String originalMessage) {
        if (originalMessage == null) {
            return "Unknown error";
        }
        
        // Remove potential sensitive information
        String sanitized = originalMessage.replaceAll("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b", "[IP]");
        sanitized = sanitized.replaceAll("\\b[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}\\b", "[UUID]");
        
        // Truncate if too long
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 497) + "...";
        }
        
        return sanitized;
    }
}
