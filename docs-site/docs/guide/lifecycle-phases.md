---
title: Lifecycle Phases
description: Detailed documentation of Tapestry's phase-based lifecycle model and phase contracts
---

# Lifecycle Phases

Tapestry's most distinctive feature is its **strict phase-based lifecycle model**. Every operation in Tapestry happens during a specific phase, and the framework enforces these boundaries with fail-fast checks. This guide provides detailed documentation of each phase, what operations are allowed, and why the phase model matters.

## Why Phases Matter

Traditional modding frameworks allow mods to run code at any time, leading to several critical problems:

- **Race conditions**: Mods interfere with each other based on load order
- **Unpredictable initialization**: No guarantees about when things are ready
- **Hard-to-debug timing issues**: Bugs appear only with specific mod combinations
- **Fragile load order dependencies**: Changing load order breaks mods

Tapestry solves these problems by dividing the boot sequence into discrete phases with clear contracts. Each phase has:

- **Specific responsibilities**: What the framework does during this phase
- **Allowed operations**: What mods and extensions can do
- **Forbidden operations**: What will throw errors if attempted
- **Guarantees**: What you can rely on after this phase completes

> 💡 **Core Principle**: If the framework ever does not know what phase it is in, it is already broken.

## Phase Progression

Tapestry progresses through phases in strict order. Phases cannot be skipped, and the framework cannot move backward. Each phase completes fully before the next begins.

```text
BOOTSTRAP → DISCOVERY → VALIDATION → REGISTRATION → FREEZE →
TS_LOAD → TS_REGISTER → TS_ACTIVATE → TS_READY →
PERSISTENCE_READY → EVENT → RUNTIME → CLIENT_PRESENTATION_READY
```

![Tapestry Lifecycle Phases Diagram](/diagrams/lifecycle.svg)
*Figure 1: Tapestry's lifecycle phase progression showing the strict sequential flow from BOOTSTRAP through CLIENT_PRESENTATION_READY, with each phase having specific responsibilities and allowed operations.*

## Phase Reference

### BOOTSTRAP

**Purpose**: Core initialization

**What Happens**:
- Instantiate core fields (API registry, TypeScript runtime, mod registry)
- Set up basic infrastructure
- Initialize phase controller

**Allowed Operations**:
- Internal framework initialization only

**Forbidden Operations**:
- Extension registration
- API calls
- Mod operations

**Guarantees After This Phase**:
- Core framework objects exist
- Phase controller is operational
- Ready to discover extensions

---

### DISCOVERY

**Purpose**: Extension scanning

**What Happens**:
- Register Fabric lifecycle hooks
- Discover extension providers via Fabric entrypoints
- Collect extension descriptors

**Allowed Operations**:
- Extension providers can be discovered
- Extension descriptors can be created

**Forbidden Operations**:
- Extension registration (happens in REGISTRATION phase)
- API calls
- Mod operations

**Guarantees After This Phase**:
- All extensions have been discovered
- Extension descriptors are available
- Ready for validation

**Example**:
```java
// Extension provider discovered during DISCOVERY
public class MyExtension implements TapestryExtensionProvider {
    @Override
    public TapestryExtensionDescriptor describe() {
        return new TapestryExtensionDescriptor(
            "myext",
            List.of("rpc", "events")
        );
    }
}
```

---

### VALIDATION

**Purpose**: Extension validation

**What Happens**:
- Validate extension descriptors
- Check dependencies and capabilities
- Freeze capability and type registries
- Ensure no duplicate extension IDs

**Allowed Operations**:
- Extension descriptor validation
- Dependency checking

**Forbidden Operations**:
- Extension registration
- API calls
- Mod operations

**Guarantees After This Phase**:
- All extensions are valid
- No duplicate extension IDs
- Dependencies are satisfied
- Capability registry is frozen
- Ready for registration

---

### REGISTRATION

**Purpose**: API domain declaration

**What Happens**:
- Extensions register their APIs, hooks, and services
- Build extension registries
- Populate API tree with extension-provided functions

**Allowed Operations**:
- Extensions can register API endpoints
- Extensions can register hooks
- Extensions can register services

**Forbidden Operations**:
- Modifying frozen registries
- Calling registered APIs
- Mod operations

