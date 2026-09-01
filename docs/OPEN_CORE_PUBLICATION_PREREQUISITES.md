# Open-core publication prerequisites

Status date: 2026-09-01. This gate authorizes no package publication and does
not create the future foundations repositories.

## Publication rights matrix

The owner confirms that all IDAX, Ledger, osTRIS, DevKit and frontend code
considered here is his own AI-assisted work, without another human contributor
whose consent is required. Selected own code is authorized for Apache-2.0
publication. Private repository visibility does not license an entire source
tree; only paths in a reviewed export manifest are promoted.

| Component | Repository | Own code | Third-party content | Third-party license | Generated/external origin | Candidate public license | Publication status | Required sanitization | Blocking issue |
|---|---|---|---|---|---|---|---|---|---|
| LOCAL authentication/orchestration | IDAX Platform source | yes; history and owner confirmation | framework dependencies only | mainly Apache-2.0/EPL/MIT/BSD | handwritten | Apache-2.0 | AUTHORIZED OWN CODE | reviewed classes only; neutral packages/config/errors | decompose contract before promotion |
| LOCAL issuance and refresh | IDAX Platform source | yes | Spring Security JOSE/Nimbus | Apache-2.0 | handwritten | Apache-2.0 | AUTHORIZED OWN CODE | single authoritative encoder/decoder | security branch must merge into source of truth |
| Auth HTTP controller/DTOs | IDAX Platform source | yes | Spring MVC/OpenAPI | Apache-2.0 | handwritten | Apache-2.0 | AUTHORIZED OWN CODE | promote contract, not controller tree | current shape exposes private orchestration |
| Admin HTTP adapters | IDAX Platform source | yes | Spring MVC/OpenAPI | Apache-2.0 | handwritten | Apache-2.0 | AUTHORIZED OWN CODE | exclude preferences, data areas and Flyway export | smaller DTO/port boundary required |
| Core tenant/user/membership/role/permission services | Core Runtime | yes | declared runtime dependencies | per dependency | handwritten plus owned migrations | current Core terms | AUTHORIZED WITH THIRD-PARTY CONDITIONS | consume; do not republish in foundations | none for consumption |
| First-admin composition | future foundations | yes when authored | PostgreSQL/JDBC/Core | Apache-2.0 dependencies | new | Apache-2.0 | AUTHORIZED OWN CODE | consume a Core-neutral command | existing onboarding writes legacy state |
| Module SDK/loader algorithm | IDAX frontend source | yes | React/router/i18next | MIT | handwritten, duplicated | Apache-2.0 | AUTHORIZED OWN CODE | converge first; remove globals/product probes | refactor incomplete |
| Alert, Spinner, Modal | IDAX frontend source | yes | React, Bootstrap, Font Awesome | MIT; Font Awesome icons CC-BY-4.0/fonts OFL-1.1 | handwritten UI | Apache-2.0 | AUTHORIZED WITH THIRD-PARTY CONDITIONS | dependency notices; no copied assets | stabilize accessibility/API |
| DataGrid/CRUD framework | IDAX frontend source | yes | declared npm dependencies | permissive/asset conditions | handwritten with product rules | Apache-2.0 | AUTHORIZED OWN CODE | remove business identifiers/defaults | REQUIRES REFACTOR |
| i18n bootstrap/namespace loader | IDAX frontend source | yes | i18next/react-i18next | MIT | handwritten | Apache-2.0 | AUTHORIZED OWN CODE | exclude product locales | namespace tests missing |
| Vendored `public/lib` and theme assets | IDAX frontend source | no assumption | extensive minified libraries/fonts/images | mixed individual terms | third-party/vendor | none | NOT ELIGIBLE | exclude entire tree | unnecessary and provenance not closed file-by-file |
| DevKit source/schema/generator/templates/tests | Module DevKit | yes | PyYAML and jsonschema, not vendored | MIT | owned generator and output | Apache-2.0 | AUTHORIZED WITH THIRD-PARTY CONDITIONS | LICENSE/NOTICE/notices/public schema identity | hygiene branch must merge |
| Generated module output | DevKit output | templates/inputs owned | dependencies of generated product | per product | generated; record generator/input/version | Apache-2.0 by target declaration | AUTHORIZED WITH THIRD-PARTY CONDITIONS | deterministic provenance headers | validate each output inventory |
| Ledger/osTRIS extension contracts | product repositories | yes | product dependencies | per product inventory | handwritten/generated split | Apache-2.0 | AUTHORIZED WITH THIRD-PARTY CONDITIONS | declarative manifests only | release SBOM/license gates pending |

