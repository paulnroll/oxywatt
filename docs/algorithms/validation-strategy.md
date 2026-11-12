# Validation strategy

Status: full scenarios remain planned. T01a has executed contract-level checks for portions of V01/V02/V03 and JVM independence in V13; see [task evidence](../spdd/tasks/t01a-canonical-input.md). T01b adds six passing synthetic replay/oracle tests for these same partial scenarios; see [evidence](../spdd/tasks/t01b-synthetic-replay.md). T01c verifies the FIT side on a synthetic binary fixture with independent canonical expectations. See [FIT replay evidence](../spdd/tasks/t01c-fit-replay.md). These checks do not establish real adapter equivalence, lifecycle state-machine behavior or hardware acceptance. Stable scenario IDs link requirements to future tests. Developer FIT replay does not imply product import or historical API access.

| ID | Scenario and observable oracle | Requirements |
|---|---|---|
| V01 | Equivalent decoded Karoo/FIT values with identical timing policy produce equal analytics; receipt/source provenance remains different | R01 |
| V02 | HR dropout makes HR-dependent analysis unavailable without destroying valid power records; zero W remains a real value | R02 |
| V03 | Manual/auto pause boundaries and initial Idle do not create false completed rides; resume does not bridge windows | R03 |
| V04 | Crash during batch write preserves atomic checkpoint; replay same local sequence does not duplicate rows; repeated equal power is retained | R03 |
| V05 | Restart while recording vs restart into new ride leaves ambiguous sessions separate; unknown interval marked missing | R03 |
| V06 | Constant/ramp/irregular interval signals match independent integral reference; gaps, spikes and sample-frequency changes handled explicitly | R04 |
| V07 | Ordinary submaximal-only activity cannot force FTP; CP synthetic fit is not presented as FTP; manual baseline distinct | R05 |
| V08 | Invalid mass/HRmax, missing HR, unstable or insufficient segments refuse VO₂max; parameter sensitivity recorded | R06 |
| V09 | Same canonical inputs, profile, code/model/preprocessing/config versions yield same result within declared numeric tolerance | R07 |
| V10 | Raw vs smoothed chart, unavailable gaps, stale labels, last calculation and source references render truthfully; no cohort mixing | R08 |
| V11 | Karoo 3 four-hour recording with hidden/no field, screen off and app closed; pause, reconnect, low-memory kill and user force-stop tested separately | R09 |
| V12 | Offline ride works; network permission absent unless justified; no GPS/private fixture leakage; package secrets not logged | R10 |
| V13 | JVM engine independent of Android; long-run memory/queue/battery budgets measured against repeatable baseline | R11 |
| V14 | Upgrade real prior schema, recompute/delete winning contributor, transactional result writes and storage-full recovery | R12 |
| V15 | One ride with two process deaths creates partial/ambiguous fragments but zero trend contributors; an initial attachment while Recording also contributes zero. Three separate observed-start/end sessions can meet the minimum. No automatic merging; ordinary model-qualified sample gaps do not alone imply identity ambiguity. | R03/R08 |
| V16 | Accepted generation 1 followed by unavailable generation 2 removes the old value from current/trend while preserving audit history. Pending/failed generation 2 cannot fall back to generation 1; late generation 1 completion cannot replace generation 2. Transaction retry leaves one authoritative pointer and correct contributors. | R07/R08/R12 |
| V17 | Burst above telemetry count and payload ceilings, including queued plus in-flight writes, remains within N01. Reserved recording/profile events retain admission order; exhausted control capacity interrupts acquisition. Loss accounting stays bounded under stalled/full storage, and recovery or death never bridges the unknown interval. Oversize inputs are rejected explicitly. | R03/R11 |

## Simulation-first acceptance proposal — 2026-11-11

Status PROPOSED. The following criteria extend stable V IDs and remain unexecuted. [ADR 0004](../architecture/adr/0004-simulation-first-testing.md) specifies clock/source semantics.

