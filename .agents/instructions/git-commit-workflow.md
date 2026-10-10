# Commit workflow

Inspect the diff and group changes by intent before staging. Keep one concern per commit.
Stage explicit files when other work is present. Use short imperative subjects.
Use `git commit --fixup <commit>` for corrections, then autosquash before handoff.
Prefer rebase over merge. Do not squash history or leave fixup commits without explicit instruction.
Commit-message enforcement is deferred; CI enforces executable behavior.
