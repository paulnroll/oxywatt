# T07a — Independent VO₂max method audit — 2026-11-11

Owner authorized this documentation/research audit after T06a. [Task and G01–G08](../../spdd/tasks/t07a-vo2max-method-audit.md); [T05b evaluation gates](../../spdd/tasks/t05b-evaluation-protocol.md). PR #33 was open at start and merged during this work; local main and this branch advanced to 7a58d89. T06a findings are preserved, not treated as a VO₂max decision.

**Recommendation:** retain automatic VO₂max as model_not_approved. None of the audited routes currently supports a reproducible, scientifically admitted ordinary-cycling power/HR estimator in OxyWatt. A controlled submaximal protocol merits possible separate research, but requires an owner scope decision and cycling CPET validation; it is not silently substituted for ordinary rides. This bounded audit does not prove that all passive approaches are impossible.

No method, participant experiment or product is admitted. No private FIT, participant dataset, device, estimator implementation or new infrastructure was used. Public article bodies and availability statements only were read; no author was contacted.

## G01 — Search and access log

Search/access date: 2026-11-11. Start with the existing three ledger citations; retain primary scientific sources. Vendor material is classified as a vendor primary claim rather than independent evidence. Reviews/search snippets only locate primary material; forum claims are excluded.

| Group / representative exact queries | Sources retained / disposition |
|---|---|
| Controlled cycling: `Björkman 2016 revised Ekblom Bak cycle test 4983286 equation validation`; `Validity of the revised Ekblom correction erratum` | S1 publisher full body. PMC challenge avoided through publisher. No correction surfaced in this bounded search; not a guarantee of absence. |
| Gas/threshold models: `2023 differential submaximal cycling model 10338562 VO2max`; `Wiecha 2023 37162318 VO2max submaximal prediction model`; `VO2max prediction based on submaximal cardiorespiratory full text` | S2 PMC body; Nature route failed. S3 published eLife PDF; eLife HTML/PMC challenged. |
| Corrections: `86291 correction erratum VO2max`; `s41598-023-38089-7 correction` | No relevant correction found; unrelated identifier collision excluded. Do not label all corrections verified absent. |
| Passive cycling: `cycling VO2max estimation free riding heart rate power validation CPET Garmin Firstbeat study`; `VO2max cycling Firstbeat validation study -site:reddit.com -site:vo2max.jp` | S4 vendor white paper; A1–A4 are modality/target/nonreproducibility leads, not adopted equations. No independently reproducible passive cycling candidate established in this search. |

Stop after these three groups have a primary assessment and explicit compatibility/access conclusions. This is not an exhaustive systematic review or ranking of every wearable. No participant-data or code attachment was downloaded to resolve availability. Full-body access means article text retrieved, not that every appendix, underlying raw record or cited study was audited.

## G02/G03 — Source-located equation, protocol and validation audit

The source facts below are centralized here; later compatibility/recommendation sections are OxyWatt interpretations. No source performance statistic is adopted as a product accuracy criterion. Equations are transcriptions for audit, not selected methods or implementation instructions.

### S1 — Björkman et al. (2016), revised Ekblom–Bak

