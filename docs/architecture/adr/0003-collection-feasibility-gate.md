# ADR 0003 — Verify whole-ride collection before committing the live design

Date: 2026-11-10. Status: owner-decided gate; host lifecycle implementation remains conditional.

Context: a visible field's subscription does not guarantee automatic multi-hour acquisition. Process death prevents collection. Public recording state does not identify a durable native ride.

Decision: acquisition has a lifecycle independent of UI. M0 produces documents and source-verified findings only. After approval, T02 must probe ride-scoped host binding, startup, hidden/no field, screen-off, disconnect, process death, force-stop and resource budgets before T03 assumes reliable acquisition.

Alternative rejected: create acquisition inside `DataTypeImpl.startView` or claim reconnect equals replay. Candidate: host `startFit` binding; fallback: compliant user-armed foreground service after review. Neither is guaranteed until device evidence exists.

If no supported lifecycle meets R09, stop and propose an explicit mode/scope change. Never invent a keep-alive, emit fake FIT data, or silently accept partial rides as full. Requirements R02/R03/R09/R11; tests V02–V05/V11/V13.

## Proposed dependency amendment — 2026-11-11

[ADR 0004](0004-simulation-first-testing.md) allows T03a–T03d software development after accepted simulated source and repository contracts, while T03e production wiring and live acceptance continue to require the T02 acquisition decision. The owner accepted this architecture on 2026-11-11; each implementation slice still needs named approval. Hardware still determines startup, sustained residency, radios, death/force-stop behavior and resource budgets. Failed feasibility triggers architecture review before live deployment.
