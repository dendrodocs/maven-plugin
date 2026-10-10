# Release procedure

The repository is not yet published. The initial version is `0.1.0-SNAPSHOT`.
The release profile attaches source and Javadoc JARs, signs artifacts, and uploads a bundle
to Maven Central with `autoPublish=false`.

Before the first production release:

1. Resolve the recorded standalone analyzer advisory matches and pass `scripts/Audit-Analyzer.ps1 -FailOnFindings`.
2. Require successful Windows/Linux CI for the release commit.
3. Configure the `maven-central` GitHub environment with required reviewers and Central credentials.
4. Provide `MAVEN_USERNAME`, `MAVEN_PASSWORD`, `MAVEN_GPG_PRIVATE_KEY`, and `MAVEN_GPG_PASSPHRASE` as environment secrets.
5. Confirm that the Central account owns the `dev.dendrodocs` namespace and that the signing key is published.
6. Set a stable version in `pom.xml` in a reviewed commit; tag that commit and create release notes.
7. Run the manual **Stage Maven Central release** workflow on `main`, supplying the exact POM version.
8. Inspect the staged bundle in Central and publish it after approval.

The workflow validates the version rather than rewriting the POM at release time.
Do not release snapshots or reuse an already published version.
Credential setup, signing, Central staging, and publication need live verification; local test success is not evidence for them.
Repository branch protections and environment reviewers are administrator setup tasks, not established by a workflow YAML file.
