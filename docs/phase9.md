# 🧱 Phase 9 — Persistence & Save Lifecycle

## Purpose

Introduce a **formal persistence layer** to Tapestry that:

* Allows mods to store durable state
* Survives server restarts
* Integrates cleanly with world lifecycle
* Preserves phase guarantees
* Remains sandbox-safe

This is **server-side and client-side**. Each environment has its own store.

No cross-environment sync.

---

# 🧭 Design Principles

1. **Persistence is explicit**

   * Mods must declare they want persistence.
   * No hidden magic saves.

2. **Persistence is scoped**

   * Per-mod isolation.
   * No cross-mod shared storage unless explicitly allowed.

3. **Lifecycle is deterministic**

   * Load before RUNTIME.
   * Save during shutdown or world save event.
   * No lazy disk I/O during runtime.

4. **Fail-fast**

   * Corrupt state → clear error.
   * Schema mismatch → clear error.

5. **No implicit schema migrations (yet)**

   * That’s Phase 9.5 or later.

---

# 🏛 Architectural Overview

Introduce:

```
PersistenceService
    ↳ ModStateStore
    ↳ StorageBackend
```

And a new API namespace:

```
tapestry.persistence.*
```

---

# 🔁 Lifecycle Integration

We introduce a new internal phase:

```
PERSISTENCE_READY
```

**PERSISTENCE_READY is always present** in the lifecycle, even if:

* No mods use persistence
* No persistence calls are made
* Client-only environment
* Server-only environment

The phase becomes a no-op if nothing is registered.

Updated lifecycle:

```
BOOTSTRAP
DISCOVERY
TS_READY
PERSISTENCE_READY   ← NEW (always executed)
RUNTIME
SHUTDOWN
```

---

# 📦 What Happens When?

### During SERVER_STARTED:

1. Discover all mods.
2. Initialize persistence backend.
3. Load each mod’s saved state.
4. Inject state into runtime context.
5. Advance to `PERSISTENCE_READY`.
6. Then advance to `RUNTIME`.

---

# 📂 Storage Model

## Storage Location

### On Server

World-scoped:

```
<world>/data/tapestry/<modId>.json
```

### On Client

Game instance scoped:

```
.minecraft/config/tapestry/<modId>.json
```

**No cross-read between client and server stores.**

If a mod wants unified data, it must explicitly proxy through the server.

Example:

```
world/data/tapestry/twila.json
.minecraft/config/tapestry/twila.json
```

---

## File Format

Use JSON with reserved version field for future migrations:

```json
{
  "__version": 1,
  "data": {
    "key1": "value1",
    "key2": 42
  }
}
```

Why JSON?

* Human-readable
* Easy debugging
* No binary corruption surprises
* Aligns with TS ecosystem

**Note:** The `__version` field is reserved but not used in Phase 9.

---

# 🧩 Persistence API (TS Side)

Introduce:

```ts
tapestry.persistence.get(key)
tapestry.persistence.set(key, value)
tapestry.persistence.delete(key)
tapestry.persistence.keys()
```

This is **per-mod scoped automatically**.

Mods cannot access other mod namespaces.

---

## Example Usage

```ts
const count = tapestry.persistence.get("lookCount") ?? 0;
tapestry.persistence.set("lookCount", count + 1);
```

---

# 🛡 Data Model Rules

Allowed value types:

* number
* string
* boolean
* null
* arrays (recursive)
* plain objects (recursive)

Disallowed:

* functions
* circular references
* ProxyObjects
* native Java objects

Enforce deep validation before write.

---

# 🧠 Java Side Architecture

## 1️⃣ PersistenceService

Responsibilities:

* Manage disk IO
* Cache mod states in memory
* Provide per-mod Map<String, Object>
* Serialize on save

Internal structure:

```java
Map<String, Map<String, Object>> modState;
```

---

## 2️⃣ StorageBackend

Encapsulates:

* Load from disk
* Save to disk
* JSON serialization/deserialization