[Publisher](https://link.springer.com/article/10.1007/s00421-016-3412-0), DOI 10.1007/s00421-016-3412-0; Methods, Results “New equation”, Table 3. Development 217, external cross-validation 115 adults; reference is maximal **treadmill** gas testing, not cycling CPET. Predictor: Monark, 60 rpm, four minutes at approximately 30 W then four minutes higher workload; HR sampled at 3:15/3:30/3:45/4:00 each stage. Sex-specific equations predict ln(VO₂max in L/min):

- Men: 2.04900 − 0.00858 A − 0.90742 R + 0.00178 D − 0.00290 H.
- Women: 1.84390 − 0.00673 A − 0.62578 R + 0.00175 D − 0.00471 H.

A = age years; D = workload difference W; R = ΔHR/D (bpm/W); H = low-stage HR bpm. Exponentiate to obtain L/min. Coefficients correspond to inverse input units. Ages span 20–86; HR-affecting medication excluded. Reference acceptance uses at least three of five criteria including plateau/RER/effort/duration/HR checks. External SEE 0.30 L/min and CV 9.4%, not a cycling accuracy guarantee. Article CC BY 4.0; no lawful paired ordinary-ride/cycling-reference dataset secured. Source protocol is controlled and sex/age-specific, not free riding.

### S2 — Differential model (2023)

[Primary article](https://pmc.ncbi.nlm.nih.gov/articles/PMC10338562/), DOI 10.1038/s41598-023-38089-7; Methods, Eq. 4, SMO steps 1–6/Table 1. Retrospective group: 17 young active men; prospective validation: 9 students, 5 men/4 women, randomized maximal/submaximal cycle tests. Protocol ramps 25 W/min, includes warm-up/recovery and measured HR **and VO₂**. Model:

`dX/dt + (X − X0)/τ = (K/τ) P(t)`.

Fit X separately for HR and gas-measured VO₂; X0 baseline, τ time, K response per W. Extrapolate both to the time predicted HR reaches estimated HRmax. Source uses sex/age HRmax regressions and terminates at 80% of that estimate; 1 Hz signals receive 15-point smoothing. These are source choices, not approved defaults. Table 1 mean absolute relative error 5.56 ± 3.92%; n=9 comparison, not ordinary-ride coverage. Reference is exhaustive cycle gas testing; no independent verification-bout acceptance protocol established in this audit. Data available on author request, not secured. Package/procedure and parameter-fitting details would need further reproducibility review; missing measured VO₂ already excludes current power/HR-only use.

### S3 — Wiecha et al. (2023), eLife.86291

[Published PDF](https://elifesciences.org/articles/86291.pdf), Methods pp. 3–5, Table 5 p. 8; full body. Cycling subset: 1094 men, split 656/219/219 for derivation/testing/validation; internal split, not external transport validation. Cyclus2 graded cycling: 20–30 W increments every two minutes, calibrated Cosmed gas measurements; highest 15-s VO₂, with multi-criterion maximality screening. Retained audit examples (output mL/kg/min):

- RCP: `8.57 + 1.08 rVO2_RCP − 0.04 VE_RCP`.
- Somatic: `82.36 − 0.14 BM − 0.66 BF − 0.22 Age`.

rVO₂ is measured mL/kg/min at gas-defined RCP; VE L/min, BM kg, BF percent, Age years. Coefficients carry corresponding output/input units. RCP validation RMSE/MAE 2.03/1.64; somatic 6.11/4.74 mL/kg/min. AT/combined variants also require gas-derived inputs and are not retained as executable candidates. Article CC BY; availability statement says data included in manuscript, with file-transformation code listed. No independent ride/CPET pairs or reuse permissions were established by attachment inspection (none downloaded). Same-test submaximal gas predictors are not passive field inputs; the somatic route does not use ride physiology.

### S4 — Firstbeat manufacturer report

[2014 white paper, updated 2017](https://assets.firstbeat.com/firstbeat/uploads/2015/11/white_paper_VO2max_11-11-2014.pdf), pp. 3–5; full body, vendor report rather than independent peer-reviewed validation. Describes selecting reliable HR/workload segments and reports cycling observations from 29 cyclists. The cycling sentence gives “92%” accuracy alongside “MAPE ~5%”; this wording is internally ambiguous and is not converted into an OxyWatt target. Detailed cycling coefficients, segment scoring, training/held-out split, reference protocol and participant exclusions are not supplied reproducibly. HRmax uncertainty is discussed, including age estimates. Document reserves rights; no implementation license or paired dataset secured. Relevant feasibility claim, but not a reproducible equation or independent cycling validation. Manufacturer output remains a comparison observation, never ground truth.

### Additional sources/leads excluded from executable selection

| ID / primary source | Evidence actually accessed and reason for exclusion |
|---|---|
| A1 [Firstbeat VO₂ estimation paper](https://www.firstbeat.com/wp-content/uploads/2015/10/white_paper_vo2_estimation.pdf), updated 2012, full body | Estimates oxygen consumption during activity using R–R-derived signals/neural networks. It is a different target from maximum oxygen uptake; beat intervals are not interchangeable with sampled canonical bpm. No reproducible network or license established. |
| A2 [Forerunner 245 study](https://pubmed.ncbi.nlm.nih.gov/40770433/), abstract/search extraction only | Outdoor running protocol, not independent cycling power/HR validation. Full equation/device implementation not audited; do not borrow its accuracy for Karoo. |
| A3 [Free-living heart-rate/acceleration study](https://pubmed.ncbi.nlm.nih.gov/31999530/), abstract/search extraction only | Activity-specific HR/acceleration models, additional inputs and incomplete equation/reference audit; no drop-in cycling power/HR equation retained. |
| A4 [Denham et al.](https://doi.org/10.1519/JSC.0000000000002253), publisher abstract/author manuscript extracts from T06a | FTP-based VO₂ prediction is not approved here. Exact reference and transport/calibration would require separate full audit, and automatic estimated FTP cannot become an unreviewed input. No coefficient adopted. |

No reviewed article or vendor claim establishes a universal eligibility window, physiological confidence percentage or cycling accuracy threshold for this application. No claim that vendor methods cannot work: the missing reproducible method and admitted evaluation evidence are the limitations relevant here.

## G04 — Compatibility with existing source-neutral inputs

Inspected `RiderProfileSnapshot` and T05a `ExperimentProfile`: mass, configured HRmax, configured FTP and optional resting HR/calibration/protocol/reference evidence can be represented with missing states. No age/sex/body-fat/gas/ventilation/threshold measurements are fabricated or added. Canonical sampled power/HR/cadence and receipt/file time provenance are observations, not lab stages, beat-to-beat intervals or a measured maximum.

| Route | Present capability | Missing evidence / refusal consequence |
|---|---|---|
| Controlled two-workload HR prediction | Power/HR/time and optional mass | Stable protocol-qualified stages, age/sex and equipment/effort context absent by default. Treadmill target does not satisfy cycling-reference gate. No arbitrary segments relabeled as stages. |
| Differential kinetics | Power/HR time series and explicit gaps | Measured VO₂ time series absent; gas-parameter identification cannot be recovered by deleting that equation. No age-HRmax fallback to make the source run. |
| Gas-threshold regression | Optional mass and power/HR | Gas-derived threshold/VO₂/ventilation absent. Cannot replace them with manual FTP, CP or HR breakpoints. Same-test predictor/reference correlation is not ordinary-ride transport validity. |
| Somatic regression | Optional mass | Body fat/age absent; no ride contribution. Not a substitute for intended cycling analytics or personal longitudinal fitness without validation. |
| Vendor passive estimate | Power/HR observations broadly overlap | Exact qualified segments, coefficients, calibration and error/abstention behavior unknown. Existing fields do not license/reproduce a black box. |

No universal mass/HRmax requirement is imposed: required inputs depend on the actual equation. Missing stays missing. HR drift/lag, pauses, power-meter calibration, medication/context and uncertainty of supplied HRmax require method-specific evidence; this audit invents no correction. Existing T05a rights checks validate declarations, not actual consent. Existing hashes/storage and synthetic aggregate exclusion stay unchanged.

## G05 — Conditional sensitivity and numerical checks

This is a future evaluation plan, not executed estimator sensitivity. Numerical conformance, robustness, deterministic repeatability, physiological test–retest and criterion validity remain distinct. No new tests/code or generic experiment runner.

| Actual dependency if later selected | Independently checkable relation / planned cases | Unresolved evidence |
|---|---|---|
| Log-output model | Check natural exponentiation and units, finite values and distinct sex-specific coefficients. For an exponent increment δ, absolute output ratio is exp(δ). For R=ΔHR/ΔP test coupled HR/power errors and near-zero ΔP refusal. | Protocol-matched uncertainty, approved domain/minimum separation and paired cycling CPET validation; no arbitrary bounds. |
| Mass normalization | Relative = 1000 × absolute L/min ÷ kg. Invented 3 L/min and 75 kg gives 40 mL/kg/min; 60 kg gives 50, purely arithmetic. Distinguish prediction mass from reference normalization to expose coupled errors. | Dated mass/calibration error and covariance, not a universal percentage perturbation. |
| Differential fit | Under a constant input, equilibrium X = X0 + K P; transient scale τ and initial condition matter. Independent invented equilibrium substitutions could check algebra. Missing measured response must refuse rather than receive assumed gain/efficiency. | Full fitting objective, identifiability, gas/time preprocessing, HRmax source and extrapolation domain; no source cutoff adopted. |
| Linear gas/threshold or somatic regression | Check each coefficient unit and substitution independently only after candidate audit; joint perturbations for correlated threshold/gas/mass measurements. | Source population/measurement uncertainties and threshold reproducibility; no fake gas or demographic defaults. |
| Passive selection/HRmax | Report output changes together with numeric-prediction coverage, refusal transitions and failures. Freeze stage/segment handling before held-out evaluation. | Reproducible algorithm and justified ranges/qualification/lag/drift rules missing; vendor HRmax fallback not approved. |

Bounds/interaction grids must be justified from actual measurement/source evidence in development data before evaluation. Invalid-data stress is separate from plausible uncertainty. Synthetic reconstruction and arithmetic can check software semantics now; they cannot establish VO₂max, prescribe accuracy or resolve lack of legal independent references.

## G06 — Rights and reference qualification

Article licenses and author availability statements are not processing/redistribution permission for participant-linked ordinary rides and CPET. No dataset is secured/admitted; no identities, GPS, raw research records or private hashes committed. S2 author-request access was not pursued. S3 publication/source-code listings were read, not downloaded as a training dataset. S4 protected/proprietary materials do not grant implementation rights. Public quotes/equations are attributed; no paper/figure is vendored.

Future study requires lawful same-person cycling CPET references, actual protocol/calibration/maximality/averaging/mass/date evidence, justified temporal matching and participant-level split/cluster analysis under T05b. A treadmill reference, a peak lacking verified maximum status, a manually configured value and a vendor prediction cannot silently satisfy that gate. Report all ineligible/abstained/failed cases and denominators; selective coverage cannot be hidden behind conditional accuracy. No source's N or error becomes our sample/accuracy target.

## G07 — Decision record

| VO₂max decision | Current outcome / reopening evidence |
|---|---|
| O1 target/reference/protocol/time | Narrowed, not closed. Cycle predictor and cycle reference must be checked separately; require actual cycling CPET maximum/peak status and temporal matching. |
| O2 useful error/agreement/coverage | Open; intended-use decision and scientifically justified criteria needed. Literature RMSE/CV/vendor percentages are not acceptance thresholds. |
| O3 participant/sample/precision/splits | Open; internal/external source designs inform questions, not our design or N. Participant-level independence and repeated visits remain required. |
| O4 legal data/population | Blocked: no lawful paired ordinary-ride/cycling-CPET dataset secured or admitted. Populations/modalities remain candidate-specific. |
| O5 method/inputs/refusal/sensitivity | Narrowed, not closed. Explicit missing gases/protocol/demographics or nonreproducible proprietary method prevent adoption. No defaults, new input schema or estimator introduced. |

| Gate | Outcome |
|---|---|
| T07a bounded audit | G01–G08 completed with declared partial-source limits, ready for owner review |
| Scientific experiment admission | NO-GO pending O1–O5 and a concrete approved experiment |
| Method selection | None approved; retain model_not_approved |
| T07b/product | Not authorized/admitted |
| FTP/T06 | Independent; no estimated-FTP shortcut or change to its findings |
| Live T02c/T03e/parent T04 | Deferred and open; no bicycle or Karoo needed for this audit |

Possible later owner decisions: keep passive intent with unavailable physiological estimates while pursuing lawful reference evidence, or separately scope an optional controlled-protocol feasibility study. Neither is selected here. Observed power curves remain useful software output without borrowing physiological validity. A negative bounded audit is a complete research result, not permission to force a number.

## G08 — Evidence and review mapping

| Case | Executed evidence | Mapping |
|---|---|---|
| G01 | Three-group dated search/access/correction log | R06/R07; V08/V09 |
| G02 | S1–S4 equations or explicit nonreproducible exclusion; A1–A4 limited-access/target exclusions | R06/R07; V08/V09 |
| G03 | Source population/split/reference modality, maximum status and metrics distinguished | R06/R07; V08/V09 |
| G04 | Canonical/profile compatibility/refusal table | R02/R06/R07; V02/V08/V09 |
| G05 | Conditional sensitivity/oracles with unapproved bounds explicit | R06/R07; V08/V09/V18/V19 |
| G06 | Rights/reference qualification and no admitted dataset | R06/R07/R10; V08/V09/V12 |
| G07 | VO₂max O1–O5 and distinct gates | R06/R07; V08/V09 |
| G08 | Task/algorithm/ledger/Canvas/backlog/decision/validation synchronization; checks recorded in task | R06/R07; V08/V09 |

Self-review cross-checked source equations/units/locations, exponential versus linear output, cycling versus treadmill reference, measured-gas dependencies, vendor versus independent evidence, input availability and preserved FTP boundaries. Access failures and unreviewed attachments remain limits. No application tests, participant experiment or estimator execution; no physiological/live acceptance claimed.

## Owner priority after this audit — 2026-11-11

The owner prioritizes useful within-person trends over exact absolute agreement. This changes the primary intended-use question for follow-up; it does not turn these cross-sectional/protocol source findings into longitudinal evidence. Investigate repeatability/change sensitivity and report absolute agreement separately. A stable bias may permit useful change tracking, but stability across conditions/profile/calibration changes remains an assumption to evaluate. O2 is narrowed, not numerically resolved. A clearly labeled exploratory fitness proxy is an option for a concrete later specification; it cannot be silently labeled physiological VO₂max. Audit findings and no-approved-estimator status remain intact.
