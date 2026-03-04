# Tapestry Boot Sequence

## Document Status

- Status: As-built
- Last updated: 2026-03-04
- Source baseline: `src/main/java/com/tapestry`

## Goal

Trace what actually happens from Fabric entrypoint invocation to runtime callbacks, including dedicated and integrated server branches.

## Entrypoints and Ownership

- `com.tapestry.TapestryMod` is registered as `main` entrypoint in `fabric.mod.json`.
- `com.tapestry.TapestryServer` is registered as `server` entrypoint in `fabric.mod.json`.
- Both can execute on dedicated server environments.

## Phase Engine Constraints

All phase transitions go through `PhaseController.advanceTo(...)` and must move exactly one step forward. Skips are illegal and throw `IllegalStateException`. Transitioning to the same phase is a warning and no-op.

Implication for boot:

- Any callback that advances phase must run in the expected order.
- Late callbacks often guard with ordinal checks to avoid invalid transitions.

## High-Level Startup Path

`TapestryMod.onInitialize()` calls:

1. `bootstrapFramework()`
2. `validateExtensions()`
3. `loadTypeScriptMods()`

Then runtime callbacks registered during discovery continue initialization at server start and server tick.

## Detailed Timeline (As-Built)

| Sequence | Phase entered | Location | Main actions |
|---|---|---|---|
| 1 | `BOOTSTRAP` | `TapestryMod.bootstrapFramework` | Instantiate core fields (`TapestryAPI`, registries, `TypeScriptRuntime`, discovery helpers, RPC registry placeholders). |
| 2 | `DISCOVERY` | `TapestryMod.bootstrapFramework` | Register Fabric lifecycle/network/player hooks via `registerFabricHooks()`. |
| 3 | `VALIDATION` | `TapestryMod.validateExtensions` | Discover extension providers, run `ExtensionValidator`, print reports, keep enabled set. |
| 4 | `REGISTRATION` | `TapestryMod.loadTypeScriptMods` | Build extension registries, register core player capabilities, run `ExtensionRegistrationOrchestrator`. |
| 5 | `FREEZE` | `orchestrator.registerExtensions` completion then `api/hook/serviceRegistry.freeze()` | Registries become immutable, API tree captured for TS runtime. |
| 6 | `TS_LOAD` | `TapestryMod.loadTypeScriptMods` | Initialize TS runtime for mod loading and type integration, discover scripts from filesystem/classpath. |
| 7 | `TS_REGISTER` | `TapestryMod.loadTypeScriptMods` | Extend runtime for capability registration, evaluate discovered scripts, require `mod.define`, validate dependencies, build activation order. |
| 8 | `TS_ACTIVATE` | `TapestryMod.loadTypeScriptMods` | Extend runtime for activation and run capability validation consistency checks. |
| 9 | `TS_READY` | `TapestryMod.loadTypeScriptMods` | Extend runtime with ready APIs, execute all `onLoad` handlers, allow TS hook registration, complete mod loading flags. |
| 10 | `PERSISTENCE_READY` | `TapestryMod.initializeRuntimeServices` | Initialize persistence phase marker (currently logs/no-op in this method). |
| 11 | `EVENT` | `TapestryMod.initializeRuntimeServices` | Initialize event phase marker (currently logs/no-op in this method). |
| 12 | `RUNTIME` | `TapestryMod.initializeRuntimeServices` | Create `SchedulerService`, `EventBus`, `ConfigService`, `ModStateService`, extend runtime with runtime APIs. |

After sequence 12, server lifecycle callbacks still run and perform additional initialization work.

## Fabric Callback Registrations (Installed During DISCOVERY)

`registerFabricHooks()` registers:

- `ServerLifecycleEvents.SERVER_STARTED`
- `ServerTickEvents.END_SERVER_TICK`
- `ServerPlayConnectionEvents.JOIN`
- `ServerPlayConnectionEvents.DISCONNECT`
- Additional `SERVER_STARTED` handler on client env for integrated-server presentation phase advance.

## Server Started Callback Behavior

On `SERVER_STARTED`, `TapestryMod` performs:

1. `PlayerService` creation and injection into `PlayersApi`.
2. Persistence backend selection:
- Dedicated server: `ServerPersistenceService.initialize(Paths.get("world"))`.
- Integrated server: `ClientPersistenceService.initialize(Paths.get("."))`.
3. Conditional phase advance to `PERSISTENCE_READY` only if current phase is earlier.
4. `initializeRpcSystem(server)`:
- Create dispatcher/runtime/handlers.
- Register RPC payload codecs in `PayloadTypeRegistry`.
- Register C2S global receiver and hop work back to server thread.
- Extend TS runtime for RPC APIs.
- Register disconnect cleanup for handshake/watch/rate state.
5. `initializePhase105ModSystem()`:
- Contains guards to skip registration/activation if current phase has already passed them.
- Discovers/evaluates mods through `ModDiscovery`.
6. Calls `advanceTo(RUNTIME)` again.
- If already `RUNTIME`, this is a warning no-op.
7. Dedicated server branch (`envType == SERVER`) tries `CLIENT_PRESENTATION_READY`, extends runtime for client presentation, emits `engine:runtimeStart` if event bus exists.

