# OxyWatt agent instructions

## Current authorization

Discovery, architecture, specifications and project skills only. Do not generate application code, scaffold Android, or implement a diagnostic app until the owner explicitly approves a named implementation slice. The first milestone ends with reviewed domain model, REASONS Canvas and source-verified SDK capabilities. Device behavior remains conditional until tested.

## Routing

- Product intent: `docs/product/requirements.md`.
- Every substantive change: `docs/spdd/reasons-canvas.md`, relevant task in `docs/spdd/tasks/`, and `docs/spdd/decision-log.md`.
- Karoo integration: `docs/discovery/karoo-sdk-feasibility.md` and `.agents/skills/karoo-extension/SKILL.md`.
- Scientific changes: relevant `docs/algorithms/` document, `docs/discovery/scientific-research.md`, and `.agents/skills/oxywatt-models/SKILL.md`.
- Spec creation, readiness and synchronization: `.agents/skills/oxywatt-spec/SKILL.md`.
- Approved implementation, fixes and refactors: `.agents/skills/oxywatt-developer/SKILL.md` (minimal complete changes and behavior-driven TDD).

## Invariants

Collector normalizes Karoo and future FIT inputs to one source-neutral model. The core has no Android, Hammerhead, Room or Compose types. FTP and VO₂max are separate models; Critical Power is not FTP. Never invent SDK APIs, measurement timestamps, historical access or process survival guarantees. Distinguish facts, observations, estimates and manual calibration. Missing data is not zero. Input quality is not calibrated physiological confidence.

Version model, preprocessing, parameters, rider settings and trend configuration. Retain inputs needed for reproducibility. Record gaps and ambiguous recovery honestly. Never merge uncertain sessions automatically. Do not commit private rides, GPS traces, credentials, keystores or telemetry. Read-only research does not authorize publishing or contacting others.

## Workflow

Read the applicable specification, identify requirement/task/test IDs, then work within the approved scope. Propose material behavior or scope changes in the Canvas and decision log before implementation. Routine choices inside approved constraints need no repeated approval. Do not introduce mandatory agents, elaborate governance, or speculative modules.

Update task evidence and documentation with actual results. A source-verified capability is not hardware-verified. Do not mark an implementation or hardware check complete without evidence. Small corrections need no new full specification.

## Verification

Currently documentation only: check relative Markdown links, requirement/task/test traceability, YAML skill frontmatter, and `git diff --check`. Validate skills with the installed skill-creator validator when available. No Gradle project exists; do not report application tests as run. Once build tooling is approved, add real commands here and run the checks appropriate to each slice.
