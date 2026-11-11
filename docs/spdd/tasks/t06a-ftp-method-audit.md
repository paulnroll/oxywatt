# T06a — Primary-source FTP method audit

Status: owner approved named T06a audit execution on 2026-11-11 after specification PR #32 merged. Bounded F01–F08 audit completed with explicit source-access limitations; evidence ready for owner review. No method selected or estimator implementation authorized.

## Goal and boundaries

Produce a reviewable FTP research recommendation: identify a defensible candidate for a later explicitly approved experiment, or conclude that none of the audited methods supports the intended ordinary-ride use. No candidate is automatically selected. A negative finding is a complete outcome, not a reason to invent a formula.

Dependencies: [T05a](t05a-fixture-reproducibility.md) reproducible input/profile contracts, [T05b protocol](t05b-evaluation-protocol.md) and its [executed exercises](../../evidence/t05b-protocol-review/README.md), [FTP research status](../../algorithms/ftp-estimation.md), [evidence ledger](../../discovery/scientific-research.md), and [ADR 0002](../../architecture/adr/0002-independent-estimators.md). PR #30 merged with four successful CI checks; baseline main is 62dd690. T05 scientific admission remains blocked. This read-only research may proceed without closing O1–O5 or T02c/T03e/parent T04 live acceptance. It does not bypass those gates for experiments or release.

Scope is source audit and documentation only. No core/app/build/test-code changes, estimator or CP fitting implementation, new manifest/store/framework, private FIT read, dataset download, author contact, device action or new mandatory calibration flow. T07a owns VO₂max independently. Preserve automatic FTP as model_not_approved and manual/configured FTP as a separate baseline.

## Bounded search and evidence extraction

Start with the primary sources already cited in the FTP document and evidence ledger. Audit three question groups separately: protocol-specific short-test FTP estimation; sustained-effort/protocol-defined FTP evidence; and power-duration/CP approaches as a comparator or explicitly supported predictor, never as FTP by renaming. Search specifically for ordinary-ride applicability and independent validation in each group. These are search categories, not chosen equations or an exhaustive universe of methods.

Use primary research for scientific conclusions. Reviews, vendor descriptions and search snippets can locate studies but cannot establish equations or accuracy. For each group record search date, query, source URLs, inclusion/exclusion rationale and evidence gaps. Follow direct primary validation/correction references when they materially affect a candidate. Stop the bounded audit once each group has a documented source assessment and conclusion; if a decisive source is inaccessible or ordinary-ride validation is absent, record the limitation rather than asserting an exhaustive absence of evidence. Unretrieved full text/appendices cannot be labeled audited.

For each retained candidate or comparator, provide one compact evidence row/sheet:

- Source identity and stable link; full-text/appendix availability; precise equation/table/section location. Distinguish derivation, internal validation and independent external evaluation. Check corrections where discoverable.
- Exact target definition, equation and every coefficient, units, transformation, calculation domain and required inputs. Distinguish population constants from individually fitted or calibrated quantities. If no reproducible equation is supplied, say so; do not reverse-engineer proprietary claims.
- Acquisition/test protocol: effort sequence/duration, recovery, maximality evidence, equipment/calibration, preprocessing, timing and any lactate/gas/demographic input. Record what the paper actually used separately from what the application has.
- Population, recruitment, sample/participant counts, repeat observations, environment and exclusions as reported. Distinguish study facts from support inferred for our use; note unreported fields.
- Reference target/protocol and independence, participant-level splitting/calibration, repeated-measures handling, actual error/agreement and coverage/refusal evidence. Preserve units, denominators, uncertainty and study limitations. Correlation alone does not show individual agreement.
- Data/code availability and explicit use/license evidence. “On request” is not a secured dataset; public accessibility is not permission. Read availability statements without downloading participant data or contacting anyone.
- Applicability to ordinary rides: required evidence observable in current canonical inputs/profiles, absent evidence, identifiable assumptions, and consequences for abstention. No assumed maximal effort from a high observed power or HR value.

