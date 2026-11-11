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
| T04a / T04 | Time-weighted power-duration engine; own `core/power` files | T03 complete per existing T04 gate; accepted persisted replay contract; power-curve specification | R04/R11, V06/V13: required durations, gaps, irregular intervals and independent integral oracle. Risk: inflated maxima; numerical and bounded-state evidence |
| T04b / T04 | Persist/project observed curve and minimal app readout; own distinct projection/UI files, reuse result contract | T04a output contract and result acceptance; module/UI state requirements | R04, V06/V13: real recording reaches persisted readout, missing/partial states honest. Risk: wiring changes semantics; device evidence and actual resource measurements complete the first slice |

T01b and T01c may run concurrently after T01a. T03b and T03c may run concurrently after T03a freezes their shared contracts; T03d follows the schema agreement. Keep the rest sequential initially. T04 work remains behind T03 as specified in the existing backlog. The coordinator reviews changes against requirements and runs combined checks before accepting each parent task.

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
