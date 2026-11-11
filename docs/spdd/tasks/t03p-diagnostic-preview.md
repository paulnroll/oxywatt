# T03p — On-device diagnostic preview

Status: preview observed on Karoo on 2026-11-11 after PR #18; scoped device evidence and open limits are recorded [here](../../evidence/t03p-karoo-2026-11-11/README.md). Implementation authorized on 2026-11-11 after PR #17 merged; software evidence below supersedes the planning-only status. Original planning followed PR #12. The owner accepted the offer to plan a small diagnostic screen after T03a–d. This document defines that package; it does not authorize T03a–d implementation, live T03e wiring, installation or a completed device check.

## Problem and goal

T03a–d establish collection/storage components but do not expose them on Karoo. Provide an inspectable debug screen fed by those actual components, so the owner can see recording, missing channels, committed inputs and interrupted recovery before the live acquisition gate closes. R02/R03/R07/R10/R11; V03–V05/V09/V12/V17–V19/V22 and new V23. This is a diagnostic preview, not the R08 history screen or T04 power curve.

## Dependencies and order

T03a lifecycle/recovery → T03b bounded admission and T03c real transactional input repository → T03d generation storage → **T03p preview** → T03e live integration → T04. T03b/T03c retain their existing contract dependencies. T02c hardware feasibility can proceed independently; its passed acquisition decision remains mandatory for T03e. T03p uses completed T01d/T01e simulation contracts and T02d Android build evidence.

Before implementation, finalize the T03a snapshot/query and T03c repository contracts in their tasks. The preview consumes those contracts rather than inventing a second Collector, checkpoint writer or recovery state machine. T03d completion does not imply that any physiological estimator exists.

## Screen and controls

Use a dedicated `preview` build type initialized from debug, with application ID suffix `.preview` and a small native Android layout. Its manifest selects PreviewActivity as the launcher and disables the Karoo-discovered DiagnosticExtension service and original launcher. The existing debug diagnostic host stays available for live probes. No new UI framework or module is required. A proposed English layout (all strings in resources; English is the default and only planned UI language):

```text
OxyWatt — Diagnostics
DEMO · Test scenario

Session: Collecting / Paused / Interrupted
Power           180 W       0.2 s ago
Heart rate      —           Unavailable
Cadence         85 rpm      0.4 s ago

Saved events: 124
Pending events: 3
Gaps / lost events: 1 / 5
Last commit: 0.8 s ago

[Start demo]  [Stop]
```

- Keep the DEMO or FIT REPLAY banner visible throughout a run and when viewing its stored result. Replay also shows EOF/stopped separately from recording state. EOF during Recording is not a native ride end or a successful save.
- Show Collector state independently of source run state and storage health. Cover empty, running, paused, source-finished, interrupted/ambiguous, missing-channel and storage-error states; show the latest bounded reason rather than a growing log.
- Latest power/HR/cadence are **received observations**, not proof of commit. Zero power/cadence render as zero; missing values render a dash plus reason. On an explicit missing event, remove the old numeric reading. Silent age remains visible rather than implying a fresh value. Do not invent a physiological freshness threshold.
- Compute age from the applicable acquisition clock within an epoch. On replay, identify virtual activity time separately from playback/wall time; across epochs show age unavailable. Pauses or changing playback speed must not change stored activity durations.
- “Saved events” counts committed canonical envelopes in this demo session (including recording/profile/missing-value events), not received callbacks, queued inputs or completed sensor samples. Last commit time comes from the repository; display pending/admission loss separately. A failed transaction never increments committed count.
- Start one fixed safe scenario; reject duplicate start while it is running. Include zero power, a missing HR channel, pause/resume and a silent gap. Add one bundled synthetic FIT projection verified by the existing pinned exporter. Stop requests source cancellation and preserves committed data with honest partial-session status. Do not synthesize Idle or merge restarted fragments.
- Keep state/readout updates coalesced, at most twice per second, with constant retained UI state. A repository read or slow screen must not block ingestion or allocate a per-event UI queue. No raw input writes, decoding, pumping or database queries on the main thread.

