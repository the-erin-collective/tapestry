# TypeScript API Reference

## Document Status

- Status: As-built
- Last updated: 2026-03-04
- Audience: Tapestry mod authors
- Source baseline: `src/main/java/com/tapestry/typescript`

## Usage Rules

- APIs are phase-gated by `PhaseController` and by runtime extension points.
- Several namespaces are injected only after specific lifecycle extensions.
- Many functions require a current mod execution context (`TypeScriptRuntime.getCurrentModId()`).
- Failures are explicit (exceptions), not silent.

## Availability Snapshot

| Namespace | First available phase | Side | Injection path |
|---|---|---|---|
| `tapestry.mod.define` | `TS_LOAD` then replaced in `TS_REGISTER` | both | `TypeScriptRuntime#initializeForModLoading`, `TypeScriptRuntime#extendForRegistration`, `TypeScriptRuntime#extendForCapabilityRegistration` |
| `tapestry.scheduler` | `RUNTIME` | both | `TypeScriptRuntime#extendForRuntime` |
| `tapestry.config` | `RUNTIME` | both | `TypeScriptRuntime#extendForRuntime` |
| `tapestry.state` | `RUNTIME` | both | `TypeScriptRuntime#extendForRuntime` |
| `tapestry.runtime` | `RUNTIME` | both | `TypeScriptRuntime#extendForRuntime` |
| `tapestry.persistence` | `RUNTIME` (if persistence initialized) | both | `TypeScriptRuntime#extendForRuntime` |
| `tapestry.mod.on/emit/off` | `TS_ACTIVATE`+ | both | `TypeScriptRuntime#extendForActivation` and runtime/client extensions |
| `tapestry.mod.state.createState` | `TS_ACTIVATE`+ | both | `StateFactory#createStateNamespace` |
| `tapestry.mod.capability` | `TS_REGISTER` for registration, runtime accessor later | both | `CapabilityApi#createCapabilityNamespace`, `CapabilityApi#createRuntimeCapabilityNamespace` |
| `tapestry.worldgen.onResolveBlock` | `TS_READY` | both | `TypeScriptRuntime#extendForReadyPhase` |
| `tapestry.client.players` | `CLIENT_PRESENTATION_READY` | client | `TypeScriptRuntime#extendForClientPresentation` |
| `tapestry.client.overlay` | `CLIENT_PRESENTATION_READY` | client | `TypeScriptRuntime#extendForClientPresentation` |
| `tapestry.rpc` | `RUNTIME` (after RPC extension) | both | `TypeScriptRuntime#extendForRpcPhase` |
| `tapestry.env` | `RUNTIME` (via RPC extension path) | both | `TypeScriptRuntime#extendForRpcPhase` |

## `mod.define` Contracts (Important)

`tapestry.mod.define` has two active implementations depending on lifecycle stage:

| Path | Enforced phase | Required fields | Handler |
|---|---|---|---|
| TS mod-loading define | `TS_LOAD` | `id`, `onLoad` (`onEnable` optional) | `com.tapestry.typescript.TsModDefineFunction#define` |
| registration define | `TS_REGISTER` | `id`, `version` (`dependsOn`, `activate`, `deactivate` optional) | `com.tapestry.typescript.TypeScriptRuntime#createModDefineFunction` |

Direct refs:

- `com.tapestry.typescript.TypeScriptRuntime#initializeForModLoading`
- `com.tapestry.typescript.TsModDefineFunction#define`
- `com.tapestry.typescript.TypeScriptRuntime#extendForRegistration`
- `com.tapestry.typescript.TypeScriptRuntime#extendForCapabilityRegistration`

## `env` (`tapestry.env`)

| API | Signature | Side | Phase |
|---|---|---|---|
| `isClient` | `() => boolean` | both | `RUNTIME+` (after RPC extension) |
| `isServer` | `() => boolean` | both | `RUNTIME+` (after RPC extension) |
| `side` | `"client" | "server"` | both | `RUNTIME+` (after RPC extension) |

Direct refs:

- `com.tapestry.typescript.EnvApi#createNamespace`
- `com.tapestry.typescript.TypeScriptRuntime#extendForRpcPhase`

## `mod` core and events (`tapestry.mod`)

