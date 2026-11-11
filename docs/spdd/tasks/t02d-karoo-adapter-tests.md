# T02d — SDK adapter integration tests

Owner-approved on 2026-11-11 after T01e merged (PR #11). Scope: R01/R02/R09/R11; V20 and SDK dependency-isolation portions of V22. Depends on accepted T01d/T01e contracts and the artifact/toolchain compatibility spike.

Goal: exercise the real Karoo SDK with karoo-ext-testing 0.1.1 and Robolectric, then map named metrics, recording state and configured profiles through a source-neutral KarooRideDataSource. Preserve diagnostic logging and FIT binding observations. The diagnostic service owns acquisition independently of views; FIT cancellation remains a host observation and does not fabricate recording state. Collector identity, persistence and hardware guarantees remain outside this package.

Expected files: app adapter/service and JVM integration tests, scoped test dependencies, opt-in Android CI, this task, Canvas and decision log. No SDK upgrade: keep 1.1.9. Default package coordinates remain unchanged; `-PkarooSdkJitpack=true` explicitly selects the public JitPack build of the same SDK tag for verification without package credentials. Never load both SDK coordinates. Fake libraries are test-only; no appstore stand-in is installed.

Acceptance: API 32 and 34 mapping against independent expected canonical payloads; missing versus invalid channels, repeated readings with distinct receipt keys, initial and subsequent lifecycle/profile callbacks; host startFit/cancel, living-process reconnect and teardown without duplicate/leaked consumers. Verify release dependency isolation. Serialized receipt stamping uses a process epoch, local UTC/monotonic time and a local sequence, never a sensor timestamp/sequence. Profiles are configured snapshots with stable versioned SHA-256 hashes of normalized values/origins; missing/invalid settings stay absent.

Mapping policy: Searching → DROPOUT; Idle/NotAvailable or a missing named field → UNAVAILABLE; nonfinite/negative values (also zero HR) → INVALID_SOURCE_VALUE. Zero power/cadence and repeated identical readings remain present. Native pause auto/manual both map to PAUSED. SourceTime and sourceSequence stay absent. Profile effective time stays unknown; invalid/nonpositive settings are absent. No profile values or numeric samples enter diagnostic logs. Binder cleanup runs outside the delivery monitor; cancellation inside accept waits to acknowledge until that callback exits. Any subscription error/completion terminates the source rather than continuing with missing lifecycle events.

Risks: fake sticky replay exceeds documented host guarantees; artifact metadata/toolchain incompatibility; callback concurrency and cancellation. No simulated result closes T02c, whole-ride residency, radio or process-survival acceptance.

## Verification evidence

Software package complete locally on 2026-11-11. Hardware acceptance and remote CI remain open.

Compatibility spike passed using published JitPack artifacts: SDK 1.1.9, fake/testing-robolectric 0.1.1, Robolectric 4.17 and AndroidX test core 1.7.0; existing Gradle 9.1.0, AGP 9.0.1, Kotlin 2.3.20 and JDK 25. Android compile SDK 36 / Build Tools 36.0.0; JVM target/release 17. No upgrade to the fake project's own AGP/Kotlin toolchain was necessary. The default GitHub Packages coordinate still needs package credentials and was not verified in this package; JitPack verification is explicit, not evidence that both feeds produce identical bytes.

```sh
./gradlew spotlessApply --console=plain
./gradlew -PkarooDiagnostic=true -PkarooSdkJitpack=true spotlessCheck :core:check :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:verifyReleaseDependencyIsolation --console=plain
```

The combined check passed: 22 app test executions (ten Robolectric cases at each of API 32 and 34, plus two mapping unit cases); 35 core tests discovered with 34 passing and one explicit private-FIT opt-in skip. Debug assembly passed, lint passed with zero errors and ten warnings (existing target/backup conventions plus the JitPack SDK's Timber lint preference for diagnostic Android Log calls). Release runtime dependency inspection found exactly one SDK and no fake/Robolectric/JUnit artifacts. No release APK or hardware run is claimed. Default JVM CI remains unchanged; `.github/workflows/android-check.yml` adds manual dispatch only. It has not been run remotely.

V20 evidence: exact canonical observation equality against an independently enumerated direct source event table, repeated zero values with distinct receipt keys, absent/invalid channels, configured profile settings and stable hashes, initial state/manual+auto pause, three reconnect cycles without duplicate registrations, self-cancellation/external in-flight cancellation, partial binding failure, sink failure and lifecycle subscription completion. The real diagnostic service runs against FakeKarooHost; test assertions observe startFit and cancellation log callbacks, no FIT effects, continued service subscriptions after FIT cancellation, and zero consumers after explicit Android service destruction **before** the fake is closed. V22 is satisfied only for dependency isolation in this package; persistence/history isolation remains deferred.

Red/green evidence: the initial metric mapper failed the zero-power assertion before implementation. An additional lifecycle-subscription completion regression failed at `assertNotNull(stopped)` on both APIs before adding completion handling to ride/profile subscriptions; both now pass. SDK JSON rejects NaN profiles before adapter delivery, so the integration test uses zero/negative settings; pure mapper tests cover nonfinite channel inputs directly.

Tooling corrections: lint caught java.time on minSdk 23, fixed with core-library desugaring 2.1.5. A combined build exhausted default Gradle Metaspace; a bounded 1 GiB heap / 768 MiB Metaspace in gradle.properties allowed the full checks to pass. Binder cleanup occurs outside the delivery monitor to avoid callback/teardown lock inversion. The diagnostic source does not buffer or persist observations; bounded Collector admission and loss/recovery remain T03.

No Karoo installation, live sensor delivery, background residency, battery or process-death claim was made. The private FIT was not required or read for T02d. T02c and T03 retain named approval gates.

Repeatability: `:app:testDebugUnitTest --rerun-tasks` passed again (22 tests) after adding assertions for the actual FIT start/cancel callbacks. Markdown links, workflow YAML parsing and `git diff --check` pass.

## Pre-publication self-review

The owner requested review and PR publication. Review reproduced nested accept callbacks when a sink synchronously triggered another SDK event: the new regression failed on both API 32 and 34. The source now rejects reentrant delivery with an explicit failure rather than nesting callbacks or adding an unbounded buffer. Cleanup acknowledgement remains deferred until the outer callback exits. V20/source serialization is preserved; no canonical contract change.

The review checked source cancellation/setup/cleanup, mapping/provenance, diagnostic FIT callbacks, release dependency isolation and all tracked/untracked files for scope and private/generated artifacts. GitHub identity was checked using the repository configuration. Live connection callbacks are diagnostic observations, not evidence of continuity; T03 must define collection loss/recovery before live integration.

Post-review verification: the combined formatting/core/app tests/debug assembly/lint/release-isolation command passed again. App tests now total 24 executions, including the reentrancy regression at API 32/34 (22 passing Robolectric executions plus two mapper cases). Core remains 34 passing plus one explicit private replay skip; lint has zero errors. All 125 relative Markdown links, workflow YAML, skill frontmatter and diff whitespace checks pass.
