# Dependency intake

Maven Central is the source for Maven artifacts, matching the existing DendroDocs publication constraint.
Official GitHub Actions are pinned to verified commit SHAs. Maven Wrapper downloads are checksum pinned.
Dependencies execute build code and are versioned explicitly; Dependabot monitors Maven and action updates.

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
Dependabot cannot see these embedded versions or the filtered analyzer version property automatically.
Maintainers must rerun this audit when changing the analyzer version.

Before a production release, review the embedded analyzer components and current advisories,
Windows/Linux CI, signing credentials, Central namespace ownership, and the staged bundle.
A process boundary isolates Java libraries; it does not isolate filesystem or network access.

CI accepts fork pull requests with read-only permissions and no release credentials.
Central staging is a manual workflow on the default branch, guarded by a `maven-central` environment.
Secrets are supplied only to signing and staging steps. Automatic publication is disabled.
