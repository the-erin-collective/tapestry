package com.tapestry.typescript;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.RaycastContext;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side player API for TypeScript mods.
 * Provides client-side only player functions that don't require server round-trips.
 */
public class ClientPlayersApi {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientPlayersApi.class);
    
    /**
     * Creates the client players namespace for TypeScript.
     * 
     * @return ProxyObject containing all client-side player APIs
     */
    public ProxyObject createNamespace() {
        Map<String, Object> players = new HashMap<>();
        
        // Client-side raycasting - no server required
        players.put("raycastBlock", createRaycastBlockFunction());
        
        // Client-side player info
        players.put("getPosition", createGetPositionFunction());
        players.put("getLook", createGetLookFunction());
        
        return ProxyObject.fromMap(players);
    }
    
    /**
     * Creates the client-side raycastBlock function.
     * Performs raycasting on the client world without server communication.
     */
    private ProxyExecutable createRaycastBlockFunction() {
        return args -> {
            try {
                // Get the Minecraft client instance
                MinecraftClient client = MinecraftClient.getInstance();
                ClientPlayerEntity player = client.player;
                ClientWorld world = client.world;
                
                if (player == null || world == null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("hit", false);
                    return ProxyObject.fromMap(result);
                }
                
                // Parse options from arguments
                double maxDistance = 5.0;
                boolean includeFluids = false;
                
                if (args.length > 0 && args[0].hasMembers()) {
                    Value options = args[0];
                    if (options.hasMember("maxDistance")) {
                        maxDistance = options.getMember("maxDistance").asDouble();
                    }
                    if (options.hasMember("includeFluids")) {
                        includeFluids = options.getMember("includeFluids").asBoolean();
                    }
                }
                
                // Enforce safety limits
                if (maxDistance <= 0 || maxDistance > 32.0) {
                    throw new IllegalArgumentException("maxDistance must be between 0 and 32");
                }
                
                // Perform raycast
                Vec3d start = player.getCameraPosVec(1.0F);
                Vec3d direction = player.getRotationVec(1.0F);
                Vec3d end = start.add(direction.multiply(maxDistance));
                
                RaycastContext context = new RaycastContext(
                    start,
                    end,
                    RaycastContext.ShapeType.OUTLINE,
                    includeFluids 
                        ? RaycastContext.FluidHandling.ANY 
                        : RaycastContext.FluidHandling.NONE,
                    player
                );
                
                HitResult hitResult = world.raycast(context);
                
                Map<String, Object> result = new HashMap<>();
                
                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hitResult;
                    BlockPos pos = blockHit.getBlockPos();
                    BlockState state = world.getBlockState(pos);
                    Identifier blockId = Registries.BLOCK.getId(state.getBlock());
                    
                    result.put("hit", true);
                    
                    Map<String, Object> blockPos = new HashMap<>();
                    blockPos.put("x", pos.getX());
                    blockPos.put("y", pos.getY());
                    blockPos.put("z", pos.getZ());
                    result.put("blockPos", ProxyObject.fromMap(blockPos));
                    
                    result.put("blockId", blockId.toString());
                    result.put("blockName", state.getBlock().getName().getString());
                    
                    Direction side = blockHit.getSide();
                    result.put("side", side.asString());
                } else {
                    result.put("hit", false);
                }
                
                return ProxyObject.fromMap(result);
                
            } catch (Exception e) {
                LOGGER.error("Client-side raycastBlock failed", e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("hit", false);
                errorResult.put("error", e.getMessage());
                return ProxyObject.fromMap(errorResult);
            }
        };
    }
    
    /**
     * Creates the client-side getPosition function.
     */
    private ProxyExecutable createGetPositionFunction() {
        return args -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                ClientPlayerEntity player = client.player;
                
                if (player == null) {
                    return null;
                }
                
                Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
                Map<String, Object> result = new HashMap<>();
                result.put("x", pos.x);
                result.put("y", pos.y);
                result.put("z", pos.z);
                
                return ProxyObject.fromMap(result);
                
            } catch (Exception e) {
                LOGGER.error("Client-side getPosition failed", e);
                return null;
            }
        };
    }
    
    /**
     * Creates the client-side getLook function.
     */
    private ProxyExecutable createGetLookFunction() {
        return args -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                ClientPlayerEntity player = client.player;
                
                if (player == null) {
                    return null;
                }
                
                Vec3d dir = player.getRotationVec(1.0F);
                Map<String, Object> result = new HashMap<>();
                result.put("yaw", player.getYaw());
                result.put("pitch", player.getPitch());
                
                Map<String, Object> direction = new HashMap<>();
                direction.put("x", dir.x);
                direction.put("y", dir.y);
                direction.put("z", dir.z);
                result.put("dir", ProxyObject.fromMap(direction));
                
                return ProxyObject.fromMap(result);
                
            } catch (Exception e) {
                LOGGER.error("Client-side getLook failed", e);
                return null;
            }
        };
    }
}
