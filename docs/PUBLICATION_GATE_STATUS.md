# Publication gate status

## Completed on review branches

- The secure local refresh correction is limited to token verification, decoder configuration, and targeted tests; it is ready for owner merge.
- DevKit carries Apache-2.0 for project-owned source, separate third-party notices, coherent package metadata, 22 passing tests, and a passing wheel build; it is ready for owner merge.
- A Core-neutral first-administrator command and its mirrored generic migration exist on dedicated WIP branches. Java compilation passes. Two real-PostgreSQL tests cover one-shot state and concurrent attempts; their execution was not completed in this session because Docker was unavailable at the attempted run and was not rerun at owner request.
- The four private module hosts now consume one generic loader on a WIP branch. Its targeted tests and production build pass.
- IDAX and osTRIS brand sources are versioned publicly with deterministic derivatives and explicit brand/license boundaries.

## Remaining blockers

- The prerequisite branches are not merged into their sources of truth.
- npm scope control remains an owner action.
- Candidate Java and npm foundation artifacts do not yet exist. Consequently, exact artifact-scoped dependency inventories, CycloneDX SBOMs, and checksums cannot be truthfully generated without starting the prohibited extraction. Monorepo-wide reports would not satisfy this gate.
- The bootstrap PostgreSQL tests remain to be executed once the branch is reviewed.

Wave 1 extraction and all Maven/npm publication remain blocked.
