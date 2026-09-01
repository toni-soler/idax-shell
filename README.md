# IDAX Shell

IDAX Shell completes the public open-core distribution around IDAX Core
Runtime. It is an independently authored Spring Boot host and React shell. It
does not contain or redistribute any private application, API, frontend,
legacy integration or generator source.

## What is included

- the architecture and reuse gate for login, JWT, secure first-owner bootstrap,
  tenants, users, memberships, roles and permissions;
- responsive base navigation and versioned frontend extension discovery;
- a production-oriented Compose topology for PostgreSQL, Core, Shell, Ledger,
  osTRIS and Caddy;
- CycloneDX SBOM generation and guarded public-source promotion scripts.

Read [ARCHITECTURE.md](ARCHITECTURE.md), the
[capability reuse matrix](docs/CAPABILITY_REUSE_MATRIX.md) and the
[open-core boundary](docs/OPEN_CORE_BOUNDARIES.md) before contributing. Version
0.1.0 is an architecture checkpoint, not yet an installable production release.

## Build

Requirements: Java 21, Maven 3.9, Node 22 and Docker Compose v2.

```sh
mvn verify
cd frontend
npm ci
npm test
npm run build
```

The Core binary is resolved from its public Maven repository and remains under
its own binary license.

## Linux installation

1. Clone this repository at a signed release tag.
2. Clone the public Core Runtime repository into
   `vendor/idax-core-runtime`, check out the compatible release and verify its
   published checksum.
3. Create Docker secrets named `postgres_password`, `jwt_private_key`,
   `jwt_public_key` and `bootstrap_token`. Use at least 32 random bytes for the
   bootstrap token and an RSA key of at least 3072 bits.
4. Set `IDAX_HOSTNAME` and immutable image references in `.env`.
5. Run `docker compose config`, then `docker compose up -d`.
6. Call `POST /api/shell/v1/bootstrap` once with the bootstrap secret in
   `X-Bootstrap-Token`, then remove that secret from the service definition and
   redeploy.

The bundled Compose file assumes Docker Swarm-style external secrets. For plain
Compose, use an external secret provider or bind read-only root-owned files
under `/run/secrets`; never store secret values in `.env`.

## Updates

Pin every image by digest. Back up first, pull the new tags, inspect release
notes and SBOMs, run migration jobs, then roll backends one at a time. Flyway
migrations are forward-only; application rollback never reverses an applied
schema migration.

## Backup and restore

Take encrypted `pg_dump --format=custom` backups and separately back up Caddy
state and external secret metadata. Test restoration regularly on an isolated
network. Restore PostgreSQL before starting migration jobs or application
containers; rotate service credentials after any disaster recovery event.

## Operations

Monitor `/actuator/health/readiness`, certificate renewal, PostgreSQL capacity,
authentication failures and migration status. Restrict the database network,
run containers as non-root, forward logs to an external store and alert on any
unexpected bootstrap request after installation.

## Safe synchronization

The promotion tool only accepts a closed allowlist manifest conforming to
`sync/export-manifest.schema.json`, pins an immutable source commit and scans
for secrets and environment-specific paths. Reusable code should first be
extracted into a separately reviewed, licensed public component; Shell can then
consume it one-to-one. The exporter only produces drift reports or changes on a
review branch and cannot publish silently to the default branch.
