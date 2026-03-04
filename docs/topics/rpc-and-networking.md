# RPC and Networking (As-Built)

## Scope

Transport registration, handshake, RPC dispatch, and server-to-client event push behavior.

## Transport and Registration

- RPC transport uses Fabric typed payloads on channel `tapestry:rpc`.
- Server registration paths exist in both `TapestryMod` and `TapestryServer`.
- Client receiver registration is in `com.tapestry.client.TapestryClient`.

## End-to-End Call Path

1. Client API proxy builds `rpc_call` payload.
2. Server packet handler validates size/protocol/sanitization.
3. Server runtime checks handshake and rate limits.
4. Dispatcher validates method allowlist/namespace and executes.
5. Response routes back to client pending-call registry.

## Handshake and Security

- Client sends `hello`; server returns `hello_ack` or `handshake_fail`.
- RPC before handshake is denied.
- Hardening includes protocol checks, sanitization, allowlist enforcement, and per-client rate limits.

## Implementation Examples

### basic payload structure
```json
{
  "type": "rpc_call",
  "namespace": "modid:api",
  "method": "foo",
  "args": [1,2,3],
  "requestId": "abcd1234"
}
```

### server handler checks (Java)
```java
public void handle(PacketContext ctx, RpcPacket pkt) {
  if (!ctx.isHandshakeComplete()) {
      throw new IllegalStateException("RPC before handshake");
  }
  if (pkt.payload().length() > MAX_SIZE) {
      ctx.disconnect("packet too large");
  }
  // sanitization
  JsonObject obj = parse(pkt.payload());
  if (obj.getString("type").equals("rpc_call")) {
      dispatch(obj);
  }
}
```

## Current Wire-Contract Risks

- Some server senders use `response` + `requestId`, while client handlers expect `rpc_response` + `id`.
- Some server push paths use `event` + `channel/data`, while client handlers expect `server_event` + `event/payload`.
- Handshake-installed-mod integration in dispatcher remains incomplete (TODO-level).

## Primary References

- `../networking.md`
- `../architecture/03-runtime-data-flows.md`
- `../reference/ts-api-reference.md`
