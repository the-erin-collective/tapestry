# Extensions and Registration (As-Built)

## Scope

Extension discovery, validation, registration, and registry freeze behavior.

## Discovery and Validation

- Providers are discovered by `ExtensionDiscovery`.
- Validation runs via `ExtensionValidator` and is lifecycle-gated to `VALIDATION`.
- Validation includes descriptor shape, capability declarations, and dependency checks.

## Registration Contract

- Registration orchestration runs through `ExtensionRegistrationOrchestrator`.
- API/hook/service registries accept writes only during `REGISTRATION`.
- Mutation after freeze throws (`RegistryFrozenException`/`IllegalStateException`).

## Freeze Boundary

- Registries are frozen after extension registration and before TypeScript mod loading.
- After freeze, extension API shape is immutable for the remainder of startup/runtime.

## Determinism and Rejection Model

- Validation/registration failures are fail-fast during startup.
- Dependency and conflict failures stop progression to runtime.
- Capability and type registry freezes lock final validated contracts.

## Implementation Examples

### Descriptor & provider
```java
public record TapestryExtensionDescriptor(
  String id,
  List<String> capabilities
) {}

public interface TapestryExtensionProvider {
  TapestryExtensionDescriptor describe();   // must be side-effect free
  void register(TapestryExtensionContext ctx); // only called in REGISTRATION
}
```

### ID validation snippet
```java
private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
if (!ID_PATTERN.matcher(descriptor.id()).matches()) {
    throw new ValidationException("invalid extension id: " + descriptor.id());
}
```

## Primary References

- `../architecture/02-phase-contracts.md`
- `../architecture/03-runtime-data-flows.md`
- `../reference/troubleshooting.md`
