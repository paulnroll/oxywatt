# T01a — JVM bootstrap and canonical inputs

Status: T01a complete, verified 2026-11-10 within owner-authorized scope. Parent T01 remains incomplete.

Goal: compile a source-neutral immutable input boundary without Android. Owner approved T01a and core/adapters architecture explicitly in this session. Accepted contract: [domain model v1](../../architecture/domain-model.md#canonical-input-contract-v1--t01a). Requirements R01/R02/R11; contract-level portions of V01/V02/V03, JVM independence portion of V13.

Scope/files: root Gradle scripts/wrapper, `core/build.gradle.kts`, `core/src/{main,test}/kotlin/com/oxywatt/core/collection/CanonicalInput{,Test}.kt`, authorization/build documentation. One coordinator; no workers needed before contracts stabilize.

Dependencies: named scope approval (received), installed JDK 25; Gradle 9.1.0/Kotlin 2.3.20, target/release 17. No runtime libraries beyond Kotlin/JDK; Kotlin test/JUnit for contract checks. Android compatibility is unresolved in T02.

Acceptance: preserve zero versus missing values, reject invalid/nonfinite units and malformed metadata, retain receipt/source timing and profile provenance, prevent time subtraction across epochs, represent recording/profile transitions without lifecycle side effects. Deterministic public-contract tests, no private data. Clock ordering is not deduplication; collector sequence enforcement and state machine remain T03.

Verification command: `./gradlew :core:check --console=plain` on JDK 25; `git diff --check` plus Markdown link/traceability checks. Build/test only, no Gradle wiring TDD requirement.

Risks/limits: no real Karoo/FIT adapter, decoder choice, replay engine, persistence, analytics or Android code. T01b/T01c and T02 remain unapproved. Full V01 equivalence, V02 downstream analysis and V03 lifecycle/window guarantees cannot be established by these contracts alone.

## Evidence

- JDK: Temurin 25+36; Gradle 9.1.0; Kotlin 2.3.20; target/release 17. Distribution SHA-256 matched the Gradle-hosted checksum and is pinned in wrapper properties.
- Behavioral red/green: cross-epoch duration initially failed with an assertion (1 test/1 failure), then passed after epoch/order checks. Added input/metadata validation cases: 7 tests/2 assertion failures before validation; after implementation, 7 tests/0 failures/0 errors.
- Actual final command: `GRADLE_USER_HOME="$PWD/.gradle-user-home" ./gradlew :core:check --console=plain` → BUILD SUCCESSFUL. Gradle's local socket requires sandbox escalation in this environment. No Android/device tests run.
- `git diff --check` passed; 52 local Markdown link targets exist; skill frontmatter name/description and T01a R/V references checked. Installed skill-creator `quick_validate.py` passed for all four project skills. Main source imports only Java time/UUID; runtime dependencies are Kotlin/JDK only.
- Canonical input v1 and build are delivered. V01/V02/V03 are covered only at the contract boundary described above, not full scenario acceptance; V13 device budgets remain untested. Parent T01 remains incomplete until separately approved T01b/T01c evidence exists.
