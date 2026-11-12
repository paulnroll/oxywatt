# T02b — Karoo binding and stream probe

The owner authorized this diagnostic increment after T02a installed and launched on Karoo 3. It covers R02/R09 and partial V02/V11. T02a records the device and SDK baseline. T02c covers sustained acquisition and resource checks.

The extension declares FIT capability so the host can exercise `startFit`. It registers a cancellation callback and emits no FIT effects. A service instance opens a KarooSystem connection and subscribes to ride state and the power, heart-rate and computed cadence types. The visible activity owns no subscriptions. Service teardown calls the SDK disconnect, which removes consumers. A new service instance starts a fresh set of subscriptions.

Log connection changes, FIT binding and cancellation, ride state including auto-pause, and presence or validity of named fields. Zero power and cadence are valid. Zero heart rate is invalid. Channel logs include local monotonic receipt time and the interval since the preceding callback. A stable status is logged at most once per five seconds. No numeric readings, sensor IDs, profile values or GPS are written to the log. Receipt timing measures callback delivery. Sample freshness and device measurement time remain unknown.

Run the diagnostic build and lint as in [T02a](t02a-diagnostic-host.md). Install the APK on the identified Karoo and inspect only the probe tag:

```sh
adb -s DEVICE logcat -v threadtime -s OxyWattProbe:I '*:S'
```

With the diagnostic activity closed and no OxyWatt data field visible, start recording, pause, resume and end. Wake or connect power, HR or cadence sensors to test their channels. Record observed callbacks and gaps. A single recording cannot establish four-hour operation or recovery after process death.

## Code review

Checked the 1.1.9 SDK registration and teardown paths, matching field constants, and explicit absence/invalid-value reporting. The SDK logs no measurement timestamp. The probe avoids logging SDK exception messages and does not emit artificial FIT data.

## Evidence

- Formatting and all 17 JVM tests passed. The debug APK assembled and Android lint passed against the official 1.1.9 SDK sources using the isolated T02a build. The published-AAR build still requires package credentials.
- ADB installed `com.oxywatt.diagnostic` 0.2.0 (version code 2) on the same Karoo 3. The host created the extension service, the KarooSystem connection reported true, and the initial ride callback was IDLE. Power, heart rate and cadence each reported unavailable at idle. The following observations came from the test ride.
- During the USB test, the same extension process reported manual PAUSED, then RECORDING 4.636 seconds later, then manual PAUSED 1.814 seconds later. The available log snapshot did not include the initial recording transition, final IDLE or a FIT binding marker. Sensor samples remain unverified.
- After the owner ended the test, the same process reported RECORDING → manual PAUSED → IDLE, then the FIT cancellation callback 104 ms after IDLE. The cancellation callback is registered only by `startFit`, so this indirectly confirms a FIT binding. The log buffer did not retain the start marker or its timing. The three sensor channels reported searching after the end. No valid sensor sample was observed in this trial.

## Simulation-first follow-up proposal — 2026-11-11

Existing device evidence above remains partial. [ADR 0004](../../architecture/adr/0004-simulation-first-testing.md) proposes moving future canonical mapping/teardown regression coverage into T02d with the SDK fake. The current probe logs presence and sampled callback intervals. It does not yet implement RideDataSource or record all canonical inputs. Real sensor delivery, startFit timing and the sustained acquisition matrix remain pending. Proposed T03 software development can proceed independently after named approval of the amended dependencies.

T02d follow-up: subscriptions now live behind KarooRideDataSource, with the same presence/timing diagnostic logging and observation-only FIT callbacks. Configured profiles are normalized but never logged or stored. See [T02d](t02d-karoo-adapter-tests.md) for SDK-fake regression evidence; the earlier hardware observations apply to the original probe APK, not automatically to this refactor.


## HR-only connected-device follow-up — 2026-11-11

After the T03p synthetic verification the owner offered a real HR sensor and confirmed it was paired/ready. This permits a limited continuation of the existing T02b probe, not full T02c or T03e. No new APK was installed, no native ride was started by the agent and no numeric HR values, sensor identifiers, profile or GPS data were read/published.

Target remained Hammerhead k24, Android 12/API 32, build SKQ1.230210.001 dev-keys; ride host 4.217.1-ad98202524/code 27885909. Existing original diagnostic package is 0.2.0/code 2. Its installed APK SHA-256 is `33c7763252e203241fa2fdb688d35276d50c94c18f9ad720b3ea2d7fc2cb0056`. APK DEX inspection found no KarooRideDataSource marker: this is the older installed probe, not proof that the refactored T02d adapter or current main APK ran on hardware. The original package was neither replaced nor cleared.