No inspected candidate handwritten file carries a conflicting notice or claim
of third-party derivation. The broad Java report contains Apache-2.0, MIT, BSD,
EPL, MPL and LGPL-family declarations; no AGPL or GPL-only dependency was
identified. It includes unrelated legacy dependencies, so the exact future
artifacts must be scanned independently. The frontend lock has 1,686 package
entries. Its two entries without a machine-readable license field contain MIT
and BSD license files. The vendored frontend asset tree remains excluded.

## Source license strategy and DevKit

Platform and Frontend do not receive a repository-root open-source license. An
internal export manifest records source commit, included paths,
`expectedLicense: Apache-2.0`, generated origin, exclusions, forbidden
imports/references, NOTICE fragments and inventory hash. The public destination
gets LICENSE, NOTICE, third-party notices and SBOM. Once both hosts consume the
public artifact, it becomes source of truth and the manifest is retired.

**DEVKIT PUBLICATION AUTHORIZATION: CONFIRMED BY OWNER.** Choose option A: the
complete DevKit should become public because schema, validator and generator
form one small coherent conformance tool. Its WIP hygiene branch adds
Apache-2.0 metadata, NOTICE, MIT dependency notices and a public schema ID.

## Refresh security

The old flow parsed a signed JWT and trusted claims without verifying its
signature. Modified identity, roles, tenant or expiry could therefore be
exchanged for a newly signed access token.

```text
refresh -> authoritative local JwtDecoder
        -> RS256 + signature + timestamps + issuer(idax-local)
        -> type == refresh + non-empty subject + valid userId
        -> authoritative local JwtEncoder
```

The WIP source fix preserves the current absence of server-side revocation and
rotation; a public contract must state that or add atomic rotation/reuse
detection. Tests with real RSA keys reject modified payload, wrong key,
expiration, access-as-refresh, malformed JWT, `alg:none`, HS/RS confusion and
wrong issuer; a valid LOCAL refresh passes. Auth-mode tests confirm LOCAL,
KEYCLOAK and DUAL selection leaves SERVICE trust independent. Keycloak refresh
remains the provider's responsibility.

**LOCAL REFRESH TOKEN VALIDATION: PASS on WIP; merge pending.**

## Auth decomposition and contract

```text
AuthModeRouter
├─ KEYCLOAK -> provider flow + Core resource-server validation
├─ LOCAL
│  ├─ PasswordAuthentication -> RateLimit
│  ├─ MfaPolicy -> signed MfaChallenge -> TOTP
│  ├─ TenantSelection -> effective memberships
│  ├─ TokenIssuance -> authoritative encoder
│  └─ Refresh -> authoritative decoder -> current-user check -> issuance
└─ DUAL -> explicit provider routing
Every outcome -> SecurityAudit port
Host -> keys, issuer, lifetimes, mode, optional session policy
```

The reusable module exposes ports for those responsibilities; Spring MVC is a
thin adapter. Branding, mail, preferences, legacy data areas and product policy
stay outside. MFA remains part of reusable LOCAL auth because tenant policy can
require it; only its UI is separate.

| API | Request | Success | Closed errors | Invariants |
|---|---|---|---|---|
| `POST /api/auth/v1/login` | `{username,password,tenantId?}` | token pair, MFA state or tenant-selection state | `INVALID_CREDENTIALS`, `RATE_LIMITED`, `MFA_REQUIRED`, `TENANT_SELECTION_REQUIRED`, `AUTH_MODE_UNAVAILABLE` | generic errors; effective membership; audited |
| `POST /api/auth/v1/mfa/verify` | `{challengeToken,code,tenantId?}` | token pair/selection | `INVALID_CHALLENGE`, `INVALID_MFA_CODE`, `RATE_LIMITED` | signed, expiring, purpose-bound challenge |
| `POST /api/auth/v1/refresh` | `{refreshToken}` | token pair | `INVALID_REFRESH_TOKEN`, `REFRESH_EXPIRED`, future `SESSION_REVOKED` | crypto before claims; issuer/type/subject/current user checked |
| `GET /api/auth/v1/mode` | none | `{mode,localLogin,mfa}` | none | capabilities only, no secrets |
| logout | none while stateless | client deletes tokens | n/a | add endpoint only with revocation store |

