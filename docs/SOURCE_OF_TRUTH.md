# Source of truth registry

| Component | Canonical owner | Consumer model |
|---|---|---|
| Core Java contracts and services | IDAX Core Runtime binary release | Maven dependency |
| Core database contract | IDAX Core Runtime migration release | immutable Flyway input |
| Shell composition and distribution | IDAX Shell | image/release assets |
| Module manifest and permission catalog contract | IDAX Module DevKit until extracted to a dedicated public contract package | generated artifacts and validators |
| Ledger domain/backend/frontend | IDAX Ledger product repositories | image and extension package |
| osTRIS domain/backend/frontend | osTRIS product repositories | image and extension package |
| Shared React components | future `@idax/ui` public package | package dependency from private UI and Shell |
| Module host SDK | future `@idax/module-sdk` public package | package dependency from Shell and modules |
| Shared i18n bootstrap/tooling | future `@idax/i18n` public package | package dependency |
| Authentication/admin domain rules and security | IDAX Core Runtime binary release | Maven dependency from Shell |
| Public auth/admin HTTP adapters | IDAX Shell until extracted to dedicated public components | thin adapters over Core services; no duplicated SQL/domain rules |

No row permits two manually maintained implementations. During extraction, the
private implementation is temporarily canonical and promotion is manifest
driven. Completion changes the canonical owner to the public component and the
private product becomes a consumer.

The workspace synchronizer treats Shell differently from module snapshots: it
validates the public-owned Shell and, during a versioned Core release, updates
its Maven dependency. Ledger and osTRIS continue to be copied from their private
repositories. Core Java source is never copied; the reviewed JAR and Flyway
migrations are the synchronization boundary.