| API | Signature | Side | Phase |
|---|---|---|---|
| `define` | `(definitionObj) => void` | both | `TS_LOAD` and `TS_REGISTER` paths |
| `export` | `(key, value) => void` | both | `TS_ACTIVATE` |
| `require` | `(modId) => any` | both | `TS_ACTIVATE` |
| `on` | `(eventName, handler) => void` | both | `TS_ACTIVATE+` |
| `emit` | `(eventName, payload?) => void` | both | `TS_ACTIVATE+` |
| `off` | `(eventName, handler) => void` | both | `TS_ACTIVATE+` |
| `state.createState` | `(name, initialValue) => StateProxy` | both | `TS_ACTIVATE+` |
| `capability.provideCapability` | `(name, impl) => void` | both | `TS_REGISTER` |
| `capability.requireCapability` | `(name) => void` | both | `TS_REGISTER` |
| `capability.getCapability` | `(name) => any` | both | after capability registry freeze |

Direct refs:

- `com.tapestry.typescript.TsModDefineFunction#define`
- `com.tapestry.typescript.TypeScriptRuntime#extendForActivation`
- `com.tapestry.typescript.ModEventApi#createModEventApi`
- `com.tapestry.typescript.StateFactory#createStateNamespace`
- `com.tapestry.typescript.CapabilityApi#createCapabilityNamespace`
- `com.tapestry.typescript.CapabilityApi#createRuntimeCapabilityNamespace`

## `worldgen` (`tapestry.worldgen`)

| API | Signature | Side | Phase |
|---|---|---|---|
| `onResolveBlock` | `(handler) => void` | both | `TS_READY` only |

Notes:

- Must be called during `onLoad` context for a mod.
- Throws if called during script-evaluation context.

Direct refs:

- `com.tapestry.typescript.TsWorldgenApi#onResolveBlock`
- `com.tapestry.typescript.TypeScriptRuntime#extendForReadyPhase`

## `scheduler` (`tapestry.scheduler`)

| API | Signature | Side | Phase |
|---|---|---|---|
| `setTimeout` | `(callback, delayMs) => handle` | both | `RUNTIME+` |
| `setInterval` | `(callback, intervalMs) => handle` | both | `RUNTIME+` |
| `clearInterval` | `(handle) => void` | both | `RUNTIME+` |
| `nextTick` | `(callback) => handle` | both | `RUNTIME+` |

Notes:

- Requires current mod context ID.
- No explicit phase check in `TsSchedulerApi`; availability is by runtime injection.

Direct refs:

- `com.tapestry.typescript.TsSchedulerApi#createNamespace`
- `com.tapestry.scheduler.SchedulerService`
- `com.tapestry.typescript.TypeScriptRuntime#extendForRuntime`

## `config` (`tapestry.config`)

| API | Signature | Side | Phase |
|---|---|---|---|
| `get` | `(modId) => configObj` | both | `RUNTIME+` |
| `self` | `() => configObj` | both | `RUNTIME+` |

Direct refs:

- `com.tapestry.typescript.TsConfigApi#createNamespace`
- `com.tapestry.config.ConfigService`

## `state` (`tapestry.state` and `tapestry.mod.state`)

| API | Signature | Side | Phase |
|---|---|---|---|
| `state.set` | `(key, value) => void` | both | `RUNTIME+` |
| `state.get` | `(key) => any` | both | `RUNTIME+` |
| `state.has` | `(key) => boolean` | both | `RUNTIME+` |
| `state.delete` | `(key) => boolean` | both | `RUNTIME+` |
| `state.keys` | `() => string[]` | both | `RUNTIME+` |
| `state.clear` | `() => void` | both | `RUNTIME+` |
| `mod.state.createState` | `(name, initialValue) => StateProxy` | both | `TS_ACTIVATE`, `CLIENT_PRESENTATION_READY`, `RUNTIME` |

Direct refs:

- `com.tapestry.typescript.TsStateApi#createNamespace`
- `com.tapestry.typescript.StateFactory#createStateNamespace`
- `com.tapestry.state.ModStateService`
- `com.tapestry.state.StateCoordinator`

## `runtime` (`tapestry.runtime`)

| API | Signature | Side | Phase |
|---|---|---|---|
| `runtime.log.info` | `(message, context?) => void` | both | `RUNTIME+` |
| `runtime.log.warn` | `(message, context?) => void` | both | `RUNTIME+` |
| `runtime.log.error` | `(message, context?) => void` | both | `RUNTIME+` |

Direct refs:

- `com.tapestry.typescript.TsRuntimeApi#createNamespace`
- `com.tapestry.typescript.TypeScriptRuntime#extendForRuntime`

