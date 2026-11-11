# T05b protocol review and worked exercises — 2026-11-11

Owner approved named T05b after merging specification PR #29. This is execution evidence for [B01–B08](../../spdd/tasks/t05b-evaluation-protocol.md), not a physiological study. All people, visits, rights statuses, references and predictions below are invented paper examples. No real dataset, private FIT, hardware, estimator or application test was used. No scientific source/equation is newly selected or claimed validated.

## B01/B02 — Separate estimand sheets

| Field | FTP | VO₂max |
|---|---|---|
| Question | Can an independently selected candidate infer protocol-defined FTP from qualified cycling observations, with useful coverage and individual agreement? | Can an independently selected candidate infer independently measured cycling CPET VO₂max with useful coverage and individual agreement? |
| Target units | W | Relative mL/kg/min; absolute L/min separately when available |
| Reference evidence required | Named/versioned FTP effort protocol, target definition, maximal-effort checks, equipment/calibration, conversion convention if used and date | Cycle CPET stages, gas-system calibration, averaging, maximality/verification criteria, measured outcome/date and dated mass for relative normalization |
| Independence | Separate reference acquisition; using the same reference effort as prediction input changes the claim to protocol replication | Measured gas exchange independent of candidate predictions; shared mass normalization recorded as possible error coupling |
| Rejected substitutes | CP, configured/manual FTP, vendor estimate and unqualified observed P20 | Vendor value, inferred oxygen cost, fixed efficiency, modeled HR response, estimated FTP; VO₂peak is not silently relabeled VO₂max |
| Intended support | Qualified cycling population/context to be established; no general-population claim | Qualified cycling population/context to be established; no cross-modality claim |
| Method inputs | Not selected; power/effort requirements and any optional inputs audited in T06 | Not selected; exact HR/power/mass/calibration/demographic/gas requirements audited in T07 |
| Present reference state | Missing; no lawful paired reference dataset secured | Missing; no lawful paired reference dataset secured |
| Accuracy/coverage criterion | O2 unresolved, not assessable | O2 unresolved, not assessable |
| Admission outcome | NO-GO for a physiological experiment | NO-GO for a physiological experiment |

These sheets complete protocol questions and explicit exclusions; they do not fill unresolved O1–O5 with defaults.

## B03 — Paper qualification exercise

Primary disposition is assigned at the first failed gate in this order: rights → reference identity/modality/protocol/time/population → method inputs → prediction/refusal/execution. Additional reasons remain flags. This ordering is for reporting reconciliation, not a new application policy. A reference-qualified pair here includes population support; reports must disclose that qualification definition.

| Invented case | Declared evidence / deliberate defect | Expected disposition | Claim allowed |
|---|---|---|---|
| Q1 | Invented constant-power oracle, explicit synthetic recipe and declared processing/redistribution permission | Synthetic numerical exercise only | Software conformance to its hand table, never criterion validity |
| Q2 | Unknown processing permission despite otherwise complete fields | Rights-blocked before processing | None; hashing the declaration cannot cure missing rights |
| Q3 | Ride and reference belong to different invented persons | Reference-ineligible: person mismatch | No criterion pair |
| Q4 | Cycling ride paired to running reference | Reference-ineligible: modality mismatch | No cycling criterion pair |
| Q5 | FTP label lacks test protocol; CP is the only supplied measurement | Reference-ineligible: protocol/target missing | CP observation only, no FTP reference |
| Q6 | Dates/linkage cannot establish permitted temporal pairing | Reference-ineligible: ambiguous timing | No pair until O1 resolved; no invented maximum interval |
| Q7 | Subject outside the candidate's eventually documented population | Reference-ineligible: unsupported population | Exploratory description only; excluded from supported-use accuracy |
| Q8 | A future audited candidate's required input X is absent | Input-ineligible, conditional on X actually being required | Reference may be qualified but no prediction; do not assume mass/HR universally required |
| Q9 | Permission permits local analysis but redistribution is unknown | Local use only after actual evidence review; no public records | Safe aggregate publication only if separately permitted; declaration alone insufficient |
| Q10 | CPET documentation establishes only VO₂peak | Reference-ineligible for a VO₂max estimand | Explicit VO₂peak description; separate revised study question needed |

All ten expected dispositions were reviewed against the protocol. None supplies measured physiological evidence. Even Q1 is not a legal audit of third-party rights; only invented contributions are used here.

## B04 — Participant independence and freeze rehearsal

