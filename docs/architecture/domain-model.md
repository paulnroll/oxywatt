# Domain model

Status: proposal for owner review; decisions D01–D03 already explicitly supplied. Names describe responsibilities, not mandatory classes/tables.

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