You may use:

* Gson (already common in Fabric ecosystem)

---

## 3️⃣ ModStateStore

Thin wrapper:

```java
class ModStateStore {
    Object get(String key);
    void set(String key, Object value);
    void delete(String key);
    Set<String> keys();
}
```

Injected into runtime per mod.

---

# 🔄 Save Triggers

PersistenceService should save when:

* `ServerLifecycleEvents.SERVER_STOPPING`
* Optionally `ServerWorldEvents.SAVE`

Start minimal:

Save on SERVER_STOPPING only.

Later you can add autosave interval.

---

# 🧪 Error Handling Rules

**Strict fail-fast, no automatic recovery.**

1. Invalid JSON → fail startup, log exact file path and parse error.
2. Corrupt file → fail startup, log exact file path.
3. Type mismatch during serialization → throw immediately.
4. Write failure → log and fail server shutdown cleanly.

**No silent recovery, no auto-delete, no backups.**

User workflow for corrupt files:

* Delete the file manually
* Or fix it manually
* Or uninstall mod

Corruption must be visible and explicit.

---

# 🧱 Runtime Injection Model

At runtime context creation:

Inject:

```java
context.put("persistence", new PersistenceApi(modId));
```

This ensures strict scoping.

---

# 🚫 What Phase 9 Does NOT Include

* Schema migrations (version field reserved only)
* Active versioning state format
* Distributed sync
* Player-scoped persistence
* Cross-environment data sync
* Compression
* Encryption
* Concurrency primitives
* Transactions
* Memory limits or quotas
* Lazy loading

Keep it simple.

---

# 📋 Acceptance Criteria

Phase 9 complete when:

* [ ] State survives server restart.
* [ ] Separate mod files are created.
* [ ] No cross-mod leakage possible.
* [ ] No cross-environment leakage possible.
* [ ] Corrupt file fails fast with clear error.
* [ ] JSON matches in-memory structure.
* [ ] Phase ordering respected (PERSISTENCE_READY always executed).
* [ ] No disk IO during RUNTIME calls.
* [ ] Single-thread guarantee documented.
* [ ] Version field reserved in JSON format.

---

# 🧭 Future Extension Points

Design now to allow:

* Version field in JSON file
* Migration hooks
* Per-player scoped storage
* Async save batching
* Config vs runtime state separation

---

# 🔥 Key Design Decision

Persistence should feel like:

> A durable extension of mod memory.

Not:

> A file system API.

Keep abstraction high-level.

---

# 🏁 Implementation Order

1. Design PersistenceService
2. Add new phase enum
3. Integrate server lifecycle hook
4. Implement load logic
5. Implement save logic
6. Expose TS API
7. Add validation
8. Test restart cycle

---

# 🧠 Concurrency Model

**Single-thread bound by design.**

Persistence API must only be called from Tapestry runtime thread.

No transactions.
No atomic multi-key updates.
No concurrency primitives.

If a mod needs atomic logic:

```ts
const a = get("a");
const b = get("b");
set("a", newValue);
set("b", newValue);
```

Within same tick.

---

# 💾 Memory Management

**Full in-memory load, no limits.**

No size limits.
No quotas.
No eviction.
No lazy loading.

Load full mod state at `PERSISTENCE_READY`.
Keep in memory.
Write full file on shutdown.

If a mod stores excessive data, it will crash or freeze - that is their responsibility.

---

# 🏛 Finalized Architecture Decisions

## Lifecycle
`PERSISTENCE_READY` is always executed, becomes no-op if unused.

## Scope
* Server → world-scoped (`<world>/data/tapestry/`)
* Client → instance-scoped (`.minecraft/config/tapestry/`)
* No cross-environment coupling

## Storage Format
JSON per mod with reserved `__version` field.

## Failure Mode
Strict fail-fast, no automatic recovery, explicit user intervention required.

## Concurrency
Single-thread bound, no transactions, no atomic batching.

## Memory
Full in-memory load, no artificial limits.
