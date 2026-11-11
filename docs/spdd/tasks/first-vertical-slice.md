# First vertical slice — Karoo → Collector → Persistence → Power Curve

Status: T01a and T01b are complete. See [T01a evidence](t01a-canonical-input.md) and [T01b evidence](t01b-synthetic-replay.md). Full vertical slice NOT APPROVED. Task IDs T01–T04. Read Canvas v0.2 and ADRs 0001–0003 first.

Goal: a real recording session on Karoo produces durable source-neutral inputs and correctly observed power-duration records, independent of visible fields.

Scope: minimal Gradle `:core` and `:app`; canonical observations; profile/recording adapter; lifecycle probe; local repository; bounded checkpoints; numerical power curve; simple current-session/result readout. Developer replay fixtures support validation. No FTP/VO₂max formula, product FIT import, elaborate chart, external service or publication.

Dependencies: explicit architecture/slice approval; actual device/ADB access supplied through authorized tooling; package credentials available privately; selected compatible Android/JDK toolchain. These are prerequisites, not assumed present.

Expected paths after approval: `settings.gradle.kts`, Gradle wrapper/version configuration; `core/src/main/kotlin/.../{collection,power,ports}` and JVM tests; `app/src/main/.../{karoo,persistence,ui}` plus manifest/extension XML; `docs/discovery/karoo-hardware-validation.md` created only when real evidence exists. Final file names follow chosen namespace; no placeholders are created now.

Acceptance:

1. T01 maps live/file semantics into one contract; V01–V03 fixtures pass independently of Android.
2. T02 proves or rejects source timing/startup/lifecycle assumptions on the actual Karoo build. Record four-hour coverage, field visibility, screen/app state, pause/reconnect, process kill and force-stop separately. If host residency fails, stop before T03's production acquisition design.
3. T03 commits bounded data/checkpoint atomically, survives restart with explicit gaps/ambiguity, never duplicates local sequence keys. Storage failure is visible; enforce N01 across queues and in-flight writes, including reserved control capacity and fail-closed overflow recovery. Result-generation persistence cannot reactivate superseded runs. V04/V05/V09/V12/V14/V16/V17 pass (V16 may use repository fixtures before physiological models exist).
4. T04 time-weighted maxima match independent integrals, exclude gaps and persist provenance. A recorded device session reaches the readout without requiring our field to remain visible. V06/V13 pass; memory/battery budgets measured.

Risks: SDK has no sensor timestamp/durable ride ID; host binding unproven; telemetry-free hardware diagnostics still contain sensitive physiological data. Keep traces local and report summaries only.

Definition of Done: requirement/test evidence attached, actual OS/SDK/toolchain recorded, no scientific estimates implied, bounded-loss limitations visible, docs updated, reviewer accepts T04. A successful build or sample APK install alone is insufficient.

## Simulation-first amendment for review — 2026-11-11

[ADR 0004](../../architecture/adr/0004-simulation-first-testing.md) proposes building the source/scheduler and SDK regression layers before more hardware work. Its amendment would permit T03a–T03d against accepted simulated contracts. The live T03e/T04 delivery continues to depend on the T02 acquisition decision and real-device evidence. The current gate above remains effective until review accepts the amendment.
