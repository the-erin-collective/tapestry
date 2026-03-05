---
title: Core Concepts
description: Understand the fundamental concepts and design principles of the Tapestry modding framework
---

# Core Concepts

Tapestry is built on several key principles that distinguish it from traditional Minecraft modding frameworks. Understanding these concepts will help you write better, more reliable mods.

## TypeScript-First Design

Tapestry treats TypeScript as a **first-class citizen**, not a scripting afterthought. Your mod code runs in a GraalVM-powered JavaScript runtime with full type safety and modern language features.

```ts
// TypeScript is the primary language for mod logic
tapestry.mod.define({
  onLoad(api) {
    // Full TypeScript support with type checking
    const result: string = api.someMethod();
    console.log(`Result: ${result}`);
  }
});
```

**Key Benefits:**
- Modern language features (async/await, destructuring, arrow functions)
- Type safety catches errors at compile time
- Familiar tooling and ecosystem
- Same runtime on both client and server

## Explicit Lifecycle Model

The most important concept in Tapestry is its **strict phase-based lifecycle**. Every operation in Tapestry happens during a specific phase, and the framework enforces these boundaries with fail-fast checks.

### Why Phases Matter

Traditional modding frameworks allow mods to run code at any time, leading to:
- Race conditions between mods
- Unpredictable initialization order
- Hard-to-debug timing issues
- Mods breaking when load order changes

Tapestry solves this by dividing startup into discrete phases with clear contracts:

```
BOOTSTRAP → DISCOVERY → VALIDATION → REGISTRATION → FREEZE →
TS_LOAD → TS_REGISTER → TS_ACTIVATE → TS_READY →
PERSISTENCE_READY → EVENT → RUNTIME → CLIENT_PRESENTATION_READY
```

Each phase has specific responsibilities and restrictions. If you try to perform an operation during the wrong phase, Tapestry throws an error immediately.

### Phase Guarantees

- **Deterministic order**: Phases always execute in the same sequence
- **No skipping**: Every phase completes before the next begins
- **Fail-fast**: Illegal operations throw errors immediately
- **Clear contracts**: Each phase documents what you can and cannot do

> 💡 **Core Principle**: If the framework ever does not know what phase it is in, it is already broken.

## Hard Boundaries

Tapestry enforces **hard boundaries** between different parts of the system. These boundaries prevent common modding mistakes:

### API Freeze

Once the `FREEZE` phase completes, the API shape is **permanently sealed**. No mod can add, remove, or modify API methods after this point.

```ts
// During REGISTRATION phase (Java extension)
ctx.registerRpcEndpoint("myapi", handler); // ✓ Allowed

// After FREEZE phase
ctx.registerRpcEndpoint("another", handler); // ✗ Throws error
```

This guarantee means:
- Mods can safely cache API references
- No runtime surprises from API changes
- Dependency resolution is deterministic

### Client/Server Separation

Tapestry provides clear flags to detect which side your code is running on:

```ts
tapestry.mod.define({
  onLoad(api) {
    if (tapestry.isClient) {
      // Client-only code (rendering, input, overlays)
      setupClientUI();
    }
    
    if (tapestry.isServer) {
      // Server-only code (world logic, persistence)
      initializeServerState();
    }
  }
});
```

**Important**: Both flags can be true in single-player mode (integrated server).

### Phase Boundaries

Operations are restricted to specific phases:

```ts
// ✓ Correct: Registering during TS_READY
tapestry.mod.define({
  onLoad(api) {
    // This runs during TS_READY phase
    api.events.on('playerJoin', handler);
  }
});

// ✗ Wrong: Trying to register too late
setTimeout(() => {
  // This runs during RUNTIME phase
  api.events.on('playerJoin', handler); // Throws error!
}, 5000);
```

## Extension System

Tapestry uses an **additive-only extension model**. Extensions are Java components that add new capabilities to the framework.

### Extension Lifecycle

1. **DISCOVERY** - Extensions are discovered via Fabric entrypoints
2. **VALIDATION** - Extensions are validated (no mutations allowed)
3. **REGISTRATION** - Extensions declare capabilities and register API domains
4. **FREEZE** - API shape is sealed

### Creating an Extension

