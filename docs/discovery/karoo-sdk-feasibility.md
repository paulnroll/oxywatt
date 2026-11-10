# Karoo Extensions feasibility

Inspected 2026-11-10. Source review only; no physical-device tests or build executed. VERIFIED means the API/source contract exists, not that end-to-end behavior was measured.

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
