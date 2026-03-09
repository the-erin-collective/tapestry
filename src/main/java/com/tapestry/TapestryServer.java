package com.tapestry;

import com.tapestry.gameplay.GameplayAPI;
import com.tapestry.gameplay.patch.ModLoadOrder;
import com.tapestry.gameplay.patch.PatchPlan;
import com.tapestry.gameplay.patch.PatchRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import com.tapestry.networking.RpcPayload;
import com.tapestry.networking.RpcPayloadCodec;
import com.tapestry.rpc.ApiRegistry;
import com.tapestry.rpc.ServerApiRegistry;
import com.tapestry.rpc.RpcDispatcher;
import com.tapestry.rpc.HandshakeRegistry;
import com.tapestry.rpc.HandshakeHandler;
import com.tapestry.rpc.RpcServerRuntime;
import com.tapestry.rpc.RpcPacketHandler;
import com.tapestry.rpc.client.RpcClientRuntime;
import com.tapestry.rpc.WatchRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Server-side Tapestry initialization.
 * Handles server-specific lifecycle and services.
 */
public class TapestryServer implements ModInitializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TapestryServer.class);
    
    // Phase 16: RPC system components
    private static com.tapestry.rpc.ApiRegistry rpcApiRegistry;
    private static RpcDispatcher rpcDispatcher;
    private static HandshakeRegistry handshakeRegistry;
    private static RpcServerRuntime rpcServerRuntime;
    private static RpcPacketHandler rpcPacketHandler;
    private static RpcClientRuntime rpcClientRuntime;
    private static WatchRegistry watchRegistry;
    
    @Override
    public void onInitialize() {
        LOGGER.info("Tapestry server initialization started");
        
        // Only run server-specific initialization
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            try {
                // Initialize server-side components
                initializeServerComponents();
                
                LOGGER.info("Tapestry server initialization completed");
            } catch (Exception e) {
                LOGGER.error("Tapestry server initialization failed", e);
                throw new RuntimeException("Failed to initialize Tapestry server", e);
            }
        } else {
            LOGGER.warn("Tapestry server entrypoint called on client - skipping");
        }
    }
    
    /**
     * Initialize server-specific components.
     */
    private void initializeServerComponents() {
        LOGGER.info("Initializing server-specific components");
        
        // Initialize Phase 16 RPC system
        initializeRpcSystem();
        
        // Register debug commands
        registerDebugCommands();
        
        // Register server lifecycle events for additional server-specific handling
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Server starting - performing server-specific setup");
            // Additional server-side setup can be added here
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping - cleaning up server-specific resources");
            // Server cleanup can be added here
        });
        
        // Initialize server-specific services that might not be covered in TapestryMod
        initializeServerSpecificServices();
        
        // Register server-specific event handlers
        registerServerEventHandlers();
        
        LOGGER.info("Server components initialized");
    }
    
    /**
     * Registers debug commands with the command system.
     * 
     * <p>This method registers the {@code /tapestry patches <target_id>} command
     * for debugging patch operations. The command is available in both development
     * and production environments.</p>
     */
    private void registerDebugCommands() {
        LOGGER.info("Registering debug commands");
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Create a minimal PatchRegistry and PatchPlan for the debug command
            // In a full implementation, this would be provided by the patch system
            PatchRegistry registry = new PatchRegistry();
            registry.freeze();
            
            ModLoadOrder modLoadOrder = (modA, modB) -> modA.compareTo(modB);
            PatchPlan patchPlan = PatchPlan.compile(registry, modLoadOrder);
            
            // Create GameplayAPI instance and register commands
            GameplayAPI gameplayAPI = new GameplayAPI();
            gameplayAPI.registerCommands(dispatcher, registryAccess, environment, patchPlan);
            
            LOGGER.info("Debug commands registered successfully");
        });
    }
    
    /**
     * Initialize server-specific services.
     */
    private void initializeServerSpecificServices() {
        // This method can be used for services that are specifically server-only
        // and not handled in the main TapestryMod initialization
        
        LOGGER.info("Server-specific services initialized");
        
        // Examples of what could be added:
        // - Server-wide configuration management
        // - Server analytics or metrics
        // - Server-side caching systems
        // - Cross-world data synchronization
    }
    
    /**
     * Register server-specific event handlers.
     */
    private void registerServerEventHandlers() {
        LOGGER.info("Registering server-specific event handlers");
        
        // Register server tick events for server-specific periodic tasks
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started - registering server-specific tick handlers");
            
            // Register server tick handler for server-specific periodic tasks
            ServerTickEvents.END_SERVER_TICK.register(tickingServer -> {
                // Server-specific periodic tasks can be added here
                // Examples: cleanup tasks, metrics collection, etc.
            });
        });
        
        // Register server-specific world events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Register world load/unload events if needed
            LOGGER.info("Server world event handlers registered");
        });
        
        LOGGER.info("Server event handlers implemented");
    }
    
    /**
     * Phase 16: Initializes RPC system when server is ready.
     */
    private static void initializeRpcSystem() {
        LOGGER.info("=== PHASE 16: INITIALIZING RPC SYSTEM ===");
        
        try {
            // Initialize RPC system components
            handshakeRegistry = new HandshakeRegistry();
            rpcApiRegistry = new ApiRegistry();
            watchRegistry = new WatchRegistry(null); // Will be updated when server is available
            
            // Close method registration - security: no more methods can be registered
            ServerApiRegistry.closeRegistration();
            
            // Register server lifecycle to initialize dispatcher when server is available
            ServerLifecycleEvents.SERVER_STARTED.register(server -> {
                try {
                    // Initialize client-side runtime for singleplayer/integrated server
                    rpcClientRuntime = new RpcClientRuntime();
                    
                    // Create secure dispatcher
                    rpcDispatcher = new RpcDispatcher(server);
                    
                    // Create handshake handler
                    var handshakeHandler = new HandshakeHandler(handshakeRegistry, rpcDispatcher, List.of());
                    
                    // Create server runtime
                    rpcServerRuntime = new RpcServerRuntime(rpcDispatcher, handshakeRegistry);
                    
                    // Create packet handler
                    rpcPacketHandler = new RpcPacketHandler(rpcServerRuntime, handshakeHandler);
                    
                    // Register RPC payload types (Minecraft 1.20.5+ system)
                    PayloadTypeRegistry.playS2C().register(
                        RpcPayload.ID,
                        RpcPayloadCodec.CODEC
                    );
                    
                    PayloadTypeRegistry.playC2S().register(
                        RpcPayload.ID,
                        RpcPayloadCodec.CODEC
                    );
                    
                    // Register server receiver (CORRECT 1.21.11 SIGNATURE)
                    ServerPlayNetworking.registerGlobalReceiver(
                        RpcPayload.ID,
                        (payload, context) -> {
                            ServerPlayerEntity player = context.player();
                            String json = payload.json();
                            
                            // CRITICAL: Hop back to server thread
                            context.server().execute(() -> {
                                rpcPacketHandler.handle(player, json);
                            });
                        }
                    );
                    
                    LOGGER.info("RPC payload system initialized with receiver");
                    
                    // Initialize TypeScript RPC API
                    com.tapestry.typescript.RpcApi.initializeForServer(rpcApiRegistry);
                    com.tapestry.typescript.RpcApi.initializeForClient(rpcClientRuntime);
                    
                    // Set server reference for emitTo functionality
                    com.tapestry.typescript.RpcApi.setServer(server);
                    
                    // Set watch registry for watch functionality
                    com.tapestry.typescript.RpcApi.setWatchRegistry(watchRegistry);
                    
                    // Extend TypeScript runtime with Phase 16 APIs
                    var tsRuntime = com.tapestry.typescript.TypeScriptRuntime.getInstance();
                    if (tsRuntime != null) {
                        tsRuntime.extendForRpcPhase();
                    }
                    
                    // Register player disconnect hook to clean up RPC state
                    ServerPlayConnectionEvents.DISCONNECT.register((handler, serverInstance) -> {
                        var player = handler.getPlayer();
                        if (player != null) {
                            handshakeRegistry.remove(player.getUuid());
                            watchRegistry.removeAllWatches(player);
                            rpcServerRuntime.removePlayer(player);
                            LOGGER.debug("Cleaned up RPC state for disconnected player: {}", 
                                       player.getName().getString());
                        }
                    });
                    
                    LOGGER.info("Phase 16 RPC system initialized successfully");
                    
                } catch (Exception e) {
                    LOGGER.error("Failed to initialize RPC system for server {}", server.getName(), e);
                    throw new RuntimeException("RPC system initialization failed", e);
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Phase 16 RPC system", e);
            throw new RuntimeException("RPC system initialization failed", e);
        }
    }
}
