# Tapestry — Architecture & Lifecycle

Tapestry is a **TypeScript-first modding framework** built on top of Fabric.
It provides a **strict, explicit lifecycle** and **enforced phase model** that makes it impossible to run mod logic at the wrong time.

Tapestry is *not* a compatibility layer for existing Fabric mods.
Any mod that works with Tapestry **must be designed for Tapestry from the beginning**.

---

## Design Goals

* **Explicit lifecycle** — no implicit “this should be ready by now” assumptions
* **Hard boundaries** — illegal operations fail immediately
* **TypeScript as a first-class citizen** — not a scripting afterthought
* **Additive-only extensibility** — no overrides, no mutation after freeze
* **Deterministic behavior** — same inputs always produce the same results

Tapestry intentionally limits what TypeScript can do, in exchange for strong guarantees.

---

## What Tapestry Is *Not*

To avoid confusion, Tapestry does **not**:

* integrate arbitrary Fabric mods
* wrap or adapt existing Fabric APIs automatically
* attempt Bukkit / Paper compatibility
* allow registry mutation from TypeScript
* provide unrestricted scripting access

If a mod does not explicitly declare Tapestry entrypoints, **Tapestry ignores it**.

---

## High-level Architecture

```
Fabric (Minecraft lifecycle)
        ↓
Tapestry Core (phases, safety, API shape)
        ↓
Tapestry-aware Mods (Java extensions + TypeScript logic)
```

Fabric is treated as an **input source**, not the authority.
Tapestry owns lifecycle, permissions, and API shape.

---

## Lifecycle Model (Runlevel-style Phases)

Tapestry uses an explicit, enforced phase machine inspired by Linux runlevels.

### Phase enum

```java
BOOTSTRAP
DISCOVERY
REGISTRATION
FREEZE
TS_LOAD
TS_READY
RUNTIME
```

Only **one phase is active at any time**.
Phases advance **monotonically** and **never regress**.

---

## Phase Semantics

### `BOOTSTRAP`

JVM is running. Tapestry core initializes.

**Allowed**

* initialize internal state
* set up phase controller

**Forbidden**

* Fabric access
* mod discovery
* API mutation
* TypeScript access

---

### `DISCOVERY`

Tapestry-aware extensions are discovered via Fabric entrypoints.

**Allowed**

* scan Fabric entrypoints
* instantiate extension providers
* read extension descriptors

**Forbidden**

* API mutation
* side effects
* TypeScript access

Discovery must be **pure**.

---

### `REGISTRATION`

Extensions are allowed to define API shape.

**Allowed**

* extend core domains (additive only)
* register mod-owned namespaces
* declare capabilities

**Forbidden**

* TypeScript access
* gameplay hooks
* world access
* registry mutation

This is the **only phase** where API shape may change.

---

### `FREEZE`

API surface is sealed permanently.

**Allowed**

* validation
* consistency checks
* object freezing

**Forbidden**

* *everything else*

After this phase, API shape is immutable.

---

### `TS_LOAD`

TypeScript runtime is initialized.

**Allowed**

* start JS engine (GraalVM)
* inject frozen API object
* load TS modules *without executing mod logic*

**Forbidden**

* running mod logic
* registering hooks

---

### `TS_READY`

TypeScript setup is allowed.

**Allowed**

* `onLoad` execution
* hook registration
* read-only access to registries

**Forbidden**

* API mutation
* registry mutation
* dimension registration

This is the **safe setup window** for TypeScript mods.

---

### `RUNTIME`

Server is live and gameplay begins.

**Allowed**

* worldgen
* events
* gameplay logic

**Forbidden**

* API changes
* extension registration
* TS reloads

---

## Phase Enforcement

All sensitive operations must explicitly check the current phase.

> **Silent failure is considered a bug.**

Phase violations throw `IllegalStateException` with clear diagnostics:

* attempted operation
* expected phase
* current phase

---

## Fabric Integration

### Entrypoints

