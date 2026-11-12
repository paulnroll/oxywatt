# T04b3 — Explicit saved curve for the latest ended diagnostic session

Status: **OWNER APPROVED on 2026-11-12, including the limited diagnostic ordering amendment; software implementation complete locally; verification evidence below.** Repository-wide Markdown search found no T04b3 or T04c1 task/reference before reservation on 2026-11-12. Owner deferred T02c2 and requested continued progress; that does not itself approve this newly specified implementation. No application code, device analysis or publication in this planning step.

## Problem, goal and limited ordering amendment

Real canonical inputs now persist through T03e1, and T03e2 verifies actual local recording/end with documented deviations. T04a/T04b1/T04b2 already provide a tested observed-power engine, committed replay, typed result storage and synthetic preview. Deliver one complete diagnostic use: explicitly calculate and display the latest eligible ended session's saved curve from the existing live database.

Propose this narrow diagnostic T04b3 before full T02c/T03e acceptance, while [T02c2](t02c2-short-background-check.md) is owner-deferred. This is a dependency amendment to [ADR 0003](../../architecture/adr/0003-collection-feasibility-gate.md)/[ADR 0004](../../architecture/adr/0004-simulation-first-testing.md) and parent T04b, requiring named T04b3 approval. It does not claim whole-ride acquisition, pass hidden/screen-off testing, or admit product live T04. Keep results explicitly diagnostic, with acquisition verification still conditional.

Dependencies: [T03e1](t03e1-live-diagnostic-persistence.md), [T03e2](t03e2-standstill-lifecycle.md), [T03a–d contracts](../../architecture/domain-model.md), [T04a](t04a-power-curve-engine.md), [T04b1](t04b-persisted-curve.md), [T04b2](t04b2-long-variable-scan.md) and [power semantics](../../algorithms/power-curve.md). No new scientific equation or external source claim; [ADR 0002](../../architecture/adr/0002-independent-estimators.md) remains unchanged.

## Current implementation gaps, verified from code

- CanonicalPowerReplay accepts only preview-silence-v1. Live inputs use karoo-diagnostic-silence-v1, 5,000,000,000 ns. Explicit support and independent tests are needed; renaming the policy is invalid.
- PowerCurveCalculation requires a SYNTHETIC manifest and uses a synthetic-only result cohort. Live origin must not be disguised as synthetic or mixed into that slot.
- Calculation currently performs two complete scans synchronously on the preview worker, where acquisition is stopped. Copying that call into the live writer loop could delay ingestion. Reuse the implementation with bounded cooperative progress rather than introducing another Collector, worker, queue or repository system.
- StoredPowerCurve/curve codec v2 and AnalysisRepository already support typed durable results/newest generation and reject curves from aggregate membership. Preserve those semantics and scalar compatibility.

## Observable behavior

Add an English resource-backed action, “Calculate latest ended session”, to the current diagnostic Activity. Target only the latest committed session in the checkpoint when it is ENDED_LOCALLY, OBSERVED_START and unambiguous; do not search backward for an older eligible session. Active/paused/interrupted/cold/ambiguous latest sessions show an explicit unavailable reason. The source must currently be Idle and canonical storage healthy. No history picker or automatic end-triggered calculation.

A click latches one coalesced request for that session; duplicate taps retain no action queue. UI/Activity does no SQLite read, numerical work or source ownership. After local end is acknowledged, freeze revision R and terminal snapshot from persisted effects, then hash/replay only <=R through the existing bounded cursor. Live out-of-session callbacks may continue; pending need not globally reach zero. Do not analyze their uncommitted tail, reset counters or stop/restart acquisition to obtain quiescence.

Display nine existing durations (5/30/60/180/300/600/1200/1800/3600 s), W or unavailable reason, generation/status, input revision/hash, local-end/session origin, calculation completeness/resource limit and “diagnostic; acquisition verification incomplete”. “Observed best mean power” is not FTP, CP, VO2max or maximal physiological capacity. Missing power remains unavailable; valid zero power may produce zero records. HR/cadence availability is not required by the power engine.