Cross-check any transcribed equation against the primary source and its units before calling it reproducible. Conflicting equations/protocols stay visible with source-specific revisions. Source numerical examples may support an equation audit, but no estimator code or scientific validation experiment runs in T06a.

## Current input compatibility and sensitivity deliverable

Map candidate requirements onto existing canonical observations, confirmed intervals/power curve and separate `ftp-input-profile-v1` evidence. Distinguish a supplied field from reliable effort/protocol evidence. Current nine-duration maxima are observations and may not contain the distinct maximal trials, durations or uncertainty evidence a source method needs. Do not extend production contracts to make a candidate look compatible.

For every actual dependency record units, source/provenance, required/optional status, missing-input behavior, source-supported eligibility and sensitivity questions. Give proposed perturbation bounds only where primary evidence or measurement specifications justify them; otherwise identify an open decision and how to resolve it. Separate plausible measurement uncertainty from invalid-input stress cases. Include coupled assumptions/interactions when used by the equation. Do not require HR, mass or efficiency for a method that does not use them.

Preserve T05b's separation of numerical conformance, robustness, deterministic repeatability, physiological test–retest and criterion validity. Explain what a future synthetic oracle could check for each candidate and what remains impossible without independent physiological reference data.

## Recommendation and gate handling

Compare candidates without a fabricated composite score or unapproved accuracy cutoff. Use explicit evidence dimensions: target match, reproducible equation, input/protocol compatibility, independence and strength of validation, population support, sensitivity/abstention feasibility and lawful data availability. Label source findings, engineering interpretation and recommendation separately.

Permitted recommendations: candidate worth a later experiment subject to named blockers; unsuitable for ordinary rides but relevant to an optional controlled protocol; comparator only; insufficient evidence; or no suitable candidate found in the bounded audit. A controlled protocol cannot silently replace the owner's ordinary-ride goal. CP remains CP; any empirical prediction of protocol-defined FTP from CP needs its own independent evidence and validation.

Update T05b O1–O5 per FTP track with evidence or unresolved status:

| Decision | T06a contribution | Cannot close by assertion |
|---|---|---|
| O1 | Audit FTP reference definitions, protocol checks and temporal-pairing evidence | No real pair is qualified without its actual protocol/identity/time evidence |
| O2 | Extract source error/agreement/reference uncertainty and intended-use questions | Owner usefulness decision and justified accuracy/coverage targets remain explicit |
| O3 | Extract participant counts/design/precision limitations | A paper's N is not our approved sample size or independent evaluation plan |
| O4 | Inspect availability/rights statements and candidate population | No lawful matched dataset is secured merely by a citation |
| O5 | Audit equations/inputs/eligibility and justified sensitivity/numerical-oracle needs | Unknown parameters/bounds stay unresolved; no defaults invented |

End with separate statuses for audit completeness, experiment admission, method selection and product admission. A completed audit can coexist with NO-GO for experiment admission. Recommend an owner review; do not approve a method on the owner's behalf. T06b remains separately scoped and approved only after a method decision and its required evidence. If no candidate qualifies, propose an explicit scope decision rather than coding a fallback.

## Measurable acceptance and R–T–V

These are planned document review criteria; no physiological test is passed by satisfying them.

| ID | Observable audit deliverable | Mapping |
|---|---|---|
| F01 | All three search groups have dated queries, primary-source inclusion/exclusion records and bounded conclusions; missing full text is explicit. | T06a; R05/R07; V07/V09 |
| F02 | Every retained candidate has a source-located equation/units/input/protocol audit or explicit nonreproducible exclusion; CP/configured FTP/P20 distinctions preserved. | R05/R07; V07/V09 |
| F03 | Each candidate has a canonical/profile compatibility and absent-evidence table; no ordinary submaximal trace is assumed maximal; method-specific refusals remain sourced or unresolved. | R02/R05/R07; V02/V07/V09 |
| F04 | Each source assessment records reference definition/independence, population, participant/repeat counts, split/calibration and actual agreement/coverage evidence or missing status. | R05/R07; V07/V09 |
| F05 | Conditional sensitivity/oracle plan covers actual equation dependencies, units, supported bounds/interactions or open decisions, separating synthetic from criterion claims. | R05/R07; V07/V09/V18/V19 |
| F06 | Availability/rights audit cites source statements; no dataset rights or physiological validity inferred from accessibility or synthetic fixtures. | R05/R07/R10; V07/V09/V12 |
| F07 | Candidate comparison, recommendation and FTP O1–O5 disposition give distinct audit/experiment/method/product statuses; no method automatically approved or estimator introduced. | R05/R07; V07/V09 |
| F08 | Evidence, task status, Canvas, backlog and traceability synchronized; primary-source links/source locations, relative links and whitespace reviewed; limitations and actual checks recorded. | R05/R07; V07/V09 |

