# Core 0.3 JDBC debt register

Core 0.3 composes the existing Core services without rewriting their persistence.
Normal new domain persistence remains JPA-first. The following existing JDBC is
recorded for a later, dedicated **JPA NORMALIZATION / ORDINARY JDBC REDUCTION**
gate.

| Class / methods | Current JDBC type | Category | JPA candidate? | Future gate |
| --- | --- | --- | --- | --- |
| `RolePermissionService` — role listings, permission replacement and user-role assignment | Direct SQL for role/permission aggregates and assignments | ORDINARY JDBC | Yes | Normalize role and RBAC persistence after Core 0.3 |
| `IdaxMessagingService` — group, message and recipient operations | Direct SQL for messaging CRUD and projections | ORDINARY JDBC | Yes | Introduce JPA repositories/projections while preserving tenant RLS |
| `PermissionService` — effective-permission authorization reads | Direct SQL on the hot authorization path | JUSTIFIED SPECIAL | Yes, subject to performance and security proof | Evaluate a JPA/query projection without weakening authorization or RLS |
| `PermissionCatalogLoader` — catalog bootstrap and idempotent role-permission seeding | Bulk/idempotent bootstrap SQL combined with repository access | JUSTIFIED SPECIAL | Partially | Separate bootstrap mechanics from ordinary catalog persistence |
| `TenantOnboardingService` — privileged tenant/bootstrap operations | Explicit SQL and database-role transitions for privileged onboarding | SECURITY CAPABILITY | Ordinary portions only | Split privileged capabilities from JPA-eligible domain persistence |

This register is documentary. No JDBC implementation is changed by the Core
0.3 composition gate.