| ID | Scenario and observable oracle | Requirements / package |
|---|---|---|
| V18 | At 1x, 10x and unpaced execution a fixed manifest yields identical envelopes, activity durations, timer firings and result inputs. Equal timestamps retain input order. Injected UTC jumps leave monotonic durations intact. Silence advances flush/gap timers. At equal timestamps, due timers run before observations in input-index order, including exact timeout/flush boundaries. | R01/R07/R11, T01d/T01e/T03 |
| V19 | Explicit dropout retains null/reason while power zero remains present. A silent gap and pause break coverage. Restart creates a new epoch, with ambiguous fragments excluded. Duplicate start fails, close idempotently requests cancellation, partial-start failure releases registrations, and onStopped acknowledges cleanup after any in-flight callback exits. EOF/failure while Recording interrupts continuity. One source can span two known rides while the Collector assigns distinct sessions. | R02/R03/R11, T01d/T03a |
| V20 | The real Karoo adapter under FakeKarooSystem/Host maps named power/HR/cadence fields, initial and subsequent ride states and profiles to expected canonical inputs. Missing and invalid values remain distinct, repeated values retain keys. startFit/cancel, living-process reconnection and teardown leave zero active consumers and no duplicate registration. | R01/R02/R09/R11, T02d |
| V21 | A lawfully sourced real FIT plus independent expected event/value table survives pinned decode/export/replay with original FILE_RECORD times and declared virtual acquisition mapping. CRC corruption/truncation fails explicitly. Missing/regressing time has a diagnostic. Pause/stop variants and EOF retain uncertainty. | R01/R02/R07/R10, T01e |
| V22 | Simulation origin and versions remain in run manifests, fixed seeds reproduce hashes, synthetic results are isolated from real history/trends, and release dependency inspection excludes SDK fakes, emulator stand-ins and simulation controls. | R07/R08/R10/R11, T01d/T02d/T08 |
| V23 | Debug preview after T03a–d projects actual demo Collector/repository state: received values/age/missing reasons versus committed counts, pending/loss and storage failures. Activity recreation keeps one run, source EOF/restart cannot fabricate a completed ride, and 1x/10x preserve acquisition semantics. Demo database/origin and release artifacts are isolated; a Karoo preview claim requires identified-device screenshots and manifest/version evidence. | R02/R03/R07/R10/R11, T03p |

| Layer | Acceptance before claiming the layer passes | What the evidence settles |
|---|---|---|
| Pure JVM source and generator tests | V18/V19, fixed manifest and independently enumerated event tables, all signal shapes and fault combinations. No wall-clock sleep is required. | Source contract, deterministic ordering/time and scenario reproducibility. |
| Pure JVM Collector and numerical regressions | V01–V09/V15–V19 as applicable. Independently calculated constant/interval/ramp anchors and coverage exclusions. Compare exact inputs and declared numerical tolerances. | Software policy and numerical conformance after those packages exist. |
| Real FIT decoder bridge | V21 and V18 at all replay speeds. Decoder regenerates fixture export before JVM replay. Record fixture hash, permission, preprocessing and timestamp policy. | Real-file decoding and canonical semantics for the accepted fixture. |
| SDK integration with Robolectric | V20/V22 at API 32 and the additional supported API selected by the compatibility spike. Feed the same scenario through the SDK fake and direct source, compare canonical values/keys and expected provenance. Repeat without leaked consumers. | SDK-facing mapping and controlled service/cleanup behavior. |
| Real repository integration | V04/V05/V09/V14/V16/V17 with actual transactional repository on Android test runtime or emulator. Fail writes between commit/checkpoint, retry, restart, delete and supersede concurrently. Assert N01 bounds including in-flight payload/count and control reserve. | Persistence and overload behavior under injected faults. |
| Emulator end-to-end, optional | Isolated appstore stand-in, debug app and bounded scenario. Trace start → persist → query, cancellation and process recreation. Verify no fake package in release artifact. | Android wiring under the emulated host. |
| Karoo replay-tool smoke, optional | Verify helper APK/tool/OS and permissions first. Pair virtual channels, compare 1x availability/dropout transitions against manifest, restore configuration. Record accelerated tests as wiring/load evidence. | Integration with the real host through virtual devices. |
| Real Karoo and real sensors | Finish V02/V11/V13 with actual power/HR/cadence delivery, field hidden/absent, screen off, four hours, manual/auto pause, reconnect, low-memory kill and force-stop separately. Measure memory/battery with matched baselines. | Radio delivery, real host/OS behavior and resource budgets on the tested build. |
| Scientific study | Existing T05–T07 method selection and criterion validation, participant-level splits and matched references. | Physiological applicability/accuracy within the studied population. |

