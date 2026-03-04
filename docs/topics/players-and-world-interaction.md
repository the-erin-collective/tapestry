# Players and World Interaction (As-Built)

## Scope

Player-facing APIs and world interaction surfaces currently exposed through runtime/client paths.

## Runtime Player Services

- Server-side player utilities are wired through `PlayerService` and exposed into runtime namespaces.
- Join/leave and tick-driven engine events are emitted when event bus is available.

## Client Player APIs

`tapestry.client.players` currently exposes:

- `getPosition()`
- `getLook()`
- `raycastBlock(options?)`

Current behavior is client-local and phase-gated to client presentation readiness.

## Raycast Constraints

- Client raycast enforces safety limits such as max distance (`<= 32` in current implementation).
- Result shapes are sanitized before use in JS-facing APIs.

## Implementation Examples

### server event listener
```ts
// server-side in a mod
tapestry.mod.on("player_join", e => {
  console.log("player joined: " + e.playerName);
});
```

### client API call
```ts
const pos = tapestry.client.players.getPosition();
const look = tapestry.client.players.getLook();
```

## Primary References

- `../reference/ts-api-reference.md`
- `../architecture/01-boot-sequence.md`
- `../architecture/03-runtime-data-flows.md`
