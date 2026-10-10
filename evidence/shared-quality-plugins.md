# Shared quality plugins — 2026-10-10

Reference: current [Java tool POM](https://github.com/dendrodocs/java-tool/blob/main/pom.xml), blob 251c510886bc469f1b51ad32bb9d930349c61ee2. The local Java-tool checkout predates its Spotless configuration.

Adopted Google Java format 1.23.0, GOOGLE style, long-string reflow, unchanged Javadoc formatting, semantic import ordering, and the same SortPom ordering/settings.
Spotless 3.10.4 and JaCoCo 0.8.15 use released, pinned artifacts from Maven Central. Existing compiler, Enforcer, release-source/Javadoc/signing, and guarded Central publication configuration remains integrated with the new checks.

Spotless check first failed on the unformatted Java/POM sources. After applying the rules, validate and verify pass. Formatting covers plugin/test Java, valid integration fixture Java including extra roots, and root/fixture POMs. The deliberately invalid Java inputs in the skip, invalid-source, and alternative-tool fixtures remain byte-for-byte unchanged in Git.

Toolchain: Windows, Eclipse Temurin JDK 21.0.4.

| Maven runtime | Spotless check | Unit tests passed | Integration fixtures passed | Failures / errors / skipped |
| --- | --- | --- | --- | --- |
| 3.10.0 | Pass | 28 | 6 | 0 / 0 / 0 |
| 3.9.11 | Pass | 28 | 6 | 0 / 0 / 0 |

Commands: `spotless:apply verify` with the corresponding Maven runtime and a local dependency cache.

JaCoCo report covers two adapter classes and excludes generated HelpMojo code:

| Counter | Covered | Missed | Percent |
| --- | --- | --- | --- |
| Instructions | 955 | 104 | 90.2% |
| Branches | 108 | 18 | 85.7% |
| Lines | 199 | 26 | 88.4% |
| Methods | 21 | 1 | 95.5% |

HTML/XML output is generated under `target/site/jacoco` and included in CI artifacts.
These counters measure the unit-test JVM, not the separate Invoker builds or analyzer JVMs. No coverage threshold or language-analysis coverage is claimed.
