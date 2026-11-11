# OxyWatt REASONS Canvas v0.3 — simulation-first proposal

Status: the owner approved the source-neutral architecture, T01a–T01c and T02a/T02b. The simulation-first architecture, T01d and T01e are owner-approved (2026-11-11); other implementation packages remain gated. Updated 2026-11-11. Owner decisions D01–D03 constrain all future slices. This is the maintained engineering prompt; linked documents expand it without creating competing requirements.

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

A planned debug-only diagnostic preview, [T03p](tasks/t03p-diagnostic-preview.md), makes T03a–d visible on Karoo through synthetic simulation/FIT replay before live T03e integration. It projects actual Collector and transactional demo-repository state; received readings and committed event counts remain distinct. The Activity owns no acquisition, demo storage is separate from real history, and simulation support/assets are absent from release. T03p/V23 is owner-requested planning, not authorization of its dependencies or a completed hardware check.

## O — Operations

[Backlog](implementation-plan.md): T01 canonical input/replay fixtures; T02 Karoo timing/lifecycle feasibility; T03 durable ingestion; T04 power curve and minimal projection, completing first vertical slice. Then T05 model experiment protocol, independent T06 FTP/T07 VO₂max selection, T08 history/chart, T09 release validation. T02 is an explicit stop if continuity cannot be supported; failed hypothesis requires architecture review, not hidden scope reduction.

The [simulation-first packages](tasks/simulation-first-development.md) add T01d source/generators, T01e real FIT and T02d SDK integration tests. After separate approval, T03a–T03d may develop against accepted simulation contracts. T03e live wiring and parent live acceptance remain behind passed T02 device acquisition checks. The owner accepted this dependency order and approved T01d only on 2026-11-11.

Read each task before implementing. Implement only explicitly approved tasks. Each task links goal, files, dependencies, tests, risks and completion evidence. Keep SDK findings source-verified versus hardware-tested.

Proposed [agent work packages](tasks/agent-work-packages.md) subdivide the backlog into bounded file ownership and contract-based handoffs. Parallel work is optional and follows accepted interfaces and existing gates; it adds no implementation authorization.

## N — Norms

The project [PR skill](../../.agents/skills/oxywatt-pr/SKILL.md) covers self-review, slice-specific verification, repository GitHub identity and Conventional Commit formatting for both commit subjects and PR titles. Publication follows the owner’s session authorization.

Owner-approved [T01a-format tooling](tasks/t01a-formatting-ci.md): pinned Spotless/ktfmt with Kotlin style formats core Kotlin and Gradle Kotlin DSL. GitHub Actions checks formatting, JVM contracts and the core JAR build on pushes and pull requests, retaining available test reports; local `check` includes formatting. This adds no application behavior.

The [developer skill](../../.agents/skills/oxywatt-developer/SKILL.md) guides approved packages toward minimal complete changes and behavior-driven TDD. Domain tests precede implementation; SDK, build and device claims require their respective checks. This workflow does not authorize code or close feasibility/scientific gates.

Kotlin types express units/nullability, explicit reason codes, immutable result/config snapshots. Core imports neither Android nor Hammerhead/Room/Compose. Flow is orchestration, not a replacement for time semantics. Bounded work/queues using the count, payload, reserved-control capacity and fail-closed overload policy in N01; indexed batches and transactional writes. No UI-owned collector. Numerical tests use independent oracles and irregular-time datasets. All UI text is English and stored in resources; application screens cover empty, collecting, partial, unavailable, stale and storage-error states.

Read [AGENTS](../../AGENTS.md) and applicable project skills. Update Canvas/ADRs when intent changes; update docs to actual implementation after refactoring. Never mark a task done from a successful build alone. Documentation, product/diagnostic UI, code comments, tests and commit/PR text are English. Conversation with the owner may be in Polish. Additional UI localization requires an owner request.

## S — Safeguards

