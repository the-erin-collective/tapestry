package com.tapestry.gameplay.patch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.util.Identifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for {@link PatchLifecycleManager}.  These tests focus on
 * the lifecycle plumbing rather than the underlying patch engine which is
 * covered by {@link PatchEngineTest} and friends.
 */
class PatchLifecycleManagerTest {

    // tests use the singleton instance provided by PatchRegistry
    // so changes affect the same registry used by the manager
    private PatchRegistry registry;
    private PatchContext context;

    @BeforeEach
    void setUp() {
        // ensure a fresh singleton for each test
        PatchRegistry.reset();
        registry = PatchRegistry.getInstance();
        // also clear lifecycle manager so previous tests cannot interfere
        PatchLifecycleManager.reset();
        context = new PatchContext() {
            @Override
            public boolean isModLoaded(String modId) {
                return false;
            }

            @Override
            public boolean registryContains(net.minecraft.util.Identifier id) {
                return false;
            }

            @Override
            public boolean traitExists(net.minecraft.util.Identifier traitId) {
                return false;
            }

            @Override
            public net.minecraft.registry.Registry<?> getRegistry(net.minecraft.util.Identifier registryId) {
                return null;
            }
        };
    }

    @Test
    void initialize_canBeCalledOnceAndPlanCompiles() {
        // avoid potential Fabric reload listener issues by catching any runtime errors
        assertDoesNotThrow(() -> PatchLifecycleManager.initialize(context, Collections.emptyMap()));
        // second call should just log a warning rather than throwing
        assertDoesNotThrow(() -> PatchLifecycleManager.initialize(context, Collections.emptyMap()));
        assertNotNull(PatchLifecycleManager.getEngine());
    }

    @Test
    void applyAllPatches_reappliesOnMultipleInvocations() {
        // register a dummy patch set for a made-up target type on the global registry
        PatchTarget<String> target = new PatchTarget<>(Identifier.of("foo", "bar"), String.class);
        PatchSet<String> set = new PatchSet<>(
            Identifier.of("modid", "dummy"),
            target,
            0,
            List.of(new PatchOperation<String>() { @Override public void apply(String t) {} }),
            Optional.empty()
        );
        registry.register(set);

        // counters increments when handler is invoked
        AtomicInteger invocations = new AtomicInteger();
        PatchLifecycleManager.registerHandler(String.class, (eng, ctx, t) -> invocations.incrementAndGet());

        // initialize with our registry via reflection hack: freeze registry then compile
        // uses internal registry -- we simulate by manually freezing and compiling
        registry.freeze();
        PatchLifecycleManager.initialize(context, Collections.emptyMap());

        // initialization already applies patches once
        assertEquals(1, invocations.get(), "handler should have been called once during initialization");

        // simulate a datapack reload by invoking applyAllPatches again
        PatchLifecycleManager.applyAllPatches(context);
        assertEquals(2, invocations.get(), "handler should be invoked again during reload");
    }
}
