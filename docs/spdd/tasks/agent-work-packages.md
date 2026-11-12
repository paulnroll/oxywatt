# Small implementation work packages

Status: T01a and owner-resumed T01b complete; see [T01a evidence](t01a-canonical-input.md) and [T01b evidence](t01b-synthetic-replay.md). [T01c](t01c-fit-replay.md) is complete at the developer FIT fixture level. [T02a](t02a-diagnostic-host.md) is owner-approved. [T02b](t02b-karoo-probe.md) is owner-approved. T02c and later packages remain unapproved. Parent tasks T01–T09 and all existing gates remain authoritative. This adds execution boundaries, not product scope. See [Canvas](../reasons-canvas.md), [backlog](../implementation-plan.md), [first slice](first-vertical-slice.md), and [decisions](../decision-log.md).

## Execution approach

Use one coordinator to maintain contracts, shared build configuration, integration and evidence. Start each worker with a fresh context and one package. Workers read root AGENTS.md, their approved task, the relevant requirement/validation sections and only the domain/SDK/science documents needed for that package. A small context is achieved through bounded scope and stable interfaces, not by omitting invariants.

For approved implementation use the [developer skill](../../../.agents/skills/oxywatt-developer/SKILL.md): small complete changes, red/green/refactor for domain behavior, and appropriate integration/device verification elsewhere. Skill creation is complete when its frontmatter, links and documentation checks pass; it does not complete any T01–T09 implementation or V scenario.

Before code, the coordinator records the accepted interface signatures, units, nullability, time semantics, reason codes, versions and fixture examples in the applicable architecture document. Producer and consumer tasks use that same contract. If a worker needs a material contract change, it reports the issue for integration and design synchronization rather than independently changing both sides. Routine implementation choices remain local.

Each task handoff contains: approved package and parent IDs; goal and exclusions; exact allowed files; direct dependencies and accepted contract revision; relevant R/N/V sections; verification command once tooling exists; Done evidence and unresolved limitations. Link canonical documents instead of copying the entire specification into every prompt. Discovery workers must read source evidence, not just a coordinator's summary.

The paths below are proposed ownership areas, not directories to scaffold now. Resolve exact namespace and files before dispatch. One worker owns any given file at a time. Shared build files and specification synchronization belong to the coordinator. Separate worktrees may isolate concurrent changes after code is authorized; they do not remove contract dependencies. No mandatory worker count, branch ceremony or additional physical modules is introduced.

## First slice decomposition

All packages require named implementation approval and their parent gates. “Ready” below describes dependency order only. V IDs refer to [validation strategy](../../algorithms/validation-strategy.md); completion records actual commands/results, never merely a worker's assertion.

