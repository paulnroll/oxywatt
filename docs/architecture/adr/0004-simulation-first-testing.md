# ADR 0004 — Simulation-first development through a shared input boundary

Date 2026-11-11. Status PROPOSED for owner review. This change request authorizes specification work. Implementation and adoption of new dependencies require a named slice approval.

## Context and evidence

The core already accepts source-neutral canonical v1 envelopes. T01b provides small synthetic traces and synchronous replay. T01c decodes a generated binary FIT into a checked-in TSV with independent JVM expectations. Its Python verifier accepts only its own generated stream. Arbitrary FIT input, scenario generation and a virtual scheduler remain to be built.

T02b confirmed connection, initial Idle, manual pause/resume and final Idle on Karoo 3, Android 12/API 32, ride app 4.217.1-ad98202524. FIT cancellation indirectly confirms startFit was called. Its start timing was absent from retained logs. Sensor channels reported unavailable/searching. There is no measured sensor delivery, sustained residency or recovery evidence. The probe logs presence and callback intervals, with a five-second throttle for unchanged status. It cannot reconstruct all input samples.

Move deterministic collection and replay work ahead of repeated hardware sessions. Keep the live feasibility decision attached to the device checks in ADR 0003.

## Decision proposed

Use one small RideDataSource port at the existing canonical envelope boundary. Run the same Collector, persistence policy and eventual analytics with live and simulated sources. Add synthetic generation and FIT decoding in development/test support. Keep the two physical application modules. The SDK test double belongs to Android tests. Device-level replay is an optional external tool.

### Proposed source contract

Place the port beside canonical inputs in core when approved. Its proposed signature is `start(accept: (InputEnvelope) -> Unit): AutoCloseable`. Implementations are KarooRideDataSource in app and SimulatedRideDataSource in test support. The FIT decoder feeds SimulatedRideDataSource. Naming a separate FIT source implementation is unnecessary until there is another consumer.

The Collector/harness constructs each source with an acquisition context containing the locally assigned session and epoch IDs, a receipt clock and an epoch-local sequencer. These use the existing UUID/ReceiptStamp types. The context describes the transport boundary and does not become a new ride aggregate. The source preserves its adapter/version and normalization version in every envelope.

Contract obligations for T01d review and tests:

- One active subscription per source instance. A second start fails explicitly. start either returns a cancellation handle or releases partially acquired registrations before throwing.
- Callbacks are serialized in admitted order, including equal timestamps. Sequence advances for each envelope. Equal readings are retained.
- accept synchronously hands an event to the Collector's bounded admission path. The source retains no asynchronous unbounded queue. N01 governs combined queues and in-flight work. Live overflow produces the existing bounded loss report. Controlled replay may wait for admission/drain in a separate conformance mode.
- close is idempotent, removes SDK consumers and scheduler tasks, and waits for an in-flight callback to finish before returning. Cancellation from inside a callback defers final teardown until that callback exits. No later callbacks enter accept. A failed close reports the failure for retry/diagnosis.
- EOF and cancellation are transport outcomes. Only explicit recording observations affect local lifecycle. A finite trace ending in Recording becomes interrupted/partial under the Collector policy.
- A restart is a new source instance and acquisition epoch. The Collector decides session identity and ambiguity. A source never joins fragments based on a filename, UUID or playback position.
- Test harnesses inject failures and clock readings directly. Android connection/error reporting stays with adapter orchestration. Add another port method only if implementation demonstrates that terminal errors need to cross this boundary.

Karoo maps named fields and observed states before emitting envelopes. Source time stays absent when the SDK provides none. Recorded/file and simulated origins remain explicit. No simulator-specific branching belongs in Collector algorithms.

### Time and acceleration

Keep three concepts explicit in the test manifest.

1. The activity timeline defines the scenario offsets and original FIT record timestamps.
2. The acquisition timeline provides ReceiptStamp UTC and epoch-local elapsed time. Live acquisition uses actual clocks. Deterministic replay uses a virtual receipt clock with versioned origin/offset policy, including injected delivery lag and UTC jumps.
3. The playback scheduler controls how fast the test executes. It must never stamp domain events with the test machine's execution time.

For ordinary synthetic replay, virtual acquisition advances with scenario time. Preserve the exact generated envelopes at 1x, 10x and unpaced execution. A 3,600-second trace still covers 3,600 activity seconds. Use integer nanosecond offsets and absolute deadlines derived from the playback start. The multiplier affects scheduling only. No repeated rounded delay accumulation. Maintain a deterministic input-index tie-breaker and explicit per-channel offsets.

Inject the same virtual clock into Collector timeouts, five-second flush scheduling, staleness checks and analysis as-of selection. Advance through scheduled timers during silence and across paused intervals. Virtual data with real-time flush timers is an invalid test configuration. Use a fixed seed, UUID assignment, start UTC, scenario/generator/normalization/profile versions and scheduler policy. Persist their manifest/hash beside test results.

