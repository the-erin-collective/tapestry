# Client Presentation and Overlays (As-Built)

## Scope

Client presentation readiness, overlay registration, validation, and rendering flow.

## Initialization

- Client presentation APIs are injected through `TypeScriptRuntime#extendForClientPresentation`.
- No explicit `entrypoints.client` registration exists; timing depends on current callback-driven lifecycle paths.

## Overlay API

`tapestry.client.overlay` exposes:

- `register`
- `setVisible`
- `getCount`
- `template`
- `add` (placeholder behavior)

## Registration and Render Contract

- Required overlay definition fields: `id`, `anchor`, `render`.
- Overlay IDs are unique per `modId:overlayId`.
- Cross-mod visibility mutation is blocked.
- Per-frame rendering evaluates JS on runtime thread, then renders sanitized snapshot on HUD render path.

## Known Current Gaps

- `overlay.add(fragment)` is not full composition behavior yet.
- `ClientInfo.PlayerInfo.fromPlayer(...)` currently returns `null`.
- Template support is lightweight interpolation, not a full template engine.

## Implementation Examples

### registering an overlay
```ts
const overlay = {
  id: "healthBar",
  anchor: "topCenter",
  render: () => {
    return { type: "text", text: "HP: " + tapestry.client.players.getHealth() };
  }
};
tapestry.client.overlay.register(overlay);
```

### per-frame render flow
1. JS evaluation happens on the runtime thread.
2. Resulting node tree is sanitized via `OverlaySanitizer`.
3. Snapshot stored in `OverlayRegistry`.
4. HUD render callback paints snapshot.

## Primary References

- `../ui_notes.md`
- `../architecture/03-runtime-data-flows.md`
- `../reference/ts-api-reference.md`
