# T03d — Authoritative analysis persistence

OWNER-APPROVED 2026-11-11 after merged PR #16. R07/R12; V09/V14/V16 storage portions. Depends on T03a/T03c. No physiology, trend mathematics, UI, T03p or live T03e.

## Contract before code

Use a shared platform SQLite helper, schema v2. Explicit additive v1 migration preserves all five existing tables, indexes, data and collector revision. Unknown upgrades/downgrades fail without destructive fallback. No Room or new dependency.

Analysis slot is (session UUID, nonblank metric identifier, nonblank cohort identifier). request(slot, request UUID, provenance) transactionally allocates a globally increasing Long generation, inserts pending run, advances newest-requested and removes affected aggregate memberships. Retry identical request UUID/content returns its original generation, including after later requests; conflicting retry fails. Existing session is required. Provenance is an immutable versioned document naming input/source/profile/model/preprocessing/parameter/rider/trend hashes or versions and requested UTC time; persistence validates presence, not scientific truth. No measurement timestamp is invented.

complete(generation, terminal) stores available (finite value/unit/quality), unavailable (reason) or failed (reason) with calculated UTC timestamp. Terminal retry must match exactly; conflicting terminal rewrite fails. Historical runs remain queryable. Only newest available may be read as current; pending/failed/unavailable have no fallback. Completion never changes newest-requested. Completion and invalidation of any memberships containing this run share a transaction. Internal fault hook executes before transaction success for rollback tests.

Aggregate membership is a storage port, not a trend calculator: replaceMembership(aggregate ID, generation IDs) accepts only newest available structurally trend-eligible sessions, one per slot, one metric/cohort. Query returns current membership, empty after invalidation of any contributor. Aggregates are invalidated as whole projections, including all their member rows. No automatic rebuild or historical projection value is claimed. Session deletion and complete trend snapshots remain T08; no deletion API is introduced here.

Expected files: shared helper, result repository/contracts and native file-backed tests in existing app persistence packages; task, architecture, Canvas, decision log and authorization. Test on API 32/34: migration from actual v1 DDL with committed inputs, reopen, idempotency, reversed completions, unavailable supersession, failed/pending exclusion, independent writers, actual SQL constraints, request/completion rollback and aggregate invalidation. Run full owner-supplied Gradle command, links/traceability, skill validation and diff check. Risks: stale fallback, partial invalidation, destructive migration, conflicting retries, lost provenance. Device survival and physiological reproducibility remain open.

## Executed evidence — 2026-11-11

The owner-supplied command passed on JDK 25/Java 17 target with Android SDK 36:

```sh
ANDROID_HOME=/Users/pawelkozicki/Library/Android/sdk ./gradlew -PkarooDiagnostic=true -PkarooSdkJitpack=true spotlessApply :core:check :core:build :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:verifyReleaseDependencyIsolation --console=plain
```

70 app tests passed, no failures/errors/skips; seven new cases run on native file-backed SQLite at both API 32/34 (14 executions). Core: 68 passed and one explicit private-FIT skip. Debug assembly, lint and release dependency isolation passed. Existing native-access/SQLiteMode tooling warnings remain. No dependencies, SDK mapping, manifest permissions or physiology changed.

- Available → pending → unavailable → failed never falls back, including reopen; requests invalidate the whole aggregate, retaining historical results.
- Reverse-order completion preserves newest pointer and its aggregate membership. Exact old request/terminal retries preserve generations; changed request content and conflicting terminal rewrites fail.
- Injected pre-commit request failure rolls back allocation, pointer and all aggregate membership changes. Completion failure preserves pending; reopen and exact completion retry succeed. Two independent repository writers allocate distinct increasing generations with a common newest pointer.
- A frozen copy of production v1 DDL is populated with committed synthetic T03c input rows, effects, session and checkpoint. Migration through InputRepository preserves every column of all five legacy tables exactly, advances schema to v2 and permits durable results. Existing future-schema and corrupt-checkpoint tests were adjusted to v2/unsupported-v3 and pass.
- Metric/cohort slots remain isolated; mixed-cohort/metric memberships and old/noncurrent generations are rejected. Actual SQLite foreign keys reject a request for an unknown session without altering existing authority.
- Provenance stores immutable input/source/profile/model/preprocessing/parameter/rider/trend versions or hashes, code version and requested UTC; terminals retain calculated UTC, finite synthetic values/unit/quality or explicit reason. Hexadecimal doubles retain binary values; no physiological equations exist.

Development evidence: initial repository cases passed after compiler setup fixes; these were not preceded by a behavioral red run and are not claimed as such. During self-review, a compiling provenance regression failed on both APIs because code version was absent independently of model version. Adding the required codeVersion field made it pass with the full suite. The first full run also exposed a stale test fixture resetting schema to v1 after creating v2 tables; correcting that fixture to the current v2 schema restored the intended corrupt-codec assertion. No compiler/setup failure is counted as behavioral red evidence.

Self-review inspected the complete tracked/untracked diff, migration DDL, transaction boundaries, idempotency, out-of-order completion, aggregate invalidation, provenance, foreign-key ownership and privacy. Relative Markdown links resolve, all five project skills pass the installed validator, R07/R12 and V09/V14/V16 storage traceability is retained, and git diff --check passes.

Limitations: software storage evidence only, not Karoo durability/power-loss or hardware acceptance. Membership is a bounded (256 contributors) current projection port, not stored historical trend values or a trend algorithm. Complete session deletion/rebuild remains T08; V14 is partial. V09 physiology/reproducibility and V16 rendered trends remain later acceptance. No private FIT, projection or ride logs were read or published. T03p requires separate approval; live T03e still requires the T02 hardware gate.
