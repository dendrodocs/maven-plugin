# Testing

Run `./mvnw verify` or `mvnw.cmd verify`.
The wrapper uses Maven 3.10.0; CI also runs the complete suite on the minimum supported Maven 3.9.11, on Windows and Linux with JDK 21.

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
