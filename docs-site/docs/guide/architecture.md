---
title: Architecture
description: Understanding Tapestry's system design and runtime architecture
---

# Architecture

This guide explains Tapestry's system design, runtime architecture, and how the various subsystems work together to provide a TypeScript-first modding framework for Minecraft.

## System Overview

Tapestry is built on a strict phase-based lifecycle model with clear subsystem boundaries. The architecture ensures that mod code runs at the correct time and that illegal operations fail immediately with clear diagnostics.

![Tapestry Architecture Diagram](/diagrams/architecture.svg)
*Figure 1: Tapestry's system architecture showing the relationships between core subsystems, including lifecycle management, extension system, TypeScript runtime, mod graph, events and state, RPC transport, overlay presentation, and persistence.*

### High-Level Runtime Shape

```text
Fabric loader
  -> TapestryMod bootstrap/validation/TS pipeline
    -> PhaseController (global phase machine)
    -> Extension registration and frozen API tree
    -> TypeScriptRuntime (GraalVM + API injection)
    -> ModRegistry (TS_REGISTER/TS_ACTIVATE graph)
    -> Runtime services (scheduler, event bus, config, state)
    -> RPC stack (packet handler, handshake, dispatcher, client runtime bridge)
    -> Client presentation overlays (when CLIENT_PRESENTATION_READY reached)
    -> Persistence services and mod stores
```

## Core Subsystems

Tapestry is organized into distinct subsystems, each with clear responsibilities and ownership boundaries:

### Lifecycle Management

**Package**: `com.tapestry.lifecycle`

The lifecycle subsystem is the authoritative source for phase state and guards. It ensures that all operations happen at the correct time in the boot sequence.

**Key Classes**:
- `TapestryPhase` - Enum defining all lifecycle phases
- `PhaseController` - Global phase state machine and transition enforcement

**Responsibilities**:
- Enforce strict single-step phase transitions
- Provide phase guards (`requirePhase`, `requireAtLeast`, `requireAtMost`)
- Prevent invalid phase transitions with fail-fast errors

### Extension System

**Package**: `com.tapestry.extensions`

The extension system discovers, validates, and registers Java-based extensions that provide capabilities to TypeScript mods.

**Key Classes**:
- `ExtensionDiscovery` - Discovers extension providers
- `ExtensionValidator` - Validates extension descriptors and dependencies
- `ExtensionRegistrationOrchestrator` - Coordinates extension registration
- `DefaultApiRegistry` - Manages API function registration and freezing

**Responsibilities**:
- Discover extension providers during DISCOVERY phase
- Validate extension capabilities and dependencies during VALIDATION phase
- Register extension APIs during REGISTRATION phase
- Freeze API tree before TypeScript runtime initialization

### TypeScript Runtime

**Package**: `com.tapestry.typescript`

The TypeScript runtime owns the GraalVM JavaScript context and manages the `tapestry` namespace injection, script execution, and execution context tracking.

**Key Classes**:
- `TypeScriptRuntime` - Owns JS context and API injection
- `TsModDiscovery` - Discovers TypeScript mod files
- `TsModDefineFunction` - Implements `mod.define` bridge
- `TsWorldgenApi` - Provides worldgen API to TypeScript

**Responsibilities**:
- Initialize GraalVM context for mod loading
- Inject `tapestry` namespace with phase-appropriate APIs
- Execute TypeScript mod scripts
- Track current mod execution context
- Extend runtime APIs as phases progress

### Mod Graph

**Package**: `com.tapestry.mod`

The mod graph subsystem manages TypeScript mod registration, dependency validation, and activation ordering.

**Key Classes**:
- `ModRegistry` - Central registry for mod descriptors
- `ModDescriptor` - Metadata for a single mod
- `ModDiscovery` - Discovers and evaluates mod scripts

**Responsibilities**:
- Collect mod descriptors during TS_REGISTER phase
- Validate mod dependencies
- Build activation order based on dependency graph
- Manage mod exports and requires during TS_ACTIVATE phase

### Events and State

**Packages**: `com.tapestry.events`, `com.tapestry.state`

The event and state subsystems provide synchronous event dispatch and deferred state flush coordination.

