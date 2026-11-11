# Cycling VO₂max estimation

Status: research-gated. No defensible production formula for passive ordinary rides selected in this discovery. R06 is retained, not considered complete.

Intended inputs: qualified power/HR segments, rider mass, explicit HRmax provenance and optional resting HR/calibration. Cadence may help qualification but is not guaranteed. No age-based HRmax fallback, fixed-efficiency truth, estimated FTP shortcut or fake laboratory precision.

Proposed research pipeline: identify stable, well-covered segments → evaluate HR lag/drift and model applicability → run a reviewed model → retain per-ride value or abstention with inputs/method/config → aggregate compatible accepted results. Segment duration, variance, lag and drift thresholds remain research parameters, not invented production constants.

Candidate family: regression/extrapolation of power versus HR with a published oxygen-cost relationship, or an explicitly calibrated model of individual response. Each requires justification for extrapolation, efficiency and HR response assumptions. A smooth regression and repeated rides do not eliminate systematic error. The [research ledger](../discovery/scientific-research.md) records why reviewed lab methods are not direct substitutes.

Before enabling: document every equation, unit, coefficient source, validation population/protocol, calibration requirements, sensitivity, abstention thresholds and supported use. Do not transplant ventilatory/gas-analysis inputs that our hardware does not measure. No universal default accuracy or confidence percentage is approved.

Results expose data coverage, applicability and validation status independently. Display mL/kg/min with restrained rounding determined by model evidence; preserve full computation internally for reproducibility. If an exploratory model is approved, visibly label it experimental. If no method qualifies, show unavailable and seek an explicit release-scope decision.

Trend: preserve per-ride accepted values and reasons for rejected rides. Separate indoor/outdoor and incompatible model/calibration cohorts until comparability is established. Smoothing specification is in the Canvas; it summarizes estimates and adds no physiological validity.


## T05b protocol proposal — 2026-11-11

[Canonical T05b specification](../spdd/tasks/t05b-evaluation-protocol.md). The proposed T05b protocol requires independently measured cycling CPET references, explicit VO₂max versus VO₂peak status, dated mass/normalization provenance and participant-level held-out evaluation. Sensitivity depends on actual T07 candidate inputs; no HR/mass/efficiency parameter is invented or universally required. No lawful paired dataset secured; experiment and product admission remain blocked.


## T05b executed protocol exercises — 2026-11-11

Owner approved T05b after merged PR #29. [B01–B08 report](../evidence/t05b-protocol-review/README.md) completes the bounded documentation exercise: separate FTP/VO₂max estimands, ten invented qualification cases, participant split/freeze rehearsal, reconciled coverage/refusal/error arithmetic, conditional sensitivity and explicit O1–O5 decisions. Self-reviewed evidence is ready for owner review; it is not a physiological study. Both experiment admissions remain NO-GO; no lawful matched dataset, justified scientific thresholds/sample plan or method selected. T06/T07 retain independent research/selection and later implementation; no automatic authorization follows. T05 protocol/reproducibility deliverables are now recorded, while parent scientific acceptance and deferred T02c/T03e/parent T04 live gates remain open.


## T07a executed independent audit — 2026-11-11

Owner authorized T07a following T06a; PR #33 merged during work and local main/branch updated. [G01–G08 report](../evidence/t07a-vo2max-method-audit/README.md) centralizes primary equations, protocol/population/reference and rights/access findings, input compatibility and conditional sensitivity. Recommendation: retain model_not_approved for ordinary-ride VO₂max on present evidence. VO₂max O1–O5 remain unresolved/blocked; no method, experiment, dataset or T07b implementation admitted. Any optional controlled-protocol direction requires a separate scope decision. FTP remains independent; R06/physiological V08 and deferred T02c/T03e/parent T04 live acceptance remain open. No application code, private FIT, dataset download, device action or framework.

## Owner intended use — 2026-11-11

Prioritize within-person change tracking over precise absolute agreement. Follow-up method review must evaluate longitudinal sensitivity, repeatability, confounders and coverage; smoothing alone cannot establish validity. Retain physiological units only with reviewed support for that claim, or separately specify an exploratory fitness proxy with honest labeling. No equation or implementation is selected.