No invented SDK APIs or process guarantees. No absent-input zero fill, hidden HRmax formula, unjustified efficiency constant, fake confidence or inferred lab accuracy. No backend, accounts, runtime telemetry, unnecessary GPS/permissions or committed secrets/private FITs. Preserve data provenance and deletion limitations. Enforce bounded resource use and reveal acquisition loss. Candidate SDK is 1.1.9, latest verified release in this review, not yet device-tested.

## Revision history

- v0.3 ACCEPTED architecture (T01d implementation only): simulation-first source/test boundary, virtual timeline, V18–V22 and selective T03 dependency amendment. T02 hardware and estimator science gates remain open.

- v0.2: address PR review findings with conservative trend eligibility for interrupted fragments, authoritative recalculation generations, and hard ingestion limits. Add V15–V17; all remain planned, not executed.
- v0.1: initial discovery and architecture proposal.

## Approval and unresolved decisions

Architecture approval must identify this version and the allowed task scope. It does not certify continuous operation, scientific accuracy or license choice. See [review packet](review.md) for decisions. Current state: owner approved T01a and the core/adapters architecture on 2026-11-10. T01a is implemented and verified by 10 JVM contract tests after self-review; [evidence](tasks/t01a-canonical-input.md). Canonical input contract v1 is in the domain model. The owner resumed T01b after PR #3 merged. The [synthetic replay evidence](tasks/t01b-synthetic-replay.md) records its completion. T01c verifies the FIT side of canonical replay on a synthetic binary fixture. See [T01c evidence](tasks/t01c-fit-replay.md). Live Karoo mapping and the full V01/V02 scenarios still require later evidence. The owner approved the [T02a diagnostic host](tasks/t02a-diagnostic-host.md) after PR #7 merged. The owner approved the [T02b Karoo probe](tasks/t02b-karoo-probe.md). T01d is owner-approved; T01e is also owner-approved; T02c/T02d and later implementation remain gated.

T01a self-review: Kotlin API restricted to Java 17 in addition to JVM target 17; time helper explicitly makes no continuity claim. See task evidence for the compiler probe and boundary tests.

T01d evidence: the source-neutral port and test-only simulator are implemented; 28 JVM tests pass with formatting/build and production-JAR isolation checks. See [T01d](tasks/t01d-simulated-source.md). This closes source-level V18/V19 and manifest-level V22 only; Collector and hardware gates remain open.

T01d PR #10 review correction: virtual time cannot advance recursively inside a timer callback; this prevents time reversal. Timer scheduling/cancellation remains allowed. All 30 JVM tests pass; see the T01d task review evidence.

T01e implements the pinned developer FIT exporter and test-only simulator bridge, with strict timestamp/integrity checks, explicit timer metadata and private owner-file verification. [Task/evidence](tasks/t01e-fit-replay.md) records V21/V01/V02/V18 scope and the absence of a redistributable real fixture. Public CI uses generated data only.

T01e review correction (V18/V21): exported bytes and checked-out hashed fixtures use LF consistently; replay rejects inconsistent manifest end/count at streaming EOF before completing acquisition. See the T01e task for red/green evidence and private-file verification.

T02d is owner-approved on 2026-11-11 after T01e merged: SDK-fake compatibility, canonical adapter mapping and lifecycle regressions, plus test dependency isolation. See [task](tasks/t02d-karoo-adapter-tests.md). Hardware acceptance remains open.

T02d software evidence: app → core Karoo source mapping and diagnostic migration are verified with SDK 1.1.9/fake 0.1.1 under Robolectric API 32/34. Cancellation/reconnect/teardown and release dependency isolation pass; default JVM CI remains independent of Android. [Evidence and limits](tasks/t02d-karoo-adapter-tests.md). This does not close parent live V11/V13 or authorize T03.

T02d pre-publication review: reject reentrant SDK delivery explicitly; a nested callback cannot overlap accept or prematurely acknowledge cleanup. Regression evidence is recorded in the T02d task.

T03a is owner-approved after PR #13 merged: pure core lifecycle/session assignment, conservative recovery checkpoints and per-channel coverage effects against simulation. [Contract and evidence](tasks/t03a-collector-lifecycle.md). T03b–e/T03p and live device acceptance retain their gates.

