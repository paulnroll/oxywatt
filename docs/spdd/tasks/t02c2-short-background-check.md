# T02c2 — Short background and screen-off persistence check

Status: **DEFERRED by owner on 2026-11-12; NOT RUN.** Markdown search on 2026-11-12 found only the historical unused-ID-search mention in T03e2, no T02c2 task or reservation. This document reserves T02c2 as a short verification-only subdivision of T02c. No new application behavior or dependency exception.

## Goal and dependencies

[T03e2](t03e2-standstill-lifecycle.md) verifies actual recording/session persistence with explicit deviations. Its manual follow-up had missing channels, so it cannot establish delivery of present sensor readings while the Activity is hidden or the display is off. Test that narrow hardware question through the existing DiagnosticExtension → KarooRideDataSource → BoundedAdmission → CollectorLifecycle → InputRepository path, without a workout or new code.

Dependencies: completed T02b/T02c1/T02d/T03a–d/T03e1 and T03e2 evidence; [ADR 0003](../../architecture/adr/0003-collection-feasibility-gate.md), [ADR 0004](../../architecture/adr/0004-simulation-first-testing.md), [canonical/time/storage contracts](../../architecture/domain-model.md), and [V11](../../algorithms/validation-strategy.md). No full T02c/T03e/live T04 acceptance follows.

PR #45 is confirmed merged at fb810cf85f099793d1f5d766f215b750a3f33fdd, head 5a8c00d, four CI checks successful, no reviews/issue/inline comments at read-back. Clean local main safely fast-forwarded to that merge before this specification branch. Installed binary remains the separately verified 0.4.0/code 4 APK, SHA-256 `923a7fd70cf66d2710b644b15deea2ce73bbc37f67ab8a85a662e0ee4b8b5313`, built from 429495a plus pre-commit implementation, not a PR/merge-SHA build.

## Proposed bounded procedure and owner participation

One stationary owner-operated native recording, approximately 150 seconds of phase holds, maximum ten-minute device window. USB stays attached. Require one **already paired, physically active** sensor channel, preferably HR; owner wakes/wears it without pairing/calibration changes. No pedalling required. Power/cadence may remain missing. Constant repeated valid readings are distinct callbacks, not duplicates.

1. **Preflight:** matching installed APK, Connected/Idle, healthy storage, current source epoch and consistent baseline copy. Require at least ten distinct present receipts from one channel during a 20-second observation window; this is a test-readiness choice, not a cadence guarantee or physiological threshold. If unavailable, do not start a ride: return sensor-readiness incomplete. Record the original native auto-pause setting privately; owner temporarily disables auto-pause and later restores it, even on failure. No app reinstall/data clear/source restart.
2. **Visible, 30 s:** owner presses native Start once; confirm persisted observed-start Recording, then show diagnostics. Record a device monotonic boundary and a visible readout. Owner keeps still; there is no manual pause/resume exercise in this package.
3. **Outside diagnostics, 45 s:** owner switches to the native ride page. Verify diagnostics is not resumed using activity state metadata and a private screen capture. No OxyWatt field is added: this diagnostic build declares no custom data types. Record actual foreground surface, rather than claiming removal of an existing field. Do not reopen diagnostics during this phase.
4. **Display off, 45 s:** owner uses the normal sleep/display button. Confirm actual display-off/noninteractive state using narrow display/power metadata; a sleep-key request alone is insufficient. At the end recheck state before owner wakes the display. Avoid repeated ADB polling, screenshots, database pulls, wakelocks or app launches during the hold. USB/charging/ADB remain limitations; this does not reproduce unplugged battery use.
5. **Return, 30 s:** owner wakes display and returns to diagnostics. Record display-on/Activity-visible state and counters; source must not be restarted or replaced. Owner ends the same native recording and personally handles save/discard; require observed Idle and committed ENDED_LOCALLY. Native save is not verified. Owner restores the original auto-pause setting and confirms it.
6. **Read-back:** after an ordinary flush interval, obtain consistent private SQLite/report copies using T03e2's inactive-journal/identical-double-copy/integrity method, at most three attempts. Compare actual committed receipt positions within each phase. Do not require global pending=0 while inputs continue. One same-epoch UI revision must reconcile with its database prefix. Stop after ten minutes or one failed transition/commit confirmation exceeding 30 seconds; no automatic second ride.

Record owner actions and actual host/display/source observations separately. Do not fabricate Recording, sample timestamps, terminal Idle or an epoch to satisfy the trace. Unexpected auto-pause/transport loss/source or process restart makes the continuity result partial; owner ends the existing recording and restores settings, with no reconnect/death experiment or automatic session merge. If display-off cannot be verified, record that phase as unverified and do not claim screen-off delivery.

## Phase timing and evidence

Phase boundaries use the same device monotonic time base, with the ADB command round-trip uncertainty retained privately. The existing source reports its receipt origin in the timing/status report; canonical elapsed receipt time plus that origin maps to device elapsedRealtimeNanos. Use conservative interior intervals: post-entry confirmation through pre-exit confirmation, not owner message timestamps or workstation UTC. Report any interval that cannot be bounded as unverified. Checking display state at its boundaries does not prove uninterrupted display-off throughout; no touch during the hold is owner-declared and retained as a limitation.

