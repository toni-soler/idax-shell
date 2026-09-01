# Public Auth/Admin API v1 — draft

All authenticated tenant routes require a bearer access token, validated
`TenantContext`, tenant membership and the listed permission. Errors use a
closed `{code,message,requestId}` shape; `message` contains no credential or
existence oracle.

| Method | Path | Authentication / permission | Tenant semantics | Request → response |
|---|---|---|---|---|
| POST | `/api/auth/v1/login` | anonymous, rate limited | tenant resolved after identity; ambiguity returns selection challenge | credentials → token pair or MFA challenge |
| POST | `/api/auth/v1/refresh` | signed refresh token | immutable claims revalidated against current user state | refresh token → rotated token pair |
| POST | `/api/auth/v1/mfa/verify` | short-lived signed challenge | challenge binds user/provider | challenge+code → token pair |
| POST | `/api/bootstrap/v1/first-admin` | one-shot operator secret | creates initial tenant/owner atomically | bootstrap DTO → 201/no secret response |
| GET/POST | `/api/admin/v1/tenants` | superuser; tenant create permission | global admin operation | tenant DTO(s) |
| GET/POST | `/api/admin/v1/tenants/{tenantId}/users` | `system.users.read/create` | path must equal authorized context | user DTO(s) |
| PUT/DELETE | `/api/admin/v1/tenants/{tenantId}/users/{userId}` | `system.users.update/delete` | same-tenant only | user DTO / 204 |
| POST | `/api/admin/v1/tenants/{tenantId}/memberships` | tenant manager | target tenant fixed by path | membership DTO |
| GET/POST | `/api/admin/v1/tenants/{tenantId}/roles` | `system.roles.read/manage` | same-tenant only | role DTO(s) |
| PUT/DELETE | `/api/admin/v1/tenants/{tenantId}/roles/{roleId}` | `system.roles.manage` | role ownership checked | role DTO / 204 |
| GET | `/api/admin/v1/permissions` | `system.roles.read` | catalog is global definitions, grants remain tenant scoped | permission DTOs |
| PUT | `/api/admin/v1/tenants/{tenantId}/roles/{roleId}/permissions` | `system.roles.manage` | assignment only | permission codes → effective codes |
| PUT | `/api/admin/v1/tenants/{tenantId}/users/{userId}/roles` | `system.roles.manage` | all roles must belong to tenant | role IDs → 204 |

The minimal public surface omits legacy data-area administration, generated
Flyway export, product preferences and business audit projections. Those need
separate contracts rather than accidental exposure of all Core services.

