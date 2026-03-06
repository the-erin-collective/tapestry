---
layout: home

hero:
  name: "Tapestry"
  text: "TypeScript-First Modding Framework for Minecraft"
  tagline: "A structured, type-safe modding platform that enforces lifecycle safety and brings predictable development to Minecraft mods."
  image:
    src: /banner.jpg
    alt: Tapestry Banner
  actions:
    - theme: brand
      text: Getting Started
      link: /guide/getting-started
    - theme: alt
      text: API Reference
      link: /api/
    - theme: alt
      text: View on GitHub
      link: https://github.com/alizzycraft/tapestry

features:
  - title: Explicit Lifecycle Phases
    details: >
      Tapestry divides mod startup into discrete, enforced phases. Every API call validates the current phase and throws immediately if used incorrectly. This prevents timing-related bugs and ensures mods interact with Minecraft only when it is safe to do so.

  - title: Capability-Driven Extensions
    details: >
      Extensions declare capabilities and dependencies rather than exposing a monolithic API. This allows safe integration between systems and prevents cross-mod violations through a structured capability model.

  - title: TypeScript-First Development
    details: >
      Write mods in TypeScript with full type safety. A GraalVM runtime executes your code while the Java core enforces safety checks. Modern tooling and familiar syntax meet runtime protection.

  - title: Frozen Host API
    details: >
      The host API is sealed during startup and becomes immutable before mod code runs. This guarantees a stable runtime surface and prevents unexpected mutations during execution.

  - title: Deterministic Execution
    details: >
      Predictable execution ordering and phase transitions eliminate timing-related bugs common in traditional modding. Systems initialize in a known order with well-defined API availability.

  - title: Built for Complex Mods
    details: >
      Explicit lifecycle stages and capability declarations allow large mods to remain maintainable as they grow. Structured initialization prevents the fragility that emerges from ad-hoc ordering.
---

## Why Tapestry?

Minecraft modding has grown organically over many years, but this flexibility introduces structural problems:

- **Initialization order becomes unclear** – hooks fire in unpredictable sequences
- **Systems depend on fragile timing assumptions** – mods break when load order changes
- **APIs mutate during startup** – no guarantee of a stable surface
- **Large mods become difficult to maintain** – interdependencies grow tangled

Tapestry solves these problems by enforcing a **strict, explicit lifecycle model**.

Instead of loosely ordered hooks, Tapestry defines clear execution phases and **prevents mods from performing operations outside their allowed window**. This creates a predictable environment where:

- Systems initialize in a **known order**
- APIs become available at **well-defined phases**
- Illegal operations **fail immediately**
- Runtime behavior remains **deterministic**

> 💡 *If the framework ever does not know what phase it is in, it is already broken.*

---

## How It Works

### The Architecture Stack

```
Minecraft
   ↓
Fabric API (vanilla integration & networking)
   ↓
Tapestry Java Core (lifecycle control, registries, RPC)
   ↓
GraalVM TypeScript Runtime (safe mod execution)
   ↓
Your TypeScript Mods
```

Each layer has a clear responsibility:

**Fabric** provides vanilla integration and networking primitives.

**Tapestry Core** controls lifecycle phases, validates capabilities, exposes the host API, and enforces safety checks.

**GraalVM Runtime** executes mod code and communicates with the host through a JSON-RPC bridge.

**Your Mods** interact only with the safe API surface provided by Tapestry.

### A Minimal Example

```ts
tapestry.mod.define({
  onLoad(api) {
    console.log("Hello from Tapestry!");
  }
});
```

Mods interact with Tapestry through a **frozen host API** that exposes capabilities such as RPC, overlays, persistence, and events—all while enforcing lifecycle restrictions.

---

## Core Features

### Explicit Lifecycle Phases

Tapestry divides execution into discrete phases, each with specific allowed operations:

