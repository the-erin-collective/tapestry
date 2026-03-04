# Events and State (As-Built)

## Scope

Event bus behavior, namespacing, lifecycle exposure, and deferred state-change emission.

## Event Model

- Registration/emit APIs are exposed through `tapestry.mod.on`, `emit`, and `off`.
- Dispatch is synchronous in `EventBus`.
- Listener failures are logged while dispatch continues.

## Namespace and Ownership Rules

- `EventBus` validates event namespace ownership and naming patterns.
- Lifecycle checks are primarily enforced at API exposure/call boundaries, not by deep phase locks inside `EventBus`.

## Implementation Examples

### State interface from Phase 12
```ts
interface State<T> {
  get(): T;
  set(value: T): void;
  update(mutator: (current: T) => T): void;
  subscribe(handler: (event: StateChangeEvent<T>) => void): Unsubscribe;
}
```

### handler-depth pseudocode
```ts
let handlerDepth = 0;
let pendingStateChanges: StateChangeEvent<any>[] = [];

function dispatch(event) {
  handlerDepth++;
  try { runHandlers(event); } finally {
    handlerDepth--;
    if (handlerDepth === 0) flushStateChanges();
  }
}
```

## State Coordination Model

- `StateCoordinator` tracks dispatch depth.
- State changes are deferred during nested dispatch.
- Flush occurs when outermost dispatch completes, emitting internal state-change events.
- No coalescing semantics are currently enforced by a dedicated coalescing stage.

## Practical Implication

- Event behavior is safe and deterministic but less tightly lifecycle-coupled than some older phase-spec wording implied.

## Primary References

- `../architecture/03-runtime-data-flows.md`
- `../reference/ts-api-reference.md`
- `../architecture/02-phase-contracts.md`
