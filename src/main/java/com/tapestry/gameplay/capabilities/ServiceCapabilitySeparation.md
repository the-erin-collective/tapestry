# Service Capability Separation

## Overview

Tapestry uses two complementary systems with distinct purposes: **Service Capabilities** for explicit dependencies and platform services, and **Gameplay Traits** for emergent compatibility and gameplay composition. This separation ensures each system can evolve independently with clear responsibilities.

## Dual System Architecture

### Service Capabilities (Existing System)

**Purpose**: Platform services, API extensions, explicit mod dependencies

**Model**: One provider per capability, explicit dependencies

**Resolution**: INITIALIZATION phase

**Use Cases**:
- Database services
- Network APIs
- Logging systems
- UI overlay systems
- Configuration managers
- Authentication services
- Custom protocol handlers

**API**:
```java
// Provide a capability
capabilities.provideCapability("mymod:database", databaseService);

// Require a capability (explicit dependency)
DatabaseService db = capabilities.requireCapability("mymod:database");

// Get a capability (optional dependency)
Optional<DatabaseService> db = capabilities.getCapability("mymod:database");
```

**Characteristics**:
- **One-to-one**: Each capability has exactly one provider
- **Explicit**: Dependencies declared explicitly
- **Type-safe**: Capabilities are strongly typed
- **Versioned**: Capabilities can have version requirements
- **Fail-fast**: Missing required capabilities fail at initialization

### Gameplay Traits (New System)

**Purpose**: Gameplay composition, emergent compatibility

**Model**: Many providers, many consumers, implicit compatibility

**Resolution**: COMPOSITION phase

**Use Cases**:
- Item behaviors (fish_food, milk_like, egg_like)
- Entity interactions (breeding, feeding, taming)
- Crafting ingredients (plant_fiber, metal_ore)
- Tool materials (wood_like, stone_like)
- Food types (vegetarian, carnivore, omnivore)

**API**:
```java
// Register a trait (many mods can register items with same trait)
traits.register("fish_food", new TraitConfig("tapestry:fish_items"));

// Declare trait consumption (many entities can consume same trait)
traits.consume("fish_food", new ConsumptionConfig("minecraft:cat", "food"));

// Assign traits to items (many items can have same trait)
items.register("mymod:nori", new ItemOptions()
    .traits("fish_food", "plant_fiber"));
```

**Characteristics**:
- **Many-to-many**: Multiple providers, multiple consumers
- **Implicit**: Compatibility emerges from trait assignments
- **Tag-based**: Uses Minecraft's tag system
- **Additive**: Multiple mods contribute to same traits
- **Graceful**: Missing traits don't fail, just no compatibility

## When to Use Each System

### Use Service Capabilities When:

1. **Explicit Dependencies**: Your mod requires another mod's API
   ```java
   // Explicit: "My mod requires the database mod"
   DatabaseService db = capabilities.requireCapability("mymod:database");
   ```

2. **Platform Services**: Providing infrastructure for other mods
   ```java
   // Platform service: "I provide a logging service"
   capabilities.provideCapability("mymod:logger", loggerService);
   ```

3. **One Provider**: Only one mod should provide this functionality
   ```java
   // One provider: "I am THE authentication service"
   capabilities.provideCapability("mymod:auth", authService);
   ```

4. **Type Safety**: You need strong typing and compile-time checks
   ```java
   // Type-safe: DatabaseService interface enforced
   DatabaseService db = capabilities.requireCapability("mymod:database");
   ```

5. **Versioning**: You need version compatibility checks
   ```java
   // Versioned: "I require database API v2.0+"
   capabilities.requireCapability("mymod:database", ">=2.0");
   ```

### Use Gameplay Traits When:

1. **Emergent Compatibility**: Items should work with systems without explicit integration
   ```java
   // Emergent: "My item is fish food, works with all fish-eating entities"
   items.register("mymod:nori", new ItemOptions().traits("fish_food"));
   ```

2. **Many Providers**: Multiple mods contribute items with same behavior
   ```java
   // Many providers: Multiple mods add fish food items
   items.register("mod1:fish", new ItemOptions().traits("fish_food"));
   items.register("mod2:seaweed", new ItemOptions().traits("fish_food"));
   items.register("mod3:kelp", new ItemOptions().traits("fish_food"));
   ```

3. **Many Consumers**: Multiple entities/systems use same trait
   ```java
   // Many consumers: Cats, dolphins, axolotls all consume fish_food
   traits.consume("fish_food", new ConsumptionConfig("minecraft:cat", "food"));
   traits.consume("fish_food", new ConsumptionConfig("minecraft:dolphin", "food"));
   traits.consume("fish_food", new ConsumptionConfig("minecraft:axolotl", "food"));
   ```

4. **Vanilla Integration**: Items should work with vanilla entities
   ```java
   // Vanilla integration: Modded items work with vanilla cats
   items.register("mymod:custom_fish", new ItemOptions().traits("fish_food"));
   // Cats automatically recognize this item
   ```

5. **Cross-Mod Compatibility**: Items from different mods should work together
   ```java
   // Cross-mod: Mod A's entity recognizes Mod B's items
   // Mod A:
   traits.consume("plant_fiber", new ConsumptionConfig("moda:herbivore", "food"));
   
   // Mod B:
   items.register("modb:grass", new ItemOptions().traits("plant_fiber"));
   // Mod A's herbivore automatically eats Mod B's grass
   ```

