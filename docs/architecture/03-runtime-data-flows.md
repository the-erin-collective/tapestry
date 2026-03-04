# Tapestry Runtime Data Flows

## Document Status

- Status: As-built
- Last updated: 2026-03-04
- Source baseline: `src/main/java/com/tapestry`

## Goal

Capture the core end-to-end runtime flows that drive behavior and failure modes.

## Flow 1: TypeScript Mod Discovery to OnLoad Execution

### Scope

From runtime bootstrap through TS discovery/registration/activation and `onLoad` execution.

### Key classes

- `TapestryMod`
- `TsModDiscovery`
- `TypeScriptRuntime`
- `TsModRegistry`
- `ModRegistry`
- `TsModDefineFunction`

### As-Built Sequence

1. `TapestryMod#loadTypeScriptMods` enters extension `REGISTRATION`, builds API/service/hook registries, and freezes the extension API tree.
2. `TapestryMod#loadTypeScriptMods` advances to `TS_LOAD` and calls `TypeScriptRuntime#initializeForModLoading(apiTree, modRegistry, tsHookRegistry, typeRegistry)`.
3. `TsModDiscovery#discoverMods` scans filesystem from Fabric config dir `.../tapestry/mods` and classpath `assets/tapestry/mods` in mod containers.
4. Loader advances to `TS_REGISTER`, then `TypeScriptRuntime#extendForCapabilityRegistration` replaces `tapestry` with a writable mod namespace for registration.
5. Each discovered script is evaluated via `TypeScriptRuntime#evaluateScript`.
6. In this phase, `tapestry.mod.define` is served by `TypeScriptRuntime#createModDefineFunction` and writes into `ModRegistry#registerMod`.
7. Registration validation runs in `ModRegistry` using `validateDependencies` then `buildActivationOrder`.
8. Activation order is stored with `TypeScriptRuntime#setStoredActivationOrder`.
9. Loader advances to `TS_ACTIVATE`, calls `TypeScriptRuntime#extendForActivation`, and activation executes in dependency order via `TypeScriptRuntime#activateMods`.
10. Loader advances to `TS_READY`, installs worldgen API via `TypeScriptRuntime#extendForReadyPhase`, then executes each mod `onLoad` via `TypeScriptRuntime#executeOnLoad`.
11. Hook registration is unlocked (`HookRegistry#allowRegistration`), and discovery/loading flags are completed in `TsModRegistry`.

### Failure points

- `Invalid phase transition: ...` when phase sequencing skips required enum order (`PhaseController#advanceTo`).
- Duplicate `mod.define`/mod IDs (`ModRegistry#registerMod`, `TsModDefineFunction#define` source guards).
- Missing or circular dependencies (`ModRegistry#validateDependencies`, `#buildActivationOrder`).
- Missing stored activation order at TS_ACTIVATE (`TypeScriptRuntime#extendForActivation`).
- `onLoad` exception aborting startup (`TypeScriptRuntime#executeOnLoad` call site in `TapestryMod`).

### Spec vs As-Built Notes

- Two `mod.define` implementations exist: `TsModDefineFunction#define` (strict `TS_LOAD` contract) and the TS_REGISTER path `TypeScriptRuntime#createModDefineFunction`.
- As-built startup primarily uses the TS_REGISTER define path for discovered scripts.

### Direct references

- `com.tapestry.TapestryMod#loadTypeScriptMods`
- `com.tapestry.typescript.TsModDiscovery#discoverMods`
- `com.tapestry.typescript.TypeScriptRuntime#initializeForModLoading`
- `com.tapestry.typescript.TypeScriptRuntime#extendForCapabilityRegistration`
- `com.tapestry.typescript.TypeScriptRuntime#evaluateScript`
- `com.tapestry.typescript.TypeScriptRuntime#extendForActivation`
- `com.tapestry.typescript.TypeScriptRuntime#executeOnLoad`
- `com.tapestry.mod.ModRegistry#registerMod`
- `com.tapestry.mod.ModRegistry#validateDependencies`
- `com.tapestry.mod.ModRegistry#buildActivationOrder`

## Flow 2: RPC Client Call Round Trip

### Scope

Client-side RPC call from TS API to server dispatch and response completion.

