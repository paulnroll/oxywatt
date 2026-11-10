# OxyWatt

Open Cycling Performance Analytics.

Offline-first cycling analytics for Hammerhead Karoo 3, with a device-independent Kotlin calculation engine. The intended product derives power observations and cautiously estimates FTP and VO₂max from ordinary rides, retaining explainable history and trends.

**Status: JVM contracts and replay fixtures are in place. The T02a diagnostic host installs and launches on Karoo 3.**

Start with [the review packet](docs/spdd/review.md), [REASONS Canvas](docs/spdd/reasons-canvas.md), and [implementation plan](docs/spdd/implementation-plan.md).

## Documentation map

- [Product requirements](docs/product/requirements.md) and [roadmap](docs/product/roadmap.md)
- [SDK feasibility](docs/discovery/karoo-sdk-feasibility.md), [scientific research](docs/discovery/scientific-research.md), [risks](docs/discovery/risk-assessment.md)
- [Domain model](docs/architecture/domain-model.md), [context](docs/architecture/context.md), [boundaries](docs/architecture/bounded-contexts.md), [modules](docs/architecture/module-structure.md), [flow](docs/architecture/data-flow.md)
- [Power curve](docs/algorithms/power-curve.md), [FTP](docs/algorithms/ftp-estimation.md), [VO₂max](docs/algorithms/vo2max-estimation.md), [validation](docs/algorithms/validation-strategy.md)
- [Decisions](docs/spdd/decision-log.md), [agent workflow](AGENTS.md)

The first milestone ends at architectural review. Production code requires explicit approval of a named slice. Scientific validity and device reliability are not established by this documentation. The owner approved work through T02b. Later packages require named approval.

Personal FIT files and local measurement databases must stay out of Git. The open-source license remains an owner decision; no project license has yet been granted.

## JVM core checks

Use JDK 25 and run `./gradlew :core:check --console=plain` (Windows: `gradlew.bat :core:check`). The wrapper pins Gradle 9.1.0 with a SHA-256 checksum; Kotlin is pinned to 2.3.20, targeting JVM/release 17. First run downloads Gradle and Maven dependencies. The default build includes only `:core`. The [T02a diagnostic task](docs/spdd/tasks/t02a-diagnostic-host.md) explains how to build and install the opt-in Android host. The [T02b probe task](docs/spdd/tasks/t02b-karoo-probe.md) records the first ride-state checks on the device. Tests establish contract behavior, not adapter equivalence or device reliability. See [T01a evidence](docs/spdd/tasks/t01a-canonical-input.md).

## Code formatting and CI

Use `./gradlew spotlessApply` to format Kotlin and Gradle Kotlin DSL, and `./gradlew spotlessCheck` to verify formatting. Spotless 8.11.0 uses ktfmt 0.64 with Kotlin style (4-space block indentation). Generated files and local caches are outside the formatting targets.

`./gradlew check --console=plain` runs formatting and JVM core checks; `:core:check` also includes formatting. GitHub Actions runs an explicit formatting step followed by JVM core checks and `:core:build` (including the JAR) on every push and pull request, using Temurin JDK 25. CI reports formatting errors without modifying files and uploads available HTML/XML test reports, including on failure. See [formatting/CI task](docs/spdd/tasks/t01a-formatting-ci.md).
