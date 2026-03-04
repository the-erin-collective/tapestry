# Tapestry Phase Contracts

## Document Status

- Status: As-built
- Last updated: 2026-03-04
- Canonical enum: `com.tapestry.lifecycle.TapestryPhase`

## Purpose

Define the real, enforced contracts for each lifecycle phase based on current code.

## Enforcement Primitives

- `PhaseController#advanceTo(...)`
- `PhaseController#requirePhase(...)`
- `PhaseController#requireAtLeast(...)`
- `PhaseController#requireAtMost(...)`
- Registry freeze guards (`RegistryFrozenException`, `IllegalStateException` on frozen stores)

## Global Invariants (As-Built)

| Invariant | Enforcement |
|---|---|
| Phase transitions are single-step and monotonic. | `PhaseController.isValidTransition`: target ordinal must be current + 1. |
| Re-entering same phase is no-op with warning. | `PhaseController.advanceTo`: same phase logs warning and returns. |
| Phase violations fail fast. | `requirePhase/requireAtLeast/requireAtMost` throw `IllegalStateException`. |
| Extension API/hook/service registrations are registration-only and freezeable. | `DefaultApiRegistry`, `DefaultHookRegistry`, `DefaultServiceRegistry`. |
| TS mod registration and activation are split and phase-gated. | `ModRegistry` and `TypeScriptRuntime` enforce `TS_REGISTER` vs `TS_ACTIVATE`. |

## Guard Inventory (Primary)

| Area | Guard points |
|---|---|
| Core phase machine | `lifecycle/PhaseController` |
| Extension validation | `extensions/ExtensionValidator.validate` requires `VALIDATION` |
| Extension registration orchestration | `extensions/ExtensionRegistrationOrchestrator.registerExtensions` requires `REGISTRATION` |
| Extension registries | `DefaultApiRegistry`, `DefaultHookRegistry`, `DefaultServiceRegistry` require `REGISTRATION` and reject writes when frozen |
| TS runtime extensions | `TypeScriptRuntime.initializeForModLoading/extendFor*` methods require specific phases |
| TS mod registry | `mod/ModRegistry` requires `TS_REGISTER` or `TS_ACTIVATE` for specific operations |
| Runtime services | `typescript/PlayersApi` requires `RUNTIME`; `state/ModStateService` requires `RUNTIME+` |
| Persistence services | `ClientPersistenceService.initialize`, `ServerPersistenceService.initialize`, `PersistenceService.initialize` require `PERSISTENCE_READY+` |
| Client presentation | `overlay/OverlayRegistry.registerOverlay` requires `CLIENT_PRESENTATION_READY+` |

## Direct Class/Method Reference Map

