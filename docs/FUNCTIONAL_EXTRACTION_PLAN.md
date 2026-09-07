# Functional extraction plan

## Objective

Turn IDAX Shell into the independently deployable public host for IDAX Core,
Ledger and osTRIS. The private implementation is the behavioral reference, but
the public repository must contain only the generic platform vertical and a
newly designed user interface.

## Public boundary

The Shell may include:

- local login, JWT access/refresh tokens and optional MFA;
- secure one-shot first-administrator bootstrap;
- tenant onboarding and tenant selection;
- user, membership, role and permission administration;
- the current user's profile and password operations;
- module discovery, navigation and extension loading;
- generic error handling, localization and responsive layout.

The Shell must not include:

- `idax-legacy`, Dynamics AX entities, routes or synchronization;
- application-specific APIs from the private `idax-app`;
- generators or generated legacy screens;
- private modules, customer configuration or internal infrastructure names;
- copied private visual assets, layout or styling.

## Backend extraction map

| Public capability | Required behavior | Public implementation |
| --- | --- | --- |
| Login and refresh | Credential verification, token issue and refresh | Shell-owned controllers and orchestration using Core repositories and JWT configuration |
| First administrator | Core `FirstAdministratorBootstrapService` | Consume from the next Core runtime; do not duplicate privileged bootstrap SQL in Shell |
| Tenant lifecycle | Tenant creation, update and enablement | Thin Shell controllers over Core tenant services |
| Tenant users | Membership and user administration | Thin Shell controllers over `TenantUserService` and `TenantMembershipService` |
| Roles and permissions | Role lifecycle and permission assignment | Thin Shell controller over `RolePermissionService` |
| Current session | Identity, tenant selection and session renewal | Shell-owned `/api/shell/v1/me` contract without legacy/data-area assumptions |
| Modules | Safe discovery and navigation | Versioned public extension manifest and explicit allowlist |

Published Core Runtime 0.2.0 already exposes the tenant-user and
role-permission services, entities and repositories required by much of this
vertical. It does not contain `FirstAdministratorBootstrapService`, although
that generic capability is planned for the next Core runtime. A new Core
runtime must therefore be published before implementing bootstrap in Shell. The Shell must
not work around this by adding a second bootstrap implementation.

## API sequence

1. Publish a compatible Core runtime containing the reviewed generic bootstrap
   service and its migration.
2. Implement `/api/shell/v1/bootstrap/status` and one-shot bootstrap.
3. Implement `/api/shell/v1/auth/login` and `/refresh` with generic local
   identity lookup, rate limiting and audit.
4. Implement `/api/shell/v1/me` and tenant switching.
5. Add tenant, user, membership, role and permission administration endpoints.
6. Register Ledger and osTRIS as allowlisted modules and validate their JWT
   audience, tenant and permission semantics end to end.

Every step requires PostgreSQL integration tests. Authentication errors must
not reveal whether a user exists, and tenant-scoped operations must be tested
against cross-tenant access.

## Public frontend direction

The public UI will be designed independently rather than reskinned from the
private frontend:

- a compact top bar plus contextual workspace rail instead of copying the
  private navigation structure;
- neutral slate surfaces with the public IDAX cyan/blue brand as an accent;
- an installation/bootstrap experience before the login screen;
- tenant context always visible and switchable from the top bar;
- administration presented as a dedicated control center;
- module cards and routes generated from the signed/allowlisted extension
  manifest;
- responsive layouts and accessible keyboard/focus behavior from the first
  implementation.

The frontend will use React and JavaScript, expose no private component imports,
and maintain its own design tokens and component primitives. Translations must
cover the same 12 public locales used by the modules.

## Acceptance gate

The first installable release is blocked until an automated scenario can:

1. start the source-built Compose stack on an empty PostgreSQL volume;
2. bootstrap exactly one administrator and reject a second bootstrap;
3. log in and refresh a token;
4. create a tenant and user, assign a role and permission, and switch tenant;
5. deny the same operations across tenant boundaries;
6. open the public shell and discover Ledger and osTRIS;
7. pass backend/frontend tests, secret/exposure scanning and SBOM generation.
