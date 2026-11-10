---
name: karoo-extension
description: Design or verify OxyWatt integrations with official karoo-ext, including stream mapping, extension lifecycle, fields, packaging and Karoo hardware evidence. Use for Hammerhead adapter changes.
---

# Karoo integration

Read `docs/discovery/karoo-sdk-feasibility.md` and ADR 0003 before API or lifecycle choices. These provide pinned official sources and evidence states. Recheck latest compatible release before changing the SDK pin; candidate 1.1.9 is not yet device-validated. Avoid obsolete karoo-sdk.

Use `KarooSystemService` event consumers with `OnStreamState.StartStreaming(dataTypeId)` and verified `DataType.Type`/field constants. Validate units and per-channel states. Karoo source cadence and computed cadence IDs differ. Track/remove consumers and cancel jobs/emitters on teardown; living-process reconnect is not recovery after process death.

`DataPoint` has no sensor timestamp or sequence. Preserve local receipt UTC, monotonic time and process epoch; do not fabricate measurement time. Repeated equal readings are not duplicates. `RideState.Idle` is not proof of native save or a durable ride ID. Profile HRmax/FTP are configured values, not verified measurements.

Acquisition belongs outside visible field/view lifecycle. `startFit` host binding is a hypothesis to test, not a supported keep-alive guarantee. No fake FIT writes, scanning or map subscription solely to keep a process alive. If reliable independent acquisition fails, record evidence and propose an explicit architecture change.

Custom field views use the SDK RemoteViews path; Compose app UI is a separate surface. Follow manifest/XML IDs exactly and keep extension ID distinct from Android application ID. Runtime does not need network access just because package resolution uses credentials.

For hardware reports record Karoo OS/software/SDK, startup triggers, hidden/no field, screen off, pauses, service reconnect, process kill versus force-stop, callback timing, gaps, memory and battery. Never upgrade CONDITIONAL to VERIFIED hardware behavior from sample code. No device or application code is authorized until the M0 checkpoint is approved.
