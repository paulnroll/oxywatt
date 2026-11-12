# T02c1 — Standstill live diagnostics and bounded durable report

Status: **Owner approved the described standstill-first diagnostic increment on 2026-11-12**, explicitly deferring a complete workout and accepting later fixes. ID checked unused before reservation. Scope was recorded before code; implementation and P01–P06 software/short-device evidence are complete for review. No repeated implementation approval needed. Parent T02c sustained acquisition/resource matrix and T03e live Collector/storage integration remain open. No Git publication authorized.

## Goal, boundary and dependencies

Provide an inspectable live power/HR/cadence screen plus a durable diagnostic timing/status report on the existing `com.oxywatt.diagnostic` host, verifiable in a short idle/connected-sensor check. Use the already approved/current KarooRideDataSource, not another source adapter, Collector, database or UI framework. Existing SDK 1.1.9 mapping and conservative canonical time/missing contracts remain unchanged. Dependencies: [T02b](t02b-karoo-probe.md), [T02d](t02d-karoo-adapter-tests.md), [ADR 0003](../../architecture/adr/0003-collection-feasibility-gate.md) and [validation](../../algorithms/validation-strategy.md).

The earlier idea of using Collector/persistence is deliberately deferred: this slice records diagnostic metadata only and does not wire T03e, session identity, power curves or physiological estimates. Owner's training/USB participation is not required for software tests; optional short on-device check is installation/readout evidence only. No full-workout, four-hour, battery/kill or native recording required or claimed.

## Behavior and contracts

- Service owns the existing source. Activity reads an application-scoped bounded latest-state snapshot, refreshed at most twice per second; opening/recreating/closing it does not register another source or terminate capture. Waiting for host/source is visible; do not force a fake recording, FIT write, foreground keep-alive or sensor scan.
- Show actual current received W/bpm/rpm, reason for missing values and local receipt age. Zero W/rpm are real zero; explicit missing clears the old value. Silent age remains visible without inventing physiological TTL/stability. Source stop/failure clears live numbers; after process recreation, historical report data never become current readings.
- Show recording state, host connection, per-channel callback counts, report status/bytes and lost diagnostic-event count. Labels distinguish live readings from the metadata report; numeric readings are displayed only, not retained in the report.
- Durable report is a private append-only UTF-8 JSON-lines file in app files. Include report format/code revision, process-run UUID, local UTC/monotonic receipt meaning, connection/recording/FIT/source lifecycle events, and coalesced channel status/count/last-receipt/preceding-interval summaries. No numeric sensor values, rider profile, sensor/device IDs, GPS or training FIT contents. A process-run ID is not a native ride/session ID.
- Persistence runs on one background writer, at most once per second for dirty state; SDK/UI callbacks do no file I/O and no per-sample writer task/queue. Retain only three metric snapshots and at most 128 lifecycle/status events. Overflow increments a visible/recorded diagnostic-event-loss counter; no claim that those events were retained.
- One report file capped at 8 MiB; reaching the cap visibly stops report writing and preserves the existing file. File append followed by flush/fsync must complete before saved status/count advances. I/O error visibly marks unavailable report; latest readings may continue, never imply saved. Incomplete existing final line is detected and marked incomplete without silently repairing/deleting the file. These are diagnostic engineering caps/policies, not physiological N or product retention defaults.
- New process creates a new report-run marker, with prior continuity unknown and current numbers empty. Historical metadata stay retained; no automatic ride merge or resumption. Last unsynced interval can be lost on process death; do not promise continuous capture or transactional canonical persistence.
- Keep UI strings in English resources, with explicit “readings shown live; report stores timing/status” text. Keep the existing preview package's service/launcher disabled and synthetic path isolated. Update diagnostic version name/code to 0.3.0/3 so this binary is visibly distinct from the old 0.2.0 builds. No new permission/dependency, schema, module or SDK pin.

## Scope/files and verification

Allowed application files: existing MainActivity/DiagnosticExtension, new small DiagnosticCapture/DiagnosticRuntime helpers, strings resources, app version fields, and focused app tests. Existing core, adapter mapping, InputRepository/AnalysisRepository, manifests, preview source/database and scientific methods remain unchanged. Documentation: this task, T02b cross-link, Canvas, decision log, implementation-plan, agent-work-packages, validation and SDK feasibility evidence as needed. Preserve earlier local device/FIT evidence. No publishing or native ride/pairing/force-stop action implied.

P01–P06 are task-local cases, not new top-level V IDs:

