# T03p Karoo preview evidence — 2026-11-11

Scope: R02/R03/R07/R10/R11, V22/V23. Owner authorized preview installation, synthetic runs, screenshots, scoped bug fixes and PR publication. These observations do not close T02, full T02c, live T03e, radio, resource or physiology gates. [Task](../../spdd/tasks/t03p-diagnostic-preview.md).

## Device and artifacts

One authorized USB target was identified as Hammerhead model k24 (Karoo 3), Android 12/API 32, build `SKQ1.230210.001 dev-keys`. Installed ride host: `io.hammerhead.rideapp` 4.217.1-ad98202524, code 27885909. Original `com.oxywatt.diagnostic` 0.2.0/code 2 was present before installation and was neither replaced nor cleared.

Installed preview: `com.oxywatt.diagnostic.preview` 0.2.0/code 2, target 34/min 23. SDK dependency: explicit JitPack karoo-ext 1.1.9; toolchain JDK 25, compile SDK 36/Build Tools 36.0.0. Initial APK hash was `b580cce3886651a847b308327ac41b096173faeb9fa29ed4a1e910a0e6d2d908`; corrected APK hash is `40dd2b60649750150a6588a67448de694d0c9bdb6210d89ff66483f57e649ecf`. Only preview and its `.preview.test` instrumentation companion were installed. No private FIT/projection, production history, GPS, serials or raw device logs are included here.

The merged manifest disables DiagnosticExtension and MainActivity. Package Manager's extension query returned no preview services; Activity Manager showed no preview service records. Synthetic SQLite payloads carry simulated source provenance. This verifies preview isolation in the observed trials, not future native host startup under every condition.

## Observed screen and SQLite outcomes

Screenshots are unmodified device captures. The small screen requires scrolling; paired upper/lower captures are different instants. Filenames describe observed content rather than claiming a precisely scheduled capture time. The 500 ms worker/UI updates and ADB latency mean captures lag requested deadlines.

| Evidence | Actual observation |
|---|---|
| [Demo zero](demo-zero.png) | Running, virtual 1.1 s; received power 0.0 and cadence 85, age 0.1 s. Previous interrupted session remains the committed projection until the new batch commits; pending 4, loss 0. |
| [Demo received versus committed](demo-pending.png) | Running, virtual 6.8 s; received power 180, committed power 0; committed session PAUSED, saved 5, pending 2, loss 0. HR unavailable, cadence age 5.8 s. Resume has been received but is still pending. |
| [Demo EOF](demo-eof.png) | Virtual 10 s; Collector and committed session INTERRUPTED, saved 7, pending/loss 0, four coverage breaks, power age 4 s and HR unavailable. No Idle/native save is fabricated. |
| [FIT running](fit-running.png) | Running, virtual 1.7 s; power 200, HR 145, cadence 90. Pending 4; committed readout still refers to the previous interrupted FIT session. |
| [FIT pause/zero/missing](fit-pause-zero-missing.png) | Virtual 5.4 s; PAUSED, 0 W/0 rpm, HR INVALID_SOURCE_VALUE; saved 8, pending/loss 0, five coverage breaks. |
| [FIT EOF upper](fit-eof.png), [lower](fit-eof-counts.png) | Virtual 9 s, INTERRUPTED; final invalid values replace numeric values, saved 15, pending/loss 0. |

Read-only local copies of the **synthetic** SQLite databases were inspected with Python sqlite3; database files and raw dumps are not committed. Full demo sessions each contain exactly seven session-linked events at 0/1/1/2/3/5/6 s, plus the unscoped initial Idle. The two complete FIT executions after the fix contain 15 events each (30 total), with original FILE_RECORD timestamps, distinct receipt keys, zero versus null/reason preserved, pause at 3 s and resume at 8 s. Their terminal sessions are INTERRUPTED/SOURCE_STOPPED. Stored effects include SILENCE and MISSING_VALUE; FIT also records PAUSE coverage breaks. No duplicate receipt keys or completed sessions were found.

## FIT device regression and fix

The first actual FIT launch failed with DateTimeParseException. Robolectric had passed because it uses the host JVM's time implementation. A dependency-free platform instrumentation runner was added to the preview test APK and run **before** the parser fix: it failed behaviorally with `FAIL: java.lang.AssertionError: FIT REPLAY · SYNTHETIC / Source/storage failure: DateTimeParseException`. Earlier runner setup omitted `Instrumentation.start()`; that setup issue is not the behavioral red.

The exporter emits offset timestamps (`+00:00`). Android API 32's Instant parser rejected them; use OffsetDateTime parsing followed by toInstant for file origin and row times. Hash/count/end/time validations remain unchanged. The same instrumented acceptance then passed on Karoo: 15 committed events and an interrupted terminal session. Subsequent visible replay also passed. No fault panel was added. The original error was viewed in a real capture; retained screenshots here show corrected replay states.

