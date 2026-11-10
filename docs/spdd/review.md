# M0 architectural review packet

Date: 2026-11-10. Status: ready for review, not approved; no production code generated.

## Executive summary

OxyWatt will use an independent Collector and source-neutral Kotlin inputs, separate FTP/VO₂max models and versioned local history. Karoo 3 power/HR ingestion is API-feasible; whole-ride activation, longevity, timestamp quality and recovery still require device tests. The reviewed scientific sources do not establish a ready production method for passive ordinary-ride VO₂max.

## What is verified

SDK release 1.1.9, power/HR/cadence type definitions, recording states, configured rider profile, extension fields, service-reconnect consumers and FIT augmentation APIs. These are source findings, not measured device guarantees. Exact evidence and limitations: [matrix](../discovery/karoo-sdk-feasibility.md).

## Architecture and scientific recommendations

Approve the domain boundaries in [domain model](../architecture/domain-model.md) and the two-module architecture. Keep FTP and VO₂max independently gated; first deliver observed power analytics. Use supplied FITs for robustness/replay once available, without claiming lab truth or building import UI prematurely. Keep the normal-ride product objective and require a specific model decision before estimator code.

## Review corrections included

Canvas v0.2 excludes uncertain recording fragments from trend eligibility, defines authoritative recalculation generations including unavailable results, and specifies hard ingestion limits/overflow recovery. Regression scenarios V15–V17 are planned; they have not been executed against an application.

## Decisions requested

1. Approve or amend Canvas v0.2/domain model and T01–T04 first vertical slice. T02 is the device feasibility checkpoint, not a claim already validated.
2. Accept basic chart/raw points plus provisional 42-day/14-day-half-life/3-session trend defaults, or adjust them. These defaults are engineering proposals.
3. Decide project license before release; Apache-2.0 and MIT are options for later legal/license review, not a decision already made.
4. Scientific gate remains open: production FTP and VO₂max methods, usable paired validation data and acceptable error criteria require T05–T07 review. Architecture approval cannot imply scientific validation.

## Delivery and checkpoint

M0: documents/skills/source evidence → owner review → separately authorized T01–T04 → T02 hardware gate → first persisted observed power curve → independent model gates → history/chart → release validation.

**Stop here. No scaffold, diagnostic APK or production application code until explicit owner approval of the named implementation scope.** This checkpoint comes from the owner's bootstrap and subsequent milestone instruction. No device access or private FIT availability is assumed. Optional research and documentation amendments can continue within M0 authorization.

## Subsequent owner checkpoint — 2026-11-10

Owner approved source-neutral core/adapters architecture and T01a only. [T01a](tasks/t01a-canonical-input.md) records implementation and verification evidence. Historical M0 status above describes the original review packet; it no longer blocks that named package. Other slice, device, science, trend-default and license approvals remain open.
