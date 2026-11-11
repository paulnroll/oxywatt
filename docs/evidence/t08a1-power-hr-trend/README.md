# T08a1 — Synthetic comparable power–HR experiment

Execution authorized by the owner on 2026-11-11 after specification PR #35 merged. [Scope and H01–H07](../../spdd/tasks/t08a1-power-hr-trend.md). This is a development-only software experiment, not longitudinal physiological validation or a selected method. No participant data, private FIT, bicycle, Karoo, author contact, new storage, manifests, dependencies or production estimator.

**Recommendation:** retain both candidates only as software comparators. Neither is admitted as a physiological fitness trend. Both recover injected observation changes, but neither distinguishes a power-sensor offset from change in the observed relation. A is sensitive to the retained HR distribution; B is sensitive to HR offsets and cannot produce a result without sufficient bracketed HR support. These findings do not prove passive estimation impossible; they identify confounding and evidence gaps for the next review.

## Reproducibility and explicit synthetic configuration

Sources are the existing `ExperimentFixture`, separate FTP/VO₂max `ExperimentProfile` and RunManifest contracts. New recipe helper and calculations live only in core test fixtures; persisted replay/report orchestration lives only in preview tests. The descriptor permits a list of later immutable profile snapshots under the same existing schema IDs; each profile event must reference one listed snapshot. Initial manifest profile version remains explicit. Legacy two-profile digests are unchanged; extended history uses a domain/count tag. No third physiological profile, registry or manifest exists.

Fixture base is repository main c2f5a81; actual execution used its working implementation diff. Stored provenance records build Git HEAD, selected committed input hash, initial profile digest, policy digest, candidate/model/preprocessing/trend versions and frozen input revision. This is development evidence rather than an identified release build. Input hashes exclude random database/request IDs and playback speed. Existing T05a rights declarations remain explicit invented-data permissions, not participant consent.

| Setting | Exact invented software value and purpose |
|---|---|
| Joint coverage | Acquisition elapsed ns, independent power/HR hold, each TTL 3 s; no hold past receipt/break/expiry/committed horizon |
| Window grid | 10 s, nonoverlapping half-open windows from start/reset; discard partial windows; no search for highest power |
| Time-in-ride stratum | Windows wholly within 0–60 s since observed start; explicit 0–10 s variant exercises exclusion. Synthetic boundary only, no physiological support inferred |
| Settling | 0 s in base recipe to expose arithmetic; explicit 10 s alternative tests refusal and support loss. Neither is a real warm-up recommendation |
| HR anchor / band | 150 bpm / half-width 15 bpm, chosen to bracket the invented 140/160 bpm stages |
| Stability / drift | HR SD ≤2 bpm, power SD ≤20 W; absolute HR slope ≤0.5 bpm/s, power slope ≤5 W/s; strict exceedance refuses |
| Transition reset | Power step >50 W resets grid/settling; this detector is synthetic-only and unvalidated for real rides |
| Fit | HR variance ≥1 bpm², support strictly on both sides, positive slope, weighted residual RMS ≤1 W; no extrapolation |
| Limits | At most 4096 accepted updates / 256 retained windows; overflow variants stop at 5 updates or 1 window. Constant-size moment accumulators; no whole-ride regression buffer |
| Integration tolerance | Independent anchors at 1e-10 where noninteger slopes are expected; exact integral anchors/semantic identities otherwise compared exactly. Not a scientific accuracy tolerance |
| Context | Invented indoor context, calibration-1 and sensor/calibration-1 label. Unknown refuses; changed profile/context/calibration/sensor changes comparison cohort |
| Baseline | First two explicitly named sessions in each five-session series, equal session weights; no replacement if either is unavailable/incompatible |
| Direction / timing | q=2 W and q=5 W, strict greater/less; visits at virtual report times 0/100/200/300/400 s. These are reporting/stress parameters, not physiological thresholds or a visit protocol |

Irregular integration deliberately loosens both TTLs to 20 s, transition threshold to 1000 W, power SD and slope bounds to 1000 in their respective units; this isolates time-integration correctness rather than declaring the signal physiologically stable. The unstable recipe uses transition threshold 100 W to expose within-window dispersion. All policies are hashed, explicit and versioned; no missing parameter receives a real-use default. Baseline, horizon, smoothing, staleness, sample N and meaningful physiological change remain open decisions.

## Independent numerical and refusal evidence