Readout comes from the acknowledged newest saved result. Recreate/HOME/reopen never requests a calculation; existing saved results can load without issuing another generation. Pending/failed/unavailable newest has no older fallback. Repeated Calculate with unchanged terminal input/config and an available saved generation displays it rather than appending duplicate runs. An explicit Retry for failed/abandoned work may request a newer generation; make supersession visible. No retry or analysis is initiated by Activity construction.

This first slice excludes interrupted/ambiguous fragments from calculation instead of merging them. No real history/aggregate/chart, trend/smoothing, FTP/VO2max, FIT read/import/export, native-save inference or processing of earlier private FIT files. Later fragment analysis would be a separate reviewed expansion.

## Timing, origin and storage contracts

Use canonical-power-hold-v1 with the existing confirmed-horizon, timer-before-equal-input, loss/missing/pause/segment/epoch breaks and T04b2 engine/work-v2 unchanged. Accept the new exact live policy tuple karoo-diagnostic-silence-v1 / 5,000,000,000 ns. Preserve existing preview policy behavior and hashes; unknown live policy/version/timeout fails explicitly. Receipt elapsed time governs engineering holds, never a fabricated measurement timestamp or sensor-freshness claim. Last value alone cannot extend the committed horizon.

Add an explicit acquisition-origin input to PowerCurveCalculation with compatibility for existing synthetic callers. Retain the existing synthetic slot/hash behavior. Define a separate real slot cohort `real-karoo-diagnostic-power-hold-v1-work-v2` for metric observed-power-curve; record the exact live policy/timeout, work settings and real acquisition metadata in provenance/hash. No fake synthetic manifest/seed/profile. Selected committed input metadata must consistently identify real-karoo-diagnostic and supported acquisition versions; reject mixed/unknown provenance.

Extend typed curve encoding to v3 for an explicit real/synthetic origin descriptor and the acquisition provenance needed to reproduce it; continue reading v2 synthetic curves and scalar-v1/terminal-v2 results without rewriting them. Unknown versions fail explicitly. Do not alter existing synthetic equality/hash vectors or accept a real curve in the synthetic slot. No new table/schema or aggregate membership; AnalysisRepository's curve-membership rejection stays in force.

Use the existing AnalysisRepository against the same oxywatt-live-diagnostic.db, confined to the existing live worker. Every database helper opened for live results must apply/verify the existing 32 MiB main-file cap, including result request/completion connections. Keep preview defaults unchanged. Analysis cannot rotate/delete inputs or write a parallel measurement/result file. FULL/failure preserves input data, leaves result status honestly pending/failed and does not imply canonical-write failure or success. Journal/temp overhead remains outside the main-file cap.

Freeze request UUID/provenance and prepared terminal across uncertain-response retry. Same generation/result is committed idempotently; no rehash against a later revision or duplicate result after a lost response. After process death, show persisted newest status and require explicit Retry for abandoned work; do not automatically resume a numerical scan or merge sessions. Request/completion must use existing newest-requested authority and retain zero aggregate memberships.

## Single-worker scheduling and bounds

Keep DiagnosticExtension the sole source owner and T03e1's stamp/admission/cut gate and input worker intact. Convert the existing calculation's hash and replay passes into small resumable steps usable by both preview and live callers; do not fork their semantics. Preview can drive the same steps to completion as before. No generic job framework or second analysis executor.

Before each analysis step, pump due canonical input/retry/cut work first. Retain at most one analysis cursor/request/prepared completion, one replay page <=256 effects and existing bounded engine state. An analysis step processes no more than one effect and its existing bounded numerical calls before returning to the driver; no whole-history list or scan of the live database on every UI refresh. The paged cursor may retain a single page between steps. Preserve T04b2 limits (16384 intervals, 100,000,000 charged work units) and cancellation checks inside engine work. These are operation/structure bounds, not a measured Karoo latency/RSS budget.

