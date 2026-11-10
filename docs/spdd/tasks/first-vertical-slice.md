# First vertical slice — Karoo → Collector → Persistence → Power Curve

Status: NOT APPROVED. Task IDs T01–T04. Read Canvas v0.1 and ADRs 0001–0003 first.

Goal: a real recording session on Karoo produces durable source-neutral inputs and correctly observed power-duration records, independent of visible fields.

Scope: minimal Gradle `:core` and `:app`; canonical observations; profile/recording adapter; lifecycle probe; local repository; bounded checkpoints; numerical power curve; simple current-session/result readout. Developer replay fixtures support validation. No FTP/VO₂max formula, product FIT import, elaborate chart, external service or publication.

Dependencies: explicit architecture/slice approval; actual device/ADB access supplied through authorized tooling; package credentials available privately; selected compatible Android/JDK toolchain. These are prerequisites, not assumed present.

Expected paths after approval: `settings.gradle.kts`, Gradle wrapper/version configuration; `core/src/main/kotlin/.../{collection,power,ports}` and JVM tests; `app/src/main/.../{karoo,persistence,ui}` plus manifest/extension XML; `docs/discovery/karoo-hardware-validation.md` created only when real evidence exists. Final file names follow chosen namespace; no placeholders are created now.

Acceptance:

1. T01 maps live/file semantics into one contract; V01–V03 fixtures pass independently of Android.
2. T02 proves or rejects source timing/startup/lifecycle assumptions on the actual Karoo build. Record four-hour coverage, field visibility, screen/app state, pause/reconnect, process kill and force-stop separately. If host residency fails, stop before T03's production acquisition design.
3. T03 commits bounded data/checkpoint atomically, survives restart with explicit gaps/ambiguity, never duplicates local sequence keys. Storage failure is visible; V04/V05/V09/V12/V14 pass.
4. T04 time-weighted maxima match independent integrals, exclude gaps and persist provenance. A recorded device session reaches the readout without requiring our field to remain visible. V06/V13 pass; memory/battery budgets measured.

Risks: SDK has no sensor timestamp/durable ride ID; host binding unproven; telemetry-free hardware diagnostics still contain sensitive physiological data. Keep traces local and report summaries only.

Definition of Done: requirement/test evidence attached, actual OS/SDK/toolchain recorded, no scientific estimates implied, bounded-loss limitations visible, docs updated, reviewer accepts T04. A successful build or sample APK install alone is insufficient.
