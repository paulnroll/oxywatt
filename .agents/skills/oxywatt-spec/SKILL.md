---
name: oxywatt-spec
description: Create, review, phase or synchronize OxyWatt REASONS specifications and requirement-to-test traceability. Use for project behavior or architecture changes and readiness reviews.
---

# OxyWatt specifications

Read `AGENTS.md`, `docs/product/requirements.md`, `docs/spdd/reasons-canvas.md`, and relevant decisions/tasks before changing intent. Repository-root paths below are relative to OxyWatt.

Separate owner decisions, recommendations, source-verified facts and hardware/science evidence. Start a new feature with a compact problem/scope/unknowns sketch. Ask only questions that block product/architecture; continue independent authorized research. For this bootstrap, the owner requires an architectural review before any application code; never treat a draft Canvas as approval.

Use REASONS: Requirements, Entities, Approach, Structure, Operations, Norms, Safeguards. Extend existing documents instead of duplicating their requirements in a parallel spec directory. Preserve R/T/V IDs; each behavior needs an observable criterion and named verification. Every task needs goal, scope, dependencies, expected files, tests, risks and Done evidence. Small fixes need no full new Canvas.

Readiness check: input/time semantics, session identity, lifecycle, missing-data policy, quality/provenance, scientific method status, device assumptions and budget are explicit. An unresolved assumption cannot become supported by wording alone. Mark blockers per slice; source-only approval does not close a hardware or model gate.

When implementing later, read the approved task, keep changes within it, record deviations and actual verification. Update design intent before a material behavior change; synchronize actual structure after a refactor. Completion requires evidence, not checkbox wording. Do not introduce mandatory agents or external PR operations.

Reference process inspiration: [Open Mercato spec-writing](https://github.com/open-mercato/skills/blob/main/skills/om-spec-writing/SKILL.md) and [SPDD](https://martinfowler.com/articles/structured-prompt-driven/). OxyWatt's local rules take precedence over those external examples.
