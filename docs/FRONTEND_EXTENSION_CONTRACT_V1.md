# Frontend extension contract v1

The host loads a signed or integrity-pinned manifest. Product names do not
appear in host code; enabled products are deployment data.

Required module fields:

- `id`, `displayName`, `version` and `contractVersion`;
- `shellCompatibility` as an explicit semantic-version range;
- `entrypoint.url` and `entrypoint.integrity`;
- `routes[]`: path, exported component name and permission;
- `navigation[]`: stable key, route, label key, icon token, group and order;
- `permissions[]`: codes referenced by routes or actions;
- `translations`: locale-to-URL maps with integrity values;
- `assets[]`: URL, media type and integrity;
- optional `backend`: same-origin base path and health path.

The public SDK passed to a lazy entrypoint contains React, router primitives,
authenticated fetch, current identity/tenant, permission checks, i18n resource
registration, stable UI components and an error boundary. It contains no
private service or domain object.

Routes must remain below the module namespace. API paths must remain below the
declared backend prefix. Cross-module imports are forbidden; cooperation uses
versioned HTTP APIs or public events. Unknown manifest fields are rejected for
v1 so evolution requires another contract version.