Tapestry recognizes **only** the following Fabric entrypoints:

#### Tapestry Core

* Standard Fabric mod with `ModInitializer#onInitialize()`

#### Tapestry Extensions (Java)

```json
"entrypoints": {
  "tapestry:extension": [
    "com.example.MyTapestryExtension"
  ]
}
```

Only mods declaring `tapestry:extension` are considered part of Tapestry.

---

### Phase Transitions (Fabric → Tapestry)

| Fabric trigger            | Tapestry phase |
| ------------------------- | -------------- |
| Tapestry static init      | `BOOTSTRAP`    |
| Start of `onInitialize()` | `DISCOVERY`    |
| After discovery completes | `REGISTRATION` |
| End of registration       | `FREEZE`       |
| After freeze              | `TS_LOAD`      |
| After TS runtime ready    | `TS_READY`     |
| Server started (later)    | `RUNTIME`      |

---

## Extension System

### Extension Provider Interface

```java
public interface TapestryExtensionProvider {
  TapestryExtensionDescriptor describe();
  void register(TapestryExtensionContext ctx);
}
```

* `describe()` must be side-effect free
* `register()` is called **only during REGISTRATION**

---

### Extension Descriptor

Descriptors are Java objects returned from `describe()`:

```java
public record TapestryExtensionDescriptor(
  String id,
  List<String> capabilities
) {}
```

#### ID rules

* Regex: `[A-Za-z][A-Za-z0-9_]*`
* Must be globally unique
* `_` allowed, `-` disallowed

---

### Capabilities

A capability is a **named optional API surface**.

* Format: `domain.feature` (e.g. `worlds.fog`)
* Validated for uniqueness
* Used for conflict detection and future gating
* Metadata-only in Phase 1

---

## Core API Shape (Phase 1)

Phase 1 defines **structure only**, not behavior.

```ts
tapestry = {
  worlds: {},
  worldgen: {},
  events: {},
  mods: {},
  core: {
    phases,
    capabilities
  }
}
```

* Domains exist even if empty
* Domains are extensible only during REGISTRATION
* Domains are immutable after FREEZE
* No `.ext.` namespace exists

---

## TypeScript Runtime Model (Phase 1)

### Engine

* **GraalVM Polyglot JavaScript**

### Rules

* TS runtime loads **only after FREEZE**
* TS receives a **read-only, frozen API object**
* No TS mod code executes at load time
* All mod logic must be routed through explicit lifecycle hooks

---

### TS Mod Contract (Conceptual)

```ts
tapestry.mod.define({
  onLoad(api) {},
  onEnable(api) {}
});
```

* `onLoad` → `TS_READY`
* `onEnable` → `RUNTIME`

Top-level side effects are discouraged and may be blocked.

---

## Error Handling Policy

* Phase violations → **hard fail**
* Invalid descriptors → **hard fail**
* Extension conflicts → **hard fail**
* Partial startup is never allowed

Failing fast is preferred over ambiguous runtime behavior.

---

## Testing Strategy (Phase 1)

* Unit-test phase machine (valid + invalid transitions)
* Unit-test extension discovery & registration
* Unit-test API freeze enforcement
* Verify TS runtime loads only after FREEZE
* Verify no TS logic executes before TS_READY

Minecraft gameplay is **not required** for Phase 1 tests.

---

## Phase 1 Non-goals

Phase 1 explicitly does **not** include:

* worldgen hooks
* block placement
* dimension logic
* gameplay events
* persistence
* TS mod discovery from disk

These are layered on **after** the lifecycle spine is proven correct.

---

## Guiding Principle

> **If the framework does not know what phase it is in, it is already broken.**

Tapestry’s value comes from enforcing *when* things may happen, not just *how*.

---

## Final Note to Contributors

If you find yourself thinking:

> “This would be easier if we just allowed this earlier…”

Stop.

That friction is intentional.
It exists to prevent entire classes of bugs that only appear weeks later.