| Phase | Primary class/method references |
|---|---|
| `BOOTSTRAP` | `TapestryMod#bootstrapFramework`, `TypeScriptRuntime#evaluatePlatformScript` |
| `DISCOVERY` | `TapestryMod#bootstrapFramework`, `TapestryMod#registerFabricHooks`, `ExtensionDiscovery#discoverProviders` |
| `VALIDATION` | `TapestryMod#validateExtensions`, `ExtensionValidator#validate` |
| `REGISTRATION` | `TapestryMod#loadTypeScriptMods`, `ExtensionRegistrationOrchestrator#registerExtensions`, `DefaultApiRegistry#addFunction`, `DefaultHookRegistry#registerHook`, `DefaultServiceRegistry#addService` |
| `FREEZE` | `ExtensionRegistrationOrchestrator#registerExtensions` (phase completion), `DefaultApiRegistry#freeze`, `DefaultHookRegistry#freeze`, `DefaultServiceRegistry#freeze` |
| `TS_LOAD` | `TapestryMod#loadTypeScriptMods`, `TypeScriptRuntime#initializeForModLoading`, `TsModDefineFunction#define` |
| `TS_REGISTER` | `TapestryMod#loadTypeScriptMods`, `TypeScriptRuntime#extendForRegistration`, `TypeScriptRuntime#extendForCapabilityRegistration`, `ModRegistry#beginRegistration`, `ModRegistry#registerMod`, `ModRegistry#validateDependencies`, `ModRegistry#buildActivationOrder`, `TypeScriptRuntime#evaluateModScript` |
| `TS_ACTIVATE` | `TapestryMod#loadTypeScriptMods`, `TypeScriptRuntime#extendForActivation`, `ModRegistry#beginActivation`, `ModRegistry#registerExport`, `ModRegistry#requireExport` |
| `TS_READY` | `TapestryMod#loadTypeScriptMods`, `TypeScriptRuntime#extendForReadyPhase`, `TypeScriptRuntime#executeOnLoad`, `TsWorldgenApi#onResolveBlock`, `HookRegistry#allowRegistration` |
| `PERSISTENCE_READY` | `TapestryMod#initializeRuntimeServices`, `ClientPersistenceService#initialize`, `ServerPersistenceService#initialize`, `PersistenceService#initialize` |
| `EVENT` | `TapestryMod#initializeRuntimeServices`, `TypeScriptRuntime#extendForEventPhase`, `EventBus#subscribe`, `EventBus#emit` |
| `RUNTIME` | `TapestryMod#initializeRuntimeServices`, `TypeScriptRuntime#extendForRuntime`, `TypeScriptRuntime#extendForRpcPhase`, `PlayersApi#createNamespace` handlers, `ModStateService#set/get/delete` |
| `CLIENT_PRESENTATION_READY` | `TapestryMod#registerFabricHooks`, `com.tapestry.client.TapestryClient#onInitializeClient`, `TypeScriptRuntime#extendForClientPresentation`, `OverlayRegistry#registerOverlay` |
| `FAILED` | `TapestryPhase#FAILED` (enum contract only, no explicit transition path currently wired) |

## As-Built Contract Matrix

| Phase | Allowed operations (as-built) | Guarded forbidden operations | Enforcement locations |
|---|---|---|---|
| `BOOTSTRAP` | Core object construction and optional platform script eval. | Running non-bootstrap internal script eval paths. | `TypeScriptRuntime.evaluatePlatformScript` requires `BOOTSTRAP`. |
| `DISCOVERY` | Hook registration and discovery orchestration setup. | Direct mutation APIs from later phases. | Mostly orchestration in `TapestryMod`; limited direct method guards. |
| `VALIDATION` | Descriptor, dependency, capability, and type validation. | Validation outside validation phase. | `ExtensionValidator.validate` requires `VALIDATION`. |
| `REGISTRATION` | Extension capability registration into API/hook/service registries. | Registration from any other phase; post-freeze writes. | `ExtensionRegistrationOrchestrator`; `Default*Registry` phase + freeze checks. |
| `FREEZE` | Registry sealing and transition to TS load. | Further extension registry mutation. | `apiRegistry.freeze()`, `hookRegistry.freeze()`, `serviceRegistry.freeze()`, frozen checks in registries. |
| `TS_LOAD` | JS runtime bootstrap and initial `tapestry.mod.define` bridge wiring. | Runtime extension calls for TS_REGISTER/TS_READY/RUNTIME APIs. | `TypeScriptRuntime.initializeForModLoading` requires `TS_LOAD`. |
| `TS_REGISTER` | Mod definition collection, dependency graph validation, capability registration API. | Export/require registration and activation-only operations. | `ModRegistry.beginRegistration/registerMod/validateDependencies/buildActivationOrder`; `TypeScriptRuntime.extendForRegistration` and `extendForCapabilityRegistration`; `evaluateModScript`. |
| `TS_ACTIVATE` | Add `mod.export/mod.require`, activate mods in dependency order. | Registering new mods or calling TS_REGISTER-only APIs. | `TypeScriptRuntime.extendForActivation`; `ModRegistry.beginActivation/registerExport/requireExport`. |
| `TS_READY` | `onLoad` execution and hook registration window. | Hook registration outside this window; runtime-only APIs. | `TypeScriptRuntime.extendForReadyPhase` and `executeOnLoad`; `TsWorldgenApi.onResolveBlock` requires `TS_READY`. |
| `PERSISTENCE_READY` | Persistence backend initialization and state-store access readiness. | Initializing persistence before this phase. | `ServerPersistenceService.initialize`, `ClientPersistenceService.initialize`, `PersistenceService.initialize` require `PERSISTENCE_READY+`. |
| `EVENT` | Optional event API extension into TS object. | Calling event-phase extension in wrong phase. | `TypeScriptRuntime.extendForEventPhase` requires `EVENT`. |
| `RUNTIME` | Gameplay-facing APIs: players, state, scheduler/event/config runtime APIs, RPC/env API extension. | Player/state access before runtime. | `PlayersApi` methods require `RUNTIME`; `ModStateService` requires `RUNTIME+`; `TypeScriptRuntime.extendForRuntime` and `extendForRpcPhase` require `RUNTIME`. |
| `CLIENT_PRESENTATION_READY` | Overlay and client presentation API injection and overlay registration. | Overlay registration before presentation-ready. | `TypeScriptRuntime.extendForClientPresentation`; `OverlayRegistry.registerOverlay` requires `CLIENT_PRESENTATION_READY+`. |
| `FAILED` | Intended terminal failure phase. | Further forward transitions after failure (by policy). | Enum exists; explicit transition to `FAILED` is not currently wired in boot path. |