### Key classes

- `RpcApi`
- `RpcClientRuntime`
- `PendingCallRegistry`
- `RpcPacketHandler`
- `RpcServerRuntime`
- `RpcDispatcher`
- `RpcResponseSender`

### As-Built Sequence

1. TS-side call uses `tapestry.rpc.server.<method>(...)` (created by `RpcApi#createServerProxy`).
2. Proxy delegates to `RpcClientRuntime#callServer(method, argsJson)`.
3. `RpcClientRuntime#callServer` checks handshake, creates call ID, and registers future in `PendingCallRegistry#register`.
4. Client sends payload (`RpcPayload`) through `ClientPlayNetworking.send`.
5. Server receiver (registered in `TapestryMod#initializeRpcSystem`) forwards JSON to `RpcPacketHandler#handle`.
6. `RpcPacketHandler#handle` enforces size/protocol/sanitization, then routes `rpc_call` to `RpcServerRuntime#handleCall`.
7. `RpcServerRuntime#handleCall` enforces handshake readiness and per-player rate limits (`RateLimiter#canMakeCall`).
8. `RpcDispatcher#dispatch` validates method allowlist (`ServerApiRegistry`) and executes server method.
9. Server sends response via `RpcResponseSender#sendSuccess` or `#sendError`.
10. Client packet path reaches `RpcClientRuntime#handle`/`handleIncoming`; response is resolved/rejected in `PendingCallRegistry`.

### Failure points

- Handshake not complete (`RpcApi` proxy and `RpcClientRuntime` guards).
- Protocol mismatch or packet validation failure (`RpcPacketHandler`).
- Rate limiting (`RateLimiter` in `RpcServerRuntime`).
- Unknown method/access denied (`RpcDispatcher`).
- Client timeout (`PendingCallRegistry` default 10s timeout).

### Wire-contract mismatch note (as-built)

Current server and client packet field/type names are inconsistent:

- server responses: `type: "response"`, `requestId`
- client response handler expects: `type: "rpc_response"`, `id`
- server events: `type: "event"`, `channel`/`data`
- client event handler expects: `type: "server_event"`, `event`/`payload`

This is an interoperability risk until normalized.

### Direct references

- `com.tapestry.typescript.RpcApi#createServerProxy`
- `com.tapestry.rpc.client.RpcClientRuntime#callServer`
- `com.tapestry.rpc.client.PendingCallRegistry#register`
- `com.tapestry.rpc.RpcPacketHandler#handle`
- `com.tapestry.rpc.RpcServerRuntime#handleCall`
- `com.tapestry.rpc.RateLimiter#canMakeCall`
- `com.tapestry.rpc.RpcDispatcher#dispatch`
- `com.tapestry.rpc.RpcResponseSender#sendSuccess`
- `com.tapestry.rpc.client.RpcClientRuntime#handle`

## Flow 3: Mod Event Emit and Deferred State Flush

### Scope

Event registration and synchronous dispatch, including deferred state change flush behavior.

### Key classes

- `ModEventApi`
- `EventBus`
- `StateCoordinator`

### As-Built Sequence

1. TS mod registers handlers through `tapestry.mod.on`, backed by `ModEventApi#createModEventApi`.
2. API validates allowed lifecycle phase (`TS_ACTIVATE`, `CLIENT_PRESENTATION_READY`, `RUNTIME`).
3. Subscription enters `EventBus#subscribe` and namespace rules are checked by `EventBus#validateNamespace`.
4. Producer emits via `tapestry.mod.emit` -> `EventBus#emit`.
5. `EventBus#emit` calls `StateCoordinator#onDispatchStart`, snapshots listeners, and executes handlers synchronously.
6. Handler exceptions are logged; dispatch continues to remaining listeners.
7. `EventBus#emit` calls `StateCoordinator#onDispatchEnd`.
8. At depth zero, `StateCoordinator` flushes pending state changes by emitting internal `__state_change__` events.

### Failure points

- Phase misuse (`mod.on`/`emit`/`off` outside allowed phases).
- Namespace ownership violations (`EventBus#validateNamespace`).
- Recursive dispatch depth warnings (deep event nesting).
- High state-change queue churn warning (`StateCoordinator`).

### Direct references

