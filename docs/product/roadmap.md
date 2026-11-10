# Roadmap

All releases are provisional. Scope changes need an explicit recorded decision.

- Milestone M0: discovery, domain model, REASONS Canvas, source capability matrix, skills and review. No production code. Current work.
- Milestone M1 after approval: Karoo → Collector → Persistence → Power Curve. Begin with lifecycle and timing feasibility gate T02. This milestone is not the complete 0.1 release.
- Version 0.1: reliable activity acquisition, observed power curve, manual calibration, independently gated FTP and VO₂max models, reproducible history and basic trend chart. If scientific feasibility is not met, revisit release scope explicitly rather than inserting a guessed formula.
- Version 0.2: possible CP/W′, aerobic efficiency and decoupling. CP research may inform 0.1 without shipping CP as FTP.
- Version 0.3: advanced change detection, alerts and confidence refinement. Basic history/trend is already 0.1.
- Version 0.4: user-facing FIT import and historical backfill. Developer FIT replay belongs earlier in validation.
- Later: load, fatigue, recovery-related research, other source adapters and optional export.

No generic plugin registry, cloud service, multi-user system or ML infrastructure is planned. Add extension points only where two actual implementations or a reviewed model boundary justify them.
