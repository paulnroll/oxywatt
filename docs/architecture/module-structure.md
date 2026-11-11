# Module structure and persistence

Owner-approved architecture: two physical modules. T01a created `:core`. T02a adds the diagnostic `:app`, enabled with `-PkarooDiagnostic=true`. See [toolchain and evidence](../spdd/tasks/t02a-diagnostic-host.md).

```text
:core       Kotlin/JVM: collection contract, lifecycle, analytics, estimation, history policies, ports
:app        Android: Karoo adapter, Room repositories, orchestration, Compose app UI, extension fields
```

Current `:core` contains only `com.oxywatt.core.collection` canonical inputs and JVM tests, using Gradle 9.1.0/Kotlin 2.3.20 on JDK 25 with target/release 17. This does not select the Android toolchain.

Package boundaries inside core follow the logical contexts. Add a development-only FIT replay module only when an actual parser and licensing/dependency choice require it; otherwise keep decoder helpers in test support. A future FIT adapter maps to the same Collector input. No production dependency on a FIT library is required for the live slice.

Dependency direction: app → core; core tests → deterministic fixtures. Android adapters implement repository/clock/input contracts. Prefer constructor injection; no DI framework is required initially. Coroutines/Flow may orchestrate streams; numerical functions and model inputs remain ordinary immutable Kotlin values.

Room is recommended for the Android adapter, conditional on compatible versions. Proposed indexed tables: sessions, segments, observation chunks or bounded sample batches, profile snapshots, analysis runs, power records, estimate snapshots, trend contributors. Batch/chunk representation and compression are to be benchmarked in T03; avoid requiring one database transaction per sensor callback.

Persist source facts needed for replay plus normalization/config versions. Idempotent chunk commits and checkpoint updates share a transaction. Derived records can be replaced only by a new versioned run; preserve history/provenance. Persist generation allocation, the newest-requested pointer per session/metric/cohort and invalidation of current aggregate membership in one transaction. Commit terminal results with aggregate invalidation; stale completion must not overwrite a newer pointer. Unavailable supersedes prior availability, while pending/failed newest requests contribute nothing. Store contributor run/generation IDs for reproducible historical projections. Foreign keys and unique keys enforce local ownership. Deleting a contributing ride invalidates/rebuilds affected aggregates and explicitly removes replay availability.

Do not choose toolchain/dependency versions solely from the newest Android samples. Candidate SDK 1.1.9 is source-verified; JDK/Gradle/Kotlin/Compose/Room matrix remains to be resolved against actual Karoo OS in T02.

## Proposed simulation-first boundary

[ADR 0004](adr/0004-simulation-first-testing.md) proposes a core RideDataSource port producing observations for Collector-owned envelope/session assignment, test-support generators/FIT replay and an app Karoo implementation. Existing core/app modules remain sufficient. SDK test fakes belong only in app tests. An optional emulator appstore harness is isolated from the physical device and release artifacts. T01d must select the smallest test-source layout that allows the core scenarios to feed app tests without copying scenario definitions. Promote reusable test fixtures through Gradle test fixtures only when T02d consumes them.
