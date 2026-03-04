# Troubleshooting Guide

## Document Status

- Status: As-built
- Last updated: 2026-03-04
- Audience: maintainers and mod developers
- Source baseline: `src/main/java/com/tapestry`

## Fast Triage

1. Capture the first thrown exception and message (ignore follow-on noise first).
2. Identify phase and side at failure time (`TS_LOAD`/`TS_REGISTER`/`TS_READY`/`TS_ACTIVATE`/`CLIENT_PRESENTATION_READY`/`RUNTIME` and client/server).
3. Map the message to subsystem below (phase, mod registration, TS context, RPC, events, overlay, persistence).
4. Reproduce with smallest mod set (target mod + one dependency if needed).
5. Confirm fix with a full phase progression run.

## Common Issues

| Symptom | Likely cause | Where to look | Immediate action |
|---|---|---|---|
| `Invalid phase transition: X -> Y` | non-monotonic phase advance | `com.tapestry.lifecycle.PhaseController#advanceTo` | only advance to exact next enum phase |
| `Operation requires phase A, but current phase is B` | API called in wrong phase | `PhaseController#requirePhase`, caller method | move call to the correct lifecycle hook |
| `TypeScript runtime not initialized` | extension method invoked before runtime init | `TypeScriptRuntime#extendFor*`, `evaluate*` | ensure initialization and phase order before extension calls |
| `Multiple tapestry.mod.define calls in source ...` | one-define-per-file rule violated | `TsModDefineFunction#define` | keep exactly one `tapestry.mod.define` per source file |
| `Duplicate mod ID: ...` / `Mod ID 'x' is already registered` | duplicate mod registration | `ModRegistry#registerMod`, `TsModRegistry#registerMod` | rename one mod ID |
| `Mod 'a' depends on missing mod 'b'` | unresolved dependency in graph | `ModRegistry#validateDependenciesAndPrepareActivation` | add/install missing mod or remove stale `dependsOn` |
| `mod.require('x') not declared in dependsOn ...` | undeclared dependency import | `TypeScriptRuntime#createModRequireFunction` | add `x` to `dependsOn` before requiring |
| `No mod ID set in current context` | API invoked outside mod execution context | `TsSchedulerApi`, `TsStateApi`, `TsConfigApi`, `ModEventApi` | call from mod hooks/runtime where execution context is set |
| `Hooks may only be registered during onLoad() execution ...` | worldgen hook registration too late/early | `TsWorldgenApi#onResolveBlock` | register in `onLoad` during `TS_READY` path only |
| `tapestry.client.overlay.register is not a function` | client overlay API not injected yet | `TypeScriptRuntime#runSanityCheckForPhase` + `#extendForClientPresentation` | defer overlay registration until `CLIENT_PRESENTATION_READY` |
| `Overlay ID 'x' is already registered by mod 'y'` | duplicate overlay ID per mod namespace | `OverlayRegistry#registerOverlay` | use unique overlay ID |
| `Mod 'a' cannot modify overlay 'x' owned by mod 'b'` | cross-mod overlay mutation blocked | `OverlayApi#setOverlayVisibility` | only modify your own overlays |
| `PersistenceService not initialized` / persistence namespace missing | persistence startup not complete | `PersistenceService#getInstance`, `TypeScriptRuntime#extendTapestryObjectForRuntime` | wait for persistence init; guard for missing `tapestry.persistence` |
| `Mod 'a' cannot access persistence for mod 'b'` | cross-mod persistence access blocked | `TypeScriptRuntime#createPersistenceApiFactory` | access only current mod store |
| `Server RPC not ready (handshake incomplete)` / `HANDSHAKE_INCOMPLETE` | client calling RPC before handshake | `RpcApi#createServerProxy`, `RpcClientRuntime#callServer` | wait for handshake completion before RPC calls |
| Disconnect message `RPC protocol violation` | server received RPC pre-handshake | `RpcServerRuntime#handleCall` / `#disconnect` | verify client handshake flow and ordering |
| `Rate limited RPC call from player ...` | per-client rate limits exceeded | `RateLimiter#canMakeCall`, `RpcServerRuntime#handleCall` | back off calls; batch/coalesce client requests |
| `Invalid event name format` / namespace rejection | bad event namespace or unauthorized emit | `RpcApi#emitTo`, `EventBus#validateNamespace` | use valid event format and allowed namespace ownership |
| `getCapability() only available after capability resolution completes` | capability access before registry freeze | `CapabilityApi#validateRuntimeAccess` | move access to runtime after resolution completes |

## High-Signal Message Map

Use these exact substrings to classify failures quickly:

```powershell
Invalid phase transition:
Operation requires phase
TypeScript runtime not initialized
No mod ID set in current context
Multiple tapestry.mod.define calls
Duplicate mod ID:
depends on missing mod
not declared in dependsOn
Server RPC not ready (handshake incomplete)
HANDSHAKE_INCOMPLETE
RPC protocol violation
Rate limited RPC call
PersistenceService not initialized
cannot access persistence for mod
tapestry.client.overlay.register is not a function
```

Direct refs:

- `com.tapestry.lifecycle.PhaseController#advanceTo`
- `com.tapestry.lifecycle.PhaseController#requirePhase`
- `com.tapestry.typescript.TypeScriptRuntime#extendForRuntime`
- `com.tapestry.typescript.TsModDefineFunction#define`
- `com.tapestry.mod.ModRegistry#registerMod`
- `com.tapestry.mod.ModRegistry#validateDependenciesAndPrepareActivation`
- `com.tapestry.typescript.RpcApi#createServerProxy`
- `com.tapestry.rpc.RpcServerRuntime#handleCall`
- `com.tapestry.rpc.RateLimiter#canMakeCall`
- `com.tapestry.overlay.OverlayRegistry#registerOverlay`
- `com.tapestry.persistence.PersistenceService#getInstance`
- `com.tapestry.events.EventBus#validateNamespace`

## Diagnostic Commands

Run from repository root:

```powershell
# Runtime/phase failures
rg -n --glob "*.log" "Invalid phase transition|Operation requires phase|TypeScript runtime not initialized" .

# Mod definition/dependency failures
rg -n --glob "*.log" "Multiple tapestry.mod.define calls|Duplicate mod ID|depends on missing mod|not declared in dependsOn" .

# RPC handshake/protocol/rate failures
rg -n --glob "*.log" "handshake|HANDSHAKE_INCOMPLETE|RPC protocol violation|Rate limited RPC call|Protocol mismatch" .

# Overlay/persistence failures
rg -n --glob "*.log" "overlay|tapestry.client.overlay.register|PersistenceService not initialized|cannot access persistence for mod" .
```

## Incident Template

```text
Date/time:
Environment (integrated or dedicated):
Current phase and side:
Failing mod ID:
First exception (full line):
Immediately preceding log line:
Minimal reproduction steps:
Root cause class/method:
Fix applied:
Regression test added:
```

## Escalation Checklist

- [ ] Confirm failure is reproducible with minimal mod set.
- [ ] Confirm phase/side at failure matches API contract.
- [ ] Confirm this is not a mod-context misuse (`No mod ID set in current context` family).
- [ ] Confirm spec docs and as-built runtime behavior align (or record divergence).
- [ ] Add regression test in `src/test/java` for the specific failure signature.