- `com.tapestry.typescript.ModEventApi#createModEventApi`
- `com.tapestry.events.EventBus#subscribe`
- `com.tapestry.events.EventBus#emit`
- `com.tapestry.events.EventBus#validateNamespace`
- `com.tapestry.state.StateCoordinator#onDispatchStart`
- `com.tapestry.state.StateCoordinator#onDispatchEnd`

## Flow 4: Persistence Access Through Runtime API

### Scope

Runtime persistence access from TS mods via `tapestry.persistence`.

### Key classes

- `TypeScriptRuntime`
- `PersistenceService`
- `PersistenceApi`
- `ModStateStore`

### As-Built Sequence

1. During runtime extension, `TypeScriptRuntime#extendTapestryObjectForRuntime` attempts persistence injection.
2. If `PersistenceService` is initialized, runtime injects factory-style `tapestry.persistence` from `TypeScriptRuntime#createPersistenceApiFactory`.
3. Mod calls `tapestry.persistence.getModStore(modId)`.
4. Factory enforces same-mod access (`modId` must equal current execution context mod ID).
5. Runtime returns host `ModStateStore` from `PersistenceService#getModStateStore(modId)`.
6. Mod uses store API (`get`, `set`, `delete`, `keys`, plus `hasKey`, `clear`, `save`, `getAll`).

### Failure points

- Persistence service not initialized when runtime attempts injection.
- Cross-mod access denial (`cannot access persistence for mod`).
- Invalid key/value type enforcement in `ModStateStore#set`.

### Direct references

- `com.tapestry.typescript.TypeScriptRuntime#extendTapestryObjectForRuntime`
- `com.tapestry.typescript.TypeScriptRuntime#createPersistenceApiFactory`
- `com.tapestry.persistence.PersistenceService#getModStateStore`
- `com.tapestry.persistence.ModStateStore#set`
- `com.tapestry.persistence.ModStateStore#save`

## Flow 5: Client Presentation and Overlay Render Path

### Scope

Client presentation readiness and overlay registration/render flow.

### Key classes

- `TypeScriptRuntime`
- `OverlayApi`
- `OverlayRegistry`
- `OverlayRenderer`

### As-Built Sequence

1. Runtime reaches `CLIENT_PRESENTATION_READY` and `TypeScriptRuntime#extendForClientPresentation` runs.
2. Runtime injects `tapestry.client.players` and `tapestry.client.overlay`, plus `tapestry.client.mod` event/state/capability sub-APIs.
3. Overlay definitions register through `OverlayApi#registerOverlay` -> `OverlayRegistry#registerOverlay`.
4. `OverlayRenderer#getInstance` registers HUD callback and starts per-frame render path.
5. Each frame queues JS overlay evaluation on the runtime thread and renders latest sanitized snapshot on client thread.

### Failure points

- Phase mismatch before `CLIENT_PRESENTATION_READY`.
- Invalid overlay definition (`id`, `anchor`, `render`) or invalid anchor/zIndex.
- Cross-mod visibility mutation denial (`OverlayApi#setOverlayVisibility`).
- Overlay runtime render exceptions; overlay may be disabled by registry logic.

### Direct references

- `com.tapestry.typescript.TypeScriptRuntime#extendForClientPresentation`
- `com.tapestry.overlay.OverlayApi#createNamespace`
- `com.tapestry.overlay.OverlayApi#registerOverlay`
- `com.tapestry.overlay.OverlayRegistry#registerOverlay`
- `com.tapestry.overlay.OverlayRenderer#getInstance`

## Cross-Flow Dependencies

| Producer flow | Depends on | Why |
|---|---|---|
| TS mod load/activate | strict phase ordering | `TS_LOAD -> TS_REGISTER -> TS_ACTIVATE -> TS_READY` contracts gate API availability. |
| RPC round trip | `RUNTIME` + handshake + network registration | `RpcApi` proxy and server runtime both enforce handshake/protocol rules. |
| Events/state | event bus availability and mod context | event APIs require active event bus and valid execution context. |
| Persistence | persistence service initialization | runtime injects persistence only when service is ready. |
| Overlay | `CLIENT_PRESENTATION_READY` and client runtime | overlay API and renderer are client presentation artifacts only. |
