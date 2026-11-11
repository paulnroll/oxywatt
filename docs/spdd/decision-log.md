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
