# Π Handoff — 2026-07-12T06:05:32Z

- **Branch:** `main`
- **Commit:** `19d3e3b`
- **Tag:** `pi/2026-07-12T06-05-32Z-19d3e3b`
- **Files:** 55 changed (643 insertions, 425 deletions)
- **Tests:** 186 tests, 569 assertions, 0 failures

## New source (this Π)
- `src/epiphany/domain/history_replacement.clj` — detect history replacement evidence
- `src/epiphany/domain/ingestion.clj` — ingestion run orchestration
- `src/epiphany/domain/section_extraction.clj` — section extraction from markdown
- `src/epiphany/infra/adapters/` — in-memory adapters for ports
- `src/epiphany/infra/profile.clj` — profile/config boundary
- `src/epiphany/infra/services.clj` — service wiring
- `src/epiphany/law/ports.clj` — port definitions

## Modified source
- `src/epiphany/infra/main.clj` — expanded CLI with register, status, history commands
- `src/epiphany/law/markdown.clj` — schema refinements
- `src/epiphany/law/observation.clj` — expanded observation schemas
- `src/epiphany/law/registry.clj` — registry schema updates

## Tests (new)
- `test/epiphany/domain/history_replacement_test.clj`
- `test/epiphany/domain/ingestion_test.clj`
- `test/epiphany/domain/section_extraction_test.clj`
- `test/epiphany/infra/adapters/` — adapter tests
- `test/epiphany/infra/profile_test.clj`
- `test/epiphany/infra/services_test.clj`

## Tests (modified)
- `test/epiphany/infra/main_test.clj` — expanded CLI tests
- `test/epiphany/integration_suite_test.clj` — integration updates

## Modified docs
- Kanban stories: US-00a through US-021 — status and dependency updates
- Epic-01 through Epic-06 — phase progression
- `BOARD-BREAKDOWN.md`, `AGENTS.md` — process updates

## Deleted
- `docs/research/phase-1-corpus-archaeology.md` — moved to `docs/designs/`

## Intentionally unstaged
- `.lsp/.cache/db.transit.json` — LSP runtime cache, not repo-relevant
