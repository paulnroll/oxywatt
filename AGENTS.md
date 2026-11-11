# OxyWatt agent instructions

## Current authorization

Owner approved T01a and the source-neutral core/adapters architecture on 2026-11-10: minimal JVM build, canonical input contracts and their JVM tests. See `docs/spdd/tasks/t01a-canonical-input.md`. Owner resumed T01b on 2026-11-10 after PR #3 merged: synthetic replay fixtures and independent expected outputs only; completed with evidence in `docs/spdd/tasks/t01b-synthetic-replay.md`. Owner approved T01c after PR #6 merged. Developer FIT decoding and canonical replay are verified on a synthetic fixture in `docs/spdd/tasks/t01c-fit-replay.md`. Owner also approved formatting/CI tooling on 2026-11-10; see `docs/spdd/tasks/t01a-formatting-ci.md`. The owner approved T02a after PR #7 merged. See `docs/spdd/tasks/t02a-diagnostic-host.md` for the diagnostic host and verification status. The owner approved the T02b diagnostic probe. The owner approved the simulation-first architecture and T01d on 2026-11-11; see `docs/spdd/tasks/t01d-simulated-source.md`. The owner approved T01e on 2026-11-11 and supplied a private FIT for verification; see `docs/spdd/tasks/t01e-fit-replay.md`. The owner approved T02d on 2026-11-11 after PR #11 merged; see `docs/spdd/tasks/t02d-karoo-adapter-tests.md`. The owner approved T03a on 2026-11-11 after PR #13 merged; see `docs/spdd/tasks/t03a-collector-lifecycle.md`. The owner approved T03b on 2026-11-11 after PR #14 merged; see `docs/spdd/tasks/t03b-bounded-admission.md`. T02c, T03c–e, T03p and later implementation remain unapproved. Discovery, architecture, specifications and project skills remain authorized. Do not generate application code, scaffold Android, or implement a diagnostic app until the owner explicitly approves a named implementation slice. The first milestone ends with reviewed domain model, REASONS Canvas and source-verified SDK capabilities. Device behavior remains conditional until tested.

## Routing

- Product intent: `docs/product/requirements.md`.
- Every substantive change: `docs/spdd/reasons-canvas.md`, relevant task in `docs/spdd/tasks/`, and `docs/spdd/decision-log.md`.
- Karoo integration: `docs/discovery/karoo-sdk-feasibility.md` and `.agents/skills/karoo-extension/SKILL.md`.
- Scientific changes: relevant `docs/algorithms/` document, `docs/discovery/scientific-research.md`, and `.agents/skills/oxywatt-models/SKILL.md`.
- Spec creation, readiness and synchronization: `.agents/skills/oxywatt-spec/SKILL.md`.
- Approved implementation, fixes and refactors: `.agents/skills/oxywatt-developer/SKILL.md` (minimal complete changes and behavior-driven TDD).
- Pull request preparation and publication: `.agents/skills/oxywatt-pr/SKILL.md` (self-review, Conventional Commit subjects and PR titles, repository GitHub identity).

## Language

Use English for all product and diagnostic UI, documentation, code comments, test names and commit/PR text. Conversation with the owner may be in Polish. Keep UI strings in resources; English is the default and only planned UI language unless the owner requests localization.

## Invariants

Collector normalizes Karoo and future FIT inputs to one source-neutral model. The core has no Android, Hammerhead, Room or Compose types. FTP and VO₂max are separate models; Critical Power is not FTP. Never invent SDK APIs, measurement timestamps, historical access or process survival guarantees. Distinguish facts, observations, estimates and manual calibration. Missing data is not zero. Input quality is not calibrated physiological confidence.

Version model, preprocessing, parameters, rider settings and trend configuration. Retain inputs needed for reproducibility. Record gaps and ambiguous recovery honestly. Never merge uncertain sessions automatically. Do not commit private rides, GPS traces, credentials, keystores or telemetry. Read-only research does not authorize publishing or contacting others.

## Workflow

Read the applicable specification, identify requirement/task/test IDs, then work within the approved scope. Propose material behavior or scope changes in the Canvas and decision log before implementation. Routine choices inside approved constraints need no repeated approval. Do not introduce mandatory agents, elaborate governance, or speculative modules.

Update task evidence and documentation with actual results. A source-verified capability is not hardware-verified. Do not mark an implementation or hardware check complete without evidence. Small corrections need no new full specification.

## Verification

For T01a run `./gradlew :core:check --console=plain` (JDK 25, JVM target/release 17). Also check relative Markdown links, requirement/task/test traceability, YAML skill frontmatter, and `git diff --check`. Validate skills with the installed skill-creator validator when available. For T02a also run the opt-in Android build and lint when the SDK and package access are available. Record device checks only when performed. Run checks appropriate to each approved slice.

## GitHub CLI account

This repository uses its own GitHub CLI configuration: run repository GitHub commands with `GH_CONFIG_DIR=.git/gh-personal` from the repository root (for example, `GH_CONFIG_DIR=.git/gh-personal gh pr create`). Check this account before diagnosing authentication or collaborator failures; the global `gh` account may belong to a different identity. Do not request a new login or extract credentials from Git helpers to work around the wrong account. Keep `.git/gh-personal` private and out of commits.
