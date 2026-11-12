# Dependency-ordered implementation backlog

Status: T01a and owner-resumed T01b complete; [T01a evidence](tasks/t01a-canonical-input.md), [T01b evidence](tasks/t01b-synthetic-replay.md). [T01c evidence](tasks/t01c-fit-replay.md) closes the developer FIT work package. The owner approved [T02a](tasks/t02a-diagnostic-host.md). Real Karoo mapping is planned for T02b. M0 ends with this review packet; approval must name tasks.

| Task | Goal | Dependencies | Requirement / tests | Output / risk / Done |
|---|---|---|---|---|
| T01 | Canonical input, lifecycle and deterministic replay contract | Architecture approval | R01/R02/R11; V01/V02/V03 | `core` contracts, JVM fixtures, decoder/license decision; risk time semantics; done when two source mappings preserve units/provenance and fixture oracles pass |
| T02 | Verify Karoo lifecycle/timing and toolchain | T01; named permission for diagnostic code/device run | R02/R09/R11; V02/V11/V13 | Android diagnostic slice and evidence report; risk unreliable binding; done when startup/4-hour/kill scenarios recorded and acquisition design accepted or rejected |
| T03 | Durable, bounded collection | Accepted ADR 0004 permits T03a–d after simulation contracts, with T03e/live acceptance still gated by T02. | R03/R07/R10/R12; V04/V05/V09/V12/V14/V16/V17 | Android repositories/checkpoints plus core lifecycle, migrations; risk ambiguous identity/disk loss; done when interruption, storage-full, hard-cap overload, generation/supersession and idempotency tests pass |
| T04 | End-to-end observed power curve | T03 | R04; V06/V13 | Core integration + app readout; risk false rolling windows; done when device ride reaches persisted curve and independent irregular-time oracle agrees |
| T05 | Reproducible model experiment protocol | T05a: existing simulation/persistence/curve software and public synthetic fixtures; T05b: T05a software for protocol design; lawful references required for experiments | R05/R06/R07; V07/V08/V09 | Reproducible fixtures/profiles and reviewed evaluation protocol with go/no-go decisions; risk no lawful ground truth; T06/T07 retain candidate audit/selection and later experiments |
| T06 | Select then implement FTP model | T04, T05 and method approval | R05; V07/V09 | Core FTP strategy/config + evidence; risk nonmaximal effort; done when numerical and eligibility tests pass and validity limits reviewed; CP cannot satisfy FTP automatically |
| T07 | Select then implement VO₂max model | T05 and method approval; T03 for device integration | R06; V08/V09 | Independent core model + abstention; risk unidentified physiology; done when equations, coefficients, sensitivity, applicability and external validation evidence reviewed |
| T08 | History, smoothing and basic chart | T03/T04; T06/T07 for numeric estimator acceptance | R07/R08/R12; V09/V10/V14/V15/V16 | Persisted runs/contributors, app UI; risk hidden bias/cohort mixing; done when raw vs trend, partial-session exclusion, staleness, unavailable supersession and deletion/recalculation work |
| T09 | Release hardening and publication readiness | T04/T06/T07/T08; license decision | All applicable scenarios | Signed artifact/release evidence after authorization; risk compatibility/privacy; done when hardware, migration, offline and science gates pass, docs current and publication explicitly authorized |

First vertical slice is T01 → T02 → T03 → T04: **Karoo → Collector → Persistence → Power Curve**. T02 first tests the acquisition hypothesis; it does not silently become a separate production feature. Detailed scope: [first-slice task](tasks/first-vertical-slice.md).

For smaller agent contexts, see the proposed [work packages and handoff template](tasks/agent-work-packages.md). These subdivide existing tasks without authorizing implementation or bypassing dependencies.

T05 can use developer FIT replay before any product import UI. T06 and T07 are logically independent once shared evidence is ready; no agent parallelization is required. Failure of a scientific gate leads to reviewed scope/method change, not an arbitrary formula.

Review checkpoints: M0 architecture; T02 device feasibility; T04 first slice; each model selection before code; full-release evidence before publication. No estimated implementation dates until hardware/toolchain and model gates are understood.


## Accepted simulation-first architecture — 2026-11-11

Architecture ACCEPTED; T01d/T01e complete and T02d owner-approved. [ADR 0004](../architecture/adr/0004-simulation-first-testing.md) and the [change task](tasks/simulation-first-development.md) define dependencies, bounded files, acceptance and risks. See [T02d adapter tests](tasks/t02d-karoo-adapter-tests.md) for its scope and evidence; other packages retain named approval gates.