## Per-Phase Details

### BOOTSTRAP

- Entry: `TapestryMod.bootstrapFramework()` advances to `BOOTSTRAP`.
- Enforced usage: `TypeScriptRuntime.evaluatePlatformScript(...)` requires this phase.
- Notes: Most bootstrap constraints are convention and call-order in `TapestryMod`, not broad API guards.
- Direct refs: `com.tapestry.TapestryMod#bootstrapFramework`, `com.tapestry.typescript.TypeScriptRuntime#evaluatePlatformScript`.

### DISCOVERY

- Entry: `TapestryMod.bootstrapFramework()` advances to `DISCOVERY` and registers Fabric callbacks.
- Enforced usage: limited direct phase-checked API in this phase.
- Notes: Discovery purity is primarily architectural intent; hard checks are sparse.
- Direct refs: `com.tapestry.TapestryMod#registerFabricHooks`, `com.tapestry.extensions.ExtensionDiscovery#discoverProviders`.

### VALIDATION

- Entry: `TapestryMod.validateExtensions()` advances to `VALIDATION`.
- Enforced usage includes `ExtensionValidator.validate(...)` requiring `VALIDATION`.
- Capabilities and types are finalized via `CapabilityRegistry.freeze()` and `ExtensionTypeRegistry.freeze()` as part of validation.
- Direct refs: `com.tapestry.TapestryMod#validateExtensions`, `com.tapestry.extensions.ExtensionValidator#validate`, `com.tapestry.extensions.CapabilityRegistry#freeze`, `com.tapestry.extensions.types.ExtensionTypeRegistry#freeze`.

### REGISTRATION and FREEZE

