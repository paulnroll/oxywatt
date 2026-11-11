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
