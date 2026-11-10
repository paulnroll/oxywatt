# Module structure and persistence

Proposed minimal physical modules after approval:

```text
:core       Kotlin/JVM: collection contract, lifecycle, analytics, estimation, history policies, ports
:app        Android: Karoo adapter, Room repositories, orchestration, Compose app UI, extension fields
```

Package boundaries inside core follow the logical contexts. Add a development-only FIT replay module only when an actual parser and licensing/dependency choice require it; otherwise keep decoder helpers in test support. A future FIT adapter maps to the same Collector input. No production dependency on a FIT library is required for the live slice.

Dependency direction: app → core; core tests → deterministic fixtures. Android adapters implement repository/clock/input contracts. Prefer constructor injection; no DI framework is required initially. Coroutines/Flow may orchestrate streams; numerical functions and model inputs remain ordinary immutable Kotlin values.

Room is recommended for the Android adapter, conditional on compatible versions. Proposed indexed tables: sessions, segments, observation chunks or bounded sample batches, profile snapshots, analysis runs, power records, estimate snapshots, trend contributors. Batch/chunk representation and compression are to be benchmarked in T03; avoid requiring one database transaction per sensor callback.

Persist source facts needed for replay plus normalization/config versions. Idempotent chunk commits and checkpoint updates share a transaction. Derived records can be replaced only by a new versioned run; preserve history/provenance. Foreign keys and unique keys enforce local ownership. Deleting a contributing ride invalidates/rebuilds affected aggregates and explicitly removes replay availability.

Do not choose toolchain/dependency versions solely from the newest Android samples. Candidate SDK 1.1.9 is source-verified; JDK/Gradle/Kotlin/Compose/Room matrix remains to be resolved against actual Karoo OS in T02.
