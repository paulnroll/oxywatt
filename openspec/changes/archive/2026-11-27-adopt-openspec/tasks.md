# Tasks

## Adoption

- [x] Pin OpenSpec 2.2.0 in the private package.json and package-lock.json; ignore node_modules.
- [x] Add project context, packaged-schema usage and the capability/authority map.
- [x] Migrate T08b1 with stable R/T/V/F traceability and evidence-qualified F08 PARTIAL.
- [x] Route project instructions and skills through the map, preserving legacy evidence.
- [x] Verify npm installation, OpenSpec, links, traceability, skills and whitespace; self-review and publish PR #59.

## Evidence — 2026-11-27

- Clean npm ci with ignore-scripts/no-fund/no-audit installed 66 packages from the
  lockfile. Local CLI reports 2.2.0; all four templates resolve from its npm package.
- Strict baseline and archived-task validation PASS. skip_specs intentionally
  describes a tooling/docs-only migration with no product behavior delta.
- Documentation check: 13 Markdown files / 277 relative links and anchors PASS;
  F01–F08 and referenced R/V IDs resolve to the original acceptance documents.
- Installed skill-creator validator: oxywatt-spec, oxywatt-developer and oxywatt-pr PASS.
- Lockfile uses public npm registry URLs and integrity hashes; no credentials or
  local paths. node_modules is ignored; whitespace checks PASS.
- Self-review against main dd7edd9 corrected stale F08 NOT RUN to PARTIAL using
  existing physical-editor evidence and preserved later main authorizations.
- PR #59 was published as commit 4f2ba12; its four existing CI checks passed before
  the subsequent documentation correction below.

## Owner correction: optional agent integration

The owner requires no project policy about where contributors install agent tools
or skills. Final instructions describe package.json/lockfile and npm commands only;
optional integrations are left to contributors. The dependency pin is unchanged.
Documentation was checked again before updating the same PR.

No new application/device/private-input checks, model approval, merge or release
are claimed by this change. Existing T08b1 software/device results remain linked
historical evidence, and F08 stays PARTIAL.