| Package / parent | Goal and bounded ownership | Dependencies and minimal specialist reading | Verification / risk / Done evidence |
|---|---|---|---|
| T01a / T01 | Coordinator: resolve minimal JVM build and canonical input contracts; own shared build configuration and `core` contract types | Architecture approval; domain model, module structure, time/provenance requirements | R01/R02/R11, V01/V02/V03: units, missing values, epochs, recording/profile transitions represented; core compiles without Android. Risk: unstable shared interface; publish accepted signatures and commands |
| T01b / T01 | Synthetic replay fixtures and independent expected outputs; own core test-support files | T01a accepted contract; input/time sections of validation strategy | V01/V02/V03: deterministic irregular-time, missing-channel and lifecycle examples. Risk: fixtures mirror implementation; record independently derived expectations |
| T01c / T01 | Developer FIT mapping and replay verification; own test-only decoder/helper files | T01a; lawful fixtures and explicit decoder/license selection; FIT mapping sections | V01/V02: equivalent decoded inputs preserve values and distinguish timing provenance. Risk: decoder semantics/private data; record parser/license and safe fixture provenance; do not claim source equivalence from synthetic data alone |
| T02a / T02 | Coordinator: resolve Android toolchain and minimal diagnostic host; own app/build/manifest/extension configuration | T01 complete; named diagnostic approval; Karoo skill and feasibility matrix | R02/R09/R11, V02/V11/V13: build/install evidence on identified OS/build. Risk: incompatible toolchain; install is not lifecycle proof |
| T02b / T02 | SDK mapping and lifecycle probe; own `app` Karoo adapter/probe files | T02a; canonical contract; Karoo skill and relevant source evidence | V02/V11: measured receipt timing, missing/freshness behavior and state transitions; record exact SDK/host versions. Risk: invented timestamp or continuity claims |
| T02c / T02 | Coordinator with device operator: execute feasibility scenarios and decide acquisition gate; evidence documents only | T02b and authorized hardware access; full T02 scenario list | V11/V13: four-hour, hidden field, pause/reconnect, kill and force-stop evidence. Failure blocks T03 production acquisition; record accepted/rejected design and residual unknowns |
| T03a / T03 | Core lifecycle, identity and recovery state machine; own `core/collection` state-machine files | Accepted T01d and relevant T01e/T02d contracts under ADR 0004; T02 remains required for live acceptance; domain session/segment rules | R03, V03/V04/V05/V15: cold attach, gaps and ambiguous fragments never silently merge. Risk: false continuity; deterministic transition traces required |
| T03b / T03 | Bounded admission, loss accounting and flush policy; own distinct `core/collection` ingestion files | T03a state/port contract accepted; N01 in full | R03/R11, V17: count/payload limits include in-flight work, reserved controls and fail-closed exhaustion. Risk: hidden upstream buffering; stress evidence covers whole ingestion path |
| T03c / T03 | Transactional input batches/checkpoints and recovery repository; own input-storage files in `app/persistence` | T03a repository contract accepted; T02 storage compatibility evidence | R03/R07/R10/R12, V04/V05/V09/V12/V14: idempotency, restart, rollback, storage-full and migrations. Risk: non-atomic checkpoints; record fault-injection results |
| T03d / T03 | Authoritative analysis-generation storage and result ports; own distinct result-storage files | T03c schema/transaction contract accepted; generation policy in Canvas | R07/R12, V09/V16: newest-requested pointer, unavailable supersession, failed/pending exclusion and late completion. Risk: resurrecting stale results; repository fixtures suffice before estimators exist |
| T03p / T03 | Debug-only on-device preview of actual demo collection/storage; own app debug preview/readout and narrowly shared simulator support | T03a–d contracts complete; [preview task](t03p-diagnostic-preview.md); separate named implementation approval | R02/R03/R07/R10/R11, V23/V22: received versus committed values, UI-independent run, missing/error states, demo/release isolation and actual Karoo screenshots. Does not close T02c/T03e live gate |
| T03e / T03 | Coordinator: wire adapter, queue, writer and recovery; own orchestration files | T03a–T03d complete | Parent T03 checks including V17 against the integrated pipeline; no UI lifetime dependency. Risk: components pass in isolation but violate ordering/caps together; record integrated fault scenarios |
| T04a / T04 | Time-weighted power-duration engine; own `core/power` files | Approved simulation-only amendment: T01/T03a and reviewed [T04a](t04a-power-curve-engine.md) interval policy; full T03/live gate retained for T04b | R04/R11, V06/V13: required durations, gaps, irregular intervals and independent integral oracle. Risk: inflated maxima; numerical and bounded-state evidence |
| T04b1 / T04b / T04 | Proposed [terminal synthetic persisted curve](t04b-persisted-curve.md): effect replay, intervals, T04a, typed T03d terminal and preview readout | T03a–d/T03p/T04a complete; named T04b1 approval required; proposed software-only amendment before T03e | R02/R03/R04/R07/R10/R11/R12; B1–B7 map existing V IDs. Software acceptance without hardware; bounded work and durable generations required. Live parent acceptance stays open |

T01b and T01c may run concurrently after T01a. T03b and T03c may run concurrently after T03a freezes their shared contracts; T03d follows the schema agreement. Keep the rest sequential initially. The approved [T04a amendment](t04a-power-curve-engine.md) permits the pure interval engine before T03e; persisted/device integration still retains its separate acceptance gate. The coordinator reviews changes against requirements and runs combined checks before accepting each parent task.

## Later packages

Do not expand every future implementation task now: scientific decisions may change the work. Use these next boundaries when prerequisites are met:

- T05a: fixture provenance, dataset/profile manifest and replay reproducibility. T05b: evaluation protocol, sensitivity and go/no-go criteria. T05 completes only when both evidence and protocol are reviewed.
- T06a and T07a: independent FTP and VO₂max method-selection research, potentially concurrent after T05. Each requires the models skill and primary-source evidence. Each produces a reviewable method decision; no estimator code follows automatically.
- T06b and T07b: separately approved model implementation and abstention tests after method approval and each parent's dependencies. Each owns its own estimation package and numerical fixtures. Neither owns shared result-envelope files.
- T08a: history eligibility/cohort/trend policy and V15/V16 cases. T08b: persistence/deletion/recalculation integration after the policy contract. T08c: chart/readout after the query contract, covering empty, partial, unavailable and stale states. Parent numerical acceptance still requires T06/T07.
- T09: split release checks only after implemented features and target device evidence are known. Publication remains separately authorized.

Before dispatch, turn each later boundary into a task with exact files, dependencies, R/V IDs, risks and evidence criteria. This outline is not a ready-to-run prompt.

## Worker prompt template

```text
Implement only approved package <ID>, within parent <Txx>.
Read AGENTS.md, <task>, <relevant canonical sections>, <applicable skill>.
Accepted contracts/dependencies: <revision and completed evidence>.
Goal: <single observable outcome>.
Allowed files: <exact files>; shared files belong to coordinator.
Out of scope: <adjacent work>.
Acceptance: <R/N/V IDs and relevant scenarios>.
Run: <real commands established for this slice>.
Return: changed files, verification results, limitations, contract conflicts.
Do not mark device/science checks complete without corresponding evidence.
If a required contract conflicts with the spec, report it before dependent work.
```