A new received Recording/Paused, transport/source stop, canonical retry/fatal status or application teardown cancels analysis through a fixed source-state/cancellation flag checked before pages/effects and inside the existing engine cancellation path. It does not wait for lifecycle to pass queued inputs before noticing a new ride. Cancellation cannot change Collector state, acknowledge admission early, or block canonical retry. An incomplete newest result is marked honestly; no old curve fallback. Read-only input pages and numerical work run outside the admission gate/main thread. Tests must interleave real canonical commits with both passes and with cancellation, not only mock the coordinator.

Do not advertise a five-second maximum flush latency from this design. Verify deterministic writer progress between analysis steps under synthetic load and retain N01 pending/loss/error behavior. If existing engine calls cannot satisfy those cooperative/cancellation contracts without a material core change, document that concrete mismatch before expanding the package.

## Independent acceptance and traceability

C01–C08 are task-local acceptance cases; actual software execution evidence follows below. Software tests use safe synthetic canonical inputs with clearly test-owned real-origin metadata in temporary live-shaped SQLite. They establish software behavior, not actual Karoo acquisition. No private database or FIT is needed for implementation acceptance.

| Case | Inputs and observable oracle | Mapping |
|---|---|---|
| C01 | Present zero, repeated readings, explicit missing, pause/resume, loss, exact live five-second expiry, timer-only horizon and equal-time boundaries produce hand-authored intervals. Unknown/mismatched policy/origin fails; preview remains unchanged | R02/R03/R04 → V02/V05/V06/V18/V19 |
| C02 | Literal live-policy trace with >5 s confirmed 200 W gives 200 W at 5 s, longer unavailable; refreshed constant trace covers all nine durations. Independent irregular-time energy table and gap/pause cases forbid crossing breaks. Missing-only session gives nine unavailable entries, not zero | R04/R07 → V06/V09 |
| C03 | Actual InputRepository → revision R → shared calculation steps → AnalysisRepository → close/reopen yields the expected curve/origin/hash. Later live commits/new session cannot enter R or change the target snapshot. Latest noneligible session does not fall back to an older ended one | R03/R04/R07/R12 → V04/V09/V14/V15 |
| C04 | Repeated taps/recreate/reopen create no duplicate request; one explicit failed/abandoned Retry supersedes correctly. Request/completion uncertain-response and reversed completion retain exact retry/newest authority, no old curve fallback | R07/R12 → V14/V16 |
| C05 | Synthetic traces interleave continuing out-of-session inputs, timer/cut/flush, slow storage and both analysis passes. Canonical writer progresses between steps; at most one page/action/job, N01 queued+in-flight unchanged. New Recording/source loss/canonical retry cancels before further numerical progress | R03/R11 → V04/V13 software/V17/V20 |
| C06 | Real-origin v3 and old synthetic curve-v2/scalar-v1 round-trip; distinct slots/hashes/provenance, unknown versions rejected, zero aggregate membership. Live result helper enforces 32 MiB; actual SQLite FULL leaves old inputs intact and newest result honestly incomplete | R07/R10/R12 → V09/V12/V14/V16 |
| C07 | Diagnostic UI shows saved nine-duration result or explicit unavailable/pending/error/conditional status; Activity owns no source/SQL/calculation, reopening makes no new generation. Preview remains isolated and existing synthetic readout/equality tests pass | R04/R07/R08/R10 → V09/V12/V22/V23 software |
| C08 | Core/debug/preview tests, build/lint, format, release/preview isolation, links/traceability pass with actual counts. No application installation/device recording/previous private-data analysis is claimed from these tests | R04/R07/R11 → V06/V09/V13 software/V22/V23 software |

## Files, checks, risks and Done

