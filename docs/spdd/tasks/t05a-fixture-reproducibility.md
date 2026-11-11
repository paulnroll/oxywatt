# T05a — Fixture provenance, profiles and reproducible replay

Status: owner approved named **T05a** implementation on 2026-11-11 after specification PR #27 merged. Original planned criteria below remain the acceptance baseline; actual implementation evidence follows. Parent T05 remains open.

## Problem and smallest complete scope

Make an experiment input reconstructible and its permitted use auditable before choosing a physiological method. Extend existing development manifests and tests, not acquisition, result storage or a generic experiment framework. Dependencies: T01d/T01e replay contracts, T03a–d/T03p persistence and isolation, T04a/T04b1/T04b2 software evidence. T02c/T03e and parent T04 live acceptance are not prerequisites for this software package and remain open. No bicycle, Karoo or private FIT access is required.

Deliver one versioned fixture description connected to the existing RunManifest, two separately versioned input profiles, independently authored expectation tables, and executable reproducibility/refusal checks. No estimator, model selection, CP fit, sensitivity study, accuracy target, new module/dependency, schema migration, UI or second result store. T05b owns evaluation protocol, participant splits, sensitivity and scientific go/no-go; T06/T07 own separate method decisions.

## Existing contracts and precise gaps

Paths are repository-root relative; these are inspected software facts, not physiological evidence.

| Existing contract | Already supplies | T05a extension/boundary |
|---|---|---|
| `core/src/testFixtures/kotlin/com/oxywatt/core/collection/simulation/Scenarios.kt` RunManifest | scenario, seed, epoch, UTC origin, end ns, generator/normalization/profile versions, scheduler; length-prefixed UTF-8 SHA-256 | Seed/version alone do not encode offsets, segments, noise, dropouts, silence or merge order. Add a versioned fixture descriptor referenced from this manifest; preserve legacy hash behavior for legacy manifests. |
| Same file signal/mergeChannels; VirtualTimeline and SimulatedRideDataSource | seeded java.util.Random, lazy channels, stable channel tie order, monotonic virtual acquisition and separate playback deadlines | Record exact parameters, RNG identity, tie order and UTC shifts; fresh iterators for every replay. |
| `tools/export_fit_replay.py` and FitStreamReplay | fit-replay-v1, pinned fitdecode 0.11.0, input/projection byte hashes, original file times, row/end validation and provenance labels | Keep format and decoder checks. Descriptor references existing manifest/hashes; it adds rights evidence, not another FIT manifest/exporter. No private input opened. |
| `core/.../collection/CanonicalInput.kt` RiderProfileSnapshot | version/effective date, nullable mass kg, configured HRmax bpm and FTP W with manual/configured origins | Preserve canonical acquisition shape. Experimental profiles wrap/reference snapshots and record evidence unavailable in the host; no fabricated measured origin in SettingOrigin. |
| `app/.../persistence/PowerReplayHash.kt`, PowerCurveCalculation and AnalysisProvenance | typed sorted-object hashing, revision-bounded selected effects/profile hash, policy/model/code versions, atomic curve-v2 and newest generation | Reuse hashing codec for descriptor projection checks in app tests. Existing input hash includes session identity and manifest; do not claim equality across arbitrary new UUIDs or redefine old hashes. |
| LongPowerCurveTest, ExactPowerOracle and LongPowerReplayTest | independent integer/BigDecimal oracles; six long persisted cases at three speeds, reopen/readout | Add manifest mutations and profile/refusal fixtures to this established test path; reuse existing long fixtures rather than another benchmark suite. |

The abbreviated `core/...` and `app/...` entries above denote the existing main Kotlin packages. Exact ownership paths follow below.

## Fixture descriptor contract (development support only)

Use one immutable descriptor alongside RunManifest in existing simulation test fixtures. Its schema is `experiment-fixture-v1`; it is not a database entity. A dataset is an ordered list of these descriptors with pinned fixture/profile IDs and digests, not a separate registry. Start with small Kotlin fixture values and explicit expectation tables; no configurable runner or new serialization dependency.

Required fields:

- Stable fixture ID/revision, origin enum SYNTHETIC / REAL_OBSERVATION, creator/source citation, creation/generator revision, data-use status (allowed / restricted / unknown), license or consent evidence reference, permitted local use and redistribution separately, and privacy review. Unknown/restricted public redistribution blocks publication of data; unknown processing permission blocks replay. Synthetic data is still assigned explicit repository reuse terms; do not assume a repository license exists. Public fixtures contain invented identities/times only. Rights records for future private data stay local, with no subject identifiers or private hashes committed.
- Generator version and source revision, seed (explicit even when unused), RNG algorithm/version, ordered channel factories, sample offsets, half-open segment tables, noise amplitude/distribution, dropouts, silence, recording transitions, restart/epoch plan, UTC shifts and end horizon. Small fixtures enumerate values; long fixtures reference checked-in deterministic recipes and parameter digests. Record counts/end independently so a truncated prefix fails.
- Units: W, bpm, rpm, kg, integer ns and UTC ISO instants. Separate monotonic receipt epoch/elapsed time, FILE_RECORD source time and synthetic UTC mapping; never rename receipt as measurement time. Record exact equal-time channel/input order and timers-before-input-v1. Playback speed is execution metadata only.
- Normalization, preprocessing, lifecycle silence/hold/expiry, admission/flush configuration, numerical tolerance, resource limits, code revision and profile references. Pin existing canonical-power-hold-v1, power-range-means-v2 and power-replay-work-v2 when using the current curve. Unused policies are explicitly not applicable, never silently defaulted. Missing remains null plus reason; zero remains observed.
- Expected canonical event/count/end table, eligible coverage spans, nine-duration expected records or unavailable reasons, independent derivation reference, and reference status NONE / SYNTHETIC_ORACLE / INDEPENDENT_PHYSIOLOGY. A synthetic oracle is not physiology. Future reference records require separately lawful protocol/measurement evidence.

## Separate input profiles, without method selection

Define `ftp-input-profile-v1` and `vo2-input-profile-v1` as development records referencing the immutable canonical snapshot, effective time, digest, context and evidence provenance. Every optional datum has present/missing/not-applicable state and source; absent values cannot receive population defaults. A profile revision changes its digest, not the existing snapshot in place. Profile changes within a ride retain event order and effective time.

| Profile | Record now | Missing evidence / interpretation |
|---|---|---|
| FTP | configured/manual FTP baseline W if supplied; power/effort window references, claimed effort intent and protocol ID/version or unknown; indoor/outdoor/unknown context; reference type/date/protocol if available | Baseline is not a target or ground truth. Observed P20 and CP are not FTP. Ordinary submaximal-only trace has no established maximal protocol. No fixed 0.95 conversion or mandatory mass/HR invented. |
| VO₂max | mass kg and configured HRmax bpm if supplied, separate evidence source/date (measured/configured/manual/unknown), available power/HR channel references; optional resting HR or calibration only when supplied, with units/source | No age fallback, inferred resting HR, assumed efficiency or estimated-FTP shortcut. Required model-specific demographics, gas/threshold measurements and stage protocol remain unresolved until T07; profiles explicitly list unresolved requirements rather than fabricated parameters. |

T05a validates presence/units/provenance and fixture expectations only. It does not declare any profile physiologically eligible. Invalid nonfinite/nonpositive settings are rejected at the existing boundary; missing settings remain representable. Both automatic metrics remain `model_not_approved` in expectation metadata; no production estimator/refusal API is added. Additional fixture reason labels describe absent reference, missing profile input, insufficient protocol evidence, ambiguous continuity or forbidden use without pretending a method-specific threshold was approved.

## Hash and execution contract

Retain three distinct identities: existing FIT byte hashes; existing RunManifest hash; existing persisted replay input/profile hashes. Add one versioned descriptor digest using the same length-delimited SHA-256 principle; in app projections reuse PowerReplayHash. Pin field order/type/null/array order, UTF-8, integer nanoseconds, finite Double.toString representation and explicit schema/domain token in tests. Do not hash serializer formatting or unordered map iteration. Legacy hashes remain readable/unchanged; new descriptor-linked manifests use an explicit version/domain and cannot collide semantically with legacy ones.

Descriptor digest covers all parameters, rights status/evidence reference, profile and expectation revisions, source/projection references and semantic policies. Store pinned code revision as reproducibility context; expectation changes change the descriptor even when signal is identical. A changed signal/profile/policy must change the appropriate descriptor/replay digest; rights-only edits change descriptor identity without claiming changed physiology.

Execution record is a small test report, not persistent infrastructure: descriptor digest, existing input/profile hashes, code/runtime, speed, start/finish wall times, request UUID/generation, database path and elapsed performance. Exclude execution speed, wall times, random request IDs and temporary paths from semantic projections. Keep fixed fixture session/epoch IDs, input order, UTC mapping and commit batching across speed comparisons because existing replay hashing includes identity and selected effects. Cross-UUID or cross-batching equivalence is not promised. Deterministic virtual acquisition drives real timers; no sleeping tests and no bypassing timers with accelerated receipt timestamps.