Coordinator accepts the work, integrates it, records actual evidence in the parent task, and synchronizes Canvas/decision log when intent changes. A worker summary alone is not completion evidence. No Gradle or application checks have been executed for this planning document.

## Simulation-first proposal

The owner accepted the [simulation-first architecture](simulation-first-development.md) and ADR 0004 on 2026-11-11, approving T01d and subsequently T01e. The accepted amendment permits T03a–T03d software development after the relevant simulation contracts are accepted; every package still needs named approval. T03e live wiring and parent live acceptance remain behind passed T02 hardware acquisition checks. T02d and optional emulator/replay tooling retain their separate gates.

T03a follow-up: the owner approved the package after PR #13 merged. Its [implemented contract and core evidence](t03a-collector-lifecycle.md) define the checkpoint/effect boundary for T03b/T03c; database/live acceptance is not implied.

T03b is owner-approved after PR #14; see [contract and executed evidence](t03b-bounded-admission.md). T03c → T03d → T03p remains the later sequence; live T03e requires the T02 hardware gate.

T03c is owner-approved after PR #15; see [transaction contract and evidence](t03c-transactional-inputs.md). T03d/e/p still require named approval; no result tables or live orchestration are included.

T03d is owner-approved after PR #16; see [contract and evidence](t03d-analysis-persistence.md). T03p and live T03e remain outside this approval.