```java
public class MyExtension implements TapestryExtensionProvider {
    @Override
    public TapestryExtensionDescriptor describe() {
        return new TapestryExtensionDescriptor(
            "myext",                    // Unique ID
            List.of("rpc", "events")    // Capabilities
        );
    }
    
    @Override
    public void register(TapestryExtensionContext ctx) {
        // Register API endpoints during REGISTRATION phase
        ctx.registerRpcEndpoint("myapi", (ctx, data) -> {
            return JsonValue.of("pong");
        });
    }
}
```

### Capability Declarations

Extensions declare their capabilities upfront:
- `rpc` - Provides remote procedure call endpoints
- `events` - Provides event handling
- `persistence` - Provides data storage
- `overlay` - Provides client UI overlays
- Custom capabilities for domain-specific features

Mods can declare dependencies on specific capabilities, and Tapestry validates these during the `TS_ACTIVATE` phase.

## Mod Structure

A Tapestry mod consists of TypeScript code that interacts with the frozen API:

```ts
// Basic mod structure
tapestry.mod.define({
  // Mod metadata
  id: "my-mod",
  version: "1.0.0",
  
  // Main entry point (runs during TS_READY)
  onLoad(api) {
    console.log("Mod loaded!");
    
    // Register event handlers
    api.events.on('tick', () => {
      // Per-tick logic
    });
  },
  
  // Cleanup hook
  onUnload(api) {
    console.log("Mod unloading...");
  }
});
```

### Mod Lifecycle

1. **TS_LOAD** - Script files are loaded but not executed
2. **TS_REGISTER** - `tapestry.mod.define()` is called, metadata collected
3. **TS_ACTIVATE** - Dependencies resolved, activation order determined
4. **TS_READY** - `onLoad()` hooks execute in dependency order

## JSON-RPC Communication

Tapestry provides a lightweight JSON-RPC transport for client-server communication:

```ts
// Server-side: Register RPC handler (Java extension)
ctx.registerRpcEndpoint("getPlayerStats", (ctx, data) -> {
    String playerName = data.get("player").asString();
    return JsonValue.of(Map.of(
        "health", 20,
        "level", 5
    ));
});

// Client-side: Call RPC endpoint (TypeScript)
const stats = await tapestry.rpc.call("getPlayerStats", {
    player: "Steve"
});
console.log(`Health: ${stats.health}`);
```

Features:
- Automatic handshake and connection management
- Rate limiting to prevent abuse
- Type-safe data serialization
- Works in both directions (client→server, server→client)

## Fail-Fast Philosophy

Tapestry embraces a **fail-fast** approach to error handling:

```ts
// Example: Trying to use API before it's ready
tapestry.mod.define({
  onLoad(api) {
    // ✓ Correct: API is ready during TS_READY
    api.events.on('tick', handler);
  }
});

// ✗ Wrong: Trying to use API too early
api.events.on('tick', handler); // Throws: "API not available in current phase"
```

**Benefits:**
- Bugs are caught immediately, not hours later
- Clear error messages point to the exact problem
- No silent failures or undefined behavior
- Easier debugging and testing

## Read-Only API

The TypeScript API is **frozen and read-only**. Mods cannot modify the API object:

```ts
tapestry.mod.define({
  onLoad(api) {
    // ✓ Allowed: Reading API
    const version = api.version;
    
    // ✗ Forbidden: Modifying API
    api.newMethod = () => {}; // Throws error
    delete api.events;        // Throws error
  }
});
```

This prevents mods from:
- Interfering with each other
- Breaking the API for other mods
- Creating hidden dependencies

## Next Steps

Now that you understand Tapestry's core concepts, explore these topics:

- [Lifecycle Phases](/guide/lifecycle-phases) - Detailed phase documentation
- [Extension System](/guide/extensions) - Building Java extensions
- [Architecture](/guide/architecture) - System design and data flows
- [API Reference](/api/) - Complete API documentation

## Key Takeaways

1. **TypeScript is first-class** - Not a scripting layer, but the primary mod language
2. **Phases are enforced** - Every operation happens at the right time or fails
3. **Boundaries are hard** - API freeze, client/server separation, phase restrictions
4. **Extensions are additive** - New capabilities are added, never removed
5. **Fail-fast is good** - Immediate errors are better than silent failures
6. **API is read-only** - Mods cannot modify the framework itself
