---
title: Extension System
description: Learn how to build Java extensions to add new capabilities to the Tapestry framework
---

# Extension System

Tapestry's extension system allows you to add new capabilities to the framework through Java components. Extensions are discovered during the `DISCOVERY` phase, validated during `VALIDATION`, and registered during the `REGISTRATION` phase. This guide explains how to build extensions that integrate seamlessly with Tapestry's lifecycle model.

## What Are Extensions?

Extensions are Java components that extend Tapestry's functionality by:

- **Adding new APIs** - Expose Java functionality to TypeScript mods
- **Registering hooks** - Allow mods to respond to game events
- **Providing services** - Offer Java services to other extensions
- **Exporting types** - Share TypeScript type definitions with mods

Extensions follow an **additive-only model**: they can only add capabilities, never remove or modify existing ones. This ensures that mods can safely depend on the API shape remaining stable after the `FREEZE` phase.

## Creating an Extension

### Step 1: Implement TapestryExtensionProvider

Create a class that implements the `TapestryExtensionProvider` interface:

```java
package com.example.myextension;

import com.tapestry.extensions.*;
import java.util.List;
import java.util.Optional;

public class MyExtensionProvider implements TapestryExtensionProvider {
    
    @Override
    public TapestryExtensionDescriptor describe() {
        return new TapestryExtensionDescriptor(
            "my_extension",              // Unique ID (snake_case)
            "My Extension",              // Display name
            "1.0.0",                     // Extension version
            "0.1.0",                     // Minimum Tapestry version
            List.of(                     // Capabilities this extension provides
                new CapabilityDecl(
                    "my_extension.greet",
                    CapabilityType.API,
                    false,
                    Map.of(),
                    "tapestry.mods.my_extension.greet"
                )
            ),
            List.of(),                   // Required extension dependencies
            List.of(),                   // Required capabilities
            Optional.empty(),            // Type export entry (optional)
            List.of()                    // Type imports (optional)
        );
    }
    
    @Override
    public TapestryExtension create() {
        return new MyExtensionInstance();
    }
}
```

### Step 2: Implement TapestryExtension

Create the extension instance that performs the actual registration:

```java
package com.example.myextension;

import com.tapestry.extensions.*;
import com.tapestry.runtime.ProxyExecutable;

public class MyExtensionInstance implements TapestryExtension {
    
    @Override
    public void register(TapestryExtensionContext ctx) {
        // Register API function
        ctx.api().addFunction(
            ctx.extensionId(),
            "my_extension.greet",
            new ProxyExecutable() {
                @Override
                public Object execute(Object... args) {
                    String name = (String) args[0];
                    return "Hello, " + name + "!";
                }
            }
        );
        
        ctx.log().info("My Extension registered successfully");
    }
    
    @Override
    public void onFreeze() {
        // Optional: Perform any finalization before API freeze
    }
}
```

### Step 3: Register with Fabric

Add your extension provider to `fabric.mod.json`:

```json
{
  "entrypoints": {
    "tapestry:extension": [
      "com.example.myextension.MyExtensionProvider"
    ]
  }
}
```

Tapestry will discover your extension during the `DISCOVERY` phase via Fabric's entrypoint system.

## Extension Descriptor

The `TapestryExtensionDescriptor` defines your extension's metadata and capabilities:

### Extension ID

The unique identifier for your extension. Must be:
- Lowercase with underscores (snake_case)
- Start with a letter
- Contain only letters, numbers, and underscores

```java
"my_extension"  // ✓ Valid
"MyExtension"   // ✗ Invalid (uppercase)
"123_ext"       // ✗ Invalid (starts with number)
```

### Display Name

Human-readable name shown in logs and error messages:

```java
"My Extension"
"Infinite Dimensions"
"World Generation Plus"
```

### Version Requirements

Specify your extension version and minimum Tapestry version:

```java
"1.0.0",      // Extension version (semantic versioning)
"0.1.0"       // Minimum Tapestry version required
```

Tapestry validates version compatibility during the `VALIDATION` phase.

### Capabilities

Capabilities define what your extension adds to Tapestry. Each capability has:

- **name** - Unique capability identifier (e.g., `"my_extension.greet"`)
- **type** - Capability type: `API`, `HOOK`, or `SERVICE`
- **exclusive** - Whether only one extension can provide this capability
- **meta** - Optional metadata map
- **apiPath** - For API capabilities: where the function appears in JavaScript

