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
| Public auth/admin HTTP adapters | future public auth/admin component(s) after extraction review | dependency from private host and Shell |

No row permits two manually maintained implementations. During extraction, the
private implementation is temporarily canonical and promotion is manifest
driven. Completion changes the canonical owner to the public component and the
private product becomes a consumer.