Proposed order is T01d (source port, virtual clock and generators) → T01e (lawful real FIT replay) and T02d (SDK fake compatibility and mapping tests) → applicable T03a–d software packages. Existing T03a → T03b/T03c → T03d dependencies still apply. Hardware T02c evidence can proceed alongside that approved software work. T03e remains gated by the passed T02 acquisition decision. T04 still follows T03. T05–T07 scientific gates remain unchanged.

T02b has partial host/state evidence and pending real-sensor checks. T02d makes its mapping and cleanup regressions repeatable. T02c concentrates the hardware matrix. SDK fakes, emulator and external replay are evidence layers, each with limits in V18–V22. Individual package completion cannot close parent V11/V13 or physiological validity.

## Planned on-device preview checkpoint

After T03a–d, [T03p](tasks/t03p-diagnostic-preview.md) adds a debug-only screen over the real demo Collector/repository: latest readings with age/missing reasons, committed/pending events, gaps/loss and storage health. Owner requested this plan on 2026-11-11; implementation still needs named approval and completed component contracts. It uses synthetic simulation/FIT replay in a separate demo database and does not require or close the live acquisition gate. Sequence: T03a → T03b/T03c → T03d → T03p; then T03e after passed T02c, followed by T04. V23 records screen/integration and actual Karoo preview acceptance.

T03a is owner-approved and implemented locally: [lifecycle/checkpoint contract and evidence](tasks/t03a-collector-lifecycle.md). It provides pure JVM session assignment, conservative recovery and per-channel coverage effects; durable repository and bounded admission remain T03c/T03b. Next dependency-ordered implementation package is T03b, requiring named approval. T03p remains after T03a–d.


