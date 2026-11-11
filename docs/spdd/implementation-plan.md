# Dependency-ordered implementation backlog

Status: T01a and owner-resumed T01b complete; [T01a evidence](tasks/t01a-canonical-input.md), [T01b evidence](tasks/t01b-synthetic-replay.md). [T01c evidence](tasks/t01c-fit-replay.md) closes the developer FIT work package. The owner approved [T02a](tasks/t02a-diagnostic-host.md). Real Karoo mapping is planned for T02b. M0 ends with this review packet; approval must name tasks.

| Task | Goal | Dependencies | Requirement / tests | Output / risk / Done |
|---|---|---|---|---|
| T01 | Canonical input, lifecycle and deterministic replay contract | Architecture approval | R01/R02/R11; V01/V02/V03 | `core` contracts, JVM fixtures, decoder/license decision; risk time semantics; done when two source mappings preserve units/provenance and fixture oracles pass |
| T02 | Verify Karoo lifecycle/timing and toolchain | T01; named permission for diagnostic code/device run | R02/R09/R11; V02/V11/V13 | Android diagnostic slice and evidence report; risk unreliable binding; done when startup/4-hour/kill scenarios recorded and acquisition design accepted or rejected |
| T03 | Durable, bounded collection | Accepted ADR 0004 permits T03a–d after simulation contracts, with T03e/live acceptance still gated by T02. | R03/R07/R10/R12; V04/V05/V09/V12/V14/V16/V17 | Android repositories/checkpoints plus core lifecycle, migrations; risk ambiguous identity/disk loss; done when interruption, storage-full, hard-cap overload, generation/supersession and idempotency tests pass |
| T04 | End-to-end observed power curve | T03 | R04; V06/V13 | Core integration + app readout; risk false rolling windows; done when device ride reaches persisted curve and independent irregular-time oracle agrees |
| T05 | Reproducible model experiment protocol | T01, owner-provided FIT or approved licensed fixtures | R06/R07; V08/V09 | Versioned datasets, profiles, candidate audit and benchmark report; risk no ground truth; done when evaluation protocol/limitations and go/no-go criteria reviewed |
| T06 | Select then implement FTP model | T04, T05 and method approval | R05; V07/V09 | Core FTP strategy/config + evidence; risk nonmaximal effort; done when numerical and eligibility tests pass and validity limits reviewed; CP cannot satisfy FTP automatically |
| T07 | Select then implement VO₂max model | T05 and method approval; T03 for device integration | R06; V08/V09 | Independent core model + abstention; risk unidentified physiology; done when equations, coefficients, sensitivity, applicability and external validation evidence reviewed |
| T08 | History, smoothing and basic chart | T03/T04; T06/T07 for numeric estimator acceptance | R07/R08/R12; V09/V10/V14/V15/V16 | Persisted runs/contributors, app UI; risk hidden bias/cohort mixing; done when raw vs trend, partial-session exclusion, staleness, unavailable supersession and deletion/recalculation work |
| T09 | Release hardening and publication readiness | T04/T06/T07/T08; license decision | All applicable scenarios | Signed artifact/release evidence after authorization; risk compatibility/privacy; done when hardware, migration, offline and science gates pass, docs current and publication explicitly authorized |

First vertical slice is T01 → T02 → T03 → T04: **Karoo → Collector → Persistence → Power Curve**. T02 first tests the acquisition hypothesis; it does not silently become a separate production feature. Detailed scope: [first-slice task](tasks/first-vertical-slice.md).

For smaller agent contexts, see the proposed [work packages and handoff template](tasks/agent-work-packages.md). These subdivide existing tasks without authorizing implementation or bypassing dependencies.

T05 can use developer FIT replay before any product import UI. T06 and T07 are logically independent once shared evidence is ready; no agent parallelization is required. Failure of a scientific gate leads to reviewed scope/method change, not an arbitrary formula.

Review checkpoints: M0 architecture; T02 device feasibility; T04 first slice; each model selection before code; full-release evidence before publication. No estimated implementation dates until hardware/toolchain and model gates are understood.


## Accepted simulation-first architecture — 2026-11-11

Architecture ACCEPTED; T01d owner-approved. [ADR 0004](../architecture/adr/0004-simulation-first-testing.md) and the [change task](tasks/simulation-first-development.md) define dependencies, bounded files, acceptance and risks. Only T01d implementation is authorized; other packages retain named approval gates.

Proposed order is T01d (source port, virtual clock and generators) → T01e (lawful real FIT replay) and T02d (SDK fake compatibility and mapping tests) → applicable T03a–d software packages. Existing T03a → T03b/T03c → T03d dependencies still apply. Hardware T02c evidence can proceed alongside that approved software work. T03e remains gated by the passed T02 acquisition decision. T04 still follows T03. T05–T07 scientific gates remain unchanged.

T02b has partial host/state evidence and pending real-sensor checks. T02d makes its mapping and cleanup regressions repeatable. T02c concentrates the hardware matrix. SDK fakes, emulator and external replay are evidence layers, each with limits in V18–V22. Individual package completion cannot close parent V11/V13 or physiological validity.
