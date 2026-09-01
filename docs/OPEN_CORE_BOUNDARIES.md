# Open-core boundaries and provenance gate

## Categories

- **A — consume:** capability is already in a published binary/package or
  public product. Shell adds no equivalent implementation.
- **B — extract:** a reusable implementation exists but is not independently
  public. Extraction is reviewed for copyright, dependencies, configuration,
  secrets, terminology and tests before the public component becomes canonical.
- **C — clean room:** private coupling cannot be removed economically. A new
  implementation is designed only from published contracts and observable
  behavior, with recorded provenance.
- **D — compose:** Shell supplies wiring, configuration, routing or policy only.

Every implementation PR must cite one matrix row and one category. Category C
requires an architecture decision record explaining why A and B failed.

## Publication gate

No exported file is publishable until all checks pass: explicit copyright
authority; SPDX license; allowlisted inputs; deterministic transform; forbidden
import/dependency scan; secret scan; internal reference scan; generated-source
policy; formatter; build; tests; SBOM/license inventory; human diff approval.

The public checkout never receives credentials or network access for private
repositories. Promotion runs in a trusted integration environment and opens a
review branch or emits a patch. It cannot push to the protected default branch.