| Invented participant | All linked records | Assignment | Reference/calibration boundary |
|---|---|---|---|
| P1 | ride-a, ride-b, visit-a, all segments/augmentations | Development only | May inform fitting/calibration within development |
| P2 | ride-c, visit-b, a restarted fragment and duplicate export | Development only | Duplicate export is one underlying observation, not a new person/pair |
| P3 | ride-d, ride-e, visits-c/d, all derived segments | Locked evaluation only | Neither visit outcome available for calibration/tuning |
| P4 | ride-f, visit-e | Locked evaluation only | Same boundary as P3 |
| P5/P6/P7/P8/P9 | One screened record each in the B05 toy ledger | Locked evaluation only | Same boundary as P3; exclusions do not move a person to development |

Assignment is explicit, not randomized; a seed is not applicable to this toy table. This is not a recommended split ratio or sample size. P3's two rides are one participant. P2's fragment is never merged as certain continuity. Suspected duplicate identity across splits would block evaluation pending resolution. Personal calibration for an evaluation participant would require a prespecified independent pre-evaluation input/acquisition plan; none is assumed here.

The toy table passes the no-crossing/no-reference-for-calibration review. There are two development people and seven screened evaluation people; record and segment totals never become independent N. No real held-out outcome was opened.

Freeze rehearsal (all required before an actual experiment):

| Frozen field | Toy rehearsal status / real blocker |
|---|---|
| Track question, target units and evidence exclusions | B01/B02 filled; exact reference protocol/verification and temporal pairing O1 unresolved |
| Intended use, supported population, actual rights and dataset revision | Invented examples only; O2/O4 unresolved for reality |
| Participant identity audit, assignments and seed if randomized | Toy assignment checked; actual data and O3 allocation unresolved |
| Candidate equation/input/calibration, code, preprocessing, profile and existing input identities | No candidate/data; O5 unresolved; no fake production revisions or private hashes |
| Primary endpoint, weighting, agreement/interval assumptions, precision and sample plan | Toy arithmetic below only; real O2/O3 unresolved |
| Qualification/refusal/missingness rules, primary disposition order and denominators | Toy order demonstrated; actual method requirements O5 unresolved |
| Sensitivity ranges/interactions and numerical tolerance | B06 conditional checklist; O5 unresolved |
| Analysis approval date, dataset lock and held-out access controls | No real lock claimed; owner approved this exercise, not a real experiment |
| Deviations and changes after outcome access | None for this paper exercise; real changes require exploratory revision/fresh evaluation |

## B05 — Hand-worked reporting and denominator exercise

Two separate toy target columns illustrate arithmetic only. They are not paired physiological measurements or a shared candidate. Placeholder predictions are authored numbers, not model output. Assume declared reference/input qualification solely to exercise accounting; no actual reference protocol or thresholds are thereby approved.

| Record / person | Primary disposition | Toy FTP reference → prediction (W) | Toy relative VO₂ reference → prediction (mL/kg/min) |
|---|---|---|---|
| E1 / P3 | Prediction | 200 → 210 | 40 → 41 |
| E2 / P3 | Prediction, repeat person | 220 → 210 | 42 → 41 |
| E3 / P4 | Prediction | 250 → 270 | 50 → 52 |
| E4 / P5 | Input-ineligible: required X missing | Invented reference qualified; no prediction | Same reporting disposition, not an actual shared requirement |
| E5 / P6 | Method abstained | Invented reference/input qualified; no prediction | Same reporting disposition |
| E6 / P7 | Execution failed | Invented reference/input qualified; no prediction | Same reporting disposition |
| E7 / P8 | Reference-ineligible: wrong modality | Excluded from accuracy denominator | Excluded from accuracy denominator |
| E8 / P9 | Rights-blocked | No processing; no reference qualification claimed | Same reporting disposition |

Reconciliation, for each toy track: 8 screened records / 7 people = 1 rights-blocked + 1 reference-ineligible + 1 input-ineligible + 1 abstained + 1 failed + 3 predictions. Six reference-qualified records represent five people (P3–P7); five input-qualified records represent four people (P3/P4/P6/P7); three predictions represent two people. No zero is imputed for missing results.

Record-level prediction coverage = 3/6 = 50% of reference-qualified pairs and 3/5 = 60% conditional on input qualification. Input ineligibility = 1/6; abstention = 1/5 and failure = 1/5 of input-qualified records (each 20%); also report each as 1/6 of reference-qualified records. Those same events are 1/8 each of all screened records. These denominators are distinct and labeled, not interchangeable.

