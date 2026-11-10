# Cycling VO₂max estimation

Status: research-gated. No defensible production formula for passive ordinary rides selected in this discovery. R06 is retained, not considered complete.

Intended inputs: qualified power/HR segments, rider mass, explicit HRmax provenance and optional resting HR/calibration. Cadence may help qualification but is not guaranteed. No age-based HRmax fallback, fixed-efficiency truth, estimated FTP shortcut or fake laboratory precision.

Proposed research pipeline: identify stable, well-covered segments → evaluate HR lag/drift and model applicability → run a reviewed model → retain per-ride value or abstention with inputs/method/config → aggregate compatible accepted results. Segment duration, variance, lag and drift thresholds remain research parameters, not invented production constants.

Candidate family: regression/extrapolation of power versus HR with a published oxygen-cost relationship, or an explicitly calibrated model of individual response. Each requires justification for extrapolation, efficiency and HR response assumptions. A smooth regression and repeated rides do not eliminate systematic error. The [research ledger](../discovery/scientific-research.md) records why reviewed lab methods are not direct substitutes.

Before enabling: document every equation, unit, coefficient source, validation population/protocol, calibration requirements, sensitivity, abstention thresholds and supported use. Do not transplant ventilatory/gas-analysis inputs that our hardware does not measure. No universal default accuracy or confidence percentage is approved.

Results expose data coverage, applicability and validation status independently. Display mL/kg/min with restrained rounding determined by model evidence; preserve full computation internally for reproducibility. If an exploratory model is approved, visibly label it experimental. If no method qualifies, show unavailable and seek an explicit release-scope decision.

Trend: preserve per-ride accepted values and reasons for rejected rides. Separate indoor/outdoor and incompatible model/calibration cohorts until comparability is established. Smoothing specification is in the Canvas; it summarizes estimates and adds no physiological validity.
