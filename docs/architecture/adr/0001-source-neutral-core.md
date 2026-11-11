# ADR 0001 — Independent Collector and canonical inputs

Date: 2026-11-10. Status: owner-decided direction; contract details proposed.

Context: both live Karoo callbacks and owner FIT files must drive the same analytics. Sources differ in timing guarantees and lifecycle evidence.

Decision: adapters translate source values into SourceObservation. A source-neutral Collector applies common lifecycle, validation and normalization policy. Engine inputs are the same canonical RideSample/segment model; provenance distinguishes receipt time from decoded file time. Collector is not embedded in a Karoo field or FIT parser.

Alternative rejected: SDK DTOs throughout the engine, or separate FIT-only calculation code. They create inconsistent semantics and require hardware for basic numerical tests.

Consequences: pure JVM replay; explicit mapping/versioning; decoding and time reconciliation remain adapter responsibilities. Matching values alone cannot establish identical measurement timing. Verified mapping is T01 and hardware timing is T02. Requirements R01–R04; tests V01–V06.

## Proposed simulation-first extension — 2026-11-11

[ADR 0004](0004-simulation-first-testing.md) proposes a RideDataSource technical port using SourceObservation values built from canonical v1 types, with Karoo and simulated implementations. The Collector assigns session IDs when creating envelopes. FIT decoding feeds simulated replay in developer support. Acquisition context remains Collector-owned. Review is pending before implementing the port.
