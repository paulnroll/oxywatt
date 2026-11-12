# Decision log

2026-11-10. Owner direction is accepted as intent; technical recommendations remain proposed until reviewed. Owner subsequently approved T01a only in this session.

| ID | State | Decision / rationale | Consequence |
|---|---|---|---|
| D01 | OWNER-DECIDED | Independent Collector; both Karoo and FIT supply one canonical input | [ADR 0001](../architecture/adr/0001-source-neutral-core.md); no SDK types in engine |
| D02 | OWNER-DECIDED | FTP and VO₂max are separate models with explicit credibility | [ADR 0002](../architecture/adr/0002-independent-estimators.md); no CP/FTP equivalence |
| D03 | OWNER-DECIDED | Whole-ride acquisition is the principal feasibility gate; M0 ends before code | [ADR 0003](../architecture/adr/0003-collection-feasibility-gate.md); first live slice only after approval |
| D04 | OWNER-APPROVED ARCHITECTURE | Two physical modules, logical contexts inside core | Avoid seven modules before actual implementation needs |
| D05 | PROPOSED | Room local persistence, Compose application screen, optional numeric field | Verify runtime OS/toolchain first; custom field UI is not arbitrary Compose |
| D06 | PROPOSED | Developer FIT replay early; product import/backfill later | Validate real datasets without adding import UI to 0.1 |
| D07 | PROPOSED | One authoritative estimate per unambiguous session, versioned 42-day recency-weighted trend, basic chart | Owner requests smoothing/history now; defaults require review |
| D08 | OPEN / BLOCKING ESTIMATORS | No automatic FTP or passive VO₂max equation yet approved | T05–T07 must resolve; cannot call manual-only product full 0.1 |
| D09 | OPEN / BLOCKING RELEASE | License choice (for example Apache-2.0 or MIT) | Preserve vendor notices; no license file until owner choice |
| D10 | PROPOSED | Conservative session recovery; no silent merging after ambiguous death | Partial recordings may need future FIT reconciliation |
| D11 | OPEN / BLOCKING LIVE VALIDATION | Actual Karoo OS/build, host binding longevity, background start behavior | T02 source evidence is insufficient to claim reliable recording |
| D12 | PROPOSED | Small agent work packages within existing T01–T09, stable shared contracts, one coordinator for integration | [Package plan](tasks/agent-work-packages.md); smaller task contexts and disjoint file ownership; optional concurrency, unchanged product scope and approval gates |
| D13 | OWNER-REQUESTED WORKFLOW | Project developer skill inspired by Ponytail's minimal complete changes, with TDD for domain behavior and appropriate checks for SDK/build/device work | [Developer skill](../../.agents/skills/oxywatt-developer/SKILL.md) supports T01–T09 packages and existing V scenarios; no separate TDD skill or application authorization |

## PR review corrections — Canvas v0.2

Owner requested corrections to all three review findings. These refine the draft; application implementation is still unapproved.

- R03/R08, T08, V15: interrupted/ambiguous/cold-attached sessions and linked recovery fragments are excluded from trend counts and weights; no automatic merging. A new UUID does not establish independence.
- R07/R08/R12, T03/T08, V16: persisted increasing analysis generations and newest-requested pointers select authoritative results; unavailable supersedes available, pending/failed requests have no fallback, and late completions cannot move the pointer backwards.
- N01/R03/R11, T03, V17: replace nominal-duration queue wording with event/payload hard limits, in-flight accounting, reserved control capacity, bounded loss reporting and fail-closed lifecycle recovery.

Changes after approval must record what changed, why, affected requirement/task/test IDs, and whether scope needs reapproval. No automatic GitHub issues, pushes or publication are part of this bootstrap.

## T01a authorization and contract decision

2026-11-10: owner replied “tak to jedziemy” to the explicit T01a + core/adapters architecture approval request. Approved: minimal Kotlin/JVM build, canonical input/time/profile/recording contracts and focused JVM tests (R01/R02/R11; partial V01/V02/V03). No Android scaffold or other package approval. Domain model contract v1 records signatures, units, nullability and epoch semantics before code. Sequence ordering and lifecycle effects remain T03 responsibilities.

