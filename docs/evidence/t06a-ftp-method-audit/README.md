# T06a — FTP primary-source audit — 2026-11-11

Owner authorized the [T06a audit](../../spdd/tasks/t06a-ftp-method-audit.md) after merged PR #32. Baseline main 4f6eedf includes the CI repository fix; PR #32's four checks succeeded. This is a bounded literature/source and input-compatibility audit, not a physiological experiment or method approval. No participant dataset, private FIT, estimator, application code or device was used. Existing [T05b gates](../../spdd/tasks/t05b-evaluation-protocol.md) apply.

**Recommendation:** no audited candidate currently justifies automatic FTP from ordinary rides. Retain model_not_approved. A separately scoped, optional controlled-protocol comparison is a possible research direction, subject to owner product intent and O1–O5; it is not an approved replacement for passive ordinary-ride estimation. CP research could have its own merit but does not deliver FTP. This is a conclusion about this search/evidence, not proof that no suitable method exists anywhere.

## F01 — Search log and stopping boundary

All searches/accesses below occurred on 2026-11-11. Scientific findings come from primary publications or their indexed abstracts; reviews, commercial material, forum posts and secondary bibliographies were not used as evidence. Search results were used to locate full sources. Full-text access means the article body was retrieved, not that underlying data or every cited source was audited.

| Group / exact representative queries | Included sources | Disposition and search limit |
|---|---|---|
| Short protocol: `functional threshold power 20 minute 60 minute validity cyclists primary study Borszcz 2018`; `MacInnis 2019 20 minute 60 minute time trial functional threshold power full text` | S1, S3, S5; A1 | Retain controlled-test evidence, both favorable and unfavorable. Do not treat differing warm-ups or training populations as interchangeable. A1 full text unavailable. |
| Sustained/reference: `Functional Threshold Power in Cyclists validity concept physiological responses full text`; `60-min effort 17 FTP McGrath 2019`; `Functional threshold power ordinary validation` | S1, S3, S5 | Distinguish maximal 60-min performance from completing a prescribed load for 60 min and from a physiological steady-state target. No universal reference definition selected. |
| Power-duration / real riding: `critical power training racing data field professional cyclists validity power profile study`; `An improved methodology for estimating critical power full text repository`; `Do Critical and Functional Threshold Powers Equate 34055164` | S2; A2–A4 | Field/racing CP is relevant to feasibility, not independent FTP validation. Abstract-only findings cannot approve a filtered algorithm. |
| Linked alternatives: `Cycling power outputs predict functional threshold power equation Denham`; `Prediction Functional Threshold Mc Grath` | S4; A5 | Inspect laboratory predictors to avoid assuming their inputs can be reconstructed from routine power/HR. |
| Corrections: exact S1 title + `correction erratum`; exact S2 title + `correction erratum`; exact S4 title + `2022 correction erratum`; exact A3 title + `correction erratum` | C1; S2 inconsistency below | C1 exists but content inaccessible. No separate correction surfaced for S2/S4/A3 in these queries; not a guarantee that none exists. |

Stop after all three groups have a primary-source assessment and compatibility conclusion, including inaccessible-source exclusions. This is not a systematic review, exhaustive candidate ranking or a claim to cover every new paper. The 2025 S5 publication is retained rather than relying only on the older ledger. No source author was contacted and no dataset link/file was downloaded.

## F02/F04 — Compact source extraction

Source facts are limited to the following sheets; later compatibility and recommendations are OxyWatt interpretations. Page numbers below are PDF page numbers starting at one, including repository covers where present. No raw participant rows are reproduced.

### S1 — Borszcz et al. (2018), DOI 10.1055/s-0044-101546