T03a local software evidence: 20 new core lifecycle/replay tests pass; the full suite has 54 passes and one explicit private replay skip. Versioned checkpoints retain source/policy/profile provenance; per-channel breaks and interruption/recovery fragments remain separate. [Evidence](tasks/t03a-collector-lifecycle.md). Persistence, admission and live/hardware gates remain open.

T03a review correction: ID allocation/validation precedes lifecycle mutation; known previous/root IDs cannot be reopened and failed allocation preserves receipt/checkpoint and pending recovery effects for retry. See the T03a evidence.

T03b is owner-approved after merged PR #14. [Admission contract](tasks/t03b-bounded-admission.md) defines shared queued/in-flight N01 budgets, bounded out-of-band loss, fail-closed controls and explicit commit acknowledgement. Core-only V17 evidence does not close repository/live gates.
T03b core evidence: twelve admission/lifecycle regression tests pass, including a 100,000-event stalled-write burst and queued/in-flight accounting; full suite has 68 passes and one private-FIT skip. Repository atomicity and live timer/writer integration remain open.

T03c is owner-approved after merged PR #15. [Input repository contract](tasks/t03c-transactional-inputs.md) chooses platform SQLite for atomic input/effect/checkpoint commits, versioned replay and conservative recovery. Room remains optional rather than adding a processor for this boundary. No live acquisition or result storage follows.
T03c software evidence: atomic input/effect/checkpoint commits, exact retries, pre-lifecycle receipt rejection, competing writers, conservative restart and native SQLite-full rollback pass in 32 new API 32/34 executions; all 56 app tests and 68 core tests pass (one private replay skip). [Evidence and limits](tasks/t03c-transactional-inputs.md). Schema v1 starts migration history; hardware/full live V17 and later result/deletion scenarios remain open.

## T03d authorization and contract

2026-11-11: owner approved T03d after PR #16 merged. [T03d](tasks/t03d-analysis-persistence.md) freezes additive v1 migration, idempotent generation requests, newest-only result reads and transactional whole-aggregate membership invalidation before code. Synthetic results only; T03p/T03e and physiology remain excluded.

T03d storage implementation is software-verified: durable idempotent generations, newest-only current results and atomic whole-aggregate membership invalidation use platform SQLite schema v2 with a preserved-data v1 migration. Full app/core/build/lint checks pass; see the T03d evidence. T03p/T03e, scientific calculations and complete T08 deletion/trend projections remain gated.


T03p implementation authorized by the owner on 2026-11-11 after PR #17. Readiness: T03a–d contracts are implemented. Preview uses a dedicated application-scoped worker, separate demo database, bounded latest-value projection, and shared dependency-free simulator fixtures. Portable stream replay preserves exporter validation. No live acquisition, physiology or hardware acceptance is included. R02/R03/R07/R10/R11; V22/V23.


