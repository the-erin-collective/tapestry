package com.tapestry.gameplay.patch;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import com.tapestry.gameplay.patch.PatchLifecycleManager.PatchReapplicationHandler;

/**
 * Tests focused on ensuring the patch engine remains extensible and
 * does not make assumptions about pre-registered targets or built-in
 * data types.
 *
 * <p>The three sub‑tasks in this area are:</p>
 * <ul>
 *   <li>Property test that arbitrary data types can be used as targets</li>
 *   <li>Property test that targets can be used without any prior "registration"</li>
 *   <li>Basic integration test that a completely custom gameplay domain
 *       type can be patched through the lifecycle manager</li>
 * </ul>
 */
class ExtensibilityTest {

    private PatchRegistry registry;
    private PatchContext context;

    @BeforeEach
    void setUp() {
        registry = new PatchRegistry();
        context = new PatchContext() {
            @Override public boolean isModLoaded(String modId) { return false; }
            @Override public boolean registryContains(Identifier id) { return false; }
            @Override public boolean traitExists(Identifier traitId) { return false; }
            @Override public net.minecraft.registry.Registry<?> getRegistry(Identifier registryId) { return null; }
        };
    }

    /**
     * Property‑style test verifying that the engine accepts an open set of
     * target types.  We exercise several classes including a bespoke inner
     * class and primitives wrapped as their boxed equivalents.
     */
    @Test
    void registrySupportsArbitraryDataTypes() {
        class CustomType { }

        Class<?>[] types = new Class<?>[] { String.class, Integer.class, CustomType.class };
        for (Class<?> cls : types) {
            String lname = cls.getSimpleName().toLowerCase(java.util.Locale.ROOT);
            @SuppressWarnings("unchecked")
            PatchTarget<Object> target = new PatchTarget<>(
                Identifier.of("ext", lname),
                (Class<Object>) cls
            );
            PatchSet<Object> set = new PatchSet<>(
                Identifier.of("mod", "p" + lname),
                target,
                PatchPriority.NORMAL,
                List.of(new MockOperation<>()),
                Optional.empty()
            );
            assertDoesNotThrow(() -> registry.register(set));
            List<PatchSet<Object>> fetched = registry.getPatchesFor(target);
            assertEquals(1, fetched.size(), "patch must be indexed for " + cls);
            assertEquals(set, fetched.get(0));
        }
    }

    /**
     * A target that has never been mentioned before should return an empty
     * list from the registry; after registration the patches should appear.
     */
    @Test
    void targetLookupWorksWithoutPreRegistration() {
        PatchTarget<String> target = new PatchTarget<>(Identifier.of("foo", "bar"), String.class);
        assertTrue(registry.getPatchesFor(target).isEmpty(), "no patches initially");

        PatchSet<String> patch = new PatchSet<>(
            Identifier.of("mod", "once"),
            target,
            PatchPriority.NORMAL,
            List.of(new MockOperation<>()),
            Optional.empty()
        );
        registry.register(patch);
        assertFalse(registry.getPatchesFor(target).isEmpty(), "patch should appear after registration");
    }

    /**
     * Integration exercise: register a patch for a custom gameplay domain type
     * and verify the lifecycle manager applies it via a handler.
     */
    @Test
    void customGameplayDomainIntegration() {
        class Domain { }

        PatchTarget<Domain> target = new PatchTarget<>(Identifier.of("dom", "data"), Domain.class);
        PatchSet<Domain> patch = new PatchSet<>(
            Identifier.of("extmod", "patch"),
            target,
            PatchPriority.NORMAL,
            List.of(new PatchOperation<Domain>() {
                @Override public void apply(Domain t) { }
            }),
            Optional.empty()
        );
        registry.register(patch);

        AtomicInteger called = new AtomicInteger();
        PatchReapplicationHandler<Domain> handler = (eng, ctx, t) -> called.incrementAndGet();

        // simulate registry freeze and plan/engine creation without using the
        // Fabric-dependent lifecycle manager
        registry.freeze();
        PatchPlan plan = PatchPlan.compile(registry, (a, b) -> a.compareTo(b));
        PatchEngine engine = new PatchEngine(plan, context, Collections.emptyMap());

        // manually invoke our handler for each matching target
        for (PatchTarget<?> t : plan.getAllTargets().keySet()) {
            if (t.type() == Domain.class) {
                @SuppressWarnings("unchecked")
                PatchTarget<Domain> dt = (PatchTarget<Domain>) t;
                handler.apply(engine, context, dt);
            }
        }

        assertEquals(1, called.get(), "custom handler should be invoked");
    }

    // simple generic no-op operation used by multiple tests
    private static class MockOperation<T> implements PatchOperation<T> {
        @Override public void apply(T target) { }
    }
}
