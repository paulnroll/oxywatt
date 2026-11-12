# T08a6 — Single-row influence on the 115 bpm descriptor

Status: **Owner approved the described single-used-row deletion diagnostic on 2026-11-12 (“no to jedziemy”).** ID checked unused in documentation/backlog/work packages before reservation. Freeze the scope/contributors before calculations. M01–M06 complete for owner review at source/report layer. Preserve the already local T08a5 documents; no commit/push/PR/merge or application implementation authorized.

## Question, dependencies and boundary

How much does each fixed T08a5 115-bpm descriptor change when exactly one of its used stored rows is omitted, keeping its HR nodes, interpolation weights and exact/interpolated branch frozen? Report every deletion and every unavailable result; original source remains unchanged. This is a finite mathematical influence diagnostic, not a noise model, calibrated uncertainty, physiological repeatability or prospective validation.

Dependencies: [T08a5 method and evidence](t08a5-salford-anchor115.md), [T08a4 means](t08a4-salford-exact-hr-rows.md), [existing dossier](../../evidence/t08a2-comparable-observations/README.md), [validation](../../algorithms/validation-strategy.md), [ADR 0002](../../architecture/adr/0002-independent-estimators.md). Same Scarf/Salford four-rider DOI `10.17866/rd.salford.3080560.v1`, article 3080560/file 4787857, CC BY 4.0, archive SHA-256 `834e371c8aaf1e9fd04cf9c44619d045ac18dd3270ab7f2653e1f3692afe0c19`. Fixed primary 197-column nominal 5 s stratum, no fallback, other workbook/grid, acquisition/contact or archive redownload.

## Frozen estimand and refusal

For a used node e in column c, let its raw powers have sum S, count n, empirical mean m=S/n, and frozen T08a5 weight a_e. Baseline descriptor is `x_c = sum(a_e m_e)`. Enumerate each used source row exactly once, deleting only that row from its node and keeping all other nodes unchanged.

- If n>1, deletion result is `x_c,−i = x_c + a_e (m_e − p_i)/(n−1)` W. Signed influence `delta = x_c,−i − x_c`; no rounding before the final reported value/difference.
- If n=1, result is unavailable `required_node_empty`. Do not set its mean to zero, drop the required node, change HR levels/weights or switch exact to interpolated (or vice versa). Source loss/hash mismatch is an operational failure, distinct from this deliberate diagnostic refusal.
- Source P/HR values and original T08a5 descriptors are never mutated. Every Power-zero and positive extreme participates as originally admitted. No trimming, favorable deletion, sign/positive-slope filter, minimum-n cutoff, anchor sweep, further fit or artifact-cleaning claim.

This deliberate perturbation is additional scope accepted by the owner; it does not retroactively extend T08a5's one-configuration numerical definition. Equal-index pairing, equal weights within HR nodes, uncleaned artifacts, local interpolation and missing source-time/context remain assumptions. One stored row is not necessarily an independent physical sample; deletion is not a calibrated error or plausible acquisition fault distribution.

## Outputs and full denominators

Local `deletions.jsonl`: column/source-row/node identity, frozen method/HR/weight, n, baseline exact value, status/reason, deleted power, exact rational deletion value and signed delta plus once-rounded W when available. Complete intended pool is the T08a5 3,226 used rows in 206 nodes over all 197 columns. Freeze exact contributor identities and expected unavailable count from node n before new values. Every occurrence gets one status; never omit unavailable scenarios.

Local `column-envelopes.jsonl`: one record per all 197 columns, method, requested/available/unavailable/error counts, retained baseline, and min/max deletion W, min/max signed delta and maximum absolute delta **over available deletions only**. Mark `all_available`, `partially_unavailable` or `all_unavailable` explicitly. If none are finite, envelope fields are null, not zero; a finite envelope does not hide additional unavailable deletions. Do not include the unperturbed baseline as an extra deletion. Repeated values remain separate source-row scenarios even when their numeric changes coincide. No ranking or selecting the largest example for reporting.

Aggregate Git report: source/method/scenario/column counts, unavailable/complete/partial envelope denominators, exact-zero-influence counts if useful, hashes/checks/limits and the scope decision. Detailed W values/ranges, per-column identities, participant dates/notes and source rows stay outside Git. No across-column power statistic, threshold classifying “stable enough”, baseline normalization, pairwise chronology, trend, smoothing, FTP/VO₂max or qualified A/B. Envelopes are **deterministic single-row sensitivity ranges**, never confidence/credible intervals or physiological error bars.

## Operations and integrity