Expected application ownership: existing core CanonicalPowerReplay policy validation and focused tests; app persistence PowerCurveCalculation/PowerCurveCodec/AnalysisRepository/CollectorDatabase and tests; DiagnosticRuntime/LiveDiagnosticCoordinator only for bounded source-owned orchestration; MainActivity/resources and Activity/integration tests. No second Collector/InputRepository implementation or copy of PreviewPipeline, module, dependency, manifest permission or SDK pin change. Proposed future diagnostic app version 0.5.0/code 5 distinguishes the behavior; no binary is built/installed by this document.

Documentation: this task, T02c2 deferral, parent T04b, algorithm/evidence ledger, validation, Canvas, decision log, implementation plan, work packages and ADR ordering links. Use behavior-driven regression tests before changing real-policy validation, authority, cancellation or cap handling. Run the established JDK 25/JVM 17 core check/build, debug/preview unit tests, debug/preview assemble/lint, format and release/preview isolation with karooDiagnostic/karooSdkJitpack opt-ins; never enable private-FIT replay. Record exact commands/results and do not relabel inherited tests as rerun.

Risks: accidentally treating real acquisition as synthetic, accepting an unknown hold policy, starving canonical input commits, result FULL consuming the same finite storage, stale result fallback and overclaiming a complete ride. Resolve them with explicit origin/versioning, cooperative priority/cancellation, frozen retries and honest readout. Full T02c/T03e/live T04 and physiology remain conditional; T02c2 is deferred rather than passed.

Planning Done: complete named package and synchronized links/traceability. Implementation Done after named approval: C01–C08 actual software evidence plus persisted readout/reopen and no regression in T03e1. Device installation, evaluating any existing private canonical session, new recordings and publication require their own session authorization; software implementation can complete without hardware or private data.

**Decision requested:** approve T04b3 software implementation as specified, including the limited diagnostic ordering amendment, real-policy/origin/result codec support, cooperative same-worker calculation and saved readout. This proposal does not approve FTP/VO2max, smoothing, hidden/screen-off reliability or analysis of earlier private FIT files.


Planning verification: thirteen changed/new Markdown files, 390 relative links including anchors, existing R/N/V/E references, eight unique C01–C08 cases and ten synchronized proposal links PASS. T02c2 explicitly owner-deferred; T04b3 implementation/ordering amendment unapproved. `git diff --check` PASS. No code/build/test/device/private-data operation or publication; local proposal branch only.


Owner explicitly approved T04b3 after reviewing this package. Earlier proposal/unapproved statements are historical; software implementation and its specified ordering/policy/origin amendments are authorized. Installation, private-data analysis and publication remain separate.


## Implementation structure in progress — 2026-11-12

The existing PowerCurveCalculation now supplies the same frozen-revision, one-effect-per-step Job to synthetic and real callers. Legacy synthetic hashing/slot and terminal-v2 with curve-body-v1 remain compatible; real curve bodies use codec v3 with an explicit real acquisition descriptor (outer typed terminal remains v2). Exact live policy ID/5 s, origin/policy metadata, separate real slot and curve-slot validation prevent synthetic relabelling. No new table.

A small LiveCurveController file confines analysis orchestration to the existing runtime's input worker; it is not a Collector, repository implementation or executor. UI emits a coalesced tap and reads a fixed snapshot. A sticky source-state latch notices a new Recording/Paused even before its lifecycle batch commits. Input pump runs before each calculation step, and request/completion keep exact retry state with bounded backoff. AnalysisRepository uses the same live database with the existing cap option. New build version is 0.5.0/code 5; not installed. Full checks are in progress, so no software-complete or hardware claim is made here.


## Software execution and review evidence — 2026-11-12

No device, real canonical database, private FIT or hardware command was used. All app tests use temporary synthetic data, including explicitly test-owned metadata for the real-origin branch. Implemented version is 0.5.0/code 5 locally; installation/publication remain outside this work.

