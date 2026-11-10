# ADR 0002 — Independent, evidence-gated estimates

Date: 2026-11-10. Status: owner-decided direction; method selection open.

Decision: FTP and VO₂max have separate eligibility rules, models, configurations and versioned results. Share time-series primitives and measured evidence, not undocumented cross-metric conversion. CP is not renamed FTP. Numeric output can be absent with reasons.

Alternative rejected: universal estimator that always returns numbers, fixed FTP fraction for all rides, or converting estimated FTP directly into VO₂max. These obscure different physiological assumptions and error sources.

Consequences: manual calibration and observed power records are separate from estimates. Quality records signal evidence/applicability; probability or error bounds need calibration data. Models require independent scientific review and release gates. Requirements R05–R08; tasks T05–T08; tests V07–V10.
