# T01e — Developer FIT replay

Status: OWNER-APPROVED on 2026-11-11: owner requested the next package after PR publication and supplied a real Karoo FIT for private testing. Depends on T01d (PR #10); advanced by fast-forward to main `c329b85` after the owner confirmed its merge.

Scope: R01/R02/R07/R11; V21/V01/V02/V18. Extend pinned fitdecode 0.11.0 tooling with strict arbitrary-file decoding, timestamp diagnostics, lifecycle mapping and versioned exports. Feed projections through the T01d simulator. Files: tools decoder/tests, JVM replay helper/tests and synthetic resources. Private input/output stays in ignored fixtures/private, never public resources.

Policy: preserve relevant file message order and original timestamps, including timer metadata. Missing/regressing relevant timestamps are errors; never silently sort. Timer stop/stop_all map to PAUSED, not native save or Idle. Unsupported timer types are explicit errors. Missing initial/final timer states are diagnostics, never fabricated. UTC original source times stay FILE_RECORD on metric payloads. Manifest retains event times/types that canonical v1 omits. No GPS, device identifiers or unrelated fields in decoded exports.

Tests: strict CRC/corrupt/truncated rejection, absent/invalid channels, stop_all, timestamp errors, deterministic export/hash, exact replay at all speeds. Risks: real export may violate timestamp policy; report diagnostics rather than reconcile without review. Redistribution is not authorized for the owner's file or projection.

## Use

Install the pinned decoder in the temporary environment described in [T01c](t01c-fit-replay.md). From the repository root:

```sh
/tmp/oxywatt-fit-replay/bin/python tools/verify_fit_replay.py
/tmp/oxywatt-fit-replay/bin/python -m unittest discover -s tools -p 'test_export_fit_replay.py'
/tmp/oxywatt-fit-replay/bin/python tools/export_fit_replay.py --synthetic --output-dir core/src/test/resources/fit-scenario
/tmp/oxywatt-fit-replay/bin/python tools/export_fit_replay.py --input /path/to/private.fit --output-dir fixtures/private/t01e
OXYWATT_PRIVATE_FIT_REPLAY_DIR="$PWD/fixtures/private/t01e" ./gradlew :core:test --tests '*FitScenarioReplayTest' --console=plain
```

Input-file mode enforces an output directory inside ignored `fixtures/private`, resolving symlinks before the check. Export contains only three channels and timer metadata; it excludes GPS and device/rider identifiers. Never add private projections or manifests to Git. Public CI verifies only generated synthetic data. Synthetic fixtures may be regenerated intentionally with the command above; the Python suite compares both committed files to the pinned decoder.

The seven TSV columns retain decoder frame index, original file UTC, offset from first relevant message, kind, value, missing reason and original timer type. The JSON manifest versions decoder/normalization/timestamp/scheduler policies, hashes original bytes and projection, retains timer timestamps/types/triggers and records unknown completion/absent boundary diagnostics. An absent timer trigger stays null. The JVM helper consumes this exporter's canonical JSON layout, verifies the projection hash before any delivery, and uses lazy line iteration. It preserves FILE_RECORD source time while receipt origin is a fixed fixture UTC/epoch. Playback speed changes deadlines only. A real-time driver and arbitrary JSON ingestion are outside this developer helper.

Gradle tracks the private-test opt-in and private fixture files as local test inputs, so enabling/disabling/changing them cannot reuse an inapplicable test result. Without the opt-in the private test is explicitly skipped. No private fixture is required in CI.

## Evidence

- Pinned fitdecode 0.11.0 reproduced the T01c projection (15 events) and rejected bad CRC. Eight Python tests pass: strict corruption/truncation/trailing/concatenated rejection, missing/regressing timestamps, unsupported timer type, stop_all → PAUSED, timer trigger preservation, missing lifecycle boundaries, private-output containment and exact committed synthetic export regeneration.
- JVM test first reproduced missing projection-hash validation; the bridge now rejects a changed TSV before delivery. The timer-trigger Python test failed before its manifest field was implemented.
- Owner-provided real Karoo export passed strict CRC/length/order checks: 6,083 record messages, 14 timer events (7 start and 7 stop_all), 18,263 projected observations. No missing/regressing relevant timestamp was observed. Private projection and manifest remain under ignored `fixtures/private/t01e`, including original input hash. No native saved/discarded status is inferred.
- Private replay at 1x/10x/unpaced produced equal observation counts, virtual end time and streamed SHA-256 over canonical observations. Public synthetic replay also matches literal offsets, states, power and source-time provenance at all three speeds.
- `:core:check :core:build` passed on JDK 25 / JVM release 17. Standard run: 33 tests, zero failures, one explicit private-test skip. Opt-in private run: 33 tests, zero failures/skips. Toggling the opt-in re-executed tests without forcing reruns. Formatting, relative documentation links, `git diff --check` and JAR exclusion of replay/simulation helpers passed. The new Python CI job has been authored and its commands passed locally; no remote T01e CI run is claimed.

No redistribution permission was granted for the real file or derivatives. Public CI therefore has no real-ride fixture; the real-file acceptance evidence is local and cannot be reproduced from a fresh public clone alone. This establishes decoder/replay software behavior, not SDK equivalence, collection eligibility, a power curve or physiological validity. T02d/T03 and later implementation remain gated.

## Additional code review — 2026-11-11

Two integrity defects were reproduced before correction (R01/R07/R11; V18/V21):

- Text-mode export could translate LF into CRLF on Windows after the projection hash was computed. Export now writes the exact UTF-8 bytes being hashed; Git attributes enforce LF for the byte-hashed public fixtures. The regression simulates Windows text-mode newline translation; no native Windows run is claimed.
- The JVM bridge trusted manifest end time and projected-row count. Increasing the end time could manufacture an extra acquisition tail; a wrong count was silently accepted. Streaming EOF now checks a positive row count and exact agreement between the last file offset and declared end before source EOF can advance virtual time. Inconsistent metadata terminates replay with an error. Hash validation still precedes delivery; semantic EOF validation does not roll back already delivered observations.

The new Python test and both JVM regression tests failed before their corrections. Afterward all nine Python tests and all 35 JVM tests pass with the private replay enabled; formatting and core build pass. Git attributes report LF for both fixture files. Public/private isolation remains in effect.