TDD observations: live-policy replay failed with Unsupported lifecycle policy before exact live policy support; the compiling real-origin calculation case failed against the unsupported operation before the shared implementation; real-curve-to-synthetic-slot regression failed because the repository accepted it before origin/slot validation; persistent codec-error regression failed with SAVED instead of FAILED before explicit-retry error latching. All four regressions subsequently pass. No compiler/cache setup error is counted as a behavioral red test. A test-fixture acquisition epoch mismatch was corrected without weakening production validation.

| Case | Executed software evidence |
|---|---|
| C01 | CanonicalPowerReplayTest live exact-five-second timer-before-input/zero/pause table, independent 70 W fractional five-second energy oracle and unknown live ID/timeout refusal pass. Existing gap/loss/UTC/epoch/pause suites rerun against the shared implementation. Real calculation validates exact supported acquisition IDs, cap, epoch, source/normalization and admission policy; synthetic entry refuses real metadata |
| C02 | LivePowerCurveTest six-second 200 W anchor gives 200 W/5 s and unavailable longer values; refreshed 3600 s fixture gives 200 W at all nine durations. Missing-only fixture returns nine unavailable records without zero-fill. Core fractional oracle is 2*100 + 3*50 = 350 J over five seconds, independent of engine traversal |
| C03 | Native SQLite live-shaped InputRepository/AnalysisRepository save/reopen passes. Cooperative job freezes R while the actual coordinator commits one additional out-of-session input between every step, excluding later 999 W values. Long real fixture reaches a 256-effect retained-page high-water without exceeding it; latest active session hides the prior saved result |
| C04 | One hundred coalesced/repeated taps yield one generation; reopening controller/Activity issues none. Abandoned PENDING remains pending without a tap; explicit Retry supersedes and late old completion cannot replace it. Lost request/completion responses retry the same request/generation/terminal. Unknown stored codec remains visibly FAILED even if storage changes until explicit Retry |
| C05 | Actual input writer progresses between hash/replay steps. Sticky received Recording -> Idle cancels before queued controls apply; allocated-job cancellation does not acknowledge pending input. Canonical SQLite failure cancels numerical work, retains the failed result until input storage recovers and suppresses busy analysis wakeups. Existing stalled-write/overflow/loss-only drain regressions also rerun |
| C06 | Real curve-body v3 round-trip, legacy synthetic curve-body v1/typed-terminal-v2/scalar-v1 regressions, unknown origin/ordering/codec rejection and real/synthetic slot ownership pass. Native SQLite FULL on result completion preserves all nine canonical rows and leaves the newest run pending. Live AnalysisRepository open verifies 32 MiB cap; no aggregate membership accepted |
| C07 | LiveCurveRuntimeTest runs actual MainActivity button -> existing worker -> real Karoo adapter with SDK fake -> real SQLite -> saved 200 W/5 s on API 32/34. Activity recreation retains generation and exactly five source consumers. Readout marks diagnostic acquisition limitations and unavailable durations; existing preview/isolation regressions pass in the first full run |
| C08 | First full verification PASS: core 120 passes/one private-FIT skip, debug 156 passes, preview 180 passes, builds/lint and both isolation checks. Added final review regressions pass focused API 32/34 tests; expanded full verification and final cancellation-review results follow below |

The final manual-follow-up runtime/UI test source is a fake SDK host, not device evidence. Five-second receipt hold remains an engineering policy; neither software curves nor the conditional diagnostic label approve physiological models or whole-ride capture. Real and synthetic calculations share the same stepping implementation with distinct origin/hash/cohort; the single input worker owns both repository interfaces in the same capped SQLite database. No duplicate source, Collector, schema or job framework was added.


## Final verification and disposition — 2026-11-12

Expanded full verification passed in 4 min 45 s:

```sh
ANDROID_HOME=/Users/pawelkozicki/Library/Android/sdk ./gradlew -PkarooDiagnostic=true -PkarooSdkJitpack=true spotlessApply :core:check :core:build :app:testDebugUnitTest :app:testPreviewUnitTest :app:assembleDebug :app:assemblePreview :app:lintDebug :app:lintPreview :app:verifyReleaseDependencyIsolation :app:verifyPreviewIsolation --console=plain
```

Results: core check/build PASS (unchanged cached 120 passes/one explicit private-FIT skip, previously executed in this package), full debug 168 PASS, full preview 192 PASS, zero failures/errors. Debug/preview/release artifacts, lint and release/preview isolation PASS. Existing AGP source-directory, SQLiteMode and JDK native-access warnings remain tooling warnings.

Final self-review reproduced one additional compatibility regression: cancelling a synchronous preview hash pass before request allocation retained that job's cancellation callback on a later call. The compiling regression failed on the retry with CancellationException. Clear a pre-request cancelled/failed job, while retaining a frozen phase-1 request for exact storage retry. Regression now passes on API 32/34; numerical traversal, hash format and normal long-fixture path were unchanged.

After that fix, the final affected checks passed in 44 s:

```sh
ANDROID_HOME=/Users/pawelkozicki/Library/Android/sdk ./gradlew -PkarooDiagnostic=true -PkarooSdkJitpack=true spotlessApply :core:check :core:build :app:testDebugUnitTest :app:testPreviewUnitTest --tests '*PowerCurvePersistenceTest*' --tests '*LivePowerCurveTest*' --tests '*LiveCurveRuntimeTest*' :app:assembleDebug :app:assemblePreview :app:lintDebug :app:lintPreview :app:verifyReleaseDependencyIsolation :app:verifyPreviewIsolation --console=plain
```

Final full debug **170 PASS**; final focused preview **54 PASS** covering legacy storage/cancellation, real calculation/controller and Activity/runtime on API 32/34. The immediately preceding full preview **192 PASS** remains the numerical/long-fixture/isolation baseline, not a claimed final 194-test full rerun. Core check/build, format, both APK builds/lint and both isolation checks PASS again. C01–C08 software acceptance is complete with the scope/limits stated above.

Final source review covered sole source/worker ownership, admission gate and writer priority, bounded action/page/job state, frozen requests/completions and backoff, sticky cancellation, fatal-codec error latching, real policy/origin/slot compatibility, 32 MiB cap on both live repository interfaces, newest generation/no fallback, resources/English UI, synthetic release isolation, and all tracked/untracked files. No new dependency/module/schema, FIT access, physiological model, device action or application installation. T02c2 stays owner-deferred; broader hardware/product/scientific gates stay open. No commit/push/PR/merge.


Local prepared debug APK SHA-256: `37db936410c1259207e67c9c5c30dafa16d72998cc52103e54e91a96c6a0ffbd`, path `app/build/outputs/apk/debug/app-debug.apk`, version 0.5.0/code 5, built from fb810cf plus the local T04b3 implementation. It is not a committed release or the currently installed 0.4.0 APK; no device read-back/installation is claimed.


Final documentation/artifact checks: 18 changed/new Markdown files, 425 relative links including anchors, R/N/V/E references, C01–C08 acceptance/evidence and ten synchronized references PASS. Resource XML/references, all five unchanged local skills via installed skill-creator validator, and `git diff --check` PASS. Scope contains 33 application/test/build/document files with no private/raw/generated artifacts staged or tracked. Local branch is feat/t04b3-live-diagnostic-curve; changes remain uncommitted and unpublished.


## Installation/publication authorization and pre-PR review — 2026-11-12

Owner explicitly requested installing this version on Karoo and publishing a PR, with code review before PR creation. Authorization covers in-place same-signer 0.5.0/code 5 installation without data clear, launch/status read-back, and commit/push/PR of the scoped implementation/documents. Earlier no-installation/publication statements describe prior stages. No new native recording, Calculate/Retry action on real stored sessions, FIT access, merge or release is inferred. Private installation/status evidence stays outside Git.

