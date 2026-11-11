# T05b — Evaluation protocol, sensitivity and scientific go/no-go

Status: specification proposed on 2026-11-11; publication authorized, named T05b execution approval pending. No estimator or method is approved. Parent T05 remains open.

## Problem, scope and dependencies

Define what an experiment could establish before selecting FTP or VO₂max methods or examining held-out outcomes. The smallest complete next package is a reviewed protocol, a worked synthetic reporting exercise and an explicit unresolved-decision register. It is documentation/evaluation design, not an experiment framework. T06/T07 retain independent method selection and later separately approved implementation.

Baseline: [T05a](t05a-fixture-reproducibility.md), merged [PR #28](https://github.com/paulnroll/oxywatt/pull/28), head bac8d718234346e51884c1fa95449056571b12e5, main merge dca3639. Four remote checks succeeded. [T05a evidence](../../evidence/t05a-software-2026-11-11/README.md) records 107 core passes/one private-FIT skip, debug 92 and preview 106 in the full run; final focused rerun covers 12 small persisted combinations. The full run also covered the existing 18 long T04b2 combinations. These are historical results, not new executions in T05b.

T05a already supplies an optional descriptor digest on RunManifest, explicit synthetic recipes/rights declarations, separate versioned FTP/VO₂max input profiles, hash/refusal tests and real virtual-timer → Collector/admission → SQLite → atomic curve generation → reopen/readout checks. Existing FIT byte manifests, replay hashes, scalar-v1/curve-v2, newest-requested/no-fallback and synthetic aggregate exclusion stay authoritative. Its helper currently rejects real-observation descriptors; a future lawful real-data path needs its own reviewed admission scope. Declared permission is not proof of rights; neither profile completeness nor a synthetic oracle establishes physiological eligibility.

Dependencies are T01d/e, T03a–d/p and T04a/b software contracts through T05a. No bicycle, Karoo, private FIT read, new data download or author contact is required. T02c/T03e and parent T04 live acceptance remain open and deferred. No application/test code, schema, UI, production hash, new manifest/store or generic runner is in this package.

## Separate research questions and estimands

| Track | Primary question | Target and required comparison | Cannot establish target |
|---|---|---|---|
| FTP / R05 | For the explicitly supported cycling population and use context, can an independently selected candidate estimate a prespecified protocol-defined FTP with acceptable individual agreement and useful coverage? | Watts against a separately acquired FTP reference under a named/versioned effort protocol, with its target definition, maximal-effort verification and conversion convention (if any) declared. Report protocol strata separately. | CP, best observed P20, manual/configured FTP and vendor estimates are not interchangeable FTP reference labels. If a candidate uses the reference effort itself, report protocol replication, not independent ordinary-ride prediction. |
| VO₂max / R06 | Can an independently selected candidate estimate cycling CPET VO₂max within the intended population/context, with acceptable individual agreement and coverage? | Independent cycle-ergometer CPET gas-exchange measurement, with test stages, calibration, averaging, maximality/verification criteria, units and actual reference outcome. Preserve absolute L/min if available and the dated measured mass used for relative mL/kg/min. | FIT, assumed oxygen cost/efficiency, HR regression fit, estimated FTP or a vendor value do not supply measured VO₂max. If only VO₂peak is established, label it explicitly; do not silently relabel it VO₂max. |

T06/T07 must audit primary-source equations, coefficients/units, required inputs, protocol, population and limitations for each candidate. This protocol selects no equation and adds no claims about any paper's accuracy. If no independent defensible FTP operational definition or CPET reference is available, that track remains blocked rather than substituting a convenient target.

## Four evidence dimensions

1. Numerical conformance: match independent calculations for a pinned equation/preprocessing configuration and declared tolerances. Current power-curve oracles prove only that pipeline; future estimator oracles require a selected equation in T06/T07.
2. Robustness: record behavior under gaps, missing/invalid channels, discontinuities, profile provenance changes and resource limits, including explicit abstention. Passing a fault case does not establish physiological accuracy.
3. Repeatability: identical inputs/configuration must reproduce semantic output; distinguish this from physiological test–retest across independently repeated rides/reference visits. The latter includes biological and reference measurement variation and requires real qualified data.
4. Criterion validity: compare locked candidate predictions with independent matched references on held-out participants. Only this dimension can support an accuracy claim within the evaluated population; synthetic model recovery cannot.

Report these dimensions separately, including untested or blocked states. Coverage, effort suitability, applicability, reference quality and freshness remain separate evidence fields, never a calibrated confidence percentage.

## Data qualification and independence

Before admitting any real experiment, maintain a restricted data inventory referencing existing descriptor/profile/input identities where supported. This is a study table in the evaluation report, not a new application manifest or persistent registry. Record:

- Lawful processing basis and scope, license/consent evidence checked by a responsible reviewer, local analysis versus redistribution separately, retention/deletion and permitted derived publications. Unknown processing rights block use; unknown redistribution blocks publishing records. Do not commit private identifiers, reference documents, input hashes or reidentifiable small-cohort tables. Public reporting uses safe aggregate evidence only.
- Pseudonymous participant linkage checked without exposing identity; same individual and cycling modality, environment/equipment, calibration, protocol/version, reference date and ride date, training/illness/intervention context and reference uncertainty. Cross-modality or unmatched-person data cannot be pooled as criterion pairs.
- Time pairing rule and permitted interval justified for the specific protocol/population before outcome access, with changes in fitness/health and repeat visits addressed. No universal time window is invented. Ambiguous matches are ineligible with reasons.
- Actual input availability, source timing semantics, preprocessing, coverage/effort evidence, mass/HRmax provenance where required, and all candidate-specific missing requirements. No population defaults, inferred maximal effort or bridging uncertain sessions.
- Recruitment, training status, age/sex and relevant health/context characteristics only as lawfully available and needed to describe support. Missing descriptors limit external validity. A convenience sample cannot justify general-population claims; unsupported strata remain excluded or explicitly exploratory.

Record every screened participant/ride/reference pair in a disposition ledger: admitted, reference-ineligible, rights-blocked, input-ineligible, method abstained or execution failed, with reasons. Counts reconcile; multiple reason flags may overlap but one predefined primary disposition makes totals auditable. Reference-ineligible records never enter accuracy denominators and remain visible in screening totals.

Assign participants, not segments or rides, to development/calibration and locked evaluation sets. All visits, fragments and augmented/synthetic variants of one person stay together; suspected duplicates across datasets require resolution before use. Preprocessing fitting, threshold selection, calibration and hyperparameter tuning use development participants only; prespecify how personal calibration can precede evaluation without consuming its reference outcome. Lock split assignment, randomization seed (if used), dataset revision, code/configuration and analysis plan before held-out labels/results are accessed. No specific split ratio is justified yet.

Repeated rides are nested within participants, not independent subjects. Prespecify one primary participant-level summary/weighting and repeated-measures handling; show participant and pair counts separately. A resampling interval must resample participants with all their observations, not individual segments. Test-set feedback changes create a new exploratory revision and require a fresh independent evaluation; do not present retuning as confirmation.

## Locked report and metrics

For each track separately, record protocol revision/approval date, intended use, candidate/config/preprocessing/profile revisions, existing input identities, dataset/split revision, exclusions, primary endpoint, analysis units, missingness policy and deviations. Keep sensitive linkage local. Results table separates synthetic, development and untouched evaluation evidence.

On eligible pairs with a numeric prediction, define error as prediction minus reference. Report MAE and RMSE, signed bias and agreement limits in W for FTP and mL/kg/min for relative VO₂max; report absolute VO₂ metrics separately if evaluated. Define participant weighting and interval method before execution. Show error-versus-magnitude plots, reference distribution and heteroscedasticity checks; the agreement-limit model, assumptions and uncertainty procedure must be justified for repeated data. Do not automatically apply independent-pair limits to nested observations. Correlation/regression fit is secondary and cannot establish interchangeability. Relative/percentage errors, if added, require prespecified denominator handling and remain secondary.

Report screened participants/records, reference-qualified pairs, method-input-qualified pairs, predictions, abstentions and failures. Report prediction coverage against all reference-qualified pairs and separately conditional on input qualification; report abstention reason counts and failure rates with explicit denominators. Report accuracy conditional on predictions alongside coverage to expose selective abstention. Never replace a refusal with zero or discard hard cases silently. Uncertainty intervals must state method, unit of independence and limitations; no calibrated individual confidence interval is claimed without evidence.

Predefine supported strata and comparison policy; mark unplanned subgroup analysis exploratory. Report paired candidate differences only when methods are evaluated on the same locked pairs, together with differing coverage; do not rank methods by incomparable complete-case subsets. Numerical and scientific tolerances are distinct.

## Method-dependent sensitivity plan

After each candidate audit, T06/T07 supplies an input-to-assumption table: actual input/parameter, units/provenance, equation role, uncertainty evidence, proposed perturbation range/grid, interactions, expected numerical behavior and refusal boundary. Freeze it before evaluation. Required entries are only those the method actually uses:

| Dependency if present | Proposed analysis; bounds require evidence |
|---|---|
| Power and time integration | Calibration scale/offset, irregular timing, missing spans and effort selection; preserve zero/missing and epoch rules. |
| HR response / HRmax / resting HR | Measurement/source error, lag/drift, missingness and provenance; no age-derived fallback. |
| Body mass / demographics | Dated measurement error and applicability, only for used terms; distinguish mass used in prediction from reference normalization to expose shared-error coupling. |
| Efficiency / calibration / fitted coefficients | Inspect source assumptions and permissible uncertainty, compare justified alternatives and calibration availability. No fixed-efficiency truth or invented coefficient. |
| Eligibility windows / observation horizon | Evaluate coverage/error and abstention changes jointly; no arbitrary universal stable-window or maximality cutoff. |

Run one-factor perturbations and scientifically justified joint/interaction cases on development data first. Separate plausible measurement uncertainty from deliberately invalid stress inputs. Report output change in target units, coverage, refusal transitions, failures and largest observed changes, with untested ranges explicit. A candidate with no justified perturbation bounds has an open sensitivity decision, not a passed test. Synthetic constructions can expose discontinuities, identity/hash changes and deterministic fault behavior; without a selected model they cannot provide estimator sensitivities. Held-out sensitivity analyses must also be prespecified, never tune on their outcomes.

## Decision gates and unresolved decisions

| Decision | Required evidence / outcome now | Authority and next step |
|---|---|---|
| Protocol readiness | B01–B08 below reviewed; unresolved items explicitly assigned. This can pass as a bounded design without any physiological dataset. | Owner reviews named T05b deliverable. Does not admit an experiment. |
| Experiment admission, independently FTP/VO₂max | Lawful reference-linked data, audited candidate inputs, frozen estimand/pairing/splits/metrics/sensitivity and justified sample/precision/accuracy criteria; all blocking decisions resolved. Otherwise NO-GO. | Owner approves a concrete offline experiment after scientific review; no data/contact authorization follows from this spec. Both tracks blocked now. |
| Method selection | T06/T07 source audit plus numerical, robustness, sensitivity and criterion evidence within support, with limits reviewed. May reject all candidates. | Separate named method decision; no automatic estimator implementation. |
| Product admission | Supported use/claims, scientific acceptance, separately approved implementation and software evidence, honest abstention/UI and later live acceptance. | Separate owner release/scope decision; unavailable remains valid if science fails. |

Open decisions (none silently receive defaults):

| ID | Missing justification | Resolution and blocking gate |
|---|---|---|
| O1 | Exact FTP reference definition/protocol and VO₂max CPET verification; temporal pairing | T06/T07 primary-source audit and dataset protocol review; blocks experiment admission per track. |
| O2 | Intended-use error/agreement and coverage targets | Owner defines useful decision/use context; scientific reviewer derives defensible criteria from primary evidence/reference error. Freeze before evaluation; blocks scientific pass/fail. No numerical accuracy target approved. |
| O3 | Sample size, split allocation, primary participant summary, interval/precision plan | Qualified statistical design using anticipated variability and O2 precision requirements, with participant-level independence; blocks experiment. No invented N or power claim. |
| O4 | Lawful paired data and supported population | Rights/reference audit and safe inventory, including a separately reviewed real-observation admission path; blocks real experiment. No dataset secured. |
| O5 | Candidate-specific inputs, qualification, sensitivity bounds and numerical tolerance | T06/T07 source/equation audit and independent oracle design; blocks candidate experiment, not this generic protocol review. |

A stopped experiment reports why and what evidence would reopen it. Missing thresholds mean “not assessable”, never scientific GO. Software pass alone cannot resolve O1–O5. No method is selected by filling this template.

## Measurable acceptance and R–T–V traceability

These are planned T05b review/exercise criteria, not executed physiological tests. No new top-level R/V identifiers are needed.

| Case | Observable Done evidence | Mapping |
|---|---|---|
| B01 | Two separate completed question/estimand sheets with units, reference exclusions and explicit unresolved method inputs; CP and configured FTP rejected as automatic FTP ground truth. | T05b → T06; R05/R07; V07/V09 |
| B02 | VO₂max sheet distinguishes cycling CPET VO₂max/VO₂peak and independent gas measurement from modeled oxygen cost, with missing-reference state. | T05b → T07; R06/R07; V08/V09 |
| B03 | Paper qualification exercise uses invented cases: valid declared synthetic oracle, unknown rights, wrong person, wrong modality, missing protocol, ambiguous timing, unsupported population and missing required input. Each has explicit disposition; none earns criterion validity. | R05/R06/R07/R10; V07/V08/V09/V12 |
| B04 | Invented participant/ride table demonstrates no participant crossing splits, no shared reference outcome used for calibration, and segment count never treated as participant N; freeze checklist names every unresolved field. | R05/R06/R07; V07/V08/V09 |
| B05 | Hand-worked reporting example reconciles screening/qualification/prediction/refusal/failure denominators and independently calculated MAE/RMSE/bias. Agreement analysis assumptions and blocked uncertainty/precision decisions explicit; toy values are not performance evidence. | R05/R06/R07; V07/V08/V09 |
| B06 | Sensitivity checklist conditionally maps actual candidate inputs to justified ranges, interactions, units and coverage/refusal effects; absent candidate leaves O5 blocked rather than populating invented parameters. | R05/R06/R07; V07/V08/V09/V18/V19 |
| B07 | Decision record shows protocol-ready does not imply experiment-admitted, method-selected or product-admitted; each track has its own go/no-go and O1–O5 disposition. | R05/R06/R07; V07/V08/V09 |
| B08 | Reuse audit cites T05a tests/evidence and existing storage/hashes; software/scientific/live status table reports no new physiological or hardware pass. Links, traceability and whitespace checks pass. | R04/R07/R10/R12; V06/V09/V12/V14/V16/V18/V22/V23 |

## Files, verification, risks and completion

Named T05b execution, if approved, owns this task and `docs/algorithms/validation-strategy.md`, `docs/algorithms/ftp-estimation.md`, `docs/algorithms/vo2max-estimation.md`, `docs/discovery/scientific-research.md`, plus one compact worked report at `docs/evidence/t05b-protocol-review/README.md` (create only during approved execution). Synchronize Canvas, decision log, implementation plan and work packages. The report contains B01–B08 sheets/tables and open decisions, not a registry, runner or production result. No core/app changes expected; any need for those requires a new named scope.

Verification is document self-review, hand-checking the toy arithmetic/dispositions and participant assignments, relative Markdown link/traceability checks and `git diff --check`. No application test run is needed for this documentation-only package. If software is later changed, its own named approval and affected regression suites apply. No private FIT opt-in or hardware check runs here.

Done for T05b means B01–B08 evidence reviewed and limitations recorded, including a legitimate blocked scientific admission decision. It does not mean experiments ran or parent physiological requirements passed. Parent T05 protocol readiness can be reviewed with blockers; scientific acceptance remains open until qualified independent data and track-specific criteria are met.

Risks: false ground truth, declaration mistaken for rights proof, leakage, pseudoreplication, shared prediction/reference mass error, selectively reported coverage, retrospective thresholds and population overreach. Qualification ledgers, locked analysis, participant-level units, conditional metrics and explicit decisions address these risks without promising to eliminate them.

| Acceptance layer | Current position |
|---|---|
| Software | T05a historical evidence exists; T05b review/exercises B01–B08 await approval/execution. No estimator conformance exists. |
| Scientific | Both tracks blocked on lawful independent reference data and O1–O5; R05/R06 and physiological V07/V08 remain open. |
| Live | T02c/T03e/parent T04 and physical-device budgets remain open; owner defers tests until the end. Deferral passes no gate. |

## Specification self-review — 2026-11-11

Reviewed T05a source contracts/tests, task/evidence, domain constraints and the full documentation diff including this new file. Corrected the parent backlog to keep candidate audit/selection in T06/T07 and distinguish protocol design dependencies from real experiment admission. Checked B01–B08 mappings against existing R/V definitions, rights/privacy boundaries, conditional sensitivity and unresolved O1–O5. All 155 relative Markdown links in the nine affected documents resolve; `git diff --check` passes. No application tests were run for this documentation-only change; no physiological or live result is claimed. Worked B01–B08 execution remains pending named approval.
