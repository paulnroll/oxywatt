# OxyWatt REASONS Canvas v0.3 — simulation-first proposal

Status: the owner approved the source-neutral architecture, T01a–T01c and T02a/T02b. The simulation-first architecture and T01d are owner-approved (2026-11-11); other implementation packages remain gated. Updated 2026-11-11. Owner decisions D01–D03 constrain all future slices. This is the maintained engineering prompt; linked documents expand it without creating competing requirements.

Method: [Structured-Prompt-Driven Development, Thoughtworks authors on martinfowler.com](https://martinfowler.com/articles/structured-prompt-driven/). We adopt its REASONS structure and synchronization; specific engineering choices below belong to OxyWatt. [Open Mercato spec workflow](https://github.com/open-mercato/skills/blob/main/skills/om-spec-writing/SKILL.md) informs skeleton/review/phasing, not Android implementation rules.

## R — Requirements

Build offline cycling analytics for Karoo 3 from normal rides with real power/HR. R01–R12 and N01–N03 in [requirements](../product/requirements.md) are the acceptance baseline. Distinguish collected facts, observed maxima, estimates, user settings and trends. The owner wants a progressively aggregated value and chart. First milestone is documentation only; first implementation slice, once separately approved, is Karoo → Collector → Persistence → Power Curve.

Done means linked acceptance evidence and honest limitations, not a displayed number. R05/R06 remain scientifically gated. T01a delivers the canonical JVM contracts and T01b adds test fixtures for replay. Further packages require named approval.

Simulation-first architecture is accepted under [ADR 0004](../architecture/adr/0004-simulation-first-testing.md), with layer acceptance V18–V22 in the validation strategy. Synthetic fixtures provide numerical/software evidence. Scientific evaluation of FTP and VO₂max remains T05–T07.

## E — Entities

Use the [domain model](../architecture/domain-model.md): local RecordingSession and contiguous segments; SourceObservation and normalized RideSample; versioned RiderProfileSnapshot; observed PowerCurve; independent FTP/VO₂max results within an immutable PerformanceEstimate envelope; manual calibration and derived TrendSnapshot. Session UUID is local, not native ride identity. No framework objects in core, no per-sample aggregate.

RideDataSource is a small technical port producing SourceObservation values from existing canonical stamp/source/payload types. The Collector assigns session identity and builds InputEnvelope. The harness supplies acquisition epoch/clock context. Source adapters own transport and mapping. Test scenarios and run manifests stay in development support.

## A — Approach

Accepted simulation amendment uses KarooRideDataSource and SimulatedRideDataSource immediately before the existing envelope boundary. Source termination is acknowledged after callback quiescence and follows the shared interruption policy. FIT decoding feeds the simulator. Virtual receipt/activity time advances independently of playback speed, including timeout/flush scheduling. Simulated origin, hashes, seed and versions remain explicit and isolated from real history. SDK mapping tests use the real adapter with a fake host. External Karoo replay at accelerated speed tests wiring/load because live receipt time is compressed.

Adapters emit the same source-neutral observations to an independent Collector. Preserve timing origin and per-channel freshness. Serialize ingestion, batch persistence and use monotonic time within an epoch. Gaps/pause/restart break contiguous analytics. Use deterministic time integration for power curves; independently research physiological models. No CP-to-FTP substitution or VO₂max equation approval implied.

Compute physiology after sufficient persisted evidence and locally observed end, independently of visible fields. Persist every run with input/profile/configuration hashes and versions. Recalculation creates a new run. For each `(session, metric, cohort)` result slot, allocate a persisted increasing generation before execution and point the slot at the newest requested generation. Allocation, pointer advancement and invalidation of current aggregate membership share a transaction. Only that generation may contribute to current values/trends after committing a terminal available or unavailable result. Unavailable supersedes a formerly available value; pending/failed newest runs temporarily contribute nothing and expose their status, with no fallback to older accepted values. Late completion of an older generation cannot move the pointer backwards. Historical runs remain inspectable. Commit terminal results and affected aggregate invalidations transactionally. Native ride saved/discarded cannot currently be established; label local recording accordingly.

Proposed trend algorithm `trend-v0` for review: for each metric and compatible model/preprocessing/calibration/environment cohort, take at most one current accepted estimate per trend-eligible session over the preceding 42 days. A session is trend-eligible only when its recording start and local end were observed and it has no interrupted or ambiguous identity/recovery history. Exclude interrupted sessions, ambiguous recovery fragments, sessions first seen already recording, and every fragment linked to that uncertainty; show them separately as partial observations. Local sample gaps still follow model-specific eligibility. No automatic merging or grouping of uncertain fragments is permitted. A fresh session becomes independent only after an observed Idle → Recording boundary, not from a new UUID or process epoch. Require ≥3 such distinct sessions before showing an aggregate; individual points remain visible earlier. Use recency-weighted mean `T = sum(w_i*x_i)/sum(w_i)`, `w_i = 2^(-ageDays_i/14)`, ages relative to explicit persisted as-of date. These 42/14/3 settings are provisional UX parameters, not physiological constants. All included values have passed that model's eligibility gate; do not fabricate numeric confidence weights. Missing/rejected rides do not become zero. Flag stale when newest contributor is older than 14 days. Do not extrapolate improvement/decline from silence; preserve raw points and contradictory results. Persist contributor IDs and config. Changing model or calibration starts a separate cohort/recalculation. A quality score is not a probability or error bound.

## S — Structure

Initially two modules: pure Kotlin/JVM `:core`, Android `:app`. Core contains collection, power analytics, separate estimation packages and history policies. App adapts Karoo, local Room storage (subject to OS compatibility), orchestration, Compose screens and optional numeric ride field. FIT replay helper for development precedes user-facing import. Constructor injection and direct calls; no generic plugin/event infrastructure. See [module structure](../architecture/module-structure.md).

## O — Operations

[Backlog](implementation-plan.md): T01 canonical input/replay fixtures; T02 Karoo timing/lifecycle feasibility; T03 durable ingestion; T04 power curve and minimal projection, completing first vertical slice. Then T05 model experiment protocol, independent T06 FTP/T07 VO₂max selection, T08 history/chart, T09 release validation. T02 is an explicit stop if continuity cannot be supported; failed hypothesis requires architecture review, not hidden scope reduction.

The [simulation-first packages](tasks/simulation-first-development.md) add T01d source/generators, T01e real FIT and T02d SDK integration tests. After separate approval, T03a–T03d may develop against accepted simulation contracts. T03e live wiring and parent live acceptance remain behind passed T02 device acquisition checks. The owner accepted this dependency order and approved T01d only on 2026-11-11.

Read each task before implementing. Implement only explicitly approved tasks. Each task links goal, files, dependencies, tests, risks and completion evidence. Keep SDK findings source-verified versus hardware-tested.

Proposed [agent work packages](tasks/agent-work-packages.md) subdivide the backlog into bounded file ownership and contract-based handoffs. Parallel work is optional and follows accepted interfaces and existing gates; it adds no implementation authorization.

## N — Norms

The project [PR skill](../../.agents/skills/oxywatt-pr/SKILL.md) covers self-review, slice-specific verification, repository GitHub identity and Conventional Commit formatting for both commit subjects and PR titles. Publication follows the owner’s session authorization.

Owner-approved [T01a-format tooling](tasks/t01a-formatting-ci.md): pinned Spotless/ktfmt with Kotlin style formats core Kotlin and Gradle Kotlin DSL. GitHub Actions checks formatting, JVM contracts and the core JAR build on pushes and pull requests, retaining available test reports; local `check` includes formatting. This adds no application behavior.

The [developer skill](../../.agents/skills/oxywatt-developer/SKILL.md) guides approved packages toward minimal complete changes and behavior-driven TDD. Domain tests precede implementation; SDK, build and device claims require their respective checks. This workflow does not authorize code or close feasibility/scientific gates.

Kotlin types express units/nullability, explicit reason codes, immutable result/config snapshots. Core imports neither Android nor Hammerhead/Room/Compose. Flow is orchestration, not a replacement for time semantics. Bounded work/queues using the count, payload, reserved-control capacity and fail-closed overload policy in N01; indexed batches and transactional writes. No UI-owned collector. Numerical tests use independent oracles and irregular-time datasets. UI text is translatable; application screens cover empty, collecting, partial, unavailable, stale and storage-error states.

Read [AGENTS](../../AGENTS.md) and applicable project skills. Update Canvas/ADRs when intent changes; update docs to actual implementation after refactoring. Never mark a task done from a successful build alone. Docs are English for open-source collaboration; conversation may remain Polish.

## S — Safeguards

No invented SDK APIs or process guarantees. No absent-input zero fill, hidden HRmax formula, unjustified efficiency constant, fake confidence or inferred lab accuracy. No backend, accounts, runtime telemetry, unnecessary GPS/permissions or committed secrets/private FITs. Preserve data provenance and deletion limitations. Enforce bounded resource use and reveal acquisition loss. Candidate SDK is 1.1.9, latest verified release in this review, not yet device-tested.

## Revision history

- v0.3 ACCEPTED architecture (T01d implementation only): simulation-first source/test boundary, virtual timeline, V18–V22 and selective T03 dependency amendment. T02 hardware and estimator science gates remain open.

- v0.2: address PR review findings with conservative trend eligibility for interrupted fragments, authoritative recalculation generations, and hard ingestion limits. Add V15–V17; all remain planned, not executed.
- v0.1: initial discovery and architecture proposal.

## Approval and unresolved decisions

Architecture approval must identify this version and the allowed task scope. It does not certify continuous operation, scientific accuracy or license choice. See [review packet](review.md) for decisions. Current state: owner approved T01a and the core/adapters architecture on 2026-11-10. T01a is implemented and verified by 10 JVM contract tests after self-review; [evidence](tasks/t01a-canonical-input.md). Canonical input contract v1 is in the domain model. The owner resumed T01b after PR #3 merged. The [synthetic replay evidence](tasks/t01b-synthetic-replay.md) records its completion. T01c verifies the FIT side of canonical replay on a synthetic binary fixture. See [T01c evidence](tasks/t01c-fit-replay.md). Live Karoo mapping and the full V01/V02 scenarios still require later evidence. The owner approved the [T02a diagnostic host](tasks/t02a-diagnostic-host.md) after PR #7 merged. The owner approved the [T02b Karoo probe](tasks/t02b-karoo-probe.md). T01d is owner-approved; T01e, T02c/T02d and later implementation remain gated.

T01a self-review: Kotlin API restricted to Java 17 in addition to JVM target 17; time helper explicitly makes no continuity claim. See task evidence for the compiler probe and boundary tests.

T01d evidence: the source-neutral port and test-only simulator are implemented; 28 JVM tests pass with formatting/build and production-JAR isolation checks. See [T01d](tasks/t01d-simulated-source.md). This closes source-level V18/V19 and manifest-level V22 only; Collector and hardware gates remain open.

T01d PR #10 review correction: virtual time cannot advance recursively inside a timer callback; this prevents time reversal. Timer scheduling/cancellation remains allowed. All 30 JVM tests pass; see the T01d task review evidence.