Revision `salford-row-influence-v1`, one reference configuration. Freeze plan and all 197-column/206-node/3,226-row contributors before new deletion outputs. Verify current T08a5 authority is available and all pinned archive/member/reader/T08a5 plan/result/contributor/check/helper identities. Reconstruct selected raw node powers and exact baseline against T08a5; do not use rounded baseline/means. Use standard library plus existing external xlrd 2.0.2 environment only. Serial work bounded by this fixed source pool; one deletion per used row and one envelope per column. These are engineering limits, not physiological N. No new support/time/availability audit or synthetic replacement series.

External artifacts under the retained Salford cache in `t08a6-row-influence-v1/`: frozen plan/contributors, `oxywatt-salford-row-influence.py`, independent checker, full deletion/envelope tables, summary and rerun/authority/check metadata. Pin code/reader/input/config/result hashes, origin `real-public-development`, source attribution and extraction/pairing/weight versions. Profile/calibration/trend remain unknown/not used. Keep all detailed data and research scripts outside Git; no project dependency, importer or framework.

Newest-requested configuration/input revision is authoritative. Pending/failed/unavailable newest report excludes older successes; late older completion cannot replace it. Required source/report/config/contributor change/loss invalidates reproduction; no T08a5 run fallback. Verify identities/current source before finalizing and reading. Operational/numerical/resource failures record failed/incomplete status and expected/completed table counts; never publish a truncated success. Exact rational arithmetic with once-rounded binary64 outputs; independent check tolerance one binary64 ULP is engineering conformance, not measurement precision. Request timestamps/relocated paths are separate from deterministic semantic tables.

Canonical ReceiptStamp/SourceTime, observed start/end, Collector continuity, hold-v1, profile/AnalysisRepository/storage contracts unchanged. No canonical epochs/time/duration, pauses or ride chronology inferred from a column. No synthetic/history membership or physiological/product gate is admitted. Existing T08a4 vetted offline authority functions may be reused with source hash pinned; this is report metadata, not application V16 implementation.

## Acceptance and files

M01–M06 are task-local cases, not new top-level V IDs.

| Case | Observable criterion | Mapping |
|---|---|---|
| M01 | Required current source/reader/config/identity checks and before-output contributor freeze match all 197 columns, 206 nodes and 3,226 used rows; no outcome-based reselection | R02/R07/R10 → V02/V09/V12 source/report only |
| M02 | Independently reconstruct every selected raw node and baseline, explicitly remove each row and recompute full weighted descriptor using exact integer-ratio references; all deletion values/deltas within one binary64 ULP, every n=1 removal unavailable | R02/R07 → V02/V09 arithmetic only |
| M03 | Every deletion and all 197 envelopes independently reconcile; min/max/delta/absolute envelopes exclude unavailable variants and label partial/all-unavailable; null versus zero, Power-zero and one-row support retained. No arbitrary adequacy threshold or physiological interval | R02/R07/R08 → V02/V09/report-only V10 |
| M04 | Full rerun byte-identical semantic output; actual newest pending/failed/unavailable/late completion and missing/changed current-input/config/contributor refusal; injected output interruption records exact partial counts and no available result/fallback | R07/R10 → V09/V12 report integrity only |
| M05 | Full local outputs and explicit assumptions/limits, no raw or per-column data in Git, no favorable deletion/chronology/physiological claim; data unchanged | R06/R08/R10 → V08 claim limits/report-only V10/V12 |
| M06 | Actual commands/versions/hashes and M01–M05 evidence retained, task/dossier/Canvas/log/backlog/work-package/validation/scientific synchronization and links/R/T/V/whitespace PASS | R07/R10 → V09/V12 documentation only |

Repository scope: this task, T08a5 task cross-link, existing T08a2 dossier, `docs/spdd/reasons-canvas.md`, `docs/spdd/decision-log.md`, `docs/spdd/implementation-plan.md`, `docs/spdd/tasks/agent-work-packages.md`, `docs/algorithms/validation-strategy.md`, `docs/discovery/scientific-research.md`. Preserve earlier local T08a5 work. No app/core/build/skill/schema/UI/framework change, author contact or Git publication. No application/Android/hardware tests appropriate for this external source/report diagnostic.

Done requires complete local deletion/envelope tables or explicit refusal, independent checks, deterministic rerun/integrity and aggregate documented evidence. Owner approved the described diagnostic before assigning the ID; routine implementation choices need no repeated approval. Original T08a1/T08a3 J04–J07 and T02c/T03e/parent T04, T08b/c, T06/T07 and O1–O5 remain open.

## Scope endpoint

