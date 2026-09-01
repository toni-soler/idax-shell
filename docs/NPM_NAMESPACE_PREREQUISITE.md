# npm namespace prerequisite

Preferred package names use the `@toni-soler` scope. Before any publication, the owner must:

1. Sign in to npmjs.com with the intended publishing account and enable two-factor authentication for authorization and publishing.
2. Run `npm login`, then `npm whoami`, and verify the result is the intended account.
3. Open the scope/package settings on npmjs.com and verify that the account can create public packages under `@toni-soler`.
4. Optionally prove control without publishing by creating a local package tarball with the intended scoped name and running `npm access list packages @toni-soler` while authenticated.
5. Record the controlled account in the private release runbook; never commit npm tokens.

If that scope cannot be controlled, use an owner-controlled npm organization as the next choice. An unscoped distinctive package name is preferable to publishing under someone else's scope. GitHub Packages is a fallback, but npmjs remains preferred so public consumers do not need repository credentials.

No package has been published by this work.
