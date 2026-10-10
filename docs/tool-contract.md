# Standalone analyzer contract

The Java tool `dev.dendrodocs:tool:0.1.0` is the default. A future Kotlin tool or another JVM analyzer can be selected without changing the Maven goal if it implements this contract.

## Artifact and runtime

Configure `toolArtifact` or `-Ddendrodocs.toolArtifact=groupId:artifactId:version`.
Supported coordinate forms are `groupId:artifactId:version`, `groupId:artifactId:jar:version`, and `groupId:artifactId:jar:classifier:version`. Use a concrete version; ranges, LATEST, RELEASE, empty fields, and non-JAR artifacts fail configuration validation.

The artifact must be an executable standalone JAR, including its runtime dependencies.
Maven Resolver uses the consumer's plugin repositories, mirrors, credentials, and offline cache; the adapter does not resolve the tool's transitive dependencies.
The child JVM uses the same Java installation as Maven. The selected tool must support that runtime.

## Invocation

For each non-overlapping main source root, the adapter invokes:

```text
java -jar <tool.jar> --project <source-root> --output <fresh-output.json> [--classpath <jar-paths>]
```

Arguments are passed individually without a shell. The classpath is a single argument containing JAR paths joined by the platform path separator. Compiled class directories become temporary class-only JARs; ordinary dependency JARs pass through. The tool must accept an omitted classpath when no entries exist.

Each invocation analyzes its source root recursively. It must reject invalid source in its own language, validate the shared [DendroDocs intermediary JSON schema](https://github.com/DendroDocs/schema), and write a fresh UTF-8 JSON array to the supplied output path. Every type needs a nonblank textual `FullName`. Exit zero signals success; invalid input, analysis failure, or schema rejection should exit nonzero with actionable diagnostics.

The adapter additionally rejects missing fresh output, invalid JSON, trailing content, missing names, duplicate names, nonzero exits, and timeouts. It merges roots and replaces the destination only after all succeed. It does not independently validate the complete shared schema.

## Source and validation selection

Registered Maven main source roots are used by default. Set `sourceDirectories` explicitly for language roots not registered by that language's Maven integration; paths are module-relative. Bind the goal after language compilation when generated sources or compiled dependencies are required.

Java syntax and declaration-completeness checks are automatically enabled for artifacts with group `dev.dendrodocs` and artifact `tool`, including a different version. For other artifact identities, the selected tool owns language validation.
Set `validateJava` or `-Ddendrodocs.validateJava=true|false` to override that choice. Java preflight supports UTF-8 sources through Java 21, parses without annotation processing, and requires top-level Java declarations to appear in the result. Disabling it also disables that Java declaration check.

## Compatibility evidence

The alternative-tool integration fixture verifies artifact resolution, argument handling, child-JVM execution, XML/property binding, explicit Kotlin source roots, and conditional Java checks.
Its executable is a contract stub. It does not implement Kotlin analysis or prove full schema conformance. An actual alternative analyzer must supply its own language and schema compatibility tests and dependency assessment before adoption.
