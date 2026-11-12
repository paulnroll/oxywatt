# T08a2 — Comparable power–HR observation qualification and pilot readiness

Status: owner approved named documentation-only execution on 2026-11-11 after PR #37 merged. **I01–I07 dossier completed and self-reviewed, ready for owner review; real-data/scientific pilot remains NO-GO.** This is the next unused subdivision of T08a after T08a1; it does not rename T08b/c. Execution proposed here is a bounded documentation/evaluation-design package, not code, method selection or participant research.

## Problem, goal and scope

T08a1 established reproducible software behavior, not repeatability in a rider. The next useful evidence is repeated, independently acquired observations from one person with known measurement/context history, followed eventually by independent change references. Another synthetic series cannot supply either. T08a2 will deliver a reviewable qualification worksheet, an analysis/freeze plan and an explicit admission decision for such evidence. A blocked pilot is a legitimate result; a software pass cannot make it ready.

Use the label **Experimental power at comparable HR** (W). A change describes the measured power–HR relation under a declared selection policy. It does not establish improvement, aerobic efficiency, FTP or VO₂max. Stable absolute bias could be acceptable for personal tracking, but neither its stability nor its cancellation in differences has been demonstrated. Fatigue-related HR suppression can move this observation in the apparently favorable direction.

Dependencies: [T08a1 specification](t08a1-power-hr-trend.md), [H01–H07 evidence](../../evidence/t08a1-power-hr-trend/README.md), implemented T03/T04/T05a contracts, [T05b protocol](t05b-evaluation-protocol.md) and [B01–B08 evidence](../../evidence/t05b-protocol-review/README.md), [T07a audit](../../evidence/t07a-vo2max-method-audit/README.md). T06/T07 retain independent physiological admissions and O1–O5. No bike, Karoo or private FIT required now. T02c/T03e/parent T04 live gates and T08b/c stay open.

## Critical review of T08a1

The reviewed baseline is PR #36, head `d97078e2a4d54b793e0c9e895bafc6e836682611`, merged as `31b43adaff60a0e68ceb163a3bc0149815f3a33f`. Reported local passes remain local evidence; remote CI is recorded separately below.

- Both methods recover injected observational steps. A power offset produces the same observed series as a true change in the constructed relation. No statistic of those same channels can identify which cause occurred.
- A changes when retained HR duration/distribution changes. B resists that particular exact linear construction, but moves under HR offset and needs bracketed support. Neither demonstrates greater physiological validity.
- In `powerHrValue`, A filters the common qualified segments by its band, while B fits all supplied qualified segments. H01–H07 do not establish an empirical A/B comparison on matched cross-session HR distributions/time-in-ride. The five-visit synthetic series' complete coverage is recipe-specific.
- Joint-interval weighting, missing/zero distinction, settling/breaks, resource refusal, retained hashes and newest-generation tests are useful prerequisites. Synthetic transition knowledge, context declarations and fixed numeric stress policies do not validate real-world qualification or sensor identity.
- The report discloses an invalid earlier run caused by concurrent JAR rebuilding and the later sequential verification. Preserve that disclosure. No parallel Gradle builds sharing artifacts.
- No smoothing was added; it cannot identify a power offset, remove selection bias or validate fitness. No A/B winner follows from these findings.

## Primary evidence and full-text gate

[Source review and access register](../../evidence/t08a2-comparable-observations/README.md) separates retrieved full bodies from abstracts/search extraction. It covers submaximal cycling, repeatability, longitudinal changes and HR suppression. Its conclusions are bounded: controlled research warrants investigation, not transplantation of protocol, HRmax anchors, thresholds or reported error to passive rides.

Named execution must finish the full-source audit for the directly relevant power repeatability/longitudinal sources, or explicitly record why it remains blocked. For each: equation/observable, protocol control and feedback, calibration, population, repeated-session design, reference timing/independence, excluded records, uncertainty and access/rights. Source HRV reliability is not power–HR reliability. Cross-sectional correlation is not change validity. No source N or reported smallest change becomes an OxyWatt default. Full-text access failure blocks claiming that source's protocol is verified; it does not block honest design completion.

## Proposed qualification worksheet and common-support comparison