There is no fixed quota of experiments that establishes a fitness trend. Recommend this as the final single-row numerical sensitivity check of the present fixed 115-bpm table, then review what is delivered and decide the next evidence route. Further archive-only arithmetic cannot recover missing timing, synchronization, lifecycle, calibration or context. A next acquisition/reference plan needs a separately approved scope; no additional automatic numerical sequence or physiological eligibility is authorized here.


## Execution evidence — 2026-11-12

[Complete aggregate dossier, exact commands and artifact identities](../../evidence/t08a2-comparable-observations/README.md#t08a6-executed-single-row-influence-diagnostic--2026-11-12). Plan SHA-256 `c5e28559ffcf769dc49dbc747454d77ac8b20ef5104196644ff2e47f1ed78cb7` and contributor SHA-256 `2367b1b70c1d07c1ba2dfe565bd43f6ae2b6a64828be6ab93e52719d6fa13532` frozen before deletion values. Both full runs and detailed tables/checks are outside Git under `t08a6-row-influence-v1/` in the existing cache.

| Case | Actual evidence |
|---|---|
| M01 | All 197 frozen columns, 206 nodes, 3,226 contributors and current input/reader/config identities verified; 22 node-empty cases predicted from support before new outputs |
| M02 | All 3,226 variants independently checked by actual list omission/full exact weighted recomputation: 3,204 available and 22 unavailable, values/deltas/refusals PASS |
| M03 | All 197 envelopes reconcile: 180 all-available, one partial, 16 all-unavailable; exact extrema/nulls/counts PASS, zero numeric errors. Four finite envelopes have exact-zero influence, no validity classification |
| M04 | Full rerun byte-identical for all three semantic files; actual authority/dependency/config/contributor loss/change, newest/no-fallback/late completion and interrupted-write checks PASS. Interrupted output: one deletion and zero envelopes completed, expected 3,226/197, failed/incomplete |
| M05 | All local W ranges/row identities preserved outside Git; deterministic diagnostic label and full refusals/raw assumptions retained; no cleaning, favorable example, physiological uncertainty or trend claim |
| M06 | Source/script/config/output/check/rerun identities and commands retained; task/dossier/Canvas/log/backlog/work-package/validation/scientific synchronization complete; final document verification follows |

This completes approved single-row influence only. Recommend no automatic further fixed-table arithmetic experiment; timing/context/reference evidence and the intended trend still need a separately scoped decision. Earlier local T08a5 work preserved, no application/hardware tests appropriate or rerun, no Git publication or physiological/product gate approval.


Final T08a6 verification (2026-11-12): independent all-scenario/envelope/authority checks PASS before and after external-cache preservation; retained `final-1/check.json` SHA-256 `3736ad3ea5bfe23bd12e7b28a7a510bd38a8430f4c03b61c034aab1368209876`. Full rerun byte identity PASS for all three semantic outputs. Ten changed/new Markdown files (including preserved local T08a5 scope), 314 relative links including anchors and referenced 12 R/23 V IDs resolve; M01–M06 and L01–L06 contract/evidence and synchronized backbone/task links checked. `git diff --check` and explicit untracked-task whitespace PASS. Self-review confirms full refusal/partial-envelope denominators, source/method/weight preservation, no threshold/physiological interval, no private per-column W/date/identity table in Git, unchanged contracts/gates. No app/core/build/skill change or application/hardware test, no commit/push/PR/merge.


## T08a5/T08a6 publication authorization — 2026-11-12

Owner explicitly requested a PR for the completed T08a5 and T08a6 documentation. This authorizes commit, push and publication of the ten scoped Markdown files, including both task specifications, aggregate results, verification and synchronized references. Earlier no-publication statements describe their preceding execution stages. Detailed source/column/row/Power values, research scripts and complete result tables remain outside Git. No merge, new numerical experiment, source acquisition/contact, application implementation or physiological/product admission authorized.

Publication self-review covers the complete diff and both new tasks: fixed source/115-bpm anchor/method/endpoints, exact versus interpolated evidence, single-row and zero flags, complete 3,226-deletion/197-envelope denominators, unavailable versus zero and partial envelope labels, independent/rerun/current-input/no-fallback evidence, claim limits and privacy. Ten Markdown files only; no raw participant records, dates/notes, private contributor identifiers, local research tools/results, credentials or application code staged. Relative-link/anchor checks (314), requirement/validation and L01–L06/M01–M06 traceability, new-file whitespace and `git diff --check` PASS. No application/hardware tests rerun for documentation-only publication; actual external source/report checks remain the execution evidence. Verify remote PR head/base and CI separately after publication.
