# T01c — Developer FIT replay

The owner authorized T01c after T01b. This package covers R01/R02/R11 and the FIT side of V01/V02. It depends on canonical input v1 and the synthetic replay fixtures from T01b.

Use [fitdecode 0.11.0](https://pypi.org/project/fitdecode/0.11.0/) under its [MIT license](https://github.com/polyvertex/fitdecode/blob/master/LICENSE.txt) in a standalone developer script. The Garmin Java SDK uses a [separate agreement](https://github.com/garmin/fit-java-sdk/blob/main/LICENSE.txt) with use and distribution conditions. The Python decoder keeps the JVM build independent of a FIT SDK.

Generate the binary fixture in memory from literal synthetic records. It contains no ride file from a person or device. Decode with strict CRC checks and emit a small TSV fixture for JVM replay. Power, HR and cadence keep their native units. A missing field maps to UNAVAILABLE. A present invalid sentinel or zero HR maps to INVALID_SOURCE_VALUE. Timer start maps to RECORDING and timer stop to PAUSED. A timer stop alone cannot establish a completed session. Preserve data message order, including equal timestamps. FIT timestamps retain FILE_RECORD provenance. A synthetic receipt clock is supplied by the JVM test using the same offsets as its comparison trace.

Scope is `tools/verify_fit_replay.py`, `core/src/test/resources/fit-replay.tsv`, `FitReplayTest.kt` in the collection tests, and linked documentation. Verification requires regenerating and comparing the TSV, checking exact canonical payloads and provenance on JVM, and rejecting a corrupted FIT. Full V01 analytics equivalence and V02 downstream eligibility require later packages.

Run from the repository root:

```sh
python3 -m venv /tmp/oxywatt-fit-replay
/tmp/oxywatt-fit-replay/bin/python -m pip install fitdecode==0.11.0
/tmp/oxywatt-fit-replay/bin/python tools/verify_fit_replay.py
./gradlew :core:check --console=plain
```

Use `--write` to regenerate the checked-in TSV after an intentional fixture change.

## Evidence and code review

- On main `47088b5`, the pinned decoder reproduced 15 projected events and rejected a bad footer CRC. `:core:check :core:build` passed on JDK 25 with 17 tests.
- Review found that byte comparison could reject a correct TSV checked out with CRLF. The verifier compares text with universal newline handling and enforces the pinned decoder version.
- The fixture contains record and timer messages inside a CRC-valid FIT stream. It exercises file parsing and mapping. A full activity export with device-specific data is outside this fixture.
- Canonical v1 carries source time on metric payloads. Timer timestamps stay in the projection and place transitions on the synthetic receipt timeline for this test. A stop produces PAUSED, and the fixture asserts event order and identity without deriving session completion.
