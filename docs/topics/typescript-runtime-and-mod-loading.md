# TypeScript Runtime and Mod Loading (As-Built)

## Scope

JavaScript runtime setup, mod discovery/evaluation, registration, activation, and `onLoad`.

## Runtime Ownership

- `TypeScriptRuntime` owns JS context creation, API injection, task queueing, and current mod execution context.
- Mod loading is orchestrated by `TapestryMod` with `TsModDiscovery` and `ModRegistry`.

## Implementation Examples

### dual `mod.define` usage
Two loader paths coexist in the runtime:
```java
// registration-phase define (used inside TsModDefineFunction)
public void define(String id, ScriptObjectMirror factory) {
    requirePhase(TapestryPhase.TS_LOAD);
    modRegistry.register(new TsMod(id, factory));
}

// runtime-injected define (created by TypeScriptRuntime)
public static Value createModDefineFunction() {
    return polyglotContext.eval("js", "(id, factory) => { /* ... */ }");
}
```

### activation order
```java
modRegistry.validateDependencies();
for (Mod m : modRegistry.computeActivationOrder()) {
    m.activate(); // inject APIs, run onLoad
}
```

## Mod Discovery and Evaluation

- Mods are discovered from filesystem (`.../tapestry/mods`) and classpath mod assets.
- Scripts are evaluated during registration paths after runtime extension for registration APIs.

## `mod.define` Contract (Current Reality)

Two active implementations exist:

- `TsModDefineFunction#define` path (`TS_LOAD`-gated contract)
- runtime-injected define from `TypeScriptRuntime#createModDefineFunction` (`TS_REGISTER` path)

This dual path is active and must be treated as current behavior.

## Activation and `onLoad`

- `ModRegistry` validates dependencies and computes activation order.
- Activation happens in dependency order.
- `onLoad` execution runs once ready APIs are injected.
- Failures in evaluation/activation/onLoad are startup-fatal.

## API Injection Timing

- Runtime namespaces are injected in staged lifecycle extension methods (`extendForRegistration`, `extendForActivation`, `extendForReadyPhase`, `extendForRuntime`, and client/runtime-specific extensions).

## Primary References

- `../architecture/03-runtime-data-flows.md`
- `../reference/ts-api-reference.md`
- `../architecture/02-phase-contracts.md`
