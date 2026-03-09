package com.tapestry.gameplay.patch;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.loot.LootTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.tapestry.gameplay.trades.TradeAPI;
import com.tapestry.gameplay.loot.LootAPI;

/**
 * Helper responsible for compiling the patch plan and reapplying patches
 * when the game loads or reloads datapacks.
 *
 * <p>This class is initialized by the framework immediately after the
 * TS_REGISTER phase completes.  It freezes the {@link PatchRegistry},
 * compiles a {@link PatchPlan} and installs a resource reload listener that
 * reapplies patches each time datapacks are reloaded.  Domains which want to
 * participate in patching (for example loot tables or trade tables) may
 * register handlers which know how to locate the concrete object for a
 * given {@link PatchTarget}.</p>
 */
public final class PatchLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatchLifecycleManager.class);

    private static PatchPlan plan;
    private static PatchEngine engine;

    // map from target type to handler that knows how to apply patches for that type
    private static final Map<Class<?>, PatchReapplicationHandler<?>> handlers = new HashMap<>();

    private PatchLifecycleManager() {
        // utility class
    }

    /**
     * Initialize the patch lifecycle machinery.  Should be called exactly once
     * after TS_REGISTER phase has finished and all patch sets have been
     * registered (the registry will be frozen by this method).
     *
     * @param context runtime context used when evaluating conditions
     * @param config  configuration values that drive engine behaviour
     */
    public static void initialize(PatchContext context, Map<String, Object> config) {
        if (plan != null) {
            LOGGER.warn("PatchLifecycleManager.initialize() called more than once");
            return;
        }

        // freeze registry so no further registrations are allowed
        PatchRegistry registry = PatchRegistry.getInstance();
        registry.freeze();

        // set up API helpers for mods that register builder functions during TS_REGISTER
        TradeAPI.setRegistry(registry);
        LootAPI.setRegistry(registry);
        // future domains can also call PatchLifecycleManager.registerHandler(...) themselves

        // compile plan using simple alphabetical mod-load order by default
        plan = PatchPlan.compile(registry, (a, b) -> a.compareTo(b));
        engine = new PatchEngine(plan, context, config == null ? Collections.emptyMap() : config);

        LOGGER.info("Compiled patch plan for {} targets", plan.getAllTargets().size());

        // allow domains to register handlers if desired
        registerDefaultHandlers();

        // apply patches once immediately as part of bootstrap
        applyAllPatches(context);

        // listen for datapack reloads (may not be available in unit tests)
        try {
            ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of("tapestry", "patch_engine_reload");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        LOGGER.info("Datapack reload detected - reapplying patches");
                        applyAllPatches(context);
                    }
                });
        } catch (Throwable t) {
            // ignore environments without Fabric runtime (unit tests)
            LOGGER.debug("Failed to register reload listener, continuing anyway", t);
        }

        LOGGER.info("PatchLifecycleManager initialized and reload listener registered");
    }

    private static void registerDefaultHandlers() {
        // currently there are no built-in handlers.  Gameplay domains such as
        // loot tables and trade tables install their own event-driven patch
        // application logic elsewhere (e.g. FabricLootRegistry).  This method
        // exists primarily to document the extension point and may be removed
        // in the future if it becomes unnecessary.
    }

    /**
     * Registers a handler which knows how to apply patches for a particular
     * gameplay data type.
     *
     * @param type    class of the gameplay data objects
     * @param handler handler that will be invoked for each target of that type
     * @param <T>     generic data type
     */
    public static <T> void registerHandler(Class<T> type, PatchReapplicationHandler<T> handler) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");
        handlers.put(type, handler);
    }

    /**
     * Applies all patches stored in the compiled plan using the given context.
     * Unhandled target types are logged and ignored.
     *
     * @param context current patch context
     */
    public static void applyAllPatches(PatchContext context) {
        if (plan == null || engine == null) {
            LOGGER.warn("PatchLifecycleManager not initialized - skipping patch application");
            return;
        }
        for (PatchTarget<?> target : plan.getAllTargets().keySet()) {
            @SuppressWarnings("unchecked")
            PatchReapplicationHandler<Object> handler = (PatchReapplicationHandler<Object>) handlers.get(target.type());
            if (handler != null) {
                handler.apply(engine, context, (PatchTarget<Object>) target);
            } else {
                LOGGER.debug("No handler registered for patch target type {} (id={}), skipping",
                        target.type().getSimpleName(), target.id());
            }
        }
    }

    /**
     * Returns the patch engine instance, if initialized.  Primarily useful for
     * domain code that wants to perform ad-hoc patching.
     */
    public static PatchEngine getEngine() {
        return engine;
    }

    /**
     * Resets internal state for testing. Not for production use.
     */
    public static void reset() {
        plan = null;
        engine = null;
        handlers.clear();
    }

    /**
     * Functional interface that applies patches for a single target.
     *
     * @param <T> type of the target data
     */
    @FunctionalInterface
    public interface PatchReapplicationHandler<T> {
        void apply(PatchEngine engine, PatchContext context, PatchTarget<T> target);
    }
}