- Entry: `TapestryMod.loadTypeScriptMods()` advances to `REGISTRATION`.
- Enforced usage includes:
- `ExtensionRegistrationOrchestrator.registerExtensions(...)` requires exact `REGISTRATION`.
- `DefaultApiRegistry.addFunction(...)` requires exact `REGISTRATION`; validates namespace/declaration; rejects frozen writes.
- `DefaultHookRegistry.registerHook(...)` and `DefaultServiceRegistry.addService(...)` require exact `REGISTRATION`; reject frozen writes.
- Freeze behavior:
- `orchestrator.registerExtensions(...)` completes `REGISTRATION` and advances to `FREEZE`.
- Registries are explicitly frozen after orchestration in `TapestryMod`.
- Direct refs: `com.tapestry.extensions.ExtensionRegistrationOrchestrator#registerExtensions`, `com.tapestry.extensions.DefaultApiRegistry#addFunction`, `com.tapestry.extensions.DefaultApiRegistry#freeze`, `com.tapestry.extensions.DefaultHookRegistry#registerHook`, `com.tapestry.extensions.DefaultHookRegistry#freeze`, `com.tapestry.extensions.DefaultServiceRegistry#addService`, `com.tapestry.extensions.DefaultServiceRegistry#freeze`.

### TS_LOAD

- Entry: `TapestryMod.loadTypeScriptMods()` advances to `TS_LOAD`.
- Enforced usage includes `TypeScriptRuntime.initializeForModLoading(...)` and `TsModDefineFunction.define(...)` requiring `TS_LOAD`.
- Notes: runtime later replaces `mod.define` during `TS_REGISTER` with a different implementation backed by `ModRegistry`.
- Direct refs: `com.tapestry.typescript.TypeScriptRuntime#initializeForModLoading`, `com.tapestry.typescript.TsModDefineFunction#define`.

### TS_REGISTER

- Entry: explicit advance in `TapestryMod.loadTypeScriptMods()`.
- Enforced usage includes:
- `TypeScriptRuntime.extendForRegistration(...)` and `extendForCapabilityRegistration(...)` require `TS_REGISTER`.
- `ModRegistry.beginRegistration/registerMod/defineMod/validateDependencies/buildActivationOrder` require `TS_REGISTER`.
- `TypeScriptRuntime.evaluateModScript(...)` requires `TS_REGISTER`.
- Direct refs: `com.tapestry.typescript.TypeScriptRuntime#extendForRegistration`, `com.tapestry.typescript.TypeScriptRuntime#extendForCapabilityRegistration`, `com.tapestry.mod.ModRegistry#beginRegistration`, `com.tapestry.mod.ModRegistry#registerMod`, `com.tapestry.mod.ModRegistry#validateDependencies`, `com.tapestry.mod.ModRegistry#buildActivationOrder`, `com.tapestry.typescript.TypeScriptRuntime#evaluateModScript`.

### TS_ACTIVATE

- Entry: explicit advance in `TapestryMod.loadTypeScriptMods()`.
- Enforced usage includes `TypeScriptRuntime.extendForActivation(...)` and `ModRegistry.beginActivation/registerExport/requireExport` requiring `TS_ACTIVATE`.
- Direct refs: `com.tapestry.typescript.TypeScriptRuntime#extendForActivation`, `com.tapestry.mod.ModRegistry#beginActivation`, `com.tapestry.mod.ModRegistry#registerExport`, `com.tapestry.mod.ModRegistry#requireExport`.

### TS_READY

- Entry: explicit advance in `TapestryMod.loadTypeScriptMods()`.
- Enforced usage includes:
- `TypeScriptRuntime.extendForReadyPhase(...)` requires `TS_READY`.
- `TypeScriptRuntime.executeOnLoad(...)` requires `TS_READY`.
- `TsWorldgenApi.onResolveBlock(...)` requires `TS_READY` and additional mod-context checks.
- Additional guard model: `com.tapestry.hooks.HookRegistry` uses an explicit registration window (`allowRegistration`/`freeze`) instead of direct phase checks.
- Direct refs: `com.tapestry.typescript.TypeScriptRuntime#extendForReadyPhase`, `com.tapestry.typescript.TypeScriptRuntime#executeOnLoad`, `com.tapestry.typescript.TsWorldgenApi#onResolveBlock`, `com.tapestry.hooks.HookRegistry#allowRegistration`, `com.tapestry.hooks.HookRegistry#freeze`.

### PERSISTENCE_READY

