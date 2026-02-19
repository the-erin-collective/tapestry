package com.tapestry.rpc;

import java.util.*;

/**
 * Phase 16.5: Argument sanitization layer for RPC security.
 * Enforces strict data types and size/depth limits.
 * 
 * Number Type Policy:
 * - JSON numbers are normalized to Double for consistency
 * - Mod authors should cast to Integer/Long as needed
 * - This prevents ClassCastException on numeric operations
 */
public class RpcSanitizer {
    
    // Security limits
    private static final int MAX_DEPTH = 16;
    private static final int MAX_STRING_LENGTH = 32768;
    private static final int MAX_LIST_SIZE = 1024;
    private static final int MAX_MAP_SIZE = 1024;
    
    public static Object sanitize(Object obj) throws RpcValidationException {
        return sanitize(obj, 0); // Root depth starts at 0
    }
    
    private static Object sanitize(Object obj, int depth) throws RpcValidationException {
        if (depth > MAX_DEPTH) {
            throw new RpcValidationException("Max depth exceeded");
        }
        
        // Allow primitive types (numbers normalized to Double)
        if (obj == null || obj instanceof String || obj instanceof Boolean || obj instanceof Number) {
            if (obj instanceof String && ((String) obj).length() > MAX_STRING_LENGTH) {
                throw new RpcValidationException("String too long");
            }
            // Normalize all numbers to Double for consistency
            if (obj instanceof Number) {
                return ((Number) obj).doubleValue();
            }
            return obj;
        }
        
        // Handle Maps with deterministic order
        if (obj instanceof Map<?, ?> map) {
            if (map.size() > MAX_MAP_SIZE) {
                throw new RpcValidationException("Map too large");
            }
            Map<String, Object> sanitized = new LinkedHashMap<>(); // Preserves insertion order
            for (var entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new RpcValidationException("Non-string map key");
                }
                sanitized.put(key, sanitize(entry.getValue(), depth + 1));
            }
            return sanitized;
        }
        
        // Handle Lists
        if (obj instanceof List<?> list) {
            if (list.size() > MAX_LIST_SIZE) {
                throw new RpcValidationException("List too large");
            }
            List<Object> sanitized = new ArrayList<>();
            for (Object element : list) {
                sanitized.add(sanitize(element, depth + 1));
            }
            return sanitized;
        }
        
        throw new RpcValidationException("Unsupported type: " + obj.getClass());
    }
}
