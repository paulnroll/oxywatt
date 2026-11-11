# T07a — Independent VO₂max primary-source audit

Status: owner authorized execution on 2026-11-11 following the T06a audit. Documentation/research only; no method selection, estimator or participant experiment authorized.

## Goal, dependencies and scope

Audit whether a reproducible method can support cycling VO₂max from ordinary power/HR rides; produce a reviewable recommendation or an evidence-limited negative finding. Read [T05b protocol](t05b-evaluation-protocol.md), [VO₂max status](../../algorithms/vo2max-estimation.md), [scientific ledger](../../discovery/scientific-research.md) and ADR 0002. Reuse T05a descriptors/profiles, hashes and existing storage. Independent of FTP approval. At start PR #33 is open; retain its documentation as a branch dependency, do not merge it. The branch initially used its head 8b3c653. Owner reported its merge during this audit; verified merged state, updated local main and fast-forwarded this branch to 7a58d89. Publication now targets main.

Bounded search: controlled submaximal cycling; gas/threshold or demographic regression; passive HR/power/wearable validation. Start from the three existing ledger citations and inspect linked primary findings/corrections relevant to input or reference mismatches. Stop after each group has a documented assessment and gaps. No exhaustive-review claim. No data download, private FIT, author contact, device action, application/build changes, framework or estimator code. Read public article/data-availability statements only. No age-HRmax fallback, assumed efficiency as truth or estimated-FTP shortcut.

Extract exact equations/units/coefficients and source locations, required inputs/protocol/calibration, population and participant counts, reference modality/maximality, split/repeat independence, error/agreement/coverage evidence and rights/access limitations. Inaccessible coefficients/appendices remain nonreproducible exclusions. Distinguish measured submaximal VO₂ from power/HR and cycle predictor from cycle CPET reference; VO₂peak is not silently VO₂max. Proprietary vendor reports are primary claims, not independent validation or licensed implementation.

## Observable acceptance / R–T–V

| Case | Required evidence | Mapping |
|---|---|---|
| G01 | Dated searches in all three groups, source selection/exclusion and access/correction limits | R06/R07; V08/V09 |
| G02 | Source-located equation/input/units audit or explicit nonreproducible exclusion for each retained route | R06/R07; V08/V09 |
| G03 | Population, CPET modality/maximality, participant/split/repeat/calibration and actual error/coverage evidence or missing status | R06/R07; V08/V09 |
| G04 | Canonical/profile compatibility and missing-evidence/refusal table; no physiology defaults | R02/R06/R07; V02/V08/V09 |
| G05 | Sensitivity/oracle plan conditional on actual inputs, justified ranges or open decisions, synthetic versus criterion separation | R06/R07; V08/V09/V18/V19 |
| G06 | Article/data/code rights distinguished; no dataset admitted by accessibility alone | R06/R07/R10; V08/V09/V12 |
| G07 | Recommendation and VO₂max O1–O5 update with separate audit/experiment/method/product decisions; FTP gate independent | R06/R07; V08/V09 |
| G08 | Self-review, source/unit checks, links/traceability/whitespace and synchronized actual evidence | R06/R07; V08/V09 |

## Files, risks, checks and Done

Own this task, `docs/evidence/t07a-vo2max-method-audit/README.md`, VO₂max algorithm document and scientific ledger. Synchronize Canvas, decision log, implementation plan, work packages, validation strategy and VO₂max follow-up in T05b evidence; record authorization in AGENTS.md. No changes to historical FTP findings. Risks: cross-modality target, oxygen-cost confused with maximum, unavailable gases/thresholds omitted, running/device accuracy transplanted, vendor black box mistaken for reproducible method, leakage and rights assumptions. Mitigate by source-specific extraction, conditional sensitivity and explicit unresolved decisions.

Done is G01–G08 with actual evidence including permissible negative recommendation, not R06/physiological V08 completion. Checks: source equations/units, independent arithmetic if used, relative Markdown links, R/T/V and `git diff --check`; no application test run needed for documentation. Experiment admission needs lawful cycling CPET pairs and resolved T05b O1–O5; method selection, T07b implementation and later product/live admission remain separate. T02c/T03e/parent T04 stay open and deferred.

## Executed evidence — 2026-11-11

[Audit report](../../evidence/t07a-vo2max-method-audit/README.md) completes G01–G08 with four primary-source/full-body assessments (including a vendor report), four limited/target-mismatch exclusions, conditional sensitivity, input compatibility and independent VO₂max O1–O5 dispositions. Source licenses/availability do not establish dataset admission. Recommendation retains model_not_approved for ordinary-ride VO₂max; no method selected or estimator authorized. Controlled-protocol investigation remains an optional separate scope decision. Scientific experiment admission stays NO-GO and later live gates remain deferred/open. Evidence is ready for owner review, not scientific acceptance.

Verification/self-review: source-located equation/unit/protocol checks and G01–G08 R/T/V review completed. Independent Python fractions checks confirm invented 40/50 mL/kg/min normalization anchors; no model run or scientific sensitivity claimed. All 189 relative Markdown links in the eleven affected documents resolve; `git diff --check` passes. No application tests needed/run for documentation-only work, no skills modified, no private/research dataset read or external contact.

## Owner intended-use clarification after audit — 2026-11-11

Owner prioritizes within-person trend rather than exact absolute accuracy. The audit above addresses source/method feasibility and does not establish longitudinal validity. Follow-up must assess repeatability and ability to track change, including paired repeated references for physiological claims; no requirement for perfect absolute agreement is introduced. O2 narrows accordingly, but numeric acceptance and O1/O3/O4/O5 remain unresolved. An exploratory fitness proxy/trend is a possible separately specified route; it is not automatically VO₂max. No implementation authorization follows. See updated requirements, Canvas and validation strategy.
