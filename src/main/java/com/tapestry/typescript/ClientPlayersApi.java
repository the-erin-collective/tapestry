package com.tapestry.typescript;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
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
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
                Map<String, Object> result = executeRaycastOnClientThread(maxDistance, includeFluids);
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
     * Runs raycast on Minecraft's client thread and returns plain Java data.
     * This avoids stale/off-thread camera reads when called from TWILA-JS thread.
     */
    private Map<String, Object> executeRaycastOnClientThread(double maxDistance, boolean includeFluids) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) {
            return performRaycast(client, maxDistance, includeFluids);
        }

        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        client.execute(() -> {
            try {
                future.complete(performRaycast(client, maxDistance, includeFluids));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future.get(250, TimeUnit.MILLISECONDS);
    }

    /**
     * Performs actual raycast using current client-thread player/camera state.
     */
    private Map<String, Object> performRaycast(MinecraftClient client, double maxDistance, boolean includeFluids) {
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;

        Map<String, Object> result = new HashMap<>();
        if (player == null || world == null) {
            result.put("hit", false);
            return result;
        }

        HitResult crosshairTarget = client.crosshairTarget;
        if (crosshairTarget instanceof EntityHitResult entityHitResult) {
            double maxDistanceSq = maxDistance * maxDistance;
            if (player.getCameraPosVec(1.0F).squaredDistanceTo(crosshairTarget.getPos()) <= maxDistanceSq) {
                Entity entity = entityHitResult.getEntity();
                Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());

                result.put("hit", true);
                result.put("targetType", "entity");
                result.put("entityId", entityId.toString());
                result.put("entityName", entity.getType().getName().getString());

                Map<String, Object> entityPos = new HashMap<>();
                entityPos.put("x", entity.getX());
                entityPos.put("y", entity.getY());
                entityPos.put("z", entity.getZ());
                result.put("entityPos", entityPos);

                return result;
            }
        }

        Vec3d start = player.getCameraPosVec(1.0F);

        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(maxDistance));

        // Detect water surface when above water, but ignore fluid faces while in water.
        boolean inWater = player.isSubmergedInWater() || player.isTouchingWater();
        RaycastContext.FluidHandling fluidHandling = includeFluids && !inWater
            ? RaycastContext.FluidHandling.ANY
            : RaycastContext.FluidHandling.NONE;

        RaycastContext context = new RaycastContext(
            start,
            end,
            RaycastContext.ShapeType.OUTLINE,
            fluidHandling,
            player
        );

        BlockHitResult blockHit = world.raycast(context);
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Identifier blockId = Registries.BLOCK.getId(state.getBlock());
            FluidState fluidState = world.getFluidState(pos);

            result.put("hit", true);

            Map<String, Object> blockPos = new HashMap<>();
            blockPos.put("x", pos.getX());
            blockPos.put("y", pos.getY());
            blockPos.put("z", pos.getZ());
            result.put("blockPos", blockPos);

            Identifier fluidId = Registries.FLUID.getId(fluidState.getFluid());
            boolean isFluidTarget = includeFluids
                && !inWater
                && fluidState != null
                && !fluidState.isEmpty()
                && fluidId != null
                && !"minecraft:empty".equals(fluidId.toString());

            if (isFluidTarget) {
                result.put("targetType", "fluid");
                result.put("blockId", fluidId.toString());
                result.put("blockName", toDisplayName(fluidId));
            } else {
                result.put("targetType", "block");
                result.put("blockId", blockId.toString());
                result.put("blockName", state.getBlock().getName().getString());
            }

            Direction side = blockHit.getSide();
            result.put("side", side.asString());
        } else {
            result.put("hit", false);
        }

        return result;
    }

    private String toDisplayName(Identifier id) {
        String path = id.getPath().replace('_', ' ');
        String[] words = path.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) {
                continue;
            }
            String normalized = word.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(normalized.charAt(0)));
            if (normalized.length() > 1) {
                builder.append(normalized.substring(1));
            }
            if (i < words.length - 1) {
                builder.append(' ');
            }
        }
        return builder.toString().trim();
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