For 1x/10x/unpaced compare canonical events and committed selected effects exactly, semantic hashes exactly, all nine values within existing max(1e-8 W, 1e-12 × magnitude) tolerance, winning ns boundaries/coverage/completeness/reasons exactly, then save/reopen/readout through current repositories. Compare semantic projection of provenance, not completedAt/request timestamps. Existing generation authority, no-fallback, scalar-v1/curve-v2 and synthetic aggregate exclusion remain unchanged. Byte hash mismatch, unsupported version, bad rights, malformed unit/time, count/end mismatch or unresolved profile reference fails explicitly before a successful experiment report; no partial success or manufactured numeric output.

## Planned measurable software acceptance / R–T–V

All A cases belong to T05a and are unexecuted. No new top-level requirement IDs are introduced.

| ID | Observable criterion and independent expectation | Mapping |
|---|---|---|
| A01 | Every public fixture has explicit synthetic origin, rights/use/redistribution evidence and no private fields. Unknown permission fails replay; unknown redistribution fails public fixture admission. Generated FIT descriptor resolves existing byte hashes/decoder version. | R07/R10; V09/V12/V21/V22 |
| A02 | Regenerate small constant/irregular/noisy seeded fixtures twice with fresh iterators: identical event table and digests. Mutate seed, offsets, noise, tie order, UTC mapping, end, preprocessing and budget one at a time: affected digests differ; corrupt counts/end/version/units fail. | R01/R02/R07/R11; V01/V02/V09/V18/V19/V22 |
| A03 | Separate profiles cover complete supplied fields, absent mass/HRmax, invalid mass/HRmax, absent effort protocol, configured FTP baseline, and mid-session profile change. No defaults appear; profile/version/effective-time changes alter digest. FTP/VO₂max expectations remain model_not_approved. | R05/R06/R07; V07/V08/V09 |
| A04 | Hand-authored constant 200 W for 60 confirmed seconds: 5/30/60 s winners start at 0; longer durations unavailable. Independent irregular overlap and pause/expiry/zero/missing tables match exact events, coverage, reasons and nine results. Tables cannot call production generator, integrator or comparator to derive expected output. | R02/R03/R04/R07; V03/V06/V09/V19 |
| A05 | Descriptor-linked representative constant, irregular and break/profile-change fixtures pass real virtual-timer → Collector/admission → SQLite → calculation → atomic save → reopen/readout at all three speeds with the equality contract above. Verify full counts/end and hashes, not only winning values. Existing six long fixtures retain their 18 speed combinations. | R03/R04/R07/R12; V04/V09/V14/V18/V22/V23 |
| A06 | Rights/unsupported-schema/hash/profile-reference failures emit no successful report. Insufficient coverage yields explicit unavailable duration; ambiguous fragments remain separate and synthetic contributes zero aggregates. Newest pending/failed/unavailable and legacy scalar/curve regressions pass unchanged. | R03/R07/R10/R12; V05/V12/V14/V15/V16/V22 |
| A07 | Changing speed/wall time/request UUID/path only leaves semantic comparison unchanged; changing canonical session/epoch identity is detected, not silently normalized. Check legacy golden hashes unchanged and new encoding type/null/field-order vectors independently. | R07; V09/V18/V22 |

## Files, checks, risks and Done

Expected implementation ownership (no files below are implemented by this specification):

- Extend `core/src/testFixtures/kotlin/com/oxywatt/core/collection/simulation/Scenarios.kt`; add only `ExperimentFixture.kt` and `ExperimentProfiles.kt` beside it for descriptor/profile values and validation. Reuse PreviewScenario/FitStreamReplay unchanged unless descriptor linkage requires a narrow compatibility check.
- Add `core/src/test/kotlin/com/oxywatt/core/collection/simulation/ExperimentFixtureTest.kt`; extend SyntheticReplayTest and existing simulation/FIT tests for public generated fixtures only. Reuse ExactPowerOracle and handwritten tables, with derivations documented in test fixtures.
- Extend `app/src/testPreview/kotlin/com/oxywatt/diagnostic/preview/LongPowerReplayTest.kt` and add `ExperimentReplayTest.kt` beside it for small manifest/profile cases and existing SQLite/readout flow. Reuse production PowerReplayHash; no production storage/hash changes expected. If linkage requires changing existing production hash semantics, stop and revise this task first.
- Record actual A01–A07 evidence in this task and `docs/evidence/t05a-software-2026-11-11/README.md` (use actual execution date), synchronizing Canvas, backlog, decisions and validation strategy. No export regeneration from private FIT. No new dataset download or external contact.

