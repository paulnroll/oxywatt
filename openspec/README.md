# OxyWatt specification map

OpenSpec is the workflow for new changes from 2026-11-27. Migration is incremental:
only the capabilities listed below have moved. Existing approvals, requirement IDs,
scientific gates and device evidence retain their original meaning.

Migration scope and verification: [completed adoption change](changes/archive/2026-11-27-adopt-openspec/tasks.md).

## Authorities

| Area | Maintained authority | History and evidence |
|---|---|---|
| Product intent, R01–R12 and N01–N03 | [Requirements](../docs/product/requirements.md) | Existing linked tasks |
| Cross-cutting V acceptance IDs | [Validation strategy](../docs/algorithms/validation-strategy.md) | Existing linked evidence |
| Saved power curve ride field, T08b1 | [saved-curve-field specification](specs/saved-curve-field/spec.md) | [T08b1 design, F01–F08 and evidence](../docs/spdd/tasks/t08b1-saved-curve-field.md) |
| Other capabilities and architecture | Existing [Canvas](../docs/spdd/reasons-canvas.md), [plan](../docs/spdd/implementation-plan.md), tasks and architecture/algorithm documents | [Decision log](../docs/spdd/decision-log.md) |
| Current permissions and invariants | [AGENTS.md](../AGENTS.md) | Owner decisions in tasks and session instructions |

For a migrated capability, change its OpenSpec contract through a change delta;
do not maintain the historical behavior prose as a second specification.
Retain existing task files and anchors for original designs, approvals and evidence.
For an unmigrated capability, read its legacy contract, then migrate that contract
with explicit provenance before extending it. Proposed or unverified behavior stays
visibly proposed or unverified. Keep R/T/V and task-local IDs stable.

The saved-field baseline comes from the recorded merged T08b1 implementation,
not a fresh source verification of this checkout. Its recorded implementation
head is 7b3fb08 (0.9.0/code 9), merge da7f20a. This checkout started at 8ff0960
with local follow-up documentation. Migration does not reconcile application code
or rerun earlier software/device checks. Publication review used an isolated worktree
on main dd7edd9, preserved its newer authorizations and reconciled F08 to PARTIAL.

## New work

Use `npm run openspec -- new change <name>` and the packaged `spec-driven` schema. Keep
proposal, behavior deltas, design and tasks together under `openspec/changes/`.
Project guidance is in [config.yaml](config.yaml); detailed engineering rules
remain in the OxyWatt skills. Artifacts do not approve application implementation.
Small corrections need proportional documentation; tooling/docs-only changes
can set `skip_specs: true` in `.openspec.yaml` when behavior is unchanged.

Before implementation identify the named authorization and acceptance IDs.
Record actual verification in the change or linked evidence, synchronize applicable
architecture/scientific documents, and log material intent decisions. Completed
changes can be archived after verification; this is workflow completion, not
product release or hardware/scientific acceptance.

## Project tooling

OpenSpec **2.2.0** is pinned in the root package.json and package-lock.json.
Node **22.20.0 or newer** is required; Node 24.14.0 was used here. This private
developer-tools package is independent of Android app versioning/builds.
Dependencies and packaged schemas/templates are installed in ignored `node_modules/`.
Agent integration is optional and configured by each contributor.

```sh
npm ci --ignore-scripts --no-fund --no-audit
npm run spec:list
npm run spec:check
npm run openspec -- list
npm run openspec -- templates --json
```

The npm CLI script disables OpenSpec telemetry. Structural validation does not
prove agreement with application code, hardware behavior or scientific validity.