```java
new CapabilityDecl(
    "my_extension.greet",           // Capability name
    CapabilityType.API,             // Type: API function
    false,                          // Not exclusive
    Map.of("category", "utility"),  // Optional metadata
    "tapestry.mods.my_extension.greet"  // JavaScript path
)
```

### Dependencies

Declare dependencies on other extensions:

```java
List.of("core_lib", "utility_ext")  // Required extensions
```

Tapestry validates that all dependencies are present and resolves the activation order during `VALIDATION`.

### Capability Requirements

Declare required capabilities from other extensions:

```java
List.of("persistence.state", "events.tick")  // Required capabilities
```

This allows your extension to depend on specific features without depending on entire extensions.

## Capability Types

### API Capabilities

API capabilities expose Java functions to TypeScript mods:

```java
new CapabilityDecl(
    "my_extension.calculate",
    CapabilityType.API,
    false,
    Map.of(),
    "tapestry.mods.my_extension.calculate"
)
```

Register the function during `REGISTRATION`:

```java
@Override
public void register(TapestryExtensionContext ctx) {
    ctx.api().addFunction(
        ctx.extensionId(),
        "my_extension.calculate",
        new ProxyExecutable() {
            @Override
            public Object execute(Object... args) {
                int a = (int) args[0];
                int b = (int) args[1];
                return a + b;
            }
        }
    );
}
```

Mods can call this function after `TS_READY`:

```ts
tapestry.mod.define({
  onLoad(api) {
    const result = tapestry.mods.my_extension.calculate(5, 3);
    console.log(`Result: ${result}`); // Result: 8
  }
});
```

### Hook Capabilities

Hook capabilities allow mods to respond to events:

```java
new CapabilityDecl(
    "my_extension.onCustomEvent",
    CapabilityType.HOOK,
    false,
    Map.of("phase", "runtime"),
    null  // Hooks don't need apiPath
)
```

Register the hook bridge during `REGISTRATION`:

```java
@Override
public void register(TapestryExtensionContext ctx) {
    ctx.hooks().registerBridge(
        ctx.extensionId(),
        "my_extension.onCustomEvent",
        (callback) -> {
            // Store callback and invoke it when event occurs
            eventCallbacks.add(callback);
        }
    );
}
```

Mods can register handlers:

```ts
tapestry.mod.define({
  onLoad(api) {
    api.hooks.on('my_extension.onCustomEvent', (data) => {
      console.log('Custom event fired:', data);
    });
  }
});
```

### Service Capabilities

Service capabilities provide Java services to other extensions:

```java
new CapabilityDecl(
    "my_extension.data_processor",
    CapabilityType.SERVICE,
    true,  // Exclusive: only one extension can provide this
    Map.of("interface", "com.example.DataProcessor"),
    null
)
```

Register the service during `REGISTRATION`:

```java
@Override
public void register(TapestryExtensionContext ctx) {
    ctx.services().register(
        ctx.extensionId(),
        "my_extension.data_processor",
        new DataProcessorImpl()
    );
}
```

Other extensions can access the service:

```java
DataProcessor processor = (DataProcessor) ctx.services()
    .get("my_extension.data_processor");
```

## Extension Lifecycle

Extensions progress through several phases:

![Extension System Flow Diagram](/diagrams/extension-flow.svg)
*Figure 1: Extension system lifecycle flow showing the progression from DISCOVERY through FREEZE phases, including provider instantiation, descriptor validation, extension registration, and API freezing.*

### 1. DISCOVERY Phase

Tapestry discovers extension providers via Fabric entrypoints:

```java
// Your provider is instantiated
MyExtensionProvider provider = new MyExtensionProvider();

// describe() is called to get the descriptor
TapestryExtensionDescriptor descriptor = provider.describe();
```

**Requirements:**
- `describe()` must be **pure** (no side effects)
- Must return a valid descriptor
- Must not access game state or other extensions

### 2. VALIDATION Phase

Tapestry validates all extension descriptors:

- Checks extension IDs are unique and valid
- Validates version requirements
- Resolves dependency graph
- Checks for circular dependencies
- Validates capability declarations
- Ensures required capabilities are available

If validation fails, Tapestry throws an error with details about the problem.

### 3. REGISTRATION Phase

Tapestry calls `create()` to instantiate your extension, then calls `register()`:

```java
// Extension instance is created
TapestryExtension extension = provider.create();

// register() is called with context
extension.register(context);
```

**During registration, you can:**
- Register API functions via `ctx.api()`
- Register hook bridges via `ctx.hooks()`
- Register services via `ctx.services()`
- Access logger via `ctx.log()`

