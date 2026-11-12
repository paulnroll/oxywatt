# T08a4 — Salford exact-HR stored-row description

Status: **Owner approved T08a4 exact-HR row description v1 execution on 2026-11-12. K01–K06 execution and verification complete for owner review; finite stored-row description only.** Search of the backlog, work packages and repository documentation found T08a4 unused before reservation. This is a separate subdivision of T08a; it does not rename T08b/c or replace T08a3 J04–J07.

## Decision requested and purpose

Recommend approving **T08a4 exact-HR row description v1** as the smallest next real-data numerical scope, accepting the explicit development assumptions below. Deliver a finite empirical description, not an A/B estimator comparison. The alternative is to defer numerical work until separately authorized source-verifiable timing/context acquisition; no further archive audit is proposed here.

Question: **Under equal-source-index Power/HR pairing, what is the arithmetic mean stored power at each exact positive recorded HR level in each of the fixed primary workbook's 197 structural columns, and how do these conditional means differ between columns at levels both actually contain?** This asks about the deposited cells. It does not ask what power a rider can sustain at a physiological HR anchor, whether a column is a complete ride, or whether fitness changed.

Dependencies: [T08a3 completed support](t08a3-salford-index-ab.md), [actual support/quality evidence](../../evidence/t08a2-comparable-observations/README.md#t08a3-executed-support-discovery--2026-11-12), [T08a2 source semantics](t08a2-comparable-observations.md), [unchanged T08a1 A/B](t08a1-power-hr-trend.md), [validation strategy](../../algorithms/validation-strategy.md), [ADR 0002](../../architecture/adr/0002-independent-estimators.md). No new evidence/source search is needed to define this finite empirical quantity.

## Exact estimand and outputs

For source column c and exact stored HR value h, define `I(c,h) = {i: H_ci = h > 0, P_ci is finite and >= 0}` under the retained structural index-pair hypothesis. No rounding, bins, interpolation or inferred sentinel conversion. A numeric XLS cell means its numeric value, excluding booleans, text, blanks and errors, with the pinned reader's existing extraction semantics. Let `n(c,h) = |I(c,h)|` and `m(c,h) = sum(P_ci for i in I(c,h)) / n(c,h)` W. This is a **finite empirical conditional row mean**, not a population expectation, duration-weighted mean or estimate of an unobserved HR level. Each stored row has weight one, including the last row; no support interval is assigned to it.

Emit one local table for every column at every level in the primary's observed HR union: n, sum P, mean P when n>0, minimum/maximum P and Power-zero count, with existing quality flags and local row contributors. At n=0, mean/min/max are absent with `no_exact_level_rows`, never zero. n=1 is reportable as a one-row descriptor with min=mean=max; it is not adequate physiological evidence. Counts are disclosed, with no minimum-N filter or precision claim.

For every unordered primary-column pair c<d in original source-coordinate order, and every level in that union, emit `D(c,d,h) = m(d,h) - m(c,h)` W only when both means exist; otherwise record c-only, d-only or neither support. Order is a reproducibility convention, not chronology. Report both row counts, both Power-zero counts and both within-cell power ranges beside each difference. **Do not average differences across HR levels, select a favorable HR level/pair, or collapse them into a session scalar, ranking, change direction or calendar trend.** There is no common all-column exact HR, so no universal exact-level scalar is promised.

The numerical deliverable is these complete local tables plus an aggregate report of level/pair support, unavailable/error dispositions, checks and limitations. The aggregate may state how many columns support each level and how many pair-level cells are available; it must not publish per-column power records, exact dates/notes/coordinates, selected largest effects or a power-selected illustrative pair. Keep detailed descriptors/differences outside Git. This scope can expose how much a comparison depends on the chosen recorded HR level; it cannot settle which level is useful for real riding.

## Differences from original A/B

| Dimension | T08a1 / conditional T08a3 A/B | Proposed T08a4 |
|---|---|---|
| Unit and time | Qualified nonoverlapping segments with jointly covered measured/declared durations, observed lifecycle/context | Equal-index stored row pairs only; source columns are not qualified sessions; no elapsed time |
| A | Duration-weighted band mean at a frozen anchor | No A, HR band, anchor or duration weighting; exact-level row means |
| B | Local weighted linear fit, bracketing, positive slope and approved conditioning/residual/stability rules | No B, slope, fit, extrapolation or invented fit cutoffs |
| Comparison target | Comparable qualified segments/exposure; potential session contrasts | Within-exact-recorded-HR differences of deposited row means only; other exposure/context is unmatched |
| History and interpretation | Synthetic software evidence; real-use/physiology gates separate | Exploratory archive description only, no baseline, index, trend, repeatability, p-value or confidence interval |

Equal exact HR matches only the stored predictor value. It does not match time-in-ride, workload history, serial dependence, measurement error, sensor settings or physiological state. HR display quantization and unequal row frequencies remain source features; do not manufacture continuous HR precision or standardized exposure. This design deliberately declines a single cross-HR target instead of supplying an arbitrary reference distribution.

## Frozen proposed choices, evidence and assumptions

The one-binary64-ULP check tolerance below is an explicit numerical engineering choice, not measurement precision or a meaningful-change threshold. Compute reference differences before rounding the two means separately, so cancellation does not silently redefine the target.

Version the prospective configuration as `salford-exact-hr-rows-v1`; the owner accepted this configuration with explicit T08a4 execution approval on 2026-11-12. Approval of this whole named scope accepts its following development choices, not their physiological validity.

| Choice | Basis / status |
|---|---|
| Source | Existing Salford four-rider DOI `10.17866/rd.salford.3080560.v1`, Figshare article 3080560/file 4787857, CC BY 4.0; retain Scarf/Salford attribution. Archive SHA-256 `834e371c8aaf1e9fd04cf9c44619d045ac18dd3270ab7f2653e1f3692afe0c19`. No redownload if verified local bytes pass |
| Primary and set | Inherit the pre-support lexicographic workbook identity and 5 s structural stratum, all 197 columns, no fallback. The main numerical table excludes other workbooks/grids by design, not after power results; their existing support inventories remain historical context. No new analyses there |
| Pairing | Same source index in the retained sheet pair, **unverified developmental assumption**; equal headers/extents do not establish synchronization. No channel shifting/lag fitting |
| HR levels | All exact observed positive levels, not a selected subset; primary union has 201 levels in existing evidence. No anchor/band. In particular 115 and 112–118 remain T08a3 support proposals, unused here |
| Row admission | Finite/nonnegative P and finite positive HR; reuses the support pass's technical value domain, **not validated artifact removal or work qualification** |
| Weights / summary | Equal row weight and arithmetic mean, an explicit conventional descriptive choice. Min/max and zero count expose part of the raw spread, not calibrated uncertainty. No assumption of equal actual duration or representative independent sampling |
| Contrasts | All 19,306 unordered primary-column pairs, all union levels; pair membership fixed independently of power. No temporal ordering, baseline, person pooling or HR-level aggregation |
| Sensitivity | One configuration only. No fitted or thresholded candidate requires numerical perturbation. No claim of robustness to timing, artifacts, rounding or row weighting; assessing alternatives needs a separately specified revision approved before outputs |

No physiological parameters are derived here. The primary set, HR union and available support are empirical/inherited; equal-index pairing, equal weights, arithmetic means, complete enumeration and absence of cleaning are development assumptions requiring acceptance. The source does not validate them. Numerical execution must pin the configuration/hash and exact local contributor identity list before any HR-conditioned power outputs. No parameter may be revised because the power results look unfavorable.

## Artifact policy and refusal

Retain source values unchanged. HR=0 is absent from positive-HR means, counted separately; do not label its cause dropout/pause. Power=0 remains an observed value in both mean and range, even when it may represent coasting or a sentinel of unknown meaning. Positive extreme HR/power, isolated spikes, duplicate dates and zero-containing columns are not silently removed; all existing flags remain. Without source-linked artifact encoding, **uncleaned** is a mandatory label. A same-HR cell or contrast can be mathematically defined and still be dominated by an artifact. No clipping, winsorization, trimmed mean, inferred warm-up, stability/settling rule or cherry-picked index region.

Refusals are separated by layer:

- Absent exact-level support refuses only that mean/contrast; keep its denominator and reason. No bracketing or adjacent-HR replacement.
- Hash/member/reader/extraction/config mismatch, lost input, unexpected structural identity/count/HR-union change or script failure invalidates the requested report as an operational failure. Do not silently shrink the set or substitute a workbook. New technical invalid values in this pinned input require investigation, not outcome-dependent cleaning.
- Numeric conversion or nonfinite arithmetic refuses the affected output with an explicit error; never publish infinity/NaN/zero in its place. A run with numerical errors is incomplete, with expected versus completed output counts reported.
- Actual synchronized power-at-HR, qualified visit comparability, longitudinal change, physiological benefit and independent repeatability remain **not assessable under this design**, even if every table cell is computed.

No synthetic series fills missing levels or validates the real source. No profile, mass, HRmax, FTP or demographic linkage is supplied. Missing calibration/environment/lifecycle evidence stays unknown.

## Bounded local operations and integrity

Use the retained external cache `/Users/pawelkozicki/Library/Caches/oxywatt/research/salford-3080560-v1/`, xlrd 2.0.2 in the external temporary environment and standard library only. Later approved execution may create `oxywatt-salford-exact-hr-rows.py`, versioned plan/result/check files there; no script or data enters the project. Reuse prior hash/ledger evidence, verify pinned input integrity at execution, do not repeat the support/source audits. If the reader environment is gone, recreate it outside project dependencies.

Stream the primary workbook once for per-column/level aggregates, then enumerate pair tables from those aggregates serially. Bounds are derived from the fixed input: 394,026 paired rows (390,252 positive-HR + 3,774 HR-zero), at most 197 × 201 column-level cells and 19,306 × 201 pair-level cells, including unavailable entries. These are engineering enumeration limits, not eligibility N or actual duration. Exceeding the pinned inventory, cancellation, disk-full or memory failure produces failed/incomplete status, never a complete-looking truncated table. Do not retain all pair-level results in memory. Preserve deterministic column/level ordering; no extra arbitrary runtime/memory threshold becomes a scientific rule.

Record archive/member/reader/script/config/check/result hashes, extraction version, exact local row/column contributors, pairing/weight policy, code/environment versions, input revision and request revision. Profile/calibration/trend fields are explicitly not supplied/not used, not fabricated. Pin one newest-requested input/config revision; pending/failed/unavailable newest report has no fallback to an older successful revision. Keep old reports historical and recheck required input identities before returning results; lost required input makes reproduction unavailable. No automatic retry using a different person, level or configuration.

Canonical `ReceiptStamp`/`SourceTime`, Collector session/effect continuity, `CanonicalPowerReplay` hold-v1, `ExperimentProfile` and `AnalysisRepository` contracts remain unchanged. No canonical epochs/observed-start/end, pauses, durations, persisted scalar, app database or trend membership is generated. Report authority is an offline provenance obligation, not a new V16 persistence implementation. Synthetic evidence remains separately labeled/excluded from real history; scalar-v1 alone still does not guarantee origin isolation.

## Acceptance, files and authorization boundary

K01–K06 are **task-local acceptance** cases, not new top-level V IDs. The contract below was approved with the named scope; actual execution evidence is recorded at the end of this task.

| Case | Observable check on separately approved execution | Mapping |
|---|---|---|
| K01 | Pinned archive/member/reader/config and all 197 local column identities match existing support; primary 394,026 rows reconcile with positive/zero counts, all 201 exact HR levels retained; no fallback | R02/R07/R10 → V02/V09/V12 source/report only |
| K02 | Every column-level n matches the retained histogram; sums, mean, min/max and Power-zero counts independently recomputed from source using exact rational representations of numeric reader values, rounding only on output. Exact counts/sets; floating output agrees within one binary64 ULP of correctly rounded rational reference; nonfinite results refuse | R02/R07 → V02/V09 arithmetic only |
| K03 | Every pair-level cell has exactly one both/c-only/d-only/neither disposition; at each h with k supporting columns, mathematically supported pairs equal k(k−1)/2 (numeric failures remain separate). Every available D independently checked from K02 references within one binary64 ULP of correctly rounded exact mean difference; all others absent; no hidden pair/level selection | R07/R08 → V09/report-only V10 |
| K04 | Same inputs/config give byte-identical semantic tables on full rerun, execution timestamps stored separately; newest failed/pending/unavailable, late older completion and missing/changed required input checks never return an old success as current. Error/resource interruption marked incomplete; all expected outputs reconciled | R07/R10 → V09/V12 report integrity only |
| K05 | Report labels raw uncleaned index-paired rows, discloses all weights/denominators/artifact/context unknowns, retains n=1 and Power-zero, and makes no A/B, chronological/physiological ranking, independence or precision claim; no private detail in Git | R06/R08/R10 → V08 claim limits/report-only V10/V12 |
| K06 | Script/plan/check/result versions/hashes and actual commands/check outcomes recorded locally, aggregate evidence and synchronized task/Canvas/log/backlog/validation/scientific documents reviewed, links/traceability/whitespace PASS | R07/R10 → V09/V12 documentation only |

Exact repository scope: this task; T08a3 task cross-link; T08a2 evidence dossier cross-link and later aggregate result; `docs/spdd/reasons-canvas.md`; `docs/spdd/decision-log.md`; `docs/spdd/implementation-plan.md`; `docs/spdd/tasks/agent-work-packages.md`; `docs/algorithms/validation-strategy.md`; `docs/discovery/scientific-research.md`. No changes to T08a1 equations/admission/evidence, core/app/build/skills, importer or reusable framework. Check arithmetic against real retained source on execution; independent arithmetic checks are not additional synthetic evidence of scientific validity. No application/hardware test needed for this document-only proposal.

Done **now** means a complete reviewable scope/estimand/assumption/refusal/check specification, not numerical results. Done **after named execution approval** requires all K01–K06 with actual evidence or explicit failed/incomplete status; no result size/direction is an acceptance target. No execution is implied by approval of T08a3 support discovery or by publication of its report. Git publication requires separate owner authorization, recorded below; merge remains unapproved.

T08a3 J04 stays NO-GO/not frozen and J05–J07 unapproved/unperformed. T02c/T03e/parent T04, T08b/c, T06/T07 and all physiological O1–O5 gates remain open. This scope advances a precise archive-description question while leaving the owner's useful within-person trend unvalidated.

## Session baseline verification — 2026-11-12

Repository GitHub identity verified as `paulnroll` using `GH_CONFIG_DIR=.git/gh-personal`. Contrary to the handoff's earlier local-only snapshot, [PR #40](https://github.com/paulnroll/oxywatt/pull/40) is MERGED at 09:15:49 UTC: head `1fc6562e89766368af9358b614b39052968cb95a`, merge `0d7de35d751ea977a85cc46a96c7bf2438fe055c`. All four checks across runs 43189699684 and 43189817152 are SUCCESS; main run 43189916874 is SUCCESS. The nine inherited runs 43095615450, 43095608185, 43095780996, 43096566790, 43096560344, 43096948167, 43097566082, 43097541824 and 43097770138 remain queued with no conclusion in the current read; none is claimed successful.

Initial working tree was clean on the already published documentation branch. Fetched origin; local main fast-forwarded from e42ff25 to 0d7de35, preserving the tracked T08a3 document and all merged evidence. New local branch `docs/salford-row-description-proposal`; no PR created or merged by this session. Read-only canonical time/lifecycle/hold/profile/persistence review confirms the proposal assigns no source timestamps, sessions, profile evidence or scalar history. No source data or external research script executed, no application/hardware tests rerun.

## Proposal verification — 2026-11-12

Self-review distinguishes stored-row arithmetic from qualified A/B and physiology, checks the outcome-independent primary/all-level/all-pair selection, raw artifact retention, unequal/context-unmatched exposure, refusal/status denominators, input authority and private contributor boundaries. No original T08a1 document/code changed. Documentation check `/tmp/oxywatt-check-row-proposal.py`: nine changed/new Markdown files, 271 relative links including anchors resolve; all referenced 12 R and 23 V IDs resolve; K01–K06 each defined once and six synchronized task links present. New-file whitespace check and `git diff --check` PASS. These are specification checks only; K01–K06 numerical execution remains pending owner approval. Skills read but unchanged; no skill validator needed. No source audit rerun, app/hardware tests, commit, push or new PR.


## Execution authorization — 2026-11-12

Owner explicitly approved the proposed T08a4 scope after the specification review. This accepts its equal-index pairing, equal-row weights, exact-HR means, complete same-level primary-pair enumeration, raw artifact policy and K01–K06 checks for development description only. Local external research scripts and numerical execution are authorized; no original A/B, app/core implementation, new source/contact, commit/push/PR/merge or physiological gate is authorized. Earlier proposal-only statements are historical snapshots.


## Executed results and acceptance — 2026-11-12

[Full aggregate dossier and artifact identities](../../evidence/t08a2-comparable-observations/README.md#t08a4-executed-exact-hr-stored-row-description--2026-11-12). Accepted plan SHA-256 `e559fd080af590beed4d73994d2f4c59150d5b8e3dc4b2e223240991c6a88d12` and contributor ledger SHA-256 `8a341aa53ba68c9f60905b3cd15b31ee501f35e07b7ecfd0c250fa7d77fa3ea7` were frozen before HR-conditioned power calculation. Scripts/config/checks and both complete final runs retained under the existing external cache's `t08a4-exact-hr-rows-v1/` subdirectory. Detailed cells and contributor identities remain outside Git.

| Case | Actual evidence / result |
|---|---|
| K01 | All 197 fixed-primary columns and 201 exact HR levels verified against retained identities; 394,026 source rows = 390,252 positive-HR + 3,774 HR-zero. No new selection/fallback |
| K02 | All 39,597 column-level cells independently checked from source with integer-scaled exact rational references. 15,189 means, 24,408 absent levels; 2,271 numeric means have one row. Counts/ranges/zero flags/row contributors and values PASS |
| K03 | All 3,880,506 pair-level cells independently checked: 1,074,462 both, 303,128 c-only, 524,992 d-only, 1,977,924 neither. 187,894 numeric differences have at least one one-row side. Values/dispositions/all-level combination identities PASS; zero numerical errors |
| K04 | Complete second final run gives byte-identical semantic tables/summary. Actual authority checks and injected producer write interruption verify failed/incomplete status, output counts and no current fallback; required input/config/contributor changes/loss invalidate reproduction |
| K05 | Uncleaned/raw-index labels and counts retained, no favorable subset or power effect selected; no timing/physiological/trend inference or private detailed table in Git. Self-review PASS |
| K06 | Exact commands, script/config/output/check hashes and rerun record retained; aggregate dossier, task/Canvas/log/backlog/work-package/validation/scientific synchronization complete; final document checks recorded below |

The delivered output is a reproducible **finite stored-cell description**. No exact HR level supports every column; no universal scalar or useful trend follows from these tables. Original T08a1 equations/admission unchanged; T08a3 J04–J07 and all live/product/physiological gates remain open. No application or hardware checks appropriate or rerun; no Git publication performed.


## Final execution verification — 2026-11-12

Independent checks PASS before and after preservation in the external cache; relocated artifact locators resolve and all semantic hashes match both full final runs. The retained `final-1/check.json` has SHA-256 `d1da33d080bd5e846a51b1d705b50bf799d95a4cf51e957ba8225835dd990bf5`. All 39,597 column-level and 3,880,506 pair-level cells checked; authority/input-loss/config-change and actual write-interruption checks PASS. Full final rerun matches every byte of all four semantic output files. K01–K06 complete at source/report layer only.

Final self-review covers complete selection/denominators, index pairing/equal-row weighting, raw artifact retention, single-row exposure, private contributor exclusion, no temporal/physiological scalar and unchanged T08a1/T08a3 admission. `/tmp/oxywatt-check-row-proposal.py`: nine changed/new Markdown files, 280 relative links including anchors PASS; all 12 referenced R and 23 V IDs resolve, K01–K06 contract/evidence and synchronized backlog/work-package links checked. `git diff --check` and explicit untracked-task whitespace check PASS. The link checker was adjusted to distinguish K-case definitions from the new execution evidence table; an initial duplicate-definition alert reflected those two tables, not missing traceability. No application/Android/hardware tests appropriate or rerun, no skills changed, no commit/push/PR/merge.


## Publication authorization and self-review — 2026-11-12

Owner explicitly requested a commit, push and PR for the completed T08a4 documentation. This authorizes publication of the nine scoped Markdown files, including the accepted specification, aggregate results, source/report verification and synchronized references. Earlier no-publication statements record the prior stages. No merge, further numerical analysis, new source/contact, application implementation or physiological admission is authorized.

Self-review covers the complete diff including this new task: arithmetic/count reconciliation, all-level/all-pair selection, single-row/zero exposure, unknown pairing/context/artifacts, exact rational and deterministic-rerun evidence, current/no-fallback integrity, unchanged T08a1/T08a3 gates and English documentation. No raw workbooks, source rows, per-column power/difference tables, participant dates/notes/identities, local scripts/results or credentials are staged. Detailed artifacts remain in the external cache.

Documentation checks: nine changed/new Markdown files, 280 relative links including anchors resolve; referenced R/V IDs and K01–K06 contract/evidence/backlog/work-package traceability PASS. `git diff --check` and new-task whitespace PASS. No application or hardware tests rerun for this documentation-only publication; prior source/report execution checks remain the recorded evidence. Repository GitHub identity and current base checked before publication; PR/remote CI state is read back after creation, never assumed passed.