After named approval, behavior-driven implementation runs `./gradlew :core:check :core:build --console=plain` and the established Android command with ANDROID_HOME, `-PkarooDiagnostic=true -PkarooSdkJitpack=true`, `:app:testDebugUnitTest :app:testPreviewUnitTest :app:assemblePreview :app:lintPreview :app:verifyReleaseDependencyIsolation :app:verifyPreviewIsolation` (JDK 25/JVM release 17). Private FIT opt-in remains disabled. Run Markdown link/traceability checks and git diff --check. Done requires A01–A07 passing with independently reviewed expectations, public-safe fixture rights, actual command/test counts and limitations, not merely a descriptor type compiling.

Risks: duplicated manifests, incomplete generator parameters, confusing file/semantic hashes, identity-dependent replay digests, circular expectations, rights claims without evidence, or treating missing profiles as population defaults. Mitigate by references to existing contracts, mutation tests, golden vectors, fixed identities and explicit refusal tables. Avoid expanding canonical acquisition/storage to accommodate hypothetical scientific methods.

## Separate scientific and live acceptance

T05a software acceptance establishes deterministic reconstruction, declared-use checks and numerical pipeline conformance. It cannot verify consent truth by hashing a declaration, maximal effort intent, FTP validity or VO₂max accuracy. A synthetic signal encodes assumptions; an observed power curve is a maximum of covered observations; neither is an independently measured physiological reference.

No legally reusable paired dataset is secured. Later scientific acceptance requires lawful participant-linked cycling data with independently measured protocol-specific FTP reference and separately CPET VO₂max reference, measurement units/date, equipment/calibration, reference protocol and population/context. CP trials may support CP research but are not FTP labels; manual settings/vendor estimates are comparison observations. Missing reference protocol or temporal matching prevents criterion-validity claims. T05b must specify evaluation, sensitivity, independence/splits and go/no-go before results; T06/T07 audit primary equations and exact required inputs before method approval. T05a selects none and adds no new scientific claims or thresholds.

Live acceptance remains T02c/T03e/parent T04: sensor delivery, host lifecycle, physical CPU/RSS/battery and integrated live persistence require later identified-device evidence. Postponement closes none of those gates. No hardware action or private FIT read is authorized or needed here.

## Specification self-review evidence — 2026-11-11

Reviewed existing generator/manifest, canonical profile, FIT export/stream validation, selected-effect hashing and long persisted replay contracts. Checked scope against R/T/V, rights versus actual permission, identity-dependent hashes, missing profile evidence, independent expectations and software/science/live separation. All 137 relative Markdown links in the six affected documents resolve; A01–A07 each have R/V mappings; git diff --check passes. No application files changed, private FIT accessed, hardware checks performed or new scientific sources/accuracy claims introduced. No implementation tests run for this documentation-only package. Implementation remains pending named T05a approval.

## Approved implementation and software acceptance — 2026-11-11

Owner approved T05a after PR #27 merged. [Executed evidence](../../evidence/t05a-software-2026-11-11/README.md) maps A01–A07 to seven new core tests and two preview tests. Existing RunManifest adds an optional descriptor digest with a new linked domain, leaving legacy hashes unchanged. Two development-support files define explicit synthetic fixture tables/rights and separate FTP/VO₂max input profiles; no production source/storage/hash changes, module, dependency or experiment framework was added.

Four small fixtures (constant, irregular, pause/profile/missing and silent expiry) traverse actual virtual acquisition timers, Collector/admission, native SQLite, atomic generation and reopened preview readout at three speeds. Independent tables pin counts/end, all nine numerical/absent records, exact winning boundaries and unavailable reasons; canonical effects, semantic hashes and provenance match across speeds with fixed identities. Existing 18 long persisted combinations and scalar/curve/no-fallback/aggregate regressions pass. Rights declarations and source/projection references are checked before delivery; only synthetic descriptors are executable now, and unsupported execution policy is refused. Real/reference data admission is deliberately not claimed without lawful data.

Full verification: core 107 passes plus one explicit private-FIT skip; debug 92 and preview 106 passes; build/lint/isolation pass. After final self-review assertions/policy guard, full core and both selected preview tests (12 combinations), lint/build/isolation pass again. Evidence distinguishes these runs and records TDD history. Software A01–A07 acceptance is complete for the public synthetic scope. Scientific T05b/T06/T07, legal paired references and live T02c/T03e/parent T04 remain OPEN. No private FIT or device action occurred.

Final documentation review: 146 relative Markdown links resolve across affected documents; A01–A07 retain R/V mappings and executed evidence. git diff --check passes.
