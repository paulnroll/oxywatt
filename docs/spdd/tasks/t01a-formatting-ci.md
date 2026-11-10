# T01a-format — Formatting and CI

Status: implemented and locally verified 2026-11-10; remote CI run pending.

Goal: consistent Kotlin formatting locally and on pull requests. Supports R11 and the JVM-only portion of V13; adds no domain acceptance or hardware evidence. Depends on T01a JVM tooling.

Scope/files: root Gradle configuration, formatting-only changes to core source/tests and Gradle scripts, `.github/workflows/ci.yml`, README, Canvas and decision log. Preserve existing uncommitted functional changes. Use Spotless 8.11.0, ktfmt 0.64, Kotlin style; target core source/tests and root/core Kotlin DSL only, excluding generated/cache files by construction.

Acceptance/verification:

- F01: `spotlessCheck` fails for misformatted Kotlin or Gradle Kotlin DSL.
- F02: `spotlessApply` repairs formatting; a second application is idempotent.
- F03: `check` and `:core:check` include formatting and JVM contract tests on JDK 25, retaining JVM/release 17.
- F04: GitHub Actions runs explicit formatting and JVM check steps on push and pull request, with read-only repository permissions. Remote execution is evidenced only after a GitHub run.

Risks/limits: formatter upgrades may cause diff churn; pin versions. No Android/device checks, automatic commits or branch protection changes.

## Evidence

- F01: initial `spotlessCheck` failed with Kotlin format violations. A controlled misformat in `settings.gradle.kts` failed `spotlessKotlinGradleCheck`; original content restored and final checks passed.
- F02: `spotlessApply` repaired existing files. A second invocation left SHA-256 hashes of all targeted files unchanged.
- F03: `spotlessApply spotlessCheck check :core:check --console=plain` passed on Temurin 25; 10 JVM tests, zero failures/errors. Final `spotlessCheck :core:check` also passed. Existing Java 17 API restriction and contract review corrections were preserved. Gradle needed sandbox escalation for its local socket and dependency downloads.
- F04: workflow has push/PR triggers, explicit formatting/JVM steps and read-only contents permission. Remote workflow has not been run; branch protection is unchanged.
- Relative Markdown link targets in the touched documentation exist; R11/V13 references resolve to the existing baseline. `git diff --check` passed. No skills changed.
