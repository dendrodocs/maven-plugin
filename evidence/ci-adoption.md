# CI adoption — 2026-10-10

Inspected the current remote Java-tool workflows, rather than the older local checkout:

| Source | Git blob |
| --- | --- |
| Continuous.yml | ab1f7f0c9292cb4dc8febdb068b8afb0d90d9383 |
| Release.yml | 09effe27b0f5afd05e3d3258884a4e6ec35aea19 |
| codeql.yml | 8609a04c1c13c50a723df9049c7259876e06ffef |

Adopted dependency caching, downloadable builds, Java CodeQL analysis, and its Monday schedule. Applied the GitHub workflow best-practices skill: immutable action references, explicit runners, least-privilege jobs, bounded execution, concurrency, secret scoping, and explicit fork behavior. Existing guarded Central staging replaces the Java-tool patterns that expose a signing passphrase to generic builds, permit package writes during continuous CI, or rewrite a release POM from an event tag.

Pinned CodeQL v4.38.3 to 24c54180a607b1449ed407dd24f251e4e9147c8d, verified against its official tag. Initialization uses the linked bundle; a manual Maven build compiles the main adapter without consumer fixtures or tests. Official action metadata was checked for every new input, including cache-read-only and CodeQL upload controls.

Local actionlint 1.7.12 passed all three workflows; shellcheck was unavailable and disabled explicitly. The official Windows archive checksum was verified: 6e7241b51e6817ea6a047693d8e6fed13b31819c9a0dd6c5a726e1592d22f6e9. No lint executable is checked into the repository.

The summary script was checked against real reports (28 unit tests and six successful fixtures; 90.2% instruction, 85.7% branch, 88.4% line coverage), missing-report inputs, and synthetic failure/skipped/zero-coverage/audit inputs. XML parsing ignores document types and disables external entity resolution; the real JaCoCo report includes a document type. No missing report is represented as a pass.

Remote results and security-alert status must be verified for the committed revision. Local reporting checks do not prove GitHub token settings, fork behavior, signing credentials, or Central staging.

The exact CodeQL Maven command also passed locally on JDK 21.0.4 and Maven 3.10.0: clean package with maven.test.skip, invoker.skip, and jacoco.skip enabled. This verifies compilation, not a local CodeQL query run.

GitHub default CodeQL setup was found configured for actions and java-kotlin with weekly scanning. The explicit workflow preserves both languages: manual compiled Java analysis and no-build Actions analysis. Default setup is replaced to avoid overlapping configurations; the workflow supplies all action pins and trust boundaries.
