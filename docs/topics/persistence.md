# Persistence (As-Built)

## Scope

Per-mod state persistence backends, runtime access model, and constraints.

## Initialization Model

- Persistence services are initialized in server-start callbacks.
- Backend selection differs by environment:
- dedicated server path uses server persistence service
- integrated/client path uses client persistence service

## Runtime Access

- Runtime injects `tapestry.persistence.getModStore(modId)` only when persistence is initialized.
- Access is same-mod only: caller mod context must match requested `modId`.

## Store Operations

`ModStateStore` exposes host-backed operations including:

- `get`, `set`, `delete`, `keys`
- helpers such as `hasKey`, `clear`, `save`, `getAll`

## Implementation Examples

```ts
const store = tapestry.persistence.getModStore("myMod");
store.set("foo", 42);
console.log(store.get("foo"));
```

## Failure Rules

- Missing persistence initialization prevents injection.
- Cross-mod access is denied.
- Invalid key/value usage fails via runtime/store validation.

## Primary References

- `../architecture/03-runtime-data-flows.md`
- `../reference/ts-api-reference.md`
- `../architecture/01-boot-sequence.md`