| Phase | Purpose |
|-------|---------|
| `BOOTSTRAP` | JVM loads, core structures initialize |
| `DISCOVERY` | Scan Fabric entrypoints for Tapestry extensions |
| `VALIDATION` | Extensions validated, no mutations allowed |
| `REGISTRATION` | Extensions declare API domains and capabilities |
| `FREEZE` | API shape sealed; no further mutation |
| `TS_LOAD` | GraalVM starts, frozen API injected, scripts loaded |
| `TS_REGISTER` | Mod registration, scripts evaluated, metadata collected |
| `TS_ACTIVATE` | Mod activation, dependency resolution |
| `TS_READY` | `onLoad` hooks run, event listeners registered |
| `PERSISTENCE_READY` | Persistence layer initialized |
| `EVENT` | Event system active |
| `RUNTIME` | World and player logic may run |
| `CLIENT_PRESENTATION_READY` | Client overlay system initialized (client-side only) |

Every API call checks the current phase and throws if illegal.

### Mod Types

Mods may target:

- **Client only** – installable on the player's machine; cannot affect server state
- **Server only** – runs on dedicated servers; client code isn't executed
- **Both** – packages with code executed on both sides using conditional guards (`if (tapestry.isClient) ...`)

The framework exposes `tapestry.isClient` and `tapestry.isServer` flags to help authors write portable code.

### Client / Server Communication

A lightweight JSON-RPC transport enables remote procedure calls and event pushes between client and server. Mod authors register server-side RPC handlers and call them from client code (and vice versa) with automatic handshake and rate-limiting.

---

## Getting Started

### Prerequisites

- Java Development Kit 17 or later
- Gradle (wrapper included)
- GraalVM (see `gradle.properties` for the version)

### Importing

Open the project as a Gradle project in your IDE. The `:` root project contains the core and test sources.

### Building

```sh
./gradlew clean build
```

A clean build prints lint and test results.

### Running a Development Instance

Use Loom tasks provided by the build:

```sh
./gradlew runClient
./gradlew runServer
```

These launch a Minecraft instance with the current JAR on the classpath. Mods live under `run/mods`.

---

## Testing

Unit tests (JUnit) cover the core lifecycle, phase checks, extension registry, and runtime-side helpers. They do **not** require a Minecraft client.

```sh
./gradlew test
```

Integration tests that exercise Fabric or the JS engine can be invoked with `./gradlew integrationTest` when they are added.

---

## Design Philosophy

Tapestry is built around a few core principles:

**Explicit Structure**  
Lifecycle phases and capability declarations replace implicit ordering rules.

**Fail Fast**  
Illegal operations throw immediately instead of corrupting game state.

**Determinism**  
Execution order and runtime transitions are predictable and consistent.

**Stable API Surface**  
The host API freezes before mod code runs, preventing unexpected mutation.

**Modularity**  
Capabilities allow independent systems to integrate safely without tight coupling.

---

## What Tapestry Is (and Isn't)

Tapestry is **not a replacement for Fabric**. Fabric still provides the integration with Minecraft.

Instead, Tapestry adds a **structured runtime layer** on top of Fabric that enforces lifecycle safety and organizes how mods interact with the game.

The goal is simple:

> Make complex mods easier to build, reason about, and maintain.

---

## Learn More

**New to Tapestry?** Start with the guide:

→ **[Getting Started Guide](/guide/getting-started)**

**Ready to build?** Explore the API:

→ **[API Reference](/api/)**

**Want to understand the architecture?** Read the deep dive:

→ **[Architecture Guide](/guide/architecture)**

---

## Contributing

1. Fork the repository and create a feature branch
2. Follow the existing Java/TS code style (`.editorconfig`)
3. Add or update unit tests for any behavior changes
4. Keep topic docs in sync with implementation changes
5. Submit a pull request with a clear description

We welcome new ideas, but please respect the lifecycle guarantees—they are the core value proposition of Tapestry.

---

## License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL‑3.0)**. See the `LICENSE` file for the full text and compliance notes.

Have questions or run into issues? Open an issue on GitHub.