Activity Manager showed one original probe service record. Fresh tag-filtered captures discarded the initial retained record. During a 12-second baseline, two sampled heart-rate=present messages reported previous-callback intervals of 1004 and 1002 ms. After HOME and the screen-sleep key command, a 15-second capture again reported HR present (sampled preceding intervals 1027 and 1016 ms). The agent then sent the wake command. These intervals describe callback delivery immediately preceding sampled log entries; logs are throttled to about five seconds and do not establish every callback, measurement timestamps, worst-case gap or four-hour continuity. The screen-sleep command was issued, but display state was not independently queried during that window.

In the same latter window, power and cadence reported unavailable. Their reported ~120-second preceding callback intervals are sparse status notifications, not measured sample gaps. Missing channels were not treated as zero. Numeric HR correctness and canonical SQLite persistence are not established by this probe, which only logs presence/status and timing.

The owner was asked to switch off/disconnect the HR transmitter without unpairing it for a dropout/reconnection check. No raw logs or device identifiers are committed.

A further 45-second observation window still showed HR present throughout the sampled entries (eight messages; preceding callback intervals 1015–1035 ms). No dropout transition occurred in this initial window; transmitter disconnection had not yet been confirmed by the owner.


The owner subsequently confirmed the transmitter was off. A fresh inspection of the retained probe buffer contained HR unavailable followed by searching; the original service still had one service record. Earlier present records were no longer retained in that buffer, so these observations do not establish an exact off-to-notification delay. The sampled preceding-callback intervals were 117967 ms and 466688 ms respectively; these are delivery intervals for sparse channel-status callbacks, not sensor freshness or physiological measurements. A dedicated post-confirmation capture was then started.

The dedicated 30-second post-off-confirmation capture produced no new probe records after the initial retained entry was discarded. This is a silent observation window, not repeated unavailable callbacks. The owner was then asked to reconnect/power the transmitter on without changing pairing.


After the owner confirmed power/reconnection, the retained probe buffer again showed HR present. Six recent present entries included sampled preceding-callback intervals 14019, 1008, 1003, 1005, 1005 and 1013 ms. The 14019 ms entry is an inter-callback delivery interval, not the delay from the owner's power-on action. One original probe service record remained present. The agent did not reinstall, restart the original probe, change pairing, start a native ride, clear app data or read numeric HR during this trial. These observations demonstrate return of the HR channel in the existing probe; they do not establish continuous service residency or recovery after process death.

A fresh 15-second post-on capture independently recorded two HR present entries, with preceding callback intervals 1005 and 1013 ms. Readiness and dropout/reconnection observations above were reviewed for privacy, hardware-versus-software scope and V02/V11 traceability. Only documentation changed; no Gradle/application test was rerun for this evidence-only follow-up.


## Current-adapter diagnostic update — 2026-11-12

After the owner connected Karoo by USB and asked whether a new version was needed, read-only installed-APK inspection confirmed the original probe SHA-256 `33c7763252e203241fa2fdb688d35276d50c94c18f9ad720b3ea2d7fc2cb0056`, with no KarooRideDataSource DEX marker. The device again identified as model k24, Android API 32, build `SKQ1.230210.001 dev-keys`. No device serial or raw device log is committed.

Rebuilt existing main `cf12e69bd08e14aa06e3cc90f30ddbe6edbf6137` without application-code or dependency changes using `ANDROID_HOME=/Users/pawelkozicki/Library/Android/sdk ./gradlew -PkarooDiagnostic=true -PkarooSdkJitpack=true :app:assembleDebug :app:lintDebug --console=plain`: PASS in 26 seconds. This uses the existing opt-in SDK 1.1.9 configuration. No new application test execution claimed. The resulting debug APK contains the current adapter and the exact build revision; signing certificate matches the previously installed diagnostic APK.

`adb -s DEVICE install -r app/build/outputs/apk/debug/app-debug.apk` succeeded as an in-place update of `com.oxywatt.diagnostic`. Pulled the public installed APK back and verified exact SHA-256 equality with the new build: `2d840a7c9dc14b4c0bcf2633742aa40fb79739297e05ca2fdcd0b995e36572ad`. Version name/code remain 0.2.0/2, so these labels alone do not distinguish the original probe from the current binary. The separate synthetic preview package was not updated.