T03p software implementation (2026-11-11, owner-approved after PR #17): isolated debug-derived preview consumes actual T03a/b/c pipeline and schema v2. Shared test fixtures and stream FIT reader remain outside release. Received versus committed values, virtual ages, pending/loss, interruption and storage errors are inspectable. Last committed receipt time is explicitly distinguished from an unavailable durable transaction timestamp. See [T03p evidence](tasks/t03p-diagnostic-preview.md). V22/V23 software checks do not close the on-Karoo screenshot/device claim, T02 or live T03e.


Proposed software-only T04a amendment — 2026-11-11: the owner requests useful simulator-backed work with live tests at the end. [T04a](tasks/t04a-power-curve-engine.md) can implement the pure observed power-duration engine over explicitly covered intervals after T01/T03a policy review, before T03e. T04b canonical-to-interval preprocessing, persistence/readout and parent first-slice acceptance remain separately scoped and live-gated. T02c/T03e are deferred, not accepted or silently enabled. Named T04a implementation approval is still required; this supersedes the blanket T04 sequencing only for the proposed pure engine.


T04a was approved after PR #21 and is software-complete: [confirmed-interval engine/evidence](tasks/t04a-power-curve-engine.md), 14 new tests and 82 core passes plus one private-FIT skip. The approved narrow dependency amendment allows this engine before T03e; T04b canonical preprocessing, persistence/readout and integration workload need their own contract/approval. Parent T04 first-slice/device acceptance and T02/T03e gates remain open.

## Next proposed software package after PR #22

[T04b1 — terminal synthetic persisted curve](tasks/t04b-persisted-curve.md) is proposed, not implementation-approved. Dependencies T03a–d/T03p/T04a are software-complete. One complete package adds revision-bounded committed effect replay, explicit hold/expiry preprocessing, bounded T04a scan, typed curve terminal through existing T03d and durable preview readout. B1–B7 define software acceptance without bicycle/Karoo; live T02c/T03e and parent T04 remain open. This proposed amendment supersedes earlier blanket live-first T04b sequencing only for synthetic software work. No continuous analytics/history/physiology; dense-scan optimization may follow separately if needed.

T04b1 implemented after owner approval following merged PR #23: [terminal synthetic persisted curve](tasks/t04b-persisted-curve.md). Committed revision-bounded effects are converted with preview-silence-v1 hold/expiry to T04a; one typed T03d generation atomically stores all durations and partial/resource provenance. Existing preview reads saved results after reopen; synthetic curves cannot enter aggregate membership. Software evidence: core 90 passes/one private-FIT skip, debug 92 passes, preview 103 passes; builds/lint/release isolation pass. Dense scans have an explicit conservative pre-call work cap. No live T03e, device action, physiological model or private FIT read; T02c and parent T04 live acceptance remain open.


## T04b2 proposal — 2026-11-11

Next proposed package after merged PR #24: T04b2, complete long variable synthetic terminal scans with bounded stable range integration, charged work and existing persisted replay/reopen/readout at 1x/10x/unpaced. Dependencies T03a–d/T03p/T04a/T04b1 software-complete. B2-01–08 are unexecuted acceptance, not completion evidence. Named implementation approval required; live T02c/T03e/parent T04 remain open. See [T04b2 specification](tasks/t04b2-long-variable-scan.md).


## Approved T04b2 implementation — 2026-11-11

Owner approved [T04b2](tasks/t04b2-long-variable-scan.md) after specification PR #25 merged. The existing core/power now uses bounded persistent AVL range means and charged work-v2, with exact candidates/coverage/tolerance retained. Canonical hold-v1, the existing logical slot, scalar-v1/curve-v2, atomic newest-generation storage and durable preview readout remain in the same flow. Independent exact oracles and long synthetic speed/reopen checks provide software evidence; see task for final results and resource bounds. This replaces T04b1's conservative quadratic reservation for new calculations, without rewriting old records. No private FIT, live acquisition or physiological model; T02c/T03e and parent T04 live acceptance remain open.

## T05 execution boundary after PR #26

Proposed [T05a](tasks/t05a-fixture-reproducibility.md): fixture provenance/rights, complete generator recipes, separate FTP/VO₂max input profiles and fixed-identity semantic replay/reopen checks using existing manifests and storage. A01–A07 provide measurable software acceptance and R/V mapping; none executed yet. Depends on completed simulation/persistence/curve software, without private FIT or hardware. Named implementation approval pending. T05b separately owns evaluation protocol, sensitivity and scientific go/no-go. T05 completion and T06/T07 model approvals still require reviewed scientific evidence; no lawful paired reference dataset is secured. Live T02c/T03e/parent T04 remain open.


## T05a implementation — 2026-11-11

Owner approved T05a after merged PR #27. [Implementation/evidence](../evidence/t05a-software-2026-11-11/README.md) covers descriptor/profile development support and replay acceptance A01–A07. Parent T05 is not complete: T05b scientific evaluation/protocol and lawful references are still required. No estimator or live gate is accepted.


## T05b protocol proposal — 2026-11-11

[Canonical T05b specification](tasks/t05b-evaluation-protocol.md). T05b / T05: proposed protocol review and worked synthetic reporting exercise, B01–B08 (R05/R06/R07/R10/R12; V07/V08/V09 plus software reuse mappings). Depends on merged T05a software, not live acceptance. Expected output: reviewed protocol, toy disposition/metrics/split tables and O1–O5 go/no-go register. No code or physiological experiment authorized. T06/T07 keep method selection and later implementation separate; live T02c/T03e/parent T04 remain open.


## T05b executed protocol exercises — 2026-11-11

Owner approved T05b after merged PR #29. [B01–B08 report](../evidence/t05b-protocol-review/README.md) completes the bounded documentation exercise: separate FTP/VO₂max estimands, ten invented qualification cases, participant split/freeze rehearsal, reconciled coverage/refusal/error arithmetic, conditional sensitivity and explicit O1–O5 decisions. Self-reviewed evidence is ready for owner review; it is not a physiological study. Both experiment admissions remain NO-GO; no lawful matched dataset, justified scientific thresholds/sample plan or method selected. T06/T07 retain independent research/selection and later implementation; no automatic authorization follows. T05 protocol/reproducibility deliverables are now recorded, while parent scientific acceptance and deferred T02c/T03e/parent T04 live gates remain open.


## T06a scope proposal — 2026-11-11

[Named T06a scope](tasks/t06a-ftp-method-audit.md) defines a bounded primary-source FTP audit after merged T05b PR #30. F01–F08 map R05/R07 (plus input/privacy R02/R10) to V07/V09 and conditional V02/V12/V18/V19: search log, equation/protocol/population/reference audit, current-input compatibility, conditional sensitivity, rights/data availability and a reviewable recommendation. Named audit execution approval is pending; this specification chooses no method. O1–O5, experiment admission, separate T06b implementation and later live gates remain explicit. T07a stays independent. No application code, private FIT, dataset download, device action or new framework.


## T06a executed audit — 2026-11-11

Owner approved T06a after specification PR #32 merged. [F01–F08 report](../evidence/t06a-ftp-method-audit/README.md) records the bounded primary-source audit, source-access/correction limits, input compatibility, conditional sensitivity and FTP O1–O5 decisions. Recommendation is to retain model_not_approved for automatic FTP from ordinary rides; no audited candidate qualifies on present evidence. An optional controlled-protocol investigation needs a separate scope decision and cannot silently replace ordinary-ride intent. No method, dataset, estimator or experiment is admitted; T06b and independent T07a remain separate. Source numerical results are centralized in the report, not new product accuracy thresholds. R05/physiological V07 and later live acceptance remain open.


## T07a executed independent audit — 2026-11-11

Owner authorized T07a following T06a; PR #33 merged during work and local main/branch updated. [G01–G08 report](../evidence/t07a-vo2max-method-audit/README.md) centralizes primary equations, protocol/population/reference and rights/access findings, input compatibility and conditional sensitivity. Recommendation: retain model_not_approved for ordinary-ride VO₂max on present evidence. VO₂max O1–O5 remain unresolved/blocked; no method, experiment, dataset or T07b implementation admitted. Any optional controlled-protocol direction requires a separate scope decision. FTP remains independent; R06/physiological V08 and deferred T02c/T03e/parent T04 live acceptance remain open. No application code, private FIT, dataset download, device action or framework.

## Owner direction: prioritize within-person change — 2026-11-11

Owner prioritizes useful trends over precise absolute laboratory agreement for R05/R06/R08. [Requirements](../product/requirements.md) record this intended use; [validation strategy](../algorithms/validation-strategy.md) separates change tracking from absolute agreement. T05b O2 is narrowed, with numeric criteria still open. Next specification may define an explicitly labeled exploratory fitness trend; no method/estimator is approved. Repeated physiological references remain necessary for physiological change claims; smoothing and cross-sectional agreement alone do not establish longitudinal validity.


## Next proposed package — T08a1

[T08a1 experimental comparable power–HR trend](tasks/t08a1-power-hr-trend.md) subdivides existing T08a. Dependencies are implemented T03/T04/T05a software plus T05b/T06a/T07a review contracts (PR #34 merged). Specification/publication is authorized; execution is pending a named owner decision. Smallest complete delivery: synthetic A/B comparative calculation from committed replay, raw session/baseline report, persisted scalar/reopen/generation regressions and H01–H07 evidence. No method automatically selected.

T07 remains VO₂max research/selection and later T07b; T08a1 does not satisfy R06 or admit a physiological experiment. T08b/c product storage/deletion/chart follows later reviewed policy. Existing T08 dependency on T06/T07 applies to numerical physiological estimator acceptance, not this isolated nonphysiological software experiment. No chart/smoother, new storage/manifest/framework or live implementation; T02c/T03e/parent T04 live gates remain open.


## T08a1 executed software experiment — 2026-11-11

Owner approved [T08a1](tasks/t08a1-power-hr-trend.md) after PR #35 merged. [H01–H07 evidence](../evidence/t08a1-power-hr-trend/README.md) records the synthetic A/B experiment through actual committed replay, scalar-v1/reopen, newest authority, fixed-baseline/raw reports and fault/cohort/resource checks. Ten new core tests and ten API 32/34 preview executions pass; full core 117 passes/one private-FIT skip, debug 92/preview 116, build/lint/release isolation and final focused checks pass. No production method/estimator, chart, schema, store or manifest was added.

Both candidates reproduce injected observational changes but show confounder-driven false directions; neither is selected for physiological interpretation. Software H01–H07 does not close O1–O5, R05/R06/physiological V07/V08, T07/T07b, T08b/c or deferred T02c/T03e/parent T04 live gates. No participant data or hardware evidence. Smoothing and legacy trend-v0 numeric defaults remain unapproved for this route.


## T08a2 qualification and pilot-readiness proposal — 2026-11-11

[T08a2](tasks/t08a2-comparable-observations.md): Next named execution proposal: documentation-only qualification and pilot-readiness dossier I01–I07. Depends on T08a1 software and T05a/T05b/T07a contracts. Delivers source audit, common-support/time/context policy, measurement worksheet, frozen metrics plan and admission/blocker decision. No core/app changes or new infrastructure.

Specification/publication only is authorized; named execution awaits owner approval. N, windows, HR/drift thresholds, meaningful change, baseline horizon and smoothing remain open with resolution paths. No lawful dataset admitted; real-data/scientific pilot is NO-GO. T05b O1–O5, physiological R05/R06, T02c/T03e/parent T04 live gates and T08b/c remain open. No bike, Karoo or private FIT required.


## T08a2 executed qualification review — 2026-11-11

[Task/evidence](tasks/t08a2-comparable-observations.md): T08a2 I01–I07 execution is complete as a bounded design dossier, ready for owner review. Next evidence dependency is an actual lawful repeated-observation acquisition path with adequate context, then separately approved development observations; no such dataset is admitted. Numeric qualification/precision/usefulness decisions block frozen evaluation. This is not authorization for a new implementation slice.

Real-data/scientific admission remains blocked; N, qualification thresholds, meaningful change, baseline horizon and smoothing remain unresolved without defaults. T05b O1–O5, T02c/T03e/parent T04 and T08b/c remain open. No new framework, storage, manifests or UI.


## T08a2 source-index discovery continuation — 2026-11-12

[Executed timing/alignment inspection and next scope](../evidence/t08a2-comparable-observations/README.md#source-specific-timealignment-inspection--2026-11-12): source-duration formulas justify only a nominal index-grid hypothesis, not synchronized or lifecycle-qualified visits. Smallest next work remains a report-only availability summary in T08a2 discovery, with explicit reconstructed-time limits and excluded/unresolved denominators. No new task ID, importer/replay/A/B implementation or method approval; T08b/c keep their existing meanings. A later implementation/A/B proposal must reserve an unused subdivision and gain named approval first. T02c/T03e/parent T04 and scientific/pilot gates remain open.


T08a2 source-index availability continuation executed with owner approval: [report](../evidence/t08a2-comparable-observations/README.md#executed-source-index-availability-report--2026-11-12). Frozen descriptive plan, reconciled counts and row/column distributions delivered without implementation or A/B. Next evidence dependency is original timing/export and artifact semantics; comparative pilot remains blocked. Existing T08a2 ID retained; T08b/c and later implementation gates unchanged.


T08a2 source recording/export documentation continuation completed with owner approval: [source matrix/disposition](../evidence/t08a2-comparable-observations/README.md#executed-source-recordingexport-semantics-review--2026-11-12). Stop archive-only comparable-pilot qualification at the missing original timing/settings/row-transformation boundary. Further source-linked acquisition/contact or an assumption-limited A/B experiment requires its own scope/authorization; no new implementation ID reserved or executed here. Current report-only discovery is complete; T08b/c, live gates and scientific admission remain unchanged.


## Proposed T08a3 — conditional Salford index-time A/B experiment — 2026-11-12

[T08a3 canonical package](tasks/t08a3-salford-index-ab.md) reserves the next unused T08a subdivision. At proposal time the owner requested specification preparation, not execution. The initially proposed next unit was **T08a3 support discovery**: actual per-column exact-level HR support, separate workbooks/grids, fixed outcome-independent primary identity, all-position reasons and a concrete numerical-plan proposal. No A/B in this unit. After support evidence, a complete configuration must be accepted and **T08a3 A/B execution** explicitly approved before calculations. No synthetic defaults or invented numerical thresholds; no app/importer/replay/storage/UI. J01–J08 define report-layer acceptance; subsequent execution status is recorded below. All Salford is exploratory development, not held-out validation. T08b/c and live/scientific gates unchanged.


## T08a3 support discovery executed — 2026-11-12

Owner approved **T08a3 support discovery**. [J01–J03/J08 evidence](../evidence/t08a2-comparable-observations/README.md#t08a3-executed-support-discovery--2026-11-12) completes the unit with exact per-column HR support, fixed primary/no fallback, all-pair inventories and independent deterministic checks. The 115 bpm / 112–118 bpm primary support-geometry proposal has sparse/unequal occupancy, not a frozen numerical configuration. J04 remains NO-GO/not frozen; J05–J07 unperformed/unapproved. Next is a scope choice between simpler row descriptors under assumptions and the original timed/context-qualified route, not automatic A/B execution. Full experiment, product/live/science gates remain open.


## T08a4 — proposed exact-HR row description — 2026-11-12

**Historical proposal snapshot; named approval and execution are recorded below.**

[Named scope and K01–K06](tasks/t08a4-salford-exact-hr-rows.md), checked unused before reservation. Next proposed decision: accept `salford-exact-hr-rows-v1` and explicitly authorize T08a4 execution, or defer for a separately scoped timed/context-documented source. Complete proposal covers fixed primary, all exact HR levels, raw row means/same-level differences, no-cleaning assumptions, refusals, deterministic integrity and independent arithmetic checks. No original A/B configuration claimed ready; no numerical execution now, no T08b/c reassignment.


## T08a4 executed stored-row description — 2026-11-12

Owner explicitly approved the complete T08a4 configuration and numerical execution. [Aggregate results and verification](../evidence/t08a2-comparable-observations/README.md#t08a4-executed-exact-hr-stored-row-description--2026-11-12): fixed-primary 197 columns, all 201 exact HR levels, 15,189 raw means (2,271 one-row) and 1,074,462 same-level differences (187,894 with a one-row side); every unavailable cell retained. Equal-index pairing remains assumed, weights are equal stored rows and artifacts remain uncleaned. No anchor, A/B, qualified segment, cross-HR scalar, trend or physiology. Independent source/rational checks and full final rerun provide report-only evidence; exact data/scripts/results retained outside Git. T08a3 J04–J07 and all live/scientific/product gates unchanged. No application code authorized. Subsequent report-publication authorization is recorded in the T08a4 task.


## T08a5 approved 115 bpm stored-row descriptor — 2026-11-12

**Authorization/freeze snapshot; completed execution evidence follows below.**

Owner approved the described exact-at-115 / nearest-bracket linear interpolation variant, now reserved as [T08a5](tasks/t08a5-salford-anchor115.md) after checking the unused ID. Freeze the full rules and contributor endpoints before numerical execution. One output per fixed-primary column; direct/interpolated methods, counts, ranges and zero/quality flags stay explicit. Pairing and local linearity remain development assumptions, no A/B/trend/physiology or app implementation. L01–L06 define source/report arithmetic, complete denominators, independent checks, rerun/current-input/no-fallback/privacy evidence; pending execution. No new acquisition/contact or publication authorized; T08a3 and all live/scientific/product gates unchanged.


## T08a5 executed 115 bpm descriptor — 2026-11-12

[Executed results and verification](../evidence/t08a2-comparable-observations/README.md#t08a5-executed-115-bpm-stored-row-descriptor--2026-11-12): all 197 fixed-primary outputs available, 188 exact and nine interpolated, 206 HR nodes and 3,226 actual source-row contributors. Seventeen columns have a one-row used point; 132 include Power-zero in a used point, all retained. Complete independent source/rational checks, full byte-identical rerun and current/no-fallback/interruption checks PASS. Exact versus interpolated evidence remains explicit; local linearity, index pairing, artifact and context limits unchanged. L01–L06 report/source evidence only, no original A/B, chronological/physiological trend, app change or Git publication. Detailed scalars/contributors/scripts remain outside Git; all live/scientific/product gates stay open.


## T08a6 approved single-row influence diagnostic — 2026-11-12

**Authorization/freeze snapshot; executed evidence follows below.**

Owner approved the described [T08a6](tasks/t08a6-salford-row-influence.md) diagnostic before ID reservation. Freeze all used T08a5 rows/nodes/weights/methods, omit each used row once without altering source, record exact W changes or required-node-empty refusal and complete per-column envelopes. M01–M06 require independent recomputation, full unavailable/partial denominators, deterministic rerun/current-input/no-fallback/interruption and privacy checks; execution pending. This is deterministic sensitivity, without physiological uncertainty or arbitrary adequacy thresholds. Recommend ending the present fixed-table numerical sensitivity route here, then deciding a separately scoped timing/context evidence route. No publication or further automatic experiment sequence authorized.


## T08a6 executed single-row influence — 2026-11-12

[Actual results and verification](../evidence/t08a2-comparable-observations/README.md#t08a6-executed-single-row-influence-diagnostic--2026-11-12): all 3,226 deletions enumerated, 3,204 numeric and 22 required-node-empty refusals; all 197 envelopes accounted for (180 complete, one partial, 16 all-unavailable). Exact independent full-recomputation checks, byte-identical rerun and current-input/no-fallback/interruption checks PASS. Deterministic sensitivity is not calibrated uncertainty or physiological repeatability; no adequacy threshold, cleaning or ranking. M01–M06 source/report only. Recommend ending this fixed-table numerical sensitivity route and deciding a separately scoped timing/context evidence route. Detailed W/identities/scripts stay outside Git; earlier local T08a5 work and all scientific/live/product gates preserved. No publication authorized.
