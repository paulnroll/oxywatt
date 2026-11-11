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

T02d adds the actual app → core dependency and KarooRideDataSource. Fake 0.1.1 and Robolectric 4.17 are app test dependencies only; an explicit opt-in selects the public SDK tag build for local/manual CI checks. Core JVM tests remain the default build. No shared test-fixture publication is needed for the independent adapter event table in this package. Java time core-library desugaring supports the existing minSdk 23; the package is tested under Robolectric API 32/34, not certified on older hardware. See [T02d evidence](../spdd/tasks/t02d-karoo-adapter-tests.md).

T03c resolves input storage to platform SQLiteOpenHelper in `app` without a new dependency or processor; Room remains optional for a future reviewed change. Versioned JSON values retain canonical source/lifecycle contracts; SQL unique keys, foreign keys and transactions enforce input ownership and atomic checkpoints. [T03c contract](../spdd/tasks/t03c-transactional-inputs.md) defines bounded replay and actual fault-test evidence. Core remains independent of Android/persistence types.

## T03d result storage

[AnalysisRepository](../spdd/tasks/t03d-analysis-persistence.md) adds idempotent, globally increasing durable generations, newest-requested slots and immutable terminal results with versioned provenance. Schema v2 explicitly migrates real v1 inputs without changing their rows. New requests and terminal writes invalidate affected aggregate memberships transactionally; pending/failed/unavailable never expose prior available results. Membership stores run/generation references only, validates structural session eligibility and cohort consistency, and does not compute trends. Historical result lookup remains available; deletion/rebuild and scientific acceptance remain later work. Both repositories share platform SQLiteOpenHelper; core remains Android-free.
