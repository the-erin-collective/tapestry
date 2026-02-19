package com.tapestry.typescript;

import com.tapestry.rpc.ServerApiRegistry;
import com.tapestry.rpc.ServerApiMethod;
import com.tapestry.rpc.client.RpcClientRuntime;
import com.tapestry.rpc.client.ClientEventRegistry;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Phase 16 RPC API for TypeScript mods.
 * Provides defineServerApi, server proxy, emitTo, and clientEvents.
 * Uses secure ServerApiRegistry for method allowlist.
 */
public class RpcApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcApi.class);
    
    // Client-side runtime
    private static RpcClientRuntime clientRuntime;
    
    // Server instance for emitTo functionality
    private static net.minecraft.server.MinecraftServer currentServer;
    
    // Watch registry for watch functionality
    private static com.tapestry.rpc.WatchRegistry watchRegistry;
    
    // Temporary storage for registration phase
    private static final Map<String, Value> pendingRegistrations = new ConcurrentHashMap<>();
    
    /**
     * initializeForServer - deprecated, no longer needed with secure registry.
     */
    public static void initializeForServer(Object registry) {
        LOGGER.info("Server API initialization - using secure registry");
    }
    
    /**
     * Initializes the RPC API for client-side usage.
     */
    public static void initializeForClient(RpcClientRuntime runtime) {
        clientRuntime = runtime;
    }
    
    /**
     * Sets the current server instance for emitTo functionality.
     */
    public static void setServer(net.minecraft.server.MinecraftServer server) {
        currentServer = server;
    }
    
    /**
     * Sets the watch registry for watch functionality.
     */
    public static void setWatchRegistry(com.tapestry.rpc.WatchRegistry registry) {
        watchRegistry = registry;
    }
    
    /**
     * Creates the RPC namespace object for TypeScript.
     */
    public static ProxyObject createNamespace() {
        return new ProxyObject() {
            @Override
            public String[] getMemberKeys() {
                return new String[]{"defineServerApi", "server", "emitTo", "clientEvents", "watch"};
            }
            
            @Override
            public Object getMember(String key) {
                switch (key) {
                    case "defineServerApi":
                        return (ProxyExecutable) RpcApi::defineServerApi;
                    case "server":
                        return createServerProxy();
                    case "emitTo":
                        return (ProxyExecutable) RpcApi::emitTo;
                    case "clientEvents":
                        return createClientEventsProxy();
                    case "watch":
                        return createWatchProxy();
                    default:
                        return null;
                }
            }
            
            @Override
            public boolean hasMember(String key) {
                return "defineServerApi".equals(key) || "server".equals(key) || 
                       "emitTo".equals(key) || "clientEvents".equals(key) || "watch".equals(key);
            }
            
            @Override
            public void putMember(String key, Value value) {
                throw new RuntimeException("rpc namespace is read-only");
            }
        };
    }
    
    /**
     * defineServerApi function - server-side only.
     */
    private static Object defineServerApi(Value[] arguments) {
        if (arguments.length != 1) {
            throw new RuntimeException("defineServerApi requires exactly one argument");
        }
        
        Value apiDefinition = arguments[0];
        if (!apiDefinition.hasMembers()) {
            throw new RuntimeException("defineServerApi argument must be an object");
        }
        
        // Get current mod context
        TypeScriptRuntime.ExecutionContext ctx = TypeScriptRuntime.getCurrentContext();
        if (ctx == null) {
            throw new RuntimeException("defineServerApi must be called during mod registration");
        }
        
        String modId = ctx.modId();
        Context graalContext = Context.getCurrent();
        
        // Register each method
        for (String methodName : apiDefinition.getMemberKeys()) {
            Value methodFunction = apiDefinition.getMember(methodName);
            
            if (!methodFunction.canExecute()) {
                throw new RuntimeException("API method '" + methodName + "' is not a function");
            }
            
            String qualifiedName = modId + ":" + methodName;
            pendingRegistrations.put(qualifiedName, methodFunction);
            
            // Register with secure API registry if available
            if (ServerApiRegistry.hasMethod(qualifiedName)) {
                LOGGER.warn("Server API method already registered: {}", qualifiedName);
                continue;
            }
            
            // Create ServerApiMethod wrapper
            ServerApiMethod apiMethod = new ServerApiMethod(methodFunction, graalContext, modId, methodName);
            ServerApiRegistry.registerMethod(qualifiedName, apiMethod);
            
            LOGGER.debug("Registered server API method: {}", qualifiedName);
        }
        
        return null;
    }
    
    /**
     * Creates the server proxy for client-side RPC calls.
     */
    private static ProxyObject createServerProxy() {
        return new ProxyObject() {
            @Override
            public Object getMember(String methodName) {
                return (ProxyExecutable) arguments -> {
                    if (clientRuntime == null) {
                        throw new RuntimeException("RPC client runtime not initialized");
                    }
                    
                    if (!clientRuntime.isHandshakeComplete()) {
                        throw new RuntimeException("Server RPC not ready (handshake incomplete)");
                    }
                    
                    // Convert arguments to JSON
                    com.google.gson.JsonElement argsJson = convertArgumentsToJson(arguments);
                    
                    // Make the RPC call
                    return clientRuntime.callServer(methodName, argsJson);
                };
            }
            
            @Override
            public boolean hasMember(String key) {
                return true; // Dynamic proxy - any method name is allowed
            }
            
            @Override
            public String[] getMemberKeys() {
                return new String[0]; // Dynamic - no fixed keys
            }
            
            @Override
            public void putMember(String key, Value value) {
                throw new RuntimeException("RPC server proxy is read-only");
            }
        };
    }
    
    /**
     * emitTo function - server-side only.
     */
    private static Object emitTo(Value[] arguments) {
        if (arguments.length != 3) {
            throw new RuntimeException("emitTo requires exactly 3 arguments: (player, eventName, payload)");
        }
        
        Value playerValue = arguments[0];
        String eventName = arguments[1].asString();
        Value payloadValue = arguments[2];
        
        // Validate event name
        if (!com.tapestry.rpc.ServerEventSender.isValidEventName(eventName)) {
            throw new RuntimeException("Invalid event name format: " + eventName + 
                " (must be modId:eventName)");
        }
        
        // Validate player value
        if (!com.tapestry.rpc.PlayerResolver.isValidPlayerValue(playerValue)) {
            throw new RuntimeException("Invalid player object - must have getUuid()/getName() methods or be a string");
        }
        
        // Convert payload to JSON
        com.google.gson.JsonElement payloadJson = convertValueToJson(payloadValue);
        
        // Get current server context
        TypeScriptRuntime.ExecutionContext ctx = TypeScriptRuntime.getCurrentContext();
        if (ctx == null) {
            throw new RuntimeException("emitTo must be called from within a mod context");
        }
        
        // Get current server - this needs to be accessible from context
        // For now, we'll use a placeholder approach
        try {
            // This would need access to the current server instance
            // TODO: Make server accessible through context or service locator
            net.minecraft.server.MinecraftServer server = currentServer;
            if (server == null) {
                throw new RuntimeException("No server instance available for emitTo");
            }
            
            // Resolve player
            net.minecraft.server.network.ServerPlayerEntity targetPlayer = 
                com.tapestry.rpc.PlayerResolver.resolvePlayer(playerValue, server);
            
            if (targetPlayer == null) {
                throw new RuntimeException("Player not found: " + playerValue);
            }
            
            // Send event
            com.tapestry.rpc.ServerEventSender.sendToPlayer(targetPlayer, eventName, payloadJson);
            
            LOGGER.debug("emitTo sent: event={}, player={}", eventName, targetPlayer.getName().getString());
            
        } catch (Exception e) {
            LOGGER.error("Failed to send event via emitTo", e);
            throw new RuntimeException("emitTo failed: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Creates the watch proxy for server-side watch functionality.
     */
    private static ProxyObject createWatchProxy() {
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                if ("register".equals(key)) {
                    return (ProxyExecutable) RpcApi::watchRegister;
                } else if ("unregister".equals(key)) {
                    return (ProxyExecutable) RpcApi::watchUnregister;
                } else if ("emit".equals(key)) {
                    return (ProxyExecutable) RpcApi::watchEmit;
                }
                return null;
            }
            
            @Override
            public boolean hasMember(String key) {
                return "register".equals(key) || "unregister".equals(key) || "emit".equals(key);
            }
            
            @Override
            public String[] getMemberKeys() {
                return new String[]{"register", "unregister", "emit"};
            }
            
            @Override
            public void putMember(String key, Value value) {
                throw new RuntimeException("watch namespace is read-only");
            }
        };
    }
    
    /**
     * watch.register function - server-side only.
     */
    private static Object watchRegister(Value[] arguments) {
        if (arguments.length != 2) {
            throw new RuntimeException("watch.register requires exactly 2 arguments: (player, watchKey)");
        }
        
        Value playerValue = arguments[0];
        String watchKey = arguments[1].asString();
        
        if (watchRegistry == null) {
            throw new RuntimeException("Watch system not initialized");
        }
        
        // Validate player value
        if (!com.tapestry.rpc.PlayerResolver.isValidPlayerValue(playerValue)) {
            throw new RuntimeException("Invalid player object - must have getUuid()/getName() methods or be a string");
        }
        
        // Get current server
        net.minecraft.server.MinecraftServer server = currentServer;
        if (server == null) {
            throw new RuntimeException("No server instance available for watch.register");
        }
        
        // Resolve player
        net.minecraft.server.network.ServerPlayerEntity targetPlayer = 
            com.tapestry.rpc.PlayerResolver.resolvePlayer(playerValue, server);
        
        if (targetPlayer == null) {
            throw new RuntimeException("Player not found: " + playerValue);
        }
        
        // Register watch
        watchRegistry.watch(targetPlayer, watchKey);
        
        LOGGER.debug("watch.register called: player={}, watchKey={}", 
                    targetPlayer.getName().getString(), watchKey);
        
        return null;
    }
    
    /**
     * watch.unregister function - server-side only.
     */
    private static Object watchUnregister(Value[] arguments) {
        if (arguments.length != 2) {
            throw new RuntimeException("watch.unregister requires exactly 2 arguments: (player, watchKey)");
        }
        
        Value playerValue = arguments[0];
        String watchKey = arguments[1].asString();
        
        if (watchRegistry == null) {
            throw new RuntimeException("Watch system not initialized");
        }
        
        // Validate player value
        if (!com.tapestry.rpc.PlayerResolver.isValidPlayerValue(playerValue)) {
            throw new RuntimeException("Invalid player object - must have getUuid()/getName() methods or be a string");
        }
        
        // Get current server
        net.minecraft.server.MinecraftServer server = currentServer;
        if (server == null) {
            throw new RuntimeException("No server instance available for watch.unregister");
        }
        
        // Resolve player
        net.minecraft.server.network.ServerPlayerEntity targetPlayer = 
            com.tapestry.rpc.PlayerResolver.resolvePlayer(playerValue, server);
        
        if (targetPlayer == null) {
            throw new RuntimeException("Player not found: " + playerValue);
        }
        
        // Unregister watch
        watchRegistry.unwatch(targetPlayer, watchKey);
        
        LOGGER.debug("watch.unregister called: player={}, watchKey={}", 
                    targetPlayer.getName().getString(), watchKey);
        
        return null;
    }
    
    /**
     * watch.emit function - server-side only.
     */
    private static Object watchEmit(Value[] arguments) {
        if (arguments.length != 2) {
            throw new RuntimeException("watch.emit requires exactly 2 arguments: (watchKey, payload)");
        }
        
        String watchKey = arguments[0].asString();
        Value payloadValue = arguments[1];
        
        if (watchRegistry == null) {
            throw new RuntimeException("Watch system not initialized");
        }
        
        // Convert payload to JSON
        com.google.gson.JsonElement payloadJson = convertValueToJson(payloadValue);
        
        // Emit watched value
        watchRegistry.emitWatched(watchKey, payloadJson);
        
        LOGGER.debug("watch.emit called: watchKey={}, payload={}", watchKey, payloadJson);
        
        return null;
    }
    
    /**
     * Creates clientEvents proxy for server event handling.
     */
    private static ProxyObject createClientEventsProxy() {
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                if ("on".equals(key)) {
                    return (ProxyExecutable) arguments -> {
                        if (arguments.length != 2) {
                            throw new RuntimeException("clientEvents.on requires exactly 2 arguments: (eventName, handler)");
                        }
                        
                        String eventName = arguments[0].asString();
                        Value handler = arguments[1];
                        
                        if (!handler.canExecute()) {
                            throw new RuntimeException("Event handler must be a function");
                        }
                        
                        if (clientRuntime != null) {
                            ClientEventRegistry eventRegistry = clientRuntime.getEventRegistry();
                            eventRegistry.on(eventName, payload -> {
                                // Convert JSON payload back to JS value and call handler
                                Context context = Context.getCurrent();
                                Value jsPayload = convertJsonToValue(context, payload);
                                handler.executeVoid(jsPayload);
                            });
                        }
                        
                        return null;
                    };
                }
                
                return null;
            }
            
            @Override
            public boolean hasMember(String key) {
                return "on".equals(key);
            }
            
            @Override
            public String[] getMemberKeys() {
                return new String[]{"on"};
            }
            
            @Override
            public void putMember(String key, Value value) {
                throw new RuntimeException("clientEvents namespace is read-only");
            }
        };
    }
    
    /**
     * Converts JavaScript arguments to JSON element.
     */
    private static com.google.gson.JsonElement convertArgumentsToJson(Value[] arguments) {
        if (arguments.length == 0) {
            return null;
        }
        
        if (arguments.length == 1) {
            return convertValueToJson(arguments[0]);
        }
        
        // Multiple arguments - wrap in array
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (Value arg : arguments) {
            array.add(convertValueToJson(arg));
        }
        return array;
    }
    
    /**
     * Converts a JavaScript value to JSON element.
     */
    private static com.google.gson.JsonElement convertValueToJson(Value value) {
        if (value.isNull()) {
            return null;
        } else if (value.isBoolean()) {
            return new com.google.gson.JsonPrimitive(value.asBoolean());
        } else if (value.isNumber()) {
            return new com.google.gson.JsonPrimitive(value.asDouble());
        } else if (value.isString()) {
            return new com.google.gson.JsonPrimitive(value.asString());
        } else if (value.hasArrayElements()) {
            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
            for (int i = 0; i < value.getArraySize(); i++) {
                array.add(convertValueToJson(value.getArrayElement(i)));
            }
            return array;
        } else if (value.hasMembers()) {
            com.google.gson.JsonObject object = new com.google.gson.JsonObject();
            for (String key : value.getMemberKeys()) {
                object.add(key, convertValueToJson(value.getMember(key)));
            }
            return object;
        }
        
        throw new IllegalArgumentException("Unsupported type for JSON conversion: " + value);
    }
    
    /**
     * Converts JSON element back to JavaScript value.
     */
    private static Value convertJsonToValue(Context context, com.google.gson.JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return context.asValue(null);
        } else if (json.isJsonPrimitive()) {
            var primitive = json.getAsJsonPrimitive();
            if (primitive.isBoolean()) return context.asValue(primitive.getAsBoolean());
            if (primitive.isNumber()) return context.asValue(primitive.getAsNumber());
            if (primitive.isString()) return context.asValue(primitive.getAsString());
        } else if (json.isJsonArray()) {
            Object[] array = new Object[json.getAsJsonArray().size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = convertJsonToValue(context, json.getAsJsonArray().get(i));
            }
            return context.asValue(array);
        } else if (json.isJsonObject()) {
            Map<String, Object> map = new java.util.HashMap<>();
            for (var entry : json.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), convertJsonToValue(context, entry.getValue()));
            }
            return context.asValue(map);
        }
        
        throw new IllegalArgumentException("Unsupported JSON type: " + json);
    }
    
    /**
     * Gets all pending registrations for the freeze process.
     */
    public static Map<String, Value> getPendingRegistrations() {
        return Map.copyOf(pendingRegistrations);
    }
    
    /**
     * Clears temporary registration storage.
     */
    public static void clearTemporaryStorage() {
        pendingRegistrations.clear();
    }
}
