# Domain model

Status: owner approved the source-neutral core/adapters architecture and T01a in this session (2026-11-10). Other packages and physiological methods remain gated; decisions D01–D03 already explicitly supplied. Names describe responsibilities, not mandatory classes/tables.

| Concept | Kind and responsibility | Invariants |
|---|---|---|
| RecordingSession | Aggregate; OxyWatt's local ride recording | Local UUID; source identity; lifecycle; completeness never inferred from a restart |
| RecordingSegment | Entity within session; contiguous acquisition epoch | New epoch on process restart; gaps/pauses explicit; no windows crossing discontinuities |
| SourceObservation | Value; one received metric or lifecycle event | Metric, value/unit, source, receipt UTC and monotonic time/epoch, optional source measurement time and sequence; missing differs from zero |
| RideSample | Value; normalized analysis frame | Session/segment, analysis time, power/HR/cadence nullable with per-channel age and flags; no false atomic alignment |
| RiderProfileSnapshot | Immutable value | Mass kg and configured HR/FTP with origin, effective date, version/hash; invalid values rejected |
| PowerCurve / PowerRecord | Calculated observation | Duration, average W, window boundaries, coverage, contributing session, preprocessing version |
| PerformanceEstimate | Immutable result envelope | Metric-specific value/unit or unavailable; method/version/config/input hashes; profile; quality; reasons; activity references; calculatedAt distinct from observedAt |
| FTP / VO₂max model | Independent domain services | Explicit input eligibility, no cross-metric substitution, abstention allowed |
| ManualCalibration | User-provided value | Units, date, origin and supersession; never displayed as automatically measured |
| TrendSnapshot | Derived projection | Contributor session/run/generation IDs, horizon, smoothing version, cohort and as-of date; only unambiguous sessions and authoritative results contribute; no new physiology claim |

## Lifecycle

NewSession → Recording ↔ Paused → EndedLocally → Analyzed. Unexpected death results in Interrupted; recovery starts a segment only when continuity is sufficiently evidenced, otherwise a separate Recovering/Ambiguous session. EndedLocally means a transition to SDK Idle was observed, not that the native ride was saved. Session may be partial; analysis eligibility is independent of lifecycle labels.

Trend eligibility is stricter than availability of a partial-session estimate. Exclude interrupted/ambiguous sessions and their linked recovery fragments from aggregate contributors and minimum-session counts. A cold attachment to an already recording ride is partial as well. A new UUID does not prove an independent ride; a fresh observed Idle → Recording start with observed local end and no ambiguous recovery is required. Keep partial observations visible with reasons without automatically merging them. See Canvas v0.2 and V15.

An estimate result slot `(session, metric, cohort)` points to the newest requested, persisted analysis generation. Pending/failed newest generations contribute nothing; an unavailable terminal result replaces prior availability. Older completions cannot replace a newer generation. Immutable prior runs remain historical evidence, not fallback contributors. See V16.

Do not create a separate aggregate for every sample or enforce a huge in-memory list through a Ride aggregate. Repository writes append bounded chunks with unique `(sessionId, epochId, ingestSequence)` keys. Equal numeric readings are not duplicates; lack of SDK sample IDs precludes perfect sensor-level deduplication.

Domain outcomes such as SessionInterrupted and AnalysisCompleted may drive direct application calls. Add persistent messaging only if a concrete requirement emerges.

## Quality dimensions

Keep input coverage, effort suitability, model applicability, physiological validation status and age separate. Proposed labels unavailable / limited / eligible describe evidence, not a calibrated probability. A model may have excellent signal coverage and unknown absolute accuracy. Store rejection reason codes and translate them in the UI.

The core holds no Android/SDK/ORM/UI classes. Persistence schema follows access patterns; domain relationships are not automatic Room entities.

## Canonical input contract v1 — T01a

Package `com.oxywatt.core.collection`, file `CanonicalInput.kt`. This revision is accepted for T01a by the owner’s named slice approval; it freezes the boundary for T01b/T01c, not their execution authorization.