This is a report worksheet using existing descriptors/profiles and retained input identities, not a schema, manifest or registry. Each attempted visit must receive a disposition; record missing evidence rather than inventing it.

1. Confirm lawful use, person/visit linkage, actual input origin and retention. Synthetic/development records never join real observations. Unknown rights blocks processing; participant-data access is outside this package.
2. Confirm observed start/end and continuous identity. Ambiguous/cold-attached fragments are ineligible. Canonical `ReceiptStamp` acquisition/epoch/order and `MetricReading` value/missing semantics remain authoritative: receipt is not sensor measurement time; FILE_RECORD time stays distinguishable. No inferred latency correction, gap bridging or zero fill.
3. Build candidate-neutral jointly covered intervals and nonoverlapping segments under a frozen, justified time/hold/transition/settling/stability policy. Record elapsed time since original known ride start and since latest reset separately; a resume must not make a late segment early. Unknown history is not settled. Do not reuse power hold-v1 TTL as an HR physiological rule.
4. Stratify by the same person, context, sensor/calibration history, profile/policy revisions and time-in-ride. Indoor/outdoor/unknown remain separate. SourceRef is adapter identity, not sensor identity. A matching declaration is evidence of a declaration, not proof of unchanged physiology.
5. Before evaluation, fix a common local HR support interval around one frozen absolute anchor and a common time-in-ride stratum using development support only. Each compared visit must actually cover both sides of the anchor for the paired A/B analysis. A min/max envelope alone is insufficient: record duration/distribution and holes within support. No extrapolation or anchor retuning after seeing changes.
6. Feed both candidates the same admissible segment pool restricted to that support/time/context. A still applies its band and B its fit checks: report each candidate's actual retained duration/HR distribution. If distributions cannot be matched under a prespecified development-derived stratification/weighting rule, mark the paired comparison noncomparable; do not describe merely overlapping ranges as matched exposure. A new standardization formula is a separately reviewed revision, not silently A-v1.
7. Report native A and native B coverage separately, then the intersection of visits where both qualify for the locked paired analysis. Show A-only/B-only/neither visits and reasons. Report paired differences only on that intersection, with its population limitations. Do not rank candidates on different complete-case subsets.

Narrow support and time/context restrictions reduce distribution/drift ambiguity but increase abstention and may preferentially exclude fatigued, late or irregular rides. Broad support increases coverage but changes the descriptive target and may violate local linearity. Missingness can depend on training state; show qualification by visit/time/context and do not generalize the intersection to all rides. No forced winner or automatic fallback from B to A. Either, both or neither may remain investigational.

## Minimal repeated-observation pilot plan (design only)

Propose a within-person observational feasibility route, not a prescribed LSCT or training intervention. First establish repeated acquisition in a declared approximately stable routine; then evaluate whether naturally occurring changes can be tracked. Stability is a declared condition checked against context and measurements, never ground truth of unchanged fitness. Separate familiarization/development from a prospectively locked later evaluation period. Do not randomly split segments or adjacent visits and call them independent validation.

| Evidence per attempted visit | How obtained / what remains unknown | Separation rule |
|---|---|---|
| Raw power W, HR bpm, time, recording transitions, gaps; cadence when available | Measured channels with source/time provenance and retained inputs; radio latency/measurement clock may be unknown | Unsupported timing/coverage refuses; cadence is not mandatory without justification |
| Instrument continuity, setup, firmware, zeroing/calibration procedure and result/date | Explicit local equipment alias and dated operator evidence; canonical data do not guarantee sensor identity or calibration accuracy | Changed/unknown instruments or calibration history split/block comparisons; do not collect serials by default |
| Profile and configuration revisions | Existing profile snapshot/digest, policy/preprocessing/code versions, effective time | Revision boundary; mass/HRmax do not enter A/B algebra but no automatic cohort or baseline carry-over |
| Elapsed ride time, warm-up, workload transitions, posture/cadence context | Observed timestamps plus declared protocol/context; real settling detector remains unvalidated | Common time stratum and restart qualification; unknown warm-up refuses comparative status |
| Indoor/outdoor, temperature, humidity, cooling, equipment/workload mode | Measure where available with method/time; otherwise declared or unknown, separately labeled | Prespecified context strata; no invented environmental correction |
| Recent load, perceived fatigue/effort, sleep, illness, hydration, caffeine, HR-affecting medication | Voluntary lawful declaration; no diagnosis or inferred absence; sensitive detail minimized | Flag possible confounding or incompatibility; do not reinterpret suppression as improvement |
| Reference visits, if a later claim requires them | Independent procedure, dates, calibration, variability and assessor evidence | No vendor estimate, configured FTP, A/B baseline or same-channel derived label as truth |