## Admin and first administrator

Core stays source of truth for entities, repositories, `TenantContext`,
permissions/catalog and audit storage. Admin adds only DTOs, validation,
authorization, tenant/path checks and error mapping. It excludes preferences,
mail, legacy data areas, generated migrations and product audit projections.

The existing onboarding service is not Shell-compatible because it writes
legacy data-area and mapping tables. Core first needs a neutral transactional
command creating only a Core tenant, local user/credential and owner membership;
both private host and public Admin will consume it.

First-admin then uses one PostgreSQL transaction: acquire a fixed
`pg_advisory_xact_lock`, lock/create singleton `idax_core.bootstrap_state`,
recheck that no enabled superuser and no effective owner/admin membership
exists, call the Core-neutral command, audit, set `completed_at`, commit. The
endpoint bean exists only with an explicit secret file; comparison is
constant-time, startup/calls fail closed, and no reset API exists.
Testcontainers must prove concurrent instances, rollback, existing admin,
invalid secret, one-shot refusal and audit. Design is closed; implementation
waits for the neutral Core command.

## Frontend boundary and single loader

| Candidate | Classification | Plan |
|---|---|---|
| duplicated module hosts | REQUIRES REFACTOR | one internal loader, then promote |
| registration/global SDK convention | GENERIC after refactor | public Module SDK becomes source of truth |
| hardcoded sidebar/generated legacy navigation | PRODUCT-SPECIFIC | never promote; declarative navigation |
| token interceptor/auth context/private route | PRIVATE-INFRA + REQUIRES REFACTOR | split token transport, claims and guards |
| Alert/Spinner/Modal | GENERIC | test API/accessibility, then promote |
| DataGrid/CRUD | REQUIRES REFACTOR | remove business assumptions |
| i18n initialization/namespace registration | GENERIC after refactor | public package; product translations stay owned by products |
| vendored theme/libs/fonts/images | THIRD-PARTY/PRODUCT-SPECIFIC | exclude permanently |

One fail-closed loader consumes a closed inventory, validates manifest and
SemVer compatibility, enforces unique module/route/navigation/API namespaces,
checks permissions, registers translations/assets, verifies SHA-384 SRI,
restricts origins and operates under CSP. It has no Ledger/osTRIS branches.

## Publication identities

Maven coordinates are `io.github.toni-soler.idax:idax-auth-web`,
`io.github.toni-soler.idax:idax-admin-web` and later
`io.github.toni-soler.idax:idax-foundations-bom`; Java packages use
`io.github.tonisoler.idax`. Maven Central Portal publication will require
namespace verification, GPG signatures, source/Javadoc jars, reproducible
build, SBOM/license reports, signed tag and manual approval.

For JavaScript, a controlled npm scope is preferred; unscoped names have weak
grouping/global scarcity, GitHub Packages is a viable owner-verified fallback,
and a self-hosted registry is rejected for a self-contained public build.
Preferred names after verification are `@toni-soler/idax-module-sdk`,
`@toni-soler/idax-ui` and `@toni-soler/idax-i18n`.

**NPM NAMESPACE OWNERSHIP: USER ACTION REQUIRED.** Authenticate the intended
npm owner, create/verify the scope, enable publishing 2FA, configure a trusted
publisher or least-privilege automation token, and retain `npm whoami` plus
package-access evidence. Never place a token in source or documentation.

## Tooling, blockers and next candidate

Keep Gitleaks 8.29.1 by digest over history/tree, exposure scan,
formatter/build/tests, npm audit and Compose validation. Foundation CI adds
CycloneDX JSON SBOM per artifact, exact Maven/npm/Python license policy,
provenance attestation, checksums and review-only signed releases.

Rights are closed. Remaining gates are: merge refresh WIP; add the Core-neutral
bootstrap command/tests; converge frontend loaders; verify npm scope; and run
exact artifact SBOM/license policies. After those gates, extract the LOCAL
refresh/token-validation boundary and tests first, then its thin Auth adapter.