- Entry: reached during runtime init path and can also be touched in server-start callbacks with ordinal guards.
- Enforced usage includes `ClientPersistenceService.initialize(...)`, `ServerPersistenceService.initialize(...)`, and deprecated `PersistenceService.initialize(...)` requiring `PERSISTENCE_READY+`.
- Direct refs: `com.tapestry.persistence.ClientPersistenceService#initialize`, `com.tapestry.persistence.ServerPersistenceService#initialize`, `com.tapestry.persistence.PersistenceService#initialize`.

### EVENT

- Entry: `TapestryMod.initializeRuntimeServices()` advances to `EVENT`.
- Enforced usage includes `TypeScriptRuntime.extendForEventPhase()` requiring `EVENT`.
- `EventBus` itself does not currently enforce lifecycle phase; it enforces namespace and dispatch constraints.
- Direct refs: `com.tapestry.typescript.TypeScriptRuntime#extendForEventPhase`, `com.tapestry.events.EventBus#subscribe`, `com.tapestry.events.EventBus#emit`.

### RUNTIME

- Entry: `TapestryMod.initializeRuntimeServices()` advances to `RUNTIME`.
- Enforced usage includes:
- `TypeScriptRuntime.extendForRuntime(...)` and `extendForRpcPhase()` require `RUNTIME`.
- `PlayersApi` methods require exact `RUNTIME`.
- `ModStateService` methods require `RUNTIME+`.
- Direct refs: `com.tapestry.typescript.TypeScriptRuntime#extendForRuntime`, `com.tapestry.typescript.TypeScriptRuntime#extendForRpcPhase`, `com.tapestry.typescript.PlayersApi#createNamespace` function handlers, `com.tapestry.state.ModStateService#set`.

### CLIENT_PRESENTATION_READY

- Entry: can be advanced from client entrypoint and server/integrated callbacks.
- Enforced usage includes `TypeScriptRuntime.extendForClientPresentation()` requiring exact `CLIENT_PRESENTATION_READY`.
- `OverlayRegistry.registerOverlay(...)` requires `CLIENT_PRESENTATION_READY+`.
- Direct refs: `com.tapestry.client.TapestryClient#onInitializeClient`, `com.tapestry.typescript.TypeScriptRuntime#extendForClientPresentation`, `com.tapestry.overlay.OverlayRegistry#registerOverlay`.

### FAILED

- Present in `TapestryPhase` as terminal design intent.
- Current boot path does not explicitly transition to `FAILED` on startup exceptions.
- Direct refs: `com.tapestry.lifecycle.TapestryPhase#FAILED`.

## Spec vs As-Built Differences

| Topic | Spec expectation | As-built observation | Risk |
|---|---|---|---|
| Discovery purity | Discovery phase should be strictly side-effect free. | Strongly followed in design, but limited hard phase guards for all discovery-time actions. | Medium: regression possible if discovery code mutates state without explicit checks. |
| `FREEZE` semantics | Freeze should be explicit contract boundary. | Transition to `FREEZE` is performed by `ExtensionRegistrationOrchestrator.complete(REGISTRATION)` and then concrete registries are frozen in `TapestryMod`. | Low: behavior is clear, but split ownership across orchestrator and bootstrap class. |
| TS mod define phase | Single canonical define path and phase. | Two define implementations exist (`TsModDefineFunction` in `TS_LOAD`, runtime replacement in `TS_REGISTER`). | Medium: contract confusion for maintainers and future refactors. |
| `FAILED` terminal state | Host should enter `FAILED` on irrecoverable startup error. | Startup failures throw exceptions; no explicit transition to `FAILED`. | Medium: runtime state can remain at last successful phase after fatal boot error. |
| Event lifecycle gating | Event operations tied to event/runtime phases. | `extendForEventPhase` is phase-gated, but `EventBus` operations are phase-agnostic and namespace-gated instead. | Low: intentional flexibility, but weaker lifecycle coupling than spec wording implies. |