**You cannot:**
- Call registered APIs (not available yet)
- Access game state (not initialized yet)
- Modify other extensions

### 4. FREEZE Phase

Tapestry calls `onFreeze()` on all extensions, then freezes all registries:

```java
extension.onFreeze();
```

After this phase:
- API shape is permanently sealed
- No more capabilities can be registered
- Mods can safely cache API references

## Type Exports

Extensions can export TypeScript type definitions to provide type safety for mods:

### Step 1: Create Type Definition File

Create a `.d.ts` file in your extension:

```typescript
// my-extension.d.ts
declare namespace tapestry.mods.my_extension {
  /**
   * Greets a person by name
   * @param name - The person's name
   * @returns A greeting message
   */
  function greet(name: string): string;
  
  /**
   * Calculates the sum of two numbers
   * @param a - First number
   * @param b - Second number
   * @returns The sum
   */
  function calculate(a: number, b: number): number;
}
```

### Step 2: Declare Type Export

Update your descriptor to export the type file:

```java
new TapestryExtensionDescriptor(
    "my_extension",
    "My Extension",
    "1.0.0",
    "0.1.0",
    capabilities,
    List.of(),
    List.of(),
    Optional.of("my-extension.d.ts"),  // Type export entry
    List.of()
)
```

### Step 3: Import Types in Other Extensions

Other extensions can import your types:

```java
new TapestryExtensionDescriptor(
    "dependent_extension",
    "Dependent Extension",
    "1.0.0",
    "0.1.0",
    capabilities,
    List.of("my_extension"),           // Depends on my_extension
    List.of(),
    Optional.empty(),
    List.of("my_extension")            // Import types from my_extension
)
```

Mods using your extension will get full TypeScript type checking and autocomplete.

## Best Practices

### 1. Keep describe() Pure

The `describe()` method must be pure with no side effects:

```java
// ✓ Good: Pure descriptor
@Override
public TapestryExtensionDescriptor describe() {
    return new TapestryExtensionDescriptor(/* ... */);
}

// ✗ Bad: Side effects in describe()
@Override
public TapestryExtensionDescriptor describe() {
    initializeDatabase();  // Don't do this!
    return new TapestryExtensionDescriptor(/* ... */);
}
```

### 2. Use Semantic Versioning

Follow semantic versioning for your extension:

- **Major** (1.0.0 → 2.0.0): Breaking changes
- **Minor** (1.0.0 → 1.1.0): New features, backward compatible
- **Patch** (1.0.0 → 1.0.1): Bug fixes

### 3. Declare Dependencies Explicitly

Always declare dependencies on other extensions:

```java
List.of("core_lib", "utility_ext")  // Explicit dependencies
```

Don't rely on implicit load order or assume other extensions are present.

### 4. Use Exclusive Capabilities Sparingly

Only mark capabilities as exclusive when truly necessary:

```java
new CapabilityDecl(
    "worldgen.dimension_provider",
    CapabilityType.SERVICE,
    true,  // Only one dimension provider allowed
    Map.of(),
    null
)
```

Exclusive capabilities prevent other extensions from providing the same capability.

### 5. Provide Type Definitions

Always export TypeScript type definitions for your APIs:

```typescript
// Provides autocomplete and type checking for mods
declare namespace tapestry.mods.my_extension {
  function myFunction(arg: string): number;
}
```

### 6. Log Important Events

Use the provided logger to log registration and errors:

```java
@Override
public void register(TapestryExtensionContext ctx) {
    ctx.log().info("Registering {} capabilities", capabilities.size());
    
    try {
        // Registration logic
        ctx.log().info("Registration complete");
    } catch (Exception e) {
        ctx.log().error("Registration failed", e);
        throw e;
    }
}
```

### 7. Validate Arguments

Validate arguments in your API functions:

```java
new ProxyExecutable() {
    @Override
    public Object execute(Object... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                "Expected 1 argument, got " + args.length
            );
        }
        
        if (!(args[0] instanceof String)) {
            throw new IllegalArgumentException(
                "Expected string argument"
            );
        }
        
        String name = (String) args[0];
        return "Hello, " + name + "!";
    }
}
```

### 8. Handle Errors Gracefully

Provide clear error messages when operations fail:

```java
@Override
public Object execute(Object... args) {
    try {
        return performOperation(args);
    } catch (IOException e) {
        throw new RuntimeException(
            "Failed to perform operation: " + e.getMessage(),
            e
        );
    }
}
```

## Common Patterns

### Stateful Extensions

Extensions can maintain state across registrations:

