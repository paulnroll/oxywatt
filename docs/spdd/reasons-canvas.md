# OxyWatt REASONS Canvas v0.1

Status: DRAFT — NOT APPROVED FOR APPLICATION IMPLEMENTATION. Updated 2026-11-10. Owner decisions D01–D03 constrain all future slices. This is the maintained engineering prompt; linked documents expand it without creating competing requirements.

Method: [Structured-Prompt-Driven Development, Thoughtworks authors on martinfowler.com](https://martinfowler.com/articles/structured-prompt-driven/). We adopt its REASONS structure and synchronization; specific engineering choices below belong to OxyWatt. [Open Mercato spec workflow](https://github.com/open-mercato/skills/blob/main/skills/om-spec-writing/SKILL.md) informs skeleton/review/phasing, not Android implementation rules.

## R — Requirements

Build offline cycling analytics for Karoo 3 from normal rides with real power/HR. R01–R12 and N01–N03 in [requirements](../product/requirements.md) are the acceptance baseline. Distinguish collected facts, observed maxima, estimates, user settings and trends. The owner wants a progressively aggregated value and chart. First milestone is documentation only; first implementation slice, once separately approved, is Karoo → Collector → Persistence → Power Curve.

Done means linked acceptance evidence and honest limitations, not a displayed number. R05/R06 remain scientifically gated. No application scaffold, diagnostic APK or production code is authorized by this draft.

## E — Entities

Use the [domain model](../architecture/domain-model.md): local RecordingSession and contiguous segments; SourceObservation and normalized RideSample; versioned RiderProfileSnapshot; observed PowerCurve; independent FTP/VO₂max results within an immutable PerformanceEstimate envelope; manual calibration and derived TrendSnapshot. Session UUID is local, not native ride identity. No framework objects in core, no per-sample aggregate.

## A — Approach

Adapters emit the same source-neutral observations to an independent Collector. Preserve timing origin and per-channel freshness. Serialize ingestion, batch persistence and use monotonic time within an epoch. Gaps/pause/restart break contiguous analytics. Use deterministic time integration for power curves; independently research physiological models. No CP-to-FTP substitution or VO₂max equation approval implied.

Compute physiology after sufficient persisted evidence and locally observed end, independently of visible fields. Persist every run with input/profile/configuration hashes and versions. Recalculation creates a new run. Native ride saved/discarded cannot currently be established; label local recording accordingly.

Proposed trend algorithm `trend-v0` for review: for each metric and compatible model/preprocessing/calibration/environment cohort, take at most one accepted estimate per session over the preceding 42 days. Require ≥3 distinct sessions before showing an aggregate; individual points remain visible earlier. Use recency-weighted mean `T = sum(w_i*x_i)/sum(w_i)`, `w_i = 2^(-ageDays_i/14)`, ages relative to explicit persisted as-of date. These 42/14/3 settings are provisional UX parameters, not physiological constants. All included values have passed that model's eligibility gate; do not fabricate numeric confidence weights. Missing/rejected rides do not become zero. Flag stale when newest contributor is older than 14 days. Do not extrapolate improvement/decline from silence; preserve raw points and contradictory results. Persist contributor IDs and config. Changing model or calibration starts a separate cohort/recalculation. A quality score is not a probability or error bound.

## S — Structure

Initially two modules: pure Kotlin/JVM `:core`, Android `:app`. Core contains collection, power analytics, separate estimation packages and history policies. App adapts Karoo, local Room storage (subject to OS compatibility), orchestration, Compose screens and optional numeric ride field. FIT replay helper for development precedes user-facing import. Constructor injection and direct calls; no generic plugin/event infrastructure. See [module structure](../architecture/module-structure.md).

## O — Operations

[Backlog](implementation-plan.md): T01 canonical input/replay fixtures; T02 Karoo timing/lifecycle feasibility; T03 durable ingestion; T04 power curve and minimal projection, completing first vertical slice. Then T05 model experiment protocol, independent T06 FTP/T07 VO₂max selection, T08 history/chart, T09 release validation. T02 is an explicit stop if continuity cannot be supported; failed hypothesis requires architecture review, not hidden scope reduction.

Read each task before implementing. Implement only explicitly approved tasks. Each task links goal, files, dependencies, tests, risks and completion evidence. Keep SDK findings source-verified versus hardware-tested.

## N — Norms

Kotlin types express units/nullability, explicit reason codes, immutable result/config snapshots. Core imports neither Android nor Hammerhead/Room/Compose. Flow is orchestration, not a replacement for time semantics. Bounded work/queues, indexed batches and transactional writes. No UI-owned collector. Numerical tests use independent oracles and irregular-time datasets. UI text is translatable; application screens cover empty, collecting, partial, unavailable, stale and storage-error states.

Read [AGENTS](../../AGENTS.md) and applicable project skills. Update Canvas/ADRs when intent changes; update docs to actual implementation after refactoring. Never mark a task done from a successful build alone. Docs are English for open-source collaboration; conversation may remain Polish.

## S — Safeguards

No invented SDK APIs or process guarantees. No absent-input zero fill, hidden HRmax formula, unjustified efficiency constant, fake confidence or inferred lab accuracy. No backend, accounts, runtime telemetry, unnecessary GPS/permissions or committed secrets/private FITs. Preserve data provenance and deletion limitations. Enforce bounded resource use and reveal acquisition loss. Candidate SDK is 1.1.9, latest verified release in this review, not yet device-tested.

## Approval and unresolved decisions

Architecture approval must identify this version and the allowed task scope. It does not certify continuous operation, scientific accuracy or license choice. See [review packet](review.md) for decisions. Current state: awaiting owner review. No approved implementation tasks.
