# Testing

Run `./mvnw verify` or `mvnw.cmd verify`.

Unit tests cover source selection, duplicate detection, malformed input, fresh-output checks,
previous-output preservation, compact JSON, skip behavior, temporary classpath JARs,
interruption, child-process exit diagnostics, missing analyzer output, omitted top-level types, and timeouts.

Invoker projects exercise actual Maven plugin injection and descriptors, the released analyzer,
Java records, multiple roots, paths containing spaces, reactor classpath resolution with `-T 2`,
the skip property, and malformed Java with an expected failed build.

Tests prove adapter behavior against the pinned analyzer. They do not prove complete Java language
coverage, complete type resolution, renderer correctness, registry credentials, or signing.
Local evidence is recorded in [evidence](../../evidence/README.md); remote CI results provide platform evidence.