T03p software implementation (2026-11-11, owner-approved after PR #17): isolated debug-derived preview consumes actual T03a/b/c pipeline and schema v2. Shared test fixtures and stream FIT reader remain outside release. Received versus committed values, virtual ages, pending/loss, interruption and storage errors are inspectable. Last committed receipt time is explicitly distinguished from an unavailable durable transaction timestamp. See [T03p evidence](t03p-diagnostic-preview.md). V22/V23 software checks do not close the on-Karoo screenshot/device claim, T02 or live T03e.

Current T04b sequencing is defined by [T04b1](t04b-persisted-curve.md); older references to live-gated T04b apply to live integration/parent acceptance, not the proposed synthetic software amendment. Implementation remains unapproved.

T04b1 implemented after owner approval following merged PR #23: [terminal synthetic persisted curve](t04b-persisted-curve.md). Committed revision-bounded effects are converted with preview-silence-v1 hold/expiry to T04a; one typed T03d generation atomically stores all durations and partial/resource provenance. Existing preview reads saved results after reopen; synthetic curves cannot enter aggregate membership. Software evidence: core 90 passes/one private-FIT skip, debug 92 passes, preview 103 passes; builds/lint/release isolation pass. Dense scans have an explicit conservative pre-call work cap. No live T03e, device action, physiological model or private FIT read; T02c and parent T04 live acceptance remain open.


## T04b2 proposal — 2026-11-11

T04b2 / T04b / T04: proposed single package owning existing core/power integration/index and budget, narrowly affected app provenance/codec/readout and tests. Dependencies T04b1 and prior software contracts complete. R02/R03/R04/R07/R10/R11/R12; B2-01–08 map V06/V09/V13/V18/V19/V22/V23 and repository regressions. Risks: numerical regrouping, eviction candidates, bounded staging, stale generation. No delegation or implementation is authorized by this entry; named owner approval remains pending. See [T04b2 specification](t04b2-long-variable-scan.md).


## Approved T04b2 implementation — 2026-11-11

Owner approved [T04b2](t04b2-long-variable-scan.md) after specification PR #25 merged. The existing core/power now uses bounded persistent AVL range means and charged work-v2, with exact candidates/coverage/tolerance retained. Canonical hold-v1, the existing logical slot, scalar-v1/curve-v2, atomic newest-generation storage and durable preview readout remain in the same flow. Independent exact oracles and long synthetic speed/reopen checks provide software evidence; see task for final results and resource bounds. This replaces T04b1's conservative quadratic reservation for new calculations, without rewriting old records. No private FIT, live acquisition or physiological model; T02c/T03e and parent T04 live acceptance remain open.

## T05a proposed bounded package — 2026-11-11

[T05a](t05a-fixture-reproducibility.md) now defines exact development-support ownership, dependencies, descriptor/profile/hash contracts, A01–A07 and R/V mappings. Reuse T01d/T01e, T03a–d/T03p and T04 software contracts after merged PR #26. No private FIT, device dependency, estimator, second manifest system/storage or experiment framework. Implementation pending named approval. T05b remains separate evaluation protocol/sensitivity/go-no-go; parent T05 is not complete.


## T05a implementation — 2026-11-11

Owner approved [T05a](t05a-fixture-reproducibility.md) after merged PR #27; development support and replay tests reuse existing two-module contracts. [Software evidence](../../evidence/t05a-software-2026-11-11/README.md) maps A01–A07, with no production storage/model work. T05b and scientific/live gates retain their separate scope.


## T05b protocol proposal — 2026-11-11

[Canonical T05b specification](t05b-evaluation-protocol.md). T05b / T05 is now a bounded documentation/evaluation-design package: protocol, B01–B08 worked review report and synchronization only. Exact files, dependencies, risks and checks are in the task. Named execution approval pending. No new manifest/storage/runner, estimator, private FIT or device dependency. Lawful reference data and O1–O5 block experiment admission, not protocol design. T06/T07 own independent method decisions.


## T05b executed protocol exercises — 2026-11-11

Owner approved T05b after merged PR #29. [B01–B08 report](../../evidence/t05b-protocol-review/README.md) completes the bounded documentation exercise: separate FTP/VO₂max estimands, ten invented qualification cases, participant split/freeze rehearsal, reconciled coverage/refusal/error arithmetic, conditional sensitivity and explicit O1–O5 decisions. Self-reviewed evidence is ready for owner review; it is not a physiological study. Both experiment admissions remain NO-GO; no lawful matched dataset, justified scientific thresholds/sample plan or method selected. T06/T07 retain independent research/selection and later implementation; no automatic authorization follows. T05 protocol/reproducibility deliverables are now recorded, while parent scientific acceptance and deferred T02c/T03e/parent T04 live gates remain open.


## T06a scope proposal — 2026-11-11

[Named T06a scope](t06a-ftp-method-audit.md) defines a bounded primary-source FTP audit after merged T05b PR #30. F01–F08 map R05/R07 (plus input/privacy R02/R10) to V07/V09 and conditional V02/V12/V18/V19: search log, equation/protocol/population/reference audit, current-input compatibility, conditional sensitivity, rights/data availability and a reviewable recommendation. Named audit execution approval is pending; this specification chooses no method. O1–O5, experiment admission, separate T06b implementation and later live gates remain explicit. T07a stays independent. No application code, private FIT, dataset download, device action or new framework.


## T06a executed audit — 2026-11-11

Owner approved T06a after specification PR #32 merged. [F01–F08 report](../../evidence/t06a-ftp-method-audit/README.md) records the bounded primary-source audit, source-access/correction limits, input compatibility, conditional sensitivity and FTP O1–O5 decisions. Recommendation is to retain model_not_approved for automatic FTP from ordinary rides; no audited candidate qualifies on present evidence. An optional controlled-protocol investigation needs a separate scope decision and cannot silently replace ordinary-ride intent. No method, dataset, estimator or experiment is admitted; T06b and independent T07a remain separate. Source numerical results are centralized in the report, not new product accuracy thresholds. R05/physiological V07 and later live acceptance remain open.


## T07a executed independent audit — 2026-11-11

Owner authorized T07a following T06a; PR #33 merged during work and local main/branch updated. [G01–G08 report](../../evidence/t07a-vo2max-method-audit/README.md) centralizes primary equations, protocol/population/reference and rights/access findings, input compatibility and conditional sensitivity. Recommendation: retain model_not_approved for ordinary-ride VO₂max on present evidence. VO₂max O1–O5 remain unresolved/blocked; no method, experiment, dataset or T07b implementation admitted. Any optional controlled-protocol direction requires a separate scope decision. FTP remains independent; R06/physiological V08 and deferred T02c/T03e/parent T04 live acceptance remain open. No application code, private FIT, dataset download, device action or framework.


## T08a1 — proposed subdivision of T08a

[T08a1](t08a1-power-hr-trend.md) is the next named execution proposal, pending owner approval: development-only fixed-band versus interpolated power-at-HR comparison on existing synthetic recipes, committed replay/native SQLite reopen, session/baseline report and H01–H07 checks. Own only the test-fixture/test/report files listed in the task and synchronized evidence. Shared canonical/storage contracts remain unchanged. No delegation requirement is introduced.

Handoff contract: input revision/terminal identity, paired segment coverage, recipe context/profile/calibration/config versions, existing hashes and newest-requested scalar results. Output is a reproducible experimental report, not a selected physiological method or product chart. T07/T07b and T08b/c remain independent later approvals; T02c/T03e/parent T04 are open.


## T08a1 executed software experiment — 2026-11-11

Owner approved [T08a1](t08a1-power-hr-trend.md) after PR #35 merged. [H01–H07 evidence](../../evidence/t08a1-power-hr-trend/README.md) records the synthetic A/B experiment through actual committed replay, scalar-v1/reopen, newest authority, fixed-baseline/raw reports and fault/cohort/resource checks. Ten new core tests and ten API 32/34 preview executions pass; full core 117 passes/one private-FIT skip, debug 92/preview 116, build/lint/release isolation and final focused checks pass. No production method/estimator, chart, schema, store or manifest was added.

Both candidates reproduce injected observational changes but show confounder-driven false directions; neither is selected for physiological interpretation. Software H01–H07 does not close O1–O5, R05/R06/physiological V07/V08, T07/T07b, T08b/c or deferred T02c/T03e/parent T04 live gates. No participant data or hardware evidence. Smoothing and legacy trend-v0 numeric defaults remain unapproved for this route.


## T08a2 qualification and pilot-readiness proposal — 2026-11-11

[T08a2](t08a2-comparable-observations.md): T08a2 is the next unused subdivision of T08a. Own only the exact documentation files listed in the task; extend its source evidence into I01–I07 after named approval. No implementation or participant execution. No delegation requirement. T08b/c remain separate subsequent packages.

Specification/publication only is authorized; named execution awaits owner approval. N, windows, HR/drift thresholds, meaningful change, baseline horizon and smoothing remain open with resolution paths. No lawful dataset admitted; real-data/scientific pilot is NO-GO. T05b O1–O5, physiological R05/R06, T02c/T03e/parent T04 live gates and T08b/c remain open. No bike, Karoo or private FIT required.


## T08a2 executed qualification review — 2026-11-11

[Task/evidence](t08a2-comparable-observations.md): Owner-approved T08a2 I01–I07 document execution completed; exact scoped documents only. The dossier contains qualification/disposition, freeze-plan, metrics/denominators and open-decision/admission evidence. No software, participant activity or additional agent package was dispatched. Lawful data/context acquisition needs its own concrete review and authorization.

Real-data/scientific admission remains blocked; N, qualification thresholds, meaningful change, baseline horizon and smoothing remain unresolved without defaults. T05b O1–O5, T02c/T03e/parent T04 and T08b/c remain open. No new framework, storage, manifests or UI.


## T08a3 — Salford support discovery and conditional index-time A/B

[Canonical scope/files/acceptance](t08a3-salford-index-ab.md). ID checked unused before reservation; J01–J08 are task-local cases. Specification preparation only authorized. Next proposed execution unit: **T08a3 support discovery**, J01–J03/J08, per-column exact HR support and J04 parameter proposal using retained Salford only. Later accepted J04 and explicit **T08a3 A/B execution** approval gate J05–J08. No new agent/delegation requirement. Output is local external research artifacts plus aggregate evidence in existing documentation; no core/app or canonical/storage/profile change. Keep person/grid/column provenance distinct; assumptions cannot satisfy T08a2 pilot admission. T08b/c retain existing meanings.


T08a3 support unit owner-approved and executed: [support dossier](../../evidence/t08a2-comparable-observations/README.md#t08a3-executed-support-discovery--2026-11-12), J01–J03/J08 complete. Actual exact-level support/overlap/concentration and quality findings do not resolve the qualified-segment policy; J04 is NO-GO/not frozen, J05–J07 remain unperformed/unapproved. No application or numerical worker dispatched; local research scripts/results retained outside Git, no new agent/delegation requirement. Any simplified row-based experiment needs an explicit scope decision; no silent change to T08a1 equations/admission.


## T08a4 — proposed stored-row descriptive package — 2026-11-12

**Historical proposal snapshot; named approval and execution are recorded below.**

[T08a4 exact scope/files/K01–K06](t08a4-salford-exact-hr-rows.md) follows T08a3 support evidence under a separate estimand, not J04–J07 execution. Own only the listed documentation and, after named approval, external local research artifacts. Fixed primary/no fallback; all exact HR levels and primary-column pairs; uncleaned equal-index/equal-weight arithmetic; no app/core/canonical/storage change or agent delegation. Specification only authorized; numerical work awaits the explicit scope decision.


## T08a4 executed stored-row description — 2026-11-12

Owner explicitly approved the complete T08a4 configuration and numerical execution. [Aggregate results and verification](../../evidence/t08a2-comparable-observations/README.md#t08a4-executed-exact-hr-stored-row-description--2026-11-12): fixed-primary 197 columns, all 201 exact HR levels, 15,189 raw means (2,271 one-row) and 1,074,462 same-level differences (187,894 with a one-row side); every unavailable cell retained. Equal-index pairing remains assumed, weights are equal stored rows and artifacts remain uncleaned. No anchor, A/B, qualified segment, cross-HR scalar, trend or physiology. Independent source/rational checks and full final rerun provide report-only evidence; exact data/scripts/results retained outside Git. T08a3 J04–J07 and all live/scientific/product gates unchanged. No application code authorized. Subsequent report-publication authorization is recorded in the T08a4 task.


## T08a5 approved 115 bpm stored-row descriptor — 2026-11-12

**Authorization/freeze snapshot; completed execution evidence follows below.**

Owner approved the described exact-at-115 / nearest-bracket linear interpolation variant, now reserved as [T08a5](t08a5-salford-anchor115.md) after checking the unused ID. Freeze the full rules and contributor endpoints before numerical execution. One output per fixed-primary column; direct/interpolated methods, counts, ranges and zero/quality flags stay explicit. Pairing and local linearity remain development assumptions, no A/B/trend/physiology or app implementation. L01–L06 define source/report arithmetic, complete denominators, independent checks, rerun/current-input/no-fallback/privacy evidence; pending execution. No new acquisition/contact or publication authorized; T08a3 and all live/scientific/product gates unchanged.


## T08a5 executed 115 bpm descriptor — 2026-11-12

[Executed results and verification](../../evidence/t08a2-comparable-observations/README.md#t08a5-executed-115-bpm-stored-row-descriptor--2026-11-12): all 197 fixed-primary outputs available, 188 exact and nine interpolated, 206 HR nodes and 3,226 actual source-row contributors. Seventeen columns have a one-row used point; 132 include Power-zero in a used point, all retained. Complete independent source/rational checks, full byte-identical rerun and current/no-fallback/interruption checks PASS. Exact versus interpolated evidence remains explicit; local linearity, index pairing, artifact and context limits unchanged. L01–L06 report/source evidence only, no original A/B, chronological/physiological trend, app change or Git publication. Detailed scalars/contributors/scripts remain outside Git; all live/scientific/product gates stay open.


## T08a6 approved single-row influence diagnostic — 2026-11-12

**Authorization/freeze snapshot; executed evidence follows below.**

Owner approved the described [T08a6](t08a6-salford-row-influence.md) diagnostic before ID reservation. Freeze all used T08a5 rows/nodes/weights/methods, omit each used row once without altering source, record exact W changes or required-node-empty refusal and complete per-column envelopes. M01–M06 require independent recomputation, full unavailable/partial denominators, deterministic rerun/current-input/no-fallback/interruption and privacy checks; execution pending. This is deterministic sensitivity, without physiological uncertainty or arbitrary adequacy thresholds. Recommend ending the present fixed-table numerical sensitivity route here, then deciding a separately scoped timing/context evidence route. No publication or further automatic experiment sequence authorized.


## T08a6 executed single-row influence — 2026-11-12

[Actual results and verification](../../evidence/t08a2-comparable-observations/README.md#t08a6-executed-single-row-influence-diagnostic--2026-11-12): all 3,226 deletions enumerated, 3,204 numeric and 22 required-node-empty refusals; all 197 envelopes accounted for (180 complete, one partial, 16 all-unavailable). Exact independent full-recomputation checks, byte-identical rerun and current-input/no-fallback/interruption checks PASS. Deterministic sensitivity is not calibrated uncertainty or physiological repeatability; no adequacy threshold, cleaning or ranking. M01–M06 source/report only. Recommend ending this fixed-table numerical sensitivity route and deciding a separately scoped timing/context evidence route. Detailed W/identities/scripts stay outside Git; earlier local T08a5 work and all scientific/live/product gates preserved. No publication authorized.


## T02c1 standstill diagnostic increment approved — 2026-11-12

Owner asked for progress that does not require another full workout, deferring long-run verification. [T02c1](t02c1-standstill-diagnostics.md) implements the existing source-backed live screen and bounded private timing/status report, not live Collector/database integration. Numeric readings display only; no new source, schema, physiology or SDK pin. P01–P06 cover missing/zero, report durability/privacy/bounds/errors, UI/source ownership, existing software/isolation checks and optional short device smoke. Parent T02c/T03e/T04/scientific gates remain open; no publication authorized. This is an observability slice, not a relaxation of full acquisition acceptance.


## T02c1 implemented and checked on standstill Karoo — 2026-11-12

[Actual scope and P01–P06 evidence](t02c1-standstill-diagnostics.md#execution-evidence--2026-11-12): existing source-owned real-value screen, receipt ages/missing reasons, callback counts and bounded private timing/status report implemented. Values display only; report excludes readings/profile/GPS. Zero/missing, reopen, storage/error/limit, queued-plus-in-flight overflow and Activity tests PASS; full debug 106/preview 130 PASS, unchanged core check cached 117 passes/one skip, build/lint and isolation PASS. Installed 0.3.0/code 3 on k24/API 32 and inspected connected/Idle UI plus private report after HOME/reopen. No full workout or native recording, no complete lifecycle/resource/scientific gate. Earlier local T02b/FIT evidence preserved, no publication performed.


## Proposed early T03e1 diagnostic increment — 2026-11-12

[T03e1](t03e1-live-diagnostic-persistence.md) is reserved, specification complete for decision, implementation unapproved. Limited proposed order: existing T03a–d/T02d/T02c1 → named T03e1 approval → integrated software tests → optional short standstill persistence evidence, while full T02c/T03e/parent T04 acceptance stays open. This exception changes the earlier blanket live-wiring dependency only for the diagnostic subdivision if approved; full acquisition gates are retained.

Scope: one service-owned source and coordinator over existing admission/lifecycle/InputRepository, received-versus-committed UI, serialized clock/timer/commit cuts, versioned policies/provenance, exact retry, bounded loss and conservative recovery. Files, concrete E01–E10 tests, caps, errors and Done evidence are in the task. No additional Collector/repository, analysis, physiology, private FIT processing or publication. No implementation or hardware test performed during planning.


T03e1 owner authorization (2026-11-12): owner explicitly approved the specified diagnostic-only implementation, policies and ordering exception. Earlier proposal status is historical. Full T02c/T03e/product reliability, live T04 and science remain open; no installation/publication/merge approval inferred.


T03e1 local implementation (2026-11-12, owner-approved): [actual structure and behavioral evidence](t03e1-live-diagnostic-persistence.md#implemented-structure-and-policy-refinements--2026-11-12) wires the sole real adapter through existing admission/lifecycle/InputRepository. One stamp/admission gate and one worker preserve timer cuts, exact retry, shared N01 bounds and loss-only drain; new epochs/clock origins, private versioned provenance and live-only 32 MiB main SQLite cap are explicit. Readout distinguishes received/current-run inputs from acknowledged commits and shows pending/loss/restart/storage states. Synthetic preview and analysis authority remain isolated. App is 0.4.0/code 4 locally, not installed. See task for final verification results; E10 and full T02c/N02/T03e/live T04/science remain open. No publication/merge.


T03e1 final software verification: debug 138 PASS, preview 162 PASS; core check PASS with unchanged cached 117 passes/one private-FIT skip. Builds/lint and release/preview isolation PASS (4 min 40 s). E01–E09 software evidence is recorded in the task; E10 NOT RUN. Local 0.4.0/code 4 APK is not installed; existing 0.3.0 device provenance is unchanged. Full T02c/N02/T03e/live T04 and science remain open. Changes stay local/uncommitted/unpublished.


T03e1 E10 short hardware check (2026-11-12): owner authorized installation/check after software completion. [Actual evidence](t03e1-live-diagnostic-persistence.md#e10-authorized-installation-and-short-karoo-check--2026-11-12) confirms matching-signer in-place 0.4.0/code 4 update on k24/API 32, exact installed prepared-APK hash and Connected/Idle zero-power/cadence plus unavailable HR. Screen received 213 / committed 203 / pending 10 matches SQLite revision 20 exactly; revision 21 retains all 213 unique observations, one epoch, zero sessions/analyses. HOME/reopen keeps the process and one source-start marker; timing/status report stays separate. E10 limited PASS supersedes earlier not-run/not-installed status. Raw SQLite/profile/report/screenshots/identifiers/timestamps stay private outside Git. No native ride or FIT replay; full T02c/N02/T03e/live T04 and science remain open. No publication/merge.


## T03e2 — Proposed verification-only standstill lifecycle

[Canonical task/procedure](t03e2-standstill-lifecycle.md), ID checked unused before reservation. Depends on merged T03e1 and E10, T03a–d, T02b/T02c1/T02d and T03p isolation contracts. Scope: one owner-operated stationary recording with manual pause/resume/local Idle; no new source/Collector/repository/code. L01–L06 cover R02/R03/R07/R09/R10/R11/R12 and limited V02/V03/V04/V05/V09/V11/V12/V15–V20. Owner native controls and explicit approval are required; execution NOT RUN.

Expected files: task/Canvas/decision log/backlog/work-package/T03e1 continuation documentation only; raw SQLite/report/screenshots/IDs/times stay outside Git. Done: after approval, actual control receipt sequence, one session/two segment indices, timer cases exercised, revision-filtered counts and read-only reopen checks with aggregate results and limits. Current delivery is the complete decision packet with link/traceability/whitespace checks. No parallel agent requirement, publication, full T02c/T03e, resources/faults or physiological acceptance.


T03e2 owner authorization (2026-11-12): owner approved the specified single stationary lifecycle verification and private canonical evidence inspection. Execution may proceed within its bounded procedure; earlier pending-approval status is historical. No publication, reinstall, additional ride or broader acceptance follows.


T03e2 executed trial (2026-11-12): [actual results](t03e2-standstill-lifecycle.md#executed-stationary-trial--2026-11-12) are PARTIAL because native auto-pause changed the controlled manual procedure. Actual persisted Idle → Recording → Paused → Recording → Paused → Paused → Idle produces one OBSERVED_START/ENDED_LOCALLY session with segment indices 0/1, same epoch and no ambiguity/interruption; analyses remain zero. Final screen 2768 received / 2760 committed / 8 pending / zero loss/rejected matches revision 253 exactly; revision 257 retains all earlier screen receipts. Integrity/foreign-key/session-effect checks and unchanged source/process markers PASS. Hardware silence boundary is not exercised. Raw evidence remains owner-only outside Git; installed pre-commit APK provenance unchanged. No code, setting change, second recording or publication. Optional manual follow-up with temporarily disabled/restored native auto-pause is specified in the task and requires separate approval; broader gates remain open.


T03e2 manual follow-up authorization (2026-11-12): owner explicitly approved one additional stationary trial with temporarily disabled/restored native auto-pause, as specified in the existing T03e2 amendment. Prior partial evidence remains unchanged. No reinstall, code, FIT access, broader matrix or publication authorized.


T03e2 approved manual follow-up (2026-11-12): [actual extended-trace evidence](t03e2-standstill-lifecycle.md#executed-manual-follow-up--2026-11-12) verifies one additional observed-start/local-end session with manual-only pause logs, segment indices 0/1/2 and no ambiguity/interruption. Owner reported a double click and requested repeating the pause in the same recording; all controls remain retained. Original exact two-segment procedure stays PARTIAL; actual manual persistence/ordering/paused exclusion PASS. Receipt-monotonic hold lower bounds ≥15 s / ≥10 s / ≥15 s verified. Current-epoch screen 30 received = 30 committed, pending/loss/rejected zero, matches revision 313; lifetime 3524 rows are not run-local counts. Integrity/reopen and unchanged source/epoch markers PASS, analyses zero; hardware silence boundary not exercised. Owner confirmed auto-pause restored. Same installed pre-commit APK; private manual-follow-up evidence separate, no code/new build/FIT access/publication. No third trial proposed; broader device/scientific gates unchanged.


## T02c2 — Short background/screen-off verification — 2026-11-12

PR #45 is confirmed merged at fb810cf (four successful checks, no review/comments); clean local main fast-forwarded safely. [T02c2](t02c2-short-background-check.md) is reserved after an unused-ID check: proposed verification-only visible 30 s → native page 45 s → verified display-off 45 s → visible 30 s during one stationary recording. Require a present already-paired channel, preferably HR; T03e2's missing-channel manual trial cannot establish present delivery in the background. B01–B06 cover phase evidence, committed receipt counts/gaps, same epoch/source/session, current-epoch counters and private SQLite consistency. Owner handles sensor wake, auto-pause disable/restore and native buttons. No new code, workout, source/repository or ordering exception. Named execution approval pending; no device action or publication in this planning step. USB/charging/ADB limits remain explicit; full T02c/T03e, residency/fault/resource/live T04 and science gates stay open.


## T02c2 deferred; diagnostic T04b3 proposal — 2026-11-12

Owner requested skipping T02c2 now and continuing development, with possible later return. Record the test as owner-deferred/NOT RUN; earlier device evidence retains its limited scope. [T04b3](t04b3-live-diagnostic-curve.md) proposes the next complete software slice: explicit latest-ended-session calculation using existing committed replay/engine/AnalysisRepository and a durable diagnostic nine-duration readout. Exact live policy support, distinct real provenance/cohort and codec compatibility, canonical-priority cooperative work on the same worker, cancellation/cap/newest-result safeguards and C01–C08 acceptance are specified. No second Collector/repository system, private FIT, physiology or smoothing. The limited diagnostic T04 ordering amendment and implementation await named approval; full T02c/T03e/product live T04, hidden/screen-off/resources and science are not accepted. No device action, application code or publication in this planning step.


T04b3 owner authorization (2026-11-12): owner approved the specified software implementation, including its limited diagnostic ordering amendment. T02c2 remains deferred, broader hardware/scientific gates open. No installation/private-data evaluation/publication inferred.


T04b3 local software completion (2026-11-12, owner-approved): [implementation/evidence](t04b3-live-diagnostic-curve.md#final-verification-and-disposition--2026-11-12) delivers explicit latest-ended diagnostic curve calculation/save/readout through the existing worker and repositories. Shared stepped replay, exact live policy and real-origin codec/slot, canonical priority/cancellation and cap/newest-result safeguards are verified with synthetic inputs only. Final full debug 170 PASS; full preview 192 PASS before the final cancellation cleanup, followed by 54 focused preview passes; core check/build 120 passes/one private-FIT skip cached, build/lint/format/isolation PASS. Version 0.5.0/code 5 built locally, not installed. No private data/FIT/device analysis or publication. T02c2 remains deferred; full acquisition/resources/product live T04 and physiology remain open.


T04b3 reviewed installation/publication (2026-11-12): [actual install evidence](t04b3-live-diagnostic-curve.md#reviewed-050-installation--2026-11-12) supersedes earlier not-installed status. Pre-PR review fixed analysis selection before latest control acknowledgement; 172 debug/196 full preview and relevant checks PASS. Same-signer 0.5.0/code 5 installed with -r, exact reviewed APK hash read back, Connected/Idle/Ready and version footer observed. Binary provenance remains fb810cf plus reviewed pre-commit changes. No real Calculate/native recording/FIT analysis or controlled hidden/screen-off claim. Private artifacts outside Git; owner authorized PR publication, not merge.