Log all attempted visits and reasons before selecting favorable segments. Repeat visits on separate occasions are the observational units; within-ride segments are dependent. For one person, inference is individual and serially correlated; participant-cluster population inference cannot be manufactured with segment counts. If multiple people are ever separately authorized, use participant-level splits/cluster analysis under T05b.

No records are collected in T08a2. A later lawful pilot needs its own approval, responsible data custodian, consent/processing and retention basis, frozen acquisition/analysis plan, and safe reporting rules. Existing T05a metadata may reference a restricted worksheet; absent context fields remain a contract gap for later approval, not a reason to add storage now. No dataset has been secured or admitted. No author contact, participant download, private ride or identifying hash in Git.

## Metrics and claims

| Dimension | Prespecified report / denominator | Evidence limit |
|---|---|---|
| Deterministic repeatability | Same frozen input yields identical selection/reasons/contributors and numeric tolerance | Already software evidence; distinct from new acquisitions |
| Observation repeatability | Within-person paired visit differences in W, signed drift, dispersion versus magnitude/context/time; justify repeated-data uncertainty model before use | Includes biological/context/instrument variability; no assertion of physiologically stable visits; no population ICC from one person |
| Sensitivity | Output/qualification changes under empirically documented sensor uncertainty/context ranges and interactions; invalid stress separate | T08a1 synthetic offsets expose vulnerability, not plausible real uncertainty bounds |
| Coverage | All attempted → rights/identity/context/time-qualified → jointly covered → native A/B → paired numeric; unavailable and operational failures separately reconciled | Report durations and visit counts, primary exclusion plus secondary flags; refused is never zero |
| False direction | Wrong sign or spurious change versus independently justified change/null label, divided by applicable evaluated comparisons, with abstentions separately | Without a defensible null/change reference: not assessable, never zero false positives; stable-routine declarations only support exploratory discrepancies |
| Detection delay | From independently dated change (or its uncertainty interval) to first correctly directed qualifying declaration: calendar time and eligible-visit count | No reference onset/threshold: not assessable. Undetected series censored/missed, not zero delay; intervals reflect uncertain onset |
| Physiological change validity | Signed/magnitude error of within-person changes against repeated independent claim-matched references, including reference variation and bias stability over time/context | O1–O5 and independent T06/T07 gates; observation repeatability alone cannot pass |

Without CPET, a lawful later pilot can assess measurement completeness, observational repeatability, support, context sensitivity and feasibility of reporting raw W changes honestly. Repeated independent cycling performance tests could evaluate a specifically named performance-tracking claim, with their own repeatability and timing evidence; that would still not be VO₂max validation. A VO₂max-change claim requires repeated independent cycling gas references with maximum/peak status, calibration, averaging, dated mass if normalized and reference variability. FTP-change claims need independently justified FTP references, not CP substitution. Two endpoint references cannot establish intervening detection timing. Even repeated CPET does not by itself resolve selection bias or prove personal bias stability across contexts.

## Open decisions and resolution paths

Every unresolved field stays explicit; a missing real-use policy blocks calculation/admission rather than inheriting synthetic recipe defaults or legacy trend-v0.

