# T01b — Synthetic replay fixtures

Status: owner resumed this named package on 2026-11-10 after PR #3 review/merge. Complete at the synthetic-fixture level; parent T01 remains incomplete.

Goal: deterministic test-only canonical traces with independently specified expectations for irregular time, missing channels and recording/profile transitions. Requirements R01/R02/R11; fixture-level V01/V02/V03 only.

Scope: `core/src/test/kotlin/com/oxywatt/core/collection/SyntheticReplayFixtures.kt` and `SyntheticReplayTest.kt`, plus specification/authorization synchronization. Accepted dependency: [canonical input v1](../../architecture/domain-model.md#canonical-input-contract-v1--t01a). No build or production contract changes.

Fixtures: irregular receipt offsets 0, 0, 250, 500, 1750, 1750, 2000, 2750, 3000, 4000, 4250, 5000 ms; explicit initial Idle, start, profile, HR dropout, zero/repeated power, pause/resume/end. Separate cold-attachment/restart trace retains different session/epoch IDs and resets the local sequence. Live-like and file-like synthetic variants share values. The file-like record times precede receipt by 10 seconds so the fixture exposes accidental substitution of the receipt clock. Neither variant is an SDK adapter or decoded FIT.

Replay contract: synchronously deliver supplied envelopes in their given order, once each per invocation, without sleeping, sorting, deduplication, alignment or lifecycle interpretation. A fresh replay starts from the beginning. No production replay engine is introduced.

Independent oracles: literal event order/offset/value/state/profile tables in tests, including a 1,250 ms receipt interval, zero versus null/dropout, equal readings with distinct local keys, and null cross-epoch duration. No power integration or model expected value is calculated; those require their later policies. Pauses and cold attach are inputs for future T03 state-machine tests, not proof of window/identity behavior.

Verification: JDK 25, `GRADLE_USER_HOME="$PWD/.gradle-user-home" ./gradlew :core:check --console=plain`; local Markdown links, R/V traceability, `git diff --check`. No skills changed.

Risks/Done: synthetic provenance must remain explicit; do not mistake matching synthetic values for real source equivalence. Done requires passing replay/oracle tests and actual evidence below. T01c decoder/license/lawful fixtures, T03 lifecycle behavior, device and science gates remain open.

## Evidence

- Behavioral red: focused `:core:test --tests '*SyntheticReplayTest'` compiled and failed at the delivery-count assertion (expected 12, actual 0) with the empty replay body. Replaced it with synchronous ordered delivery.
- Green/final: `GRADLE_USER_HOME="$PWD/.gradle-user-home" ./gradlew spotlessApply :core:check --console=plain` → BUILD SUCCESSFUL on Temurin 25; 6 new replay tests plus 10 existing contract tests, zero failures/errors. Local Gradle socket required sandbox escalation; the initial sandbox startup failure was not counted as behavioral red.
- New tests cover literal irregular-time/order/value expectations, repeat replay, empty input, unsorted/repeated delivery, missing HR with valid power, recording/profile facts, synthetic timing provenance and separate cold-attach/restart epochs.
- No production source/build/dependency changes, real FITs or device tests. Existing concurrent CI/documentation changes were preserved.

- Documentation checks: 42 relative Markdown file targets resolve; R01/R02/R11 and partial V01/V02/V03 traceability reviewed; `git diff --check` passes. Skill files/frontmatter are unchanged.

## Self-review before opening the PR

The fixture now gives file records a separate clock, 10 seconds before receipt. The test checks both timestamps against literal values and compares receipt offsets at nanosecond precision. The review packet and vertical slice status now include T01b. On base `6fb72ae`, `spotlessApply :core:check :core:build --console=plain` passed with 16 JVM tests and zero failures or errors.
