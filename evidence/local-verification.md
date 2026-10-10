# Local verification — 2026-10-10

Toolchain: Windows, Eclipse Temurin 21.0.4, Apache Maven 3.9.11 (download SHA-512 verified).

Command: `mvn --batch-mode --no-transfer-progress verify`.

- 21 unit tests passed; 0 failed, errored, or skipped.
- 5 Maven integration fixtures passed; 0 failed, errored, or skipped.
- Reactor log confirms Maven's MultiThreadedBuilder with 2 threads.
- Consumer type resolution, source merging, paths with spaces, skip, and malformed-source failure were executed against the published analyzer 0.1.0.
- Maven Wrapper 3.3.4 scripts were generated with a distribution SHA-256.
- The checksum-pinned Windows Wrapper completed `verify` successfully.
- A clean consumer build ran offline from the warmed cache and generated both class and record types.
- Generated goal help ran offline and exposed all user parameters.
- The live OSV report covered 31 embedded Maven components; the release gate was executed and rejected the four components with advisory matches.

The malformed-source fixture initially exposed an upstream analyzer false success. JDK syntax preflight now rejects it.
The analyzer audit found advisory matches in four embedded components; see the adjacent JSON report.
Reachability in the minimized JAR, signing, Central credentials, and publication have not been verified.
Remote CI supplies independent Windows/Linux evidence after pushing.
