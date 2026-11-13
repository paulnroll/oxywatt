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


## T08a1 execution authorization — 2026-11-11

Owner explicitly approved execution after specification PR #35 merged. Scope is the [T08a1 H01–H07](tasks/t08a1-power-hr-trend.md) synthetic-only software experiment, independent oracles, persisted replay/reopen and development report. No production method/estimator, product chart, participant experiment or live gate is authorized. Local main advanced to c2f5a81 with a clean working tree.


## T08a1 executed software experiment — 2026-11-11

Owner approved [T08a1](tasks/t08a1-power-hr-trend.md) after PR #35 merged. [H01–H07 evidence](../evidence/t08a1-power-hr-trend/README.md) records the synthetic A/B experiment through actual committed replay, scalar-v1/reopen, newest authority, fixed-baseline/raw reports and fault/cohort/resource checks. Ten new core tests and ten API 32/34 preview executions pass; full core 117 passes/one private-FIT skip, debug 92/preview 116, build/lint/release isolation and final focused checks pass. No production method/estimator, chart, schema, store or manifest was added.

Both candidates reproduce injected observational changes but show confounder-driven false directions; neither is selected for physiological interpretation. Software H01–H07 does not close O1–O5, R05/R06/physiological V07/V08, T07/T07b, T08b/c or deferred T02c/T03e/parent T04 live gates. No participant data or hardware evidence. Smoothing and legacy trend-v0 numeric defaults remain unapproved for this route.


## T08a2 qualification and pilot-readiness proposal — 2026-11-11

[T08a2](tasks/t08a2-comparable-observations.md): Requirements/Entities: measured power–HR observation, visit, joint support, time-in-ride and declared context remain separate from physiological state. Approach/Structure: native A/B coverage plus locked common-support comparison, existing descriptors/profiles only. Operations/Norms/Safeguards: retain all attempted visits and unknowns, newest-requested/no-fallback, versioned provenance and synthetic exclusion; scalar-v1 alone does not enforce origin.

Specification/publication only is authorized; named execution awaits owner approval. N, windows, HR/drift thresholds, meaningful change, baseline horizon and smoothing remain open with resolution paths. No lawful dataset admitted; real-data/scientific pilot is NO-GO. T05b O1–O5, physiological R05/R06, T02c/T03e/parent T04 live gates and T08b/c remain open. No bike, Karoo or private FIT required.


## T08a2 executed qualification review — 2026-11-11

[Task/evidence](tasks/t08a2-comparable-observations.md): Owner-approved I01–I07 documentation execution delivers the qualification and frozen-plan dossier. Common input pool versus actual A/B exposure and visit-level coverage are explicit; measured observations, declarations and physiological claims remain separate. Existing source-neutral contracts, provenance, newest/no-fallback and development-only synthetic isolation remain unchanged.

Real-data/scientific admission remains blocked; N, qualification thresholds, meaningful change, baseline horizon and smoothing remain unresolved without defaults. T05b O1–O5, T02c/T03e/parent T04 and T08b/c remain open. No new framework, storage, manifests or UI.


## Public-data discovery follow-up — 2026-11-11

[Search findings](../evidence/t08a2-comparable-observations/README.md): Public-data discovery now identifies candidate acquisition routes; no data are secured/admitted. Age/sex/mass do not enter current A/B raw-W algebra; lawful descriptors, dated mass/context and unknown states should remain explicit without adding schema or physiological corrections.


## Authorized Salford structural review — continuation 2026-11-12

[Audit evidence](../evidence/t08a2-comparable-observations/README.md): Actual public-source acquisition now supplies repeated real sequences for limited structural research. It exposes missing sample timestamps, mixed inferred intervals and source alignment/context uncertainty; do not fabricate canonical lifecycle or equate row order with verified joint time coverage. Numeric observation availability remains separate from comparable observation and physiological claims.


## Salford source-index timing review — 2026-11-12