Build the runner with `:app:assemblePreviewAndroidTest`, install the generated preview test APK on the same explicitly selected target, then run:

```sh
adb -s DEVICE shell am instrument -w com.oxywatt.diagnostic.preview.test/com.oxywatt.diagnostic.preview.PreviewReplayInstrumentation
adb -s DEVICE shell am instrument -w -e scenario lifecycle com.oxywatt.diagnostic.preview.test/com.oxywatt.diagnostic.preview.PreviewReplayInstrumentation
```

These are custom platform-instrumentation acceptance runs; inspect their explicit PASS/FAIL stream, not the shell exit status or JVM test counts. No new runtime/test library dependency was added. Instrumentation targets preview only; debug unit tests remain explicitly enabled alongside preview tests.

The lifecycle scenario passed on Karoo: start one demo worker, open Activity, explicitly call Activity.recreate, observe a distinct Activity instance through Instrumentation's monitor, finish it, reopen it, reject duplicate starts at both boundaries and finish with seven stored events. This is real-device Activity evidence, distinct from process recovery. Manual Back/reopen also retained the process and yielded seven events. An ADB clear-task attempt did not independently establish a changed Activity token and is not counted as recreation evidence; the instrumented assertion supplies that evidence.

## Limits remaining open

- Existing controls cannot deliberately produce overload/loss or SQLite-full. Pending was observed; loss stayed zero and storage stayed healthy. Fault acceptance remains Robolectric evidence, without a physical storage-full/error screenshot claim.
- Pause/resume are scripted, not manual controls. Only fixed 1x playback was exercised on Karoo; speed invariance remains JVM/Robolectric evidence.
- The scroll view can move the synthetic banner offscreen. Before the first batch commit, Collector/committed projection can still show the previous interrupted session while received values belong to the new run. The screen has no visible session identifier. This limits interpretation of early screenshots; SQLite session identities were checked independently.
- Process launch restores the demo database by default, not the last selected FIT database. FIT last-screen recovery remains unsupported. Recovery does not reconstruct received observation ages or historical per-run gap counters; unavailable age is honest.
- A direct run-as SIGKILL attempt was denied by device permissions and did not establish process death. Force-stop is a separate mechanism; neither proves low-memory behavior or survival. No OS/resource/radio/whole-ride claim follows.


## Actual process termination, Stop and subsequent start

At virtual 6.3 s, [before termination](before-process-kill.png), received power was 180 while committed power remained 0, saved count 5 and pending 2. `am force-stop com.oxywatt.diagnostic.preview` ended the process; PID absence was checked before reopening. [Recovery](process-recovery.png) showed INTERRUPTED, five saved events, no received values/ages and no pending inputs. SQLite retained the same interrupted session, changed the collector epoch and recorded PROCESS_RESTART; total rows stayed 62 and session count stayed eight. Another checkpoint read two seconds later was identical: no automatic source resume or backfill of the uncommitted tail. This is force-stop recovery evidence, not low-memory-kill evidence.

An explicit new demo followed by [Stop](explicit-stop.png) at virtual 2.9 s created a separate ninth session with four session-linked events (plus initial unscoped Idle), INTERRUPTED/SOURCE_STOPPED, pending/loss zero. Existing fragments remained separate; no COMPLETE phase was found. Stop did not synthesize an end or erase prior data.


A subsequent Start after Stop produced a separate tenth demo session with seven session-linked events; [new EOF](after-stop-new-eof.png) again shows INTERRUPTED. The demo database ended with 75 unique receipt rows across ten separate interrupted sessions; no automatic merge or completed ride was found. Final runtime checks again returned zero preview extension services/active service records, and the original host remained 0.2.0/code 2.

## Final software verification and self-review

The owner's full Gradle command, plus `:app:assemblePreviewAndroidTest`, passed after the parser fix. Core XML: 68 passes, one explicit private-FIT skip; debug 70 passes; preview 79 passes. The initial post-fix run re-executed affected core/preview tests; final verification reused those results. Both Android lint/build targets and release/preview isolation checks passed. Device acceptance is the two explicit platform-instrumentation PASS results above, not additional JVM test executions.

Self-review covered the parser's unchanged offset/hash/count/end semantics, preview-only test installation/runner wiring, explicit preservation of debug unit tests, source/release isolation, screenshot privacy, database provenance, actual-versus-attempted lifecycle checks and remaining V22/V23/T02 gates. No raw logs or database files are published. Screenshots show only the synthetic preview and system status bar.