For FIT, retain original FILE_RECORD timestamps on metric payloads. Map their offsets to a separately declared virtual acquisition origin. Preserve file event order and explicit timer events. Canonical v1 has source time only on metric payloads, so the decoded fixture manifest must retain original event timestamps too. Reject missing or regressing timestamps with a diagnostic until an explicit reconciliation policy is approved. Never silently sort away evidence. Backward seek or loop starts a separate run/epoch. A recorded wall-clock discontinuity can be modeled explicitly without decreasing epoch-local elapsed time.

The external Karoo replay tool sends SDK DataPoint values without the file timestamps. OxyWatt's live adapter will stamp actual callback receipt. Its 10x playback therefore compresses observed coverage. Restrict numerical duration comparisons on that path to measured 1x runs. Accelerated host runs exercise wiring, availability and load. Full-speed numerical regression uses the virtual JVM path.

### Scenario data and expected results

A versioned scenario consists of literal segments and independent channel schedules. Configure W, bpm, rpm, duration, timestamp origin, sample offsets, optional seeded jitter/noise, profile snapshots and recording events. Use constant segments for steady state and intervals, piecewise linear ramps, and a declared effort-duration/power table for maximal-effort traces. The word maximal labels a generated test shape.

Inject faults independently of signal shape. Distinguish an explicit null with a reason, a channel dropout, silence across all channels, pause/resume with auto/manual metadata in the scenario, and process restart/cold attachment. Preserve known zero values. Gap recovery and carry-forward limits are decided by the shared Collector, with explicit coverage breaks. Canonical v1 omits the auto-pause flag, so retain it in SDK expectations/manifest while checking the mapped PAUSED state.

Generate lazily into the same admission path. Full-speed conformance waits for drain. A separate burst mode deliberately exceeds N01 and verifies loss. A deterministic restart harness reconstructs sources/Collector from checkpoints. Actual process death remains a device check.

Hand-calculated anchors include a 250 W complete 3,600-second signal whose fully covered target windows all average 250 W, a 100→300 W linear 10-second reference integral of 2,000 J, explicit interval integrals, a pause splitting a candidate window, HR dropout with intact power, and a restart excluding ambiguous fragments. The continuous ramp oracle must declare the future preprocessing interpolation convention before comparing sampled data. Existing irregular-time v1 fixtures remain contract tests.

Choose expected results from analytical integrals and literal event tables. Fixtures and generators must not compute goldens by invoking the production analysis. Exact envelope/key equality is required for deterministic replay. Future floating-point analytics use a declared absolute tolerance of 1e-6 W for synthetic oracle anchors, subject to method review in T04. Generation shape and tolerance carry no physiological accuracy claim.

### Real FIT replay

Extend the pinned fitdecode 0.11.0 developer workflow only after T01e approval. Add an input-file mode, strict CRC validation, timer event coverage, explicit missing/invalid reasons and versioned decoded fixture export. Review stop_all and missing initial/final states explicitly. A file's end alone supplies no native saved/discarded fact.

Secure at least one real exported FIT with permission for the intended use. Record origin, decoder version, hash, timestamp policy, expected fields/events and redistribution permission. Keep owner files and their decoded output private. Public CI uses a minimal redistributable fixture after GPS, serials and identifying data are removed with permission. If no lawful fixture is available, record T01e's real-file criterion as pending. The generated T01c binary remains useful for protocol edge cases.

## Candidate dependencies and tradeoffs

Source review on 2026-11-11, without building or running these projects:

| Candidate | Source-verified capability | Proposed use and unresolved dependency |
|---|---|---|
| nikosavola/karoo-ext-testing, tag 0.1.1, commit 699d67fb94ebd8a7c2dc32ccd6b7dc4aec53b1cf | Apache-2.0. FakeKarooSystem can set ride/stream/profile state, FakeKarooHost can start FIT, and Robolectric binding helpers drive the real extension service. Three published artifact families include an emulator appstore stand-in. | Preferred SDK integration candidate. Use testing + testing-robolectric only in app tests initially. Confirm artifact resolution and teardown compatibility in T02d before adoption. |
| lgangitano/karoo-ride-replay v1.2.0, reviewed commit 0022abb7ef3c12119cc445779e7d06e8fea246e2 | Apache-2.0 project. Independent virtual power/HR/cadence/speed sensors, channel dropouts, replay speed controls and mock GPS. FitParser consumes RecordMesg and ignores Event/Session/Lap. | Optional hardware integration helper. File parsing/timer loss and actual receipt clocks limit the expected results. It still requires a Karoo for the real host path. |
| Existing OxyWatt fixtures and fitdecode 0.11.0 | Seventeen JVM tests, generated FIT projection, explicit source provenance and epoch arithmetic. | Reuse as a baseline. Add scheduler/scenarios and lawful real FIT cases in test support. |

