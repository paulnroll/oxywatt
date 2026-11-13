# ADR 0003 — Verify whole-ride collection before committing the live design

Date: 2026-11-10. Status: owner-decided gate; host lifecycle implementation remains conditional.

Context: a visible field's subscription does not guarantee automatic multi-hour acquisition. Process death prevents collection. Public recording state does not identify a durable native ride.

Decision: acquisition has a lifecycle independent of UI. M0 produces documents and source-verified findings only. After approval, T02 must probe ride-scoped host binding, startup, hidden/no field, screen-off, disconnect, process death, force-stop and resource budgets before T03 assumes reliable acquisition.

Alternative rejected: create acquisition inside `DataTypeImpl.startView` or claim reconnect equals replay. Candidate: host `startFit` binding; fallback: compliant user-armed foreground service after review. Neither is guaranteed until device evidence exists.

If no supported lifecycle meets R09, stop and propose an explicit mode/scope change. Never invent a keep-alive, emit fake FIT data, or silently accept partial rides as full. Requirements R02/R03/R09/R11; tests V02–V05/V11/V13.

## Proposed dependency amendment — 2026-11-11

[ADR 0004](0004-simulation-first-testing.md) allows T03a–T03d software development after accepted simulated source and repository contracts, while T03e production wiring and live acceptance continue to require the T02 acquisition decision. The owner accepted this architecture on 2026-11-11; each implementation slice still needs named approval. Hardware still determines startup, sustained residency, radios, death/force-stop behavior and resource budgets. Failed feasibility triggers architecture review before live deployment.


## Proposed diagnostic-only ordering exception — 2026-11-12

[T03e1](../../spdd/tasks/t03e1-live-diagnostic-persistence.md) proposes bounded real-source diagnostic canonical persistence before the complete T02c matrix, using completed T03 software components and explicit received-versus-committed state. **Not accepted or implementation-approved.** Named approval would permit this subdivision's software work and leave full T03e/product acquisition, parent live T04, residency/death/resource and science acceptance behind existing gates. No new keep-alive, automatic session merge or full-ride guarantee. Canvas/decision log and task define the limited exception and E01–E10 evidence, rather than silently weakening this ADR.


T03e1 owner authorization (2026-11-12): owner explicitly approved the specified diagnostic-only implementation, policies and ordering exception. Earlier proposal status is historical. Full T02c/T03e/product reliability, live T04 and science remain open; no installation/publication/merge approval inferred.


## T02c2 deferred; diagnostic T04b3 proposal — 2026-11-12

Owner requested skipping T02c2 now and continuing development, with possible later return. Record the test as owner-deferred/NOT RUN; earlier device evidence retains its limited scope. [T04b3](../../spdd/tasks/t04b3-live-diagnostic-curve.md) proposes the next complete software slice: explicit latest-ended-session calculation using existing committed replay/engine/AnalysisRepository and a durable diagnostic nine-duration readout. Exact live policy support, distinct real provenance/cohort and codec compatibility, canonical-priority cooperative work on the same worker, cancellation/cap/newest-result safeguards and C01–C08 acceptance are specified. No second Collector/repository system, private FIT, physiology or smoothing. The limited diagnostic T04 ordering amendment and implementation await named approval; full T02c/T03e/product live T04, hidden/screen-off/resources and science are not accepted. No device action, application code or publication in this planning step.


T04b3 owner authorization (2026-11-12): owner approved the specified software implementation, including its limited diagnostic ordering amendment. T02c2 remains deferred, broader hardware/scientific gates open. No installation/private-data evaluation/publication inferred.


T04b3 local software completion (2026-11-12, owner-approved): [implementation/evidence](../../spdd/tasks/t04b3-live-diagnostic-curve.md#final-verification-and-disposition--2026-11-12) delivers explicit latest-ended diagnostic curve calculation/save/readout through the existing worker and repositories. Shared stepped replay, exact live policy and real-origin codec/slot, canonical priority/cancellation and cap/newest-result safeguards are verified with synthetic inputs only. Final full debug 170 PASS; full preview 192 PASS before the final cancellation cleanup, followed by 54 focused preview passes; core check/build 120 passes/one private-FIT skip cached, build/lint/format/isolation PASS. Version 0.5.0/code 5 built locally, not installed. No private data/FIT/device analysis or publication. T02c2 remains deferred; full acquisition/resources/product live T04 and physiology remain open.


## T04b4 proposed local session selection — 2026-11-13

Owner requested preparation of [T04b4](../../spdd/tasks/t04b4-local-session-selection.md), reserved after an unused-ID search. Implementation and its explicit older-target diagnostic ordering amendment await the concrete scope decision. Show up to 20 recently persisted local session snapshots (truncation explicit), no initial selection, UUID-based selection, truthful observed receipt span and unknown coverage until a saved curve exists. Selected-session Calculate and saved readout reuse the existing source-owned worker, bounded requests/replay, exact current-epoch control acknowledgement, sticky new-recording cancellation, real slot/provenance and capped repositories. Read-only selection/reopen creates no generation/source; new runtime starts unselected. S01–S08 specify synthetic-only software acceptance; no task tests/device/private-data work performed in this planning step. No general history/aggregate/chart, FIT import/reuse, new measurement path or physiology. No publication/installation/real analysis approval inferred; T02c2 remains deferred and wider acquisition/resource/scientific gates stay open. Existing T04b3/C15 local evidence is preserved separately.


T04b4 owner authorization — 2026-11-13: owner approved the concrete software package and its explicit older-target diagnostic ordering amendment. Earlier proposal status is historical. Existing source ownership, current-epoch control acknowledgement, cancellation, bounded list/analysis and provenance/storage safeguards remain required. Installation, private data/device calculation, publication and broader hardware/scientific acceptance remain separate.


T04b4 software complete — 2026-11-13: [S01–S08 evidence](../../spdd/tasks/t04b4-local-session-selection.md#software-verification-and-disposition--2026-11-13) records explicit UUID selection from ≤20 recent persisted local snapshots, older-target calculation, saved readout/recreation without another generation/source, unknown-coverage labels and current-epoch control/cancellation/newest/cap safeguards. Full debug 196 / preview 220 PASS, unchanged cached core 120 passes/one private-FIT skip; build/lint/format/release-preview isolation PASS. Strengthened list/writer interleaving fixture afterward passes full debug 196 / focused preview 2 without application-source changes. Local 0.6.0/code 6 is built, not installed; public USB metadata confirms matching signer with installed 0.5.0 without launch/private data reads. Earlier T04b3/C15 evidence is preserved. Installation, real selected-session Calculate, recording and publication remain separate; T02c2 stays deferred and full acquisition/resource/product/science gates open.