`adb -s DEVICE shell am start -W -n com.oxywatt.diagnostic/.MainActivity` returned `Status: ok`. Fresh current-process OxyWattProbe-only logs initially showed extension creation; a subsequent read observed `karoo connected=true` and `ride=IDLE`. No FIT binding start was observed. The initial immediate post-launch read preceded connection/Idle, so it is not evidence of a connection failure.

This establishes current-APK installation, launch and a short host connection/Idle observation, supporting R02/R09 → setup/partial V02/V11 only. The owner did not start a ride as part of this verification; the agent issued no ride-start, pairing, clear-data, numeric sensor/profile or native ride-data commands. No raw/private data entered Git. Current source observations are still discarded by the diagnostic accept callback, with presence/timing logs only; no live canonical persistence is implemented. Sensor numeric correctness, pause/reconnect/startFit, sustained residency/resources, T02c, T03e and parent live T04 remain open. Earlier HR observations remain evidence of the old probe, not automatically new-adapter sensor evidence.


## Current-adapter owner trainer session: retained log tail — 2026-11-12

Owner disconnected USB, rode the trainer, followed the instruction to save the native Karoo activity, then reconnected USB and reported approximately one hour of riding. Duration and save completion are owner declarations, not independently verified native file/recording evidence. This is a limited T02b current-probe observation, not authorization or completion of the T02c matrix/T03e. No native ride was started, paused or saved by the agent.

Read only the OxyWattProbe log tag after reconnection, preserving the capture outside Git. The device is k24/API 32. Verified installed public APK SHA-256 still matches the updated cf12e69 build: `2d840a7c9dc14b4c0bcf2633742aa40fb79739297e05ca2fdcd0b995e36572ad`. The retained entries all carry one process ID, which also matches the currently running diagnostic process; no process ID/device serial is committed. The buffer contains 710 probe messages spanning **20 min 50.113 s by log clock**, only a tail of the owner-reported approximately one-hour session. Recording/startFit start markers are absent. Missing earlier entries do not establish that acquisition failed or stopped earlier; full-session coverage/residency cannot be reconstructed.

| Retained channel messages | Status counts | Preceding-callback interval at logged present entries: min / median / max, ms |
|---|---|---|
| Power: 242 | 242 present | 1003 / 1023.5 / 5642 |
| HR: 244 | 244 present | 1003 / 1031 / 5651 |
| Cadence: 221 | 220 present, one idle | 1001 / 1018 / 9236 |

These are status-message counts and sampled preceding-callback intervals. The probe logs status changes or approximately one entry per five seconds, not every callback. Intervals in the table are not the complete callback distribution, maximum true delivery gap, sensor sampling interval, freshness, independent channel synchronization or physiological timing. Power-present includes any finite nonnegative named field (including zero); HR-present only passes the probe's finite-positive value-domain check. No numeric power/HR/cadence, configured profile, sensor identity or GPS was read or logged by this check. A present flag does not validate numerical accuracy.

The remaining three messages show manual `PAUSED auto=false`, then `IDLE` **3717 ms later by receipt monotonic time**, followed by `fit binding stopped` **143 ms later by log-clock difference**. The stop marker originates from the startFit cancellation callback, providing indirect evidence that this current binary had a FIT binding; its start time is outside the retained evidence. No RECORDING transition survives, so no recording duration is calculated. IDLE/cancellation do not prove native save or discard. Channel status messages continue after IDLE; they are not additional recorded-ride duration.

Private retained artifacts are under `/Users/pawelkozicki/Library/Caches/oxywatt/device-evidence/karoo-trainer-2026-11-12/`: `probe-tail.log`, SHA-256 `eb835b20379cb859efbbcec22ab2a62836cfa244e88127a8001da33af0cb8023`; `probe-tail-summary.json`, SHA-256 `3ae997ded074c3627ab9f590b46a0a200065c64163e852a47d0ee0610756359f`. Files have owner-only access. Raw timestamps/process identifiers remain there, outside Git. Independent count/process/span/terminal-event checks match the aggregate above. Exact tag-read/summary commands used ADB through the selected connected device; no all-app log capture, buffer clear, pairing or app restart was performed.

This adds actual current-adapter channel-presence and terminal lifecycle evidence to R02/R09 → partial V02/V11. It does not pass numeric mapping, observed-start/end identity, full approximately one-hour/4-hour acquisition, hidden/no field/screen state, restart/reconnect fault, battery/memory or durable live storage. Current probe still discards canonical observations; T03e and parent live T04 remain open. No FIT was exported/decoded or native/private ride storage inspected; the native save declaration remains separate from measured evidence. No application code/test, hardware stress action, new numerical experiment or physiological claim. Existing local installation evidence remains intact.


