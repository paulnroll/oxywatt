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
