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
