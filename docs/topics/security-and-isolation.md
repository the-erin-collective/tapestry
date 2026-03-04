# Security and Isolation (As-Built)

## Scope

Runtime safety boundaries for TypeScript execution, API exposure, RPC ingress, and UI render sanitization.

## JavaScript Runtime Boundary

- `TypeScriptRuntime` controls context creation and exposed host surface.
- Most mod-facing APIs are namespace-scoped and context-checked by current mod execution ID.
- Lifecycle gating prevents early access to runtime-sensitive APIs.

## RPC Hardening

- Packet size checks, protocol checks, and JSON sanitization run in `RpcPacketHandler`.
- `RpcServerRuntime` enforces handshake completion and call-rate limits.
- `RpcDispatcher` enforces server API allowlist and namespace ownership.

## Overlay Safety

- `UINodeValidator` and `OverlaySanitizer` validate/sanitize UI nodes before render.
- Cross-mod overlay visibility mutation is denied.

## Known Security-Relevant Gaps

- RPC wire schema mismatch increases protocol-fragility risk until normalized.
- Client presentation init ownership is split across paths, increasing lifecycle-order sensitivity.

## Implementation Examples

### overlay sanitization snippet
```java
UINode root = parser.parse(jsResult);
UINode sanitized = OverlaySanitizer.sanitize(root);
```

### rpc packet check (see earlier example)

## Primary References

- `../networking.md`
- `../ui_notes.md`
- `../architecture/00-system-overview.md`