JVM bootstrap uses Gradle 9.1.0 (supports installed JDK 25), Kotlin 2.3.20 (documented Gradle range includes 9.1), Java release/JVM target 17. Sources: [Gradle release](https://docs.gradle.org/9.1.0/release-notes.html), [Kotlin compatibility](https://kotlinlang.org/docs/gradle-configure-project.html). This is not an Android toolchain decision; T02 must resolve that matrix.

T01a verification: wrapper `:core:check` passed (7 JVM tests), including assertion red/green evidence for epoch separation and input validation; see task evidence. No device/scientific gate closed.

## PR #3 self-review corrections

R11/V13/T01a: restrict Kotlin to the Java 17 API with `-Xjdk-release=17`; JVM bytecode target alone left newer JDK API usage possible. Temporary Java 21 API probe compiled before and was rejected after the change. Ten JVM contract tests pass, with additional time-boundary/copy-validation/repeated-reading coverage. `elapsedSince` remains clock arithmetic, not a continuity guarantee. No source contract or Android/science approval changed.

The owner authorized T01b after T01a, then explicitly paused it to publish/review PR #3 before any T01b changes. Keep that package paused; this PR contains T01a and its review fixes only.

## Formatting and CI tooling approval

2026-11-10: owner accepted Spotless and explicitly requested a CI check. D14 (OWNER-APPROVED): use pinned Spotless 8.11.0 and ktfmt 0.64, Kotlin style (4-space block indentation), for core Kotlin and root/core Gradle Kotlin DSL. Add GitHub Actions push/PR formatting and JVM checks on JDK 25, preserving JVM/release 17. Scope and acceptance: [T01a-format](tasks/t01a-formatting-ci.md), supporting R11 and the JVM portion of V13; no new product requirement or application implementation approval. Sources: [Spotless plugin](https://plugins.gradle.org/plugin/com.diffplug.spotless), [ktfmt](https://kotlin.github.io/ktfmt/).

## CI build and reports extension

2026-11-10: owner requested CI tests and build. Extend D14 / T01a-format / R11 / JVM V13 with explicit `:core:build` after existing formatting and contract checks, and upload available HTML/XML test reports even on failure unless cancelled. Original CI passed on main (run 42910646933); updated workflow requires a new remote run. No application scope expansion.

## T01b resumption and synthetic replay evidence

2026-11-10: after PR #3 merged, owner explicitly resumed T01b (“to jedziemy T01b”). Scope: test-only deterministic canonical traces and independent expectations, R01/R02/R11 and fixture-level V01/V02/V03. Canonical v1 remains unchanged; no production collector, SDK/FIT decoder or Android implementation. [Task/evidence](tasks/t01b-synthetic-replay.md): six new tests pass, 16 total JVM tests; full source equivalence and lifecycle acceptance remain open. T01c requires its own approval.

## T01c developer FIT decoder

After PR #6 merged, the owner authorized the next package, T01c. Use fitdecode 0.11.0 (MIT) in the developer fixture verifier. It decodes a generated binary FIT stream and verifies the checked-in projection used by the JVM tests. The developer fixture verifies units, missing reasons, timer event order and file-record provenance for R01/R02/R11 and partial V01/V02. Real Karoo mapping and downstream checks remain open. See [T01c evidence](tasks/t01c-fit-replay.md).

## T02a diagnostic host

The owner approved T02a after PR #7 merged. Keep the existing Gradle wrapper and make the diagnostic app opt-in so JVM CI runs without Android setup. Pin AGP 9.0.1 and official karoo-ext 1.1.9. Use the sample's min SDK 23 and target SDK 34 as candidates until measured on the device. R02/R09/R11 and setup portions of V02/V11/V13 are tracked in [T02a](tasks/t02a-diagnostic-host.md).

## T02b diagnostic probe

After confirming the diagnostic launcher on Karoo 3, the owner authorized [T02b](tasks/t02b-karoo-probe.md). Observe ride states, channel availability and receipt timing on an extension service instance. Advertise FIT binding for observation without emitting FIT effects. R02/R09 and partial V02/V11 remain subject to device evidence.

## D15 — Simulation-first architecture proposal

2026-11-11. State PROPOSED. The owner requested a design review before production changes. [ADR 0004](../architecture/adr/0004-simulation-first-testing.md) specifies RideDataSource immediately before canonical envelope/session assignment, virtual acquisition time independent of playback speed, deterministic generators, lawful real FIT fixtures and separate SDK/device layers. [Work packages](tasks/simulation-first-development.md) add T01d/T01e/T02d and propose a selective T03 dependency amendment. V18–V22 extend traceability. T02 device acquisition and T05–T07 scientific acceptance require their own evidence. No implementation or new dependency was adopted in this review.

## D16 — Project PR workflow skill

2026-11-11. OWNER-REQUESTED WORKFLOW. Add [oxywatt-pr](../../.agents/skills/oxywatt-pr/SKILL.md) after the PR #9 title correction. Validate commit subjects and PR titles independently, perform self-review, and use the repository GitHub configuration. [Task/evidence](tasks/pr-workflow-skill.md). This skill supports existing authorization boundaries.

## PR #9 review corrections — D15

Source observations now precede Collector-owned session/envelope assignment. The proposed terminal callback acknowledges cancellation, EOF and errors after in-flight delivery drains. This closes the missing interruption signal and the self-cancellation deadlock in the initial proposal. V18 adds timer/input ordering at equal virtual timestamps. V19 adds two rides through one source and terminal lifecycle cases. T02c can run without T02d, and V22 evidence is scoped to each package's implemented capabilities. All architecture changes remain PROPOSED.

## T01d authorization

2026-11-11: owner accepted the simulation-first proposal and named T01d implementation (“no to jedziemy”). D15 architecture is accepted; only T01d implementation is authorized. No external test dependency is adopted. See [T01d](tasks/t01d-simulated-source.md). Other packages retain separate approval gates.

T01d completion: production adds only RideDataSource/SourceObservation; simulation support stays in JVM test sources, without dependencies or Android changes. Virtual acquisition uses a harness-owned timer queue and absolute playback-deadline hook. Source termination never emits a fabricated Idle. All 28 JVM tests and core build pass; scope and deferred Collector/hardware criteria are recorded in the task.

T01d PR #10 additional review: reject recursive virtual-clock advances to prevent time reversal; regression reproduced before the fix. Timer cancellation and exception recovery are covered. All 30 JVM tests pass; no scope or live feasibility change.

## T01e authorization

2026-11-11: owner requested PR publication and the next package, T01e, then supplied a private Karoo FIT. Authorizes local decoding and verification, not redistribution of the input or derived ride data. T01e branches from T01d while PR #10 is reviewed. See [T01e](tasks/t01e-fit-replay.md).

T01e software verification: strict developer export and lazy simulator bridge pass eight synthetic Python checks and 33 JVM tests with the private opt-in enabled (one explicit skip without it). Owner FIT passes CRC/timestamp checks and replays identically at all speeds; original file/projection/hash stay private. Public CI uses generated fixtures only, with no redistribution claim. See the T01e task for limitations and commands.

T01e additional review, 2026-11-11: reproduced and fixed platform newline/hash mismatch and unchecked manifest duration/count. Exact UTF-8 export plus fixture LF attributes preserve byte hashes; EOF validation rejects fabricated acquisition tails and count mismatches. Nine Python checks and 35 JVM tests (private opt-in enabled) pass. Scope remains T01e; see task evidence for streaming-validation limits.

## T02d authorization

2026-11-11: after updating main to merged PR #11, the owner accepted the proposed next package T02d ("no to jedziemy"). Scope: real SDK adapter mapping/lifecycle tests under fake 0.1.1, compatibility spike and test dependency isolation (R01/R02/R09/R11; V20/V22). Preserve probe behavior; no T02c/T03 implementation or hardware acceptance is authorized. [Task](tasks/t02d-karoo-adapter-tests.md).

T02d implementation/evidence: KarooRideDataSource normalizes metrics, recording states and configured profiles without changing canonical v1. Published SDK 1.1.9/fake 0.1.1 with Robolectric 4.17 pass 22 app test executions at API 32/34 plus plain mapper tests; core, debug assembly, lint and release dependency isolation pass. The public JitPack SDK selection is explicit and default GitHub Packages coordinates remain unchanged. Core-library desugaring and bounded Gradle memory address actual compatibility/build failures. Opt-in Android CI is manual only and has not run remotely. [Evidence](tasks/t02d-karoo-adapter-tests.md); no hardware gate closes.

T02d pre-publication self-review reproduced reentrant accept callbacks on API 32/34. Fail-closed rejection prevents nested delivery and preserves terminal acknowledgement after outer callback exit, without buffering or changing canonical v1 (V20). See T02d for red/green evidence.

## T03p — Diagnostic preview plan

2026-11-11. OWNER-REQUESTED PLANNING: after asking whether T03a–d will be visible on Karoo, the owner accepted the offer to plan a small separate preview package. [T03p](tasks/t03p-diagnostic-preview.md) / V23 specifies debug-only simulation/FIT → real Collector/admission/repository → read-only screen, with visible source origin, observed versus committed data and separate demo storage. Minimal shared fixture support and Android stream-reader compatibility are explicit dependencies; no runtime Python or private ride bundling. T03e live wiring still needs T02 acquisition acceptance. This records the requested plan, not T03a–d/T03p code or device-test completion.

## English language requirement

2026-11-11. OWNER-DECIDED: all UI, including diagnostic previews, project documentation, code comments, test names and contribution text must be in English. Conversation with the owner may be in Polish. Replace the Polish T03p sketch with English labels and decimal formatting; preserve resource-based strings for possible future localization only if requested. This changes language requirements, not implementation authorization or task gates. Historical owner quotations remain as originally recorded.

## T03a authorization and lifecycle contract

2026-11-11. OWNER-APPROVED: the owner requested the next package after merged PR #13. Implement T03a core lifecycle/identity/recovery, R02/R03/R07/R11 and V02/V03/V05/V09/V15/V18/V19. [Task](tasks/t03a-collector-lifecycle.md) records bounded effects/checkpoints, explicit versioned per-channel silence policy, observed-start versus partial fragments and new-epoch conservative recovery before implementation. T03c owns durable atomicity/idempotency; no Android/live/UI or later-package authorization follows.

T03a software evidence: bounded core lifecycle and checkpoint effects pass 20 new tests, including exact simulation/FIT replay equality at three speeds, repeated restarts, channel-specific gaps and fail-closed source/time validation. The cross-source Idle regression prevents falsely observed starts. Full core check/build passes (54 pass, one opt-in skip); no Android, database, telemetry buffering or publication changes. See [T03a](tasks/t03a-collector-lifecycle.md) for red/green evidence and remaining V04/T03 gates.

T03a pre-publication review reproduced checkpoint mutation on ID allocation failure and reuse of a known ambiguity-root UUID. Allocate/validate before mutation and reject known root/latest collisions; preserve pending recovery changes on retry. Both regressions now form part of T03a verification; no broader repository or live-slice scope is added.

## T03b authorization and admission contract

2026-11-11. OWNER-APPROVED after PR #14 merged: bounded admission/loss/flush for R03/R11, N01/V17. [Contract](tasks/t03b-bounded-admission.md) precedes code. One in-flight batch retains its charge until commit; bounded loss snapshots survive retries, and control exhaustion requires a new epoch with conservative lifecycle recovery. T03c → T03d → T03p is the later sequence; T03e requires T02 hardware acceptance. No later implementation is authorized.
T03b self-review and evidence: twelve new tests pass; fixed repeated batch application, empty flush and loss ranges surrounding later fragments after failing regressions. Preallocated loss metadata stays outside queue capacity. Full core suite passes (68 pass, one private-FIT skip); no hardware/persistence acceptance is claimed.

## T03c authorization and transaction boundary

2026-11-11. OWNER-APPROVED after PR #15: transactional input batches/checkpoints and recovery, R03/R07/R10/R12, V04/V05/V09/V12 and partial V14/V17. [Contract](tasks/t03c-transactional-inputs.md) precedes code. Use platform SQLiteOpenHelper with no new dependency instead of the conditional Room recommendation; exact retry tokens and revision checks prevent duplicate rows or stale state. Schema v1 has no historical production predecessor; unsupported versions fail without data deletion. T03d/e/p remain gated.
T03c evidence: 16 native file-backed SQLite cases pass on API 32/34, including actual SQLITE_FULL under a page ceiling and two concurrent repository writers. Self-review reproduced and fixed receipt replay before lifecycle and missing recovery effects on epoch change; full-storage cleanup now preserves the primary exception. Full Android/core/build/lint/isolation checks pass (56 app executions, 68 core passes and one private-FIT skip). See the T03c task for exact commands, first-schema migration limits and outstanding hardware/result gates.

## T03d authorization and contract

2026-11-11: owner approved T03d after PR #16 merged. [T03d](tasks/t03d-analysis-persistence.md) freezes additive v1 migration, idempotent generation requests, newest-only result reads and transactional whole-aggregate membership invalidation before code. Synthetic results only; T03p/T03e and physiology remain excluded.

T03d software evidence: shared schema v2 additively migrates populated frozen v1 DDL without changing any legacy row. Seven result-storage cases pass on both API 32/34, covering supersession, reverse completion, retry, rollback, concurrent allocation, slot isolation and migration. Full checks pass (70 app tests; 68 core passes, one opt-in skip). No trend mathematics, physiology or hardware acceptance; see T03d for exact evidence and development limitations.


T03p implementation authorized by the owner on 2026-11-11 after PR #17. Readiness: T03a–d contracts are implemented. Preview uses a dedicated application-scoped worker, separate demo database, bounded latest-value projection, and shared dependency-free simulator fixtures. Portable stream replay preserves exporter validation. No live acquisition, physiology or hardware acceptance is included. R02/R03/R07/R10/R11; V22/V23.


T03p software implementation (2026-11-11, owner-approved after PR #17): isolated debug-derived preview consumes actual T03a/b/c pipeline and schema v2. Shared test fixtures and stream FIT reader remain outside release. Received versus committed values, virtual ages, pending/loss, interruption and storage errors are inspectable. Last committed receipt time is explicitly distinguished from an unavailable durable transaction timestamp. See [T03p evidence](tasks/t03p-diagnostic-preview.md). V22/V23 software checks do not close the on-Karoo screenshot/device claim, T02 or live T03e.


2026-11-11: owner authorized T03p preview-only Karoo installation/launch, synthetic tests, screenshots, scoped fixes and PR publication after merged PR #18. No full T02c/T03e authorization follows. Preparation found no ADB device; local software verification passed with up-to-date tests. Installation and hardware claims remain pending. Existing controls cannot deliberately inject loss/storage errors; no fault panel will be added for screenshots. See [T03p readiness](tasks/t03p-diagnostic-preview.md).


2026-11-11 T03p connected-device follow-up: preview-only install and synthetic runs performed on identified Karoo. Owner-authorized scoped fix uses OffsetDateTime for exported FIT offset timestamps after an Android API 32 behavioral red; no physiological/live feature added. Distinct Activity recreation and force-stop recovery were verified separately. Permission-denied SIGKILL is not a process-death pass; no fault panel was added. [Device evidence](../evidence/t03p-karoo-2026-11-11/README.md) supersedes the earlier no-device status without closing T02/T03e.


2026-11-11: owner offered a real HR sensor and confirmed ready for a limited T02b probe continuation. Existing original APK only; no replacement installation or native recording initiated. Record presence/status and sampled receipt timing, without numeric HR, sensor identifiers or raw logs. No full T02c/T03e approval or gate closure follows. [Evidence](tasks/t02b-karoo-probe.md).


2026-11-11 T02b HR trial: owner confirmed transmitter off, then on. Existing probe showed unavailable/searching and later present. Keep sampled callback delivery intervals distinct from sensor freshness or action-to-reconnect latency; no original-probe restart, installation, pairing change, native recording or numeric HR read by the agent. Record these as partial V02/V11 evidence only; no change to the T02c/T03e gate. [Evidence](tasks/t02b-karoo-probe.md).


2026-11-11: owner explicitly requested postponing live tests until the end and continued simulator-backed work without a bicycle. Proposed next named package is [T04a](tasks/t04a-power-curve-engine.md): pure observed power-duration engine on confirmed intervals, R04/R07/R11 and software V06/V09/V13/V18/V19/V22. This narrows the dependency amendment to core calculations before T03e; it does not accept hardware feasibility, authorize T03e or choose physiology. T04b preprocessing/persistence/device projection and parent T04 live acceptance remain separate. Implementation awaits named T04a approval.


2026-11-11: owner approved named T04a implementation after PR #21 merged. Accepted software-only dependency amendment: pure confirmed-interval engine before T03e. Use one engine per session, exact candidate boundary search, compensated weighted integration and explicit bounded saturation. Numerical-tolerance ties choose earliest comparable start; epochs are not ordered by their local origins. [Contract](tasks/t04a-power-curve-engine.md). Hardware and T04b gates remain open.


T04a implementation/evidence: use direct compensated weighted integration rather than growing cumulative-energy subtraction to avoid cancellation/overflow. This has a documented bounded but potentially quadratic per-append high-rate CPU cost. Self-review fixed a publicly mutable shared duration list after a failing regression. Fourteen new tests pass, including an independent exact-grid oracle and four virtual hours; core 82 passes/one private skip. [Evidence](tasks/t04a-power-curve-engine.md). T04b must review actual preprocessing, persistence and integration workload before claiming a live curve.

2026-11-11, after merged PR #22: owner requested concrete T04b specification and authorized its publication, not implementation. Proposed named package [T04b1](tasks/t04b-persisted-curve.md) completes terminal synthetic persisted replay through saved preview curve before T03e. Reuse ordered T03a effects and T03d generation authority, extend scalar terminal codec with a typed curve rather than a second store or nine independently current slots. Hold uses persisted lifecycle timeout; confirmed horizons never extrapolate across process death. Revision-bounded pages/hash retain reproducibility; conservative pre-call work reservation addresses T04a's potentially quadratic dense-then-long cost. Incremental/live optimization is excluded. Proposed engineering defaults and software-only dependency amendment await T04b1 approval. Deferred T02c/T03e/device gates are not passed; older T02b HR probe and T03p synthetic device evidence do not verify current live acquisition.

T04b1 implemented after owner approval following merged PR #23: [terminal synthetic persisted curve](tasks/t04b-persisted-curve.md). Committed revision-bounded effects are converted with preview-silence-v1 hold/expiry to T04a; one typed T03d generation atomically stores all durations and partial/resource provenance. Existing preview reads saved results after reopen; synthetic curves cannot enter aggregate membership. Software evidence: core 90 passes/one private-FIT skip, debug 92 passes, preview 103 passes; builds/lint/release isolation pass. Dense scans have an explicit conservative pre-call work cap. No live T03e, device action, physiological model or private FIT read; T02c and parent T04 live acceptance remain open.


## T04b2 proposal — 2026-11-11

Owner direction: continue on simulator; live tests at the end, without closing hardware gates. After merged PR #24, measured wrapper pessimism separately from direct integration cost. Recommendation pending named approval: one bounded aggregate index with local weighted means and checked work charging, rather than raising the quadratic reservation cap or subtracting floating-point energy prefixes. Preserve existing slot/generation/readout and version algorithm/work provenance. T02c/T03e and parent T04 live acceptance remain open. See [T04b2 specification](tasks/t04b2-long-variable-scan.md).


## Approved T04b2 implementation — 2026-11-11

Owner approved [T04b2](tasks/t04b2-long-variable-scan.md) after specification PR #25 merged. The existing core/power now uses bounded persistent AVL range means and charged work-v2, with exact candidates/coverage/tolerance retained. Canonical hold-v1, the existing logical slot, scalar-v1/curve-v2, atomic newest-generation storage and durable preview readout remain in the same flow. Independent exact oracles and long synthetic speed/reopen checks provide software evidence; see task for final results and resource bounds. This replaces T04b1's conservative quadratic reservation for new calculations, without rewriting old records. No private FIT, live acquisition or physiological model; T02c/T03e and parent T04 live acceptance remain open.

## T05a proposal after PR #26 — 2026-11-11

Owner requested a concrete specification and authorized its PR publication, not implementation. [T05a](tasks/t05a-fixture-reproducibility.md) extends existing RunManifest/fixture support with rights/recipe/profile descriptors and independent replay expectations. Existing FIT byte manifests, replay hash semantics and generation storage remain authoritative; reject a parallel registry/store or generic experiment runner. Separate fixed-identity semantic comparisons from execution metadata. FTP and VO₂max profiles record missing/unresolved inputs without choosing equations or default physiology. A01–A07 are planned software acceptance, not passed evidence. T05b evaluation/sensitivity/go-no-go, T06/T07 method decisions and lawful independent references remain required. Continue simulator work without private FIT/device access; deferring T02c/T03e/parent T04 live checks does not pass them. Named T05a implementation approval remains pending.


## T05a implementation — 2026-11-11

Owner approved named T05a after PR #27 merged. Reuse explicit development event tables and nullable descriptor linkage in RunManifest, preserving legacy hashes. Rights validation checks declarations, not actual consent; current fixture admission supports synthetic inputs only. Separate FTP/VO₂max profile schemas retain missing inputs and model_not_approved without estimator code. Existing typed replay hashing, generation storage and release isolation remain authoritative. [Evidence](../evidence/t05a-software-2026-11-11/README.md). T05b protocol/sensitivity/go-no-go and later scientific/live acceptance remain open.


## T05b protocol proposal — 2026-11-11

[Canonical T05b specification](tasks/t05b-evaluation-protocol.md). Owner requested specification/publication only. PR #28 is merged and all four remote checks passed; local main advanced to dca3639 before this proposal. Proposed smallest named package: protocol review plus worked synthetic qualification/reporting exercise, no code/framework. Independent protocol-defined FTP and cycling CPET VO₂max references are required; CP is not FTP. No lawful paired dataset secured. Open decisions O1–O5 require explicit resolution before experiment admission; readiness does not select T06/T07 methods or admit a product. Named T05b execution approval is pending. Continue on simulator; no private FIT read or device action; postponed live gates stay open.


## T05b executed protocol exercises — 2026-11-11

Owner approved T05b after merged PR #29. [B01–B08 report](../evidence/t05b-protocol-review/README.md) completes the bounded documentation exercise: separate FTP/VO₂max estimands, ten invented qualification cases, participant split/freeze rehearsal, reconciled coverage/refusal/error arithmetic, conditional sensitivity and explicit O1–O5 decisions. Self-reviewed evidence is ready for owner review; it is not a physiological study. Both experiment admissions remain NO-GO; no lawful matched dataset, justified scientific thresholds/sample plan or method selected. T06/T07 retain independent research/selection and later implementation; no automatic authorization follows. T05 protocol/reproducibility deliverables are now recorded, while parent scientific acceptance and deferred T02c/T03e/parent T04 live gates remain open.


## CI plugin repository repair — 2026-11-11

Owner requested repair of main run 43084264896. Add Maven Central before Plugin Portal in settings plugin management so plugin transitive dependencies do not rely solely on the Portal endpoint. Root project repositories apply to project dependencies, not plugin classpaths. Retain all pinned versions and existing CI checks; verify with an empty Gradle user home. This is a tooling correction under [T01a-format F03/F04](tasks/t01a-formatting-ci.md), with no application or architecture change.


## T06a scope proposal — 2026-11-11

[Named T06a scope](tasks/t06a-ftp-method-audit.md) defines a bounded primary-source FTP audit after merged T05b PR #30. F01–F08 map R05/R07 (plus input/privacy R02/R10) to V07/V09 and conditional V02/V12/V18/V19: search log, equation/protocol/population/reference audit, current-input compatibility, conditional sensitivity, rights/data availability and a reviewable recommendation. Named audit execution approval is pending; this specification chooses no method. O1–O5, experiment admission, separate T06b implementation and later live gates remain explicit. T07a stays independent. No application code, private FIT, dataset download, device action or new framework.


## T06a executed audit — 2026-11-11

Owner approved T06a after specification PR #32 merged. [F01–F08 report](../evidence/t06a-ftp-method-audit/README.md) records the bounded primary-source audit, source-access/correction limits, input compatibility, conditional sensitivity and FTP O1–O5 decisions. Recommendation is to retain model_not_approved for automatic FTP from ordinary rides; no audited candidate qualifies on present evidence. An optional controlled-protocol investigation needs a separate scope decision and cannot silently replace ordinary-ride intent. No method, dataset, estimator or experiment is admitted; T06b and independent T07a remain separate. Source numerical results are centralized in the report, not new product accuracy thresholds. R05/physiological V07 and later live acceptance remain open.


## T07a executed independent audit — 2026-11-11

Owner authorized T07a following T06a; PR #33 merged during work and local main/branch updated. [G01–G08 report](../evidence/t07a-vo2max-method-audit/README.md) centralizes primary equations, protocol/population/reference and rights/access findings, input compatibility and conditional sensitivity. Recommendation: retain model_not_approved for ordinary-ride VO₂max on present evidence. VO₂max O1–O5 remain unresolved/blocked; no method, experiment, dataset or T07b implementation admitted. Any optional controlled-protocol direction requires a separate scope decision. FTP remains independent; R06/physiological V08 and deferred T02c/T03e/parent T04 live acceptance remain open. No application code, private FIT, dataset download, device action or framework.

## Owner direction: prioritize within-person change — 2026-11-11

Owner prioritizes useful trends over precise absolute laboratory agreement for R05/R06/R08. [Requirements](../product/requirements.md) record this intended use; [validation strategy](../algorithms/validation-strategy.md) separates change tracking from absolute agreement. T05b O2 is narrowed, with numeric criteria still open. Next specification may define an explicitly labeled exploratory fitness trend; no method/estimator is approved. Repeated physiological references remain necessary for physiological change claims; smoothing and cross-sectional agreement alone do not establish longitudinal validity.


## T08a1 specification proposal — 2026-11-11

PR #34 was verified merged (audited head bfaff35ba0725042dff2e3a7d45dfb532edcfe46, four successful checks); local main advanced to c1526d9 with no unrelated changes. Owner authorized preparing/publishing this specification, not its execution or method selection.

Propose [T08a1](tasks/t08a1-power-hr-trend.md) under existing T08a: synthetic-only comparison of fixed-HR-band power and local interpolation at a frozen HR anchor, raw session results and a fixed-baseline development report using existing replay/storage/authority. Recommend A as descriptive reference and B as comparator, subject to review; accept neither automatically. This refines the owner's trend priority without claiming VO₂max/FTP, stable bias or physiological validity. D07/trend-v0 numeric defaults are not adopted. T07/T07b, T08b/c and T02c/T03e/parent T04 remain separately gated. No new store, manifests, framework, private data or hardware requirement. H01–H07 execution acceptance is planned, not passed.


## T08a1 execution authorization — 2026-11-11

Owner explicitly approved execution after specification PR #35 merged. Scope is the [T08a1 H01–H07](tasks/t08a1-power-hr-trend.md) synthetic-only software experiment, independent oracles, persisted replay/reopen and development report. No production method/estimator, product chart, participant experiment or live gate is authorized. Local main advanced to c2f5a81 with a clean working tree.


## T08a1 executed software experiment — 2026-11-11

Owner approved [T08a1](tasks/t08a1-power-hr-trend.md) after PR #35 merged. [H01–H07 evidence](../evidence/t08a1-power-hr-trend/README.md) records the synthetic A/B experiment through actual committed replay, scalar-v1/reopen, newest authority, fixed-baseline/raw reports and fault/cohort/resource checks. Ten new core tests and ten API 32/34 preview executions pass; full core 117 passes/one private-FIT skip, debug 92/preview 116, build/lint/release isolation and final focused checks pass. No production method/estimator, chart, schema, store or manifest was added.

Both candidates reproduce injected observational changes but show confounder-driven false directions; neither is selected for physiological interpretation. Software H01–H07 does not close O1–O5, R05/R06/physiological V07/V08, T07/T07b, T08b/c or deferred T02c/T03e/parent T04 live gates. No participant data or hardware evidence. Smoothing and legacy trend-v0 numeric defaults remain unapproved for this route.


## T08a2 qualification and pilot-readiness proposal — 2026-11-11

[T08a2](tasks/t08a2-comparable-observations.md): Owner authorized specification/publication after T08a1, not implementation, method selection or participant research. PR #36 was verified merged at 31b43adaff60a0e68ceb163a3bc0149815f3a33f and local main fast-forwarded with a clean tree. Propose a documentation/evaluation-design package rather than another synthetic-only expansion or a forced A/B winner.

Specification/publication only is authorized; named execution awaits owner approval. N, windows, HR/drift thresholds, meaningful change, baseline horizon and smoothing remain open with resolution paths. No lawful dataset admitted; real-data/scientific pilot is NO-GO. T05b O1–O5, physiological R05/R06, T02c/T03e/parent T04 live gates and T08b/c remain open. No bike, Karoo or private FIT required.


## T08a2 executed qualification review — 2026-11-11

[Task/evidence](tasks/t08a2-comparable-observations.md): After owner merged PR #37 and explicitly approved T08a2, executed the documentation-only I01–I07 review. Decision: document-design deliverable ready for owner review, real-observation pilot NO-GO pending lawful source/context and separately authorized acquisition; frozen evaluation and physiological method/claims also NO-GO. No code, method selection or participant research follows automatically.

Real-data/scientific admission remains blocked; N, qualification thresholds, meaningful change, baseline horizon and smoothing remain unresolved without defaults. T05b O1–O5, T02c/T03e/parent T04 and T08b/c remain open. No new framework, storage, manifests or UI.


## Public-data discovery follow-up — 2026-11-11

[Search findings](../evidence/t08a2-comparable-observations/README.md): Owner authorized metadata/documentation search and requested assessment of age/sex/mass. Recommend Salford four-rider material for a later bounded structural audit, subject to a separate decision lifting the earlier participant-download restriction. No raw records, implementation, method selection, participant contact or publication performed.


## Authorized Salford structural review — continuation 2026-11-12

[Audit evidence](../evidence/t08a2-comparable-observations/README.md): Owner explicitly approved public Salford four-rider archive download and local structure/quality audit. Acquisition/inspection executed outside Git; no other archives or participant contact authorized. Decision: retain raw source, reconcile timing/date/alignment before proposing any bounded development analysis; current comparative and physiological gates remain NO-GO. No private rides or app changes. Owner subsequently authorized report-only PR publication; no merge or later analysis approval follows.


## 2026-11-12 — Salford timing hypothesis remains distinct from canonical time

Owner authorized concrete inspection and documentation on the retained named archive. [T08a2 evidence](../evidence/t08a2-comparable-observations/README.md#source-specific-timealignment-inspection--2026-11-12) now verifies that stored duration formulas multiply count by an interval. Ratio agreement is therefore not independent evidence for actual clocks. Same-date/equal-extent columns support only a conditional index-pair hypothesis (711 at 5 s, 7 at 1 s), not synchronization or observed ride boundaries.

Recommendation: next investigate source-index availability with disclosed nominal-time assumptions and all exclusions, retaining records/coordinates outside Git. This is existing T08a2 discovery; no application/importer/replay/A/B or new implementation ID needed for that report. Comparative pilot remains NO-GO; numeric qualification, baseline/smoothing and physiological gates remain unresolved. PR #39 was verified merged and local main updated; its merge grants no next-experiment/publication permission. No further PR merged or published here.


## 2026-11-12 — Execute authorized Salford availability summary

Owner approved the preceding report-only proposal. [Actual evidence](../evidence/t08a2-comparable-observations/README.md#executed-source-index-availability-report--2026-11-12) retains all source dispositions and the 718-column subset, split by nominal grid. Numeric material is abundant, but positive-HR availability and marginal distributions do not establish synchronization, independent visits, context, stable windows or physiological repeatability. Decision: preserve pilot NO-GO; seek contemporaneous source timing/export/artifact evidence before proposing stronger use. No repair, application/import/replay/A/B or publication authorized/performed in this continuation.


## 2026-11-12 — Bound Salford source-documentation conclusions

Owner approved the proposed documentation review. [Source matrix](../evidence/t08a2-comparable-observations/README.md#executed-source-recordingexport-semantics-review--2026-11-12) independently confirms the paper's nominal 5 s statement while retaining the workbook's 1 s evidence. Official SRM storage/stop/export descriptions are model/version-dependent, without an established Salford device/configuration/transformation chain; no sentinel or clock reconstruction becomes verified. Decision: stop archive-only comparable-pilot qualification pending source-linked timing/settings/row evidence. A deliberately assumption-limited future A/B experiment needs its own concrete named review/approval; none authorized or executed here. No source contact, other participant archive, code or publication. Useful within-person trend remains an unmeasured goal, with scientific/live/product gates unchanged.


## 2026-11-12 — Prepare T08a3 with a support-first numerical gate

Owner requested preparation of the proposed bounded Salford experiment package. [T08a3](tasks/t08a3-salford-index-ab.md) is checked/reserved as the next unused subdivision; no execution/publication approval inferred. Recommendation: approve named support discovery first, delivering per-column support/refusals and an evidence-backed numerical-plan proposal. Require an accepted complete freeze and explicit A/B execution scope before outputs, rather than transplanting synthetic parameters. All archive material is exploratory development; fixed primary workbook/stratum and no fallback prevent favorable-result selection. Reconstructed timing remains an assumption, not canonical or physiological acceptance. No numeric policy/model, new acquisition/contact, app implementation or T08b/c change authorized.


## 2026-11-12 — Complete approved T08a3 support discovery with numerical NO-GO

Owner approved the named support unit. [Evidence](../evidence/t08a2-comparable-observations/README.md#t08a3-executed-support-discovery--2026-11-12): all within-stratum column pairs share some observed HR, yet support overlap and whole-record distributions differ; fixed primary/no fallback retained. A support-only minimax geometry yields 115 bpm with 112–118 bpm neighborhood, but 2–729 rows per column and unresolved time/artifact/segment policy prevent a complete J04 freeze. Decision: record J01–J03/J08 complete, J04 NO-GO/not frozen, J05–J07 unperformed/unapproved. No invented A band/windows/fit defaults, no A/B or app changes. A simpler raw-row question would need a separately reviewed estimand/scope; current qualified/comparative/physiological gates remain open.


## 2026-11-12 — Authorize report PR publication

Owner explicitly requested a PR containing the completed T08a2 timing/availability/source reviews, T08a3 package and approved support-discovery results. Publish aggregate documentation only, preserving external source/artifact privacy and all numerical/scientific/live gates. No A/B execution, new data/contact, application implementation or merge authorized. Earlier local-only statements record prior phases and do not override this explicit publication request.


## 2026-11-12 — Propose a distinct finite stored-row estimand

OWNER-REQUESTED PLANNING, not execution approval. Reserve unused [T08a4](tasks/t08a4-salford-exact-hr-rows.md) after checking backlog/work packages. Recommend exact-HR conditional row means and complete same-level primary-column contrasts, retaining every recorded positive level instead of choosing the 115 bpm support proposal or inventing A/B windows/fit policies. No scalar across HR levels, outcome-selected subset, cleaning or temporal/physiological claim. Equal-index pairing, equal row weights and arithmetic means are explicit development assumptions. Named owner acceptance/execution must precede all HR-conditioned power calculations. T08a3 qualified-segment numerical freeze remains unchanged. No audit rerun, data processing, app change or publication performed.


T08a4 execution authorization (2026-11-12): owner explicitly accepted the complete `salford-exact-hr-rows-v1` proposal and approved its local numerical execution with K01–K06 checks. Development assumptions remain assumptions; no original A/B, application code, physiological admission or Git publication authorized.


## T08a4 executed stored-row description — 2026-11-12

Owner explicitly approved the complete T08a4 configuration and numerical execution. [Aggregate results and verification](../evidence/t08a2-comparable-observations/README.md#t08a4-executed-exact-hr-stored-row-description--2026-11-12): fixed-primary 197 columns, all 201 exact HR levels, 15,189 raw means (2,271 one-row) and 1,074,462 same-level differences (187,894 with a one-row side); every unavailable cell retained. Equal-index pairing remains assumed, weights are equal stored rows and artifacts remain uncleaned. No anchor, A/B, qualified segment, cross-HR scalar, trend or physiology. Independent source/rational checks and full final rerun provide report-only evidence; exact data/scripts/results retained outside Git. T08a3 J04–J07 and all live/scientific/product gates unchanged. No application code authorized. Subsequent report-publication authorization is recorded in the T08a4 task.


T08a4 publication authorization (2026-11-12): owner explicitly requested commit, push and PR for the nine scoped documentation files. Earlier local-only statements are historical snapshots. Publish aggregate evidence/specification only; detailed data/scripts/results stay outside Git. No merge, further analysis, application implementation or physiological gate approval.


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


## T08a5/T08a6 publication authorization — 2026-11-12

Owner explicitly requested a PR for the completed T08a5 and T08a6 documentation. This authorizes commit, push and publication of the ten scoped Markdown files, including both task specifications, aggregate results, verification and synchronized references. Earlier no-publication statements describe their preceding execution stages. Detailed source/column/row/Power values, research scripts and complete result tables remain outside Git. No merge, new numerical experiment, source acquisition/contact, application implementation or physiological/product admission authorized.

Publication self-review covers the complete diff and both new tasks: fixed source/115-bpm anchor/method/endpoints, exact versus interpolated evidence, single-row and zero flags, complete 3,226-deletion/197-envelope denominators, unavailable versus zero and partial envelope labels, independent/rerun/current-input/no-fallback evidence, claim limits and privacy. Ten Markdown files only; no raw participant records, dates/notes, private contributor identifiers, local research tools/results, credentials or application code staged. Relative-link/anchor checks (314), requirement/validation and L01–L06/M01–M06 traceability, new-file whitespace and `git diff --check` PASS. No application/hardware tests rerun for documentation-only publication; actual external source/report checks remain the execution evidence. Verify remote PR head/base and CI separately after publication.