- `InputEnvelope(sessionId: UUID, stamp: ReceiptStamp, source: SourceRef, normalizationVersion: String, payload: InputPayload)`. Local session identity is not a vendor ride ID. SourceRef contains nonblank adapter ID/version and optional nonnegative source sequence; it contains no device serial.
- `ReceiptStamp(utc: Instant, epochId: UUID, elapsedNanos: Long, ingestSequence: Long)`: nonnegative elapsed time relative to a process-local epoch; sequence nonnegative and locally increasing (enforced by future collector, not a value object). Epoch changes on restart. UTC is audit time and cannot establish duration/continuity. `elapsedSince(earlier)` returns Duration only for the same epoch and nondecreasing elapsed time; otherwise null. Equal times return zero. This helper computes clock differences only; sharing an epoch does not establish continuity across pauses, gaps or session boundaries. Epoch IDs are opaque UUIDs.
- `InputPayload.MetricReading(metric: Metric, value: Double?, missingReason: MissingReason?, sourceTime: SourceTime?)`: POWER_W, HEART_RATE_BPM, CADENCE_RPM declare units. Present values must be finite, power/cadence ≥0 and HR >0. Missing values require UNAVAILABLE, DROPOUT or INVALID_SOURCE_VALUE; present values forbid a missing reason. A reported zero HR is invalid, not a manufactured dropout. Adapters convert sentinel/invalid values to missing with a reason. No arbitrary physiological ceiling.
- `SourceTime(utc: Instant, meaning: SourceTimeMeaning)` distinguishes MEASUREMENT from FILE_RECORD; absent when the source gives no timestamp. A decoded file record timestamp is not automatically sensor measurement time. Receipt and source timestamps are independent; no guessed time or source sequence.
- `InputPayload.Recording(state: RecordingState)` represents UNKNOWN, IDLE, RECORDING, PAUSED without asserting a saved native ride or completing a session. State machine belongs to T03a; cold attach/initial Idle have no completion side effect here.
- `InputPayload.Profile(snapshot: RiderProfileSnapshot)` carries `version: String`, `effectiveAt: Instant?`, and nullable `massKg`, `configuredMaxHeartRateBpm`, `configuredFtpWatts`, each `ProfileSetting(value: Double, origin: SettingOrigin)` with CONFIGURED or MANUAL origin. Present settings must be finite and >0. Version is an adapter-provided stable identifier/hash of values and origins; effectiveAt stays null when unknown, never fabricated from receipt time. Missing settings remain null. This represents configured facts, not estimates.

All objects are immutable; Java UUID/Instant/Duration and Kotlin values only. No normalization, alignment, freshness policy, automatic merging, persistence or analytics is implemented here. Future normalized RideSample must retain per-channel age; this event boundary does not claim atomic channel alignment. Versions travel with every envelope/profile for replay.

Contract fixture examples: power 0 W is present; HR null/DROPOUT remains absent; receipt t=5 s and t=7.5 s in one epoch give 2.5 s even if UTC goes backwards; the same elapsed numbers across epochs give no duration. A RECORDING event after cold attach represents observed host state only.

## Accepted source boundary — T01d

[ADR 0004](adr/0004-simulation-first-testing.md) defines SourceObservation as the pre-session transport record using ReceiptStamp, SourceRef, normalizationVersion and InputPayload. The Collector creates canonical v1 InputEnvelope after assigning the local session. This keeps a live subscription independent of individual ride boundaries. T01d implements `SourceObservation(stamp, source, normalizationVersion, payload)` and `RideDataSource.start(accept, onStopped): AutoCloseable`. The source is single-use; callbacks are serialized. Cancellation prevents new delivery and terminal acknowledgement follows callback exit. EOF/cancellation report null, failures report their Throwable; no terminal outcome fabricates Idle. The simulator, lazy generators and virtual timeline live only in JVM tests. Existing canonical v1 envelopes are unchanged. Collector lifecycle effects remain T03a.

T02d implements the Karoo source in the opt-in app adapter, preserving canonical v1. It normalizes named SDK fields, recording state and configured profiles before Collector-owned session assignment. Mapping/version and terminal semantics, SDK-fake acceptance and hardware limits are recorded in [T02d](../spdd/tasks/t02d-karoo-adapter-tests.md).

## T03a lifecycle/checkpoint contract

[CollectorLifecycle](../spdd/tasks/t03a-collector-lifecycle.md) assigns sessions from serialized source observations with explicit epoch/order and channel-silence policy. Bounded updates carry canonical envelopes, recording segment indices, session changes, channel coverage breaks and a versioned recovery checkpoint. Unknown state/source loss interrupts; process restoration requires a new epoch and retains linked ambiguity until an observed fresh start. Channel gaps remain distinct from lifecycle ambiguity. Structural trend eligibility does not establish model validity. Latest profile context retains original receipt/source provenance; no synthetic input is manufactured. Persistence/admission wiring remains T03b/T03c/T03e.

## T03c input storage contract

[InputRepository](../spdd/tasks/t03c-transactional-inputs.md) stores bounded source observations (including those outside sessions), their envelope/session/segment association, all lifecycle effects and loss/admission policy metadata in one platform SQLite transaction with the checkpoint. Revision checks serialize competing writers; frozen batch IDs/content make uncertain-response retries idempotent. Already committed batch IDs/receipt keys are rejected before lifecycle replay. Session birth identity and terminal state cannot be overwritten by UUID reuse. `prepareEffect` persists envelope-free silence, terminal and recovery changes. Paged input queries use a bounded local position cursor, optionally filtered by session; receipt epoch/sequence remains the canonical unique key. Schema/codec v1 is the first persistent implementation; future migrations must preserve real v1 data, and unsupported versions have no destructive fallback. No production acquisition driver or result generation schema is introduced.

## T03d result storage

[AnalysisRepository](../spdd/tasks/t03d-analysis-persistence.md) adds idempotent, globally increasing durable generations, newest-requested slots and immutable terminal results with versioned provenance. Schema v2 explicitly migrates real v1 inputs without changing their rows. New requests and terminal writes invalidate affected aggregate memberships transactionally; pending/failed/unavailable never expose prior available results. Membership stores run/generation references only, validates structural session eligibility and cohort consistency, and does not compute trends. Historical result lookup remains available; deletion/rebuild and scientific acceptance remain later work. Both repositories share platform SQLiteOpenHelper; core remains Android-free.
