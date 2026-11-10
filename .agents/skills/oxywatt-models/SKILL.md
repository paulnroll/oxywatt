---
name: oxywatt-models
description: Research, specify or validate OxyWatt power curves, independent FTP and VO2max estimators, evidence quality and historical smoothing. Use when changing analytics or scientific claims.
---

# Explainable cycling models

Read the relevant file in `docs/algorithms/`, the scientific evidence ledger, and ADR 0002. Core calculations remain pure Kotlin/JVM and consume the same normalized input from Karoo or FIT. Do not embed acquisition or ORM types in a model.

Power records are observed time-weighted maxima. Missing power is not zero; pauses/gaps/restarts break windows. Verify irregular intervals against independent integration, not just uniform one-second arrays. A best observed effort need not be maximal capacity.

Keep FTP, Critical Power and VO₂max distinct. Before selecting a physiological method, inspect primary-source equations, units, coefficients, population, test protocol, required gas/lactate/threshold inputs, validation design and data/license availability. A study requiring VO₂ cannot become a power+HR model by omission. Correlation does not prove interchangeability. No age-HRmax fallback, fixed efficiency as truth, or transplanting reported laboratory accuracy to ordinary rides.

Separate coverage, effort suitability, model applicability, external validity and freshness. Without calibrated uncertainty, show evidence labels and refusal reasons rather than confidence percentages. Repeated segments from one ride do not establish independent physiological validation. FIT robustness tests do not validate VO₂max without ground truth.

Model approval requires a reproducible equation/configuration, eligibility/abstention rules, sensitivity tests and reviewed validation limits. Version preprocessing, model, profile and calibration with retained input hashes. No production model is currently approved. Keep research failures visible and seek a scope decision instead of forcing a number.

Smoothing operates only on eligible comparable estimates; preserve raw values, contributor IDs, cohort/config/as-of date and missing/stale states. Never label smoother variability a physiological confidence interval. Use `docs/algorithms/validation-strategy.md` for test IDs and the Canvas for current trend proposal.
