package com.tapestry.rpc.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.tapestry.networking.RpcPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.loader.api.FabricLoader;

import com.tapestry.BuildConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Phase 17: Client-side RPC runtime implementing secure facade.
 * Provides safe interface for isolated JavaScript bridge.
 */
public class RpcClientRuntime implements RpcClientFacade {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientRuntime.class);
    private static final int PROTOCOL_VERSION = 1;
    
    private final PendingCallRegistry pendingCallRegistry;
    private final ClientEventRegistry eventRegistry;
    
    public RpcClientRuntime(PendingCallRegistry pendingCallRegistry, ClientEventRegistry eventRegistry) {
        this.pendingCallRegistry = pendingCallRegistry;
        this.eventRegistry = eventRegistry;
    }
    
    /**
     * Handle incoming RPC packet from server (used by client registration).
     */
    public static void handle(String json) {
        try {
            // Delegate to instance if available
            RpcClientRuntime instance = getInstance();
            if (instance != null) {
                instance.handleIncoming(json);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle RPC packet from server", e);
        }
    }
    
    private void handleIncoming(String json) {
        // Parse and handle the incoming packet
        try {
            // Parse JSON with error handling
            JsonObject data;
            try {
                data = JsonParser.parseString(json).getAsJsonObject();
            } catch (JsonParseException e) {
                LOGGER.error("Invalid JSON from server: {}", e.getMessage());
                return;
            }
            
            // Validate protocol version
            if (!data.has("protocol") || data.get("protocol").getAsInt() != PROTOCOL_VERSION) {
                LOGGER.error("Received packet with invalid protocol version: {}", data.get("protocol"));
                return;
            }
            
            // Route based on packet type
            String packetType = data.get("type").getAsString();
            switch (packetType) {
                case "hello_ack" -> handleHelloAck(data);
                case "rpc_response" -> handleRpcResponse(data);
                case "server_event" -> handleServerEvent(data);
                default -> {
                    LOGGER.warn("Unknown packet type '{}' from server: {}", packetType);
                    // Silently ignore unknown packet types as per spec
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling RPC packet from server", e);
            // Don't crash connection, just log error
        }
    }
    
    private static RpcClientRuntime instance = null;
    private boolean handshakeComplete = false;
    
    /**
     * Phase 17: Gets singleton instance for bridge access.
     */
    public static RpcClientRuntime getInstance() {
        return instance;
    }
    
    public RpcClientRuntime() {
        this.pendingCallRegistry = new PendingCallRegistry();
        this.eventRegistry = new ClientEventRegistry();
        instance = this;
    }
    
    /**
     * Phase 17: Facade implementation for secure bridge access.
     * Makes RPC call with sanitized arguments and returns sanitized result.
     */
    @Override
    public Object call(String methodId, Map<String, Object> args) {
        if (!handshakeComplete) {
            throw new RuntimeException("HANDSHAKE_INCOMPLETE: RPC handshake not complete");
        }
        
        try {
            // Convert to JSON for existing RPC system
            JsonElement argsJson = convertToJson(args);
            
            // Use existing callServer method
            CompletableFuture<JsonElement> future = callServer(methodId, argsJson);
            
            // Block for result (bridge is synchronous)
            JsonElement result = future.get();
            
            // Convert back to Object (already sanitized by RPC system)
            return convertFromJson(result);
            
        } catch (Exception e) {
            throw new RuntimeException("RPC_CALL_FAILED: " + e.getMessage());
        }
    }
    
    /**
     * Phase 17: Facade implementation for event subscription.
     */
    @Override
    public void subscribe(String eventId) {
        if (!handshakeComplete) {
            throw new RuntimeException("HANDSHAKE_INCOMPLETE: RPC handshake not complete");
        }
        
        // Use ClientEventRegistry's on() method
        eventRegistry.on(eventId, payload -> {
            // Handle server-pushed events
            LOGGER.debug("Received event: {}", eventId);
        });
        LOGGER.debug("Subscribed to event: {}", eventId);
    }
    
    /**
     * Phase 17: Facade implementation for event unsubscription.
     */
    @Override
    public void unsubscribe(String eventId) {
        if (!handshakeComplete) {
            throw new RuntimeException("HANDSHAKE_INCOMPLETE: RPC handshake not complete");
        }
        
        boolean removed = eventRegistry.unsubscribe(eventId);
        LOGGER.debug("Unsubscribed from event: {} (handlers removed: {})", eventId, removed);
    }
    
    /**
     * Converts Map to JsonElement for existing RPC system.
     * Handles nested objects, arrays, and various data types.
     */
    private JsonElement convertToJson(Map<String, Object> args) {
        if (args == null) {
            return null;
        }
        
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                json.add(key, null);
            } else if (value instanceof String) {
                json.addProperty(key, (String) value);
            } else if (value instanceof Number) {
                json.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                json.addProperty(key, (Boolean) value);
            } else if (value instanceof Character) {
                json.addProperty(key, (Character) value);
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                JsonElement nestedJson = convertToJson((Map<String, Object>) value);
                json.add(key, nestedJson);
            } else if (value instanceof java.util.List) {
                JsonArray jsonArray = convertToJsonArray((java.util.List<?>) value);
                json.add(key, jsonArray);
            } else if (value instanceof java.util.Collection) {
                JsonArray jsonArray = convertToJsonArray(new java.util.ArrayList<>((java.util.Collection<?>) value));
                json.add(key, jsonArray);
            } else {
                // Fallback to string representation for unknown types
                json.addProperty(key, String.valueOf(value));
            }
        }
        return json;
    }
    
    /**
     * Converts a List to JsonArray.
     */
    private JsonArray convertToJsonArray(java.util.List<?> list) {
        JsonArray jsonArray = new JsonArray();
        for (Object item : list) {
            if (item == null) {
                jsonArray.add((JsonElement) null);
            } else if (item instanceof String) {
                jsonArray.add((String) item);
            } else if (item instanceof Number) {
                jsonArray.add((Number) item);
            } else if (item instanceof Boolean) {
                jsonArray.add((Boolean) item);
            } else if (item instanceof Character) {
                jsonArray.add((Character) item);
            } else if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                JsonElement nestedJson = convertToJson((Map<String, Object>) item);
                jsonArray.add(nestedJson);
            } else if (item instanceof java.util.List) {
                JsonArray nestedArray = convertToJsonArray((java.util.List<?>) item);
                jsonArray.add(nestedArray);
            } else {
                // Fallback to string representation
                jsonArray.add(String.valueOf(item));
            }
        }
        return jsonArray;
    }
    
    /**
     * Converts JsonElement back to Object for bridge.
     */
    private Object convertFromJson(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return null;
        } else if (json.isJsonPrimitive()) {
            if (json.getAsJsonPrimitive().isBoolean()) {
                return json.getAsBoolean();
            } else if (json.getAsJsonPrimitive().isNumber()) {
                return json.getAsDouble();
            } else {
                return json.getAsString();
            }
        } else if (json.isJsonObject()) {
            // Convert to Map
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            for (var entry : json.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), convertFromJson(entry.getValue()));
            }
            return map;
        } else if (json.isJsonArray()) {
            // Convert to List
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (var element : json.getAsJsonArray()) {
                list.add(convertFromJson(element));
            }
            return list;
        }
        return null;
    }
    
    /**
     * Original callServer method for compatibility with existing RPC system.
     */
    public CompletableFuture<JsonElement> callServer(String method, JsonElement args) {
        if (!handshakeComplete) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("RPC handshake not complete"));
        }
        
        String callId = UUID.randomUUID().toString();
        CompletableFuture<JsonElement> future = new CompletableFuture<>();
        
        // Register the pending call
        pendingCallRegistry.register(callId, future);
        
        // Create and send RPC call packet
        JsonObject packet = new JsonObject();
        packet.addProperty("protocol", PROTOCOL_VERSION);
        packet.addProperty("type", "rpc_call");
        packet.addProperty("id", callId);
        packet.addProperty("method", method);
        
        if (args != null) {
            packet.add("args", args);
        } else {
            packet.add("args", null);
        }
        
        sendPacket(packet);
        
        LOGGER.debug("RPC call sent: method={}, id={}", method, callId);
        
        return future;
    }
    
    /**
     * Handles incoming packet from server.
     */
    public void handlePacket(JsonObject packet) {
        String packetType = packet.get("type").getAsString();
        
        switch (packetType) {
            case "hello_ack" -> handleHelloAck(packet);
            case "handshake_fail" -> handleHandshakeFail(packet);
            case "rpc_response" -> handleRpcResponse(packet);
            case "server_event" -> handleServerEvent(packet);
            default -> LOGGER.warn("Unknown packet type from server: {}", packetType);
        }
    }
    
    /**
     * Handles successful handshake response from server.
     */
    private void handleHelloAck(JsonObject packet) {
        try {
            JsonObject server = packet.getAsJsonObject("server");
            String tapestryVersion = server.get("tapestryVersion").getAsString();
            String apiHash = server.get("apiHash").getAsString();
            
            LOGGER.info("RPC handshake completed with server. Version: {}, API Hash: {}", 
                       tapestryVersion, apiHash);
            
            handshakeComplete = true;
            
        } catch (Exception e) {
            LOGGER.error("Error processing hello_ack from server", e);
        }
    }
    
    /**
     * Handles handshake failure from server.
     */
    private void handleHandshakeFail(JsonObject packet) {
        String reason = packet.get("reason").getAsString();
        LOGGER.error("RPC handshake failed: {}", reason);
        handshakeComplete = false;
    }
    
    /**
     * Handles RPC response from server.
     */
    private void handleRpcResponse(JsonObject packet) {
        String id = packet.get("id").getAsString();
        boolean success = packet.get("success").getAsBoolean();
        
        if (success) {
            JsonElement result = packet.has("result") ? packet.get("result") : null;
            pendingCallRegistry.resolve(id, result);
        } else {
            JsonObject error = packet.getAsJsonObject("error");
            String code = error.get("code").getAsString();
            String message = error.get("message").getAsString();
            pendingCallRegistry.reject(id, new RpcException(code, message));
        }
    }
    
    /**
     * Handles server event push.
     */
    private void handleServerEvent(JsonObject packet) {
        String eventName = packet.get("event").getAsString();
        JsonElement payload = packet.has("payload") ? packet.get("payload") : null;
        
        eventRegistry.emit(eventName, payload);
    }
    
    /**
     * Sends a packet to the server.
     */
    private void sendPacket(JsonObject packet) {
        try {
            ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
            if (networkHandler == null) {
                LOGGER.warn("Cannot send RPC packet - no network connection");
                return;
            }
            
            String packetData = packet.toString();
            
            // Send using modern payload system
            ClientPlayNetworking.send(new RpcPayload(packetData));
                
        } catch (Exception e) {
            LOGGER.error("Failed to send RPC packet to server", e);
        }
    }
    
    /**
     * Initiates the handshake with the server.
     */
    public void initiateHandshake() {
        JsonObject packet = new JsonObject();
        packet.addProperty("protocol", PROTOCOL_VERSION);
        packet.addProperty("type", "hello");
        
        JsonObject client = new JsonObject();
        client.addProperty("tapestryVersion", BuildConfig.getVersion());
        
        // Add client mods
        JsonArray mods = new JsonArray();
        try {
            FabricLoader.getInstance().getAllMods().forEach(modContainer -> {
                JsonObject modInfo = new JsonObject();
                modInfo.addProperty("id", modContainer.getMetadata().getId());
                modInfo.addProperty("version", modContainer.getMetadata().getVersion().getFriendlyString());
                modInfo.addProperty("name", modContainer.getMetadata().getName());
                mods.add(modInfo);
            });
            LOGGER.debug("Added {} client mods to handshake", mods.size());
        } catch (Exception e) {
            LOGGER.warn("Failed to get client mod list, continuing with empty list", e);
        }
        
        client.add("mods", mods);
        packet.add("client", client);
        
        sendPacket(packet);
        
        LOGGER.debug("RPC handshake initiated with version {}", BuildConfig.getVersion());
    }
    
    /**
     * Gets the client event registry for server event handling.
     */
    public ClientEventRegistry getEventRegistry() {
        return eventRegistry;
    }
    
    /**
     * Checks if handshake is complete.
     */
    public boolean isHandshakeComplete() {
        return handshakeComplete;
    }
}