**Guarantees After This Phase**:
- All extension APIs are registered
- API tree is populated
- Ready to freeze API shape

**Example**:
```java
// Extension registers API during REGISTRATION
@Override
public void register(TapestryExtensionContext ctx) {
    ctx.registerRpcEndpoint("myapi", (ctx, data) -> {
        return JsonValue.of("pong");
    });
}
```

---

### FREEZE

**Purpose**: API shape is sealed

**What Happens**:
- Freeze all registries (API, hook, service)
- Make API tree immutable
- Lock down extension capabilities

**Allowed Operations**:
- Internal freezing operations only

**Forbidden Operations**:
- Registering new APIs
- Modifying API tree
- Calling APIs (not yet available to mods)

**Guarantees After This Phase**:
- API shape is permanently sealed
- No more APIs can be added or removed
- API tree is immutable
- Mods can safely cache API references
- Ready to initialize TypeScript runtime

> ⚠️ **Critical**: After FREEZE, the API cannot be modified. This guarantee enables deterministic dependency resolution.

---

### TS_LOAD

**Purpose**: GraalVM starts, scripts loaded

**What Happens**:
- Initialize TypeScript runtime for mod loading
- Create GraalVM JavaScript context
- Discover TypeScript mod files from filesystem and classpath
- Inject `tapestry` namespace (but not yet fully populated)

**Allowed Operations**:
- Mod file discovery
- GraalVM context initialization

**Forbidden Operations**:
- Executing mod scripts (happens in TS_REGISTER)
- Calling APIs
- Mod registration

**Guarantees After This Phase**:
- TypeScript runtime is initialized
- All mod files have been discovered
- GraalVM context is ready
- Ready to register mods

---

### TS_REGISTER

**Purpose**: Mod registration and metadata collection

**What Happens**:
- Evaluate mod scripts in GraalVM context
- Collect mod descriptors via `tapestry.mod.define()` calls
- Validate mod dependencies
- Build activation order based on dependency graph

**Allowed Operations**:
- Mods can call `tapestry.mod.define()` to register
- Mods can declare dependencies
- Mods can declare exports

**Forbidden Operations**:
- Calling `tapestry.mod.require()` (happens in TS_ACTIVATE)
- Calling `tapestry.mod.export()` (happens in TS_ACTIVATE)
- Registering event handlers
- Calling game APIs

**Guarantees After This Phase**:
- All mods are registered
- Mod metadata is collected
- Dependency graph is validated
- Activation order is determined
- Ready to activate mods

**Example**:
```ts
// Mod registers during TS_REGISTER
tapestry.mod.define({
  id: "my-mod",
  version: "1.0.0",
  depends: ["core-lib"],
  
  onLoad(api) {
    // This doesn't run yet - just stored for TS_READY
    console.log("Mod loaded!");
  }
});
```

---

### TS_ACTIVATE

**Purpose**: Dependency resolution

**What Happens**:
- Activate mods in dependency order
- Process `tapestry.mod.export()` calls
- Process `tapestry.mod.require()` calls
- Resolve inter-mod dependencies

**Allowed Operations**:
- Mods can call `tapestry.mod.export()` to expose values
- Mods can call `tapestry.mod.require()` to import from dependencies

**Forbidden Operations**:
- Registering new mods
- Calling game APIs
- Registering event handlers

**Guarantees After This Phase**:
- All mods are activated in dependency order
- All exports are available
- All requires are resolved
- Ready to execute mod onLoad hooks

**Example**:
```ts
// Mod A exports during TS_ACTIVATE
tapestry.mod.export("utilityFunction", (x) => x * 2);

// Mod B requires during TS_ACTIVATE (after Mod A)
const util = tapestry.mod.require("mod-a", "utilityFunction");
console.log(util(5)); // 10
```

---

### TS_READY

**Purpose**: `onLoad` hooks execute

**What Happens**:
- Execute all mod `onLoad` handlers in dependency order
- Allow hook registration
- Populate `tapestry` namespace with full API

**Allowed Operations**:
- Mods can register event handlers
- Mods can access the full API
- Mods can set up initial state

**Forbidden Operations**:
- Registering new mods
- Modifying API shape
- Accessing persistence (not ready yet)

