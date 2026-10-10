# DendroDocs Maven Plugin

Build and maintain the Maven adapter for selectable DendroDocs analyzers, defaulting to the Java tool.

## Architecture guardrails

- Read `.agents/architecture-memory.yaml`, then `docs/arc42/` and linked ADRs before architecture changes. Source docs remain authoritative.
- Explain conflicts with the architecture and propose updates to the docs, code, or both.
- Keep language analysis and schema validation in the selected tool, defaulting to `DendroDocs/java-tool`. Run the standalone analyzer in a child JVM.
- Refresh derived architecture memory after architecture changes.
- Apply documented defaults to generated files and check them afterward.
- Validate behavior with `./mvnw verify` (Windows: `mvnw.cmd verify`); integration fixtures live in `src/it`.

## Documentation

Use US English, Markdown for usage guides, AsciiDoc for arc42, and PlantUML sources under `docs/arc42/images/<chapter>/`.
Keep unknowns and untested behavior explicit.

## Git

Use short imperative subjects in plain English and one logical concern per commit.
Prefer rebase over merge; use fixup and autosquash for branch corrections.
Do not leave fixup commits or squash history without explicit instruction.
See [.agents/instructions/git-commit-workflow.md](.agents/instructions/git-commit-workflow.md).
