package com.tapestry.rpc;

import com.google.gson.JsonElement;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a server API method that can be called via RPC.
 * Wraps a Graal function with metadata.
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
        
        // Validate that the function is callable
        if (!graalFunction.canExecute()) {
            throw new IllegalArgumentException("Provided value is not executable: " + methodName);
        }
    }
    
    /**
     * Invokes the method with the given context and arguments.
     * Returns a CompletableFuture that completes with the JSON result.
     */
    public CompletableFuture<JsonElement> invoke(ServerContext ctx, JsonElement argsJson) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Enter Graal context
                Context context = Context.newBuilder("js")
                    .allowHostAccess(true)
                    .allowHostClassLoading(true)
                    .build();
                
                try {
                    // Convert JSON args to JS object
                    Value argsValue = context.asValue(convertJsonToObject(argsJson));
                    
                    // Create safe context proxy
                    Value contextProxy = createContextProxy(context, ctx);
                    
                    // Execute the function
                    Value result = graalFunction.execute(contextProxy, argsValue);
                    
                    // Convert result back to JSON
                    return convertJsToJson(result);
                    
                } finally {
                    context.close();
                }
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke RPC method: " + getQualifiedName(), e);
            }
        });
    }
    
    /**
     * Creates a safe proxy for server context to expose to JavaScript.
     */
    private Value createContextProxy(Context context, ServerContext serverContext) {
        return context.eval("js", "(" + createContextProxyJs() + ")")
            .execute(serverContext);
    }
    
    /**
     * JavaScript code for creating safe context proxy.
     */
    private String createContextProxyJs() {
        return """
            (serverCtx) => ({
                player: {
                    getName: () => serverCtx.player.getName(),
                    getUuid: () => serverCtx.player.getUuid(),
                    sendMessage: (msg) => serverCtx.player.sendMessage(msg)
                },
                world: {
                    getName: () => serverCtx.world.getName(),
                    getTime: () => serverCtx.world.getTime()
                }
            })
            """;
    }
    
    /**
     * Converts JSON element to Java object for Graal.
     */
    private Object convertJsonToObject(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return null;
        } else if (json.isJsonPrimitive()) {
            var primitive = json.getAsJsonPrimitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isNumber()) return primitive.getAsNumber();
            if (primitive.isString()) return primitive.getAsString();
        } else if (json.isJsonArray()) {
            return json.getAsJsonArray().asList().stream()
                .map(this::convertJsonToObject)
                .toArray();
        } else if (json.isJsonObject()) {
            var map = new java.util.HashMap<String, Object>();
            for (var entry : json.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), convertJsonToObject(entry.getValue()));
            }
            return map;
        }
        throw new IllegalArgumentException("Unsupported JSON type: " + json);
    }
    
    /**
     * Converts JavaScript value back to JSON.
     */
    private JsonElement convertJsToJson(Value value) {
        if (value.isNull()) {
            return null;
        } else if (value.isBoolean()) {
            return new com.google.gson.JsonPrimitive(value.asBoolean());
        } else if (value.isNumber()) {
            return new com.google.gson.JsonPrimitive(value.asDouble());
        } else if (value.isString()) {
            return new com.google.gson.JsonPrimitive(value.asString());
        } else if (value.hasArrayElements()) {
            var array = new com.google.gson.JsonArray();
            for (int i = 0; i < value.getArraySize(); i++) {
                array.add(convertJsToJson(value.getArrayElement(i)));
            }
            return array;
        } else if (value.hasMembers()) {
            var object = new com.google.gson.JsonObject();
            for (String key : value.getMemberKeys()) {
                object.add(key, convertJsToJson(value.getMember(key)));
            }
            return object;
        }
        
        // Reject host objects and other unsupported types
        throw new IllegalArgumentException("Unsupported return type in RPC method: " + value);
    }
    
    public String getModId() {
        return modId;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public String getQualifiedName() {
        return modId + ":" + methodName;
    }
}