| Decision | Required resolution before relevant pilot execution |
|---|---|
| Visit count N, spacing, duration, serial dependence and precision | Statistical design based on the intended individual decision and empirical development variability/reference uncertainty; no source N copied. Separate person count from visit count |
| Segment length, settling/hold/expiry, drift/stability/transition limits | Full protocol/source audit plus labeled development observations testing timing and coverage; freeze before evaluation |
| HR anchor/band/common support, conditioning/residual and distribution matching | Development support inspection, numerical identifiability and coverage trade-off review; no outcomes-driven retuning |
| Useful/meaningful change and false-direction/delay tolerance | Owner intended-use decision plus measurement/reference variability and scientific/statistical review; not a synthetic q |
| Baseline contributors/horizon, freshness and reference matching interval | Stability/context and serial-dependence evidence; prespecified temporal matching; unavailable when named contributors fail, no replacements |
| Smoothing | Deferred; no parameter selected. Any later proposal must compare raw results and evaluate false direction/delay; cannot conceal gaps |
| Lawful data/context availability | Responsible rights/data review and separately approved acquisition/processing; no legal dataset today |

O1 reference/pairing, O2 usefulness/coverage, O3 precision/sampling, O4 rights/population and O5 qualification/sensitivity remain open for physiological admission. The observational pilot can have narrower claim-specific gates without closing these.

## Structure, operations and safeguards

Reuse `CanonicalInput.kt`, `CollectorLifecycle.kt`, `CanonicalPowerReplay.kt`, `ExperimentProfiles.kt`, T05a descriptors/recipes and `AnalysisRepository.kt`; inspect them, do not modify them in this package. Keep separate FTP/VO₂max profiles. Retain input revision/hash, origin, time meaning, method/preprocessing/parameter/profile/rider-settings/trend/code versions, contributor IDs and as-of evidence. Unknown context is not upgraded by a matching hash.

Newest-requested generation is authoritative. Pending/failed/unavailable newest results exclude older available values from current comparisons and named baseline; late completion cannot restore them. Recheck contributor authority/status before returning a derived report; deleted required input makes reproducibility unavailable. No automatic fallback or baseline substitution. These are retained requirements, not new T08b deletion integration.

**scalar-v1 alone does not enforce synthetic origin.** T08a1 uses a development-only consumer, descriptor/permission checks and separate synthetic databases; no real-history membership is written. T08a2 specifies no general storage guarantee and introduces no real-data consumer. Future T08b must explicitly review product-wide origin admission. Synthetic results stay excluded from real history, even when scalar units/quality resemble a real observation.

Risks: sensor/context confounding is partly unidentifiable; narrow support yields selective missingness; serial dependence gives false precision; protocol-controlled sources may not transport to passive rides; access/rights may block pilot readiness. Mitigation is measured/declaration/unknown labeling, locked denominators and honest abstention, not a fitted physiological correction.

## Measurable acceptance and R–T–V mapping

I01–I07 are T08a2 document review cases, not new top-level V IDs or executed application tests.

| Case | Required reviewable delivery on named execution | Mapping |
|---|---|---|
| I01 | Full-source matrix for power repeatability, longitudinal change and suppression, including exclusions/access blockers and exact source locations; no borrowed thresholds | R06/R07/R08; V08/V09 (claim limits only) |
| I02 | Completed qualification worksheet design and A/B native/intersection support/time/context decision table; all unknown paths have dispositions and reconciled denominator definitions | R02/R07/R08; V02/V09/V15 |
| I03 | Frozen-plan template with measurement/declaration/unknown columns, equipment/calibration/profile/environment boundaries and raw-input retention; explicit lawful-data status | R02/R07/R10; V02/V09/V12 |
| I04 | Metric definitions above audited for units, repeated/serial dependence, denominator, missingness, reference onset and censoring; observational versus physiological claim map | R06/R07/R08; V08/V09/V10 (design only) |
| I05 | Each numeric open decision has resolution evidence and blocking gate; no inherited N/window/HR/drift/q/baseline/smoothing default | R07/R08; V09/V10 |
| I06 | Authority/provenance/origin contract review explicitly retains newest/no-fallback and scalar-v1 limitation; no new store/manifest/UI/framework | R03/R07/R10/R12; V09/V12/V15/V16/V22 |
| I07 | Separate software/design/real-data/scientific go/no-go table, source/access limitations, self-review and synchronized links/traceability/whitespace evidence | R07/R08/R10; V09/V12/V22; T05b O1–O5 unchanged |

