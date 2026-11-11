# FTP estimation

Status: research-gated, no production equation approved. R05 remains requested scope.

FTP is a metric separate from Critical Power, 20-minute best mean power and the FTP configured in Karoo. Display manual/configured FTP as a baseline with origin, never as an automatically validated estimate.

Candidates:

- Protocol-specific 20-minute estimate: `FTP_candidate = 0.95 × P20` is a known test convention, not a universal conversion for any observed 20-minute segment. Without protocol/maximal-effort evidence it is not selected.
- Sustained near-maximal efforts: potentially useful evidence, but routine endurance activity does not identify maximum capacity. HR alone does not prove maximal intent.
- Power-duration/CP model: `P(t) = CP + W′/t` is a two-parameter CP model (CP in W, W′ in J, t in seconds). A fit requires suitable maximal efforts at distinct durations and identifiability/residual checks; its output remains CP, not FTP.

[Karsten et al.](https://pmc.ncbi.nlm.nih.gov/articles/PMC7862708/) compare controlled CP and FTP tests. Association does not establish individual equivalence. Their validation cannot be assigned to an opportunistic ordinary-ride implementation.

T06 must choose a specific method, eligibility criteria, observation horizon, effort evidence and rejection rules; document equations/coefficients and any calibration. Validate numerical recovery on synthetic model data separately from ability to predict real FTP. Until then return `unavailable: model_not_approved` for automatic FTP; this is not completion of R05.

Do not feed estimated FTP into VO₂max by default. Do not overwrite the host FTP setting automatically. History preserves manual and estimated series separately, including method, date, quality dimensions and underlying effort windows.


## T05b protocol proposal — 2026-11-11

[Canonical T05b specification](../spdd/tasks/t05b-evaluation-protocol.md). The proposed T05b protocol requires an independently acquired, explicitly defined FTP test reference, participant-level held-out evaluation, individual agreement and coverage/refusal reporting. CP, configured FTP and unqualified observed P20 cannot substitute as ground truth. T06 resolves candidate inputs, eligibility, sensitivity bounds and primary-source method audit; no method selection follows from protocol readiness.


## T05b executed protocol exercises — 2026-11-11

Owner approved T05b after merged PR #29. [B01–B08 report](../evidence/t05b-protocol-review/README.md) completes the bounded documentation exercise: separate FTP/VO₂max estimands, ten invented qualification cases, participant split/freeze rehearsal, reconciled coverage/refusal/error arithmetic, conditional sensitivity and explicit O1–O5 decisions. Self-reviewed evidence is ready for owner review; it is not a physiological study. Both experiment admissions remain NO-GO; no lawful matched dataset, justified scientific thresholds/sample plan or method selected. T06/T07 retain independent research/selection and later implementation; no automatic authorization follows. T05 protocol/reproducibility deliverables are now recorded, while parent scientific acceptance and deferred T02c/T03e/parent T04 live gates remain open.


## T06a scope proposal — 2026-11-11

[Named T06a scope](../spdd/tasks/t06a-ftp-method-audit.md) defines a bounded primary-source FTP audit after merged T05b PR #30. F01–F08 map R05/R07 (plus input/privacy R02/R10) to V07/V09 and conditional V02/V12/V18/V19: search log, equation/protocol/population/reference audit, current-input compatibility, conditional sensitivity, rights/data availability and a reviewable recommendation. Named audit execution approval is pending; this specification chooses no method. O1–O5, experiment admission, separate T06b implementation and later live gates remain explicit. T07a stays independent. No application code, private FIT, dataset download, device action or new framework.
