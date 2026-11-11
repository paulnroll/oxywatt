# T04b2 software evidence — 2026-11-11

Owner approved T04b2 after specification PR #25 merged. [Task and acceptance](../../spdd/tasks/t04b2-long-variable-scan.md). No private FIT, device action, live acquisition or physiological model. These are software counters and Robolectric SQLite results, not Karoo CPU/RSS/battery measurements.

## Numerical implementation and cost

The existing engine now uses a private persistent AVL range index. Each node stores an interval and a normalized mean over its contiguous subtree. Exact integer query boundaries select disjoint subtrees and clipped leaves; compensated local normalized terms avoid floating-point energy-prefix subtraction and W×ns overflow. Candidate order, expiration candidates, tolerance, coverage, coalescing, identity and tie rules remain unchanged. A failed append publishes neither staged index nor proposed winners. Work exhaustion preserves winners from completed appends, latches incomplete scan, releases rolling state and stops numerical admission; metadata replay still finishes. Cancellation propagates to the existing FAILED newest generation.

`power-range-means-v2` and `power-replay-work-v2:16384:100000000` identify the new calculation. The legacy logical slot is retained for supersession. Existing scalar-v1/curve-v2 decoding and atomic generation storage need no schema change. For v2, the old serialized `reserved` field holds charged work and `fragments` counts disjoint query contributions, not v1 leaf visits; policy/provenance distinguish the meanings. No historical counter is rewritten.

A unit is one range-index node visit, normalized contribution (including staged-node construction), candidate insertion/evaluation, or node construction/removal. Candidate TreeSet insertion is one candidate operation with O(log C) standard-library comparisons; range-index traversal is individually charged. Constant-size nine-winner copying and metadata validation are outside numerical units. Each unbounded numerical loop charges internally. Work checks precede operations; cancellation is checked every 1024 units and at replay boundaries. Default 100M units is an engineering ceiling, not measured time.

Representative direct-core results from LongPowerCurveTest (JDK Temurin 25+36, macOS arm64, JVM target/release 17):

| Workload | Visits | Contributions | Candidate operations | Mutations | Total units | Windows / query contributions |
|---|---:|---:|---:|---:|---:|---:|
| 4 h alternating 1 Hz | 6839963 | 2698095 | 730986 | 308252 | 10577296 | 243659 / 1868736 |
| 4 h irregular ns | 7363919 | 2827488 | 730980 | 308252 | 11230639 | see test output |
| 8 h alternating 1 Hz | 14255882 | 5657309 | 1508586 | 657662 | 22079439 | 502859 / 3887720 |
| 16000 × 1 ms then 3600 s | 4849590 | 3166016 | 332048 | 404473 | 8752127 | 155018 / 2072599 |

The 1 Hz scan enumerates exactly the baseline 243659 windows, with 1868736 disjoint query terms instead of 187673925 old overlapping leaf integrations. These contribution types have different meanings. Eight hours use 2.087× the four-hour units (acceptance ≤2.5×). Descriptive local engine times in one final core run were 112 ms, 184 ms and 117 ms for four/eight-hour and dense workloads; not timing assertions or device evidence.

Retained/staged index size peaks at 3601 for 1 Hz (3600 after eviction), 16001 for dense-then-long (one incoming interval before eviction). The conservative live-node bound is 7230 and 32032 respectively, both below 4C+2=65538. One node stores both an interval and its subtree summary. Candidate peak is 16002, below C+3=16387. The bound includes old and staged roots plus transient AVL paths/rotations; no tree snapshot escapes. Tiny cap-two repeated saturation checks ≤2 interval nodes and ≤10 live-node bound. This is structural retained-state evidence, not a heap/RSS measurement.

## Independent oracles and complete persisted flow

Ten LongPowerCurveTest cases cover B2-01–06: hand-derived alternating maxima, an independent exact integer-cell sliding-energy oracle for seeded stages/spikes, test-only exact BigDecimal energy prefixes with 80-digit quotient comparison over an independently constructed boundary union, four-hour irregular traces, dense/long stress, large origins and finite powers including 1e308, near-tolerance ties, short low windows following huge discarded coverage, explicit breaks/segments/epochs, rejection, memory/work exhaustion, cancellation and doubled-length cost. Existing power acceptance tests retain fractional/expiry, zero, coalescing, ordering and immutable snapshot regressions.

The first compiling T04b2 test failed on the old wrapper's incomplete four-hour scan, then passed with the bounded range index. Other scenarios were added against the integrated implementation; not every case is claimed as individually observed red. Self-review found disconnected peak getters after a refactor; a regression requiring peaks to cover actual retained state failed, then passed after connecting staging high-water measurements. Tiny-cap assertions also guard against vacuous zero measurements.

LongPowerReplayTest uses native SQLite, actual CollectorLifecycle/BoundedAdmission/InputRepository and AnalysisRepository, then closes and reopens the database through PreviewPipeline and renders curveText. Each fixture runs with virtual 1x/10x/unpaced delivery, fixed acquisition stamps/session/epoch and no sleeps. It compares the entire stored curve (all nine results, exact boundaries, hash, counters, coverage/session metadata), independent expected intervals and exact oracle records; processed effect and interval counts must match the full fixture. Aggregate insertion is refused. No new product control or second storage is involved.

Fixtures: four-hour alternating 1 Hz; four-hour irregular ns; 16000 dense 1 ms readings followed by actual silence/expiry and ordinary readings until four hours; four-hour pause/missing/silent-gap mixture; four-hour process-recovery interruption; four-hour persisted admission-loss interruption. The dense canonical fixture correctly refuses to treat a silent hour as a confirmed constant-power interval. Existing short loss/recovery tests exercise loss before a held expiry and an old-epoch horizon, while long terminal cases verify complete scan and durable partial-session readout. Existing scalar/curve compatibility, rollback/retry, newest-requested/no-fallback, late completion, revision-bound replay, unsupported policy/cancellation and the 17 s preview [10,15) = 200 W regressions remain required.

The complete six-fixture matrix passed all 18 speed combinations. Each speed produced equal stored curves/hashes/work counts after native SQLite reopen. For recovery/loss, 14403 relevant effects and 14400 intervals were checked, with INTERRUPTED preserved. The final command is recorded in the task: core 100 passes plus one private-FIT skip, debug 92 passes, preview 104 passes, zero failures/errors; preview/release builds, lint and both isolation checks passed. Live T02c, T03e and parent T04 remain OPEN.
