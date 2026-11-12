# T08a5 — Salford stored-row descriptor at 115 bpm

Status: **Owner approved the described 115 bpm exact-or-nearest-bracket variant on 2026-11-12 (“to dawaj”). Specification/freeze preceded execution; L01–L06 complete for owner review at source/report layer.** The description was approved before assigning this ID. Search of documentation, backlog and work packages found T08a5 unused; reserve it as a separate T08a subdivision. No publication or merge authorized. T08a1 and T08a3 J04–J07 remain unchanged.

## Scope and estimand

Deliver one local descriptive W at the same recorded-HR coordinate, **115 bpm**, for each of the existing fixed-primary 197 structural columns, with explicit exact versus interpolated provenance. This is a new mathematical descriptor of the uncleaned deposited rows, not a qualified-segment A/B result, physical synchronization, comparable ride or chronological fitness trend. Dependencies: [T08a4 definition/checks](t08a4-salford-exact-hr-rows.md), its [executed dossier](../../evidence/t08a2-comparable-observations/README.md#t08a4-executed-exact-hr-stored-row-description--2026-11-12), [T08a3 support geometry](t08a3-salford-index-ab.md), [validation](../../algorithms/validation-strategy.md) and [ADR 0002](../../architecture/adr/0002-independent-estimators.md).

Let `m_c(h)` be the exact-HR, equal-row-weight empirical mean power defined in T08a4, from finite nonnegative P and positive finite HR at equal source index. Never derive it from rounded reported means when raw exact arithmetic is available.

- If column c actually contains 115 bpm, set `x_c = m_c(115)`, method `exact_level`. Use only its actual 115-bpm rows.
- Otherwise freeze `l_c = max{h < 115 with n_c(h)>0}` and `u_c = min{h > 115 with n_c(h)>0}` using **HR support only**, then `w_c = (115-l_c)/(u_c-l_c)` and `x_c = (1-w_c)m_c(l_c) + w_c m_c(u_c)`, method `linear_interpolation`. These are adjacent observed HR levels around the anchor, not adjacent times or source rows. Means retain equal row weights within each endpoint; interpolation weights depend on HR distance, not endpoint counts or power.
- No observed exact level and no strict bracket means unavailable `unbracketed_anchor`. Never extrapolate or choose another anchor, person or column subset. This method branch is part of the explicit estimator definition, not permission to fall back to an older report when the newest fails.

Compute sums, means, weights and final values as exact rational representations of numeric XLS inputs, rounding once to binary64 for report values. No fitted regression, positive-slope constraint, residual cutoff or other fit admission. An equal/decreasing pair of endpoint means is still a raw arithmetic descriptor, not a reason to delete data or infer impairment. The interpolated result must lie within the endpoint-mean convex hull as an arithmetic invariant, not a physiological suitability test.

## Fixed choices and scientific limits

| Choice | Evidence / explicit development assumption |
|---|---|
| Archive and primary | Existing Salford four-rider DOI `10.17866/rd.salford.3080560.v1`, article 3080560/file 4787857, Scarf/Salford, CC BY 4.0; archive SHA-256 `834e371c8aaf1e9fd04cf9c44619d045ac18dd3270ab7f2653e1f3692afe0c19`. Inherit pre-power lexicographic primary and nominal 5 s stratum, all 197 columns, no fallback |
| Anchor | 115 bpm is inherited from T08a3's unique support-only minimax bracketing-radius proposal, selected before T08a4 HR-conditioned power. This execution approval accepts it for this development descriptor, not T08a3 A/B or a real-use physiological target |
| Bracket selection | Nearest observed HR on each strict side; frozen from existing support before calculating this new descriptor. Existing geometry predicts both endpoints within 112–118 bpm. This is an identity/support expectation, not a new HR-band mean or validated maximum-gap threshold; mismatch requires investigation, not widening or favorable reselection |
| Method mix | Existing support predicts 188 exact and nine interpolated columns. Verify, do not force those counts. Exact presence has priority by definition, regardless of favorable power or n; do not interpolate an exact-supported column for a better number |
| Pairing/weights | T08a4's unverified equal-source-index pairing and conventional equal-row mean remain assumptions. The final stored row counts normally; no measured/nominal duration or sampling-independence claim |
| Interpolation | New explicit assumption: a straight line between adjacent empirical HR-level means is a useful mathematical location descriptor. No evidence here verifies physiological linearity or a latent true power-at-HR relation. Exact and interpolated values carry different evidence labels |
| Artifact policy | Uncleaned: retain Power-zero, positive extremes, duplicate-date and zero-containing columns; HR-zero absent from positive-HR means and separately retained. No clipping, settling/stability/work filter, row shift or repair |
| Precision/configuration | No minimum n, invented uncertainty, p-value, meaningful-change threshold or physiology. Keep one-row endpoints, disclose n/range/zero counts; no low-n deletion. One configuration `salford-anchor115-v1`, no anchor sweep, tuning or numerical sensitivity study authorized |

Previous power outputs have been observed; all source material is exploratory development, never untouched validation. The primary, anchor and endpoint rule are fixed independently of a favorable new power outcome. Matching 115 as a coordinate does not match true HR, duration, workload history, row frequencies, exposure, calibration, sensor/device settings or context. Exact-HR quantization/error, unverified timing/synchronization, HR suppression/lag and artifacts remain unresolved. A number for every column is not proof of precision, independent visits, trend usefulness, FTP or VO₂max.

## Output, refusals and operations

Local output has exactly one record per original primary source-column identity: status, method, anchor, actual endpoint HR levels/row indices/counts, raw within-endpoint P ranges, zero counts, exact rational means/weights/value and once-rounded W, plus inherited quality flags. For exact output there is one node at 115 with weight one; do not fabricate bracketing evidence or claim joint supported seconds. Keep source-coordinate order, never chronological sorting. No pairwise power ranking/differences, global average, baseline, normalization, calendar trend, smoothing or best-case subset in this package.

Git receives only aggregate counts/method/support diagnostics, hashes, verification and limits; no detailed column identities, dates/notes, power scalars or raw rows. Reconcile 197 = exact + interpolated + unavailable + numerical/operational-incomplete records. If an operation fails before complete enumeration, show expected/completed output counts and failed/incomplete current status, never silently omit columns or mark a partial report complete.

Archive/member/T08a4 plan/table/contributor/check/authority identities are required inputs, with the current T08a4 report pinned explicitly; failure of that current input never substitutes another successful run. Require current status available and recheck authority/inputs at finalization/read. Hash mismatch, unexpected source/support/contributor change, corrupt/unavailable required files or reader mismatch is an operational failure, not zero power or physiological refusal. Arithmetic overflow/nonfinite values refuse output explicitly. Missing exact HR is only a method-branch condition; missing required source data cannot trigger interpolation using different data.

Freeze local `salford-anchor115-v1` plan and endpoint/row contributor identities before computing x. Use retained source and T08a4 support/contributor tables, not a new support/availability audit or source download. Verify integrity, then read only selected primary endpoint rows needed to reconstruct exact means. No reusable importer, application dependency or scientific method implementation. Python standard library plus existing xlrd 2.0.2 in the external environment suffice; preserve script/reader/code/config/input/result/check hashes, source attribution, extraction/pairing/weight versions and request revision. Profile/calibration/trend inputs remain unknown/not used.

External artifacts: under the existing Salford cache in `t08a5-anchor115-v1/`, versioned plan/contributors, `oxywatt-salford-anchor115.py`, independent checker, full output and rerun/check/authority records. Bound work by exactly 197 columns and one or two selected HR nodes each (at most 394 nodes, at most the primary's 390,252 positive-HR source rows); serial processing, no more workbooks or HR-level sweep. Counts are engineering bounds, not eligibility N or a physiological duration. No archive redownload if local checksum passes; no other dataset/private ride/contact authorized.

One newest-requested configuration/input revision is authoritative; pending/failed/unavailable newest report excludes older successes. Keep historical reports separate; late older completion cannot replace the newest request. Missing/changed required inputs invalidate reproduction. Freeze scalar outputs separately from request timestamps/output-location metadata so reruns compare semantic bytes. Do not write an AnalysisRepository scalar or history membership: origin-aware report provenance is not application V16 integration. Preserve synthetic exclusion. Canonical ReceiptStamp/SourceTime, Collector start/end/continuity, hold-v1 and profile/persistence contracts remain untouched; no observed start/end, canonical epoch, pause or time-in-ride is generated from a source column.

## Acceptance and file ownership

L01–L06 are task-local acceptance cases; no new top-level V IDs.

| Case | Observable verification / honest refusal | Mapping |
|---|---|---|
| L01 | Required archive/member/current T08a4/config/reader identities pass; before-output 197-column endpoint/contributor freeze is power-neutral and matches the 115-level presence/bracket rule. Verify predicted 188/9 classification without forcing it | R02/R07/R10 → V02/V09/V12 source/report only |
| L02 | Independently read each selected endpoint's actual source rows; exact rational reference validates counts, sums, means, ranges, zero flags, row identity and final value within one binary64 ULP of correctly rounded reference. Exact method uses only 115; interpolation uses nearest strict bracket and HR-distance weights; convex-hull invariant | R02/R07 → V02/V09 arithmetic only |
| L03 | All 197 output slots accounted for; methods/unavailable/errors disjoint. One-row/zero/duplicate-header flags retained, no sign/slope filtering, endpoints remain within inherited support geometry. No numerical failure becomes zero or a complete report | R02/R07/R08 → V02/V09/report-only V10 |
| L04 | Full rerun gives byte-identical semantic output; actual newest pending/failed/unavailable and late older completion never return old success. Lost/changed required input/current T08a4 authority/config/contributor identity refuses; interrupted output records expected/completed counts and no complete available result | R07/R10 → V09/V12 report integrity only |
| L05 | All exact/interpolated scalars and detailed identities remain local; aggregate report labels uncleaned/index-paired/exact-or-interpolated, discloses evidence mix and limits, no physiological/chronological ranking or claim-bearing method selection | R06/R08/R10 → V08 claim limits/report-only V10/V12 |
| L06 | Actual commands/versions/hashes and L01–L05 outcomes retained; task/dossier/Canvas/log/backlog/work-package/validation/scientific synchronization, relative links/anchors, R/T/V traceability and whitespace PASS | R07/R10 → V09/V12 documentation only |

Exact repository files: this task, T08a4 task cross-link, existing T08a2 dossier, `docs/spdd/reasons-canvas.md`, `docs/spdd/decision-log.md`, `docs/spdd/implementation-plan.md`, `docs/spdd/tasks/agent-work-packages.md`, `docs/algorithms/validation-strategy.md`, `docs/discovery/scientific-research.md`. External research scripts/results stay outside Git. No T08a1/T08a3 equation/admission change, core/app/Gradle/skill/schema/UI/framework change, author contact or publication. No app/Android/hardware test appropriate for this external description.

Done requires full local result/refusal inventory plus independent checks, rerun/integrity and aggregate documentation evidence. Current owner approval covers this described variant and routine implementation choices within it, so no repeated execution approval is needed. A changed anchor, new fit/filter/sensitivity, other participant/grid or physiological/product claim needs its own scope decision. T02c/T03e/parent T04, T08b/c, T06/T07 and O1–O5 remain open. T08a3 J04 is not frozen by this separate raw-row method; J05–J07 remain unapproved/unperformed.


## Execution evidence — 2026-11-12

[Full aggregate results, commands, checks and artifact identities](../../evidence/t08a2-comparable-observations/README.md#t08a5-executed-115-bpm-stored-row-descriptor--2026-11-12). Frozen plan SHA-256 `4634900c353d8cabc525e6a0f325fafed9301178e83e0b65f69c38ae14ef1386`; contributor SHA-256 `4b7256d0a4d85db269b3751e1d1f81b665cc34aea69a37a43ceeee366ad5420c`, both before anchor-power calculation. Detailed data and both final runs are in the existing external cache under `t08a5-anchor115-v1/`; no scalar/identity table in Git.

| Case | Actual evidence |
|---|---|
| L01 | Required identities/current source and all 197 endpoint freezes PASS; predicted 188 exact/nine interpolated confirmed without outcome selection |
| L02 | All 197 outputs, 206 nodes and 3,226 contributors independently checked from raw source against exact integer-ratio references, nearest-HR selection and interpolation/convex-hull arithmetic; one-ULP engineering comparison PASS |
| L03 | All 197 outputs available, zero unavailable/numeric errors; 11 exact and six interpolated columns have one-row used points, 127 exact/five interpolated include Power-zero, five duplicate-header flags retained; all denominators PASS |
| L04 | Full second run byte-identical for values/summary; actual current/newest/no-fallback, required-input/config/contributor changes/loss, unavailable source-current and interrupted-write checks PASS. Interrupted producer reports one completed of 197, failed/incomplete and no current result |
| L05 | Full values/identities remain local. Exact/interpolated and raw uncleaned/index-paired limits explicit; no ranking, chronology, physiological claim or original A/B admission |
| L06 | Actual commands/versions/hashes/check/rerun evidence retained; aggregate dossier/task/Canvas/log/backlog/work-package/validation/scientific synchronization complete; final link/traceability/whitespace checks follow |

This delivers a common recorded-HR coordinate with mixed exact/interpolated evidence, not matched physiological exposure or a useful longitudinal trend. No new anchor/fit/filter, model/smoothing, private ride, app/core implementation or Git publication. All prior scientific/live/product gates remain open.


Final T08a5 documentation verification (2026-11-12): nine changed/new Markdown files, 292 relative links including anchors resolve; all 12 referenced R IDs and 23 V IDs resolve, L01–L06 contract and execution evidence present, six synchronized task links plus backbone/dossier cross-links checked. `git diff --check` and explicit new-task whitespace PASS. Self-review covers fixed source/anchor/nearest-endpoint selection, exact versus interpolated evidence, full output counts, retained zeros/single-row/artifact flags, privacy, unknown timing/context and unchanged canonical/profile/persistence/science gates. No application/hardware test appropriate or rerun; no app/core/build/skill change, commit/push/PR/merge. This completes the approved T08a5 source/report descriptor only.


## T08a6 approved single-row influence diagnostic — 2026-11-12

**Authorization/freeze snapshot; executed evidence follows below.**

Owner approved the described [T08a6](t08a6-salford-row-influence.md) diagnostic before ID reservation. Freeze all used T08a5 rows/nodes/weights/methods, omit each used row once without altering source, record exact W changes or required-node-empty refusal and complete per-column envelopes. M01–M06 require independent recomputation, full unavailable/partial denominators, deterministic rerun/current-input/no-fallback/interruption and privacy checks; execution pending. This is deterministic sensitivity, without physiological uncertainty or arbitrary adequacy thresholds. Recommend ending the present fixed-table numerical sensitivity route here, then deciding a separately scoped timing/context evidence route. No publication or further automatic experiment sequence authorized.


## T08a6 executed single-row influence — 2026-11-12

[Actual results and verification](../../evidence/t08a2-comparable-observations/README.md#t08a6-executed-single-row-influence-diagnostic--2026-11-12): all 3,226 deletions enumerated, 3,204 numeric and 22 required-node-empty refusals; all 197 envelopes accounted for (180 complete, one partial, 16 all-unavailable). Exact independent full-recomputation checks, byte-identical rerun and current-input/no-fallback/interruption checks PASS. Deterministic sensitivity is not calibrated uncertainty or physiological repeatability; no adequacy threshold, cleaning or ranking. M01–M06 source/report only. Recommend ending this fixed-table numerical sensitivity route and deciding a separately scoped timing/context evidence route. Detailed W/identities/scripts stay outside Git; earlier local T08a5 work and all scientific/live/product gates preserved. No publication authorized.


## T08a5/T08a6 publication authorization — 2026-11-12

Owner explicitly requested a PR for the completed T08a5 and T08a6 documentation. This authorizes commit, push and publication of the ten scoped Markdown files, including both task specifications, aggregate results, verification and synchronized references. Earlier no-publication statements describe their preceding execution stages. Detailed source/column/row/Power values, research scripts and complete result tables remain outside Git. No merge, new numerical experiment, source acquisition/contact, application implementation or physiological/product admission authorized.

Publication self-review covers the complete diff and both new tasks: fixed source/115-bpm anchor/method/endpoints, exact versus interpolated evidence, single-row and zero flags, complete 3,226-deletion/197-envelope denominators, unavailable versus zero and partial envelope labels, independent/rerun/current-input/no-fallback evidence, claim limits and privacy. Ten Markdown files only; no raw participant records, dates/notes, private contributor identifiers, local research tools/results, credentials or application code staged. Relative-link/anchor checks (314), requirement/validation and L01–L06/M01–M06 traceability, new-file whitespace and `git diff --check` PASS. No application/hardware tests rerun for documentation-only publication; actual external source/report checks remain the execution evidence. Verify remote PR head/base and CI separately after publication.
