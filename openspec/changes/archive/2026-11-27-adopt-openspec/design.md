# Design

## Scope and authorization

On 2026-11-27 the owner approved incremental migration starting with T08b1 and
required generic material outside Git. This supersedes the project skill's earlier
REASONS-only routing. Application/device/publication permissions do not change.

## Decisions

- Global npm OpenSpec 2.2.0 and `init --tools codex --scope global --profile core`;
  no project npm dependency, vendored schema/template or generated generic skill.
- Standard packaged `spec-driven`, project context/rules only in config.yaml.
- Direct baseline migration of T08b1 without inventing product behavior deltas;
  this tooling change sets `skip_specs: true` and has no delta specs.
- One current contract per migrated capability via the capability map. Retain
  original task design/approval/evidence and anchors as history.
- Other capabilities retain legacy authorities until explicitly migrated.
- Preserve local work: checkout 8ff0960 predates documented T08b1 source
  7b3fb08/merge da7f20a. No branch switch or application reconciliation.

## Traceability and evidence

Keep F01–F08 mappings to R/V IDs and original software/device qualifications.
Migration checks mapping and links, not application behavior. F08 remains NOT RUN;
installation alone does not validate a physical ride-page field.

## Risks and rollback

Explicit routing prevents competing specifications. Legacy status paragraphs are
historical; later actual evidence takes precedence without inventing fresh results.
A clean machine needs global npm tooling/skills; the tested command is documented.
Globally discoverable generic skills grant no implementation authorization.

Rollback can restore prior routing and remove the added project artifacts without
changing application code or original evidence. Global tooling remains independent
and is not a build dependency.

## Verification

Use strict OpenSpec validation, template/skill path inspection, changed-skill
validation, relative-link/anchor and R/V/F traceability checks, and whitespace review.
Record results in tasks.md. JVM/Android and device/private-input checks are not rerun
for this documentation/tooling-only change.
