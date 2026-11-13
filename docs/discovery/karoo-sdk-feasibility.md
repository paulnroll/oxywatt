# Karoo Extensions feasibility

Initial source review 2026-11-10. The [T02a diagnostic host](../spdd/tasks/t02a-diagnostic-host.md) records the later APK build against pinned SDK sources. Installation and launch were verified on Karoo 3. Host binding and whole-ride behavior still require device checks. VERIFIED means the API/source contract exists, not that end-to-end behavior was measured.

## Evidence baseline

[S1: release 1.1.9](https://github.com/hammerheadnav/karoo-ext/releases/tag/1.1.9) is the latest release returned by GitHub releases during this review (published 2026-03-17); generated docs also identify 1.1.9. Use `io.hammerhead:karoo-ext:1.1.9` as candidate, pending dependency resolution and target-device verification. Recheck before implementation. Do not substitute master silently.

Pinned source links:

- [S2: KarooSystemService](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/lib/src/main/kotlin/io/hammerhead/karooext/KarooSystemService.kt)
- [S3: KarooEvent](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/lib/src/main/kotlin/io/hammerhead/karooext/models/KarooEvent.kt)
- [S4: DataPoint](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/lib/src/main/kotlin/io/hammerhead/karooext/models/DataPoint.kt), [StreamState](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/lib/src/main/kotlin/io/hammerhead/karooext/models/StreamState.kt), [DataType](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/lib/src/main/kotlin/io/hammerhead/karooext/models/DataType.kt)
- [S5: KarooExtension](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/lib/src/main/kotlin/io/hammerhead/karooext/extension/KarooExtension.kt), [DataTypeImpl](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/lib/src/main/kotlin/io/hammerhead/karooext/extension/DataTypeImpl.kt)
- [S6: sample extension](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/app/src/main/kotlin/io/hammerhead/sampleext/extension/SampleExtension.kt)
- [S7: library build](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/lib/build.gradle.kts), [sample app build](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/app/build.gradle.kts)
- [S8: official template](https://github.com/hammerheadnav/karoo-ext-template), [README](https://github.com/hammerheadnav/karoo-ext)
- [S9: developer platform](https://shop.sram.com/pages/hammerhead-developer-platform)

## Capability matrix

| Capability | Supported / state | Evidence | Limitations | Proposed solution |
|---|---|---|---|---|
| Power/HR/cadence streams | Yes / VERIFIED | S2–S4 | Cadence IDs differ for source and computed type; no cadence guarantee | Subscribe with `addConsumer<OnStreamState>(OnStreamState.StartStreaming(dataTypeId), …)` using verified `DataType.Type` IDs; read named fields |
| Recording lifecycle | Partial / VERIFIED | S3 `RideState` | Idle, Recording, Paused(auto); Idle means either not started or finished | Explicit local state machine; test both pause forms |
| Durable ride ID and saved/discarded signal | Not exposed in inspected lifecycle / UNSUPPORTED | S3 | Cannot prove native ride identity or save/discard from Idle | Local UUIDs; call it an OxyWatt recording session; retain ambiguity |
| Sensor measurement timestamps | Not in `DataPoint` / UNSUPPORTED | S4 | No sample sequence or sensor timestamp | Stamp receipt with monotonic and UTC clocks; store RECEIPT provenance; quantify delivery delay |
| Time fields | Partial / VERIFIED | S4 `RIDE_TIME`, `ELAPSED_TIME`, `PAUSED_TIME`; S6 | `ELAPSED_TIME` means recording time, not total wall duration. Not an authoritative moving-time signal | Preserve SDK fields separately; calculate local recording coverage; probe units/reset semantics |
| Rider settings | Yes / VERIFIED | S3 `UserProfile` | kg, maxHr, restingHr, ftp, zones; configured is not necessarily measured | Snapshot/version and validate; do not infer age/sex or HRmax |
| Custom numeric/graphical field | Yes / VERIFIED | S5, S8 | Streaming follows subscribers; custom view uses RemoteViews | Numeric status field initially; Compose app screen separate |
| Service connection recovery | Partial / VERIFIED | S2 | Consumers persist through service reconnect in living process; not durable across process death | Re-register after restart; persist recovery metadata |
| Whole-ride service residency | CONDITIONAL | S5, S6 | Sample behavior is not an OS longevity guarantee | Probe `startFit` ride binding; separate acquisition from views; test hidden field, screen-off, UI closed |
| Automatic start without opening app/field | UNKNOWN | S5 | Host binding triggers need device evidence | T02 startup matrix; consider user-armed Android service only if OS policy allows and owner accepts |
| Recover missing samples after death | No public replay found / UNSUPPORTED | S2–S4 | Reconnection is not backfill | Persist gaps; optional later FIT reconciliation; never invent values |
| Historical FIT/completed ride enumeration | No public API found / UNSUPPORTED | S2–S5 | This is a conclusion about inspected public API, not all device files | Owner-provided FIT for development; no private filesystem scraping |
| FIT augmentation | Yes / VERIFIED | S5–S6 | `startFit`, `fitFile=true`, record/session effects; host lifecycle untested | Candidate ride-scoped activation; do not emit meaningless developer fields just to stay alive |
| Android/Compose/Room compatibility | CONDITIONAL | S7 | Library minSdk 23/compileSdk 34; sample minSdk 23/targetSdk 34/compileSdk 35 are not device OS proof | Inspect `ro.build.version.sdk`, software build and toolchain; choose compatible dependency versions |
| Packaging/install | Partial / VERIFIED | S8 | APK, manifest service action and extension XML; install flow device-dependent | Adapt template after approval; debug install with ADB; record exact procedure |
| Build package access | Yes / CONDITIONAL | S8 | GitHub Packages authentication required even for public package | Local/CI read-package credentials, never committed; runtime remains offline |
| Public distribution | CONDITIONAL | S9 | Official platform permits extensions outside partnership; submission details can change | Signed APK/update manifest; verify current Library submission and signing rules before release |

## Lifecycle hypothesis and fallback

Evaluate acquisition started from the host's ride-scoped FIT binding, independent of visible data fields. `startFit` existence is verified; invocation timing, empty-effect behavior, service lifetime and release are UNKNOWN. Merely declaring a service or using Flow does not guarantee collection. Do not enable scans/map/radio permissions as artificial keep-alives.

If host binding cannot support automatic whole-ride recording, test a compliant user-armed foreground collector using the actual device OS rules. If neither works acceptably, stop live-product implementation and ask for a scope decision (explicit recording mode or FIT-first). Do not silently weaken R09.

T02 must record OS/software/SDK versions, startup triggers, callback intervals/latency, resource measurements, reconnect and process-death evidence. A killed process cannot collect; goal is bounded loss and honest recovery, not impossible continuity. Native saved/discarded reconciliation remains unsupported unless new evidence is found.


## T02c1 current diagnostic readout/report — 2026-11-12

[Standstill implementation and limited hardware evidence](../spdd/tasks/t02c1-standstill-diagnostics.md#execution-evidence--2026-11-12) uses unchanged SDK 1.1.9 and existing adapter. Diagnostic 0.3.0 displays real received values/age/status and retains bounded private timing/status metadata through a process-scoped background writer. Short k24/API 32 install/launch/connected-Idle/readout/report check passed, plus HOME/reopen with one source-start marker. No new API or keep-alive mechanism, sensor measurement time, historical access, numerical accuracy, full-ride residency/resource or T03e persistence capability is inferred. Parent conditional capabilities remain conditional; no model or release approval.


## T03e1 diagnostic canonical integration — 2026-11-12

[Owner-approved local implementation and software evidence](../spdd/tasks/t03e1-live-diagnostic-persistence.md#implemented-structure-and-policy-refinements--2026-11-12) connects the same SDK 1.1.9 adapter to existing admission/lifecycle/SQLite through a shared stamp gate and one worker. SDK-fake tests exercise canonical persistence, duplicate-source refusal, Activity recreation, service drain and fresh same-process epochs. This adds no SDK capability, native session identity, timestamp, backfill or keep-alive guarantee. Version 0.4.0/code 4 is built locally and not installed; previous 0.3.0 hardware evidence remains specific to its original binary. Full T02c/device V11/V13/N02 and parent live acceptance remain open.


T03e1 E10 short hardware check (2026-11-12): owner authorized installation/check after software completion. [Actual evidence](../spdd/tasks/t03e1-live-diagnostic-persistence.md#e10-authorized-installation-and-short-karoo-check--2026-11-12) confirms matching-signer in-place 0.4.0/code 4 update on k24/API 32, exact installed prepared-APK hash and Connected/Idle zero-power/cadence plus unavailable HR. Screen received 213 / committed 203 / pending 10 matches SQLite revision 20 exactly; revision 21 retains all 213 unique observations, one epoch, zero sessions/analyses. HOME/reopen keeps the process and one source-start marker; timing/status report stays separate. E10 limited PASS supersedes earlier not-run/not-installed status. Raw SQLite/profile/report/screenshots/identifiers/timestamps stay private outside Git. No native ride or FIT replay; full T02c/N02/T03e/live T04 and science remain open. No publication/merge.


T04b3 local software completion (2026-11-12, owner-approved): [implementation/evidence](../spdd/tasks/t04b3-live-diagnostic-curve.md#final-verification-and-disposition--2026-11-12) delivers explicit latest-ended diagnostic curve calculation/save/readout through the existing worker and repositories. Shared stepped replay, exact live policy and real-origin codec/slot, canonical priority/cancellation and cap/newest-result safeguards are verified with synthetic inputs only. Final full debug 170 PASS; full preview 192 PASS before the final cancellation cleanup, followed by 54 focused preview passes; core check/build 120 passes/one private-FIT skip cached, build/lint/format/isolation PASS. Version 0.5.0/code 5 built locally, not installed. No private data/FIT/device analysis or publication. T02c2 remains deferred; full acquisition/resources/product live T04 and physiology remain open.


T04b3 reviewed installation/publication (2026-11-12): [actual install evidence](../spdd/tasks/t04b3-live-diagnostic-curve.md#reviewed-050-installation--2026-11-12) supersedes earlier not-installed status. Pre-PR review fixed analysis selection before latest control acknowledgement; 172 debug/196 full preview and relevant checks PASS. Same-signer 0.5.0/code 5 installed with -r, exact reviewed APK hash read back, Connected/Idle/Ready and version footer observed. Binary provenance remains fb810cf plus reviewed pre-commit changes. No real Calculate/native recording/FIT analysis or controlled hidden/screen-off claim. Private artifacts outside Git; owner authorized PR publication, not merge.


## T08b1 custom-field API verification — 2026-11-13

The approved [saved-curve field](../spdd/tasks/t08b1-saved-curve-field.md) retains resolved SDK 1.1.9. Direct pinned-source reads verify [sample ExtensionInfo XML](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/app/src/main/res/xml/extension_info.xml): DataType children use typeId/displayName/description/graphical/icon, with typeId matching KarooExtension.types. [ViewConfig](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/lib/src/main/kotlin/io/hammerhead/karooext/models/ViewConfig.kt) explicitly defines viewSize in pixels and standard textSize in sp; it has no theme property. These shell source fetches succeeded after the earlier web-tool cache misses. The implementation uses resource-backed Light/Dark configuration, actual host dimensions and platform SP conversion.

Resolved SDK bytecode confirms ViewEmitter.updateView(RemoteViews) forwards a Bundle to IHandler and rejects updates under 900 ms by wall clock; T08b1 additionally limits its own cancellable snapshot loop to one second. Binder/render errors cancel only the view, never acquisition. SDK-fake host integration and native Android bitmap checks are software evidence, not actual Karoo layout/lifecycle/visibility/resource evidence. No SDK upgrade, field-driven collection, keep-alive or hardware guarantee is introduced. Actual outcomes are recorded in the task.


## T08b2 second graphical history type — 2026-11-13

The approved [history implementation](../spdd/tasks/t08b2-power-history-timeline.md) registers `power-history` alongside unchanged `saved-power-curve` under the same extension. It reuses pinned SDK 1.1.9 DataTypeImpl/RemoteViews/ViewConfig, existing image layout and bounded changed-only rendering; no new SDK capability or native historical ride enumeration is assumed. History comes from OxyWatt's existing local repositories, never Hammerhead ride storage. SDK-fake registration/frames/cancellation and API32/34 native rendering are software checks; P09 actual Karoo placement/readout and wider lifecycle/resource checks remain NOT RUN. No acquisition is owned by either field.