Review reproduced a pending-control selection race: a received Recording -> Idle pair could remain uncommitted while an explicit calculation selected the preceding ended session. A regression exercises the actual coordinator/repositories and requires control acknowledgement before analysis eligibility; telemetry may still remain pending. Fix and verification evidence follow.


Pre-PR review result: the new regression failed on API 32/34 with CALCULATING instead of UNAVAILABLE before the control-acknowledgement fix. Eligibility now requires the latest received control receipt to be acknowledged in the same epoch, while ordinary telemetry may remain pending. The cancellation reason remains visible when a running job is stopped by such controls. After the fix, full debug 172 PASS, focused preview 56 PASS, core check (cached 120 passes/one skip), format, assembleDebug/lintDebug and release/preview isolation PASS in 46 s. Code review covered all changed/new source, tests/build and documentation, including exact retries, origin/slot/codec, worker/gate order, cancellation, storage cap and privacy. No further blocking finding within T04b3. The prior prepared-APK hash above is historical and will be superseded by the reviewed installed APK below.


## Reviewed 0.5.0 installation — 2026-11-12

After code review/fix and passing pre-PR software checks, installed com.oxywatt.diagnostic 0.5.0/code 5 in place on k24/API 32. Verified old installed 0.4.0 hash against prior E10, verified the prepared APK signature and equality of its signer with the installed package, then ran selected-device `adb install -r app/build/outputs/apk/debug/app-debug.apk`: Success. No data clear/downgrade/uninstall, pairing/profile setting, native recording or Calculate/Retry action.

Pulled the public installed APK and verified exact SHA-256 equality with the reviewed prepared APK: `185fa455a23c7c8843a871e8e77d43117f624d16949b7b30e2b6823c6249796d`. Device package labels are 0.5.0/code 5; launch returned Status: ok. This binary is **fb810cf plus the reviewed local T04b3 implementation**, not the future PR commit SHA. The earlier local prepared hash 37db9364 is superseded for this installation.

Actual screen/timing-status read-back: Connected/Idle, source running, canonical commit acknowledged; display footer 0.5.0 / fb810cf + local changes, diagnostic acquisition limitation and Ready to calculate the latest locally ended session. Existing main database filename remains present before/after `-r`; no row-by-row private-input analysis is claimed. The screen was initially asleep and was awakened for readout; no controlled screen-off/residency test. No curve was calculated, so this is install/launch/readout evidence, not real numerical curve acceptance or T02c2 completion. A final optional additional button screenshot was unavailable after a transient ADB command failure; the installed hash, launch, footer and Ready read-back had already succeeded. No reconnect test or new native ride was attempted.

Private evidence root: `~/Library/Caches/oxywatt/device-evidence/karoo-install-0.5.0/`, owner-only directory/files. Retains public APK copies/signature/package/hash checks, private timing-status tails/screenshots, database file metadata and installation checker/manifest. No raw artifact/profile/report/identifier/acquisition timestamp is committed or published. Earlier device evidence and FITs remain untouched. Owner authorization now covers the pending commit/push/PR only; merge/release, extra recordings and real Calculate remain outside scope.


Final pre-publication closure: full preview rerun on the reviewed source completed in 4 min 18 s with **196 PASS**, zero failures/errors, and lintPreview PASS (`:app:testPreviewUnitTest :app:lintPreview` with the established opt-ins). This supersedes the earlier focused-only preview result for current-source validation. Current final evidence: full debug **172 PASS**, full preview **196 PASS**, unchanged cached core 120 passes/one private-FIT skip, format/build/lint/release-preview isolation PASS. Eighteen changed/new Markdown files, 430 relative links/anchors, R/N/V/E/C traceability/resources/privacy and whitespace checks PASS. Only the 33 reviewed application/test/build/document files are included in publication. Earlier uncommitted/unpublished statements describe prior stages; owner-authorized commit/push/PR follows this review, with merge excluded.