T03p software implementation (2026-11-11, owner-approved after PR #17): isolated debug-derived preview consumes actual T03a/b/c pipeline and schema v2. Shared test fixtures and stream FIT reader remain outside release. Received versus committed values, virtual ages, pending/loss, interruption and storage errors are inspectable. Last committed receipt time is explicitly distinguished from an unavailable durable transaction timestamp. See [T03p evidence](tasks/t03p-diagnostic-preview.md). V22/V23 software checks do not close the on-Karoo screenshot/device claim, T02 or live T03e.


T03p device-verification preparation after PR #18: owner authorized preview-only installation, synthetic scenarios and screenshots. Local full verification passed with cached test results; ADB currently lists no devices. No installation, screenshot or current hardware version evidence exists. V22/V23 device checks and T02/T03e gates remain open; see [readiness and limitations](tasks/t03p-diagnostic-preview.md).


T03p device follow-up: identified Karoo preview installation and synthetic demo/FIT screenshots now exist. Android offset-time parsing was fixed after a real-device red test; device instrumentation verifies FIT commits and distinct Activity recreation/reopen. Force-stop recovery conservatively preserves the committed prefix without automatic continuation. [Evidence and limitations](../evidence/t03p-karoo-2026-11-11/README.md); V22/V23 hardware scope is partial, T02/T03e remain open.


T02b HR-only follow-up: owner supplied a paired HR sensor. The existing older installed probe reported real HR presence in fresh short baseline and post-screen-sleep-command captures; power/cadence were unavailable. This does not verify current T02d code, numerical HR accuracy, SQLite live persistence or T02c/T03e. [Evidence](tasks/t02b-karoo-probe.md).


T02b real-HR follow-up now includes owner-confirmed transmitter off/on: unavailable/searching and subsequent present were observed without agent-triggered probe restart or pairing changes. No new callbacks occurred in a dedicated 30-second off window. This is limited presence/reconnection evidence from the older installed probe; current adapter, exact latency, native recording and sustained T02c/T03e acceptance remain open. [Evidence](tasks/t02b-karoo-probe.md).


Owner direction (2026-11-11): postpone live trials and continue useful work with the simulator. Proposed [T04a](tasks/t04a-power-curve-engine.md) isolates a pure, bounded observed power-duration engine over explicitly confirmed intervals; software acceptance uses irregular-time integrals, breaks and deterministic synthetic oracles. T04b owns future canonical-to-interval preprocessing and persistence/readout. No FTP/VO2max method or T03e/live hardware acceptance follows. Named T04a approval remains required before implementation; only its pure numerical dependency gate is proposed to move ahead of T03e.


T04a approved after merged PR #21 on 2026-11-11. Implement one-session confirmed-interval power-duration core with explicit breaks, bounded interval retention and independent JVM oracles; no live adapter/preprocessing, persistence, UI or physiology. [Contract](tasks/t04a-power-curve-engine.md).


T04a software evidence: confirmed-interval power engine and 14 new numerical/acceptance tests pass; full core 82 passes plus one private-FIT skip. Exact-boundary records preserve session/epoch/segment and versioned provenance. Explicit resource status prevents claiming a fully searched curve after saturation. [Evidence and computational limits](tasks/t04a-power-curve-engine.md). No canonical preprocessing, persistence, UI, live or physiology gate closes.

## T04b simulation consumer proposal — 2026-11-11

After merged PR #22, propose [T04b1](tasks/t04b-persisted-curve.md): one terminal synthetic session, committed revision-bounded effect replay → versioned hold/expiry intervals → T04a → typed curve terminal in existing T03d authority → durable preview readout. R02/R03/R04/R07/R10/R11/R12; software B1–B7 map to existing V scenarios. Entities retain session/epoch/segment and partial provenance; no uncertain fragments merge. Operations use the preview worker after terminal commit, two streaming passes, 256-record pages and a conservative pre-call computation budget. Norms preserve timer-before-input ordering, missing versus zero, codec/hash versions and newest-generation/no-fallback semantics. Safeguards keep synthetic databases/release isolation, discard uncommitted tails and report incomplete scans. This proposes the software dependency amendment before T03e; implementation is NOT approved. T02c/T03e and parent T04 live acceptance remain deferred/open; no bicycle or Karoo is needed for software acceptance. No physiology or product history is added.

T04b1 implemented after owner approval following merged PR #23: [terminal synthetic persisted curve](tasks/t04b-persisted-curve.md). Committed revision-bounded effects are converted with preview-silence-v1 hold/expiry to T04a; one typed T03d generation atomically stores all durations and partial/resource provenance. Existing preview reads saved results after reopen; synthetic curves cannot enter aggregate membership. Software evidence: core 90 passes/one private-FIT skip, debug 92 passes, preview 103 passes; builds/lint/release isolation pass. Dense scans have an explicit conservative pre-call work cap. No live T03e, device action, physiological model or private FIT read; T02c and parent T04 live acceptance remain open.


## T04b2 proposal — 2026-11-11

T04b2 proposes bounded range-mean integration and charged work for complete long variable synthetic scans. Preserve exact candidates, numerical tolerance, coverage and existing authoritative storage/readout; B2-01–08 define software acceptance. R02/R03/R04/R07/R10/R11/R12; V06/V09/V13/V18/V19/V22/V23 plus persistence regressions. No implementation approval or hardware acceptance. See [T04b2 specification](tasks/t04b2-long-variable-scan.md).


## Approved T04b2 implementation — 2026-11-11

Owner approved [T04b2](tasks/t04b2-long-variable-scan.md) after specification PR #25 merged. The existing core/power now uses bounded persistent AVL range means and charged work-v2, with exact candidates/coverage/tolerance retained. Canonical hold-v1, the existing logical slot, scalar-v1/curve-v2, atomic newest-generation storage and durable preview readout remain in the same flow. Independent exact oracles and long synthetic speed/reopen checks provide software evidence; see task for final results and resource bounds. This replaces T04b1's conservative quadratic reservation for new calculations, without rewriting old records. No private FIT, live acquisition or physiological model; T02c/T03e and parent T04 live acceptance remain open.

## T05a proposed experiment preparation — 2026-11-11

After merged PR #26, propose [T05a](tasks/t05a-fixture-reproducibility.md), R01–R07/R10–R12 with A01–A07 mapped to existing V scenarios. Entities: extend existing development RunManifest with one descriptor and separately versioned FTP/VO₂max input profiles; reference existing FIT manifest and canonical snapshots. Approach/structure: reuse simulator, hashes, Collector and authoritative SQLite generations, with no framework, second storage or estimator. Operations/norms: pin recipes, rights, units/time domains and policies; separate execution metadata and compare full persisted/reopened replay at 1x/10x/unpaced with fixed fixture identities. Safeguards: explicit unknown/missing/refusal, independent expected tables, synthetic aggregate exclusion and no physiological truth assigned to synthetic/observed curves. T05b retains evaluation/sensitivity/go/no-go; scientific references and later live acceptance remain separate open gates. T05a implementation is pending named owner approval; publication of this specification is authorized.


## T05a implementation — 2026-11-11

T05a implementation approved after merged PR #27. Extend development RunManifest only with an optional versioned descriptor digest (legacy preimage preserved); explicit synthetic event tables, rights declarations and two independent input-profile schemas live in existing test fixtures. Existing virtual source/Collector/admission/SQLite/curve/readout checks provide reproducibility evidence; no production estimator/storage changes. See [T05a](tasks/t05a-fixture-reproducibility.md) and [software evidence](../evidence/t05a-software-2026-11-11/README.md). Scientific and live gates remain separate.


## T05b protocol proposal — 2026-11-11

[Canonical T05b specification](tasks/t05b-evaluation-protocol.md). T05b defines separate FTP/VO₂max questions, independent reference qualification, participant-level splits, locked reporting and method-dependent sensitivity. Reuse T05a descriptors/profiles and existing authoritative hashes/storage. Protocol readiness, experiment admission, method selection and product admission are independent decisions. O1–O5 block real experiments; no method, accuracy threshold or sample size is selected. B01–B08 await named execution approval. Software, scientific and deferred live gates remain separate.


## T05b executed protocol exercises — 2026-11-11

Owner approved T05b after merged PR #29. [B01–B08 report](../evidence/t05b-protocol-review/README.md) completes the bounded documentation exercise: separate FTP/VO₂max estimands, ten invented qualification cases, participant split/freeze rehearsal, reconciled coverage/refusal/error arithmetic, conditional sensitivity and explicit O1–O5 decisions. Self-reviewed evidence is ready for owner review; it is not a physiological study. Both experiment admissions remain NO-GO; no lawful matched dataset, justified scientific thresholds/sample plan or method selected. T06/T07 retain independent research/selection and later implementation; no automatic authorization follows. T05 protocol/reproducibility deliverables are now recorded, while parent scientific acceptance and deferred T02c/T03e/parent T04 live gates remain open.


CI tooling repair (2026-11-11): [T01a-format F03/F04](tasks/t01a-formatting-ci.md) explicitly configures Maven Central plus Plugin Portal for plugin dependency resolution after failed main run 43084264896. This preserves pinned tooling and existing JVM acceptance scope.


## T06a scope proposal — 2026-11-11

[Named T06a scope](tasks/t06a-ftp-method-audit.md) defines a bounded primary-source FTP audit after merged T05b PR #30. F01–F08 map R05/R07 (plus input/privacy R02/R10) to V07/V09 and conditional V02/V12/V18/V19: search log, equation/protocol/population/reference audit, current-input compatibility, conditional sensitivity, rights/data availability and a reviewable recommendation. Named audit execution approval is pending; this specification chooses no method. O1–O5, experiment admission, separate T06b implementation and later live gates remain explicit. T07a stays independent. No application code, private FIT, dataset download, device action or new framework.


## T06a executed audit — 2026-11-11

Owner approved T06a after specification PR #32 merged. [F01–F08 report](../evidence/t06a-ftp-method-audit/README.md) records the bounded primary-source audit, source-access/correction limits, input compatibility, conditional sensitivity and FTP O1–O5 decisions. Recommendation is to retain model_not_approved for automatic FTP from ordinary rides; no audited candidate qualifies on present evidence. An optional controlled-protocol investigation needs a separate scope decision and cannot silently replace ordinary-ride intent. No method, dataset, estimator or experiment is admitted; T06b and independent T07a remain separate. Source numerical results are centralized in the report, not new product accuracy thresholds. R05/physiological V07 and later live acceptance remain open.


## T07a executed independent audit — 2026-11-11

Owner authorized T07a following T06a; PR #33 merged during work and local main/branch updated. [G01–G08 report](../evidence/t07a-vo2max-method-audit/README.md) centralizes primary equations, protocol/population/reference and rights/access findings, input compatibility and conditional sensitivity. Recommendation: retain model_not_approved for ordinary-ride VO₂max on present evidence. VO₂max O1–O5 remain unresolved/blocked; no method, experiment, dataset or T07b implementation admitted. Any optional controlled-protocol direction requires a separate scope decision. FTP remains independent; R06/physiological V08 and deferred T02c/T03e/parent T04 live acceptance remain open. No application code, private FIT, dataset download, device action or framework.

## Owner direction: prioritize within-person change — 2026-11-11

Owner prioritizes useful trends over precise absolute laboratory agreement for R05/R06/R08. [Requirements](../product/requirements.md) record this intended use; [validation strategy](../algorithms/validation-strategy.md) separates change tracking from absolute agreement. T05b O2 is narrowed, with numeric criteria still open. Next specification may define an explicitly labeled exploratory fitness trend; no method/estimator is approved. Repeated physiological references remain necessary for physiological change claims; smoothing and cross-sectional agreement alone do not establish longitudinal validity.


## T08a1 — proposed comparable power–HR experiment

Owner authorized specification/publication only. [Canonical scope and H01–H07](tasks/t08a1-power-hr-trend.md) subdivides T08a into a smallest complete synthetic comparative experiment: paired covered segments → two descriptive candidate session W observations → frozen baseline/change report → actual persisted replay/reopen/authority evidence. No production estimator, chart, schema, manifest or framework. T07/T07b physiological gates remain separate; R05/R06 are not completed.

REASONS amendment: R02/R03/R07/R08/R10/R11/R12 apply; entities are retained observations/configuration and existing analysis slots, with transient report contributors. Approach compares A fixed-band power and B local interpolation without choosing a production method. Structure stays in existing test fixtures/tests and scalar-v1 storage. Operations use synthetic-only isolated replay, no live wiring. Norms require independent oracles and explicit recipe parameters. Safeguards prohibit physiological relabeling, uncertain context comparison, newest-generation fallback and synthetic real-history membership.

The earlier trend-v0 42-day horizon, 14-day half-life/staleness and three-session minimum remain an unaccepted historical proposal, **not defaults for T08a1**. This experiment uses raw session observations and a recipe-named fixed baseline; smoothing/product thresholds stay open. Software Done is H01–H07 evidence, scientific Done requires repeated independent references, and live Done still requires T02c/T03e/parent T04.