[T08a2 inspection](../evidence/t08a2-comparable-observations/README.md#source-specific-timealignment-inspection--2026-11-12): duration formulas encode count × interval, so duration/count is not an independent clock check. The 718 column candidates divide into 711 matching 5 s and 7 matching 1 s grid hypotheses, without verified channel synchronization or original lifecycle. Column extents cannot become canonical observed-start/end or continuity. All remain outside comparative-pilot admission.

Smallest next scope: report-only source-index availability under that explicit assumption, within T08a2 discovery, with no importer/replay/A/B or numeric qualification defaults. Preserve provenance/versions, newest-requested/no-fallback and synthetic exclusion. No new implementation/package approval, production method or physiological claim; T02c/T03e/parent T04 and T08b/c remain open.


## Salford source-index availability executed — 2026-11-12

[Report](../evidence/t08a2-comparable-observations/README.md#executed-source-index-availability-report--2026-11-12): owner approved report-only T08a2 continuation. The unchanged 718 columns contain 1,256,470 numeric paired rows, including 15,898 HR-zero rows. Positive-HR row availability is not synchronized/qualified time or comparable visits; distributions mix unequal column lengths and contexts. No A/B, physiological inference or new code/contracts. Source timing/artifact semantics remain the next evidence dependency; T02c/T03e/parent T04 and T08b/c remain open.


## Salford source recording/export review — 2026-11-12

[T08a2 source matrix](../evidence/t08a2-comparable-observations/README.md#executed-source-recordingexport-semantics-review--2026-11-12) finds independent nominal 5 s study evidence and manufacturer distinctions between storage averages, display smoothing, stop settings and export layouts. These generic capabilities do not establish the device/settings or original time/row linkage of the deposited XLS. Stop archive-only comparable-pilot qualification at this boundary; retain index-time limits and all quality/exclusion flags. Source-linked timing/settings/transformation evidence is the missing dependency; no further participant acquisition/contact or A/B authorized. Contracts, newest-requested/no-fallback and synthetic exclusion unchanged; all deferred live/scientific/product gates remain open.


## T08a3 conditional Salford experiment proposal — 2026-11-12

[Named package](tasks/t08a3-salford-index-ab.md): owner requested a concrete specification following T08a2's archive-only evidence boundary. Proposed next authorization is support discovery on exact per-column HR distributions, separately by person/grid, yielding a concrete numerical-plan freeze proposal without A/B values. Numerical A/B remains gated on accepted evidence-backed configuration and explicit execution approval. Scope deliberately studies reconstructed-index observations under declared assumptions; it does not relax canonical lifecycle/context eligibility or admit T08a2's comparative pilot. No app/import/replay, framework, storage, UI or numeric defaults. J01–J08 are unexecuted; T06/T07/O1–O5, T02c/T03e/parent T04 and T08b/c remain open. Newest-requested/no-fallback, provenance and synthetic exclusion retained at the report boundary.


## T08a3 support evidence and numerical refusal — 2026-11-12

[Executed support discovery](../evidence/t08a2-comparable-observations/README.md#t08a3-executed-support-discovery--2026-11-12), owner-approved J01–J03/J08: fixed primary 197 columns all bracket 107–118 bpm but share no exact HR level across every column. Support-geometry candidate 115 bpm / 112–118 bpm contains 2–729 rows per column; geometry does not justify stability/windows or matched exposure. J04 remains NO-GO/not frozen and numerical execution unapproved/unperformed. Further progress requires an explicit scope choice (simpler source-row description versus independently timed/context-qualified observations), not more synthetic evidence or automatic A/B. Provenance/no-fallback/origin, T02c/T03e/parent T04, T08b/c and physiological gates unchanged.


## T08a4 proposed exact-HR row description — 2026-11-12

**Historical proposal snapshot; named approval and execution are recorded below.**

[T08a4](tasks/t08a4-salford-exact-hr-rows.md) is a separate proposed T08a subdivision after completed support discovery. Recommend a finite stored-row estimand: equal-index, equal-row-weight mean power at every exact positive HR, separately for all 197 fixed-primary columns, and all same-level pair differences. No anchor/band/fit, segment timing, universal scalar or trend; raw artifacts and unmatched context remain explicit. The conventional weighting and unverified pairing need owner acceptance. Planning only authorized; K01–K06 pending, no calculation. T08a1 unchanged; T08a3 J04–J07, physiological/live/product gates remain open.


T08a4 execution authorization (2026-11-12): owner explicitly accepted the complete `salford-exact-hr-rows-v1` proposal and approved its local numerical execution with K01–K06 checks. Development assumptions remain assumptions; no original A/B, application code, physiological admission or Git publication authorized.


## T08a4 executed stored-row description — 2026-11-12

Owner explicitly approved the complete T08a4 configuration and numerical execution. [Aggregate results and verification](../evidence/t08a2-comparable-observations/README.md#t08a4-executed-exact-hr-stored-row-description--2026-11-12): fixed-primary 197 columns, all 201 exact HR levels, 15,189 raw means (2,271 one-row) and 1,074,462 same-level differences (187,894 with a one-row side); every unavailable cell retained. Equal-index pairing remains assumed, weights are equal stored rows and artifacts remain uncleaned. No anchor, A/B, qualified segment, cross-HR scalar, trend or physiology. Independent source/rational checks and full final rerun provide report-only evidence; exact data/scripts/results retained outside Git. T08a3 J04–J07 and all live/scientific/product gates unchanged. No application code authorized. Subsequent report-publication authorization is recorded in the T08a4 task.


## T08a5 approved 115 bpm stored-row descriptor — 2026-11-12

**Authorization/freeze snapshot; completed execution evidence follows below.**

Owner approved the described exact-at-115 / nearest-bracket linear interpolation variant, now reserved as [T08a5](tasks/t08a5-salford-anchor115.md) after checking the unused ID. Freeze the full rules and contributor endpoints before numerical execution. One output per fixed-primary column; direct/interpolated methods, counts, ranges and zero/quality flags stay explicit. Pairing and local linearity remain development assumptions, no A/B/trend/physiology or app implementation. L01–L06 define source/report arithmetic, complete denominators, independent checks, rerun/current-input/no-fallback/privacy evidence; pending execution. No new acquisition/contact or publication authorized; T08a3 and all live/scientific/product gates unchanged.


## T08a5 executed 115 bpm descriptor — 2026-11-12

[Executed results and verification](../evidence/t08a2-comparable-observations/README.md#t08a5-executed-115-bpm-stored-row-descriptor--2026-11-12): all 197 fixed-primary outputs available, 188 exact and nine interpolated, 206 HR nodes and 3,226 actual source-row contributors. Seventeen columns have a one-row used point; 132 include Power-zero in a used point, all retained. Complete independent source/rational checks, full byte-identical rerun and current/no-fallback/interruption checks PASS. Exact versus interpolated evidence remains explicit; local linearity, index pairing, artifact and context limits unchanged. L01–L06 report/source evidence only, no original A/B, chronological/physiological trend, app change or Git publication. Detailed scalars/contributors/scripts remain outside Git; all live/scientific/product gates stay open.


## T08a6 approved single-row influence diagnostic — 2026-11-12

**Authorization/freeze snapshot; executed evidence follows below.**

Owner approved the described [T08a6](tasks/t08a6-salford-row-influence.md) diagnostic before ID reservation. Freeze all used T08a5 rows/nodes/weights/methods, omit each used row once without altering source, record exact W changes or required-node-empty refusal and complete per-column envelopes. M01–M06 require independent recomputation, full unavailable/partial denominators, deterministic rerun/current-input/no-fallback/interruption and privacy checks; execution pending. This is deterministic sensitivity, without physiological uncertainty or arbitrary adequacy thresholds. Recommend ending the present fixed-table numerical sensitivity route here, then deciding a separately scoped timing/context evidence route. No publication or further automatic experiment sequence authorized.


## T08a6 executed single-row influence — 2026-11-12

[Actual results and verification](../evidence/t08a2-comparable-observations/README.md#t08a6-executed-single-row-influence-diagnostic--2026-11-12): all 3,226 deletions enumerated, 3,204 numeric and 22 required-node-empty refusals; all 197 envelopes accounted for (180 complete, one partial, 16 all-unavailable). Exact independent full-recomputation checks, byte-identical rerun and current-input/no-fallback/interruption checks PASS. Deterministic sensitivity is not calibrated uncertainty or physiological repeatability; no adequacy threshold, cleaning or ranking. M01–M06 source/report only. Recommend ending this fixed-table numerical sensitivity route and deciding a separately scoped timing/context evidence route. Detailed W/identities/scripts stay outside Git; earlier local T08a5 work and all scientific/live/product gates preserved. No publication authorized.


## Current diagnostic adapter installed on Karoo — 2026-11-12

[T02b installation/startup evidence](tasks/t02b-karoo-probe.md#current-adapter-diagnostic-update--2026-11-12): rebuilt existing main cf12e69, debug assembly/lint PASS, updated the old diagnostic in place with matching signer, verified installed APK hash/current-adapter marker/build revision and launch plus short host-connected/Idle observation. Version label remains 0.2.0/2; use pinned binary hash/revision. No application changes, numeric sensor validation, ride start or live persistence. R02/R09 setup/partial V02/V11 only; T02c/T03e/parent T04 and scientific gates remain open. No publication authorized by the USB/update request.


## Current-adapter trainer log-tail evidence — 2026-11-12

[T02b retained-tail findings](tasks/t02b-karoo-probe.md#current-adapter-owner-trainer-session-retained-log-tail--2026-11-12): owner reports approximately one hour of riding and native save; retained probe buffer covers only 20 min 50.113 s, with current-APK power/HR/cadence presence, manual pause → IDLE and FIT cancellation. Startup/RECORDING markers are missing, so no full-session duration or continuity claim. Receipt/log timing and throttled status presence remain distinct from sensor timestamps and numeric correctness. R02/R09 partial V02/V11 only. No FIT/native storage access or persistent canonical input; T02c/T03e/parent live T04 and scientific gates remain open. Raw identifiers/times stay in private external evidence, aggregate documentation only.


## Owner's saved trainer FIT: bounded verification — 2026-11-12

[Authorized single-file check](tasks/t02b-karoo-probe.md#owner-authorized-saved-fit-check--2026-11-12) found the newest USB-visible FIT and preserved it privately. CRC/length PASS; file reports 3608 s timer time/3612 s elapsed, with 3608 ordered one-second records, HR in 3608, power in 3602, cadence in 3496. File time/availability are distinct from live receipt/synchronization and numeric accuracy. No GPS/profile/physiology/canonical replay or application-private storage access. Source/report R02/R07/R10 → V02/V09/V12 only; all acquisition/persistence/scientific gates unchanged. Owner need not send another copy for this bounded check; redistribution is not authorized.


## T02c1 standstill diagnostic increment approved — 2026-11-12

Owner asked for progress that does not require another full workout, deferring long-run verification. [T02c1](tasks/t02c1-standstill-diagnostics.md) implements the existing source-backed live screen and bounded private timing/status report, not live Collector/database integration. Numeric readings display only; no new source, schema, physiology or SDK pin. P01–P06 cover missing/zero, report durability/privacy/bounds/errors, UI/source ownership, existing software/isolation checks and optional short device smoke. Parent T02c/T03e/T04/scientific gates remain open; no publication authorized. This is an observability slice, not a relaxation of full acquisition acceptance.


## T02c1 implemented and checked on standstill Karoo — 2026-11-12

[Actual scope and P01–P06 evidence](tasks/t02c1-standstill-diagnostics.md#execution-evidence--2026-11-12): existing source-owned real-value screen, receipt ages/missing reasons, callback counts and bounded private timing/status report implemented. Values display only; report excludes readings/profile/GPS. Zero/missing, reopen, storage/error/limit, queued-plus-in-flight overflow and Activity tests PASS; full debug 106/preview 130 PASS, unchanged core check cached 117 passes/one skip, build/lint and isolation PASS. Installed 0.3.0/code 3 on k24/API 32 and inspected connected/Idle UI plus private report after HOME/reopen. No full workout or native recording, no complete lifecycle/resource/scientific gate. Earlier local T02b/FIT evidence preserved, no publication performed.


T02c1 owner feedback/publication (2026-11-12): owner confirmed the installed live screen displays actual sensor readings and requested a PR plus a new-session prompt. Commit/push/PR cover scoped code/tests and aggregate device/FIT documentation only; private inputs/reports/screenshots/APKs stay outside Git. This feedback does not close full T02c/T03e/T04 or numerical/scientific acceptance; no merge or additional implementation authorized.


## T03e1 diagnostic live persistence proposal — 2026-11-12

**Proposed, not implementation-approved.** [T03e1](tasks/t03e1-live-diagnostic-persistence.md) reserves the checked-unused ID and supplies a concrete next decision. R02/R03/R07/R10/R11/R12: persist real canonical inputs through existing admission/lifecycle/InputRepository, including Idle out-of-session observations, and distinguish received callbacks from acknowledged committed events. Entities/contracts remain source-neutral; DiagnosticExtension owns the sole source, one coordinator owns the existing components, Activity reads bounded state. No second Collector/repository/measurement file.

Proposed limited order exception to ADR 0003/0004: T02c1 + completed software contracts → named T03e1 approval → software fault/integration tests and optional short standstill check **before full T02c**. Full T03e/product live acceptance and parent T04 retain T02c residency/resource gates. This section supersedes the earlier blanket T03e ordering only for this proposed subdivision upon owner approval; it does not declare the exception accepted.

Operations: one short gate covers receipt stamping/admission/timer cuts; one worker owns lifecycle/prepare/commit, one frozen write retries unchanged. New epoch per source run; five-second engineering silence policy, admission-v1 ceilings, rate-limited retry, 32 MiB main SQLite cap with explicit journal-space limitation and private versioned acquisition provenance. Norms/safeguards: timer-before-equal-input, no false saved counts, bounded loss/fail-closed controls, conservative restart, preview isolation, no analysis/fallback/physiology. E01–E09 are proposed software checks; E10 is optional short hardware evidence. No tests executed or new acquisition authorized by this specification.


T03e1 owner authorization (2026-11-12): owner explicitly approved the specified diagnostic-only implementation, policies and ordering exception. Earlier proposal status is historical. Full T02c/T03e/product reliability, live T04 and science remain open; no installation/publication/merge approval inferred.


T03e1 local implementation (2026-11-12, owner-approved): [actual structure and behavioral evidence](tasks/t03e1-live-diagnostic-persistence.md#implemented-structure-and-policy-refinements--2026-11-12) wires the sole real adapter through existing admission/lifecycle/InputRepository. One stamp/admission gate and one worker preserve timer cuts, exact retry, shared N01 bounds and loss-only drain; new epochs/clock origins, private versioned provenance and live-only 32 MiB main SQLite cap are explicit. Readout distinguishes received/current-run inputs from acknowledged commits and shows pending/loss/restart/storage states. Synthetic preview and analysis authority remain isolated. App is 0.4.0/code 4 locally, not installed. See task for final verification results; E10 and full T02c/N02/T03e/live T04/science remain open. No publication/merge.


T03e1 final software verification: debug 138 PASS, preview 162 PASS; core check PASS with unchanged cached 117 passes/one private-FIT skip. Builds/lint and release/preview isolation PASS (4 min 40 s). E01–E09 software evidence is recorded in the task; E10 NOT RUN. Local 0.4.0/code 4 APK is not installed; existing 0.3.0 device provenance is unchanged. Full T02c/N02/T03e/live T04 and science remain open. Changes stay local/uncommitted/unpublished.


T03e1 E10 short hardware check (2026-11-12): owner authorized installation/check after software completion. [Actual evidence](tasks/t03e1-live-diagnostic-persistence.md#e10-authorized-installation-and-short-karoo-check--2026-11-12) confirms matching-signer in-place 0.4.0/code 4 update on k24/API 32, exact installed prepared-APK hash and Connected/Idle zero-power/cadence plus unavailable HR. Screen received 213 / committed 203 / pending 10 matches SQLite revision 20 exactly; revision 21 retains all 213 unique observations, one epoch, zero sessions/analyses. HOME/reopen keeps the process and one source-start marker; timing/status report stays separate. E10 limited PASS supersedes earlier not-run/not-installed status. Raw SQLite/profile/report/screenshots/identifiers/timestamps stay private outside Git. No native ride or FIT replay; full T02c/N02/T03e/live T04 and science remain open. No publication/merge.


T03e1 publication authorization (2026-11-12): owner requested commit/push/PR for the completed 29-file diagnostic slice and a next-session continuation prompt. Aggregate E10 evidence may be published; private artifacts/profile/identifiers/acquisition timestamps and APKs remain outside Git. Merge/release and further native recordings/new implementation slices are not authorized. Next recommended planning step is a minimal short standstill persisted recording/pause/resume/local-end verification package, with unused ID check and named device-scope approval before execution; full T02c/T03e/resource/science gates remain open.


## T03e2 proposed short lifecycle verification — 2026-11-12

[PR #44](https://github.com/paulnroll/oxywatt/pull/44) is confirmed merged at 7e32e0f; four CI checks passed and no review/comments were present at read-back. Local main was safely fast-forwarded from 429495a. The installed 0.4.0 binary retains its pre-commit provenance from T03e1 E10; merge does not change device provenance.

[T03e2](tasks/t03e2-standstill-lifecycle.md) reserves a previously unused ID for one proposed verification-only stationary Idle → Recording → Paused → Recording → Idle trial. Owner operates native controls; agent checks the existing single-source/single-worker canonical path with L01–L06 oracles for durable controls, one observed-start/local-end session, segment indices 0/1, timer effects and revision-filtered received/committed/pending reconciliation. About 45–90 seconds of state holds, ten-minute total window; no workout/new code/repository or further ordering exception. Raw evidence remains private; Idle is not native save. This complete proposal awaits named owner approval; no new recording/device check was performed. Full T02c/T03e, residency/fault/resource/live T04 and science gates remain open.


T03e2 owner authorization (2026-11-12): owner approved the specified single stationary lifecycle verification and private canonical evidence inspection. Execution may proceed within its bounded procedure; earlier pending-approval status is historical. No publication, reinstall, additional ride or broader acceptance follows.


T03e2 executed trial (2026-11-12): [actual results](tasks/t03e2-standstill-lifecycle.md#executed-stationary-trial--2026-11-12) are PARTIAL because native auto-pause changed the controlled manual procedure. Actual persisted Idle → Recording → Paused → Recording → Paused → Paused → Idle produces one OBSERVED_START/ENDED_LOCALLY session with segment indices 0/1, same epoch and no ambiguity/interruption; analyses remain zero. Final screen 2768 received / 2760 committed / 8 pending / zero loss/rejected matches revision 253 exactly; revision 257 retains all earlier screen receipts. Integrity/foreign-key/session-effect checks and unchanged source/process markers PASS. Hardware silence boundary is not exercised. Raw evidence remains owner-only outside Git; installed pre-commit APK provenance unchanged. No code, setting change, second recording or publication. Optional manual follow-up with temporarily disabled/restored native auto-pause is specified in the task and requires separate approval; broader gates remain open.


T03e2 manual follow-up authorization (2026-11-12): owner explicitly approved one additional stationary trial with temporarily disabled/restored native auto-pause, as specified in the existing T03e2 amendment. Prior partial evidence remains unchanged. No reinstall, code, FIT access, broader matrix or publication authorized.


T03e2 approved manual follow-up (2026-11-12): [actual extended-trace evidence](tasks/t03e2-standstill-lifecycle.md#executed-manual-follow-up--2026-11-12) verifies one additional observed-start/local-end session with manual-only pause logs, segment indices 0/1/2 and no ambiguity/interruption. Owner reported a double click and requested repeating the pause in the same recording; all controls remain retained. Original exact two-segment procedure stays PARTIAL; actual manual persistence/ordering/paused exclusion PASS. Receipt-monotonic hold lower bounds ≥15 s / ≥10 s / ≥15 s verified. Current-epoch screen 30 received = 30 committed, pending/loss/rejected zero, matches revision 313; lifetime 3524 rows are not run-local counts. Integrity/reopen and unchanged source/epoch markers PASS, analyses zero; hardware silence boundary not exercised. Owner confirmed auto-pause restored. Same installed pre-commit APK; private manual-follow-up evidence separate, no code/new build/FIT access/publication. No third trial proposed; broader device/scientific gates unchanged.


T03e2 publication authorization (2026-11-12): owner explicitly requested GitHub publication of the six scoped documentation files with aggregate trial results. Commit/push/PR authorized; earlier no-publication statements are historical. Private evidence/profile/identifiers/times/APKs stay outside Git. Merge/release and further native recordings/device scope remain separate.


## T02c2 proposed short background check — 2026-11-12

PR #45 is confirmed merged at fb810cf (four successful checks, no review/comments); clean local main fast-forwarded safely. [T02c2](tasks/t02c2-short-background-check.md) is reserved after an unused-ID check: proposed verification-only visible 30 s → native page 45 s → verified display-off 45 s → visible 30 s during one stationary recording. Require a present already-paired channel, preferably HR; T03e2's missing-channel manual trial cannot establish present delivery in the background. B01–B06 cover phase evidence, committed receipt counts/gaps, same epoch/source/session, current-epoch counters and private SQLite consistency. Owner handles sensor wake, auto-pause disable/restore and native buttons. No new code, workout, source/repository or ordering exception. Named execution approval pending; no device action or publication in this planning step. USB/charging/ADB limits remain explicit; full T02c/T03e, residency/fault/resource/live T04 and science gates stay open.


## T02c2 deferred; diagnostic T04b3 proposal — 2026-11-12

Owner requested skipping T02c2 now and continuing development, with possible later return. Record the test as owner-deferred/NOT RUN; earlier device evidence retains its limited scope. [T04b3](tasks/t04b3-live-diagnostic-curve.md) proposes the next complete software slice: explicit latest-ended-session calculation using existing committed replay/engine/AnalysisRepository and a durable diagnostic nine-duration readout. Exact live policy support, distinct real provenance/cohort and codec compatibility, canonical-priority cooperative work on the same worker, cancellation/cap/newest-result safeguards and C01–C08 acceptance are specified. No second Collector/repository system, private FIT, physiology or smoothing. The limited diagnostic T04 ordering amendment and implementation await named approval; full T02c/T03e/product live T04, hidden/screen-off/resources and science are not accepted. No device action, application code or publication in this planning step.


T04b3 owner authorization (2026-11-12): owner approved the specified software implementation, including its limited diagnostic ordering amendment. T02c2 remains deferred, broader hardware/scientific gates open. No installation/private-data evaluation/publication inferred.


T04b3 local software completion (2026-11-12, owner-approved): [implementation/evidence](tasks/t04b3-live-diagnostic-curve.md#final-verification-and-disposition--2026-11-12) delivers explicit latest-ended diagnostic curve calculation/save/readout through the existing worker and repositories. Shared stepped replay, exact live policy and real-origin codec/slot, canonical priority/cancellation and cap/newest-result safeguards are verified with synthetic inputs only. Final full debug 170 PASS; full preview 192 PASS before the final cancellation cleanup, followed by 54 focused preview passes; core check/build 120 passes/one private-FIT skip cached, build/lint/format/isolation PASS. Version 0.5.0/code 5 built locally, not installed. No private data/FIT/device analysis or publication. T02c2 remains deferred; full acquisition/resources/product live T04 and physiology remain open.


T04b3 installation/publication authorization (2026-11-12): owner requested same-signer in-place 0.5.0 installation and commit/push/PR, with code review first. Review reproduced/fixed pending-control selection of the preceding session; 172 debug/196 full preview tests and relevant build/lint/isolation pass. Earlier no-install/publication statements are historical. No real Calculate/Retry action, native recording, FIT analysis, merge or broader device acceptance authorized. Private artifacts remain outside Git.


T04b3 reviewed installation/publication (2026-11-12): [actual install evidence](tasks/t04b3-live-diagnostic-curve.md#reviewed-050-installation--2026-11-12) supersedes earlier not-installed status. Pre-PR review fixed analysis selection before latest control acknowledgement; 172 debug/196 full preview and relevant checks PASS. Same-signer 0.5.0/code 5 installed with -r, exact reviewed APK hash read back, Connected/Idle/Ready and version footer observed. Binary provenance remains fb810cf plus reviewed pre-commit changes. No real Calculate/native recording/FIT analysis or controlled hidden/screen-off claim. Private artifacts outside Git; owner authorized PR publication, not merge.


## T04b3 merged; proposed one saved-session device check — 2026-11-12

Fresh repository-account GitHub read-back confirms PR #46 MERGED, head 79cb1cb, merge/current remote and local main e5ffc06, four successful CI checks and no submitted reviews; initial local tree clean. Software is reviewed/merged, while installed 0.5.0/code 5 remains the separately hash-verified fb810cf plus reviewed pre-commit implementation, not a PR/merge build. No fresh device state or real curve is inferred.

[The T04b3 evidence extension](tasks/t04b3-live-diagnostic-curve.md#merged-baseline-and-saved-session-device-proposal--2026-11-12) reserves previously unused task-local C09–C14, without a parallel task/system. **Proposed / NOT RUN, owner decision pending:** one Calculate on the latest already stored eligible session, private input-derived oracle, durable real-origin generation and HOME/reopen comparison with no new generation/source. Fresh acknowledged Idle/current-epoch control, healthy storage, no existing target run, frozen revision/hash/provenance, nine values or explicit unavailable, counter reconciliation and consistent private copies are required. One tap, no Retry; ten-minute window and bounded stop rules. The prior missing-only T03e2 session may correctly yield nine unavailable durations, but current inputs must be checked after approval. Requested permission includes app-only private canonical/result reads and calculation, not FIT replay or another recording. No device/private-data action, application change or publication in this planning step. T02c2 stays deferred/NOT RUN; broader lifecycle/resource/product live T04/scientific gates unchanged. Actual C09–C14 evidence remains pending and is not supplied by software tests or installation.


T04b3 continuation: owner approved C09–C14. [Actual preflight/read-back](tasks/t04b3-live-diagnostic-curve.md#executed-preflight-and-existing-result-read-back--continuation) found an existing AVAILABLE real result before any tap, so new Calculate/save was NOT RUN. Independent missing-only oracle, existing-result UI and HOME/reopen, stable generation/source, counter reconciliation and private SQLite integrity passed; no new recording or analysis. Owner then requested a longer workout; only two standstill sessions exist locally. One-time owner permission now covers C15 on the single previously identified T02b saved trainer FIT, using existing off-device replay/core engine and an independent integral oracle. No live DB write, synthetic relabelling, history picker, physiological model or publication. Detailed scope/evidence and partial R/V mappings stay in T04b3; broader gates unchanged.


[C15 one-time trainer FIT execution](tasks/t04b3-live-diagnostic-curve.md#c15-one-time-trainer-fit-execution) PASS after new explicit owner permission: one previously identified retained FIT, strict existing decoder/export, temporary off-device core harness and independent source-row Decimal integral oracle. All nine duration outcomes/winner boundaries agree and curves match at 1x/10x/unpaced; seven numeric durations, two unavailable because coverage breaks forbid long windows. FILE_RECORD virtual timing/private-owner origin and COLD_ATTACH/INTERRUPTED remain explicit, with no fabricated Idle or live-session qualification. No app/source/build change, device analysis request, live DB mutation or publication. Raw inputs/projection/numeric reports/harness stay outside Git; one-off staging removed, original input preserved. This does not complete the unexecuted new-generation Calculate/save check or full live T04; ordinary core checks and final documentation verification are recorded in the task.


## T04b4 proposed local session selection — 2026-11-13

Owner requested preparation of [T04b4](tasks/t04b4-local-session-selection.md), reserved after an unused-ID search. Implementation and its explicit older-target diagnostic ordering amendment await the concrete scope decision. Show up to 20 recently persisted local session snapshots (truncation explicit), no initial selection, UUID-based selection, truthful observed receipt span and unknown coverage until a saved curve exists. Selected-session Calculate and saved readout reuse the existing source-owned worker, bounded requests/replay, exact current-epoch control acknowledgement, sticky new-recording cancellation, real slot/provenance and capped repositories. Read-only selection/reopen creates no generation/source; new runtime starts unselected. S01–S08 specify synthetic-only software acceptance; no task tests/device/private-data work performed in this planning step. No general history/aggregate/chart, FIT import/reuse, new measurement path or physiology. No publication/installation/real analysis approval inferred; T02c2 remains deferred and wider acquisition/resource/scientific gates stay open. Existing T04b3/C15 local evidence is preserved separately.


T04b4 owner authorization — 2026-11-13: owner approved the concrete software package and its explicit older-target diagnostic ordering amendment. Earlier proposal status is historical. Existing source ownership, current-epoch control acknowledgement, cancellation, bounded list/analysis and provenance/storage safeguards remain required. Installation, private data/device calculation, publication and broader hardware/scientific acceptance remain separate.


T04b4 software complete — 2026-11-13: [S01–S08 evidence](tasks/t04b4-local-session-selection.md#software-verification-and-disposition--2026-11-13) records explicit UUID selection from ≤20 recent persisted local snapshots, older-target calculation, saved readout/recreation without another generation/source, unknown-coverage labels and current-epoch control/cancellation/newest/cap safeguards. Full debug 196 / preview 220 PASS, unchanged cached core 120 passes/one private-FIT skip; build/lint/format/release-preview isolation PASS. Strengthened list/writer interleaving fixture afterward passes full debug 196 / focused preview 2 without application-source changes. Local 0.6.0/code 6 is built, not installed; public USB metadata confirms matching signer with installed 0.5.0 without launch/private data reads. Earlier T04b3/C15 evidence is preserved. Installation, real selected-session Calculate, recording and publication remain separate; T02c2 stays deferred and full acquisition/resource/product/science gates open.


T04b4 installation authorization — 2026-11-13: owner explicitly approved same-signer in-place 0.6.0/code 6 installation and launch/UI readout after fresh Idle/storage checks, preserving app data. No Calculate/Retry, real input analysis, native recording, data clear or publication. Actual installation evidence is recorded in the T04b4 task; raw artifacts remain private outside Git.


T04b4 installed/read back — 2026-11-13: [actual 0.6.0 evidence](tasks/t04b4-local-session-selection.md#verified-060-in-place-installation-and-ui-readout--2026-11-13) supersedes the earlier software-only not-installed status. Owner-approved same-signer install-r succeeded, actual installed APK hash exactly matches reviewed 0.6.0/code 6, and launch/readout verified Connected/Idle, source running, canonical acknowledgement/no visible storage error, two retained local session rows and disabled Calculate with no selection. Footer is e5ffc06 plus reviewed local changes, not a future PR/commit/merge artifact. No data clear, session/Calculate/Retry tap, recording, canonical SQL/FIT analysis or publication. Private raw evidence stays outside Git; UI arithmetic is not SQL-prefix verification, and full acquisition/resource/scientific gates remain open with T02c2 deferred.


T04b4 older-session hardware trial authorized — 2026-11-13: owner approved one previously documented older local session, private input-derived oracle, one Calculate only if still not calculated, saved-result and HOME/reopen comparison. Recheck fresh Idle/control acknowledgement/storage and pin UUID; no Retry/recording/FIT/publication. Extend existing task evidence; prior installation approval alone did not cover this calculation.


T04b4 real older-session trial — 2026-11-13: [actual Calculate/save/reopen evidence](tasks/t04b4-local-session-selection.md#executed-older-session-calculate-save-and-reopen--2026-11-13) verifies one explicit older UUID, one new real-provenance v3 result, frozen input revision/hash, independent all-nine-duration oracle and exact winner boundaries, stable newest result/selection through HOME/reopen, current epoch/PID and revision-prefix counters; newer session/result unchanged. Functional checks PASS; planned 60-second terminal confirmation PARTIAL (pending early copy, available in later copy; true completion time not measured). Separate report reached its bounded limit, so capped report tail was not used as fresh continuity evidence; canonical/result storage continued. One numeric duration and eight unavailable, no missing-to-zero substitution. No Retry, second Calculate, recording, FIT or application change. Private artifacts stay outside Git; no publication. Limited device evidence completes this selected-session path, not the historical latest-only procedure or full acquisition/resource/scientific gates; T02c2 remains deferred.


T04b4 publication authorized — 2026-11-13: owner requested commit/push/PR of the completed 20-file software/documentation scope, with aggregate device/oracle evidence only. Raw/private/generated artifacts remain outside Git. No application change after successful software/device verification; installed binary remains e5ffc06 plus reviewed local implementation, not a future PR SHA. Merge/release, further Calculate/Retry/recordings/private data use remain separate.


## T04b4 merged; Calculate timing scope proposed — 2026-11-13

Fresh repository-account read-back confirms PR #47 MERGED at `61e26bd` (head `1b1d161`, four CI SUCCESS, reviews empty); clean local main fast-forwarded safely. Final review found no additional T04b4 defect requiring a patch. Installed 0.6.0 still derives from e5ffc06 plus reviewed pre-commit implementation; no fresh device state is inferred. Prior numerical/save/reopen evidence remains PASS and terminal observation PARTIAL.

[Existing task timing follow-up](tasks/t04b4-local-session-selection.md#proposed-calculate-timing-follow-up--decision-pending) extends T04b4 instead of reserving another task. Task-local S09–S13 were checked unused before reservation and remain NOT RUN. Source inspection identifies two whole-prefix paged passes, per-effect steps and requested worker waits; these are candidate costs, not a diagnosis. Propose a constant-size optional runtime summary separating hash, replay/page work, worker/canonical/wait/retry time and request/completion/read-back, with independent synthetic numerical/hash/accounting oracles and a bounded host-loop check. UI/ADB observation has a separate clock-aware protocol. Implementation/instrumentation await this concrete owner decision; no new app code, real Calculate/Retry/FIT/recording/private-data access, optimization or publication. T02c2 remains deferred; full acquisition/resources/product live T04 and physiological gates stay open.


T04b4 S09–S13 owner approval and implementation — 2026-11-13: owner approved the synthetic-only timing follow-up with “dawaj”. Existing Job/controller/runtime now account for exclusive hash/page/replay/transaction/control/canonical/wait/backoff work and residual, using one bounded optional runtime summary and independent execution clock. The page seam wraps the existing replay call; no InputRepository API/schema/codec/engine/report/UI change. Literal synthetic numerical/hash oracles, on/off/broken-clock identity, retries/cancellation/report-cap cases and actual SDK-fake runtime-loop accounting are verified in focused debug tests; final full checks are recorded in the task. Host history growth increases scans/steps/waits with unchanged selected result; this is not a diagnosis of prior Karoo timing. No optimization, device/private-data/FIT action or publication; earlier partial device observation and installed-binary provenance remain unchanged. Earlier pending-approval language is historical.


T04b4 timing follow-up complete — 2026-11-13: final S09–S12 software PASS / S13 protocol-only PASS after a reproduced/fixed refused-arm timing-state race. Full final debug 210 / preview 234 PASS; cached core 120 passes/one private-FIT skip, formatting/build/lint/release-preview isolation PASS in 4 min 55 s. Independent selected numerical/hash oracle is unchanged while added unrelated history increases scans/steps/actual waits on the host. Six Markdown files/318 links/anchors and traceability/privacy/whitespace PASS. Local feature branch, uncommitted/unpublished; no device/private input/FIT action or optimization. This completes the approved timing-accounting software scope, not device attribution or the earlier partial terminal-observation criterion.


## T04b4 bounded analysis pacing proposed — 2026-11-13

Owner requested continuing after completed local S09–S13. [Concrete S14–S17 scope](tasks/t04b4-local-session-selection.md#proposed-bounded-analysis-pacing--decision-pending), reserved after an unused task-local ID check, proposes fewer requested waits during actively progressing analysis: at most 16 one-step turns before the existing 1 ms waiter, with canonical/stop checks before every step. List-only pacing, retry deadlines, pages/whole-prefix scan, frozen hashes/numerics and source acquisition ordering remain unchanged. Runtime-only diagnostic-analysis-pacing-v1 configuration is explicit; deterministic wait-count oracle and matched synthetic/native-SQLite/SDK-fake checks cover unchanged numerical identity, canonical priority/cancellation and exact uncertain-write retries. No hard latency/CPU/battery or Karoo claim. Implementation awaits the concrete owner decision; no new pacing code/tests/execution, device/private input/FIT action or publication. Existing eleven uncommitted timing/planning files preserved on the local feature branch; S09–S13 210 debug/234 preview evidence remains its original scope. T02c2 stays deferred and broader gates open.


T04b4 S14–S17 owner approval and implementation — 2026-11-13: following its concrete decision request, owner replied “następne”; the presented software scope is authorized. Existing runtime now amortizes waits across at most 16 progressing analysis turns, retaining outer-loop canonical/stop checks before every single step. A small local pacing helper/controller eligibility signal is independent of optional timing; policy/quantum appear only in the runtime summary. No numerical/hash/SQL/codec/acquisition/UI change. Focused synthetic tests establish exact 43→2 and 2,091→130 waits, unchanged independent curves/hash and canonical/retry/cancellation behavior; final full checks are recorded in the task. Earlier pending language is historical. Host improvements are not Karoo latency/resource evidence. No device/private/FIT action, publication or additional optimization; previous local timing/evidence preserved.


T04b4 S14–S17 final software verification — 2026-11-13: COMPLETE. Full final debug 218 / preview 242 PASS; cached core 120 passes/one private-FIT skip, format/build/lint/release-preview isolation PASS in 4 min 58 s. Exact requested-wait reduction 43→2 and 2,091→130 matches the independent bounded-pacing oracle, with unchanged selected hash/curve/frozen prefix and canonical-first cancellation/retry behavior. Six Markdown files/322 links/anchors plus traceability/privacy/whitespace PASS. Local feature changes remain uncommitted/unpublished; no hardware/private/FIT action. This is verified software/host pacing, not a new Karoo timing/resource result; prior partial terminal observation and all wider gates retain their dispositions.


T04b4 S09–S17 publication authorized — 2026-11-13: owner explicitly requested the PR for the completed eleven-file timing/pacing software/documentation scope. Existing local evidence remains 218 debug / 242 preview PASS with full checks in the task; fresh origin/main is 61e26bd, repository account paulnroll, no existing PR for this head. Final diff/privacy/traceability review found no additional source defect; no application change or new device claim. Commit/push/PR publication is authorized, with remote read-back after execution. Merge/release and device/private-input actions remain separate.


T04b4 S09–S17 PR created — 2026-11-13: PR #48 (https://github.com/paulnroll/oxywatt/pull/48), OPEN, `perf(android): measure and pace diagnostic power curve analysis`, base main 61e26bd, source/publication commit 8a3cc44. Commit/push/creation and matching remote head/base/title read-back verified with the repository account. Initial CI snapshot was one SUCCESS/three running, reviews empty; final head/check state is read after this documentation follow-up. Existing 218 debug/242 preview and software-only evidence unchanged; no new device/private-data claim, merge or release.
