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
