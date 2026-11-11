# Simulation-first development change request

Date 2026-11-11. Status PROPOSED for review. Documentation only is authorized by this change request. R01–R04/R07/R09–R12, N01–N03 and V01–V22 provide traceability.

## Problem, scope and unknowns

Repeated hardware sessions currently block SDK and collection regressions. Reuse canonical v1 and test fixtures to exercise the same collection path with generated and file inputs. [ADR 0004](../../architecture/adr/0004-simulation-first-testing.md) defines the proposed source port, clock semantics, source evaluation and tradeoffs. [Validation strategy](../../algorithms/validation-strategy.md) defines layer acceptance.

Unknowns are artifact/toolchain compatibility of the SDK fakes, lawful real-FIT availability, metadata needed for timer timestamps, and real host/radio/lifetime behavior. The testing library and replay app have been source-reviewed. No compatibility spike or installation of either tool has been performed.

## Proposed packages

Each row requires named approval. Paths describe bounded work areas to finalize in the slice task before implementation.

| Package | Goal and dependencies | Expected files | Verification and Done evidence | Risk |
|---|---|---|---|---|
| T01d / T01 | Shared RideDataSource contract and deterministic simulator. Depends on ADR 0004 acceptance and canonical v1. Include constant/interval/ramp/effort generators and explicit faults, scheduler and cancellation. | core collection port, core test-support sources/tests, domain contract documentation | V18/V19/V22 plus partial V01–V03/V09. Same envelopes at 1x/10x/unpaced, exact event tables, cancellation/restart evidence. Existing seventeen tests pass. | Virtual time mixed with real timers or overextended abstraction. |
| T01e / T01 | Real FIT developer replay. Depends on T01d and lawful fixture. Retain original events/timing in versioned export manifest. | tools FIT verifier, core test resources/replay tests, fixture provenance manifest | V21/V01/V02/V18. Strict corrupt/truncated rejection, missing/invalid fields, timer states, timestamp diagnostics, original offsets and deterministic hash. | Missing redistribution permission or decoder silently losing lifecycle events. Real-file acceptance stays pending until secured. |
| T02d / T02 | SDK adapter integration tests with karoo-ext-testing candidate 0.1.1. Depends on T01d and artifact/toolchain compatibility spike. | app test sources, test dependencies/repository filters and opt-in CI configuration | V20/V22/V02/V11. Real SDK mapping equals direct canonical trace, initial state, startFit/cancel, disconnect/reconnect and cleanup. Record actual resolved versions and repeatability. | Fake differs from host, duplicate SDK classpaths, unavailable package artifacts. |
| T02c / T02 amendment | Concentrated device feasibility matrix. Optional replay app evaluated after explicit tool/device setup authorization. Depends on T02b probe, T02d for regression reuse. | existing device evidence documents | V11/V13 plus live V02. Record start marker, real samples, manual/auto pause, hidden/no field, screen off, four hours, connection recovery, kill/force-stop, RSS/battery and exact software versions. | Simulator passes while actual host or radios fail. |
| T03a–T03d amendment | Develop lifecycle/recovery, admission/loss and real repository transactions against accepted simulated sources. Depends on T01d, relevant T01e/T02d contracts, and each existing inter-package dependency. | existing collection and repository ownership areas from package plan | V03–V05/V09/V14–V17 plus V18/V19. Include silence timers, storage stalls/full disk, count/payload/in-flight caps and generation concurrency. | Treating pre-hardware development as live acceptance. |
| T03e / T03 | Live adapter/queue/storage integration. Depends on T03a–d and passed T02 hardware acquisition gate. | existing app orchestration files | Original integrated T03 checks and on-device evidence. Same core policies run with live source. | Passing component tests hides integrated ordering/loss. |

T04 still follows T03 under the current backlog. The proposed simulator provides power oracles for T04 without authorizing power-engine implementation in this change request. T05–T07 scientific gates and T08/T09 remain as specified.

## T02b impact

Keep existing evidence intact. Add mapping/teardown regressions in T02d, then use device sessions to settle remaining host/radio questions. The present probe emits logs rather than canonical envelopes. Moving its SDK subscriptions behind KarooRideDataSource is a future approved refactor, tested through the SDK fake and actual source contract. Preserve diagnostic behavior during that migration. Sensor-free transitions remain partial V11 evidence.

## CI and execution proposal

Default pull-request checks keep the pure JVM core and deterministic virtual-time suite. A separate opt-in Android test job resolves pinned SDK/test dependencies and runs Robolectric. Real FIT decoder regeneration is a separate pinned Python verification job using redistributable fixtures. Existing CI currently runs JVM checks only. Proposed commands become authoritative after the packages establish working tooling.

Run emulator and Karoo tests separately with explicit manifests identifying SDK/OS/build, tool version, fixtures, source/clock policy and observed results. Keep emulator appstore stand-ins isolated from the physical Karoo. Aggregate only sanitized hardware findings in Git. Keep real ride data and local log captures private.

## Review and Done for this change request

Review the port and clock contract, limited test dependencies, proposed T03 gate amendment and per-layer criteria. Documentation completion requires consistent Canvas/ADR/backlog/validation links, unchanged code/build files, and diff checks. Implementation Done requires the evidence in each package row. Approval of this proposal must identify the named package to implement next.

Source review found two issues that constrain the design. Accelerated external replay compresses live receipt timing, and its FIT parser ignores timer events. The design confines full-speed duration oracles to virtual acquisition time and retains FIT lifecycle evidence in OxyWatt's decoder path.
