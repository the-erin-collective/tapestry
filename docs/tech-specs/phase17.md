# 📜 Tapestry Phase 17 — Graal Isolation Spec

## 🎯 Goals

1. Prevent arbitrary Java access from JS.
2. Prevent reflection / Runtime execution.
3. Prevent filesystem access.
4. Prevent class lookup.
5. Ensure only Tapestry-exposed APIs are callable.
6. Keep complexity minimal.
7. Preserve performance.

---

# 1️⃣ Threat Model

Without isolation, JS can do:

```js
Java.type("java.lang.Runtime")
Java.type("java.io.File")
Java.type("net.minecraft.server.MinecraftServer")
```

Which enables:

* OS command execution
* File reading
* Full server control
* Reflection attacks

Phase 17 must eliminate this capability completely.

---

# 2️⃣ Graal Context Hardening

## 🔒 2.1 Context Builder Requirements

When creating the Graal `Context`, you MUST use:

```java
Context.newBuilder("js")
    .allowHostAccess(HostAccess.NONE)
    .allowHostClassLookup(className -> false)
    .allowIO(false)
    .allowCreateThread(false)
    .allowNativeAccess(false)
    .allowEnvironmentAccess(EnvironmentAccess.NONE)
    .build();
```

### Required Flags Explained

| Flag                        | Why                               |
| --------------------------- | --------------------------------- |
| HostAccess.NONE             | Prevent JS calling arbitrary Java |
| allowHostClassLookup(false) | Blocks Java.type()                |
| allowIO(false)              | No file system                    |
| allowCreateThread(false)    | No background thread abuse        |
| allowNativeAccess(false)    | No native library abuse           |
| EnvironmentAccess.NONE      | No env variable access            |

---

## 🔒 2.2 Do NOT Enable These

Never enable:

```java
.allowAllAccess(true)
.allowHostAccess(HostAccess.ALL)
.allowIO(true)
```

These undo isolation.

---

# 3️⃣ Explicit API Surface Exposure

Isolation means:

JS gets nothing by default.

You must explicitly expose a minimal bridge.

---

## 3.1 Exposed Global Object

After building context:

```java
Value bindings = context.getBindings("js");
bindings.putMember("tapestry", new SafeTapestryBridge(...));
```

JS can now access:

```js
tapestry.call("modid:method", args)
tapestry.subscribe("modid:event")
```

And nothing else.

---

## 3.2 Safe Bridge Requirements

The bridge object must:

* Be a simple POJO
* Expose only annotated methods
* Return only sanitized JSON-compatible values
* Not expose Minecraft classes
* Not expose ServerPlayerEntity
* Not expose Context

Example:

```java
public final class SafeTapestryBridge {

    public Object call(String id, Map<String, Object> args) {
        return rpcClient.call(id, args);
    }

    public void subscribe(String event) {
        rpcClient.subscribe(event);
    }
}
```

---

# 4️⃣ Remove All Implicit Java Access

Verify that JS cannot use:

```js
Java.type
Polyglot.import
Polyglot.eval
Packages
```

With `HostAccess.NONE` and `allowHostClassLookup(false)` these are disabled.

---

# 5️⃣ Execution Constraints

Optional but recommended:

### 5.1 Execution Time Limit (Soft)

Wrap script execution:

```java
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<?> future = executor.submit(() -> context.eval(...));

future.get(50, TimeUnit.MILLISECONDS);
```

If timeout:

* Cancel execution
* Destroy context

This prevents infinite loops.

If you want minimal complexity, skip for now.

---

# 6️⃣ Context Lifecycle Rules

Define clear lifecycle.

---

## 6.1 Per-Mod Context

Each mod gets its own:

```
Context
Bindings
Subscription set
Pending RPC map
```

Never share contexts across mods.

This prevents cross-mod memory bleed.

---

## 6.2 Destroy Context On

* Player disconnect (client-side runtime)
* Mod unload
* Server shutdown

Call:

```java
context.close(true);
```

---

# 7️⃣ Prevent Object Leakage Back Into JS

All values returned to JS must be:

* Sanitized JSON-compatible
* No Java object references
* No functional interfaces
* No lazy wrappers

Never return:

```java
ServerPlayerEntity
MinecraftServer
World
```

If you do, isolation collapses.

---

# 8️⃣ Validation Checklist (Phase 17 Completion Criteria)

Before marking complete, verify:

### Attempt These From JS

```js
Java.type("java.lang.System")
```

Should throw error.

```js
new java.io.File("test")
```

Should fail.

```js
Polyglot.import("java.lang.Runtime")
```

Should fail.

```js
tapestry.call("core:internalMethod")
```

Should be rejected by namespace rules.

If any succeed → isolation incomplete.

---

# 9️⃣ Performance Impact

Isolation adds:

* No measurable runtime overhead
* No additional serialization
* No additional allocations

It is mostly configuration-level.

Complexity increase is low.

---

# 10️⃣ What Phase 17 Does NOT Do

This is important.

Phase 17 does NOT:

* Sandboxed CPU usage
* Sandboxed memory limits
* Sandboxed infinite loops (unless you add timeout)
* Sandboxed async promises

It isolates Java access only.

That’s the critical boundary.

---

# 11️⃣ Minimal Implementation Path (Lean Mode)

To avoid complexity creep:

Implement ONLY:

* Hardened Context builder
* Explicit bridge object
* Per-mod context
* Context destruction on disconnect

Skip:

* Execution timeouts
* Memory caps
* Advanced polyglot config

Add those later if needed.

---

# 12️⃣ Architectural Impact

After Phase 17:

Before:

```
JS ↔ Java (potentially unsafe)
```

After:

```
JS → SafeBridge → RPC → Sanitized Dispatcher → Java
```

This completes your boundary chain:

* Network boundary (Phase 16)
* Namespace boundary
* Data boundary
* Execution boundary (Phase 17)

At that point, Tapestry is a real mod platform.

---

# 🧠 Complexity vs Value

| Feature               | Complexity | Security Gain  |
| --------------------- | ---------- | -------------- |
| Namespace validation  | Low        | High           |
| Argument sanitization | Medium     | Very High      |
| Graal isolation       | Low        | Extremely High |
| Permission tiers      | Medium     | Medium         |

Graal isolation gives the highest safety gain per line of code.

---

# 🏁 Phase 17 Definition of Done

You can mark Phase 17 complete when:

* JS cannot access arbitrary Java
* Only `tapestry` object is exposed
* All RPC calls go through sanitized boundary
* Contexts are not shared
* Contexts are closed properly

---