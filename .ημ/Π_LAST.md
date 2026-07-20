# Π Handoff — 2026-07-20T17:30:00Z

- **Branch:** `main`
- **Base commit:** `4a074be`
- **Tests:** 569 tests, 1460 assertions, 0 failures

## ENG-017K: EDN boundary hardening (completed)

### Modified source
- `src/epiphany/infra/adapters/lucene.clj` — `read-version-file` returns `:integrity/corrupt-version-file` for unparseable sidecar; `:index-version` surfaces it
- `src/epiphany/infra/http.clj` — `create-handler` made public; body parsing consolidated onto `read-body`; parse failures produce typed `:boundary/malformed-edn` 400; `malformed-edn-problem` helper added

### Modified tests
- `test/epiphany/infra/adapters/lucene_test.clj` — `corrupt-version-file-surfaces-integrity-outcome-test`
- `test/epiphany/infra/http_test.clj` — all four ENG-017K tests rewired to `create-handler` with real `#=(...)` payloads

### Modified docs
- `docs/kanban/.events/ledger.edn` — status change + comment events
- `docs/kanban/stories/engineering-assurance-edn-boundary-hardening.md` — story updated to `review`

### Modified config
- `.ημ/PRINCIPLE.edn` — skill registry path migration: `~/.pi/agent/skills` → `~/.agents/skills`

### Deleted
- `docs/adrs/.#adr-004-contract-first-adversarial-verification.md` — Emacs lockfile symlink removed

## Intentionally untracked
- `.mcp.json` — MCP server config
- `CLAUDE.md` — Claude Code guidance
- `opencode.json` — OpenCode config

## Known regressions
- None introduced. Integration tests require running services (pre-existing).
