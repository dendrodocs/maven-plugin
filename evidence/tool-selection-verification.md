# Selectable-tool verification — 2026-10-10

Toolchain: Windows, Eclipse Temurin JDK 21.0.4, Maven Wrapper 3.9.11.

The focused regression tests first failed to compile against the Java-only implementation, then passed after adding tool selection and conditional Java validation.

Command: `mvnw.cmd --batch-mode --no-transfer-progress -Dmaven.repo.local=<local-cache> verify`.

- Unit tests: 28 passed, 0 failures, 0 errors, 0 skipped.
- Maven Invoker fixtures: 6 passed, 0 failures, 0 errors, 0 skipped.
- The alternate-tool reactor installs a standalone executable fixture, resolves it through the consumer's Maven session, and generates output for explicit Kotlin roots using XML and command-line selection.
- Deliberately malformed Java in those roots does not trigger Java preflight for the alternative tool.
- Default Java integration fixtures still verify real Java analysis, records, multiple roots, reactor dependencies, paths with spaces, skip behavior, and malformed-input failure.
- The default-artifact dependency audit still assesses 31 embedded components and matches advisories in 4 components; production staging remains blocked pending resolution and review.
- Derived architecture context reports current with no issues.

The alternative executable is a contract stub; this evidence does not establish Kotlin parsing, complete schema conformance, or an actual Kotlin tool's compatibility.
The default analyzer security findings, registry credentials, and signing remain separate release concerns.
