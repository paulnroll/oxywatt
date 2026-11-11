# T05a software evidence — 2026-11-11

Owner approved [T05a](../../spdd/tasks/t05a-fixture-reproducibility.md) after PR #27 merged. Scope is existing development fixture support and tests only. No private FIT, real data download, device action, estimator, new dependency, production storage or hash changes.

## Contracts and independent expectations

`ExperimentFixture` validates public synthetic descriptors and links their versioned digest to existing RunManifest. Legacy manifest hashing is unchanged. Ordered explicit event tables are authoritative reconstruction recipes; the seeded-noise test also records java.util.Random identity, seed, uniform noise amplitude, offsets and segment. Existing FIT byte manifest/export/decoder remain authoritative; the generated-FIT descriptor references their pinned hashes and retains FILE_RECORD payload times.

Separate `ExperimentProfile` records retain canonical snapshots, optional protocol/reference/calibration, evidence source and missing states, context and unresolved method requirements. Automatic results remain model_not_approved metadata. No physiological eligibility algorithm exists.

Fixture rights are explicit declarations based on owner-authorized project contributions/publication; no repository-wide license or third-party data permission is inferred. Validation checks declared permission, not legal truth. These invented tables contain no real profiles, GPS, sensor IDs or owner FIT hashes. The helper rejects real-observation descriptors for now; lawful real/reference dataset admission remains later work rather than an untested path.

Hand-derived tables (6 s silence policy):

| Fixture | Canonical count / end | Covered power intervals | Independent expected results |
|---|---|---|---|
| constant | 63 / 60 s | 60 adjacent one-second 200 W intervals | 5/30/60 s = 200 W, start 0; six longer durations unavailable |
| irregular | 7 / 12 s | [0,2) 100 W, [2,7) 200 W, [7,8) 0 W | 5 s [2,7) = 200 W; eight longer durations unavailable |
| break-profile | 10 / 12 s | [0,2) 100 W; pause; [4,7) 200 W, [7,8) 0 W | No contiguous 5 s; all nine durations unavailable; missing at 8 s |
| expiry | 4 / 12 s | [0,6) 200 W; silence timeout at 6 s | 5 s [0,5) = 200 W; all longer durations unavailable; no bridge to 12 s |

These are synthetic numerical oracles, not physiological ground truth. Existing high-precision T04 numerical regressions remain authoritative for fractional/extreme/tie cases.

## A01–A07 mapping

- A01/A06: `permissionIsCheckedBeforeReplay`, malformed descriptor/publication rejection and generated FIT linkage/hash rejection. Existing private-data/isolation and generation/compatibility regressions retained.
- A02: `manifestMutationsAndMalformedDescriptorsAreDetected` and `seededRecipesRegenerateFreshSequencesAtEverySpeed`; fixed-count/end/schema/unit/version validation.
- A03: `profilesPreserveMissingInputsAndNeverApproveModels`, invalid canonical settings, missing protocol/profile and profile arrival during pause in persisted replay.
- A04: hand table above checked by `descriptorFixturesTraverseVirtualTimersPersistAndReopenAtEverySpeed`; production engine/generator does not derive numeric expected maxima.
- A05: four fixtures × 1x/10x/unpaced = 12 native SQLite runs. Virtual acquisition timers call actual Collector.advanceTime before equal-time observations. Compare canonical observations, selected LifecycleUpdates, all nine stored results and semantic provenance; reopen with existing PreviewPipeline/readout. Existing long test retains six fixtures × three speeds = 18 additional persisted combinations.
- A07: typed UTF-8/null/order vectors, literal legacy golden SHA-256 independently computed with Python hashlib, fixed-identity hash mutations, and existing PowerReplayHash projection excluding request/wall-time/path/speed metadata.

## Verification and review

The initial compilation failure for missing APIs was setup only. A compiling permission test then failed because replay admitted unknown processing permission, and passed after rights validation was wired before delivery. Subsequent cases were added against that implementation; no individually observed red is claimed for every case. First Android run exposed the test database name violating PreviewPipeline's synthetic-name guard; the fixture name was corrected without changing production behavior.

Final command results are recorded below after completion. Existing SQLiteMode deprecation and Conscrypt native-access warnings are not hardware evidence. Scientific T05b/T06/T07 and live T02c/T03e/parent T04 gates remain open.

Passed on JDK 25/JVM release 17:

```sh
ANDROID_HOME=/Users/pawelkozicki/Library/Android/sdk ./gradlew -PkarooDiagnostic=true -PkarooSdkJitpack=true spotlessApply :core:check :core:build :app:testDebugUnitTest :app:testPreviewUnitTest :app:assemblePreview :app:lintPreview :app:verifyReleaseDependencyIsolation :app:verifyPreviewIsolation --console=plain
```

Full suites: core 108 executions (107 passed, one explicit private-FIT skip), debug 92 passed, preview 106 passed, zero failures/errors. The preview suite includes all 12 new small-fixture and 18 existing long persisted speed combinations. Build/lint/isolation passed. Runtime is descriptive software evidence only.

Final self-review added rejection of unsupported executable policy configuration and explicit independent unavailable-reason/end-boundary assertions. A descriptor may hash a different declared policy, but observations refuse to run it through a harness configured for the pinned current policy. Rechecked the affected path after those final changes:

```sh
ANDROID_HOME=/Users/pawelkozicki/Library/Android/sdk ./gradlew -PkarooDiagnostic=true -PkarooSdkJitpack=true spotlessApply :core:check :core:build :app:testPreviewUnitTest --tests '*ExperimentReplayTest' :app:lintPreview :app:verifyReleaseDependencyIsolation :app:verifyPreviewIsolation --console=plain
```

Passed: all 107 core tests plus the explicit skip, both selected preview tests (including 12 replay combinations), lint/build/isolation. This focused run replaces the local preview XML report with the two selected tests; the earlier full-suite count is not its count. Review covered tracked and new files, policy/config linkage, exact timestamps/reasons, independent hashes/tables, compatibility, privacy and scope. All 145 relative Markdown links in affected documents resolved before the final evidence links were added; final checks follow in the task. No source or model/hardware gate was closed by wording.