**Guarantees After This Phase**:
- All mod `onLoad` hooks have executed
- All event handlers are registered
- Mods are fully initialized
- Ready for persistence initialization

**Example**:
```ts
tapestry.mod.define({
  id: "my-mod",
  
  onLoad(api) {
    // This runs during TS_READY
    console.log("Mod is ready!");
    
    // Can register event handlers
    api.events.on('tick', () => {
      // Per-tick logic
    });
  }
});
```

---

### PERSISTENCE_READY

**Purpose**: Persistence backend ready

**What Happens**:
- Initialize persistence service
- Enable per-mod state stores
- Connect to storage backend

**Allowed Operations**:
- Mods can access `tapestry.mod.state` API
- Mods can read and write persistent data

**Forbidden Operations**:
- Modifying API shape
- Registering new mods

**Guarantees After This Phase**:
- Persistence backend is initialized
- Mod state stores are available
- Data can be persisted to disk
- Ready for event system initialization

**Example**:
```ts
// After PERSISTENCE_READY, mods can use state API
tapestry.mod.state.set("playerScore", 100);
const score = tapestry.mod.state.get("playerScore");
```

---

### EVENT

**Purpose**: Event system ready

**What Happens**:
- Extend runtime with event APIs
- Initialize event bus
- Enable event dispatch

**Allowed Operations**:
- Mods can emit events
- Mods can listen to events

**Forbidden Operations**:
- Modifying API shape
- Registering new mods

**Guarantees After This Phase**:
- Event system is fully operational
- Events can be emitted and received
- Ready for runtime initialization

---

### RUNTIME

**Purpose**: Game logic and events active

**What Happens**:
- Initialize runtime services (scheduler, event bus, config, state)
- Extend runtime with gameplay APIs (players, RPC, etc.)
- Connect to Minecraft server lifecycle events

**Allowed Operations**:
- Mods can interact with game world
- Mods can use RPC system
- Mods can access player data
- Mods can schedule tasks

**Forbidden Operations**:
- Modifying API shape
- Registering new mods
- Registering overlays (client only, happens in CLIENT_PRESENTATION_READY)

**Guarantees After This Phase**:
- Full game API is available
- RPC system is operational
- Scheduler is running
- Server lifecycle events are connected
- Ready for client presentation (if on client)

**Example**:
```ts
// During RUNTIME, full game API is available
tapestry.mod.define({
  onLoad(api) {
    api.events.on('playerJoin', (player) => {
      console.log(`${player.name} joined the game`);
    });
    
    // Schedule a task
    api.scheduler.runLater(() => {
      console.log("5 seconds later...");
    }, 100); // 100 ticks = 5 seconds
  }
});
```

---

### CLIENT_PRESENTATION_READY

**Purpose**: Client UI ready (client-side only)

**What Happens**:
- Extend runtime with client presentation APIs
- Enable overlay registration
- Initialize overlay renderer

**Allowed Operations**:
- Mods can register UI overlays
- Mods can render custom UI elements

**Forbidden Operations**:
- Modifying API shape
- Registering new mods

**Guarantees After This Phase**:
- Client presentation API is available
- Overlays can be registered and rendered
- Full client-side functionality is operational

**Example**:
```ts
// Register overlay during CLIENT_PRESENTATION_READY
if (tapestry.isClient) {
  tapestry.mod.define({
    onLoad(api) {
      api.overlay.register({
        id: "my-hud",
        anchor: "top-left",
        render: (ctx) => {
          ctx.drawText("Hello, world!", 10, 10);
        }
      });
    }
  });
}
```

## Phase Guards

Tapestry provides phase guard functions to enforce phase requirements:

### requirePhase(phase)

Requires the current phase to be exactly the specified phase.

```java
PhaseController.requirePhase(TapestryPhase.REGISTRATION);
// Throws if not in REGISTRATION phase
```

### requireAtLeast(phase)

Requires the current phase to be at least the specified phase (or later).

```java
PhaseController.requireAtLeast(TapestryPhase.TS_READY);
// Throws if before TS_READY
```

### requireAtMost(phase)

Requires the current phase to be at most the specified phase (or earlier).

```java
PhaseController.requireAtMost(TapestryPhase.FREEZE);
// Throws if after FREEZE
```

## Common Phase Errors

### "Cannot register API after FREEZE"

