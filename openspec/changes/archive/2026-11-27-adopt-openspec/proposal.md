# Proposal

## Why

Accumulated Canvas/task updates make OxyWatt's current contracts hard to locate.
The owner approved incremental OpenSpec adoption with project npm tooling.

## What Changes

- Pin OpenSpec 2.2.0 in package.json and package-lock.json with npm CLI/check/list scripts.
- Consume the packaged spec-driven schema from ignored node_modules.
- Migrate the existing T08b1 contract with stable R/T/V/F IDs and evidence links.
- Route project instructions through a capability map; preserve legacy contracts and history.
- Leave optional agent integration to each contributor.

## Capabilities

### New Capabilities

None. saved-curve-field is a migration of the existing documented contract, not new
application behavior. This tooling/documentation change sets `skip_specs: true`.

### Modified Capabilities

None. T08b1/F01–F08 behavior and evidence qualifications are preserved.

## Impact

R03/R04/R07–R12 and existing T08b1 V/F mappings are retained, not reaccepted.
Paths: npm manifests, openspec/, repository documentation and OxyWatt-specific skills.
The owner authorized self-review and PR publication. No app/build/CI changes,
hardware/private-input execution or scientific-method approval is included.