## Data flow and source isolation

Use the real T03a/b/c path: simulated RideDataSource → Collector/admission → transactional demo repository → bounded read-only screen projection. An application-scoped debug run owner controls one worker, source, virtual clock and repository; the Activity only subscribes and sends explicit start/stop requests. Rotation/navigation must neither duplicate subscriptions nor stop or restart the run. No promise of process survival: after process recreation, T03 recovery marks the unknown interval and does not automatically resume the old source.

Preview orchestration is limited to demo inputs and is part of T03p; it does not activate the production Karoo adapter/queue/storage coordinator owned by T03e. Demo launch must not also collect real sensors through the existing DiagnosticExtension. The preview manifest disables that service so opening or recording a native ride cannot silently enable live acquisition in the preview package. Keep the original diagnostic package independent, and route demo inputs exclusively to a separately named demo database and origin-tagged run manifest. Demo data can never enter real history/trends. Test that separation even if the production history database is not implemented yet.

The simulator currently lives in core JVM tests. Reuse its generator/clock semantics through a minimal shared test-fixture source set, consumed only by core tests and app preview code. Never move simulation controls into core main or app release. Extract dependency-free support without test assertions; do not copy scenario definitions into the UI. Before reuse, check Android compatibility of helpers: the current FIT bridge uses JVM Files.readString/Path APIs and cannot be assumed portable. Share validated parsing over streams where needed, with Android preview assets supplying the input; preserve hash/count/end/timer checks and existing core replay tests. Do not add runtime Python/FIT decoding or a general product file picker.

Only generated/synthetic projections are bundled. A private owner FIT or its derived projection is not an APK asset or committed fixture. Owner-private on-device replay requires a separately agreed local provisioning/removal procedure. This package promises a synthetic FIT demonstration, not automatic access to personal ride files.

## Expected files and bounded scope

- `app/src/preview/kotlin/com/oxywatt/diagnostic/preview/`: run owner, readout and PreviewActivity; preview manifest/resources/assets.
- Preview manifest overlays disable the original launcher/extension only in the preview package; retain debug/release diagnostic manifests and component behavior.
- `core/src/testFixtures/` for minimal reusable simulator/stream-reader support, existing core simulation tests and synthetic resources; `core/build.gradle.kts` and `app/build.gradle.kts` only for scoped fixture/preview dependencies and release-isolation checks.
- App tests for projection, real repository/run wiring and screen lifecycle; existing opt-in Android CI where appropriate.
- This task, Canvas, backlog, validation strategy and decision log with actual evidence.

Exclude power curves, FTP/VO₂max numbers, trend/history UI, live radio startup, fake appstore installation, file import UX and release publication. Require named approval of T03p implementation after its T03 dependencies and concrete contracts exist.

## Verification and Done evidence

V23 is the named preview acceptance scenario:

1. Run a fixed independent event table through the actual demo Collector/repository and read the screen projection. Assert exact received values/reasons, committed event counts, pause/gap/interruption state, commit failures and bounded pending/loss counters. The screen must never label queued data as saved.
2. At 1x and 10x compare persisted canonical payload/timing/session outcomes for the same scenario; compare against existing V18 oracles while accounting for run-local IDs. The visible virtual age and replay labels follow the acquisition clock, not accelerated wall time.
3. Recreate/close/reopen the Activity during recording: one run remains active, its session is unchanged, and one input is stored once. Stop and process-recovery scenarios use the T03 interruption policy, never automatic continuation/merging.
4. Inject storage failure and admission overload through the real repository/admission path. Show errors/loss without fabricated values, unbounded UI backlog or main-thread I/O. Do not add a user-facing fault-control panel.
5. Assert separate demo database/origin manifests and zero writes to a real-history repository spy. Inspect release dependency graph and APK to exclude simulator controls, fixtures, fake host classes and bundled replay assets.
6. Run core checks, opt-in Android tests/build/lint and release-isolation checks established by implementation. Build checks must verify the preview manifest disables live discovery and release omits demo controls/assets. Commands for the new variant include `:app:testPreviewUnitTest :app:assemblePreview :app:lintPreview` once those tasks exist. On Karoo, only after the target device is identified and setup is authorized, install the debug artifact and record screenshots of running, pause, missing-channel and persisted/error states. Record OS/host/SDK/app versions and exact synthetic scenario manifest. Emulator screenshots prove emulator behavior only.

