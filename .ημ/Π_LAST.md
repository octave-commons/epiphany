# Π Handoff — 2026-07-12T15:50:00Z

- **Branch:** `main`
- **Base commit:** `62dbf10`
- **Tests:** 528 tests, 1336 assertions, 9 failures (pre-existing regressions)

## Modified docs
- `PROCESS.md` — process documentation updates (policy areas now reference established docs)
- `.gitignore` — added `.claude/` to prevent accidental commits

## New docs
- `docs/process/inbox.md` — inbox processing documentation
- `docs/process/notes.md` — notes processing documentation

## New skills
- `.agents/skills/inbox-synthesis/SKILL.md` — skill for processing inbox notes

## Inbox notes (25 files)
- `docs/inbox/2026.07.12.*.md` — timestamped inbox entries

## Intentionally unstaged/untracked
- `.lsp/.cache/db.transit.json` — LSP runtime cache, not repo-relevant
- `docs/inbox/.#2026.07.12.10.17.57.md` — Emacs lockfile symlink, not committed

## Known regressions (pre-existing, not introduced this Π)
- Registration service now returns observation-wrapped records (`:observation/*`, `:repository/*` keys) but 6 tests expect the old flat map shape
- HTTP register endpoint returns 500 (cascading from registration shape change)
- Profile bootstrap returns observation-wrapped record but test expects flat map
