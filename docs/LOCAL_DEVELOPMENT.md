# Local development quick steps with VS Code

This workspace opens and validates the four public repositories together:
IDAX Shell, IDAX Core Runtime, IDAX Ledger and osTRIS. The expected directory
layout is:

```text
github-public/
  idax-shell/
  idax-core-runtime/
  idax-ledger/
  ostris/
```

## Current capability boundary

The Shell includes its Spring Boot backend, public React frontend, local
authentication, module navigation and a reusable administration workspace.
Users, roles and alerts share the same CRUD, filtering and saved-filter
components. **Explore the interface** runs these screens with ephemeral demo
data. A real authenticated session uses Core's validated `CurrentUser`, tenant
context, permission checks, JPA services and PostgreSQL RLS. The Shell adapters
do not query domain tables or maintain a second CRUD implementation.

## Prerequisites

- Git and VS Code.
- Extension Pack for Java (Java 21), Maven 3.9 and Node.js 22.
- Docker Desktop with Linux containers and Docker Compose v2.
- OpenSSL. Git for Windows includes it in a standard installation.

Clone all repositories as siblings, then open
`idax-shell/idax-open-core.code-workspace` in VS Code. The workspace disables
automatic Java builds and asks before Maven project reimports, preventing the
Java extension from racing command-line Maven builds.

## Create local-only secrets

Run the initialization script from the `idax-shell` terminal before executing
any Docker build or Compose command. The `.local` directory is ignored by Git.

```powershell
./scripts/initialize-local-env.ps1
```

On Linux or macOS:

```sh
./scripts/initialize-local-env.sh
```

The scripts are idempotent and preserve existing secret files. They also repair
empty directories that Docker Desktop may create when a missing bind-mounted
secret path is referenced too early.

Never commit these files or reuse them outside local development.

## Validate the sources

In VS Code select **Terminal > Run Task** and run
**Open Core: validate all sources**. This executes Maven verification and the
frontend tests/builds for Shell, Ledger and osTRIS, followed by Compose schema
validation. Maven resolves `es.idynamicsax.idax:idax-core:0.3.0` from the public
GitHub Pages Maven repository.

## Start the local stack

Run the task **Open Core: start local stack**, or:

```sh
docker compose -f compose.local.yml up -d --build
docker compose -f compose.local.yml ps
```

The source-built Shell is exposed through Caddy at
`http://localhost:8088`.

For this local environment only, the Shell creates a demo tenant and owner:

```text
Email:    admin@local.test
Password: IdaxLocal123!
```

Open the URL and use those credentials. They are deliberately predictable for
the quick start and must never be enabled or reused in staging or production.
The initializer is disabled by default in the application and is enabled only
by `compose.local.yml`. To choose different local credentials before starting:

```powershell
$env:IDAX_LOCAL_DEMO_EMAIL = "me@example.test"
$env:IDAX_LOCAL_DEMO_PASSWORD = "A-different-local-password"
docker compose -f compose.local.yml up -d --build
```

Useful checks are:

```sh
curl --fail http://localhost:8088/actuator/health/readiness
curl --fail http://localhost:8088/
```

## OpenAPI / Swagger local

The local Compose file publishes the module backends on the host for API
exploration:

| Service | Swagger UI | OpenAPI JSON |
| --- | --- | --- |
| Shell | `http://localhost:8088/swagger-ui/index.html` | `http://localhost:8088/v3/api-docs` |
| IDAX Ledger | `http://localhost:8094/swagger-ui/index.html` | `http://localhost:8094/v3/api-docs` |
| osTRIS | `http://localhost:8095/swagger-ui/index.html` | `http://localhost:8095/v3/api-docs` |

Swagger UI and its OpenAPI document are deliberately public only in this local
configuration. Business endpoints still require the bearer token issued by the
Shell. Use the browser application to sign in first, then copy its access token
from the browser developer tools and select **Authorize** in Swagger UI with
`Bearer <token>`.

The published `8094` and `8095` ports are for local development only; do not
expose them in a production reverse proxy or firewall rule.

Successful login opens the public Shell and its tenant context. The
**Explore the interface** button remains available as a UI-only demonstration;
use the credentials above when testing authentication.

To test the frontend administration workflow without changing the database:

1. Select **Explore the interface**.
2. Open **Users**, **Roles** or **Alerts** from the resizable menu.
3. Create, edit and delete demo records.
4. Choose a field and value, save the filter, then restore or delete it from
   the chips below the filter bar.
5. From Users or Roles, select **Create alert** to move to the alert workspace.

Demo records exist only in memory. Demo saved filters use browser local storage
under `idax.demo.filters.*`; they are not sent to the backend. With a real
session, saved filters and CRUD requests target `/api/shell/v1/tenants/{id}`.
The Roles editor loads the Core permission catalog. Saved-filter alerts and
configured alert definitions deliberately remain separate API contracts:

```text
/api/shell/v1/tenants/{id}/alerts       saved-filter alert requests
/api/shell/v1/tenants/{id}/core-alerts configured alert definitions
```

Inspect logs with:

```sh
docker compose -f compose.local.yml logs -f --tail=200 shell ledger ostris
```

Stop the environment without deleting its database using the VS Code task
**Open Core: stop local stack**, or:

```sh
docker compose -f compose.local.yml down
```

Only use `docker compose -f compose.local.yml down --volumes` when intentionally
discarding all local development data.

## Before calling the distribution complete

Before publishing a Core increment, run the complete source validation and a
real PostgreSQL exercise that logs in, selects a tenant, lists users and roles,
loads the permission catalog, saves a filter, creates an alert, and proves that
the same non-superuser identity is rejected for a different tenant. Keep the
candidate unpublished if any step fails.
