# Capabilities and Type Contracts (As-Built)

## Scope

Capability declaration/lookup behavior and cross-mod type contract constraints that are currently enforced.

## Capability Model

- Capabilities are integrated with extension/mod registration, not a separate runtime-only system.
- Registration-time APIs exist for provide/require declarations.
- Runtime lookup APIs are available after registry freeze/validation paths complete.

## Validation and Dependency Rules

- Capability requirements are validated during startup validation and mod graph checks.
- Dependency graph failures (missing providers/cycles/conflicts) fail startup.
- Deterministic activation order is built from validated dependency structure.

## Type Contract Direction

- Type registry and freeze paths exist in validation lifecycle.
- Cross-mod type import/export behavior remains coupled to descriptor validation and runtime resolver behavior.
- Runtime isolation and namespace ownership rules apply to prevent unbounded cross-mod type leakage.

## Practical Status

- This area has had significant design churn in phase docs; treat architecture/reference docs and runtime guards as source of truth.

## Implementation Examples

### declare capabilities in descriptor
```java
public record TapestryExtensionDescriptor(
    String id,
    List<String> capabilities
) {}
// in provider:
return new TapestryExtensionDescriptor("myext", List.of("network","ui"));
```

### runtime lookup
```ts
const loggerCap = tapestry.runtime.capabilities.require("logger");
loggerCap.log("hello");
```

## Primary References

- `../architecture/02-phase-contracts.md`
- `../architecture/03-runtime-data-flows.md`
- `../reference/ts-api-reference.md`