- H01 anchors: A time-weighted `(10×200 + 20×230)/30 = 220 W`; B `(140 bpm,200 W)` and `(160 bpm,220 W)` gives 210 W at 150 bpm, slope 1 W/bpm. Irregular held power 100 W for 2 s, 200 W for 5 s and observed zero for 3 s gives 120 W. Doubling observation density leaves the means unchanged.
- For stepwise HR 140 through 149 bpm in a ten-second window, mean 144.5 bpm, continuous time-regression slope 0.99 bpm/s, first/second half means 142/147 bpm. These independently checked moments expose drift; endpoints are not substituted for portion means.
- Missing HR and expiry never produce zero HR. Pause and gap reset joint qualification; incomplete windows are discarded. Known transitions reset settling. Singular/unbracketed, ill-conditioned, nonpositive-slope and high-residual interpolation refuse explicitly.
- Cold attachment, source failure and process-recovery fragments never contribute. Profile revision within a ride refuses cross-configuration output. Separate mass/HRmax/profile/environment/sensor changes preserve algebra where applicable but change cohorts; cross-cohort baseline unavailable. No medication, temperature, fatigue or hydration correction is invented.
- Caps stop before an extra retained window/update. Finite input causing arithmetic overflow cannot publish an infinite band value; large finite baseline values are averaged without an overflowing sum.

## Session recipe results

Each recipe traverses actual virtual timers → Collector → bounded admission → native SQLite commits → frozen paged replay → joint HR/power helper → scalar-v1 generation → reopened current read at 1x, 10x and unpaced, on API 32 and 34. Configurations and identities are fixed across speed comparisons. Table below is populated from the executed test output; hashes refer to invented inputs only.

| Recipe | A: W or refusal | B: W or refusal | Complete windows | Synthetic input hash |
|---|---|---|---|---|
| standard | 210.0 W | 210.0 W | 2 | `8b608aa2236e726e5940223f11757441bac3b949a6b0065204075bfd2ed0ef26` |
| density | 210.0 W | 210.0 W | 2 | `2d891a7bb8ea7baea6158967887d60ecf2601a2d190c5388fad20b8b2e7bbc97` |
| distribution | 215.0 W | 210.0 W | 3 | `eb9db86d5dcc11cbc1bf8457641dbee5cd93e402ba48ca707d6b73e13ae96775` |
| hr-offset | 210.0 W | 215.0 W | 2 | `fe9b1fb816047442fa36b3c6fc5484b0c334a3341d5c7ef4b9d4635d02189635` |
| drift | `excessive_drift` | `excessive_drift` | 2 | `18c5aa4f115d28c514e7fe4dc883330e1a68983344d1193b901fc7d70966c1c5` |
| lag | 210.0 W | `unbracketed_anchor` | 2 | `4a9959dc6947793317ecdb4c1b4365ec3030ef2ca9ed5c9587f110d1d016d2df` |
| missing | `no_paired_coverage` | `no_paired_coverage` | 0 | `1dae361309d622e877db1e8dd198fc06c1a7303f63cdca7f86de37ccd9a9b73a` |
| gap | `excessive_drift` | `excessive_drift` | 1 | `e53f987b077cacf0df91499067aebf4f20891de2b362be15f298efe1d4c7637e` |
| pause | `no_paired_coverage` | `no_paired_coverage` | 0 | `e1b14108cb547184ab1593d4187454ad2d0321b1d27466a58f225617b04de4e9` |
| ambiguous | `ambiguous_session` | `ambiguous_session` | 2 | `89f5705784dda9975521f9f41f61e80b9ffa1325c54ff0cc4406bd457ad40fd7` |
| profile | `configuration_changed` | `configuration_changed` | 2 | `50d2fa0ec3544164241d1417f4b2bdaa2794646e814909d3c03e033001728d1f` |
| unknown-context | `context_unverified` | `context_unverified` | 2 | `541377de5e88dc6880cac9cdea353d748a68f9976e5281dd013896d9cb53f422` |
| calibration | 210.0 W | 210.0 W | 2 | `0c2eb19b33077aec67a03488a24121d8334adf539b37d9263584ad4d28d959c5` |
| expiry | `no_paired_coverage` | `no_paired_coverage` | 0 | `c05b6e5f11f68bf6a7d3b49f7743e965bae329957878b49277c8caafa176153e` |
| zero | `nonqualifying_work` | `nonqualifying_work` | 2 | `26bec1d5413ed85d85e5cc016fb9037e033a0992a420360a1f755ef3839adfdc` |
| unstable | `unstable_window` | `unstable_window` | 2 | `02f47452bafb97bb43eaa3d1f5c68c5f74cee35f357b1b60715f2b6ffc2daa88` |
| transition | `excessive_drift` | `excessive_drift` | 1 | `64319c747acb38866bb3e335f6bf12d66ef95d8aadf67ddca87d87fd632708b1` |
| irregular | 120.0 W | `unbracketed_anchor` | 1 | `0b2a0759064fbf9c735f9dab5997b1a40be56372728ac5e4d07b0daee87be953` |

