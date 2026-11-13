# Requirements baseline

Status: DRAFT for architecture review, 2026-11-10. Owner decisions are recorded in [decision log](../spdd/decision-log.md). IDs are stable. Acceptance scenarios are defined in [validation strategy](../algorithms/validation-strategy.md).

| ID | Requirement and acceptance condition | Task / evidence |
|---|---|---|
| R01 | Karoo and FIT adapters feed the same normalized input contract. Equivalent decoded inputs produce equivalent analysis; time provenance remains distinguishable. | T01, T03; V01 |
| R02 | Collect power W, HR bpm, optional cadence rpm, receipt time, recording state and profile snapshots. Mark unavailable fields explicitly. | T01–T03; V02 |
| R03 | Persist bounded batches, recording gaps, pauses and interrupted sessions. Replays are idempotent; ambiguous recovery never silently merges rides. | T02–T03, T08; V03–V05/V15/V17 |
| R04 | Observed per-ride and historical power maxima at 5/30/60/180/300/600/1200/1800/3600 seconds respect actual time and complete coverage. | T04; V06 |
| R05 | FTP model is independent, evidence-gated, replaceable, distinct from CP, with manual baseline and provenance. Insufficient efforts yield unavailable. | T06; V07 |
| R06 | VO₂max uses a separately reviewed cycling model; equations, coefficients, population and required inputs are documented before implementation. Missing evidence yields unavailable or explicit experimental quality. | T05, T07; V08 |
| R07 | Versioned snapshots retain sources, profile/configuration, input hash, method, timestamp, quality and rejection reasons; replay is reproducible. | T03, T05–T08; V09/V16 |
| R08 | Current metrics, last calculation, staleness, reasons and history chart. Separate per-ride estimates from smoothed trend; no fake confidence interval. | T08; V10/V15/V16 |
| R09 | Normal-ride analysis is automatic within a verified recording lifecycle, including when our data field is not visible. Recovery after death records loss; no backfill promise. | T02–T03; V11 |
| R10 | Offline operation, no account/telemetry, minimum permissions, private fixtures excluded from Git. | All; V12 |
| R11 | Pure Kotlin engine runs JVM tests without Android; bounded streaming state and background persistence avoid UI blocking. | T01–T04; V13/V17 |
| R12 | Schema migrations, transactional result writes and deletion invalidate affected historical aggregates deterministically. | T03, T08; V14/V16 |

## Scope interpretation requiring review

Power curve, manual FTP baseline, automated FTP research, VO₂max research and history remain in the requested 0.1 scope. Automated estimators are release-gated research work, not approved equations. Do not silently ship manual FTP as completion of R05 or omit VO₂max while declaring the MVP done.

A small on-device history chart is proposed because the owner explicitly requests it. Full FIT import/backfill UI remains future scope; developer-side FIT decoding/replay for tests is an earlier validation dependency, not a product import feature. A mean or trend over several rides is included in R08 now; advanced longitudinal analytics remains future scope.

## Non-functional acceptance proposals

N01: pending ingestion across queues and in-flight write batches is capped at 4,096 fixed-size event envelopes and 4 MiB of encoded payload, whichever is reached first. Telemetry may occupy at most 4,032 envelopes and 4 MiB minus 64 KiB; reserve 64 envelopes/64 KiB for recording/profile transitions. Each envelope carries at most 1 KiB of validated domain payload; reject oversize inputs with a loss reason. Do not retain SDK objects or create an unbounded upstream channel. Metadata/allocation overhead is additionally measured under N02. These are provisional hard engineering limits; changing them requires a versioned policy and renewed burst checks.

On telemetry saturation, discard incoming telemetry and update a preallocated bounded loss accumulator (first/last receipt time and ingest sequence, saturating count, reason). Preserve ordering of admitted events across the reserved control capacity; a loss range breaks analytics coverage. If a control event cannot be admitted, latch lifecycle-unknown, stop admitting events and mark the session interrupted. Drain/commit retained data plus loss status when storage permits; re-read current host state before restarting acquisition, and treat unresolved continuity as a separate ambiguous session. A state snapshot does not recover missed transitions. Do not report the old session as complete or resume an analysis window across the loss. The accumulator itself must not require queue capacity; if storage remains unavailable or the process dies, recovery must treat the uncommitted interval as unknown.

Flush at least every 5 seconds while recording when storage is healthy; earlier flush on capacity pressure. Crash may lose the uncommitted tail; no zero-loss claim. V17 verifies both telemetry and control exhaustion, queue plus in-flight accounting, delayed/full storage and bounded loss reporting.

N02: memory growth plateaus during a 4-hour session; initial working budget incremental RSS ≤64 MiB and battery increase ≤2 percentage points/hour versus repeated matched baseline rides. These are proposed engineering budgets, not measured results. Device measurements must approve or revise them.