**Key Classes**:
- `EventBus` - Synchronous event dispatch with namespace validation
- `StateCoordinator` - Coordinates deferred state change flushes
- `ModStateService` - Per-mod state management

**Responsibilities**:
- Validate event namespace ownership
- Dispatch events synchronously to registered listeners
- Track dispatch depth for deferred state changes
- Flush pending state changes at dispatch depth zero

### RPC Transport and Dispatch

**Packages**: `com.tapestry.rpc`, `com.tapestry.rpc.client`, `com.tapestry.networking`

The RPC subsystem handles client-server communication, including handshake, protocol validation, call dispatch, and response routing.

**Key Classes**:
- `RpcPacketHandler` - Validates and routes incoming RPC packets
- `RpcServerRuntime` - Server-side RPC runtime with handshake and rate limiting
- `RpcDispatcher` - Dispatches RPC calls to registered methods
- `RpcClientRuntime` - Client-side RPC runtime with pending call tracking
- `RateLimiter` - Per-player rate limiting

**Responsibilities**:
- Validate RPC protocol and packet structure
- Enforce handshake requirements
- Rate-limit RPC calls per player
- Dispatch calls to registered server methods
- Route responses back to client callers

### Overlay Presentation

**Package**: `com.tapestry.overlay`

The overlay subsystem manages client-side UI registration, validation, and rendering.

**Key Classes**:
- `OverlayApi` - TypeScript API for overlay registration
- `OverlayRegistry` - Validates and stores overlay definitions
- `OverlayRenderer` - Renders overlays each frame

**Responsibilities**:
- Register overlays during CLIENT_PRESENTATION_READY phase
- Validate overlay definitions (id, anchor, render function)
- Render overlays each frame with sanitized snapshots
- Manage overlay visibility per mod

### Persistence

**Package**: `com.tapestry.persistence`

The persistence subsystem provides per-mod state storage with service-side initialization.

**Key Classes**:
- `PersistenceService` - Abstract persistence service
- `ServerPersistenceService` - Server-side persistence implementation
- `ClientPersistenceService` - Client-side persistence implementation
- `ModStateStore` - Per-mod key-value store

**Responsibilities**:
- Initialize persistence backend during PERSISTENCE_READY phase
- Provide per-mod state stores
- Enforce same-mod access restrictions
- Persist state to disk

## Boot Sequence

Understanding the boot sequence is critical to understanding when different operations are allowed.

### Phase Progression

Tapestry progresses through phases in strict order. Each phase has specific allowed operations, and attempting operations outside their designated phase results in immediate failure.

1. **BOOTSTRAP** - Core initialization
   - Instantiate core fields (API, registries, TypeScript runtime)
   - Set up basic infrastructure

2. **DISCOVERY** - Extension scanning
   - Register Fabric lifecycle hooks
   - Discover extension providers

3. **VALIDATION** - Extension validation
   - Validate extension descriptors
   - Check dependencies and capabilities
   - Freeze capability and type registries

4. **REGISTRATION** - API domain declaration
   - Register extension APIs, hooks, and services
   - Build extension registries

5. **FREEZE** - API shape is sealed
   - Freeze all registries (API, hook, service)
   - API tree becomes immutable

6. **TS_LOAD** - GraalVM starts, scripts loaded
   - Initialize TypeScript runtime for mod loading
   - Discover TypeScript mod files

7. **TS_REGISTER** - Mod registration and metadata collection
   - Evaluate mod scripts
   - Collect mod descriptors via `mod.define`
   - Validate dependencies
   - Build activation order

8. **TS_ACTIVATE** - Dependency resolution
   - Activate mods in dependency order
   - Process `mod.export` and `mod.require`

9. **TS_READY** - `onLoad` hooks execute
   - Execute all mod `onLoad` handlers
   - Allow hook registration

10. **PERSISTENCE_READY** - Persistence backend ready
    - Initialize persistence service
    - Enable mod state stores

11. **EVENT** - Event system ready
    - Extend runtime with event APIs

12. **RUNTIME** - Game logic and events active
    - Initialize runtime services (scheduler, event bus, config, state)
    - Extend runtime with gameplay APIs (players, RPC, etc.)

13. **CLIENT_PRESENTATION_READY** - Client UI ready
    - Extend runtime with client presentation APIs
    - Enable overlay registration

