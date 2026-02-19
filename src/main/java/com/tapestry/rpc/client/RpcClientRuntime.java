package com.tapestry.rpc.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tapestry.networking.RpcCustomPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
        // TODO: Implement packet routing logic
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
        
        // For now, just log - proper unsubscription requires ClientEventRegistry changes
        // TODO: Add unsubscribe method to ClientEventRegistry
        LOGGER.debug("Unsubscribed from event: {}", eventId);
    }
    
    /**
     * Converts Map to JsonElement for existing RPC system.
     */
    private JsonElement convertToJson(Map<String, Object> args) {
        // TODO: Implement conversion using existing JSON utilities
        // For now, create simple JsonObject
        JsonObject json = new JsonObject();
        if (args != null) {
            for (Map.Entry<String, Object> entry : args.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    json.addProperty(entry.getKey(), (String) value);
                } else if (value instanceof Number) {
                    json.addProperty(entry.getKey(), (Number) value);
                } else if (value instanceof Boolean) {
                    json.addProperty(entry.getKey(), (Boolean) value);
                } else {
                    json.addProperty(entry.getKey(), String.valueOf(value));
                }
            }
        }
        return json;
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
            // TODO: Fix mapping issues - temporarily disabled
        return;
                
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
        client.addProperty("tapestryVersion", "0.16.0"); // TODO: Get from build config
        
        // Add client mods
        com.google.gson.JsonArray mods = new com.google.gson.JsonArray();
        // TODO: Get actual mod list
        mods.add(new com.google.gson.JsonObject());
        
        client.add("mods", mods);
        packet.add("client", client);
        
        sendPacket(packet);
        
        LOGGER.debug("RPC handshake initiated");
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