Exact files for the proposed named execution: this task; `docs/evidence/t08a2-comparable-observations/README.md` (extend the review below into I01–I07); `docs/algorithms/validation-strategy.md`; `docs/algorithms/vo2max-estimation.md`; `docs/discovery/scientific-research.md`; `docs/product/requirements.md`; `docs/spdd/reasons-canvas.md`; `docs/spdd/decision-log.md`; `docs/spdd/implementation-plan.md`; `docs/spdd/tasks/agent-work-packages.md`. No core/app/build changes. No application TDD or hardware tests appropriate for these document deliverables. Check relative Markdown targets, R/T/V definitions/mappings, full staged diff and `git diff --check`; monitor published PR CI without merge.

## Readiness and Done

| Gate | Current status / reopening evidence |
|---|---|
| T08a1 software | Local H01–H07 recorded; remote checks separately monitored, never assumed passed |
| T08a2 specification | PR #37 merged by owner at 30795346649134d648f57e7e00800a8fef9fcfe1 |
| T08a2 named execution | Owner approved; [I01–I07 dossier](../../evidence/t08a2-comparable-observations/README.md) complete for review, not pilot evidence |
| Real-observation pilot | NO-GO: named Salford archive acquired for authorized local audit only; synchronization/lifecycle/context and real-use qualification/precision remain unresolved. Pilot acquisition/execution not authorized |
| Physiological trend / method / product | NO-GO: no method selected, no repeated independent references, O1–O5 open; T08b/c separate |

Done for T08a2 execution means a reviewed qualification/pilot decision dossier with explicit justified decisions or blockers, not a pilot launch or empirical usefulness claim. If access/data remain blocked, identify the precise missing evidence and next permissible step. Design completion cannot be reported as scientific readiness.

## Specification checks — 2026-11-11

Self-review: compared the complete proposed document change against main; checked claim boundaries, primary full-text versus limited access, A/B unequal selection, no invented numeric defaults, per-visit dependence/coverage, authority and scalar-origin limits, privacy and named authorization. Relative Markdown target check: 10 changed/new documents, 228 relative links, zero broken. All referenced R/V IDs resolve to requirements/validation and I01–I07 are defined. `git diff --check` passed. Skills were read but not changed; no skill validation needed. No application/Android tests rerun for documentation only; inherited local test counts are not fresh results.

PR #36 is merged with the exact supplied head/merge hashes. At 20:30 UTC, runs 43095615450 (pull_request), 43095608185 (push) and 43095780996 (main) were still queued, conclusion absent; main had zero jobs/check runs. Actions enabled. No failing test/log exists to diagnose yet. This is pending remote verification, not a pass or evidence of a code defect. Local main was updated to 31b43ad before branching; no unrelated changes were present.


## Named execution evidence — 2026-11-11

Owner approved T08a2 I01–I07 after merging PR #37. Execution stays documentation-only; no model, code or participant processing authorized. [Dossier](../../evidence/t08a2-comparable-observations/README.md) now contains recovered primary thesis/field-study methods and source limitations (I01), qualification/disposition and A/B native/intersection denominator tables (I02), completed freeze-plan worksheet with measured/declared/unknown evidence and acquisition boundaries (I03), auditable metrics and claim/reference limits (I04), explicit decision/blocker paths (I05), read-only canonical/profile/authority/origin reuse review (I06), and separate document/software/real-data/scientific/product admission decisions (I07).

No lawful dataset secured/admitted, no empirical repeatability/change measured, no method selected and no numeric defaults added. Full thesis chapters improve the audit beyond abstracts but do not establish final journal-version equivalence; remaining access/version limits stay visible. Next evidence needs an actual lawful source/context acquisition path and separate authorization before repeated observation collection. No further synthetic expansion substitutes for that evidence.

Execution checks: self-review cross-checked primary-source/version/access limits, distinct claim targets, common pool versus actual retained HR exposure, per-comparison matching, mutually exclusive primary dispositions, coverage set identities, dependent visit pairs, reference onset/censoring, unset numerical decisions and scalar-v1 isolation limits. Corrected the reconciliation to include uncompleted attempts explicitly and clarified matching per contrast when support is not series-wide. Relative-link check: 10 changed documents, 238 local Markdown targets, zero broken. All R/V references resolve; seven I01–I07 evidence sections present. `git diff --check` passes; only scoped Markdown documents changed. No application tests, private FIT or hardware checks run; no source PDF committed.

