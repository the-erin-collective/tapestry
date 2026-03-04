# Lifecycle and Boot (As-Built)

## Scope

Global startup orchestration, entrypoints, and lifecycle guards.

## Entrypoints

- `fabric.mod.json` registers `entrypoints.main` (`com.tapestry.TapestryMod`) and `entrypoints.server` (`com.tapestry.TapestryServer`).
- There is no `entrypoints.client` registration currently.
- Client initialization behavior is reached through server/integrated callbacks and runtime extension paths.

## Lifecycle Model

- Authoritative state machine: `com.tapestry.lifecycle.PhaseController`.
- Transitions are strict single-step increments.
- Re-entering the same state is a warning/no-op.
- Guard methods (`requirePhase`, `requireAtLeast`, `requireAtMost`) throw on violation.

## Boot Path

`TapestryMod#onInitialize` orchestrates bootstrap, extension validation, TypeScript loading/activation, and runtime service setup.

Server lifecycle callbacks then finalize server-dependent services:

- player service injection
- persistence backend init
- RPC transport/dispatcher wiring
- additional mod loader path (`initializePhase105ModSystem`)

## Failure Behavior

- Boot failures generally throw and abort startup.
- `FAILED` exists in `TapestryPhase` but startup does not consistently transition to it before throwing.

## Diagnostics and Observability

- Startup progress is logged with phase markers (`BOOTSTRAP` through `RUNTIME`).
- Phase transition violations throw immediately with explicit errors.
- Boot diagnostics and operational triage are anchored in architecture and troubleshooting references.

## Operational Notes

- `TapestryServer` also initializes RPC paths on dedicated server, creating overlapping responsibilities with `TapestryMod` callback wiring.
- Runtime may attempt to re-advance to `RUNTIME` in server-start logic; this is usually a no-op warning.

## Implementation Examples

### Phase enum & controller
```java
public enum TapestryPhase {
    BOOTSTRAP,
    DISCOVERY,
    REGISTRATION,
    FREEZE,
    TS_LOAD,
    TS_READY,
    RUNTIME,
    FAILED; // helper for error states
}

public final class PhaseController {
    private static TapestryPhase current = TapestryPhase.BOOTSTRAP;
    public static synchronized void advanceTo(TapestryPhase next) {
        if (next.ordinal() != current.ordinal() + 1) {
            throw new IllegalStateException("Cannot advance from " + current + " to " + next);
        }
        current = next;
        TapestryMod.LOG.info("phase -> " + current);
    }
    public static void requirePhase(TapestryPhase expected) {
        if (current != expected) {
            throw new IllegalStateException("Operation allowed only during " + expected + " (current phase: " + current + ")");
        }
    }
}
```

Example failure message produced by `requirePhase`:
```
IllegalStateException: Operation 'extend worlds domain' is only allowed during REGISTRATION (current phase: TS_LOAD)
```

## Primary References

- `../architecture/00-system-overview.md`
- `../architecture/01-boot-sequence.md`
- `../architecture/02-phase-contracts.md`
