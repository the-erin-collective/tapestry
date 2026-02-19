package com.tapestry.rpc;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a server API method that can be called via RPC.
 * Wraps a Graal function with metadata.
 * Enforces sanitized Map<String,Object> arguments.
 */
public class ServerApiMethod {
    
    private final Value graalFunction;
    private final Context graalContext;
    private final String modId;
    private final String methodName;
    
    public ServerApiMethod(Value graalFunction, Context graalContext, String modId, String methodName) {
        this.graalFunction = graalFunction;
        this.graalContext = graalContext;
        this.modId = modId;
        this.methodName = methodName;
        
        // Validate that function is callable
        if (!graalFunction.canExecute()) {
            throw new IllegalArgumentException("Provided value is not executable: " + methodName);
        }
    }
    
    /**
     * Gets the mod ID that owns this method.
     */
    public String getModId() {
        return modId;
    }
    
    /**
     * Gets the method name.
     */
    public String getMethodName() {
        return methodName;
    }
    
    /**
     * Invokes the method with sanitized arguments.
     * 
     * @param serverContext Server execution context
     * @param args Pre-sanitized Map<String,Object> arguments
     * @return CompletableFuture with result
     */
    public CompletableFuture<Object> invoke(Object serverContext, Map<String, Object> args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Execute on server thread
                Value result = graalFunction.execute(serverContext, args);
                
                // Convert Graal Value to Java Object
                if (result.isNull()) {
                    return null;
                } else if (result.isBoolean()) {
                    return result.asBoolean();
                } else if (result.isString()) {
                    return result.asString();
                } else if (result.isNumber()) {
                    return result.asDouble();
                } else if (result.hasArrayElements()) {
                    // Convert array to List
                    int length = (int) result.getArraySize();
                    Object[] array = new Object[length];
                    for (int i = 0; i < length; i++) {
                        array[i] = result.getArrayElement(i).as(Object.class);
                    }
                    return java.util.Arrays.asList(array);
                } else if (result.hasMembers()) {
                    // Convert object to Map
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    for (String key : result.getMemberKeys()) {
                        map.put(key, result.getMember(key).as(Object.class));
                    }
                    return map;
                } else {
                    return result.as(Object.class);
                }
            } catch (Exception e) {
                throw new RuntimeException("Method execution failed", e);
            }
        });
    }
}
