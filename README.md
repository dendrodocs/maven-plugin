# DendroDocs Maven Plugin

Generate DendroDocs intermediary JSON during Maven builds, using the Java tool by default.
Each module writes `target/dendrodocs.json` for downstream renderers.

This is an initial implementation under review. Version `0.1.0-SNAPSHOT` has not been published to Maven Central. Build and install it locally to try it.

## Requirements

- A JDK 21 or newer and Maven 3.9.11 or newer in the Maven 3 series.
- With the default Java tool: Java sources using Java 21 syntax or earlier, encoded as UTF-8.
- Alternative tools must run on Maven's JDK and implement the [standalone analyzer contract](docs/tool-contract.md).
- Maven Central access on the first run; cached dependencies and the analyzer allow offline use afterward.

The Maven Wrapper pins Maven 3.10.0. The supported minimum remains Maven 3.9.11.
CI verifies both versions on Windows and Linux with JDK 21.

## Usage

Install this checkout with `./mvnw install` (Windows: `mvnw.cmd install`), then add:

```xml
<plugin>
  <groupId>dev.dendrodocs</groupId>
  <artifactId>dendrodocs-maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <executions>
    <execution>
      <goals><goal>generate</goal></goals>
    </execution>
  </executions>
</plugin>
```

Run `mvn process-classes` or a later lifecycle phase. The default phase runs after compilation, including generated sources and compiled dependencies.

For an explicitly configured plugin, direct invocation is `mvn dendrodocs:generate`.
Run compilation first when references depend on generated or reactor classes.
The fully qualified goal is `dev.dendrodocs:dendrodocs-maven-plugin:0.1.0-SNAPSHOT:generate`.
Use `mvn dendrodocs:help -Ddetail=true -Dgoal=generate` for generated goal help.

## Configuration

| Parameter | Default | User property |
| --- | --- | --- |
| `toolArtifact` | `dev.dendrodocs:tool:0.1.0` | `dendrodocs.toolArtifact` |
| `validateJava` | `true` for `dev.dendrodocs:tool`, `false` for other tools | `dendrodocs.validateJava` |
| `outputFile` | `${project.build.directory}/dendrodocs.json` | `dendrodocs.outputFile` |
| `pretty` | `true` | `dendrodocs.pretty` |
| `skip` | `false` | `dendrodocs.skip` |
| `timeoutSeconds` | `120` per source root | `dendrodocs.timeoutSeconds` |
| `sourceDirectories` | Maven's registered main source roots | XML configuration only |

Paths supplied in configuration are resolved relative to the current module.
Missing default source roots and parent POM modules are skipped.
Missing explicitly configured roots fail the build.
Repeated or nested roots are analyzed once; duplicate type names across separate roots fail the build.
Test sources are outside the initial version's scope.

```xml
<configuration>
  <outputFile>${project.build.directory}/documentation/code.json</outputFile>
  <pretty>false</pretty>
  <timeoutSeconds>300</timeoutSeconds>
  <sourceDirectories>
    <sourceDirectory>src/main/java</sourceDirectory>
    <sourceDirectory>target/generated-sources/custom</sourceDirectory>
  </sourceDirectories>
</configuration>
```

The goal is safe for `mvn -T 2 process-classes`: each module has separate output and temporary files.
It generates one JSON file per module; it does not combine an entire reactor into a single file.

## Selecting another tool

Set `toolArtifact` to a standalone executable JAR's Maven coordinates. For example, this illustrates a future Kotlin tool; the artifact name is a placeholder:

```xml
<configuration>
  <toolArtifact>your.group:kotlin-tool:1.0.0</toolArtifact>
  <sourceDirectories>
    <sourceDirectory>src/main/kotlin</sourceDirectory>
  </sourceDirectories>
</configuration>
```

You can also use `-Ddendrodocs.toolArtifact=groupId:artifactId:version`.
A classifier is supported as `groupId:artifactId:jar:classifier:version`.
The tool must accept the common CLI and produce schema-valid DendroDocs JSON; see the [tool contract](docs/tool-contract.md).

Java syntax and omitted-declaration checks run automatically for `dev.dendrodocs:tool`.
Other tools handle their own language validation. Set `validateJava` explicitly to override this behavior.
The integration tests verify an alternative executable fixture; an actual Kotlin analyzer is future work.

## Failure behavior

The build fails on analyzer errors, timeouts, missing fresh output, invalid JSON, or duplicate types.
When Java validation is enabled, invalid Java syntax and omitted top-level Java types also fail the build.
The previous output survives a failed generation. A failed build must not publish that previous file.
Output is replaced only after every root succeeds, using an atomic move where the filesystem supports it.
Temporary files are removed after success and failure.

Selected tools are responsible for validating each root's JSON against the shared DendroDocs schema.
The default Java tool can emit `?` for unresolved types, an intentional analyzer fallback.
A child JVM isolates bundled analyzer libraries from Maven; it is not a security sandbox.

## Development and release

```sh
./mvnw verify
```

Spotless checks Java and POM formatting during validate. Apply formatting with `./mvnw spotless:apply` (Windows: `mvnw.cmd spotless:apply`). The rules follow the Java tool's Google Java format and POM sorting conventions.

Verification runs unit tests and real Maven integration builds. JaCoCo writes unit-test coverage to `target/site/jacoco/index.html` and `target/site/jacoco/jacoco.xml`; CI uploads these reports. See [testing](docs/testing/README.md), [release procedure](docs/releasing.md), and [dependency intake](docs/security/README.md).

## Architecture

Start at [arc42](docs/arc42/index.adoc). The compact derived context lives in [architecture memory](docs/architecture-memory.adoc) and `.agents/architecture-memory.yaml`.

```text
src/main/java/           Maven goal and analyzer process adapter
src/test/java/           Orchestration and process tests
src/it/                  Maven integration fixtures
docs/arc42/              Architecture source documents
docs/adr/                Decisions for initial review
docs/security/           Dependency and release controls
docs/testing/            Verification scope
evidence/                Recorded local verification
.github/workflows/      CI and manual Central staging
```

## Background and license

The student `ldj-maven-plugin` in [ISEP-LivingDocumentation](https://github.com/eNeRGy164/ISEP-LivingDocumentation) demonstrated the Maven integration.
This repository implements the DendroDocs adapter independently and reuses the published [Java analyzer](https://github.com/DendroDocs/java-tool).
The plugin is MIT licensed; the analyzer retains its own license and attribution.