**Cause**: Attempting to register an API endpoint after the FREEZE phase has completed.

**Solution**: Move API registration to the REGISTRATION phase in your extension's `register()` method.

### "API not available in current phase"

**Cause**: Attempting to call an API before it's available (usually before TS_READY).

**Solution**: Move API calls into your mod's `onLoad()` hook, which runs during TS_READY.

### "Cannot register mod after TS_REGISTER"

**Cause**: Attempting to call `tapestry.mod.define()` after the TS_REGISTER phase.

**Solution**: Ensure your mod script is discovered during TS_LOAD and evaluated during TS_REGISTER. Don't try to register mods dynamically at runtime.

### "Persistence not ready"

**Cause**: Attempting to access `tapestry.mod.state` before PERSISTENCE_READY phase.

**Solution**: Wait until the persistence system is initialized. Use event handlers that run during RUNTIME or later.

## Phase Timing Diagram

```text
Time →

BOOTSTRAP ────────┐
                  │ Framework initialization
DISCOVERY ────────┤
                  │ Extension discovery
VALIDATION ───────┤
                  │ Extension validation
REGISTRATION ─────┤
                  │ API registration
FREEZE ───────────┤
                  │ API sealed
TS_LOAD ──────────┤
                  │ TypeScript runtime init
TS_REGISTER ──────┤
                  │ Mod registration
TS_ACTIVATE ──────┤
                  │ Dependency resolution
TS_READY ─────────┤
                  │ Mod onLoad hooks
PERSISTENCE_READY ┤
                  │ State stores ready
EVENT ────────────┤
                  │ Event system ready
RUNTIME ──────────┤
                  │ Game logic active
CLIENT_PRESENTATION_READY
                  │ UI overlays ready
                  ▼
```

## Best Practices

### 1. Respect Phase Boundaries

Don't try to work around phase restrictions. They exist to prevent bugs.

```ts
// ✓ Good: Register handlers during TS_READY
tapestry.mod.define({
  onLoad(api) {
    api.events.on('tick', handler);
  }
});

// ✗ Bad: Try to register handlers later
setTimeout(() => {
  api.events.on('tick', handler); // Throws error!
}, 5000);
```

### 2. Use Phase-Appropriate APIs

Different APIs become available at different phases. Check the API documentation for phase requirements.

```ts
// ✓ Good: Use state API after PERSISTENCE_READY
tapestry.mod.define({
  onLoad(api) {
    api.events.on('serverStarted', () => {
      // Persistence is ready by now
      const score = tapestry.mod.state.get("score");
    });
  }
});
```

### 3. Declare Dependencies Correctly

Mod dependencies are validated during TS_REGISTER. Declare all dependencies upfront.

```ts
// ✓ Good: Declare dependencies
tapestry.mod.define({
  id: "my-mod",
  depends: ["core-lib", "utility-mod"],
  
  onLoad(api) {
    const lib = tapestry.mod.require("core-lib", "helper");
  }
});
```

### 4. Handle Client/Server Differences

Some phases only apply to specific sides. Use `tapestry.isClient` and `tapestry.isServer` to detect the environment.

```ts
tapestry.mod.define({
  onLoad(api) {
    if (tapestry.isClient) {
      // CLIENT_PRESENTATION_READY phase applies here
      api.overlay.register({ /* ... */ });
    }
    
    if (tapestry.isServer) {
      // Server-specific logic
      api.events.on('playerJoin', handler);
    }
  }
});
```

## Next Steps

Now that you understand Tapestry's lifecycle phases:

- Learn about [Extension System](/guide/extensions) to create Java extensions
- Review [Architecture](/guide/architecture) for system design details
- Explore the [API Reference](/api/) for phase-specific API documentation
- Read [Core Concepts](/guide/core-concepts) for the philosophy behind phases

## Key Takeaways

1. **Phases are strict** - No skipping, no going backward, fail-fast enforcement
2. **Each phase has a contract** - Specific allowed and forbidden operations
3. **API freeze is permanent** - After FREEZE, the API shape never changes
4. **Mods load in order** - TS_REGISTER → TS_ACTIVATE → TS_READY
5. **Phase guards prevent bugs** - Illegal operations fail immediately with clear errors
6. **Respect the boundaries** - Don't try to work around phase restrictions
