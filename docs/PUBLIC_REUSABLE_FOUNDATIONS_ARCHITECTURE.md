# Public reusable foundations architecture — Wave 1

## Resulting boundaries

The dependency analysis supports option A with two web adapters:

```text
IDAX Core Runtime 0.2.x (existing binary + database contract)
        ↑                         ↑
idax-auth-web                 idax-admin-web
        ↑                         ↑
        └──────── IDAX Shell host ┘

module-contract/module-sdk ← module manifests and extension bundles
ui + i18n                  ← Shell and module frontends
```

Auth and Admin must not be merged. Auth has key material, token lifetimes,
provider modes and anonymous endpoints. Admin has tenant-scoped authorization,
transactions, RLS and administrative DTOs. Both consume Core, but Admin does
not depend on Auth. Shell is only the composition root and security policy.

## Minimal repository strategy

Do not create one repository per small package. The proposed minimum is:

1. `toni-soler/idax-foundations-java`: Maven multi-module repository containing
   `idax-auth-web` and `idax-admin-web`, plus an optional BOM when a second
   released module exists.
2. `toni-soler/idax-foundations-web`: npm workspace containing the module
   contract/SDK and, only after real extraction, UI and i18n packages.
3. Existing product repositories remain canonical for Ledger and osTRIS.

No repository is created until ownership/licensing and npm namespace gates are
resolved. This avoids empty public packages and permanent sync copies.

## Proposed coordinates

- `io.github.toni-soler.idax:idax-auth-web`
- `io.github.toni-soler.idax:idax-admin-web`
- later `io.github.toni-soler.idax:idax-foundations-bom`

These public adapter coordinates deliberately differ from the separately
licensed Core coordinate. Java packages should begin
`io.github.tonisoler.idax.*`; API DTO namespaces are versioned through SemVer,
not URL-domain ownership assumptions.

The `@idax/*` npm package names currently return 404, but this machine is not
authenticated to npm and scope ownership cannot be proven. Therefore the
namespace is blocked. Preferred fallback is a verified owner scope such as
`@toni-soler/idax-module-sdk`; GitHub Packages is also viable. No npm package
will be published until the selected account controls the scope.

## Source-of-truth transition

```text
CURRENT                          TRANSITION                     TARGET
private auth/admin/UI code  → allowlisted reviewed extraction → public foundations
private host                → consumes temporary adapter       → consumes public release
Shell                       → waits                            → consumes same public release
```

After migration, public foundations are canonical. Promotion manifests are
retired rather than used for bidirectional synchronization.

## Versioning

Each Maven artifact and npm package follows independent SemVer. Breaking Java
API/DTO or frontend SDK changes require a major version. Module manifests state
their contract version and a Shell SemVer range. Shell versions do not force
Ledger, osTRIS, UI or i18n versions to move together.

## Current stop conditions

- Platform and Frontend have no public source license declaration.
- MFA is structurally part of LOCAL login; it cannot be silently omitted.
- refresh-token verification must use the authoritative decoder before auth is
  eligible for extraction.
- `@idax` npm scope ownership is unverified.
- the full audit service contains private Sales interpretation and cannot be
  promoted as a generic audit runtime.