[Accepted manuscript](https://kar.kent.ac.uk/69657/1/Functional%20threshold%20power%20in%20cyclists.pdf), full body; Methods pp. 4–6, Results p. 7/Figure 1. Twenty-three trained male cyclists; four laboratory visits over three weeks, at least 48 h apart. Velotron ergometer; randomized 20/60-min trial order. FTP20 = 0.95 × mean20; FTP60 = mean60, in W. The 20-min warm-up includes 20 min light, three 1-min accelerations with recoveries, 5 min light, 5 min maximal and 10 min light; 60-min warm-up is 10 min. Separate incremental lactate/gas testing supplies IAT, not an ordinary-ride input. Within-person agreement analysis, not held-out prediction. Reported FTP60−FTP20 bias −4 W, 95% LoA −40 to 32 W; time-to-exhaustion at FTP20 50.9 ± 15.7 min. Repository version license is unspecified. **C1 correction exists** ([notice](https://pubmed.ncbi.nlm.nih.gov/29906813/), DOI 10.1055/a-0639-1221); publisher HTML/PDF retrieval was blocked and shell access returned a bot challenge. Its content/effect is unresolved: these numbers describe the retrieved manuscript, not a correction-verified final result. Do not use this sheet alone to set acceptance targets.

### S2 — Karsten et al. (2021), DOI 10.3389/fphys.2020.613151

[Publisher full text](https://www.frontiersin.org/journals/physiology/articles/10.3389/fphys.2020.613151/full); Methods/Results, [PDF](https://www.frontiersin.org/journals/physiology/articles/10.3389/fphys.2020.613151/pdf) pp. 2–4. Seventeen moderately trained cyclists/triathletes; Cyclus2, randomized CP/FTP visits. Maximal 12/7/3-min trials, 30-min passive recoveries; inverse-time fit P = CP + W′/t selected by combined fit error. W, s, J; CP/W′ individually fitted. FTP = 0.95 × mean20 after 5 min at 100 W. HR/RPE maximality checks use age-predicted HRmax: source procedure, not an approved OxyWatt fallback. Within-person comparison, no external held-out cohort; CP is the comparator, not independent FTP60. CP−FTP bias 7 W, 95% LoA −19 to 33 W, r = 0.969 in Results. Abstract/body statistical descriptions and Discussion bias/LoA wording differ; retain Results/Figure 1 as source-located observations, not a resolved pooled estimate. Data statement offers author-mediated availability; no dataset/license secured. Not evidence for renaming CP or passive FTP.

### S3 — Mc Grath et al. (2019), DOI 10.70252/RQOO7391

[Publisher PDF](https://intjexersci.com/files/ijes/vol12/iss4/23.pdf), full body; Table 1/Methods pp. 2–5, Results/Discussion pp. 6–8. Nineteen highly trained cyclists/triathletes (12 men, 7 women), Wattbike, nonrandomized successive-week visits. cFTP = 0.95 × mean20 W. Table 1 includes a 5-min maximal pre-effort; warm-up intensity used laboratory Dmax. Five participants missed the second 20-min test: repeatability subset is 14, sustained-load assessment 19. Seventeen completed 60 min at cFTP; two stopped at 35/52 min. This prescribed-load completion is not a separately maximized FTP60 trial; study explicitly stopped at 60 min. Lactate/gas measurements assess quasi-steady behavior. Repeated-test LoA −17 to 13 W; bias sign differs between abstract and Discussion, so no signed bias is adopted here. No held-out ordinary-ride prediction or lawful reusable dataset established by the retrieved article. This favorable controlled evidence is retained with its population and endpoint limits; its ±20 W acceptance choice is not imported into OxyWatt.

### S4 — Mc Grath et al. (2022), DOI 10.70252/DJTU3106

[Publisher PDF](https://intjexersci.com/files/ijes/vol15/iss4/16.pdf), full body; Methods pp. 3–5, Table 6 p. 8. Seventy highly trained triathletes (50 men, 20 women); laboratory graded test plus 20-min FTP protocol, at least seven days apart within two weeks. FTP target is 0.95 × mean20, corrected between Garmin pedals/Lode ergometer. Selected regression: FTP(W) = −6.62 + 0.32 FBLC4(W) + 0.42 BM(kg) + 0.46 Pmax(W) in abstract; Table 6 rounds intercept to −6.6. FBLC4 is power at blood lactate 4 mmol/L, not lactate itself; BM is mass; Pmax is final completed graded workload. Coefficients have corresponding W/W and W/kg units, intercept W. Stepwise selection with internal LOO, not independent external validation; model-4 residual error 15 W in Table 6. Grade-stage reconstruction and coefficient precision require clarification before reproducible implementation; a ride maximum is not Pmax. No reference-qualified ordinary-ride coverage, independent FTP60 validation or reusable dataset license established. Exclude for current passive power/HR inputs.

### S5 — Gough et al. (2025), DOI 10.28985/1425.jsc.06

[Published PDF](https://www.jsc-journal.com/index.php/JSC/article/download/964/845/5462), full body; §§2.2–2.4, Results/Tables 1–2, limitations p. 8. Twenty-two trained men, Wattbike Pro; familiarization, repeated 20-min trials and separate exhaustion visit. Self-selected warm-up differs from pre-fatigue protocols. Methods specify exhaustion load = 0.95 × mean20 W; abstract shorthand “95% FTP20” must not be compounded into 0.95². Mean endurance 42 ± 17 min; Table 2 retains five participants at 60 min. Repeatability ICC reported 0.94 (95% CI 0.82–0.98); one exhaustion visit, no held-out passive prediction. Publication is CC BY 4.0; no participant dataset permission secured. This is a different protocol/population from S3; their outcomes cannot determine one universal multiplier. Suggested lower factors are research suggestions, not independently validated coefficients adopted here.

### Abstract-only / incomplete candidates and exclusions

| ID / primary link and access | Extracted evidence and missing audit | Disposition |
|---|---|---|
| A1 [MacInnis et al. 2019](https://pubmed.ncbi.nlm.nih.gov/29809063/), DOI 10.1123/ijspp.2018-0100; indexed abstract retrieved, publisher full text 403 | Eight trained men, two 4-min and two 20-min tests, one 60-min test. Typical errors 8.1/4.6 W for 4/20-min tests; short-test association with 60-min performance. No audited prediction coefficients, warm-up/calibration, independent validation or data rights. | Relevant sustained-performance comparator; not a reproducible approved candidate. No invented equation from R². |
| A2 [Karsten et al. 2015](https://link.springer.com/article/10.1007/s00421-014-3001-z), DOI 10.1007/s00421-014-3001-z; publisher abstract only, subscription body; [repository](https://kar.kent.ac.uk/43632/) has no full text | Three laboratory exhaustive loads compared to field protocols, including training/racing 3/7/12-min maxima. Field-extraction experiment CP SEE 5.2%, LoA −34 to 44 W. Participant N/population, filtering, calibration, fitting and rights not audited. [Crossmark](https://crossmark.crossref.org/dialog/?doi=10.1007%2Fs00421-014-3001-z) reports current document. | CP feasibility lead only; no FTP target or passive FTP validation. |
| A3 [Spragg, Leo and Swart 2023](https://pubmed.ncbi.nlm.nih.gov/37660315/), DOI 10.1080/02640414.2023.2254574; abstract retrieved, publisher body 403 | Thirteen U23 professionals, field CP test then 90 days racing; fixed 2/5/12-min and filtered MMP approaches compared with CP test. CP differences reported 5/7 W, correlations 0.872/0.922; neither establishes FTP. Exact filter/equation/protocol/rights audit incomplete. | Exclude executable filtered method pending full text. No substitution of CP target; no assumption of stable fitness over our own 90-day horizon. |
| A4 [Mc Grath et al. 2021](https://pmc.ncbi.nlm.nih.gov/articles/PMC8136559/), DOI 10.70252/ISYH9512; indexed article sections retrieved, direct PMC challenge | Fifteen highly trained athletes, 8 women/7 men; repeated laboratory CP trials and FTP test. Work-time CP 282 ± 53 W versus FTP 266 ± 55 W; within-person comparison, not held-out passive prediction. Full protocol/equation and data rights not completely audited. | Supports checking noninterchangeability; not retained as an executable conversion. |
| A5 [Denham et al. 2020](https://doi.org/10.1519/JSC.0000000000002253); publisher abstract and author-uploaded manuscript search extracts, full manuscript not audited | Forty cycle-trained/untrained participants and an 11-man intervention assessment; laboratory CPET/Wingate predictors. Coefficient precision, stage protocol, target independence and dataset rights not fully checked. | Do not transplant a Pmax equation or VO₂ requirement into ordinary rides. No VO₂max method selection in this audit. |

No scientific inference relies on the unrelated NIRS/nutrition articles returned while resolving journal locations. Preprints/secondary descriptions of S5 were superseded by its published PDF. No full corrected S1 text or inaccessible appendix is represented as verified.

## F03 — Current inputs versus required evidence

This table is engineering interpretation against inspected code, not a study result. Core `PowerCurveEngine.DURATIONS` currently exposes 5/30/60/180/300/600/1200/1800/3600 s. Canonical observations and persisted effects retain data beyond the nine maxima; adding a new duration or analysis would still need its own approved scope.

| Route | Already representable | Missing or insufficient | Qualification/refusal consequence for later design |
|---|---|---|---|
| Controlled mean20 conversion | 1200-s observed maximum, confirmed time/coverage, profile protocol/effort references if actually supplied | Maximum may be submaximal, in the wrong protocol, fatigued or poorly paced; profile evidence fields are declarations, not a maximality detector | Never convert every observed P20. Missing/unsupported protocol/effort evidence remains unavailable pending method approval. HR alone cannot certify intent. |
| Protocol-defined maximal mean60 reference | 3600-s observed maximum and source-neutral intervals | Continuity does not establish a maximal 60-min test, reference independence, protocol or current capacity; prescribed-load completion is a distinct endpoint | Preserve observed curve as observed. No automatic FTP label from a 3600-s record. |
| CP/inverse-time comparator | Irregular-time integration, some relevant durations and retained inputs | 7/12-min and 2-min values are not in current nine-duration output; trial selection/recovery/effort evidence and fit identifiability absent; racing maxima need actual source protocol | No fit from arbitrary nine winners; no CP→FTP rename or convenience interpolation. A separately approved CP task would still have a different target. |
| Graded-test/lactate predictor | Optional mass/provenance, power/HR streams | Lactate curve and laboratory workload semantics/calibration are not supplied by ordinary power/HR. Pmax is not a sprint or observed ramp maximum. | Do not drop an unavailable physiological term, replace it with HR or default a coefficient. Current-input mismatch excludes this passive route. |

Missing data is not zero; pause/loss/restart boundaries remain those of T03/T04. No profile snapshot or canonical acquisition contract was expanded. Manual/configured FTP is a baseline and cannot be used as both training label and independent criterion. Population support cannot be inferred from a user's high observed watts.

## F05 — Conditional sensitivity and future numerical oracles

These are analytic/design proposals, not executed estimator tests or physiological sensitivities. Each row is conditional on a future separately approved candidate. No numeric uncertainty range, minimal effort, residual cutoff, sample size or accuracy target is selected.

| Candidate dependency | Independently checkable numerical relation / oracle proposal | Evidence still needed |
|---|---|---|
| A fixed multiplier applied to protocol-qualified P20 | For a declared factor k, output perturbation is k × ΔP20 when k is fixed; vary k separately and record its assumed status. An invented constant 200 W integral for 1200 s with k = 0.95 gives 190 W by arithmetic only. | Power-meter scale/offset evidence, protocol effect, effort suitability, coefficient uncertainty and lawful independent reference study. This oracle cannot justify k physiologically. |
| Mean60 | Constant/fractional integral tables can test time-weighted mean, boundaries and coverage; an invented 200 W covered hour averages 200 W. | Independent reference protocol/maximality, fatigue/context and test–retest evidence. Observation correctness is not capacity validity. |
| Two-parameter CP | P(t) = CP + W′/t implies units J/s = W; invented CP 250 W, W′ 15000 J gives P(300 s) = 300 W and P(600 s) = 275 W. Recovery of invented coefficients could test algebra only. | Exact approved fitting objective, conditioning, duration/trial selection, bounds and uncertainty; distinct-error-objective linearizations cannot be assumed identical under noise. No FTP inference from successful recovery. |
| Laboratory multivariable regression | Audit coefficients/units/source precision first; independent substitution can later verify each term and missing-input refusal. No numeric substitution is executed here because required stage semantics/precision are unresolved. | Primary protocol clarification, lactate interpolation/calibration uncertainty, mass date, coefficient covariance/applicability and joint perturbations. |

For any future candidate, perturb actual used inputs individually and in justified coupled cases (power calibration/time/effort selection; regression input/calibration dependencies). Determine bounds from measurement/source evidence before evaluation; deliberately malformed data is a separate robustness exercise. Report target-unit changes together with coverage, abstention transitions and failures, on development data first. Do not invent HRmax/mass/efficiency sensitivity for equations that do not use them. Reuse T05a descriptor/profile/input hashes and T03d storage if a later named software slice is approved; no parallel experiment infrastructure follows from this table.

## F06 — Rights, data availability and access limits

Article access/license and participant data rights are separate. S1's manuscript cover and S2's author-mediated data statement do not secure a lawful paired ride/reference dataset. S5's article license permits reuse of the publication under its terms; it is not consent to ingest private rides. For S3/S4 and abstract-only entries, no reusable participant dataset/code license was established in inspected material. None is admitted. No statement was verified by contacting authors, seeking credentials or downloading participant files.

Publisher access challenges/403s remain explicit in C1/A1/A3/A4. Public manuscript reading was the only external content retrieval; no papers or figures are vendored into the repository. No correction is guessed to affect only an author's name. Resolve C1 and any decisive protocol details before using their values in an experiment or implementation decision.

## F07 — Candidate comparison and decisions

| Route | Target/equation and current-input fit | Validation/coverage and applicability assessment | Recommendation |
|---|---|---|---|
| Qualified controlled 20-min protocol | Simple arithmetic is reconstructible; required protocol/effort evidence absent in ordinary rides | Controlled evidence varies by endpoint/population/protocol; it does not qualify passive observations or calibrate refusal coverage | Possible optional controlled research question only; not selected |
| Maximal protocol-defined 60-min performance | Direct performance target possible in principle; observed maximum alone insufficient | Requires lawful independent reference and explicit target definition; not a passive estimator by itself | Reference-design option, not automatic product output |
| CP from trials/racing maxima | Different target; actual trials/durations/fitting must be audited and scoped | Field CP evidence is not criterion validity for FTP; no audited external conversion resolves that mismatch | Comparator/independent future metric only |
| Laboratory predictors | Additional protocol/physiology unavailable; precision/input audit incomplete | Internal or lab evidence cannot be assigned to ordinary-ride estimates | Exclude current passive route; no missing-term substitution |

No composite score or universal accuracy threshold is used. No candidate passes all of target match, current input evidence, reproducibility, independent passive validation, supported population, sensitivity/abstention and lawful paired-data availability. The negative recommendation does not imply that controlled FTP testing is useless or that a proprietary method has been disproved.

| FTP decision | Updated disposition and evidence needed to reopen |
|---|---|
| O1 reference/protocol/time | Narrowed, not resolved: distinguish independently maximized mean60, protocol-converted mean20, sustained prescribed-load completion and lactate/steady-state endpoints. Choose a justified operational target/protocol and dataset-specific temporal pairing; resolve C1 if relying on S1. |
| O2 usefulness/accuracy/coverage | Open. Source LoA, residual errors or study-specific tolerance choices are not OxyWatt targets. Owner intended use plus scientific/reference-uncertainty review required. |
| O3 sample/precision/split | Open. Retrieved participant counts and internal validation do not supply our participant-level held-out design or precision plan. Statistical review remains required. |
| O4 rights/population/data | Blocked: no lawful matched dataset secured or admitted; article availability does not close this. Supported ordinary-ride population unestablished. |
| O5 candidate/eligibility/sensitivity | Narrowed, not resolved: equations and input mismatches audited; effort/protocol rules, justified bounds, inaccessible details and independent oracles remain method-specific decisions. |

| Gate | Outcome |
|---|---|
| T06a bounded audit | F01–F08 completed with explicitly inaccessible/incomplete sources; ready for owner review |
| FTP experiment admission | NO-GO pending O1–O5; literature review is not a participant experiment |
| FTP method selection | None approved; recommendation is to retain unavailable for ordinary rides |
| Product / T06b implementation | Not admitted or authorized |
| VO₂max / T07a | Independent pending research; no method/FTP shortcut selected here |
| Live | T02c/T03e/parent T04 remain open and deferred; no new hardware evidence |

Suggested next owner decision: retain ordinary-ride intent and unavailable automatic FTP while investigating other evidence, or explicitly authorize investigation of an optional controlled-protocol product path. Neither choice is made here. T07a can be scoped independently; a failed FTP gate must not force its outcome.

## F08 — Review, traceability and actual checks

| Acceptance | Evidence location / limitation | R/V mapping |
|---|---|---|
| F01 | Search log, three groups and explicit stop/access limits | R05/R07; V07/V09 |
| F02 | S1–S5 and incomplete-source exclusions; equation/unit cross-check, no copied conversion implementation | R05/R07; V07/V09 |
| F03 | Current-input table and observed-capacity/refusal distinctions | R02/R05/R07; V02/V07/V09 |
| F04 | Source-specific participant/reference/repeat/design findings, full versus partial access marked | R05/R07; V07/V09 |
| F05 | Conditional analytic oracle/sensitivity proposals, no invented bounds | R05/R07; V07/V09/V18/V19 |
| F06 | Rights/access section and no admitted dataset | R05/R07/R10; V07/V09/V12 |
| F07 | Comparison and separate gate/O1–O5 tables | R05/R07; V07/V09 |
| F08 | Task, algorithm/ledger, Canvas/backlog/decision/validation synchronization and checks recorded in task | R05/R07; V07/V09 |

Self-review checked equations/units against source locations, population/protocol and signed-difference caveats, incomplete access/correction, independence versus internal fits, and privacy/scope. No article's p-value, R², CV or tolerance is converted into our acceptance criterion. Simple invented sensitivity anchors are numerical proposals only. No application tests or physiological experiment ran. No scientific/live V gate is closed by this documentation evidence.
