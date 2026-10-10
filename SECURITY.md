# Security

Report security issues through [private vulnerability reporting](https://github.com/DendroDocs/maven-plugin/security/advisories/new) when enabled.
Do not include credentials or exploit payloads in public issues.

The initial snapshot is under review and has not been released.
The pinned standalone analyzer has embedded dependency advisory matches recorded in
[evidence](evidence/analyzer-dependency-audit.json). Production Central staging fails until those matches are resolved.
The audit covers embedded Maven metadata; absence of an advisory match does not establish absence of exploitable defects.
