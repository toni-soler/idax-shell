# Frontend module loading security

Module discovery is deployment inventory, never Internet discovery. An
operator installs an explicit manifest and integrity-pinned assets from the
same release set. The host validates a closed schema, module identity,
compatibility range, route/API namespaces, permissions and SHA-384 SRI before
creating any script or stylesheet element.

Production defaults permit same-origin extension assets only. Optional trusted
origins require an explicit deployment allowlist and HTTPS. Redirects to an
origin outside that list fail. Unknown manifest fields, incompatible versions,
missing integrity and duplicate routes/navigation keys fail closed.

Recommended CSP:

```text
default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none';
base-uri 'none'; frame-ancestors 'none'; connect-src 'self' <declared APIs>;
img-src 'self' data:; require-trusted-types-for 'script'
```

The SDK exposes stable capabilities rather than global product objects. No
extension receives secrets, raw refresh tokens, arbitrary DOM injection or
unrestricted network clients. Ledger and osTRIS are configuration entries, not
hardcoded imports.