## Owner-authorized saved FIT check — 2026-11-12

Owner subsequently asked whether to send the saved file or whether the agent could check it directly. Public shared-storage metadata inspection found the USB-visible `FitFiles` folder; read only the uniquely newest-modified FIT's contents. Its recent modification and approximately one-hour duration are consistent with the reported session. Other ride contents and application-private storage/databases were not accessed. A source metadata-query formatting error was corrected before copying; the private source filename stays outside Git.

Copied the single file through ADB, checked source size/modification metadata had not changed during copying, and preserved it privately outside Git with owner-only access. Input size 131,817 bytes; SHA-256 `24ffbcd5ce6ca16eeda8eb452c5b96ceb2e6df2f8710b63a7760f015b50dbc90`. Existing pinned `fitdecode==0.11.0` environment was recreated under `/tmp/oxywatt-fit-replay/` from the cached wheel, with no project dependency/code change. The bounded checker requested only record time and power/HR/cadence availability, timer events, and session time/sport fields. No GPS, device identifiers, rider profile, performance estimates or canonical replay/export was requested or reported.

Declared FIT length and decoder CRC checks PASS. One cycling session reports **3608 s timer time** and **3612 s elapsed time**. There are **3608 record messages**, no missing/regressing record timestamp, and 3607 successive one-second FILE_RECORD differences (first-to-last record span 3607 s). These are distinct source-file quantities, not proof of independently measured acquisition duration or sensor synchronization. One timer `start` and one `stop_all` event are present. Indoor/trainer context remains owner-declared; no sub-sport is supplied in the parsed session metadata.

| Channel availability in the saved file | Finite value-domain records | Unavailable records |
|---|---:|---:|
| HR (>0) | 3608 | 0 |
| Power (>=0) | 3602 | 6 |
| Cadence (>=0) | 3496 | 112 |

Counts describe stored numeric field availability, not validated sensor accuracy, independent sample count, physical dropout duration or complete joint measured coverage. Unavailable values remain unavailable, not zero. No average/max power/HR, FTP/VO₂max, index or physiological trend was calculated. The full saved-file record sequence complements the truncated probe tail; no sample-by-sample SDK equivalence or channel alignment is possible from presence-only logs. Do not upgrade original file timestamps into sensor measurement timestamps or recover unknown live delivery from this file.

Private artifacts under the same external `karoo-trainer-2026-11-12/` evidence root: `owner-activity.fit`, source metadata/provenance, `check-owner-fit.py` (SHA-256 `f9f25f099044065d0470f174ab546f2424e8277c3defccf5e8711e8640602e13`) and `owner-activity-check.json`. Original timestamps, filename and detailed timer/session metadata remain private. Actual command: `/tmp/oxywatt-fit-replay/bin/python /tmp/oxywatt-check-owner-fit.py`, reading `/tmp/oxywatt-owner-trainer.fit`; retained copies have identical hashes. No file uploaded/published or committed. R02/R07/R10 → V02/V09/V12 source-file integrity/availability/privacy evidence only, not T01e canonical replay or full V21 acceptance. T02c/T03e/parent live T04 and all physiological gates remain open.


## T02c1 standstill diagnostic increment approved — 2026-11-12

Owner asked for progress that does not require another full workout, deferring long-run verification. [T02c1](t02c1-standstill-diagnostics.md) implements the existing source-backed live screen and bounded private timing/status report, not live Collector/database integration. Numeric readings display only; no new source, schema, physiology or SDK pin. P01–P06 cover missing/zero, report durability/privacy/bounds/errors, UI/source ownership, existing software/isolation checks and optional short device smoke. Parent T02c/T03e/T04/scientific gates remain open; no publication authorized. This is an observability slice, not a relaxation of full acquisition acceptance.


## T02c1 implemented and checked on standstill Karoo — 2026-11-12

[Actual scope and P01–P06 evidence](t02c1-standstill-diagnostics.md#execution-evidence--2026-11-12): existing source-owned real-value screen, receipt ages/missing reasons, callback counts and bounded private timing/status report implemented. Values display only; report excludes readings/profile/GPS. Zero/missing, reopen, storage/error/limit, queued-plus-in-flight overflow and Activity tests PASS; full debug 106/preview 130 PASS, unchanged core check cached 117 passes/one skip, build/lint and isolation PASS. Installed 0.3.0/code 3 on k24/API 32 and inspected connected/Idle UI plus private report after HOME/reopen. No full workout or native recording, no complete lifecycle/resource/scientific gate. Earlier local T02b/FIT evidence preserved, no publication performed.
