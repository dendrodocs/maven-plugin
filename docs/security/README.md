# Dependency intake

Maven Central is the source for Maven artifacts, matching the existing DendroDocs publication constraint.
Official GitHub Actions are pinned to verified commit SHAs. Maven Wrapper downloads are checksum pinned.
Dependencies execute build code and are versioned explicitly; Dependabot monitors Maven and action updates.

The plugin compiles against Maven 3.10.0 APIs and uses Maven's own APIs and injection at runtime, with a tested minimum of Maven 3.9.11. Maven 3.10 changes Resolver and logging dependencies; compatibility builds exercise both generations. The wrapper distribution remains checksum pinned from approved Maven Central. Staying on 3.9.11 was the alternative; adopting the released update adds upstream resolver and credential hardening while baseline CI guards consumer compatibility. See [Apache's release notes](https://maven.apache.org/docs/3.10.0/release-notes.html).

The plugin uses Maven APIs and injection as provided dependencies, Jackson for JSON merging,
JUnit for tests, and Apache Maven build plugins. Alternatives were hand-written JSON manipulation
and a custom plugin harness; these increase maintenance burden without improving the contract.

The analyzer artifact is resolved without its transitive dependency graph and runs as `java -jar`.
Its shaded libraries are absent from the Maven plugin realm. Its own embedded dependencies still
require a separate security assessment; updating the plugin's Jackson version does not update the analyzer.
Dependency tools that only inspect the plugin POM cannot certify that standalone analyzer JAR.

The initial OSV scan found advisory matches in bundled Jackson core/databind,
Commons BeanUtils, and jsoup; see `evidence/analyzer-dependency-audit.json`.
The scan covers Maven metadata in a minimized JAR and does not establish exploit reachability.
`scripts/Audit-Analyzer.ps1` refreshes the scan; `-FailOnFindings` blocks Central staging.
Dependabot cannot see these embedded versions or the filtered default analyzer coordinates automatically.
Maintainers must rerun this audit when changing the analyzer artifact. Pass -ToolArtifact and, if necessary, -ArtifactPath to assess the same alternative JAR selected in a consumer build; CI and release gating assess the embedded default tool.

Spotless 3.10.4 and JaCoCo 0.8.15 are pinned build tools from approved Maven Central, monitored by Dependabot. The formatter configuration follows the current [Java tool POM](https://github.com/dendrodocs/java-tool/blob/main/pom.xml), including pinned google-java-format 1.23.0; POM sorting uses the library default pinned by the Spotless release. Spotless executes formatters during checks and writes source only when explicitly applying changes. JaCoCo instruments the unit-test JVM; these tools do not enter the consumer plugin's runtime dependency graph. Manual formatting and unreported coverage were the alternatives; automatic checks provide repeatable enforcement and reviewable evidence.

Before a production release, review the embedded analyzer components and current advisories,
Windows/Linux CI, signing credentials, Central namespace ownership, and the staged bundle.
A process boundary isolates Java libraries; it does not isolate filesystem or network access.

CI accepts fork pull requests with read-only permissions and no release credentials.
Central staging is a manual workflow on the default branch, guarded by a `maven-central` environment.
Secrets are supplied only to signing and staging steps. Automatic publication is disabled.
