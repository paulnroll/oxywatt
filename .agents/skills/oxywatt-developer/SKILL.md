---
name: oxywatt-developer
description: Implement, fix or refactor an approved OxyWatt work package with minimal complete changes and behavior-driven TDD. Use for application implementation after named scope approval; architecture planning and method selection belong to the specification and domain skills.
---

# OxyWatt developer

Deliver the approved behavior with the least machinery that makes it correct and maintainable. Finish the whole assigned package, including affected consumers and verification. Do not trade domain correctness for a smaller diff.

## Load one package

Paths below are repository-root relative. Read `AGENTS.md`, the assigned task in `docs/spdd/tasks/`, its R/N/V acceptance sections, and the relevant accepted contracts. Use `docs/spdd/tasks/agent-work-packages.md` for handoff boundaries. Inspect the touched code and its callers before editing; load additional documents only when needed to settle a dependency or invariant.

Verify named implementation authorization in the session and task scope. Creating this skill or planning packages does not authorize application code. A stale draft status does not require repeated permission when the owner has already approved the named scope in this session; record that approval accurately. Without approval, continue permitted specification work rather than scaffolding an app.

Use `karoo-extension` for Hammerhead adapters and `oxywatt-models` for analytics/scientific changes. Use `oxywatt-spec` when changing behavior or architecture. Method selection and hardware feasibility remain separate gates; a developer task cannot resolve them by choosing a convenient formula or assuming host behavior.

## Choose the smallest complete solution

Prefer an existing project pattern, then standard-library/platform facilities, then already-installed dependencies. Add a dependency only when its concrete benefit justifies compatibility and maintenance costs. Keep the approved two-module boundaries and source-neutral core.

Avoid speculative modules, generic frameworks, extra configuration and abstractions without a present consumer. Prefer clear direct code over compressed code. Preserve validation, failure handling, provenance and reproducibility when simplifying. A missing value stays missing; an uncertain session stays uncertain. A smaller implementation must still satisfy every applicable acceptance criterion.

## TDD for behavior

For domain logic and bug fixes, work in small observable increments:

1. Pick one acceptance case from the assigned V scenario. Define inputs and expected outcome through a public contract, independently of the implementation. For a bug, reproduce the failure first.
2. Run the focused test and confirm it fails for the intended missing behavior. Fix setup/compiler failures before claiming a meaningful red test; when introducing a new API, a compile failure alone does not demonstrate its behavior.
3. Implement enough to satisfy that case. Run the focused test, then add the next relevant case. Do not write a speculative implementation of the whole package before exercising the first case.
4. Refactor with passing tests. At package completion run affected integration checks and required repository checks; broaden further when failures, changes or unresolved risk justify it.

Prioritize irregular time, pauses/gaps, missing channels, ambiguous recovery, resource exhaustion and out-of-order completion when the package touches them. Derive numerical expectations from independent integrals or reviewed reference values; do not repeat the production algorithm in the test. Test stable outcomes, not private calls or the chosen class layout. Use deterministic clocks and synthetic safe fixtures. Exercise real repository transactions for persistence guarantees rather than proving only a mock's behavior.

Do not force unit-test TDD onto Gradle wiring, declarative UI or SDK residency. Use the relevant build, UI, integration or device checks. Add behavioral regression tests where meaningful; trivial reversible changes need no ceremonial tests. A JVM test or mocked SDK cannot establish Karoo process survival, sensor timing or physiological validity.

Discover real verification commands from approved build tooling and task instructions. If tooling or hardware is unavailable, report the missing check and its impact; never invent a command result. Before application tooling exists, only documentation/skill checks are runnable.

## Integrate without expanding scope

In a worker assignment, edit the assigned files; the coordinator owns shared contracts/build configuration unless explicitly assigned otherwise. Report a shared-file conflict with the required change and evidence so it can be integrated. Do not spawn additional agents merely because this skill is active.

Resolve routine choices autonomously inside approved constraints. If a material contract or behavior must change, describe the concrete mismatch and affected R/T/V IDs, synchronize design intent before dependent implementation, and obtain further authorization only when the existing scope does not cover it. Continue independent authorized work while that dependency is unresolved.

Close with the delivered behavior, changed files, actual verification results and remaining limitations. Update task evidence; synchronize Canvas and decisions when intent changes. Never equate a passing build with hardware or scientific acceptance. Leave publication and unrelated cleanup outside the package.

Approach inspiration: [Ponytail](https://github.com/DietrichGebert/ponytail/blob/main/skills/ponytail/SKILL.md), for minimal complete changes. These project-specific instructions use OxyWatt's contracts and approval boundaries; they do not enable a session-wide mode or extra command interface.