N03: no collection of GPS, sensor serials or unrelated profile data by default. Local raw-input retention defaults to keep-until-user-delete, subject to storage review; reproducibility ends if required inputs are deleted. Notify before disk exhaustion; never fabricate samples.

## Definition of Done

Named slice accepted, linked tests pass, limitations visible, applicable hardware evidence recorded, migrations exercised, docs synchronized. Documentation milestone requires traceability and honest capability states, not a running APK. Full release additionally requires the estimator and device gates in the plan.

## Language

Owner direction: all product and diagnostic UI, documentation, code comments, test names and contribution text use English. UI strings stay in resources; no additional localization is currently planned. This applies to the T03p diagnostic preview and later product screens.

## Owner priority: within-person trends — 2026-11-11

The owner prioritizes useful direction of change within one rider over exact agreement of individual estimates with laboratory values. This clarifies the intended use of R05/R06/R08; it does not require perfect absolute accuracy, select an equation or authorize estimator implementation.

Evaluate longitudinal change detection, repeatability and coverage in comparable rides as primary product questions. Report absolute accuracy separately when claiming FTP W or VO₂max mL/kg/min. Smoothing alone does not validate a trend. Preserve raw observations, refusals and staleness; separate incompatible method/profile/calibration/environment cohorts and distinguish measurement/configuration changes from fitness changes.

An exploratory cycling fitness indicator may be a useful alternative for review; its observable definition, label and acceptance require a concrete specification. Do not silently call an unvalidated power/HR proxy FTP or VO₂max. No numeric trend threshold, observation horizon or confidence percentage is selected by this direction.


## Experimental observational route — T08a1 proposal

[T08a1](../spdd/tasks/t08a1-power-hr-trend.md) proposes a synthetic-only comparison of observed power at comparable HR and local power–HR interpolation, supporting R02/R07/R08/R11 and authority/privacy R03/R10/R12. This is an explicitly experimental observation, not completion or replacement of R05/R06. Physiological VO₂max/FTP remains model_not_approved. Execution and method/product admission require separate decisions; specification/publication only is authorized now.


## T08a1 executed software experiment — 2026-11-11

Owner approved [T08a1](../spdd/tasks/t08a1-power-hr-trend.md) after PR #35 merged. [H01–H07 evidence](../evidence/t08a1-power-hr-trend/README.md) records the synthetic A/B experiment through actual committed replay, scalar-v1/reopen, newest authority, fixed-baseline/raw reports and fault/cohort/resource checks. Ten new core tests and ten API 32/34 preview executions pass; full core 117 passes/one private-FIT skip, debug 92/preview 116, build/lint/release isolation and final focused checks pass. No production method/estimator, chart, schema, store or manifest was added.

Both candidates reproduce injected observational changes but show confounder-driven false directions; neither is selected for physiological interpretation. Software H01–H07 does not close O1–O5, R05/R06/physiological V07/V08, T07/T07b, T08b/c or deferred T02c/T03e/parent T04 live gates. No participant data or hardware evidence. Smoothing and legacy trend-v0 numeric defaults remain unapproved for this route.


## T08a2 qualification and pilot-readiness proposal — 2026-11-11

[T08a2](../spdd/tasks/t08a2-comparable-observations.md): R08 now has a proposed observational qualification route, without completing or replacing R05/R06. The label is Experimental power at comparable HR (W); higher does not automatically mean better fitness.

Specification/publication only is authorized; named execution awaits owner approval. N, windows, HR/drift thresholds, meaningful change, baseline horizon and smoothing remain open with resolution paths. No lawful dataset admitted; real-data/scientific pilot is NO-GO. T05b O1–O5, physiological R05/R06, T02c/T03e/parent T04 live gates and T08b/c remain open. No bike, Karoo or private FIT required.


## T08a2 executed qualification review — 2026-11-11

[Task/evidence](../spdd/tasks/t08a2-comparable-observations.md): The dossier operationalizes the R08 observational route with visit-level qualifications and measured/declaration/unknown evidence. Personal usefulness remains unmeasured; no R05/R06 completion or physiological meaning follows.

Real-data/scientific admission remains blocked; N, qualification thresholds, meaningful change, baseline horizon and smoothing remain unresolved without defaults. T05b O1–O5, T02c/T03e/parent T04 and T08b/c remain open. No new framework, storage, manifests or UI.


## Completed-training timeline clarification — 2026-11-13

Owner clarified R08: the Karoo plugin should present data from completed training sessions and show their evolution on a chronological timeline. The T08b1 explicitly bound single-session saved curve is an intermediate presentation capability, not completion of this history/trend requirement. Specify session eligibility, metric/time semantics, unavailable data and provenance before the next implementation slice; FTP and VO2max method approvals remain separate. See [F08 and owner clarification](../spdd/tasks/t08b1-saved-curve-field.md#f08-authorized-presentation-trial--2026-11-13).
