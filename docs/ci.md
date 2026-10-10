# Continuous integration

The [CI workflow](../.github/workflows/ci.yml) runs JDK 21 builds on Ubuntu 24.04 and Windows 2025, each with Maven 3.9.11 and 3.10.0. Every combination runs Spotless, unit tests, all six Invoker projects, and the embedded default-analyzer advisory audit. Advisory matches are reported during CI and block Central staging.

The current Java tool's [continuous workflow](https://github.com/dendrodocs/java-tool/blob/main/.github/workflows/Continuous.yml) supplied the Maven cache and downloadable-package patterns. Its [CodeQL workflow](https://github.com/dendrodocs/java-tool/blob/main/.github/workflows/codeql.yml) supplied Java security scanning and the weekly schedule. The Maven adapter uses a manual CodeQL build to analyze the actual compiled main sources and generated plugin help code, with the CodeQL bundle linked to the pinned action release.

Each build writes a GitHub job summary from Surefire, Invoker, JaCoCo, and audit reports through `scripts/Write-CiSummary.ps1`. Missing reports are labeled unavailable. Expected-failure integration scenarios count as successful fixtures when their expectations pass. Coverage describes the adapter unit-test JVM, excluding generated help code, Invoker builds, and child analyzer JVMs. GitHub summaries and archived HTML provide coverage evidence without requiring a Coveralls account or another reporting action.

Artifacts expire after 14 days:

| Artifact | Contents |
| --- | --- |
| verification-OS-maven-VERSION | Coverage HTML/XML, test XML, analyzer audit JSON, and integration logs; attempted even after failure |
| plugin-build-COMMIT | JAR and committed POM from the successful Ubuntu/Maven 3.10 job; an unsigned build for inspection |
| codeql-java | SARIF results from the compiled adapter analysis; attempted even after failure |

## Trust and cache boundaries

Actions are pinned to full commits, runners are explicit, credentials are not persisted by checkout, and jobs have concurrency controls and timeouts. The verification workflow has only contents-read permission and receives no release secrets.

Setup Java derives Maven dependency-cache keys from OS/architecture and the root/fixture POMs plus wrapper properties. The compatibility distribution is selected before computing keys, separating Maven baselines. Only dependency repositories and setup-java's wrapper/JDK caches are cached; settings, signing keys, workspaces, and generated reports are excluded. All pull requests restore caches without saving. Release staging deliberately uses a fresh dependency repository and no CI cache.

The [CodeQL workflow](../.github/workflows/codeql.yml) expands only its analysis job to contents-read and security-events-write for code-scanning upload. Fork PR tokens are reduced by GitHub; the workflow explicitly disables code-scanning and database upload for forks and retains SARIF as an artifact. It uses no release secrets and no package-write permission.

Signing and Central credentials are scoped to the manual main-branch [release workflow](../.github/workflows/release.yml), behind the maven-central environment. The committed version is checked without rewriting the POM. Central auto-publication is disabled. Environment reviewers and branch protections still require administrator verification; workflow files do not establish those settings.

## Verification

Run `./mvnw verify`. To inspect a summary locally, run:

```powershell
./scripts/Write-CiSummary.ps1 -SummaryPath ./target/ci-summary.md
```

Review all three workflow files with actionlint when changing their syntax. A successful CodeQL run proves that extraction and analysis completed; review code-scanning alerts separately. It does not assess libraries embedded in the external analyzer or prove the absence of vulnerabilities.
