# Proposal

## Why

Accumulated Canvas/task updates make OxyWatt's current contracts hard to locate.
The owner approved incremental OpenSpec adoption and requested generic tooling
and generated guidance outside the repository.

## What Changes

- Install global npm OpenSpec and globally scoped generic skills.
- Use the packaged spec-driven schema with only project artifacts/configuration in Git.
- Migrate the existing T08b1 contract with stable R/T/V/F IDs and evidence links.
- Route project instructions through a capability map; preserve legacy contracts and history.

## Capabilities

### New Capabilities

None. saved-curve-field is a migration of the existing documented contract, not new
application behavior. This tooling/documentation change sets `skip_specs: true`.

### Modified Capabilities

None. T08b1/F01–F08 behavior and acceptance status are preserved.

## Impact

R03/R04/R07–R12 and existing T08b1 V/F mappings are retained, not reaccepted.
Paths: `openspec/`, repository documentation and OxyWatt-specific skills.
No app/build/CI changes, branch reconciliation, hardware/private-input execution,
scientific-method approval or publication is included.
