# Logical boundaries

These are responsibilities inside a small monolith, not seven Gradle modules or separate databases.

| Boundary | Owns | Reads / emits |
|---|---|---|
| Activity Collection | Normalization, lifecycle, coverage, session identity and input validation | Source-neutral observations → persisted session/segments |
| Power Analytics | Time-weighted observed power maxima and provenance | Session samples → per-ride power curve |
| FTP Estimation | FTP method eligibility, calibration and results | Observed efforts + profile → FTP estimate or abstention |
| VO₂max Estimation | Physiological method eligibility, assumptions and results | Qualified power/HR segments + profile → VO₂max estimate or abstention |
| Performance History | Result snapshots, series grouping, aggregation and freshness | Versioned results → UI projections |
| Karoo adapter | Host subscriptions, receipt clocks and lifecycle mapping | SDK events → source-neutral input |
| Persistence adapter | Transactions, batches, migrations and indexed retrieval | Implements core repository contracts |

FTP and VO₂max may reuse validated time-series primitives and quality facts. They do not depend on one another's estimated result by default. There is no initial event bus; domain events are local typed outcomes only where a real caller needs them. A completed analysis transaction can persist a result and its contributor references together.

The Collector is independent of Karoo and FIT libraries. It is responsible for common ingestion policy, not sensor communications or physiology. Adapters preserve source-specific evidence in metadata without leaking framework objects.
