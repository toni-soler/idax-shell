# Public promotion pipeline

The pipeline is a trusted-environment tool, not a runtime dependency.

Each export manifest declares:

```yaml
schemaVersion: 1
component: auth-web
source:
  repositoryId: approved-private-repository-alias
  ref: immutable-commit
  paths: [path/to/component]
destination:
  repository: idax-auth-web
  path: .
include: [src/**, pom.xml]
exclude: [target/**, "**/*.generated.*"]
transformations:
  - id: normalize-public-package
    version: 1
license:
  expectedSpdx: Apache-2.0
forbidden:
  dependencies: [private-group:*]
  imports: [private.package.**]
  patterns: [internal-hostname, local-path]
generatedFiles:
  policy: reject
checks:
  format: [mvn, spotless:check]
  build: [mvn, verify]
```

Execution resolves an immutable source commit, copies only the allowlisted file
set into a clean temporary tree, applies named/versioned transformations,
normalizes modes and line endings, and produces identical bytes for identical
inputs. It then compares the tree with the public destination to report drift.

After formatter, tests, dependency/license inventory, secret scanning and
forbidden-pattern checks pass, the tool creates a local patch or a named review
branch with a provenance report and SBOM delta. Publishing requires human
approval and protected-branch CI. The tool has no mode that writes directly to
the default branch.

The reference implementation is `tools/public_promote.py`. It consumes JSON
manifests validated against `sync/export-manifest.schema.json`, requires an
immutable source commit, stages into a clean temporary directory, reports drift
with exit code 2, and refuses `--apply` on `main` or `master`. It never fetches,
pushes, opens a pull request or reads an undeclared repository.
The trusted execution environment must also supply `--repository-registry`, a
JSON map from the manifest's opaque repository alias to an allowed origin-URL
regular expression. That registry is operational policy and is not committed
to the public destination.
Every manifest must invoke Gitleaks for the staged export; `true` or an omitted
secret scanner is rejected before copying any file.

Extraction is complete when the private product consumes the public package.
At that point the export manifest is retired and the public repository is the
only source of truth.
