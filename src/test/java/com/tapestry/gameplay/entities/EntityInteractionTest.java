package com.tapestry.gameplay.entities;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntityInteractionTest {

    @BeforeEach
    void resetRegistry() {
        EntityInteractionHookRegistry registry = EntityInteractionHookRegistry.getInstance();
        registry.clear();
        registry.allowRegistration();
    }

    @Test
    void testRegistrationAllowedAndInvocation_accept() {
        EntityInteractionHookRegistry registry = EntityInteractionHookRegistry.getInstance();
        registry.registerFeedAttemptHandler("minecraft:cat", ctx -> ctx.accept());

        FeedAttemptContext context = new FeedAttemptContext(
                new FeedAttemptContext.ItemInfo("minecraft:fish", 1),
                new FeedAttemptContext.EntityInfo("minecraft:cat", "uuid")
        );

        registry.invokeFeedAttemptHandlers("minecraft:cat", context);
        assertEquals(FeedAttemptContext.FeedResult.ACCEPTED, context.getResult());
    }

    @Test
    void testInvocation_reject() {
        EntityInteractionHookRegistry registry = EntityInteractionHookRegistry.getInstance();
        registry.registerFeedAttemptHandler("minecraft:cat", ctx -> ctx.reject());

        FeedAttemptContext context = new FeedAttemptContext(
                new FeedAttemptContext.ItemInfo("minecraft:fish", 1),
                new FeedAttemptContext.EntityInfo("minecraft:cat", "uuid")
        );

        registry.invokeFeedAttemptHandlers("minecraft:cat", context);
        assertEquals(FeedAttemptContext.FeedResult.REJECTED, context.getResult());
    }

    @Test
    void testUnknownStaysUnknown() {
        EntityInteractionHookRegistry registry = EntityInteractionHookRegistry.getInstance();
        // no handlers registered
        FeedAttemptContext context = new FeedAttemptContext(
                new FeedAttemptContext.ItemInfo("minecraft:fish", 1),
                new FeedAttemptContext.EntityInfo("minecraft:cat", "uuid")
        );

        registry.invokeFeedAttemptHandlers("minecraft:cat", context);
        assertEquals(FeedAttemptContext.FeedResult.UNKNOWN, context.getResult());
    }

    @Test
    void testRegistrationDisallowedThrows() {
        EntityInteractionHookRegistry registry = EntityInteractionHookRegistry.getInstance();
        registry.disallowRegistration();
        assertThrows(IllegalStateException.class, () -> {
            registry.registerFeedAttemptHandler("minecraft:cat", ctx -> {});
        });
    }

    @Test
    void testEntitiesApiRegistersHandler() {
        // use Graal context to simulate Value function
        Context ctx = Context.newBuilder("js").allowAllAccess(true).build();
        EntitiesApi api = new EntitiesApi();
        ProxyObject namespace = api.createNamespace();
        assertTrue(namespace.hasMember("onFeedAttempt"));
        Value func = ctx.asValue(namespace.getMember("onFeedAttempt"));
        assertTrue(func.canExecute());

        // call the function with entity type and a JS function
        Value handler = ctx.eval("js", "(ctx) => { ctx.accept(); }");
        func.execute("minecraft:cat", handler);

        // ensure registry got a handler and it behaves
        EntityInteractionHookRegistry registry = EntityInteractionHookRegistry.getInstance();
        FeedAttemptContext context = new FeedAttemptContext(
                new FeedAttemptContext.ItemInfo("minecraft:fish", 1),
                new FeedAttemptContext.EntityInfo("minecraft:cat", "uuid")
        );
        registry.invokeFeedAttemptHandlers("minecraft:cat", context);
        assertEquals(FeedAttemptContext.FeedResult.ACCEPTED, context.getResult());
    }
}
