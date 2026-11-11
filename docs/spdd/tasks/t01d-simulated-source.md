# T01d — Shared source and deterministic simulator

Status: OWNER-APPROVED on 2026-11-11 (owner: “no to jedziemy” following the named T01d proposal).

Scope: R01/R02/R07/R11; V18/V19 and manifest/isolation portions of V22. Depends on canonical v1 and accepted ADR 0004. Production code contains only the source port and observation value. Test support owns lazy scenarios, virtual time, playback deadlines and terminal lifecycle. No Collector, SDK migration, persistence or physiological analysis.

Files: core collection contract, core test simulation support/tests, domain model and synchronized Canvas/decision records. No new dependency.

Acceptance: literal signal tables; identical observations at 1x/10x/unpaced; timers before same-time input and through silence; monotonic receipt with UTC jump; duplicate start rejection; quiescent EOF/error/cancellation; new epoch on restart; stable manifest hash. Collector coverage/session decisions and history exclusion remain T03/T08 acceptance.

Risks: virtual scheduling must never use machine clocks; lazy input must not create an upstream queue. Playback driver uses absolute deadlines without sleeping in JVM tests. Source delivery is driven serially by the harness; cancellation is safe from within a callback.

## Verification and Done evidence

2026-11-11: T01d implemented and verified. `./gradlew spotlessApply :core:check :core:build --console=plain` passed on JDK 25 with JVM/release 17: 28 tests, zero failures (17 baseline + 11 simulation tests). Initial timer test failed with an assertion against the no-op scheduler, then passed after implementation. Subsequent signal/lifecycle/integration cases passed.

- V18: exact literal constant/ramp/interval/effort tables, a 3,601-observation hour at 250 W, absolute playback deadlines at 1x/10x/unpaced with identical observations and timer firings, ordered ties, silence timers, UTC regression with monotonic receipt intact, flush-before-sample at the exact boundary.
- V19 source scope: explicit dropout versus valid zero, silence, profile snapshots and pause transitions, two rides on one source without source-assigned session IDs, duplicate-start rejection, idempotent/reentrant cancellation, concurrent cancellation acknowledgement after callback exit, iterator/start/sink failures, EOF without invented Idle, new epoch on restart and lazy bounded consumption. This source acquires no external registrations or owned scheduler tasks; there are none to release on a failed start.
- V22 fixture scope: seeded noise repeatability, versioned simulated manifest hash; sample manifest/hash written to ignored `core/build/simulation-evidence/manifest.txt`. JAR inspection confirmed no simulation classes in `core.jar`. No dependency added. Scenario ID/version must identify the complete literal configuration (including auto/manual pause details); changing configuration requires a new scenario ID/version. Literal offsets allow independent channel schedules, jitter, delivery lag and fault injection.
- All relative Markdown links under `docs/` resolve; `git diff --check` passes. No skill files changed.

Playback uses a harness-provided absolute-deadline waiting hook; tests run it without sleeping. All acquisition timers use the same harness-owned virtual timeline. Sources own no background thread or queued event buffer. The harness must supply a fresh timeline/epoch and fresh lazy sequence for each run. Real paced playback and hardware behavior are not claimed.

Collector session identity, interrupted/ambiguous eligibility, gap coverage, bounded admission under overload, storage/history isolation and power integrals remain their owning T03/T04/T08 checks. No analytical or scientific result is implemented here. T01e/T02d and live adapter migration remain unapproved.

## PR #10 additional code review

Found a virtual-clock reentrancy bug: a timer at 5 could recursively advance to 20 while an outer advance targeted 10, allowing the outer call to rewind the clock to 10. A regression test failed before the correction. Recursive advances are now explicitly rejected; timer callbacks may still schedule/cancel tasks. A second test checks that cancellation removes timers and an exception releases the advance guard. All 30 JVM tests pass after the fix. No canonical contract, SDK or hardware claim changed.