## Files, checks, risks and Done

Approved execution would own this task, `docs/algorithms/ftp-estimation.md`, `docs/discovery/scientific-research.md`, and one compact report `docs/evidence/t06a-ftp-method-audit/README.md` (not created by this scope specification). Synchronize `docs/algorithms/validation-strategy.md`, Canvas, decision log, implementation plan and work packages; update FTP decision dispositions in the T05b evidence only with actual findings, preserving historical exercise results. No T07 method selection or unrelated cleanup.

Checks: source/equation/unit cross-checks, F01–F08 self-review, R/T/V traceability, relative Markdown links and `git diff --check`. Application tests are not needed for documentation-only research. Do not download participant datasets to verify availability. No private FIT/hardware action. Record access failures and distinguish abstracts from full-source evidence.

Risks: protocol conversion treated as universal, CP relabeled FTP, opportunistic maxima mistaken for capacity, circular target/input, leakage, transplanted population accuracy, legal availability assumed, and unbounded literature search. Mitigate through source-specific extraction, independent-reference review, explicit compatibility/rights gaps and the bounded search log. Done is F01–F08 with a reviewable recommendation, including a justified negative finding. It is not completion of R05 or scientific/live acceptance.

## Scope specification self-review — 2026-11-11

Reviewed F01–F08 against R05/R07 and existing V07/V09, conditional input/privacy/synthetic mappings, T05b O1–O5 and independent T07/T06b gates. No new scientific claims or primary-source findings are asserted in this scope document. All 170 relative Markdown links in the seven affected documents resolve; `git diff --check` passes. No application tests run for this documentation-only specification. Existing unrelated `settings.gradle.kts` work is excluded from publication. Audit execution and F01–F08 evidence remain pending named approval.

## Executed audit — 2026-11-11

[Primary-source audit report](../../evidence/t06a-ftp-method-audit/README.md) records F01 dated searches across three groups, F02/F04 five full-body source sheets plus five partial-source exclusions and the unresolved S1 correction, F03 compatibility with existing canonical/profile/nine-duration contracts, F05 conditional sensitivity/oracle proposals, F06 availability/rights limits and F07 explicit FTP O1–O5/gate dispositions. F08 synchronizes task, algorithms/ledger, Canvas, backlog and traceability. Publication scopes remain documentation only.

Recommendation: no audited candidate supports approving automatic FTP from ordinary rides on present evidence. Controlled-protocol investigation remains an optional separate owner decision; CP stays distinct. Audit completeness does not mean every source is reproducible: inaccessible corrections/protocols and precision differences are explicit exclusions/blockers. No lawful matched dataset secured, no method selected, no estimator or participant experiment run. Experiment admission remains NO-GO; parent T06/R05, physiological V07 and deferred live gates stay open. T07a remains independent.

Verification/self-review: F01–F08 mapped to actual report sections and existing R/V definitions. Equations/units checked against primary-source locations; inconsistent source values and unverified correction not silently reconciled. Independent Python fractions checks confirm the invented 190 W and 300/275 W anchors without running an estimator. All 185 relative Markdown links in the eleven affected documents resolve; `git diff --check` passes. No application tests for documentation-only work; no skill files modified. No raw research data or private inputs retrieved.