The testing library README declares Kotlin 2.0+, JDK 17+, SDK 1.1.9, minSdk 23/minCompileSdk 33. Its own tag builds with compileSdk 37, AGP 9.4.1, Kotlin 2.4.20 and Robolectric 4.17. Consumer compatibility with OxyWatt's compileSdk 36, AGP 9.0.1, Kotlin 2.3.20 and JDK 25 requires an actual resolution/test spike. Keep one SDK coordinate, io.hammerhead:karoo-ext:1.1.9, because the fakes declare karoo-ext compileOnly. JitPack may avoid authentication for the test library, while the existing official SDK feed still requires package access. Scope repositories to the selected artifacts and pin versions/hashes. No automatic SDK/toolchain upgrade is proposed.

An emulator stand-in uses package io.hammerhead.appstore. Install it only in an isolated emulator. Keep fake APKs and simulator sources out of release dependencies. Robolectric tests should start at API 32, matching the observed device, plus a supported higher API after the compatibility spike. Reproduce sticky initial states and verify the behavior of controlled loopers and callback threads rather than relying on sleeps.

The replay app declares compile/target SDK 37 and minSdk 24, requests all-files and mock-location access, and uses Garmin FIT 21.218.0 under its separate SDK terms. These are dependencies of an optional external tool. Verify its APK and behavior on the known API 32 Karoo before adoption. Restore sensor pairings and mock-location settings after the test. Use a clearly identified test activity. Reading its sources does not establish successful installation or permission compatibility.

Pinned evidence links:

- [Testing README and install matrix](https://github.com/nikosavola/karoo-ext-testing/blob/699d67fb94ebd8a7c2dc32ccd6b7dc4aec53b1cf/README.md), [build versions](https://github.com/nikosavola/karoo-ext-testing/blob/699d67fb94ebd8a7c2dc32ccd6b7dc4aec53b1cf/gradle/libs.versions.toml), [license](https://github.com/nikosavola/karoo-ext-testing/blob/699d67fb94ebd8a7c2dc32ccd6b7dc4aec53b1cf/LICENSE).
- [Replay parser](https://github.com/lgangitano/karoo-ride-replay/blob/0022abb7ef3c12119cc445779e7d06e8fea246e2/app/src/main/kotlin/it/gangitano/karooridereplay/replay/FitParser.kt), [scheduler](https://github.com/lgangitano/karoo-ride-replay/blob/0022abb7ef3c12119cc445779e7d06e8fea246e2/app/src/main/kotlin/it/gangitano/karooridereplay/replay/ReplayEngine.kt), [virtual sensor emission](https://github.com/lgangitano/karoo-ride-replay/blob/0022abb7ef3c12119cc445779e7d06e8fea246e2/app/src/main/kotlin/it/gangitano/karooridereplay/vdevice/ReplayVirtualDevice.kt).
- [Replay build and dependencies](https://github.com/lgangitano/karoo-ride-replay/blob/0022abb7ef3c12119cc445779e7d06e8fea246e2/app/build.gradle.kts), [usage and permissions](https://github.com/lgangitano/karoo-ride-replay/blob/0022abb7ef3c12119cc445779e7d06e8fea246e2/README.md), [license](https://github.com/lgangitano/karoo-ride-replay/blob/0022abb7ef3c12119cc445779e7d06e8fea246e2/LICENSE).

## Acceptance and gates

The layer criteria and V18–V22 are maintained in [validation strategy](../../algorithms/validation-strategy.md). The implementation sequence and exact proposed work packages are in [simulation-first task](../../spdd/tasks/simulation-first-development.md).

Synthetic traces establish numerical conformance, abstention logic and regressions. Scientific evaluation of FTP and VO₂max still requires independently reviewed methods, applicable populations and real criterion evidence in T05–T07.

## Consequences and alternatives

Hardware sessions concentrate on real radio data, host startup, hidden/no-field operation, screen-off, service reconnection, process death, force-stop and measured resource budgets. SDK fakes and emulator process recreation establish software paths under controlled conditions. Four virtual hours can exercise bounded state and loss policies, while real battery/OS residency require elapsed-time device trials.

Propose revising ADR 0003 to allow T03a–T03d simulation-backed development after the source contract and required simulator tests are accepted. T03e production wiring still requires the T02 hardware acquisition gate. A failed hardware hypothesis requires review of the live architecture. Pending hardware work remains visible in the parent T02/T03 acceptance.

A broad data-source plugin framework would add configuration and domain types without a current need. Direct canonical-input simulation alone would leave SDK mapping and teardown untested. Hardware-only development would delay deterministic fault and regression coverage. The small port plus separate test layers addresses these concrete gaps.
