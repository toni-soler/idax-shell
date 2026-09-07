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

The current Shell source includes an executable Spring Boot backend and a Vite
frontend build. The frontend is still the architecture-checkpoint page and the
extension manifest is empty. Login, first-owner bootstrap, tenant, user, role
and permission administration screens and the runtime loading of the Ledger and
osTRIS frontend extensions are not implemented yet. The local stack is useful
for source builds, database migrations, backend health checks and integration
work; it is not yet a complete end-user application.

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
validation. Maven resolves `es.idynamicsax.idax:idax-core:0.2.0` from the public
GitHub Pages Maven repository.

## Start the local stack

Run the task **Open Core: start local stack**, or:

```sh
docker compose -f compose.local.yml up -d --build
docker compose -f compose.local.yml ps
```

The source-built Shell is exposed through Caddy at
`http://localhost:8088`. Useful checks are:

```sh
curl --fail http://localhost:8088/actuator/health/readiness
curl --fail http://localhost:8088/
```

The root page currently displays the Shell architecture checkpoint. A healthy
page proves the backend-served frontend bundle is present; it does not prove
that login or administration features exist.

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

Implement and verify the Shell authentication/bootstrap APIs, administration
UI, extension manifest entries, frontend extension serving/loading, and
service-to-service authentication. Then add an end-to-end test that logs in,
creates or selects a tenant, manages a user role, opens Ledger and osTRIS from
the Shell navigation, and verifies tenant isolation.