All layers report evidence separately. Real sensors, ANT+/BLE radio behavior, sensor delivery latency, OS scheduling/termination, automatic host startup, measured battery and physical-device memory budgets remain hardware risks. A fake can drive an error path without proving its frequency or recovery on Karoo. A virtual maximal-effort trace can exercise eligibility and equations while FTP/VO₂max validity still requires scientific evidence.

## Test layers

Pure JVM unit/property tests cover time integration, normalization, lifecycle, model eligibility and trend math. Reference fixtures are independently calculated, not snapshots of the implementation. Repository integration tests exercise transactions/restart/migrations; Android tests cover adapter cleanup and UI state. Real Karoo tests establish lifecycle and performance. Do not equate JVM coverage with hardware validation.

## Datasets and privacy

Use deterministic synthetic traces first. Owner FITs stay in ignored `fixtures/private/`; derive minimal anonymized fixtures only with redistribution permission, stripping route, serials and identifying timestamps. Decoder must retain timer events, missing sentinels, units, timestamp semantics and source provenance. Choose parser/license in T01. No such FIT files have been supplied or processed yet.

## Physiological validation

Separate numerical conformance, robustness, test–retest stability and criterion validity. A lab reference must match athlete, modality and time window; ordinary FIT alone lacks measured VO₂max ground truth. Use participant-level training/validation splits, held-out comparison, MAE/RMSE/bias/limits of agreement and sensitivity to HRmax/mass/efficiency. Specify numerical acceptance targets before the experiment based on intended use; no scientifically justified accuracy threshold is selected yet. Confidence intervals require calibration evidence; repeated segments from one ride are not independent subjects.

No compatible licensed paired field-ride/CPET dataset secured. Record this explicitly in any validation report. Comparison with a Garmin display is exploratory only. Scientific gates remain open until evidence and model limitations are reviewed.