| Case | Observable check | Mapping |
|---|---|---|
| P01 | Real received zero/missing/readings/counts and receipt ages render truthfully; source stop/reset removes current numbers, no physiological freshness inference | R02/R07/R08 → V02/report-only V10 |
| P02 | Actual private file round-trip contains timing/status/code/run/events but no numeric readings/profile/GPS; reopen retains report and empty current values with new run identity | R07/R10 → V09/V12 diagnostics only |
| P03 | Three fixed metric slots and 128-event ceiling, visible/event-reported overflow, 8 MiB file limit, storage failure and incomplete tail never claim saved; fsync before saved state | R07/R10/R11 → V09/V12/software V13, not full V17 canonical pipeline |
| P04 | Activity recreate/reopen leaves one service-owned source and same current model; report persists independent of screen; missing/failed/waiting states and English resources verified | R02/R09/R11 → partial V11/V23-style diagnostic display only |
| P05 | Existing SDK-fake and relevant app tests, build/lint and synthetic preview/release-isolation regressions pass; no live/synthetic path mixing | R02/R07/R10/R11 → V02/V09/V12/V20/V22 software only |
| P06 | Optional identified-device install/current binary/readout/private report short check plus actual commands/results, limitations, links/traceability/whitespace recorded | R02/R09/R10 → setup/partial V02/V11/V12 only |

Behavior-driven tests precede implementation for P01–P03; real temporary files verify durable/report limits. UI/declarative wiring uses Robolectric/device smoke and build/lint. No new synthetic physiology series, training required, calibrated physiological confidence or full parent acceptance. Finish with inspectable short-check behavior and evidence; future long-run tests remain open instead of blocking this diagnostic increment.


## Execution evidence — 2026-11-12

Implementation is complete for this diagnostic slice, with short device verification; parent sustained/live/scientific gates remain open. Existing local T02b installation/trainer/FIT evidence is preserved. No full workout, native recording or pairing was requested/performed in this increment.

Actual structure: DiagnosticCapture keeps three metric slots plus bounded lifecycle events and serializes timing/status to one private JSON-lines file. DiagnosticRuntime owns one process-scoped background writer; DiagnosticExtension retains sole source ownership and supplies canonical readings, connection and FIT lifecycle events. MainActivity reads the shared model every 500 ms and never starts another source. Source stop clears current values and late readings stay rejected until an explicit new source start. Profile observations are ignored by this diagnostic consumer. Numeric readings never enter the report. Core/adapter/Collector/SQLite/manifest/preview source contracts remain unchanged.

| Case | Actual evidence |
|---|---|
| P01 | API 32/34 tests verify zero, explicit missing, age/callback count and late-source-stop behavior. Device screenshot shows actual source Power 0 W/Cadence 0 rpm and HR unavailable, not invented placeholders |
| P02 | Real temporary-file append/reopen test retains report and new process marker while current values stay empty; unusual reading and profile values excluded. Device private report read back and parsed successfully: timing/status only |
| P03 | Real file limit, storage failure and incomplete-tail refusal tests PASS. Event overflow is visible; slow-writer concurrency regression verifies 128-event capacity includes queued plus in-flight events. Saved state advances after flush/fsync |
| P04 | Robolectric Activity recreate retains same model/value/count and closing Activity does not stop capture. Actual HOME/reopen on Karoo keeps one process-run/source-start marker; connected/Idle and saved report state visible in screenshot |
| P05 | Full debug 106 tests PASS; full preview 130 tests PASS; core check PASS using unchanged cached results (117 passed/one private-FIT skip). Build/lint and release/preview isolation PASS |
| P06 | Identified k24/API 32 updated in place to 0.3.0/code 3 with matching signer, installed APK hash verified and launch PASS. Private short-check artifacts retained; no native ride start, sensor calibration or full-session claim |

TDD evidence: initial zero/missing test failed against an empty model, then passed after received-state implementation. File persistence test failed against a no-op flush; limit/error/overflow cases failed against stubs, then passed with real private-file handling. A later slow-writer test failed because events in the active write did not count against capacity; corrected to share one queued/in-flight ceiling, and all focused/full tests passed. No compiler/setup failure counted as a behavioral red test.

Actual verification command: `ANDROID_HOME=/Users/pawelkozicki/Library/Android/sdk ./gradlew -PkarooDiagnostic=true -PkarooSdkJitpack=true spotlessApply :core:check :app:testDebugUnitTest :app:testPreviewUnitTest :app:assembleDebug :app:lintDebug :app:verifyReleaseDependencyIsolation :app:verifyPreviewIsolation --console=plain`: PASS in 4 min 18 s. New diagnostic classes contribute 14 executions per variant (six model tests plus one Activity test, each on API 32/34). Full core has 118 tests/one skip and was UP-TO-DATE; it is inherited numerical evidence, not a new physiological/source replay. Existing AGP source-directory, SQLiteMode and JDK native-access warnings are tooling notices; no product workaround introduced.

