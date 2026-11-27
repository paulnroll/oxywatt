---
name: oxywatt-spec
description: Create, review or synchronize OxyWatt OpenSpec changes, legacy contracts and requirement-to-test traceability. Use for project behavior or architecture changes and readiness reviews.
---

# OxyWatt specifications

Read `AGENTS.md`, `openspec/README.md`, `docs/product/requirements.md`, and the relevant capability/decisions/evidence before changing intent. Paths below are repository-root relative. The capability map identifies OpenSpec contracts versus still-authoritative legacy documents.

Separate owner decisions, recommendations, source-verified facts and hardware/science evidence. Use the project-pinned OpenSpec CLI through `npm run openspec -- <args>` and its packaged spec-driven schema; `npm ci` restores tooling. Keep dependencies/templates/schemas in ignored node_modules and generic skills global. Project context/rules belong in `openspec/config.yaml`. Start with problem, scope and unknowns. Artifact readiness never substitutes for named implementation approval; honor existing session authorization without repeating it.

For migrated capabilities, maintain behavior in `openspec/specs/` through deltas in `openspec/changes/<name>/`; keep proposal, design and tasks together. For unmigrated capabilities, read and explicitly migrate the existing contract before extending it, distinguishing implemented, proposed and unverified behavior. Preserve legacy evidence/anchors and R/N/T/V plus local acceptance IDs; avoid competing maintained contracts. REASONS remains a useful legacy architecture checklist, not a mandatory second specification. Every task needs clear scope, dependencies, affected files, verification, risks and actual Done evidence; keep small corrections proportional. Tooling/docs-only changes without behavior deltas may use `skip_specs: true`.

Readiness check: input/time semantics, session identity, lifecycle, missing-data policy, quality/provenance, scientific method status, device assumptions and budget are explicit. An unresolved assumption cannot become supported by wording alone. Mark blockers per slice; source-only approval does not close a hardware or model gate.

When implementing, read the approved change/task, stay within scope and record deviations and actual verification. Synchronize affected architecture/science documents; log material intent decisions. Completed software tasks do not close separately gated device/science work. Archive only completed changes with honest evidence/limitations; archiving is not release acceptance. Do not introduce mandatory agents or external PR operations.

Generic workflow instructions come from the external OpenSpec installation; this skill contains OxyWatt-specific routing and evidence requirements.