T03p software implementation (2026-11-11, owner-approved after PR #17): isolated debug-derived preview consumes actual T03a/b/c pipeline and schema v2. Shared test fixtures and stream FIT reader remain outside release. Received versus committed values, virtual ages, pending/loss, interruption and storage errors are inspectable. Last committed receipt time is explicitly distinguished from an unavailable durable transaction timestamp. See [T03p evidence](../spdd/tasks/t03p-diagnostic-preview.md). V22/V23 software checks do not close the on-Karoo screenshot/device claim, T02 or live T03e.


V22/V23 on-Karoo follow-up (2026-11-11): [device evidence](../evidence/t03p-karoo-2026-11-11/README.md) records isolated preview installation, synthetic demo/FIT SQLite/readout checks, actual screenshots, Activity.recreate/reopen and force-stop recovery. Android FIT parsing regression was red then green on the device. Loss/storage fault and speed invariance retain software-only evidence; low-memory death and T02/T03e gates remain open.


V02/V11 partial real-HR evidence (2026-11-11): [T02b](../spdd/tasks/t02b-karoo-probe.md) records real channel presence, owner-confirmed transmitter off/on with unavailable/searching then present, and short presence observation after a screen-sleep command. Older installed probe only; no numeric HR validation, display-state confirmation, native recording, current-adapter verification, SQLite live persistence or sustained T02c/T03e acceptance is claimed.


Proposed T04a software acceptance (2026-11-11): [task](../spdd/tasks/t04a-power-curve-engine.md) refines V06/V09 and software-only V13/V18/V19/V22 with independent confirmed-interval integrals, fractional/expiration boundaries, zero/missing/discontinuity policies, deterministic timing and a hard retained-interval cap. At the proposal baseline no numerical test had run for T04a; executed evidence follows below, while canonical-to-interval replay and Karoo end-to-end curve remain later T04b/hardware acceptance.


T04a executed software evidence (2026-11-11): [task](../spdd/tasks/t04a-power-curve-engine.md) records 14 passing confirmed-interval tests: constant/ramp/fractional anchors, six seeded exact-grid oracles, virtual delivery-speed equality, gaps/identity/rejection/ties, repeated resource limits and four simulated hours. Full core: 82 passes plus one private-FIT skip. V06/V09 and software V13/V18/V19/V22 remain scoped to the pure interval engine, not canonical preprocessing, device resources or live acceptance.

## Proposed T04b1 software acceptance

[T04b1 cases B1–B7](../spdd/tasks/t04b-persisted-curve.md) map exact canonical/effect interval tables to V02–V06/V15/V18/V19, independent integrals and speed/hash equality to V06/V09/V18/V22, actual SQLite/revision/codec/generation fault tests to V04/V05/V09/V14/V16, isolated durable preview readout to V12/V22/V23 and conservative computational bounds to software V13/V17. These cases are planned, unexecuted and require no bicycle or Karoo. Later live acquisition, physical-device resource budgets and parent T04 acceptance remain open; neither an older probe nor a synthetic preview substitutes for current-adapter evidence.

T04b1 implemented after owner approval following merged PR #23: [terminal synthetic persisted curve](../spdd/tasks/t04b-persisted-curve.md). Committed revision-bounded effects are converted with preview-silence-v1 hold/expiry to T04a; one typed T03d generation atomically stores all durations and partial/resource provenance. Existing preview reads saved results after reopen; synthetic curves cannot enter aggregate membership. Software evidence: core 90 passes/one private-FIT skip, debug 92 passes, preview 103 passes; builds/lint/release isolation pass. Dense scans have an explicit conservative pre-call work cap. No live T03e, device action, physiological model or private FIT read; T02c and parent T04 live acceptance remain open.


## T04b2 proposal — 2026-11-11

Planned B2-01–08 extend V06/V09/software V13 with four-hour 1 Hz/irregular traces, dense-then-long stress, exact/high-precision independent oracles, and bounded operations/staging. V18/V22/V23 require each representative canonical fixture through persisted replay, atomic generation, reopen and preview readout at 1x/10x/unpaced. V03/V05/V19 retain pause/missing/expiry/loss/restart exclusions; V14/V16 retain compatibility and no-fallback. Baseline cost investigation ran; optimized and end-to-end long-session acceptance has not. Live T02c/T03e/parent T04 remain open. See [T04b2 specification](../spdd/tasks/t04b2-long-variable-scan.md).


## Approved T04b2 implementation — 2026-11-11

Owner approved [T04b2](../spdd/tasks/t04b2-long-variable-scan.md) after specification PR #25 merged. The existing core/power now uses bounded persistent AVL range means and charged work-v2, with exact candidates/coverage/tolerance retained. Canonical hold-v1, the existing logical slot, scalar-v1/curve-v2, atomic newest-generation storage and durable preview readout remain in the same flow. Independent exact oracles and long synthetic speed/reopen checks provide software evidence; see task for final results and resource bounds. This replaces T04b1's conservative quadratic reservation for new calculations, without rewriting old records. No private FIT, live acquisition or physiological model; T02c/T03e and parent T04 live acceptance remain open.

## T05a proposed software traceability

[T05a A01–A07](../spdd/tasks/t05a-fixture-reproducibility.md) refine existing V IDs: rights/privacy and FIT references (V09/V12/V21/V22), generator mutation/time ordering (V01/V02/V09/V18/V19), separate absent/invalid profiles and no approved models (V07/V08/V09), independent numerical/coverage tables (V03/V06/V19), real persisted/reopened three-speed equality (V04/V09/V14/V18/V22/V23), refusal/ambiguity/no-fallback (V05/V12/V14/V15/V16/V22), and legacy/new hash vectors plus execution-metadata separation (V09/V18/V22). These are planned, unexecuted software checks; they do not complete physiological V07/V08, T05b evaluation/sensitivity/go-no-go or later live V11/V13. No private FIT read or new dataset claim.


## T05a implementation — 2026-11-11

Owner-approved T05a after PR #27: [software evidence](../evidence/t05a-software-2026-11-11/README.md) maps A01–A07 to actual fixture/profile/hash/refusal tests and native persisted virtual-timer replay. Numerical expectations are synthetic only; V07/V08 physiological acceptance and V11/device V13 remain open. Existing private-FIT opt-in stays disabled.


## T05b protocol proposal — 2026-11-11

[Canonical T05b specification](../spdd/tasks/t05b-evaluation-protocol.md). B01/B02 distinguish FTP protocol references and cycling CPET VO₂max (R05/R06; V07/V08/V09). B03 covers rights/person/modality/time/protocol qualification (V07/V08/V09/V12); B04 participant independence and frozen splits (V07/V08/V09); B05 error/agreement and coverage/refusal denominators (V07/V08/V09); B06 conditional sensitivity (V07/V08/V09/V18/V19); B07 independent decision gates (V07/V08/V09); B08 existing software reuse and separate live status (V06/V09/V12/V14/V16/V18/V22/V23). All are proposed review/exercise criteria, not executed scientific validation. Numerical conformance, robustness, deterministic repeatability, physiological test–retest and criterion validity must be reported distinctly. No universal thresholds, participant N or sensitivity bounds are approved.


## T05b executed protocol exercises — 2026-11-11

Owner approved T05b after merged PR #29. [B01–B08 report](../evidence/t05b-protocol-review/README.md) completes the bounded documentation exercise: separate FTP/VO₂max estimands, ten invented qualification cases, participant split/freeze rehearsal, reconciled coverage/refusal/error arithmetic, conditional sensitivity and explicit O1–O5 decisions. Self-reviewed evidence is ready for owner review; it is not a physiological study. Both experiment admissions remain NO-GO; no lawful matched dataset, justified scientific thresholds/sample plan or method selected. T06/T07 retain independent research/selection and later implementation; no automatic authorization follows. T05 protocol/reproducibility deliverables are now recorded, while parent scientific acceptance and deferred T02c/T03e/parent T04 live gates remain open.

V08 clarification: mass, HRmax and HR availability/sensitivity checks apply when required by the audited candidate, not as invented universal inputs for every possible VO₂max method. T05b supplies protocol/review evidence only; actual method-specific numerical, refusal and sensitivity execution remains T07.


## T06a scope proposal — 2026-11-11

[Named T06a scope](../spdd/tasks/t06a-ftp-method-audit.md) defines a bounded primary-source FTP audit after merged T05b PR #30. F01–F08 map R05/R07 (plus input/privacy R02/R10) to V07/V09 and conditional V02/V12/V18/V19: search log, equation/protocol/population/reference audit, current-input compatibility, conditional sensitivity, rights/data availability and a reviewable recommendation. Named audit execution approval is pending; this specification chooses no method. O1–O5, experiment admission, separate T06b implementation and later live gates remain explicit. T07a stays independent. No application code, private FIT, dataset download, device action or new framework.


## T06a executed audit — 2026-11-11

Owner approved T06a after specification PR #32 merged. [F01–F08 report](../evidence/t06a-ftp-method-audit/README.md) records the bounded primary-source audit, source-access/correction limits, input compatibility, conditional sensitivity and FTP O1–O5 decisions. Recommendation is to retain model_not_approved for automatic FTP from ordinary rides; no audited candidate qualifies on present evidence. An optional controlled-protocol investigation needs a separate scope decision and cannot silently replace ordinary-ride intent. No method, dataset, estimator or experiment is admitted; T06b and independent T07a remain separate. Source numerical results are centralized in the report, not new product accuracy thresholds. R05/physiological V07 and later live acceptance remain open.


## T07a executed independent audit — 2026-11-11

Owner authorized T07a following T06a; PR #33 merged during work and local main/branch updated. [G01–G08 report](../evidence/t07a-vo2max-method-audit/README.md) centralizes primary equations, protocol/population/reference and rights/access findings, input compatibility and conditional sensitivity. Recommendation: retain model_not_approved for ordinary-ride VO₂max on present evidence. VO₂max O1–O5 remain unresolved/blocked; no method, experiment, dataset or T07b implementation admitted. Any optional controlled-protocol direction requires a separate scope decision. FTP remains independent; R06/physiological V08 and deferred T02c/T03e/parent T04 live acceptance remain open. No application code, private FIT, dataset download, device action or framework.

## Trend-priority intended use — 2026-11-11

Owner prioritizes within-person direction/change over exact absolute laboratory agreement (R05/R06/R08). T05b O2 must now define justified change-detection/repeatability/coverage criteria; no targets are invented here. Evaluate signed/magnitude error of paired changes against repeated references, false increase/decrease and detection delay under a frozen design, with participant-level independence and clustered repeated observations. A cross-sectional agreement result or a smoothed chart alone does not validate longitudinal change. Retain absolute-unit agreement as a separate claim and explicit exploratory labeling for a nonphysiological proxy. V08/V09/V10/V15/V16 remain evidence-specific, not passed by this decision.


## T08a1 proposed observational experiment — 2026-11-11

[H01–H07 acceptance and detailed R/T/V mappings](../spdd/tasks/t08a1-power-hr-trend.md) extend existing V scenarios without claiming them complete. H01 independent A/B numerical oracles (V02/V09/software V13; V08 claim boundary); H02 joint coverage, lag/drift/transition/context/profile refusal (V02/V03/V05/V09/V15/V19); H03 raw/baseline/coverage/false-direction/delay report (V09/report-only V10/V15); H04 three-speed persisted/reopened equality (synthetic V01/V09/V18/V22); H05 newest-requested/no-fallback and report-generation consistency (V09/V12/transaction V14/V16/V22); H06 bounded state/hash/legacy/isolation (V09/V12/software V13/V22); H07 documented evidence and claims review (V08 limits/V09/report-only V10). All are planned, not executed.

The experiment's repeatability means deterministic software replay. Physiological test–retest, meaningful-change sensitivity, false increase/decrease and delay require a frozen longitudinal design and repeated independent references under O1–O5. Null/confounder/step/reversal synthetic recipes establish only injected observation behavior. Numeric eligibility windows, drift cutoffs, baseline count/horizon, q, N and meaningful-change thresholds remain open for real use; explicit invented recipe parameters do not set product defaults. V07/V08 scientific, V10 chart and V11/device V13 plus T02c/T03e/parent T04 remain open.


## T08a1 executed software experiment — 2026-11-11

Owner approved [T08a1](../spdd/tasks/t08a1-power-hr-trend.md) after PR #35 merged. [H01–H07 evidence](../evidence/t08a1-power-hr-trend/README.md) records the synthetic A/B experiment through actual committed replay, scalar-v1/reopen, newest authority, fixed-baseline/raw reports and fault/cohort/resource checks. Ten new core tests and ten API 32/34 preview executions pass; full core 117 passes/one private-FIT skip, debug 92/preview 116, build/lint/release isolation and final focused checks pass. No production method/estimator, chart, schema, store or manifest was added.

Both candidates reproduce injected observational changes but show confounder-driven false directions; neither is selected for physiological interpretation. Software H01–H07 does not close O1–O5, R05/R06/physiological V07/V08, T07/T07b, T08b/c or deferred T02c/T03e/parent T04 live gates. No participant data or hardware evidence. Smoothing and legacy trend-v0 numeric defaults remain unapproved for this route.


## T08a2 qualification and pilot-readiness proposal — 2026-11-11

[T08a2](../spdd/tasks/t08a2-comparable-observations.md): I01–I07 are document review cases mapped to existing V IDs, not executed tests. Report deterministic replay, independently acquired observation repeatability, sensitivity, native/intersection coverage, reference-qualified false direction and censored detection delay separately. Missing reference truth means not assessable.

Specification/publication only is authorized; named execution awaits owner approval. N, windows, HR/drift thresholds, meaningful change, baseline horizon and smoothing remain open with resolution paths. No lawful dataset admitted; real-data/scientific pilot is NO-GO. T05b O1–O5, physiological R05/R06, T02c/T03e/parent T04 live gates and T08b/c remain open. No bike, Karoo or private FIT required.


## T08a2 executed qualification review — 2026-11-11

[Task/evidence](../evidence/t08a2-comparable-observations/README.md): I01–I07 document review is complete: native/intersection coverage uses explicit set denominators; visit differences remain descriptive under serial dependence. False direction and delay are not assessable without independent labels/onset and a frozen declaration rule. No participant or application tests were executed.

Real-data/scientific admission remains blocked; N, qualification thresholds, meaningful change, baseline horizon and smoothing remain unresolved without defaults. T05b O1–O5, T02c/T03e/parent T04 and T08b/c remain open. No new framework, storage, manifests or UI.


## T08a2 Salford source-index evidence limit — 2026-11-12

[Time/alignment inspection](../evidence/t08a2-comparable-observations/README.md#source-specific-timealignment-inspection--2026-11-12) extends I02/I03/I05–I07, R02/R07/R10 → V02/V09/V12 as documentation/source evidence only. Archive/extracted hashes, extent counts and BIFF/shared formulas were inspected. Formula-derived duration is not independent measured time; 718 consistent P/HR grid hypotheses do not pass synchronization, lifecycle or real-use qualification. Reconstructed index span and nominal represented duration must remain distinct from confirmed joint seconds, time-in-ride, canonical V09 replay and A/B coverage. No code tests or physiological V07/V08 executed; no qualification/default thresholds selected.


T08a2 owner-approved source-index availability report: [evidence](../evidence/t08a2-comparable-observations/README.md#executed-source-index-availability-report--2026-11-12). R02/R07/R10 → V02/V09/V12, I02/I03/I05–I07 are source/document review only. All 1,344 dispositions reconcile; independent excluded-prefix subtraction matches the 718-column row/HR-zero totals, and ledger/count/quartile checks pass. No timestamp/lifecycle/window, A/B, canonical or physiological test executed. Row counts and positive-HR fractions are conditional availability measures, not confirmed time coverage or eligibility.


T08a2 [source recording/export review](../evidence/t08a2-comparable-observations/README.md#executed-source-recordingexport-semantics-review--2026-11-12) maps I01–I03/I05–I07 to R02/R07/R10 → V02/V09/V12 source/document review; R06/R08 → V08/V10 claim limits only. Independent nominal cadence, generic storage averaging and documented export time fields are distinct from archive-specific clock/support/synchronization evidence. Unverified original rows/settings/conversion and artifact rules keep the comparative pilot blocked. No new numerical policy, application/replay/A/B or physiological test executed.


## T08a3 proposed source-index experiment — 2026-11-12

[T08a3 J01–J08](../spdd/tasks/t08a3-salford-index-ab.md) were initially proposed as report/source cases: J01–J03 source identities, per-column HR support and timing/artifact limitations (R02/R07/R10 → V02/V09/V12, R06 → V08 claim limits); J04 full parameter/freeze decision before numerical work; J05–J06 conditional same-pool A/B arithmetic/refusals, actual retained distributions, native/intersection counts and fixed contrasts/sensitivity (R02/R07/R08 → V02/V09/report-only V10); J07 report revision/no-fallback/reproduction/origin checks (R07/R10 → V09/V12, no new persistence pass); J08 actual verification/claim/privacy review. At proposal time neither unit was approved; the subsequently approved support execution is recorded below. Numerical A/B remains unapproved. No physiological, canonical replay, hardware or product gate closes; synthetic recipe thresholds are not real-use defaults.


T08a3 support unit owner-approved/executed: [J01–J03/J08 evidence](../evidence/t08a2-comparable-observations/README.md#t08a3-executed-support-discovery--2026-11-12). Independent scalar-column reads verify all 718 histograms, 61,601 CDF points, 68,352 pair intersections and bracketing diagnostics; source totals reconcile and deterministic rerun is byte-identical. R02/R07/R10 → V02/V09/V12 source/report only, R08 → report-only V10, R06 → V08 claim limits. Complete qualified-segment configuration remains unsupported: J04 NO-GO/not frozen, J05–J07 unperformed/unapproved. No A/B, canonical/persistence/device or physiological acceptance newly passes.


## T08a4 proposed exact-HR stored-row checks — 2026-11-12

**Historical proposal snapshot; the executed K01–K06 status follows below.**

[T08a4 K01–K06](../spdd/tasks/t08a4-salford-exact-hr-rows.md) define planned source/report arithmetic checks: fixed identities and count reconciliation, independent real-source rational mean/difference references, exact support/disposition enumeration, byte-identical semantic rerun, newest/no-fallback/required-input failure behavior and privacy/claim review. R02/R07/R10 → V02/V09/V12 source/report only; R08 → report-only V10; R06 → V08 claim limits. No checks executed for the numerical proposal. No canonical/persistence/hardware or physiological pass; n=1 remains a disclosed raw descriptor, never validated minimum evidence.


## T08a4 executed stored-row description — 2026-11-12

Owner explicitly approved the complete T08a4 configuration and numerical execution. [Aggregate results and verification](../evidence/t08a2-comparable-observations/README.md#t08a4-executed-exact-hr-stored-row-description--2026-11-12): fixed-primary 197 columns, all 201 exact HR levels, 15,189 raw means (2,271 one-row) and 1,074,462 same-level differences (187,894 with a one-row side); every unavailable cell retained. Equal-index pairing remains assumed, weights are equal stored rows and artifacts remain uncleaned. No anchor, A/B, qualified segment, cross-HR scalar, trend or physiology. Independent source/rational checks and full final rerun provide report-only evidence; exact data/scripts/results retained outside Git. T08a3 J04–J07 and all live/scientific/product gates unchanged. No application code authorized. Subsequent report-publication authorization is recorded in the T08a4 task.


## T08a5 approved 115 bpm stored-row descriptor — 2026-11-12

**Authorization/freeze snapshot; completed execution evidence follows below.**

Owner approved the described exact-at-115 / nearest-bracket linear interpolation variant, now reserved as [T08a5](../spdd/tasks/t08a5-salford-anchor115.md) after checking the unused ID. Freeze the full rules and contributor endpoints before numerical execution. One output per fixed-primary column; direct/interpolated methods, counts, ranges and zero/quality flags stay explicit. Pairing and local linearity remain development assumptions, no A/B/trend/physiology or app implementation. L01–L06 define source/report arithmetic, complete denominators, independent checks, rerun/current-input/no-fallback/privacy evidence; pending execution. No new acquisition/contact or publication authorized; T08a3 and all live/scientific/product gates unchanged.


## T08a5 executed 115 bpm descriptor — 2026-11-12

[Executed results and verification](../evidence/t08a2-comparable-observations/README.md#t08a5-executed-115-bpm-stored-row-descriptor--2026-11-12): all 197 fixed-primary outputs available, 188 exact and nine interpolated, 206 HR nodes and 3,226 actual source-row contributors. Seventeen columns have a one-row used point; 132 include Power-zero in a used point, all retained. Complete independent source/rational checks, full byte-identical rerun and current/no-fallback/interruption checks PASS. Exact versus interpolated evidence remains explicit; local linearity, index pairing, artifact and context limits unchanged. L01–L06 report/source evidence only, no original A/B, chronological/physiological trend, app change or Git publication. Detailed scalars/contributors/scripts remain outside Git; all live/scientific/product gates stay open.


## T08a6 approved single-row influence diagnostic — 2026-11-12

**Authorization/freeze snapshot; executed evidence follows below.**

Owner approved the described [T08a6](../spdd/tasks/t08a6-salford-row-influence.md) diagnostic before ID reservation. Freeze all used T08a5 rows/nodes/weights/methods, omit each used row once without altering source, record exact W changes or required-node-empty refusal and complete per-column envelopes. M01–M06 require independent recomputation, full unavailable/partial denominators, deterministic rerun/current-input/no-fallback/interruption and privacy checks; execution pending. This is deterministic sensitivity, without physiological uncertainty or arbitrary adequacy thresholds. Recommend ending the present fixed-table numerical sensitivity route here, then deciding a separately scoped timing/context evidence route. No publication or further automatic experiment sequence authorized.


## T08a6 executed single-row influence — 2026-11-12

[Actual results and verification](../evidence/t08a2-comparable-observations/README.md#t08a6-executed-single-row-influence-diagnostic--2026-11-12): all 3,226 deletions enumerated, 3,204 numeric and 22 required-node-empty refusals; all 197 envelopes accounted for (180 complete, one partial, 16 all-unavailable). Exact independent full-recomputation checks, byte-identical rerun and current-input/no-fallback/interruption checks PASS. Deterministic sensitivity is not calibrated uncertainty or physiological repeatability; no adequacy threshold, cleaning or ranking. M01–M06 source/report only. Recommend ending this fixed-table numerical sensitivity route and deciding a separately scoped timing/context evidence route. Detailed W/identities/scripts stay outside Git; earlier local T08a5 work and all scientific/live/product gates preserved. No publication authorized.