Installed debug APK SHA-256 `8c989b005eec8ef05b6e4eed0f2989a0d334d59585dc166d010f61c612b5470c`, based on Git cf12e69 plus this local working-tree implementation, not a committed release. Matching old/new signing certificate verified before `adb -s DEVICE install -r app/build/outputs/apk/debug/app-debug.apk`; device APK SHA and version code/name verified afterward. Launch returned `Status: ok`. No application data was cleared and separate preview/native Karoo FIT storage was not modified.

Private short-check evidence root: `/Users/pawelkozicki/Library/Caches/oxywatt/device-evidence/karoo-standstill-0.3.0/`. `install.json`, `smoke.json`, `diagnostic-report.jsonl` and `screen.png` remain outside Git with owner-only access. The captured report has 159 complete JSON lines, one process-run and one source-start marker, connected/Idle status, no numeric values/profile/GPS; snapshot includes actual callback counts and presence flags. Report SHA-256 `a031e221d4f8955e2d9fe655f2ad491b5651586ad220d1e1ec1c65a9b7fdaa06`; screenshot SHA-256 `541ce255a169a90e0306805c7b7f7c1718301deaa1cc8d74ead52bf155fcbfaf`. Capture is a diagnostic snapshot, not a complete ride/history. Screen was initially asleep; it was awakened for verification. UIAutomator exposed only SystemUI nodes and is not used as readout proof; the direct screenshot was visually inspected and shows the actual app UI/report state. This is not a controlled screen-off residency test.

Observed zero W/rpm and unavailable HR verify display/domain handling only; no numeric sensor accuracy, physiological validity, prolonged hidden/no-field operation, force-stop/low-memory recovery, battery/memory budget or complete parent V11/V13 claimed. No real canonical observations are persisted here: report stores timing/status, T03e remains separate. No commit/push/PR/merge performed.


## Final self-review and documentation checks — 2026-11-12

Reviewed complete application/document diff, new tests and earlier local device/FIT evidence for scope, privacy and requirement mapping. New app code does no profile/GPS retention, canonical/analysis/history write, UI-owned subscription or unbounded per-sample scheduling. Receipt age/missing/zero/source-stop, file cap/error/incomplete tail and queued-plus-in-flight event bounds remain explicit. Version 0.3.0 is already installed; working tree is local and not published.

`/tmp/oxywatt-check-t02c1-docs.py`: eight affected/new Markdown files, 287 relative links including anchors resolve; all referenced R/V IDs resolve, P01–P06 contract/evidence and synchronized task/backlog/work-package/SDK-feasibility links checked. The initial checker inherited an irrelevant scientific-ledger synchronization requirement from T08; adjusted it to the applicable SDK-feasibility document. No scientific content was changed to satisfy that tool mismatch. English resource references (33), XML parsing, new-file whitespace and `git diff --check` PASS. Skills unchanged. No full-workout/device resource or physiological acceptance claimed, no commit/push/PR/merge.


## Owner feedback and publication authorization — 2026-11-12

Owner confirmed seeing the installed 0.3.0 screen working with actual sensor readings. This is owner-observed readout feedback, separate from the recorded zero/missing standstill screenshot and from independent numerical calibration. Parent sustained/lifecycle/resource and physiological gates remain open.

Owner explicitly requested a PR with this implementation and a next-session continuation prompt. Publication covers the 16 scoped application/test/document files, including earlier local T02b update/trainer/FIT aggregate evidence. Commit/push/PR authorized; merge, further implementation, source acquisition and raw evidence redistribution are not. Native owner FIT, private log/report/screenshot files, device identifiers, exact ride timestamps and generated APKs remain outside Git.

Pre-publication review covers the full diff including new capture/runtime/tests, source ownership, zero/missing/age, limits including in-flight events, real file failure/incomplete-tail behavior, private data exclusion, versioning, synthetic preview/release isolation and synchronized requirement/task/test evidence. Previously accepted complete build/test/lint/isolation results remain applicable; only aggregate documentation changed afterward, so no additional application tests are necessary. Local documentation links/traceability/whitespace checked before staging; remote PR/head/CI are read back after publication and not assumed passed.
