# Tapestry

Tapestry is a **TypeScript‑first modding framework** built as a wrapper around
a curated subset of the Fabric API. It uses Fabric entrypoints to hook into
Minecraft's startup, then imposes a strict, enforced phase machine so that mods
can only interact with the underlying game at the correct time. A GraalVM
type‑safe JavaScript runtime powers TypeScript mods, giving authors a familiar
language surface while the Java core mediates access and enforces safety.

Under the hood:

* Fabric provides the vanilla game integration and networking.
* Tapestry’s Java core offers lifecycle control, registry/extension services,
  and RPC/overlay/persistence helpers.
* A small GraalVM context executes mod code on both client and server,
  communicating with the host via a JSON‑RPC bridge.

The result is a predictable environment for building client‑only, server‑only,
or combined mods in TypeScript, with built‑in protections against cross‑phase
or cross‑mod violations.

> 💡 *If the framework ever does not know what phase it is in, it is already
> broken.*

---

## Table of Contents

1. [Features](#features)
2. [Getting Started](#getting-started)
3. [Building & Running](#building--running)
4. [Testing](#testing)
5. [Documentation](#documentation)
6. [Contributing](#contributing)
7. [License & Contact](#license--contact)

---

## Features

* **Explicit lifecycle phases** (`BOOTSTRAP`, `DISCOVERY`, `VALIDATION`, `REGISTRATION`, `FREEZE`, `TS_LOAD`, `TS_REGISTER`, `TS_ACTIVATE`, `TS_READY`, `PERSISTENCE_READY`, `EVENT`, `RUNTIME`, `CLIENT_PRESENTATION_READY`)
* **Fail‑fast enforcement** on illegal operations
* **Additive‑only extension model**; API shape freezes before runtime
* **TypeScript runtime** with a frozen, read-only host API (GraalVM)
* **Capability declarations** and dependency validation
* Safety checks for RPC, overlays, persistence, etc., built into the core

### Example usage

#### TypeScript
```ts
// in a TS mod file
tapestry.mod.define({
  onLoad(api) {
    console.log("hello from my mod!");
  }
});
```

#### Java extension
```java
public class MyExtension implements TapestryExtensionProvider {
    public TapestryExtensionDescriptor describe() {
        return new TapestryExtensionDescriptor("myext", List.of("rpc"));
    }
    public void register(TapestryExtensionContext ctx) {
        ctx.registerRpcEndpoint("myapi", (ctx, data) -> {
            return JsonValue.of("pong");
        });
    }
}
```

The current codebase implements the core lifecycle, extension system, and
runtime plumbing; higher‑level gameplay features are built atop this foundation.

---

## System Overview

### Lifecycle Phases

Tapestry divides startup into discrete phases:

* `BOOTSTRAP` – JVM loads, basic core structures init.
* `DISCOVERY` – scan Fabric entrypoints for Tapestry extensions.
* `VALIDATION` – Extensions validated, no mutations allowed.
* `REGISTRATION` – extensions declare new API domains and capabilities.
* `FREEZE` – API shape is sealed; no further mutation allowed.
* `TS_LOAD` – start GraalVM, inject frozen API, load scripts (no execution).
* `TS_REGISTER` – Mod registration, scripts evaluated, metadata collected.
* `TS_ACTIVATE` – Mod activation, dependency resolution.
* `TS_READY` – run `onLoad` hooks, register event listeners, read-only ops.
* `PERSISTENCE_READY` – Persistence layer initialized.
* `EVENT` – Event system active.
* `RUNTIME` – world and player logic may run, events fire normally.
* `CLIENT_PRESENTATION_READY` – Client overlay system initialized (client-side only).

Every API call that touches game state checks the current phase and throws if
illegal. This enforces the mental model that mods can schedule work only in
allowed windows.

### Client / Server Communication

A lightweight JSON‑RPC transport exists between client and server to enable
remote procedure calls and event pushes. Mod authors can register
server‑side RPC handlers and call them from client code (and vice versa) with
automatic handshake and rate‑limiting. The same TypeScript runtime is used on
both sides, allowing shared logic or side‑specific modules.

### Mod Types

Mods may target:

* **Client only** – installable on the player’s machine; cannot affect server
  state.
* **Server only** – runs on dedicated servers; client code isn’t executed.
* **Both** – packages with code executed on both sides using conditional
  guards (`if (tapestry.isClient) ...`).

The framework exposes `tapestry.isClient` / `tapestry.isServer` flags within the
runtime to help authors write portable code.

---



---

## Getting Started

**Prerequisites**

* Java Development Kit 17 or later
* Gradle (wrapper included)
* GraalVM (used by tests and runtime); see `gradle.properties` for the version

### Importing

Open the project as a Gradle project in your IDE. The `:` root project contains
the core and test sources.

### Building

```sh
# clean build prints lint/test results
./gradlew clean build
```

### Running a Development Instance

Use Loom tasks provided by the build:

```sh
./gradlew runClient
./gradlew runServer
```

These launch a Minecraft instance with the current JAR on the classpath. Mods
live under `run/mods`.

---

## Building & Running

Artifacts are produced under `build/libs` as a shaded JAR. The Gradle build
handles Minecraft mappings via Loom; you generally do not need to modify the
buildscript.

Release builds use `./gradlew shadowJar` and publish to the configured
repository in `build.gradle`.

---

## Testing

Unit tests (JUnit) cover the core lifecycle, phase checks, extension registry,
and runtime‑side helpers. They do **not** require a Minecraft client.

```sh
./gradlew test
```

Integration tests that exercise Fabric or the JS engine can be invoked with
`./gradlew integrationTest` when they are added.

---

## Documentation

All current architectural and behavioral documentation lives in
`docs/topics/`. These topic‑oriented Markdown files each describe a domain
(lifecycle, RPC & networking, client overlays, persistence, etc.) and include
example snippets showing real‑world usage patterns. Read them before
modifying core logic.

The topic docs replace the old phase‑plan material; the old spec files have
been deleted.

---

## Contributing

1. Fork the repository and create a feature branch.
2. Follow the existing Java/TS code style (`.editorconfig`).
3. Add or update unit tests for any behavior changes.
4. Keep topic docs in sync with implementation changes.
5. Submit a pull request with a clear description and link to any related
   docs or issue.

We welcome new ideas, but please respect the lifecycle guarantees; they are
the core value proposition of Tapestry.

---

## License & Contact

This project is licensed under the **GNU Affero General Public License v3.0
(AGPL‑3.0)**. See the `LICENSE` file for the full text and compliance notes.

Have questions or run into issues? Open an issue on GitHub.

