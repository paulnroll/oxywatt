# T03p — On-device diagnostic preview

Status: owner-requested planning on 2026-11-11 after PR #12 merged. The owner accepted the offer to plan a small diagnostic screen after T03a–d. This document defines that package; it does not authorize T03a–d implementation, live T03e wiring, installation or a completed device check.

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

Done requires the integrated software evidence above and a recorded on-device preview for the claim “seen on Karoo”. A build or Robolectric screenshot alone does not close that claim. Device access, T03 repository contracts and portable FIT-reader support are explicit readiness dependencies. No screenshots or new application code exist for this task yet; no T02 whole-ride, release or science gate is closed.

Planning verification: 129 relative Markdown links resolve, T03p/V23 traceability and unique V23 declaration were checked, existing skill YAML frontmatter parses, and `git diff --check` passes. Only specification files changed; no application build, screenshot, installation or preview test was performed.