Stage reconciliation for this exact suite: 18 screened → 17 identity-qualified (one cold attachment excluded) → 15 context/configuration-qualified (unknown context and within-session profile change excluded) → 12 with complete paired windows (missing HR, pause and expiry excluded) → 7 with eligible stable windows (drift, gap/transition drift, zero-work and unstable cases excluded). A produces 7/18 screened results, 7/12 conditional on paired-window qualification and 7/7 conditional on stable windows. B produces 5/18, 5/12 and 5/7 respectively; two additional unbracketed cases refuse. A: 7 numeric + 11 refused; B: 5 numeric + 13 refused; zero execution failures in this fixed suite. Operational failure, resource and unknown-policy injections are reported separately and never treated as numeric zero.

Coverage for this deliberately mixed 18-session mutation suite is reported separately per candidate, not as a real-world coverage estimate. Paired windows and their means/SD/slopes/first-last portions/refusals are retained in `PowerHrSession.windows` and deterministically recomputable from frozen inputs; no new persisted segment store. Numeric results are one scalar per candidate/session/cohort, not independent segment-level subjects. Canonical curve semantics and its storage are unchanged.

## Synthetic series, direction and delay

All eight series have two baseline sessions at 210 W and five screened/numeric sessions per candidate (5/5), in a common synthetic cohort. No abstention is hidden from the mutation table. The three post-baseline visits are the evaluation population for null false-direction denominators; baseline self-comparisons are excluded. Raw series retain baseline points for transparency.

| Series | Raw W after the two 210 W baseline sessions | A/B conclusion |
|---|---|---|
| Null | 210, 210, 210 | No increase/decrease at either q; 0/3 false directions |
| Step up | 220, 220, 220 | Both detect at change visit: 0 virtual seconds, first eligible post-change session |
| Step down | 200, 200, 200 | Both detect decrease at change visit; same delay definition |
| Ramp | 215, 220, 225 | Both detect immediately at q=2; at q=5 the first +5 W is unresolved, detection 100 s later / second eligible post-change session |
| Reversal | 220, 200, 210 | Raw directions +/−/unresolved retained; first positive change detected immediately; no smoothing hides reversal |
| Power-offset only | 220, 220, 220 | Injected underlying relation unchanged: both declare 3/3 false increases at both q values |
| HR-offset only | A: 210,210,210; B: 215,215,215 | HR lowered by 5 bpm with power unchanged. B declares 3/3 false increases at q=2, none at q=5; A unchanged in this constructed band |
| HR-distribution only | A: 215,215,215; B: 210,210,210 | Relation unchanged, selected HR durations/distribution changed. A declares 3/3 false increases at q=2, none at q=5; B unchanged for this exact line |

A separate numerical negative-offset/null-truth exercise declares one false decrease among one post-baseline comparison (baseline 210 W, observed 200 W, q=5 W). This checks the false-decrease denominator rather than a physiological effect. A separate requested negative-change case with no correctly directed declaration is reported missed/censored with absent delay, never zero delay. The step-up and power-offset recipes intentionally have identical observed input series with different injected causal labels; their observational ambiguity is visible rather than resolved by the label. The two q values show that false-direction rates and delay depend on reporting policy; they do not identify a useful physiological threshold. Synthetic algebra predicts sensor power `x'=g x+o` and linear HR-offset sensitivity `x'=x−cδ` within bracketed support. These are software sensitivities, not empirical human error bounds.

## Authority, retention and isolation

Actual repository tests cover idempotent generation request/completion, request transaction rollback followed by exact retry, newest pending/failed/unavailable exclusion, late older completion, and rechecking contributor generation/status before returning a derived report. No older result is used as baseline fallback. Deleting the isolated input database makes calculation fail rather than silently recreate past evidence. This is a retention boundary test, not implementation of T08 product deletion/recalculation.

