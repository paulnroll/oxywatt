# Design

## Scope and authorization

On 2026-11-27 the owner approved incremental migration starting with T08b1,
project package.json/lockfile tooling, self-review and PR publication. Optional
agent integration is each contributor's choice. Application/device permissions
and merge/release remain outside this change.

## Decisions

- Exact OpenSpec 2.2.0 devDependency in a private developer-tools package; npm ci
  restores its lockfile and npm scripts use the local CLI with telemetry disabled.
- Consume the standard packaged spec-driven schema from ignored node_modules;
  project context/rules belong in openspec/config.yaml.
- Direct baseline migration of T08b1 without inventing behavior deltas; the
  tooling-only adoption change sets skip_specs: true.
- One current contract per migrated capability. Original task design/approvals/
  evidence and anchors remain history; other capabilities retain legacy authorities.
- Preserve local work by preparing the PR in an isolated branch from main dd7edd9,
  rather than publishing the older checkout's mirrored documentation.

## Traceability and evidence

Keep F01–F08 mappings to R/V IDs and original software/device qualifications.
Review reconciled F08 to PARTIAL from the newer evidence on main: limited physical
editor/binding/rounding/date/HOME-reopen observations passed; recording-time,
process-death and remaining layout/error cases remain unverified.
Migration checks documentation, not application/device behavior.

## Risks and rollback

Explicit routing prevents competing contracts. npm ci restores pinned tooling
without requiring separately installed CLI or agent integrations. Neither setup
nor artifact readiness grants implementation authorization.
Rollback can restore prior routing and remove added specification/npm files
without changing application code or original evidence.

## Verification

Use strict OpenSpec and archived-task validation, local CLI/template resolution,
changed-skill validation, link/anchor and R/V/F traceability checks, and whitespace
review. Record actual results in tasks.md. No new device/private-input execution.