### Server Lifecycle Integration

Tapestry integrates with Fabric's server lifecycle events:

- **SERVER_STARTED**: Initialize player service, persistence backend, and RPC system
- **END_SERVER_TICK**: Tick scheduler and event bus; emit `engine:tick` event
- **JOIN/DISCONNECT**: Emit player join/leave events

## Key Runtime Flows

### TypeScript Mod Loading

1. `TsModDiscovery` scans filesystem and classpath for mod scripts
2. Runtime advances to TS_REGISTER phase
3. Each script is evaluated in the GraalVM context
4. Scripts call `tapestry.mod.define` to register metadata
5. `ModRegistry` validates dependencies and builds activation order
6. Runtime advances to TS_ACTIVATE phase
7. Mods are activated in dependency order
8. Runtime advances to TS_READY phase
9. Each mod's `onLoad` handler is executed

### RPC Call Flow

1. Client calls `tapestry.rpc.server.<method>(...)`
2. `RpcClientRuntime` creates call ID and registers pending future
3. Client sends RPC packet to server
4. `RpcPacketHandler` validates packet structure
5. `RpcServerRuntime` enforces handshake and rate limits
6. `RpcDispatcher` validates method and executes handler
7. Server sends response packet
8. `RpcClientRuntime` resolves pending future with result

### Event Dispatch and State Flush

1. Mod calls `tapestry.mod.emit(namespace, event, data)`
2. `EventBus` validates namespace ownership
3. `StateCoordinator` tracks dispatch depth
4. Event listeners execute synchronously
5. Listeners may modify state via `tapestry.mod.state`
6. When dispatch depth returns to zero, `StateCoordinator` flushes pending state changes
7. Internal `__state_change__` events are emitted for state watchers

## Ownership Boundaries

Clear ownership boundaries prevent subsystems from interfering with each other:

- **PhaseController** is the global authority for allowed operations
- **TypeScriptRuntime** owns the JS context thread/queue (`TWILA-JS`)
- **ModRegistry** owns mod metadata and activation constraints
- **EventBus** owns event namespace validation and dispatch
- **StateCoordinator** owns deferred state-change flush
- **RpcServerRuntime** enforces handshake and rate limits
- **OverlayRegistry** owns client presentation registration
- **PersistenceService** enforces per-mod store access

## Global Invariants

These invariants are enforced throughout the system:

- Phase transitions are strict single-step increments
- Most API operations are phase-gated and fail fast on mismatch
- Mod dependency checks fail startup on missing/circular references
- JS API calls that depend on mod context fail when no current mod ID is set
- RPC calls require handshake and are rate-limited per player
- Event namespaces must be owned by the emitting mod
- State changes are deferred and flushed at dispatch depth zero

## Error Handling

Tapestry follows a fail-fast philosophy. When something goes wrong, the system throws an exception immediately with a clear error message rather than allowing invalid state to propagate.

### Common Error Scenarios

- **Invalid phase transition**: Attempting to skip phases or transition backward
- **Phase mismatch**: Calling an API outside its allowed phase
- **Missing dependencies**: Mod depends on another mod that doesn't exist
- **Circular dependencies**: Mod dependency graph contains cycles
- **Namespace violation**: Emitting events in a namespace not owned by the mod
- **Cross-mod access**: Attempting to access another mod's persistence store
- **RPC handshake failure**: Client attempts RPC call before handshake completes
- **Rate limit exceeded**: Client makes too many RPC calls in a short time

## Further Reading

For more detailed information about specific aspects of the architecture:

- [Boot Sequence Details](https://github.com/your-org/tapestry/blob/main/docs/architecture/01-boot-sequence.md) - Detailed boot timeline
- [Phase Contracts](https://github.com/your-org/tapestry/blob/main/docs/architecture/02-phase-contracts.md) - Allowed operations per phase
- [Runtime Data Flows](https://github.com/your-org/tapestry/blob/main/docs/architecture/03-runtime-data-flows.md) - End-to-end runtime paths

## Next Steps

Now that you understand Tapestry's architecture:

- Learn about [Lifecycle Phases](/guide/lifecycle-phases) in detail
- Explore the [Extension System](/guide/extensions)
- Review the [API Reference](/api/) for specific function documentation
