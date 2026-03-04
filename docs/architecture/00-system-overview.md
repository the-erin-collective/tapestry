# Tapestry System Overview

## Document Status

- Status: As-built
- Last updated: 2026-03-04
- Source baseline: `src/main/java/com/tapestry`

## Purpose

Describe the runtime subsystems, ownership boundaries, and principal data/control paths in current code.

## Scope

- In scope: server/client runtime architecture as currently implemented.
- Out of scope: future design proposals from `docs/tech-specs`.

## Entrypoints (As-Built)

- Main Fabric entrypoint: `com.tapestry.TapestryMod` (`fabric.mod.json` -> `entrypoints.main`)
- Server Fabric entrypoint: `com.tapestry.TapestryServer` (`fabric.mod.json` -> `entrypoints.server`)
- Client classes exist (`com.tapestry.TapestryClient`, `com.tapestry.client.TapestryClient`) but no `entrypoints.client` registration is present in `fabric.mod.json`.

## High-Level Runtime Shape

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

## Subsystem Map

| Subsystem | Package | Responsibility boundary | Key classes |
|---|---|---|---|
| Lifecycle | `com.tapestry.lifecycle` | Authoritative phase state and guards | `TapestryPhase`, `PhaseController` |
| Extensions | `com.tapestry.extensions` | Discover/validate/register extension capabilities and freeze API tree | `ExtensionDiscovery`, `ExtensionValidator`, `ExtensionRegistrationOrchestrator`, `DefaultApiRegistry` |
| TypeScript Runtime | `com.tapestry.typescript` | Owns JS context, tapestry namespace injection, script execution, execution context tracking | `TypeScriptRuntime`, `TsModDiscovery`, `TsModDefineFunction`, `TsWorldgenApi` |
| Mod Graph | `com.tapestry.mod` | TS_REGISTER descriptors, dependency validation, activation order, exports | `ModRegistry`, `ModDescriptor`, `ModDiscovery` |
| Events + State | `com.tapestry.events`, `com.tapestry.state` | Synchronous event dispatch + deferred state flush coordination | `EventBus`, `StateCoordinator`, `ModStateService` |
| RPC Transport + Dispatch | `com.tapestry.rpc`, `com.tapestry.rpc.client`, `com.tapestry.networking` | Handshake/protocol validation, call dispatch, response routing, watch/event push | `RpcPacketHandler`, `RpcServerRuntime`, `RpcDispatcher`, `RpcClientRuntime`, `RateLimiter` |
| Overlay Presentation | `com.tapestry.overlay` | Client UI registration/validation/render snapshots | `OverlayApi`, `OverlayRegistry`, `OverlayRenderer` |
| Persistence | `com.tapestry.persistence` | Per-mod state storage and service-side initialization | `PersistenceService`, `ServerPersistenceService`, `ClientPersistenceService`, `ModStateStore` |

## Ownership Boundaries

- `PhaseController` is global authority for allowed operations; subsystems enforce phase via `requirePhase`/`requireAtLeast`.
- `TypeScriptRuntime` owns JS context thread/queue (`TWILA-JS`) and API surface injection timing.
- `ModRegistry` owns TS_REGISTER and TS_ACTIVATE mod metadata/activation constraints.
- `EventBus` owns event namespace validation and dispatch; `StateCoordinator` owns deferred state-change flush.
- RPC ingress/dispatch boundary: `RpcPacketHandler` validates protocol ingress, `RpcServerRuntime` enforces handshake/rate limits, and `RpcDispatcher` owns method-level authorization/execution.
- Overlay boundary is client presentation only; registration flows through `OverlayApi` into `OverlayRegistry`.
- Persistence boundary is per-mod store access, with cross-mod access denied in runtime factory path.

## Principal Data/Control Paths

1. Boot/phase path: `TapestryMod#onInitialize` -> bootstrap -> validation -> TS load/register/activate/ready -> runtime services.
2. TS mod path: `TsModDiscovery#discoverMods` -> script eval in `TypeScriptRuntime` -> `ModRegistry` dependency/activation path -> `onLoad` execution.
3. RPC path: `RpcPayload` receive -> `RpcPacketHandler#handle` -> `RpcServerRuntime#handleCall` -> `RpcDispatcher#dispatch` -> `RpcResponseSender`.
4. Event/state path: `mod.emit` -> `EventBus#emit` -> listener execution -> `StateCoordinator` deferred flush at outer dispatch depth.
5. Overlay path: `TypeScriptRuntime#extendForClientPresentation` injects client APIs -> `OverlayApi#registerOverlay` -> `OverlayRenderer` draw loop.

## Global Invariants (As-Built)

- Phase transitions are strict single-step increments (`PhaseController#advanceTo`).
- Most API operations are phase-gated and fail fast on mismatch.
- Mod dependency checks fail startup on missing/circular references.
- JS API calls that depend on mod context fail when `TypeScriptRuntime` context has no current mod ID.
- RPC calls require handshake and are rate-limited per player.

## Spec vs As-Built Notes

- Client entrypoint registration mismatch: spec intent implies explicit client initialization, but as-built `fabric.mod.json` has no `entrypoints.client`; impact is client init behavior depends on other paths/events.
- Dual `mod.define` implementations: `TsModDefineFunction#define` enforces `TS_LOAD` while runtime registration path `TypeScriptRuntime#createModDefineFunction` is used in `TS_REGISTER`; impact is phase-sensitive contract ambiguity.
- Phase orchestration complexity: `TapestryMod` drives core phase sequence, but server/client event hooks also attempt phase advances; impact is possible `Invalid phase transition` if ordering diverges.

## Direct References

- `com.tapestry.TapestryMod#onInitialize`
- `com.tapestry.TapestryMod#loadTypeScriptMods`
- `com.tapestry.TapestryServer#onInitialize`
- `com.tapestry.lifecycle.PhaseController#advanceTo`
- `com.tapestry.typescript.TypeScriptRuntime#initializeForModLoading`
- `com.tapestry.typescript.TypeScriptRuntime#extendForRuntime`
- `com.tapestry.mod.ModRegistry#validateDependencies`
- `com.tapestry.rpc.RpcPacketHandler#handle`
- `com.tapestry.rpc.RpcServerRuntime#handleCall`
- `com.tapestry.overlay.OverlayRegistry#registerOverlay`
- `com.tapestry.persistence.PersistenceService#getModStateStore`
