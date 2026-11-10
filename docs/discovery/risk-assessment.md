# Risk assessment

Status: open risks, 2026-11-10. No residual risk is considered accepted merely because it is listed.

| ID | Failure / impact | Severity | Mitigation and evidence gate | Residual limitation |
|---|---|---|---|---|
| K01 | Extension stops when field hidden; partial rides presented as complete | Critical | Independent collector; T02 lifecycle/4-hour test and V11 | Android process death always loses uncommitted/unobserved data |
| K02 | Receipt times mistaken for sensor measurement times | High | Time provenance, monotonic clock, latency and clock-jump probes | Sensor-origin delay cannot be reconstructed from SDK payload |
| K03 | Restart merges different rides or duplicates one | High | Local sessions/segments, idempotent writes, ambiguous recovery state | No native stable ride ID or replay in reviewed API |
| K04 | Plausible but inaccurate VO₂max/FTP | Critical | Separate model approval, abstention, external validity review | Good data cannot eliminate model bias |
| K05 | Smoothing hides bias or conflates algorithm versions | High | Show raw results; versioned cohorts, gaps, freshness and contributor IDs | Trend is not a confidence interval |
| K06 | Memory, battery or disk exhaustion during long ride | High | Bounded queues, flushes, incremental maxima, N01/N02 measurements | Budgets provisional until Karoo tests |
| K07 | Private GPS/health data enters Git | High | Local private fixture paths, no location collection, explicit export | Owner controls device access; no claim of app-level encrypted database yet |
| K08 | SDK/dependency/firmware mismatch | High | Pin release, verify runtime OS and signing/install flow | Firmware behavior can change; revalidation required |
| K09 | Old calculation cannot be reproduced | High | Immutable snapshots/input hashes and retained raw inputs/config | Deletion of inputs removes replay capability; disclose it |
| K10 | Open-source distribution without clear rights | Medium | Choose project license; preserve third-party notices; inspect FIT parser/data licensing | Public hosting alone is not a license grant |

Architectural response: K01 gates the live slice; K04 gates estimate release independently. A source-only architecture approval does not waive either.
