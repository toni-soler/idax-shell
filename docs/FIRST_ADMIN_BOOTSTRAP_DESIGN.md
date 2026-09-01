# First administrator bootstrap design

Bootstrap belongs to the Admin Web adapter and composes Core's onboarding and
audit services. It is not authentication logic and is never implemented in
Shell.

The endpoint exists only when an explicit bootstrap-secret file is configured.
The service starts fail-closed if the file is missing, world-readable, empty or
below the entropy policy. Only a SHA-256 digest is retained in memory; request
comparison is constant-time and responses never reveal whether the secret or
state check failed.

Inside one PostgreSQL transaction it obtains a transaction-scoped advisory
lock with a fixed public namespace, rechecks that no effective owner/admin or
superuser exists, claims a singleton bootstrap-state row, invokes Core tenant
onboarding, assigns the canonical owner role and writes a security audit event.
The unique singleton plus advisory lock makes two application instances safe.
On success the state is irreversible through the public API and subsequent
calls return a generic unavailable response. Operators then unmount the secret.

The design requires PostgreSQL/Testcontainers tests for concurrent requests,
rollback, pre-existing admin, invalid secret, success and permanent disablement.