## Server Tick Callback Behavior

Every server tick:

1. Emits `engine:tick` through `EventBus` when initialized.
2. Ticks `SchedulerService` when initialized.
3. Integrated server branch:
- If env is client and `clientPresentationInitialized == false`, checks current phase.
- When phase is `CLIENT_PRESENTATION_READY`, extends client presentation APIs, emits `engine:runtimeStart`, then sets init flag.

## Player Connection Callback Behavior

- `JOIN`: emits `eventBus.emit("engine", "playerJoin", null)` if event bus exists.
- `DISCONNECT`: emits `eventBus.emit("engine", "playerLeave", null)` if event bus exists.
- RPC system also registers its own disconnect cleanup callback to clear handshake/watch/rate state.

## Side-Specific Sequences

### Dedicated Server

1. `TapestryMod` runs full phase chain through `RUNTIME`.
2. `SERVER_STARTED` callback wires player service, persistence backend, RPC, phase 10.5 loader.
3. Presentation extension attempt happens in server callback via `CLIENT_PRESENTATION_READY` transition path.
4. `TapestryServer` server entrypoint can also initialize a parallel RPC stack and extra server handlers.

### Integrated Server (Client Environment)

1. `TapestryMod` runs bootstrap and TS loading chain.
2. `SERVER_STARTED` runs service and RPC setup.
3. Additional client-only `SERVER_STARTED` callback advances to `CLIENT_PRESENTATION_READY`.
4. `END_SERVER_TICK` gate performs one-time client presentation runtime extension and emits `engine:runtimeStart`.

## `TapestryServer` Entrypoint Path

`TapestryServer` is a separate `server` entrypoint with its own startup logic:

1. Runs only when env type is `SERVER`.
2. Calls `initializeRpcSystem()` and registers `SERVER_STARTING`, `SERVER_STOPPING`, and additional server tick handlers.
3. Its RPC initialization also registers payload codecs/receiver, TS RPC API wiring, and disconnect cleanup.

Operational note:

- Since `TapestryMod` already initializes RPC paths in its callbacks, this can produce overlapping responsibilities if both are active on the same runtime.

## Failure Behavior

| Failure point | Behavior |
|---|---|
| Any uncaught exception in `TapestryMod.onInitialize` | Wrapped and rethrown as `RuntimeException("Failed to initialize Tapestry", e)`; startup fails. |
| Invalid phase transition | `PhaseController` throws `IllegalStateException` immediately. |
| Extension registration failure | Startup aborts with `RuntimeException("Extension registration failed", e)`. |
| TS script evaluation or onLoad failure | Startup aborts with runtime exception naming failing mod/source. |
| RPC setup failure in `initializeRpcSystem(server)` | Throws `RuntimeException("RPC system initialization failed", e)`. |
| Optional presentation init failure in dedicated server branch | Logged and suppressed (startup continues). |

## Boot Log Markers to Watch

Expected markers in order:

1. `=== BOOTSTRAP PHASE ===`
2. `=== DISCOVERY PHASE ===`
3. `=== VALIDATION PHASE ===`
4. `=== REGISTRATION PHASE ===`
5. `=== TS_LOAD PHASE ===`
6. `=== TS_REGISTER PHASE ===`
7. `=== TS_ACTIVATE PHASE ===`
8. `=== TS_READY PHASE ===`
9. `=== PERSISTENCE_READY PHASE ===`
10. `=== EVENT PHASE ===`
11. `=== RUNTIME PHASE ===`
12. `Server started - initializing services`

## Spec vs As-Built Notes

| Topic | As-built observation | Impact |
|---|---|---|
| Runtime phase timing | `initializeRuntimeServices()` reaches `RUNTIME` before `SERVER_STARTED` callbacks run. | Some server-dependent services are intentionally deferred to callback time. |
| Re-advance to `RUNTIME` | `SERVER_STARTED` attempts `advanceTo(RUNTIME)` again. | Usually no-op warning when already in `RUNTIME`. |
| Phase 10.5 loader | `initializePhase105ModSystem()` can run after earlier TS phases and mostly self-skip via ordinal checks. | Boot path complexity; requires careful reasoning during failures. |
| Parallel server entrypoint | `TapestryServer` duplicates part of RPC init path. | Potential overlap in registration and debugging ambiguity on dedicated servers. |