## `persistence` (`tapestry.persistence`)

| API | Signature | Side | Phase |
|---|---|---|---|
| `getModStore` | `(modId) => ModStateStore` | both | `RUNTIME+` and persistence initialized |

Notes:

- As-built runtime injects a factory-style `tapestry.persistence.getModStore(modId)`.
- Runtime checks caller identity (`modId` must match current mod context).
- Returned `ModStateStore` host object exposes `get/set/delete/keys` and additional helpers (`hasKey`, `size`, `clear`, `save`, `getAll`).

Direct refs:

- `com.tapestry.typescript.TypeScriptRuntime#createPersistenceApiFactory`
- `com.tapestry.persistence.PersistenceApi`
- `com.tapestry.persistence.ModStateStore`

## `rpc` (`tapestry.rpc`)

| API | Signature | Side | Phase |
|---|---|---|---|
| `defineServerApi` | `(objectOfMethods) => void` | server | `RUNTIME+` (RPC extension present) |
| `server.<method>` | `(...args) => CompletableFuture<JsonElement>` | client | `RUNTIME+` (handshake complete) |
| `emitTo` | `(player, eventName, payload) => void` | server | `RUNTIME+` |
| `watch.register` | `(player, watchKey) => void` | server | `RUNTIME+` |
| `watch.unregister` | `(player, watchKey) => void` | server | `RUNTIME+` |
| `watch.emit` | `(watchKey, payload) => void` | server | `RUNTIME+` |
| `clientEvents.on` | `(eventName, handler) => void` | client | `RUNTIME+` |

Notes:

- `server.<method>` returns client runtime future, not a plain JS value.
- Requires handshake completion on client (`RpcClientRuntime.isHandshakeComplete()`).

Direct refs:

- `com.tapestry.typescript.RpcApi#createNamespace`
- `com.tapestry.typescript.RpcApi#defineServerApi`
- `com.tapestry.typescript.RpcApi#createServerProxy`
- `com.tapestry.rpc.client.RpcClientRuntime#callServer`

## `client.players` (`tapestry.client.players`)

| API | Signature | Side | Phase |
|---|---|---|---|
| `getPosition` | `() => {x,y,z} | null` | client | `CLIENT_PRESENTATION_READY+` |
| `getLook` | `() => {yaw,pitch,dir} | null` | client | `CLIENT_PRESENTATION_READY+` |
| `raycastBlock` | `(options?) => result` | client | `CLIENT_PRESENTATION_READY+` |

Notes:

- Current implementation is client-only and does not accept UUID.
- `raycastBlock` enforces `maxDistance <= 32`.

Direct refs:

- `com.tapestry.typescript.ClientPlayersApi#createNamespace`
- `com.tapestry.typescript.TypeScriptRuntime#extendForClientPresentation`

## `client.overlay` (`tapestry.client.overlay`)

| API | Signature | Side | Phase |
|---|---|---|---|
| `register` | `(definition) => void` | client | `CLIENT_PRESENTATION_READY+` |
| `setVisible` | `(modId, overlayId, visible) => void` | client | `CLIENT_PRESENTATION_READY+` |
| `getCount` | `() => number` | client | `CLIENT_PRESENTATION_READY+` |
| `template` | `(template, data?) => UINode[]` | client | `CLIENT_PRESENTATION_READY+` |
| `add` | `(fragment) => UINode[]` | client | `CLIENT_PRESENTATION_READY+` |

Direct refs:

- `com.tapestry.overlay.OverlayApi#createNamespace`
- `com.tapestry.overlay.OverlayRegistry#registerOverlay`

## Minimal Example (As-Built)

```ts
tapestry.mod.define({
  id: "example_mod",
  onLoad(t) {
    t.worldgen.onResolveBlock((ctx, vanilla) => vanilla);
  },
});
```

## Known Gaps and Undefined Behavior

- `types.d.ts` is currently a placeholder and does not describe the runtime API surface.
- `TsEventsApi` exists in code but runtime notes indicate old `tapestry.events` namespace has been removed in favor of `mod.on/emit/off`.
- RPC wire schema has mismatches between some server/client handlers (packet type and field naming) and should be treated as unstable until normalized.
- `TsModDefineFunction` enforces `TS_LOAD`, while runtime also installs a TS_REGISTER define path via `ModRegistry`; this dual path can be confusing for mod authors.
