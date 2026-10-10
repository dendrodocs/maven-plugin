# Maven 3.10 upgrade review — 2026-10-10

Reviewed Dependabot PRs [#1](https://github.com/dendrodocs/maven-plugin/pull/1) and [#2](https://github.com/dendrodocs/maven-plugin/pull/2) together with main commit 1f28cd6, including selectable-analyzer behavior.

- Official Maven Central distribution SHA-256: `1f6d9909266510f039f59aa0e13dcd2c66da85f043e56276e41f2918f8bddaff`, verified from the downloaded Maven 3.10.0 ZIP.
- PR #1's Windows wrapper script changes were line endings only. Normalize its Git representation according to the existing attributes; Windows checkout remains CRLF.
- Maven API/core dependencies remain provided. Maven supplies their runtime implementations.
- Apache's [release notes](https://maven.apache.org/docs/3.10.0/release-notes.html) identify Resolver 2.x, SLF4J 2.x, stricter validation, and credential scoping changes.
- The supported minimum remains Maven 3.9.11; only the wrapper distribution and compile-time Maven APIs move to 3.10.0.

Local toolchain: Windows, Eclipse Temurin JDK 21.0.4.

| Maven runtime | Unit tests passed | Integration fixtures passed | Failures / errors / skipped |
| --- | --- | --- | --- |
| 3.10.0 (wrapper) | 28 | 6 | 0 / 0 / 0 |
| 3.9.11 (baseline distribution) | 28 | 6 | 0 / 0 / 0 |

Both commands run `--batch-mode --no-transfer-progress -Dmaven.repo.local=<local-cache> verify` against the combined changes. Integration builds use the corresponding Maven runtime.

CI now exercises both Maven versions on Windows and Linux with JDK 21, preserving checksum pins and read-only permissions. Existing PR CI passed both platforms before merge; follow-up CI supplies combined-change platform evidence.

This evidence covers adapter compatibility. It does not establish exhaustive Maven version compatibility, private-repository credentials, signing, or Central publication. The default analyzer's separately recorded advisory findings remain a release blocker.
