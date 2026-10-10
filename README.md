# DendroDocs Maven Plugin

Generate DendroDocs intermediary JSON from a Java Maven project.
Each Java module writes `target/dendrodocs.json` for downstream renderers.

This is an initial implementation under review. Version `0.1.0-SNAPSHOT` has not been published to Maven Central. Build and install it locally to try it.

## Requirements

- A JDK 21 or newer and Maven 3.9.11 or newer in the Maven 3 series.
- Java sources using Java 21 syntax or earlier, encoded as UTF-8.
- Maven Central access on the first run; cached dependencies and the analyzer allow offline use afterward.

The Maven Wrapper pins Maven 3.9.11. Local verification currently uses JDK 21; CI covers Windows and Linux.

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

## Failure behavior

The build fails on invalid Java syntax, analyzer errors, timeouts, missing fresh output, invalid JSON, omitted top-level types, or duplicate types.
The previous output survives a failed generation. A failed build must not publish that previous file.
Output is replaced only after every root succeeds, using an atomic move where the filesystem supports it.
Temporary files are removed after success and failure.

The analyzer validates each root's JSON against its bundled DendroDocs schema.
Unresolved types can still appear as `?`, an intentional analyzer fallback.
A child JVM isolates bundled analyzer libraries from Maven; it is not a security sandbox.

## Development and release

```sh
./mvnw verify
```

Verification runs unit tests and real Maven integration builds. See [testing](docs/testing/README.md), [release procedure](docs/releasing.md), and [dependency intake](docs/security/README.md).

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
