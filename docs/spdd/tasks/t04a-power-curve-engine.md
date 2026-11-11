# T04a — Simulation-backed observed power-duration engine

Status: specification prepared on 2026-11-11 following the owner's request to continue without a bicycle and postpone live tests until the end. **Implementation requires named T04a approval.** This proposed dependency amendment permits the pure numerical engine before T03e; T04b/device integration and parent T04 acceptance retain their live gates.

## Goal and scope

Deliver a source-neutral Kotlin/JVM engine for observed time-weighted power maxima at 5/30/60/180/300/600/1200/1800/3600 seconds. R04/R07/R11; V06/V09 and software-only V13/V18/V19/V22. Consume explicitly covered power intervals, not aligned samples or SDK objects. Missing duration is unavailable; zero is a valid result. No FTP/CP/VO2max estimate, history, Android UI, database schema, live source or release change.

Dependencies: T01 canonical timing/simulator contracts, T03a session/coverage semantics, accepted [power calculation](../../algorithms/power-curve.md), [ADR 0002](../../architecture/adr/0002-independent-estimators.md). T03b–d are available for later consumers but are not runtime dependencies of a numerical core. T03e and full T02c remain deferred; a passing simulator cannot accept their hardware claims.

## Input, time and coverage contract

The minimal public boundary accepts immutable, confirmed piecewise-constant intervals with session ID, acquisition epoch, recording-segment identity, start/end elapsed nanoseconds and finite nonnegative watts. Use half-open [start,end) intervals and positive durations. Reject overlaps, time regression, invalid values or mixed identity inside a contiguous segment. Do not use UTC or FILE_RECORD audit timestamps for receipt-domain durations. Never infer that two different UUIDs are the same ride.

Intervals must represent coverage already confirmed by a caller. **T04a does not convert the latest reading into unlimited future power.** A later adapter must close a held value at the earliest next power reading, missing event, configured expiry, pause, interruption or known end. Until that adapter exists, only independently specified synthetic interval tables feed this engine. Do not claim an end-to-end canonical-input or persisted-ride curve from this package.

Adjacent intervals with the same session/epoch/segment are continuous; a positive time gap breaks all windows. An explicit break, even at an adjacent timestamp, also prevents joining windows. Start of a new recording segment/epoch/session resets rolling coverage. Retain already observed per-duration winners for the current session across ordinary pauses/gaps, but never bridge them. A new session gets a new curve. Partial/interrupted-session results remain partial observations, not eligible historical trend contributors. HR/cadence absence does not invalidate explicit power coverage.

A window ending exactly at a break can qualify using fully covered time before the break. No right-hand value is extrapolated beyond the last confirmed interval. Equal adjacent timestamps contribute no duplicated duration. Numerical integration uses fractional interval boundaries at nanosecond resolution, with elapsed differences calculated safely before conversion to seconds.

## Output and numerical policy

Each duration returns either unavailable with a bounded reason (insufficient contiguous coverage or resource limit), or the best observed mean watts and exact [start,end), session/epoch/segment provenance. Include preprocessing/configuration versions and coverage origin supplied by the caller. Deterministic ties select the earliest start within the same session; compare input-order-independent equal candidates without a user-facing confidence claim. Existing winners remain valid observations after a later coverage break; output must distinguish a resource-limited scan from a complete scan.

Use exact piecewise-constant integration rather than sampling a regular grid. A complete maximum search includes window endpoints at interval boundaries and those boundaries shifted by the target duration; between such endpoints the rolling energy is linear. Clip to the actual covered domain. Candidate enumeration must include expiration-side boundaries, not merely input arrival times. Tests must derive expectations independently rather than snapshot production output. Numerical test tolerance is max(1e-8 W absolute, 1e-12 times the expected magnitude); timestamps and selected windows remain exact integers. Reject nonfinite arithmetic outcomes explicitly without corrupting trusted state.

Retained state is bounded by the largest requested duration and a versioned hard interval cap. Proposed `power-intervals-v1`: retain at most 16384 interval fragments (including any partial boundary fragment), plus at most one winner per duration; caller can select a smaller positive cap in tests. Evict history older than needed for 3600-second windows. Coalesce exactly adjacent equal-power intervals only within the same uninterrupted identity. If the cap would be exceeded, preserve established winners, report resource-limited coverage, break the affected rolling stream and release its interval buffer before admitting a new independent span. Never silently discard a middle interval and then bridge it. This is an engineering limit, not a physiological threshold or measured device memory budget. Test repeated saturation and document operation counts/retained-state plateau; no four-hour battery/RSS claim.

## Expected files and exclusions

- `core/src/main/kotlin/com/oxywatt/core/power/`: minimal interval, policy, result and engine types; no generic analytics framework, external numerical library or module.
- `core/src/test/kotlin/com/oxywatt/core/power/`: behavioral TDD with independent integral/event tables, deterministic randomized comparisons and retained-state stress checks.
- Existing simulator fixtures may provide interval test inputs without moving them into core main. No private FIT is needed.
- This task, power-curve specification, validation strategy, Canvas, decision log and package/backlog dependencies with actual results.

Canonical-reading-to-interval preprocessing, committed repository replay/hash integration, generation persistence and preview curve readout belong to a separately scoped T04b follow-up. That follow-up must freeze hold/expiry/equal-time and terminal coverage semantics against actual T03 effects before wiring; T04a must not invent a second Collector.

## Behavioral acceptance and Done evidence

1. Constant 200 W for 300 s: 200 W for each configured duration up to 300 s, unavailable beyond. Fully covered zero produces 0 W. Empty and shorter-than-five-second coverage produce unavailable.
2. One-second intervals 0/100/200/300/400 W: 5 s mean exactly 200 W. Independently enumerated fractional/irregular intervals exercise clipped boundaries, an interior optimum and an optimum at a boundary shifted by duration.
3. Missing/gap, explicit adjacent break, pause/new segment, epoch and session changes cannot create a crossing window. HR-only absence changes nothing about a supplied power interval. Winners before a break remain attributed to their original span.
4. Reject invalid/overlapping/regressing inputs without corrupting previously accepted winners or advancing trusted state. Define and test recovery after rejection. Test exact end boundaries, equal-value adjacent intervals, deterministic ties and large monotonic offsets.
5. Equivalent safe synthetic interval schedules at 1x/10x/unpaced delivery yield equal records; wall-clock sleeps are unnecessary. Original receipt-domain duration is invariant under delivery speed.
6. Compare deterministic irregular traces against a separate slow integral oracle. Exercise all required durations and repeated resource saturation; verify maximum retained intervals and stable bounded result state over at least four simulated hours. Resource-limit reasons must remain explicit.
7. Use behavior-driven red tests before implementation, then run `./gradlew spotlessApply :core:check :core:build --console=plain`. Verify core production JAR has no Android, simulator or test tooling. Check relative links, R/T/V traceability and `git diff --check`. Record exact tests/results and any acceptance gap; no hardware acceptance is implied.

Risks: falsely extending last samples, sample-count averages, missing expiration candidates, merging discontinuities, unbounded high-rate retention and presenting partial coverage as a fully searched curve. Done requires reviewed boundary/policy, the above passing software evidence and honest pending T04b/T02 gates. This document currently contains no implementation or executed numerical evidence.

Specification self-review: checked interval identity/time/coverage, zero versus unavailable, resource-limit honesty, numerical versus live/scientific gates, and R/T/V/dependency consistency. All 167 relative documentation links resolve and `git diff --check` passes. Documentation only; no application code, numerical tests or device actions were performed for this proposal.