```java
public class StatefulExtension implements TapestryExtension {
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    
    @Override
    public void register(TapestryExtensionContext ctx) {
        ctx.api().addFunction(
            ctx.extensionId(),
            "my_extension.cache_get",
            new ProxyExecutable() {
                @Override
                public Object execute(Object... args) {
                    String key = (String) args[0];
                    return cache.get(key);
                }
            }
        );
        
        ctx.api().addFunction(
            ctx.extensionId(),
            "my_extension.cache_set",
            new ProxyExecutable() {
                @Override
                public Object execute(Object... args) {
                    String key = (String) args[0];
                    Object value = args[1];
                    cache.put(key, value);
                    return null;
                }
            }
        );
    }
}
```

### Event Broadcasting

Extensions can broadcast events to mods:

```java
public class EventExtension implements TapestryExtension {
    private final List<Object> listeners = new ArrayList<>();
    
    @Override
    public void register(TapestryExtensionContext ctx) {
        // Register hook for mods to listen
        ctx.hooks().registerBridge(
            ctx.extensionId(),
            "my_extension.onEvent",
            (callback) -> {
                listeners.add(callback);
            }
        );
        
        // Register API to trigger event
        ctx.api().addFunction(
            ctx.extensionId(),
            "my_extension.triggerEvent",
            new ProxyExecutable() {
                @Override
                public Object execute(Object... args) {
                    Object data = args[0];
                    for (Object listener : listeners) {
                        // Invoke listener with data
                        ((ProxyExecutable) listener).execute(data);
                    }
                    return null;
                }
            }
        );
    }
}
```

### Configuration Support

Extensions can load configuration from files:

```java
public class ConfigurableExtension implements TapestryExtension {
    private final Path configPath;
    private JsonObject config;
    
    public ConfigurableExtension(Path configPath) {
        this.configPath = configPath;
    }
    
    @Override
    public void register(TapestryExtensionContext ctx) {
        // Load config during registration
        try {
            String json = Files.readString(configPath);
            config = JsonParser.parseString(json).getAsJsonObject();
            ctx.log().info("Loaded configuration from {}", configPath);
        } catch (IOException e) {
            ctx.log().warn("Failed to load config, using defaults", e);
            config = new JsonObject();
        }
        
        // Register API that uses config
        ctx.api().addFunction(
            ctx.extensionId(),
            "my_extension.getConfig",
            new ProxyExecutable() {
                @Override
                public Object execute(Object... args) {
                    String key = (String) args[0];
                    return config.get(key);
                }
            }
        );
    }
}
```

## Troubleshooting

### "Extension ID already registered"

**Cause**: Two extensions have the same ID.

**Solution**: Ensure your extension ID is unique. Check for conflicts with other mods.

### "Required extension not found"

**Cause**: Your extension depends on another extension that isn't installed.

**Solution**: Ensure all dependencies are listed in your mod's dependencies and are installed.

### "Capability already registered"

**Cause**: Two extensions try to register the same capability.

**Solution**: 
- If the capability should be exclusive, mark it as such
- If not, use unique capability names for each extension

### "Cannot register API after FREEZE"

**Cause**: Attempting to register an API after the `FREEZE` phase.

**Solution**: Move all registration logic to the `register()` method, which runs during `REGISTRATION`.

### "Circular dependency detected"

**Cause**: Extension A depends on B, and B depends on A.

**Solution**: Refactor to remove circular dependencies. Consider extracting shared functionality to a third extension.

### "Version requirement not met"

**Cause**: Your extension requires a newer Tapestry version than is installed.

**Solution**: Update Tapestry or lower your `minTapestry` version requirement.

## Next Steps

Now that you understand the extension system:

- Review [Lifecycle Phases](/guide/lifecycle-phases) for detailed phase documentation
- Explore [Core Concepts](/guide/core-concepts) for the philosophy behind extensions
- Check the [API Reference](/api/) for extension API documentation
- Study [Architecture](/guide/architecture) for system design details

## Key Takeaways

1. **Extensions are additive** - They can only add capabilities, never remove them
2. **describe() must be pure** - No side effects during descriptor creation
3. **Registration happens in phases** - DISCOVERY → VALIDATION → REGISTRATION → FREEZE
4. **Capabilities have types** - API, HOOK, or SERVICE
5. **Dependencies are validated** - Tapestry ensures all dependencies are present
6. **Type exports improve DX** - Provide TypeScript definitions for better mod development
7. **Fail-fast is enforced** - Invalid operations throw errors immediately
