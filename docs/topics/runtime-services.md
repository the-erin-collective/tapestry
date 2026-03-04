# Runtime Services (As-Built)

## Scope

Scheduler, config, state service access, runtime logging, and runtime-side API exposure.

## Service Set

- scheduler: tick/timeout/interval helpers via `SchedulerService`
- config: mod configuration access via `ConfigService`
- state store: key/value runtime state via `ModStateService`
- runtime logging: `tapestry.runtime.log.*`

## Availability

- Runtime services are injected when runtime extension paths run (`TypeScriptRuntime#extendForRuntime`).
- APIs requiring current mod context fail when called outside valid mod execution context.

## Behavioral Rules

- Service calls are explicit and fail-fast on invalid phase/context.
- Scheduler and state operations are deterministic within server tick ordering and event dispatch rules.
- Persistence exposure is runtime-conditional (only injected when persistence backend is initialized).

## Implementation Examples

### scheduler usage (Java facade)
```java
SchedulerService scheduler = runtime.getService(SchedulerService.class);
scheduler.scheduleTick(() -> {
    LOGGER.info("tick callback");
});
```

### config read in JS
```ts
const cfg = tapestry.runtime.config.get("myMod.someValue");
```

## Primary References

- `../reference/ts-api-reference.md`
- `../architecture/03-runtime-data-flows.md`
- `../architecture/02-phase-contracts.md`
