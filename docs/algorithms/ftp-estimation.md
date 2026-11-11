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