At 20:41 UTC, all six inherited remote runs remained queued with no conclusion: #36 pull_request 43095615450/push 43095608185/main 43095780996; #37 pull_request 43096566790/push 43096560344/main 43096948167. There is still no executed failing check to repair or remote pass to claim. Publication CI is monitored separately from document acceptance.


## Public-data discovery follow-up — 2026-11-11

[Search findings](../../evidence/t08a2-comparable-observations/README.md): Owner authorized a public-source search after PR #38 merged, then asked about age, sex and mass. The follow-up identifies candidates and a proposed bounded structural audit; the earlier participant-download restriction remains. This does not reopen T08a2 as a pilot or select a method.


## Authorized Salford structural review — continuation 2026-11-12

[Audit evidence](../../evidence/t08a2-comparable-observations/README.md): Owner expanded the metadata search to named Salford local structural inspection, lifting the earlier download restriction only for that archive. Archive acquired outside Git; 727 both-channel ordinal columns, 718 with matching date headers and equal extents, with timing/context limitations explicit. These are structural candidates, not eligible sessions. No code/import/A/B/pilot authorization inferred.


Report publication authorization: owner requested the search/structural report as a PR for continuation in a new session. Scope remains these five documentation files, with no raw records or local audit tools in Git. Follow-up evidence maps R02/R07/R10 to V02/V09/V12 (source interpretation, provenance and privacy review only); it does not execute canonical V09 or scientific V08. Self-review checked source/measurement/inference distinctions, 727→725→718 reconciliation, mixed sampling ratios, date/cadence defects, demographic availability and preserved admission limits. Relative links and R/V references checked; whitespace checks pass. No application tests required for the document-only publication.

Next-session entry: read this task and the evidence report's “Authorized Salford structural audit” section, then review the local retained source/audit artifacts at the documented cache path. The next decision is whether a bounded source-specific timing/alignment interpretation can be justified; do not invent missing lifecycle or silently repair dates. Discovery/planning remains authorized; import/replay/A/B code and real-observation pilot need separate concrete scope approval. Check remote CI before assuming any inherited or publication run passed.


## Executed source-specific time/alignment inspection — 2026-11-12

