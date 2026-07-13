# Π Handoff — 2026-07-13T04:00:00Z

- **Branch:** `main`
- **Base commit:** `40aaac4`
- **Tests:** 583 tests, 1493 assertions, 2 failures (integration tests requiring services)

## New source files
- `src/epiphany/application/validation.clj` — validation application service
- `src/epiphany/law/operations.clj` — operation schemas and validators
- `test/epiphany/application/validation_test.clj` — validation service tests
- `test/epiphany/law/operations_test.clj` — operation schema tests
- `test/epiphany/law_suite/observations_laws.clj` — observation law definitions
- `test/epiphany/law_suite/observations_test.clj` — observation law tests

## Modified source files
- `src/epiphany/application/registration.clj` — registration service updates
- `src/epiphany/domain/evidence.clj` — evidence domain changes
- `src/epiphany/infra/adapters/in_memory.clj` — in-memory adapter enhancements
- `src/epiphany/infra/adapters/lucene.clj` — Lucene adapter updates
- `src/epiphany/infra/git.clj` — Git infrastructure changes
- `src/epiphany/infra/http.clj` — HTTP adapter updates
- `src/epiphany/infra/main.clj` — main infrastructure changes
- `src/epiphany/infra/profile.clj` — profile infrastructure updates
- `deps.edn` — dependency updates

## Modified docs
- `docs/kanban/AGENTS.md` — board contract updates
- `docs/kanban/stories/engineering-assurance-*.md` — 7 assurance stories updated
- `docs/kanban/stories/story-*.md` — 8 story files updated with status/metadata changes
- `docs/process/review-and-acceptance.md` — process documentation updates
- `receipts.edn` — append-only receipt log updated

## New docs
- `docs/kanban/stories/engineering-assurance-test-coverage-reporting.md` — new assurance story

## Agent/tool configuration (new, not in repo)
- `.claude/settings.json` — Claude Code hooks for kanban gates
- `.claude/hooks/kanban-mcp-status-gate.sh` — MCP status transition guard
- `.claude/hooks/kanban-direct-edit-guard.sh` — direct edit guard
- `.mcp.json` — MCP server configuration
- `CLAUDE.md` — Claude Code guidance
- `opencode.json` — OpenCode configuration
- `bin/kanban-done-gate` — kanban done gate script

## Intentionally unstaged/untracked
- `docs/inbox/.#2026.07.12.10.17.57.md` — Emacs lockfile symlink, not committed
- `.claude/settings.local.json` — Local agent settings, not committed
- `.mcp.json` — MCP server config, not committed
- `CLAUDE.md` — Claude Code guidance, not committed
- `opencode.json` — OpenCode config, not committed
- `.clj-kondo/imports/hiccup/` — Linter cache, not committed

## Known regressions (pre-existing, not introduced this Π)
- Integration tests require running services (MongoDB, Ollama) — 2 failures
