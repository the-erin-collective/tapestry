# Architecture Docs

These files describe how Tapestry currently works in code, not only planned behavior.

- [00-system-overview.md](00-system-overview.md): subsystem map and boundaries.
- [01-boot-sequence.md](01-boot-sequence.md): startup timeline and phase progression.
- [02-phase-contracts.md](02-phase-contracts.md): allowed and forbidden operations per phase.
- [03-runtime-data-flows.md](03-runtime-data-flows.md): key end-to-end runtime paths.

Conventions:

- Reference concrete classes and methods for each claim.
- Note "Spec vs As-Built" differences where they exist.
- Keep failure behavior explicit.
