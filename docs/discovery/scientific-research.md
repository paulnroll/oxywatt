# Scientific research and evidence ledger

Reviewed 2026-11-10. This is initial feasibility research, not a completed validation study. No coefficients are approved for OxyWatt production. Source observations below are deliberately separated from product hypotheses.

| Source / method | Evidence actually reviewed | Input/protocol mismatch | Recommendation |
|---|---|---|---|
| [Björkman et al. 2016, revised Ekblom-Bak](https://pmc.ncbi.nlm.nih.gov/articles/PMC4983286/) | Controlled two-workload cycle test; sex-specific equations using age and HR response; independent cross-validation reported | Requires prescribed stages, demographic inputs and stable responses; ordinary road segments are not its validated protocol | Calibration benchmark candidate, not drop-in normal-ride estimator |
| [2023 differential submaximal cycling model](https://pmc.ncbi.nlm.nih.gov/articles/PMC10338562/) | Full protocol includes measured oxygen uptake and extrapolation of HR and VO₂ kinetics | Despite promising title, requires respiratory gas measurement; unavailable on Karoo | Reject for power+HR-only implementation |
| [Wiecha et al. 2023](https://pubmed.ncbi.nlm.nih.gov/37162318/) | Abstract reports submaximal exercise and somatic models; VO₂ at AT/RCP contributes strongly | High reported model fit does not establish prediction from ordinary power+HR streams; full equation/data audit still needed | Do not borrow accuracy claims or coefficients |
| [Karsten et al. 2021 CP versus FTP](https://pmc.ncbi.nlm.nih.gov/articles/PMC7862708/) | Controlled cycling trials; individual agreement limits matter despite strong association | Maximal trials are not equivalent to unlabelled routine rides | Separate metrics; no CP→FTP renaming |
| [Do CP and FTP equate? 2021](https://pubmed.ncbi.nlm.nih.gov/34055164/) | Abstract directly evaluates interchangeability in highly trained athletes | Supports checking model-specific agreement, not a universal conversion | No constant conversion approved |

## Candidate ranking

1. Observed power-duration maxima: suitable for immediate deterministic engineering; no claim they are maximal physiological capacity.
2. FTP: manual baseline now; compare protocol-specific FTP and power-duration candidates using effort evidence. Lack of observed maximal work limits identification. Research does not yet select a defensible automatic ordinary-ride FTP model.
3. VO₂max: prioritize explainable power/HR response candidates for offline experiments, with measured/configured HRmax provenance and mass. Segment qualification, HR lag and efficiency assumptions must be established. No reviewed study here establishes production accuracy for this exact passive use case.
4. Optional controlled calibration may improve identifiability, but cannot become mandatory without owner agreement.

A conventional oxygen-cost equation or fixed efficiency can estimate an assumed metabolic cost; neither by itself identifies VO₂max. HRmax/resting-HR ratios alone do not use ride evidence and are not the selected model. Proprietary Garmin outputs can be comparison observations, not laboratory ground truth.

## Research still required before model approval

Retrieve complete candidate equations and coefficient units, code/data licenses and population constraints. Audit whether inputs include measured VO₂, ventilatory/lactate thresholds or maximal protocol evidence unavailable to us. Specify HR-lag handling, stable-window detection, physiological confounders, sensitivity to HRmax/mass/efficiency and out-of-distribution abstention. Document both numerical correctness and external validity.

No legally reusable matched ordinary-ride + CPET ground-truth dataset has been secured. Owner FIT files support ingestion, robustness and repeatability checks, not absolute accuracy without independent physiology measurements. The differential-model paper makes its data available on request; that is not a ready public dataset. Do not contact authors without authorization.

Decision: independent research gates T05/T06/T07; algorithms can remain unavailable rather than emitting invented precision. Release scope must be reviewed if these gates cannot be satisfied.


## T05b protocol proposal — 2026-11-11

[Canonical T05b specification](../spdd/tasks/t05b-evaluation-protocol.md). T05b proposes evaluation design, not new primary-source findings or coefficient approval. It separates numerical/robustness/repeatability evidence from criterion validity and records unresolved protocol, sample/precision, rights/population, thresholds and candidate-sensitivity decisions O1–O5. T06/T07 must verify full primary equations/inputs/populations before candidate experiments. No source contacted or real/private data accessed.


## T05b executed protocol exercises — 2026-11-11

Owner approved T05b after merged PR #29. [B01–B08 report](../evidence/t05b-protocol-review/README.md) completes the bounded documentation exercise: separate FTP/VO₂max estimands, ten invented qualification cases, participant split/freeze rehearsal, reconciled coverage/refusal/error arithmetic, conditional sensitivity and explicit O1–O5 decisions. Self-reviewed evidence is ready for owner review; it is not a physiological study. Both experiment admissions remain NO-GO; no lawful matched dataset, justified scientific thresholds/sample plan or method selected. T06/T07 retain independent research/selection and later implementation; no automatic authorization follows. T05 protocol/reproducibility deliverables are now recorded, while parent scientific acceptance and deferred T02c/T03e/parent T04 live gates remain open.


## T06a executed audit — 2026-11-11

Owner approved T06a after specification PR #32 merged. [F01–F08 report](../evidence/t06a-ftp-method-audit/README.md) records the bounded primary-source audit, source-access/correction limits, input compatibility, conditional sensitivity and FTP O1–O5 decisions. Recommendation is to retain model_not_approved for automatic FTP from ordinary rides; no audited candidate qualifies on present evidence. An optional controlled-protocol investigation needs a separate scope decision and cannot silently replace ordinary-ride intent. No method, dataset, estimator or experiment is admitted; T06b and independent T07a remain separate. Source numerical results are centralized in the report, not new product accuracy thresholds. R05/physiological V07 and later live acceptance remain open.


## T07a executed independent audit — 2026-11-11

Owner authorized T07a following T06a; PR #33 merged during work and local main/branch updated. [G01–G08 report](../evidence/t07a-vo2max-method-audit/README.md) centralizes primary equations, protocol/population/reference and rights/access findings, input compatibility and conditional sensitivity. Recommendation: retain model_not_approved for ordinary-ride VO₂max on present evidence. VO₂max O1–O5 remain unresolved/blocked; no method, experiment, dataset or T07b implementation admitted. Any optional controlled-protocol direction requires a separate scope decision. FTP remains independent; R06/physiological V08 and deferred T02c/T03e/parent T04 live acceptance remain open. No application code, private FIT, dataset download, device action or framework.


## Comparable power–HR trend research — 2026-11-11

[T08a1 source/claim review](../spdd/tasks/t08a1-power-hr-trend.md) adds a bounded primary-source basis: Lamberts 2010 single-person repeated LSCT case, Lamberts 2014 controlled performance associations and Fritzsche 1999 controlled cardiovascular-drift experiment. Access is abstract/indexed extraction with an explicit 2011 full-text 403; no validated passive coefficients, universal eligibility thresholds or dataset admission follows. Two descriptive equations are OxyWatt proposals, not published physiological estimators. Recommendation for review: compare fixed-band power with local interpolation on synthetic inputs; neither method selected. Unknown confounders and HR suppression prevent automatic fitness interpretation. No data downloaded or author contacted.


## T08a1 executed software experiment — 2026-11-11

Owner approved [T08a1](../spdd/tasks/t08a1-power-hr-trend.md) after PR #35 merged. [H01–H07 evidence](../evidence/t08a1-power-hr-trend/README.md) records the synthetic A/B experiment through actual committed replay, scalar-v1/reopen, newest authority, fixed-baseline/raw reports and fault/cohort/resource checks. Ten new core tests and ten API 32/34 preview executions pass; full core 117 passes/one private-FIT skip, debug 92/preview 116, build/lint/release isolation and final focused checks pass. No production method/estimator, chart, schema, store or manifest was added.

Both candidates reproduce injected observational changes but show confounder-driven false directions; neither is selected for physiological interpretation. Software H01–H07 does not close O1–O5, R05/R06/physiological V07/V08, T07/T07b, T08b/c or deferred T02c/T03e/parent T04 live gates. No participant data or hardware evidence. Smoothing and legacy trend-v0 numeric defaults remain unapproved for this route.