Scalar-v1 does not encode origin by itself. Isolation is enforced by this development-only consumer's synthetic descriptor/permission checks and mandatory `oxywatt-synthetic-power-hr-` database name; tests verify no real-history membership is written. We do **not** claim AnalysisRepository universally rejects a synthetic scalar by origin. No production history reader consumes these test databases. Release artifact inspection separately verifies helper classes are absent. T08b must still design product-wide admission/deletion with actual origin/context contracts.

## Acceptance mapping and remaining gates

| Case | Executed evidence / layer |
|---|---|
| H01 | `PowerHrExperimentTest`: independent duration/interpolation anchors, irregular means, fit/refusal and finite arithmetic; software only |
| H02 | Mutation and policy-boundary tables, core profile/context/interruption/recovery tests; synthetic applicability only |
| H03 | Eight persisted series, frozen baseline, raw/relative changes, q-sensitive direction/delay and reconciled coverage; no physiology |
| H04 | All 18 mutation recipes, 4 policy variants and 8 five-session series at 3 speeds, API 32/34, paged SQLite/reopen, equal hashes/results/contributors; synthetic V01/V09/V18/V22 only |
| H05 | Actual SQLite request rollback/retry/newest/no-fallback/late completion/report-race tests; unavailable/deleted inputs and isolated membership; product deletion remains deferred |
| H06 | Update/window caps, independent hash mutations, legacy fixture/hash/scalar/curve regressions and release isolation; no scientific resource/accuracy claim |
| H07 | This report, synchronized specification/Canvas/log/backlog/validation, self-review and actual checks below |

R02/R03/R07/R08/R10/R11/R12 have the bounded software evidence mapped by [task H01–H07](../../spdd/tasks/t08a1-power-hr-trend.md). R06/V08 is exercised only as a claim boundary, not physiological acceptance. No candidate selected, no participant experiment admitted, and no calibrated uncertainty or physiological improvement/decline inferred. Repeated independent references, lawful longitudinal data and resolved O1–O5 remain necessary. T07/T07b, T08b/c, product V10/full deletion and T02c/T03e/parent T04, V11/device V13 remain open. Hardware deferral closes no gate.

## Actual verification

JDK 25 / JVM release 17:

- `./gradlew spotlessApply :core:check :core:build --console=plain`: PASS, final core run 117 passed / one explicit private-FIT opt-in skip; ten new experiment tests. Existing legacy profile/hash/replay/curve tests remain passing.
- `ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew -PkarooDiagnostic=true -PkarooSdkJitpack=true :app:testDebugUnitTest :app:testPreviewUnitTest :app:assemblePreview :app:lintPreview :app:verifyReleaseDependencyIsolation :app:verifyPreviewIsolation --console=plain`: PASS in 4m26s, debug 92 / preview 116 passed. New preview class executes five tests on API 32 and 34 (ten executions).
- Final combined check after report metadata/as-of/reason preservation: `spotlessApply :core:check :core:build :app:testPreviewUnitTest --tests '*PowerHrExperimentReplayTest' :app:verifyReleaseDependencyIsolation :app:verifyPreviewIsolation :app:lintPreview` with the same Android environment/flags: PASS in 44s, all ten focused preview executions. Final core-only repeat after the negative-confound assertion also passed. No simultaneous Gradle builds during these accepted checks.
- Direct ZIP/DEX inspection: no PowerHr/ExperimentFixture helpers in production core JAR or release APK. Preview remains isolated; no live feature installed or exercised.
- Four requested project skills pass the installed skill-creator validator; skills unchanged. All 220 affected relative links resolve; H01–H07 R/T/V mappings and `git diff --check` pass before publication.

One earlier full Android run was invalidated by a concurrent Gradle rebuild of its shared fixture JAR: invalid ZIP local-header/class-loading failures and a dependent preview assertion. The accepted sequential rerun passed all tests; no product workaround was introduced. This failed verification attempt is not hidden as a successful run. Existing Robolectric SQLiteMode deprecation/JDK native-access warnings remain tooling notices, not device evidence.

TDD evidence: initial independent-value test failed against an explicit unavailable stub; implemented algebra made it pass. Joint-coverage test then failed against a replay stub and passed after paired-hold implementation. Native SQLite test failed with an empty calculation result on both API levels, then passed with actual frozen replay/scalar persistence. Self-review added failing finite-overflow and half-window-mean tests before their fixes. Compilation/setup errors are not counted as behavioral red evidence.
