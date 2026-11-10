# Requirements baseline

Status: DRAFT for architecture review, 2026-11-10. Owner decisions are recorded in [decision log](../spdd/decision-log.md). IDs are stable. Acceptance scenarios are defined in [validation strategy](../algorithms/validation-strategy.md).

| ID | Requirement and acceptance condition | Task / evidence |
|---|---|---|
| R01 | Karoo and FIT adapters feed the same normalized input contract. Equivalent decoded inputs produce equivalent analysis; time provenance remains distinguishable. | T01, T03; V01 |
| R02 | Collect power W, HR bpm, optional cadence rpm, receipt time, recording state and profile snapshots. Mark unavailable fields explicitly. | T01–T03; V02 |
| R03 | Persist bounded batches, recording gaps, pauses and interrupted sessions. Replays are idempotent; ambiguous recovery never silently merges rides. | T02–T03; V03–V05 |
| R04 | Observed per-ride and historical power maxima at 5/30/60/180/300/600/1200/1800/3600 seconds respect actual time and complete coverage. | T04; V06 |
| R05 | FTP model is independent, evidence-gated, replaceable, distinct from CP, with manual baseline and provenance. Insufficient efforts yield unavailable. | T06; V07 |
| R06 | VO₂max uses a separately reviewed cycling model; equations, coefficients, population and required inputs are documented before implementation. Missing evidence yields unavailable or explicit experimental quality. | T05, T07; V08 |
| R07 | Versioned snapshots retain sources, profile/configuration, input hash, method, timestamp, quality and rejection reasons; replay is reproducible. | T03, T05–T08; V09 |
| R08 | Current metrics, last calculation, staleness, reasons and history chart. Separate per-ride estimates from smoothed trend; no fake confidence interval. | T08; V10 |
| R09 | Normal-ride analysis is automatic within a verified recording lifecycle, including when our data field is not visible. Recovery after death records loss; no backfill promise. | T02–T03; V11 |
| R10 | Offline operation, no account/telemetry, minimum permissions, private fixtures excluded from Git. | All; V12 |
| R11 | Pure Kotlin engine runs JVM tests without Android; bounded streaming state and background persistence avoid UI blocking. | T01–T04; V13 |
| R12 | Schema migrations, transactional result writes and deletion invalidate affected historical aggregates deterministically. | T03, T08; V14 |

## Scope interpretation requiring review

Power curve, manual FTP baseline, automated FTP research, VO₂max research and history remain in the requested 0.1 scope. Automated estimators are release-gated research work, not approved equations. Do not silently ship manual FTP as completion of R05 or omit VO₂max while declaring the MVP done.

A small on-device history chart is proposed because the owner explicitly requests it. Full FIT import/backfill UI remains future scope; developer-side FIT decoding/replay for tests is an earlier validation dependency, not a product import feature. A mean or trend over several rides is included in R08 now; advanced longitudinal analytics remains future scope.

## Non-functional acceptance proposals

N01: collector queue capped at 120 seconds of nominal samples; storage flush at least every 5 seconds while recording, with loss accounting on overflow. Crash may lose the uncommitted tail; no zero-loss claim.

N02: memory growth plateaus during a 4-hour session; initial working budget incremental RSS ≤64 MiB and battery increase ≤2 percentage points/hour versus repeated matched baseline rides. These are proposed engineering budgets, not measured results. Device measurements must approve or revise them.

N03: no collection of GPS, sensor serials or unrelated profile data by default. Local raw-input retention defaults to keep-until-user-delete, subject to storage review; reproducibility ends if required inputs are deleted. Notify before disk exhaustion; never fabricate samples.

## Definition of Done

Named slice accepted, linked tests pass, limitations visible, applicable hardware evidence recorded, migrations exercised, docs synchronized. Documentation milestone requires traceability and honest capability states, not a running APK. Full release additionally requires the estimator and device gates in the plan.