Private evidence directory proposed: `~/Library/Caches/oxywatt/device-evidence/karoo-background-short-0.4.0/`, owner-only. Selected app-only logs/report, private screenshots/display/activity metadata, canonical snapshots, exact boundaries/identifiers/readings/profile remain there. Never inspect other apps' contents, private native ride storage or FITs. Public documentation contains aggregate phase counts/gaps/categories, build provenance, state/epoch continuity and explicit limits only. Existing evidence is unchanged.

## Acceptance and traceability

B01–B06 are task-local IDs, all **NOT RUN**. No new global V ID.

| Case | Observable oracle | Mapping |
|---|---|---|
| B01 | Matching binary/provenance, fresh present-channel readiness, healthy persisted Idle/current epoch; setting change/restoration explicitly owner-operated | R02/R07/R09/R10 → partial V02/V09/V11/V12 |
| B02 | Confirmed visible/outside-diagnostics/display-off/return boundaries; no app launch or intrusive polling inside hidden/off holds; uncertainty and USB/charging limitation retained | R09 → partial V11 only |
| B03 | At least two distinct present receipts from the preflight channel strictly inside each verified phase, with actual counts and maximum local inter-callback gap reported. No unreported silence/missing interval; any observed silence/gap is retained under the existing five-second engineering policy. Status-only messages do not count as present readings; equal values remain separate | R02/R03/R09 → V02/V05/partial V11/V19 |
| B04 | One new OBSERVED_START session in one epoch, no hidden/off source/process restart, interruption or ambiguous merge, final ENDED_LOCALLY. Actual control/segment table retained, including any final native Pause. No new analyses/slots/aggregate members | R03/R07/R09/R12 → V03/V05/V09/partial V11/V15/V16 |
| B05 | UI received = committed + pending + loss + rejected in the same current epoch; committed matches observation count through displayed revision. Shared queued/in-flight limits unchanged; clean target loss/rejected/storage error zero. Later copy contains the earlier admitted tail exactly once | R03/R07/R11 → V04/V09/V17/V20 |
| B06 | Read-only reopen/integrity/foreign-key and session/effect/checkpoint checks PASS, phase claims based on persisted receipts rather than sampled logs alone; aggregate documentation and private artifact separation maintained | R07/R10 → V09/V12 |

If B03 fails due to no channel data or a gap, do not replace it with a process-alive/status-only pass. Show per-phase PASS/PARTIAL/FAIL/UNVERIFIED and actual reason. Receiving present readings is not evidence of sensor accuracy, source measurement timing or physiological validity. Five-second silence remains engineering coverage metadata.

Existing software evidence is inherited: T02d adapter mapping/teardown, T03e1 coordinator stamp/timer/SQLite and Activity/extension ownership tests, T03p preview isolation. No new code/test harness or application test run is proposed. Software tests cannot pass B02/B03 hardware behavior.

## Files, risks, Done and decision

Expected tracked files: this task, Canvas, decision log, implementation plan and work packages; link T03e2 completion to this proposal. No source/build/schema/SDK/dependency change, second Collector/repository, keep-alive, analysis or physiological work. Documentation checks: relative links/anchors, R/N/V/B traceability and whitespace.

Risks: unavailable sensor, incidental auto-pause, USB-influenced residency, ambiguous display/phase evidence, background loss or failed snapshot. Stop within the bounded trial and retain partial data. Full four-hour T02c/T03e, unplugged use, reconnect, process death/force-stop, resource/battery/storage-full hardware, live T04 and science remain separate. No conclusion about automatic cold startup follows from an already running service.

Planning Done: complete reviewable scope and synchronized passing documentation checks. Execution Done after named approval: B01–B06 actual disposition, sensor presence and phase counts/gaps tied to committed inputs, current-epoch counter reconciliation, owner-confirmed setting restoration and explicit remaining gates. No publication/merge or native recording authorized by this proposal.

**Owner decision requested:** approve T02c2 as written: one short stationary recording with an existing active sensor, temporary owner-managed auto-pause disable/restore, native-page and verified screen-off phases, and bounded private evidence inspection through the same installed pipeline. Prior T03e2 approvals cover completed recordings, not this additional trial.


Planning verification: six changed/new Markdown files, 287 relative links including anchors, existing R/N/V/E references, six unique B01–B06 acceptance rows and five synchronized continuation links PASS; `git diff --check` PASS. No new application tests, device actions, native recording or private evidence reads performed. Changes remain local on docs/t02c2-short-background-check; no commit/push/PR.


## Owner deferral — 2026-11-12

Owner requested skipping this test for now, proceeding with development and possibly returning later, citing earlier device checks. T02c2 is therefore deferred, not a blocking prerequisite for preparing a new diagnostic development proposal. Existing T02b/T02c1/T03e1/T03e2 observations retain their documented scopes; no new hidden/screen-off present-channel verification is inferred. B01–B06 remain unexecuted, and no new recording, sensor action or device inspection is authorized here. A later diagnostic T04 slice needs its own named implementation approval and explicit limited ordering amendment, not a claim that the complete T02c gate passed.