Owner requested concrete inspection on the existing Salford data, with no import/replay/A/B. [Time/alignment evidence](../../evidence/t08a2-comparable-observations/README.md#source-specific-timealignment-inspection--2026-11-12) records rechecked archive/extracted hashes, independent extent/ratio counts and BIFF/shared-formula inspection. Duration is calculated from count × spreadsheet interval; it is not independent clock evidence. The 718 structural candidates split into 711 same-ratio 5 s and 7 same-ratio 1 s columns. This supports a source-index grid hypothesis only; synchronization, lifecycle and context remain unverified. All remain outside the comparative pilot.

Next smallest scope is a report-only source-index availability summary under the explicit hypothesis, retaining denominators, flags and reconstruction caveats. This stays T08a2 discovery; backlog/work-package IDs checked, no new implementation subdivision or T08b/c reassignment. No application/A/B work is necessary for that summary. Any later such work requires a named approved package first. I02/I03/I05–I07 and R02/R07/R10 → V02/V09/V12 map source interpretation/provenance/privacy review only; no new canonical/software/science pass.

Remote state verified in this session: PR #39 MERGED at 2026-11-12 07:55:12 UTC, supplied head `70873678667fffe78835b04cf7c2ee39ae2d910a`; all four checks across runs 43178482505 and 43178518379 SUCCESS. Local main fast-forwarded to `e42ff25`; working tree was clean. Main run 43178824136 also completed successfully. Older runs 43095615450, 43095608185, 43095780996, 43096566790, 43096560344, 43096948167, 43097566082, 43097541824 and 43097770138 still queued without conclusions at verification. No merge or new PR performed by this session.


Continuation verification: source/hash/extent/formula inspections and self-review completed; seven changed Markdown files, 198 relative targets including anchors resolve, referenced R/V definitions and I02/I03/I05–I07 mapping checked, denominator identities reconcile, `git diff --check` PASS. No application tests required or rerun. Only documentation changes in Git; raw records, exact dates, notes and inspection artifacts remain outside the repository. No commit/push/new PR.


## Authorized source-index availability report — 2026-11-12

Owner approved the proposed report-only follow-up in this session. Frozen descriptive plan `salford-index-availability-v1`: retain all 1,344 positions and the ordered absence/header/extent/grid/date dispositions from the timing review; report the unchanged 718-column hypothesis set separately at 1 s / 5 s. Summarize stored-row counts, finite/nonmissing cells, positive-HR and zero flags, raw channel marginal distributions and column-length/positive-HR-fraction distributions. Use linear-interpolated quartiles at index `(n−1)p`; these are descriptive statistics, not eligibility cutoffs. Pooled row distributions weight rows equally, per-column distributions weight columns equally; neither implies independent observations or time weighting. No HR-conditioned power, correlation, fitting, lag adjustment, A/B, stable windows or physiological inference. Prefer row counts; any nominal seconds must disclose the support assumption and remain distinct from measured time. No repaired values, shifted channels, concatenation or removed zero-containing columns. Keep source/member hashes, exact column coordinates, script/reader/report revisions and the detailed reason ledger outside Git; publish only aggregate documentation locally. No new task ID, dependency, application code or publication authorized. Expected files are the existing dossier and its synchronized task/Canvas/decision/backlog/scientific/validation documents. Done requires reconciled counts, independent checks against prior aggregates, limitations, link/traceability and whitespace verification; scientific/pilot gates remain open.


Executed result: [source-index availability report](../../evidence/t08a2-comparable-observations/README.md#executed-source-index-availability-report--2026-11-12). All 718 columns retained (711 at 5 s / 7 at 1 s); 1,256,470 finite paired rows, 1,240,572 with positive HR, 15,898 HR-zero rows. Zero internal sample blanks do not prove measured completeness. Row/column distributions and zero-run counts remain descriptive; no A/B or independent-visit inference. Separate excluded-prefix/previous-audit reconciliation and quartile/ledger checks pass. Next useful evidence is source recording/export timing and artifact semantics, not another synthetic series. All comparative/scientific/live/product gates unchanged.


Availability-report checks: seven changed Markdown files, 204 relative links including anchors resolve, R/V and I-case traceability checked, `git diff --check` PASS. Archive/member bytes and independent excluded-prefix/count/ledger/quartile checks pass; scripts/results retained outside Git. No application or hardware tests, commit, push or PR performed.


## Executed source recording/export documentation review — 2026-11-12

Owner approved continuing to the proposed source-timing/artifact review. [Primary-source matrix and decisions](../../evidence/t08a2-comparable-observations/README.md#executed-source-recordingexport-semantics-review--2026-11-12) inspect the linked paper, official legacy SRM setup/export documentation, dated PC VI guide and period ergometer manual with applicability limits. Paper independently supports nominal 5 s acquisition; manufacturer docs distinguish storage averaging/display smoothing and configurable stop/export semantics. None supplies the archive-specific device/settings, original row/time linkage, conversion history or sentinel rules. No metadata repair or A/B performed; all preceding subsets/denominators remain unchanged.

Bounded result: stop archive-only qualification here; more counts/newer manuals do not fill the omitted provenance. Reopening needs source-linked original timing/settings/transformation evidence or a separately scoped, explicitly assumption-limited experiment. No record acquisition/contact/A/B/implementation/publication approval inferred. I01–I03/I05–I07 and R02/R07/R10 → V02/V09/V12 source-review mappings; R06/R08 → V08/V10 claim limits only. Comparative/scientific/live/product gates remain open.


Source-review checks: seven affected Markdown documents / 210 relative links including anchors resolve, R/V and I-case mappings checked, `git diff --check` PASS. Primary sources and version/applicability/access limits self-reviewed. No app/hardware tests rerun; no records/PDFs/code or Git publication added.


## Proposed next named package — T08a3

Owner requested preparation of [T08a3](t08a3-salford-index-ab.md), support discovery and conditional index-time A/B. This is a separate assumption-limited development proposal, not reopening T08a2's blocked real-observation pilot. Preparation only authorized; proposed support discovery needs named approval, numerical outputs additionally need an accepted complete freeze/execution scope. No T08a3 analysis or new implementation performed here.
