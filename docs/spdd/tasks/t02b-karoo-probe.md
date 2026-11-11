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
