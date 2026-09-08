# IDAX Shell architecture

## Open-core boundary

IDAX Shell is a deployable host and public integration surface. It contains
only independently authored adapters, UI, deployment assets and documentation.
It depends on `es.idynamicsax.idax:idax-core:0.3.0` as an unchanged binary and
on the public database migrations distributed by IDAX Core Runtime.

It deliberately contains no application, API, frontend, legacy integration,
generator or generated source from any non-public product.

## Capability assessment for Core 0.3.0

Evidence available from its published POM, migration contract and public module
consumers shows that Core provides:

- tenant, user, membership, credential, role and permission persistence;
- PostgreSQL RLS session context and tenant-aware repositories;
- local, delegated and service-token validation primitives;
- password, JWT, MFA, audit and permission services;
- module permission-catalog parsing and registration;
- public Java contracts already consumed by IDAX Ledger and osTRIS.
- secure first-administrator bootstrap and hardened database identity capabilities.

Core is intentionally not a runnable identity-provider application. The
minimum missing product layer is the reviewed HTTP adapter that binds these
services to Shell authorization, plus production login/token orchestration.
The browser administration UI and public extension discovery already live in
Shell; until that adapter is bound, administration endpoints fail explicitly
with `CORE_RUNTIME_UPGRADE_REQUIRED` and never fall back to independent SQL.

## Runtime topology

The reverse proxy exposes one origin. `/api/shell` reaches Shell, `/api/ledger`
reaches Ledger, `/api/ostris` reaches osTRIS, and all other paths serve the
Shell frontend. Each backend is a separate process and product image. They
share no source tree and use separately versioned Flyway histories.

Frontend extensions use a versioned JSON manifest. A module contributes menu
items and an HTTPS or same-origin ES module entry point. The host validates the
manifest schema and allowed origins before dynamic import. Backend integration
uses HTTP and JWT; Shell never component-scans product backends.

## Trust and bootstrap

There is no default administrator. On an empty installation, the operator
mounts a one-time bootstrap token as a Docker secret. The bootstrap endpoint
accepts it only while no tenant membership exists, compares a SHA-256 digest
in constant time, creates the tenant and owner in one transaction, and then
permanently records completion. The secret file can be removed afterward.

JWT signing keys and database passwords are mounted as external secrets. The
proxy terminates TLS in production. Administrative routes require explicit
Shell permissions and tenant context.

## Synchronization boundary

`scripts/promote-public.ps1` and `scripts/promote-public.sh` synchronize only
files declared in `sync/public-sources.yml`. Every source must itself be a
public, redistributable repository and every destination is checked for
forbidden terms and secrets. Private working trees are rejected. This supports
one-to-one reuse of components after they have been deliberately extracted and
licensed; it cannot silently publish private code.

The binding implementation gate is the
[capability reuse matrix](docs/CAPABILITY_REUSE_MATRIX.md). Canonical ownership
is recorded in [SOURCE_OF_TRUTH.md](docs/SOURCE_OF_TRUTH.md), and the complete
promotion design is in
[PUBLIC_PROMOTION_PIPELINE.md](docs/PUBLIC_PROMOTION_PIPELINE.md).
