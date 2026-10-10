# Testing

Run `./mvnw verify` or `mvnw.cmd verify`.
The wrapper uses Maven 3.10.0; CI also runs the complete suite on the minimum supported Maven 3.9.11, on Windows and Linux with JDK 21.

Spotless checks plugin/test Java, valid integration fixture Java, and all fixture/root POMs during `validate`. Run `./mvnw spotless:apply` to apply the same Google Java format and semantic import ordering used by the Java tool. Deliberately malformed Java in the `invalid-source` and `skip` fixtures and the alternative-tool `Ignored.java` inputs is excluded; generated sources are outside the formatter's includes.

JaCoCo instruments the unit-test JVM and generates HTML/XML reports during `verify` in `target/site/jacoco`. Maven-generated HelpMojo code is excluded. The report does not include child analyzer JVMs or the separate Invoker consumer builds, and no coverage threshold is claimed. CI uploads the reports for each platform and Maven version.

Unit tests cover source selection, duplicate detection, malformed input, fresh-output checks,
previous-output preservation, compact JSON, skip behavior, temporary classpath JARs,
tool coordinate validation, conditional Java preflight, interruption, child-process exit diagnostics, missing analyzer output, omitted top-level types, and timeouts.

Invoker projects exercise actual Maven plugin injection and descriptors, the released analyzer,
Java records, multiple roots, paths containing spaces, reactor classpath resolution with `-T 2`,
the skip property, and malformed Java with an expected failed build.
An alternative executable fixture tests XML and command-line artifact selection, JVM launch, classpath arguments, and Kotlin source-root configuration without Java preflight. This is an adapter contract stub, not a Kotlin parser or a full schema conformance test.

Tests prove adapter behavior against the pinned analyzer. They do not prove complete Java language
coverage, complete type resolution, renderer correctness, registry credentials, or signing.
Local evidence is recorded in [evidence](../../evidence/README.md); remote CI results provide platform evidence.

[CI](../ci.md) reports test and coverage counts in job summaries, archives detailed evidence, and runs compiled-adapter CodeQL analysis. CodeQL and coverage reports have distinct scopes; neither assesses the standalone analyzer's embedded libraries.