Done requires the integrated software evidence above and a recorded on-device preview for the claim “seen on Karoo”. A build or Robolectric screenshot alone does not close that claim. Device access, T03 repository contracts and portable FIT-reader support are explicit readiness dependencies. At the planning baseline no screenshots or new application code existed; no T02 whole-ride, release or science gate is closed.

Planning verification: 129 relative Markdown links resolve, T03p/V23 traceability and unique V23 declaration were checked, existing skill YAML frontmatter parses, and `git diff --check` passes. Only specification files changed; no application build, screenshot, installation or preview test was performed.


T03p implementation authorized by the owner on 2026-11-11 after PR #17. Readiness: T03a–d contracts are implemented. Preview uses a dedicated application-scoped worker, separate demo database, bounded latest-value projection, and shared dependency-free simulator fixtures. Portable stream replay preserves exporter validation. No live acquisition, physiology or hardware acceptance is included. R02/R03/R07/R10/R11; V22/V23.


## Implemented contract and software evidence — 2026-11-11

The owner approved T03p in this session. T03a–d contracts were read before code; no new Collector, schema or live coordinator was introduced. Dedicated `preview` build type inherits debug, uses `.preview`, disables both live extension discovery and the original launcher, and adds a native resource-based English Activity. `PreviewRun` owns one worker and immutable coalesced screen snapshot; Activity recreation/navigation only changes the UI refresh subscription. Start rejects a second concurrent run. Stop cancels playback; EOF/stop persist interruption, never Idle or native save. A new worker epoch recovers the prior committed checkpoint conservatively; no automatic source continuation or merge.

`PreviewPipeline` calls real BoundedAdmission, CollectorLifecycle and InputRepository. Received channel observations and committed channel values/counts are separate. Missing readings remove numeric values; zero remains present. Admission pending/loss and latest bounded exception type remain visible. Committed canonical counts include controls, excluding observations outside a session. Coverage breaks include pause/missing/silence/interruption. Bounded paged reads run on the worker; the UI retains one string and at most three channel observations. Failed batch and standalone-effect commits retain frozen writes for exact retry. Restart can discard an uncommitted tail; recovery does not claim continuity.

Readout refinement: the repository has no durable wall-clock commit timestamp. The screen explicitly labels **Last committed receipt virtual time**, read from its committed checkpoint, instead of fabricating a transaction time. Virtual activity time/age is separate from wall playback; no physiological freshness threshold is added. Restart shows no age for old received observations. Snapshot publication and Activity refresh each run at most twice per second (terminal snapshot may publish immediately).

Simulator support is extracted to `core/src/testFixtures`; core main remains source-neutral and simulation-free. Existing JVM Path entry point delegates to shared `replayFitStream`, which validates hashes, row counts, original file-time offsets, end and timer semantics using reopenable streams. Android assets are build-time copies of exactly the existing generated synthetic manifest/projection, never a private input. Per-epoch origin-tagged manifests retain generator/scheduler/normalization versions, seed, epoch, hash and pinned FIT exporter metadata in a separate preference store. Demo repository names are restricted to `oxywatt-synthetic-*.db`; real history cannot be selected. Release dependency and APK inspection excludes simulator classes, preview Activity, fake host marker and FIT assets. The opt-in Android CI command now includes preview checks; no remote dispatch is claimed.