## Comparison Table

| Aspect | Service Capabilities | Gameplay Traits |
|--------|---------------------|-----------------|
| **Purpose** | Platform services, explicit dependencies | Gameplay composition, emergent compatibility |
| **Model** | One provider per capability | Many providers, many consumers |
| **Resolution** | INITIALIZATION phase | COMPOSITION phase |
| **Dependencies** | Explicit (requireCapability) | Implicit (trait assignments) |
| **Providers** | One per capability | Many per trait |
| **Consumers** | Many per capability | Many per trait |
| **Type Safety** | Strong (Java types) | Weak (string identifiers) |
| **Versioning** | Supported | Not supported |
| **Failure Mode** | Fail-fast (missing required capability) | Graceful (missing trait = no compatibility) |
| **Use Cases** | Database, network, logging, UI | Item behaviors, entity interactions, crafting |
| **Examples** | DatabaseService, NetworkAPI, Logger | fish_food, milk_like, plant_fiber |

## Code Examples

### Example 1: Database Service (Use Capability)

```java
// Provider mod
public class DatabaseMod {
    public void initialize() {
        DatabaseService db = new DatabaseServiceImpl();
        capabilities.provideCapability("mymod:database", db);
    }
}

// Consumer mod
public class ConsumerMod {
    public void initialize() {
        // Explicit dependency: fails if database mod not present
        DatabaseService db = capabilities.requireCapability("mymod:database");
        db.query("SELECT * FROM items");
    }
}
```

**Why Capability?**
- One provider (database service)
- Explicit dependency (consumer requires database)
- Type-safe (DatabaseService interface)
- Fail-fast (missing database = error)

### Example 2: Fish Food (Use Trait)

```java
// Mod A: Registers fish food trait
public class ModA {
    public void register() {
        traits.register("fish_food", new TraitConfig("tapestry:fish_items"));
        traits.consume("fish_food", new ConsumptionConfig("minecraft:cat", "food"));
    }
}

// Mod B: Adds fish food item
public class ModB {
    public void register() {
        items.register("modb:nori", new ItemOptions().traits("fish_food"));
    }
}

// Mod C: Adds another fish food item
public class ModC {
    public void register() {
        items.register("modc:seaweed", new ItemOptions().traits("fish_food"));
    }
}

// Result: Cats recognize nori and seaweed as food (emergent compatibility)
```

**Why Trait?**
- Many providers (Mod B, Mod C both add fish food)
- Implicit compatibility (no explicit dependencies)
- Emergent behavior (cats automatically recognize new items)
- Graceful (missing trait = no compatibility, not error)

## Anti-Patterns

### ❌ Don't Use Capabilities for Gameplay

```java
// WRONG: Using capability for item behavior
capabilities.provideCapability("mymod:fish_food_items", List.of("mymod:nori"));

// Problem: Only one mod can provide this capability
// Problem: Other mods can't add fish food items
// Problem: Requires explicit integration
```

**Correct Approach**:
```java
// RIGHT: Using trait for item behavior
traits.register("fish_food", new TraitConfig("tapestry:fish_items"));
items.register("mymod:nori", new ItemOptions().traits("fish_food"));

// Benefit: Multiple mods can add fish food items
// Benefit: Emergent compatibility with entities
// Benefit: No explicit integration required
```

### ❌ Don't Use Traits for Services

```java
// WRONG: Using trait for service
traits.register("database_service", new TraitConfig("tapestry:databases"));

// Problem: Traits are for gameplay, not services
// Problem: No type safety
// Problem: No explicit dependencies
```

**Correct Approach**:
```java
// RIGHT: Using capability for service
capabilities.provideCapability("mymod:database", databaseService);

// Benefit: Type-safe DatabaseService interface
// Benefit: Explicit dependencies
// Benefit: One provider per service
```

## Migration Guide

### Migrating from Capability to Trait

If you have a capability that should be a trait:

**Before (Capability)**:
```java
capabilities.provideCapability("mymod:fish_foods", List.of("mymod:nori"));
List<String> fishFoods = capabilities.requireCapability("mymod:fish_foods");
```

**After (Trait)**:
```java
traits.register("fish_food", new TraitConfig("tapestry:fish_items"));
items.register("mymod:nori", new ItemOptions().traits("fish_food"));
traits.consume("fish_food", new ConsumptionConfig("minecraft:cat", "food"));
```

### Migrating from Trait to Capability

If you have a trait that should be a capability:

**Before (Trait)**:
```java
traits.register("database", new TraitConfig("tapestry:databases"));
```

**After (Capability)**:
```java
capabilities.provideCapability("mymod:database", databaseService);
DatabaseService db = capabilities.requireCapability("mymod:database");
```

## Conclusion

**Service Capabilities** and **Gameplay Traits** are complementary systems with distinct purposes:

- **Use Capabilities** for platform services, explicit dependencies, and one-provider scenarios
- **Use Traits** for gameplay composition, emergent compatibility, and many-provider scenarios

This separation ensures:
- ✅ Clear responsibilities for each system
- ✅ Independent evolution of capabilities and traits
- ✅ Appropriate tool for each use case
- ✅ No confusion about which system to use

When in doubt:
- **Is it a service or API?** → Use Capability
- **Is it a gameplay behavior?** → Use Trait