For this toy exercise define participant coverage as at least one prediction: 2/5 = 40% of reference-qualified people, 2/4 = 50% of input-qualified people. This does not imply complete visit coverage; P3 happens to have both visits predicted. A real study must freeze its participant coverage rule and visit handling.

Primary toy error summary weights each predicted participant equally, and each of that participant's predicted pairs equally within that participant. Errors are prediction minus reference. This arithmetic convention is not a justified sample/precision design for a real study.

- FTP: P3 errors +10/−10 W give mean absolute error 10, mean squared error 100 and signed mean 0. P4 error +20 W gives 20, 400 and 20. Across the two people: MAE = (10+20)/2 = **15 W**; RMSE = sqrt((100+400)/2) = **15.811388 W**; bias = (0+20)/2 = **+10 W**.
- VO₂ toy column: P3 errors +1/−1 give 1, 1 and 0; P4 error +2 gives 2, 4 and 2. Participant-weighted MAE = **1.5 mL/kg/min**, RMSE = sqrt(2.5) = **1.581139 mL/kg/min**, bias = **+1 mL/kg/min**.
- Secondary pair-weighted FTP summary would instead be MAE 40/3 = 13.333333 W, RMSE sqrt(600/3) = 14.142136 W and bias 20/3 = 6.666667 W. The difference illustrates pseudoreplication/weighting risk; it does not replace the declared primary summary.

For an error-versus-magnitude display the FTP pair coordinates (pair mean, error) are (205,+10), (215,−10), (260,+20); VO₂ coordinates are (40.5,+1), (41.5,−1), (51,+2). Three invented points cannot assess heteroscedasticity or external validity. No correlation claim is made.

Agreement limits and uncertainty intervals are **not assessable**: repeated observations require justified within/between-person handling, distribution assumptions and adequate precision under O3. Applying a single independent-pair standard deviation formula would falsely treat E1/E2 as independent participants. A future protocol must choose and justify its repeated-measures agreement model and participant-cluster interval procedure before evaluation. No limits, confidence level, sample target or physiological accuracy threshold is invented here. No pass/fail accuracy verdict can follow from the toy errors.

## B06 — Conditional sensitivity review

No candidate exists: every candidate-specific sensitivity result is **not run / O5 blocked**. The following review checklist was exercised by tracing each conditional dependency to the protocol; it assigns no default parameters or ranges.

| If the audited method uses… | Record and justify before evaluation | Report |
|---|---|---|
| Power/time or effort windows | W/time units, source calibration/timing uncertainty, discontinuity/coverage policy, effort evidence and permitted perturbations | Change in target units, coverage, refusal transitions and failures; zero remains observed and missing remains absent |
| HR/HRmax/resting HR | Actual equation role, bpm provenance/date, measurement uncertainty and lag/drift assumptions | Same outputs; no age fallback or fabricated resting HR |
| Mass/demographics | Actual terms, kg/other units, measurement dates/uncertainty and source population support | Prediction and reference-normalization effects separately, including shared-error coupling |
| Efficiency/calibration/coefficients | Primary-source equation/units, uncertainty and applicability, calibration source and legal use | One-factor and justified joint changes; no fixed-efficiency truth |
| Eligibility windows/horizon | Actual method-specific requirements and evidence for boundaries | Joint coverage/error/refusal behavior, not accuracy only among survivors |

A dependency absent from the equation is marked not applicable with an audit citation; an unknown dependency stays unresolved. Each included row needs a baseline, evidence-based grid/range, interactions, expected numeric behavior, refusal boundary and tolerance. Plausible measurement-error perturbations and deliberately invalid stress inputs are separate. Develop these on development data; prespecify any held-out analyses and never retune on their results. No mass/HR perturbation magnitude or universal eligibility threshold was invented. Synthetic recipe/profile mutations can exercise determinism and refusals now, but cannot determine physiological sensitivity without a method/reference study.

## B07 — Decisions and reopening evidence

| Gate | FTP | VO₂max |
|---|---|---|
| Protocol review package | B01–B08 exercises completed and self-reviewed within authorized scope | Same |
| Experiment admission | NO-GO: O1–O5 open | NO-GO: O1–O5 open |
| Method selection | Not selected; T06 research decision required | Not selected; T07 research decision required |
| Product admission | Not admitted; science, implementation and later live acceptance outstanding | Same |