TDD evidence: compiling initial pipeline stub failed the committed-count assertion; real transaction wiring made it pass. The database-name isolation test failed before the name guard. Reopen recovery failed the committed-count assertion before durable interruption/readout initialization. Self-review's standalone-stop retry test failed because interruption was not retained, then passed after retaining its frozen write. Compiler/Gradle setup errors are not behavioral reds. Additional integration regression coverage exercises exact retry after SQLITE_FULL injection, bounded overload (10,000 inputs, 2 retained), fixed scenario and synthetic FIT at 1x/10x, missing/zero separation, and Activity recreation/navigation with duplicate-start rejection. These additional coverage cases are not claimed as individually observed red tests.

No Karoo installation, screenshot, hardware test, physical disk-full test or process-survival measurement was performed. Robolectric software acceptance does not close T02/T03e, N02 or any physiology gate. “Seen on Karoo” remains open. No private FIT/projection was read or bundled.


Final verification passed: `ANDROID_HOME=/Users/pawelkozicki/Library/Android/sdk ./gradlew -PkarooDiagnostic=true -PkarooSdkJitpack=true spotlessApply :core:check :core:build :app:testDebugUnitTest :app:testPreviewUnitTest :app:assembleDebug :app:assemblePreview :app:lintDebug :app:lintPreview :app:verifyReleaseDependencyIsolation :app:verifyPreviewIsolation --console=plain`. Core: 68 passed, one explicit private-FIT skip. Debug: 70 passed. Preview: 79 passed (70 inherited plus 9 preview cases), zero errors/failures/skips. Preview cases use Robolectric API 32; inherited persistence/adapter checks also cover API 34. Debug/preview lint and builds, release assembly/APK isolation passed. Existing tooling native-access/deprecation warnings remain; no hardware evidence follows. Self-review covered complete tracked/untracked scope, worker/UI separation, retry/recovery, provenance, asset whitelist and release graph. All 132 relative Markdown links resolve; five project skills pass the installed validator; R02/R03/R07/R10/R11 and V22/V23 traceability and `git diff --check` pass.


## Device-verification readiness — 2026-11-11, after PR #18

The owner authorized installing and launching **only** `com.oxywatt.diagnostic.preview` on an identified Karoo, synthetic testing, screenshots and evidence publication. This does not authorize T03e, full T02c acceptance or closing the T02 hardware gate. The clean local main was fast-forwarded to merge `cd89e6e`; preparation uses a separate branch.

ADB at `/Users/pawelkozicki/Library/Android/sdk/platform-tools/adb` responds, but repeated `adb devices -l` checks returned an empty device list. **No target has been identified, no APK installed, no device test or screenshot performed in this session.** Current device Android/host/installed-app versions remain unknown; the historical T02a versions are not a current observation. The owner has been asked to connect and authorize the Karoo.

The full verification command recorded above passed again on JDK 25 / SDK platform 36 / Build Tools 36.0.0, with 170 tasks (10 executed, 160 up-to-date). Tests were up-to-date, not freshly re-executed: existing XML reports contain core 68 passes plus one explicit private-FIT skip, debug 70 passes and preview 79 passes. Both lint targets, builds and isolation checks succeeded. SDK selection remains the explicit JitPack 1.1.9 coordinate. APK badging independently confirms `.preview`, version 0.2.0/code 2, min SDK 23 and target 34. Preview APK SHA-256: `b580cce3886651a847b308327ac41b096173faeb9fa29ed4a1e910a0e6d2d908`. The merged manifest disables MainActivity and DiagnosticExtension; runtime absence of live acquisition still requires the device check. Debug/release artifacts produced by the requested verification command are not installation targets.

Source-inspected controls and limits, **not hardware results**:

- Start demo, Start synthetic FIT replay and Stop are the only buttons. Playback is fixed at 1x. Pause/resume are scripted input events; no manual pause, speed or fault-injection panel exists.
- Demo runs for 10 virtual seconds: Idle/Recording at 0, zero power and cadence 85 at 1, unavailable HR at 2, pause at 3, resume at 5, power 180 at 6, then silence to EOF. EOF does not emit Idle. The initial Idle is outside the recorded session.
- FIT uses the existing `generated-synthetic-v1` asset: 15 projected rows, 9 virtual seconds, pause at 3 and resume at 8. Zero power/cadence and invalid/missing HR are explicit. Projection SHA-256 is `470108c641e76ca8f121bc644cdcfeeac8a33325e57b9d0439da0fdb48e57779`; pinned decoder is fitdecode 0.11.0. No private FIT/projection is used.
- Demo and FIT use separate `oxywatt-synthetic-demo.db` and `oxywatt-synthetic-fit-demo.db` stores. Process recreation opens the demo store by default, even if FIT was last selected. Automatic restoration of the last FIT screen is not implemented; do not infer FIT recovery from the demo recovery screen.
- UI publication/refresh is coalesced at 500 ms. Pending may be transient; nonzero loss and storage failure cannot be deliberately produced through existing controls. Leave their hardware cases open unless actually observed; Robolectric fault evidence remains separate.
- The source label combines EOF and Stop as “EOF / stopped — partial session”; distinguish the test action in evidence. Last committed receipt virtual time is not a durable database transaction wall timestamp. No physiological freshness policy exists.

Pending device procedure: identify model and OS/host versions without publishing serials; inspect existing packages without clearing data; install only the preview APK; confirm disabled live service and no preview live acquisition. Capture actual demo/FIT running, pause, missing/zero, aging/silence and terminal states, comparing received readout with committed synthetic SQLite rows. Recreate Activity and separately close/reopen it while verifying one source/session. Exercise Stop, EOF and subsequent starts without complete-session fabrication or merging. Finally terminate the real preview process during a run, confirm it exited, reopen and inspect conservative interrupted recovery and epoch/session separation. A force-stop is distinct from low-memory process death and Activity recreation; record the exact mechanism. Preserve original host/data, publish only reviewed synthetic evidence, and keep infeasible cases open.


## On-Karoo verification after connection — 2026-11-11

The owner connected the Karoo after the readiness checks above. That earlier missing-device status is superseded by [identified-device evidence, screenshots and open limits](../../evidence/t03p-karoo-2026-11-11/README.md). Only preview and its test companion were installed; original host/data were preserved. Demo and corrected bundled FIT replay, received versus committed SQLite values, zero/missing, virtual age/silence, scripted pause/resume, pending, Stop/EOF, Activity recreation/reopen and conservative force-stop recovery were actually exercised. No low-memory termination, induced admission loss or physical storage-full acceptance is claimed.

A real FIT launch exposed Android API 32 offset-timestamp parsing failure hidden by host-JVM Robolectric. A new dependency-free device instrumentation acceptance failed behaviorally before changing file timestamp parsing to OffsetDateTime.toInstant; the same acceptance then passed with 15 committed events/INTERRUPTED. The lifecycle device acceptance also passed with a distinct recreated Activity and seven committed demo events. These two custom instrumentation runs are separate from the existing 79 preview unit executions. Runner wiring is preview-only and retains both debug/preview unit variants; no new app feature or live coordinator was added.

Screenshots and SQLite inspection establish “seen on Karoo” only for the documented states/build. Source/storage fault forcing, 10x playback, low-memory death, last-selected-FIT restoration, banner visibility while scrolling and early-run committed-projection ambiguity remain explicit limitations. T02/T03e and science gates stay open.

Final follow-up checks: full owner command plus preview instrumentation assembly passed; affected software tests retain core 68 passes/one private skip, debug 70 and preview 79. Two separate real-device instrumentation scenarios passed. All 152 relative documentation links resolve and `git diff --check` passes. See the evidence report for exact scope, APK hash and preserved open cases.
