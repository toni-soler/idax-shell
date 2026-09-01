# Capability reuse matrix

This matrix is the gate for implementation. `Extract` means the existing owner
must first produce an independently reviewed public component; Shell must not
copy the private implementation. `Adapter` means new code is limited to HTTP or
composition glue over a public Core API.

| Capability | Existing source | Public/private today | Reusable as-is | Needs extraction | Needs clean-room implementation | Target public component | Source of truth | Sync strategy | License risk | Exposure risk |
|---|---|---|---|---|---|---|---|---|---|---|
| Login | Core credential/rate-limit/MFA primitives; current API orchestration | mixed: binary public-use contract, orchestration private | partly | yes, preferred | only a thin adapter if extraction is blocked | `idax-auth-web` or Shell adapter | future public auth component | extract once; transitional allowlisted promotion | medium | high |
| JWT resource server | Core decoder, converters, filters and tenant context | public binary | yes | no | no | `idax-core` | Core Runtime | consume Maven API | low | low |
| JWT issuance | Core encoder config and token primitives; current login orchestration | mixed | partly | yes | only composition/config | `idax-auth-web` | future public auth component | extract with contract tests | medium | high |
| First-admin bootstrap | Core tenant onboarding service; current controller/config | mixed | service yes | controller/policy candidate | Shell-only one-time gate may be new | `idax-bootstrap-web` | public component after extraction | allowlisted transition, then direct dependency | medium | high |
| Tenant management | Core domain/repository/onboarding; current API/UI | mixed | services partly | HTTP/UI | no domain logic | `idax-tenant-admin-web` + UI package | public admin components | extract by vertical slice | medium | high |
| User management | Core user and tenant-user services; current API/UI | mixed | services yes | controllers/UI | no domain logic | `idax-identity-admin` | Core for domain; public adapter for HTTP/UI | extract | medium | high |
| Membership management | Core membership service; current API/UI | mixed | service yes | controllers/UI | no | `idax-identity-admin` | Core/public adapter | extract | medium | high |
| Role management | Core role service; current API/UI | mixed | service yes | controllers/UI | no | `idax-access-admin` | Core/public adapter | extract | medium | high |
| Permission management | Core permission service/catalog lifecycle; current API/UI | mixed | service yes | controllers/UI | no | `idax-access-admin` | Core/public adapter | extract | medium | medium |
| Module discovery | DevKit manifests and generated permission catalogs | public Apache-2.0 | partly | generalized registry/descriptor | small composition registry | `@idax/module-contract` + Java contract | DevKit contract | move contract to public package; consumers depend on it | low | low |
| Backend module composition | Ledger/osTRIS standalone apps and Core public APIs | public | HTTP as-is | optional starter later | composition only | `idax-module-contract-java` | public contract repo | direct dependency/version compatibility | low | low |
| Frontend module extensions | Ledger extension bundle/manifest; partial osTRIS manifest | public | Ledger partly | SDK and schema must be separated | host loader only | `@idax/module-sdk` | public SDK repo | extract from public module contract; no product copy | low | medium |
| Navigation | Current frontend sidebar/layout; Ledger generated menu metadata | mixed | metadata yes | common renderer | no product-specific code | `@idax/shell-ui` | future public UI package | extract common renderer; declarative manifests | medium | high |
| Translations | 12-locale convention and module locale bundles | mixed/public modules | module bundles yes | host i18n bootstrap/validator | only Shell strings | `@idax/i18n` | public i18n package | extract tooling; modules own their strings | low | medium |
| Common UI components | Current common React components | private | no, pending audit | yes, component-by-component | only where audit rejects extraction | `@idax/ui` | public UI package | provenance-reviewed extraction, visual tests | high | high |
| Audit | Core audit services/entities/listeners | public binary | yes | HTTP/UI query adapter | no audit engine | `idax-audit-web` | Core/public adapter | extract API/UI, consume Core service | medium | medium |
| Health | Spring Actuator in Shell/Ledger/osTRIS | public dependencies/products | yes | no | composition/config only | each product | each product | direct configuration | low | low |
| Database migrations | Core Runtime, Shell, Ledger and osTRIS migration chains | public | yes | no | Shell schema only | owning product repository | each product | immutable versioned artifacts; never copy private DB trees | low | medium |
| Ledger integration | Ledger backend and extension bundle | public Apache-2.0 | backend yes; frontend contract partly | SDK compatibility adapter | no Ledger logic | Ledger repos + module SDK | Ledger | image/package consumption | low | low |
| osTRIS integration | osTRIS backend/frontend repositories | public Apache-2.0 | backend yes; frontend incomplete | frontend extension entrypoint | no osTRIS logic | osTRIS repos + module SDK | osTRIS | image/package consumption | low | low |
| Docker deployment | product Dockerfiles and public migration chains | public/mixed maturity | partly | shared deployment conventions | Shell composition only | `idax-shell/deploy` | Shell for distribution; products for images/migrations | pin published artifacts; no source sync | low | medium |

## Decisions

1. The provisional JDBC login and bootstrap implementations in the initial
   scaffold are not accepted as the target architecture. They remain unshipped
   evidence until replaced by public Core services or extracted adapters.
2. The provisional React screen is not an accepted common UI implementation.
   Shell will consume extracted `@idax/ui`, `@idax/i18n` and
   `@idax/module-sdk` packages before adding administration screens.
3. A reusable public component becomes the source of truth and is consumed by
   both private and public products. Export synchronization is transitional.
4. The Market is configuration, branding, policies, enabled module manifests,
   secrets, networking and bootstrap data. Product-specific behavior belongs
   in a separate module; Shell is never forked for it.