Protocol package review completion is not owner scientific acceptance. This report is reviewable evidence; no experiment approval or product GO is inferred from owner approval to execute T05b.

| Decision | Present disposition | Proposed resolution responsibility / reopening evidence |
|---|---|---|
| O1 | Both tracks unresolved | T06 FTP / T07 VO₂max research: primary-source reference protocol, verification and justified temporal pairing; review actual dataset protocol |
| O2 | Both unresolved | Owner intended use plus scientific review of defensible error/agreement/coverage criteria and reference uncertainty; freeze before experiment |
| O3 | Both unresolved | Statistical design review: independent participant counts, allocation, summary/weighting, repeated-measures limits, interval method and precision justification linked to O2 |
| O4 | Both blocked: no lawful matched dataset | Authorized rights/data review: actual processing/publication evidence, identity/modality/time/population linkage and separately reviewed real-observation admission path |
| O5 | Both unresolved: no method audited | T06/T07 equation/input audit: calibration, qualification/refusal rules, numerical oracle/tolerance, justified sensitivity ranges/interactions |

These are responsibilities for later proposed scopes, not people contacted or tasks silently authorized. NO-GO reopens only with the missing evidence and explicit track-specific experiment approval. Method research can propose candidates without granting experiment admission; method selection/implementation cannot be forced by a blocked dataset.

## B08 — Reuse audit and acceptance boundaries

Inspected contracts remain those documented in [T05a evidence](../t05a-software-2026-11-11/README.md): `ExperimentFixture` and optional `RunManifest.fixtureDigest`, `ExperimentProfile` with separate schema IDs, existing FIT byte manifests and replay hashes, existing analysis-generation repository, scalar-v1/curve-v2 and preview reopen/readout. No substitute manifest, experiment registry, storage or configurable runner was created. Toy IDs above are report labels, not canonical session IDs or new application records.

T05a tests `permissionIsCheckedBeforeReplay`, `profilesPreserveMissingInputsAndNeverApproveModels`, manifest/hash mutation cases and `descriptorFixturesTraverseVirtualTimersPersistAndReopenAtEverySpeed` establish the cited software scope. Four small fixtures × three speeds and the earlier 18 long combinations remain historical evidence; no test rerun is claimed here. Automatic FTP/VO₂max remains model_not_approved. Rights validation checks declarations, not legal truth. Synthetic aggregate exclusion, newest-requested/no-fallback and legacy hashes/storage remain unchanged.

| Layer | Evidence now | Still open |
|---|---|---|
| Software/document protocol | B01–B08 paper exercises and checks below; historical T05a deterministic replay/numerical evidence | No selected estimator equation or estimator conformance/sensitivity execution |
| Scientific | Separate target/reference plans, screening/split/reporting rehearsal and explicit NO-GO | Actual rights/references, O1–O5, test–retest and criterion validity; R05/R06 and physiological V07/V08 |
| Live | No new action or pass | T02c/T03e/parent T04, current-adapter delivery/continuity and physical CPU/RSS/battery; deferral closes none |

## Review and verification

Self-review checked every B01–B08 criterion against these sheets/tables, reviewed exclusion/reference labels, reconciled totals and checked participant assignments and leakage boundaries. Toy arithmetic was independently recalculated with Python standard-library fractions/math; the verification did not invoke any OxyWatt estimator or derive expected values from production code. Relative Markdown link checks, R/T/V traceability and `git diff --check` results are recorded in the task. No application tests are required for this documentation-only package; no private input was opened and no third party contacted.

## FTP follow-up from T06a — 2026-11-11

The [executed FTP audit](../t06a-ftp-method-audit/README.md) narrows O1/O5 with source-specific target/equation/input findings and explicit unresolved correction/protocol details. O1/O5 are not closed; O2/O3 remain unresolved and O4 lacks a secured lawful paired dataset. FTP experiment admission remains NO-GO, no method selected. Historical B01–B08 toy results above are unchanged. This follow-up does not resolve any VO₂max decision or live gate.

## VO₂max follow-up from T07a — 2026-11-11

The [independent VO₂max audit](../t07a-vo2max-method-audit/README.md) narrows O1/O5 through explicit reference-modality and required-input/reproducibility checks. O1/O5 remain open, O2/O3 unresolved and O4 lacks an admitted lawful paired dataset. Experiment admission remains NO-GO, no method selected. Historical B01–B08 exercise and FTP decision dispositions remain unchanged.
