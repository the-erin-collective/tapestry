---
title: Getting Started
description: Learn how to install and use Tapestry to build TypeScript-first Minecraft mods
---

# Getting Started

Tapestry is a TypeScript-first modding framework for Minecraft that provides an explicit lifecycle model with fail-fast enforcement. This guide will help you get started with building your first Tapestry mod.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit 17 or later** - Required for running Minecraft and the Tapestry framework
- **Gradle** - Build tool (wrapper included in the project)
- **GraalVM** - Powers the TypeScript runtime (version specified in `gradle.properties`)

## Installation

### 1. Clone the Repository

```sh
git clone https://github.com/your-org/tapestry.git
cd tapestry
```

### 2. Build the Project

Clean build the project to ensure all dependencies are resolved:

```sh
./gradlew clean build
```

This command will compile the core framework, run tests, and produce artifacts in `build/libs`.

### 3. Run a Development Instance

Launch a Minecraft client or server with Tapestry loaded:

```sh
# Launch Minecraft client
./gradlew runClient

# Or launch a dedicated server
./gradlew runServer
```

Your mods should be placed in the `run/mods` directory.

## Basic Usage

### Creating a TypeScript Mod

Create a TypeScript mod file that uses the Tapestry API:

```ts
// my-mod.ts
tapestry.mod.define({
  onLoad(api) {
    console.log("Hello from my Tapestry mod!");
    
    // Your mod logic here
    // This runs during the TS_READY phase
  }
});
```

The `onLoad` hook is called during the `TS_READY` phase, after the API has been frozen and your mod's dependencies have been resolved.

### Client vs Server Detection

Tapestry provides flags to detect which side your code is running on:

```ts
tapestry.mod.define({
  onLoad(api) {
    if (tapestry.isClient) {
      console.log("Running on client");
      // Client-only logic
    }
    
    if (tapestry.isServer) {
      console.log("Running on server");
      // Server-only logic
    }
  }
});
```

### Creating a Java Extension

You can extend Tapestry's capabilities by creating Java extensions:

```java
public class MyExtension implements TapestryExtensionProvider {
    @Override
    public TapestryExtensionDescriptor describe() {
        return new TapestryExtensionDescriptor("myext", List.of("rpc"));
    }
    
    @Override
    public void register(TapestryExtensionContext ctx) {
        ctx.registerRpcEndpoint("myapi", (ctx, data) -> {
            return JsonValue.of("pong");
        });
    }
}
```

Extensions are discovered during the `DISCOVERY` phase and registered during the `REGISTRATION` phase.

## Understanding the Lifecycle

Tapestry enforces a strict phase model to ensure mods interact with the game at the correct time. The key phases are:

- **BOOTSTRAP** - Core initialization
- **DISCOVERY** - Extension scanning
- **REGISTRATION** - API domain declaration
- **FREEZE** - API shape is sealed
- **TS_LOAD** - GraalVM starts, scripts loaded
- **TS_REGISTER** - Mod registration and metadata collection
- **TS_ACTIVATE** - Dependency resolution
- **TS_READY** - `onLoad` hooks execute
- **RUNTIME** - Game logic and events active

Every API call checks the current phase and throws an error if called at the wrong time. This fail-fast approach helps you catch bugs early.

## Next Steps

Now that you have Tapestry installed and understand the basics, explore these topics:

- [Core Concepts](/guide/core-concepts) - Deep dive into Tapestry's architecture
- [Lifecycle Phases](/guide/lifecycle-phases) - Detailed phase model documentation
- [Extension System](/guide/extensions) - Learn how to extend Tapestry
- [API Reference](/api/) - Complete API documentation

## Getting Help

If you encounter issues or have questions:

- Check the [Architecture Guide](/guide/architecture) for system design details
- Review the [API Reference](/api/) for detailed function documentation
- Open an issue on [GitHub](https://github.com/your-org/tapestry)
