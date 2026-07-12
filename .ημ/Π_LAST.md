# Π Handoff — 2026-07-12T14:37:01Z

- **Branch:** `main`
- **Base commit:** `1c9fc42`
- **Tests:** 528 tests, 1336 assertions, 9 failures (pre-existing regressions)

## Modified source
- `src/epiphany/application/registration.clj` — registration service returns observation-wrapped records
- `src/epiphany/infra/adapters/mongo.clj` — mongo adapter refinements
- `src/epiphany/infra/http.clj` — HTTP adapter updates
- `src/epiphany/infra/main.clj` — CLI updates

## Modified docs
- `PROCESS.md` — process documentation updates
- `STYLE.md` — style documentation updates
- `docs/process/kanban.md` — kanban process updates
- `receipts.edn` — append-only receipt log

## Moved docs (delete from docs/process/, add to docs/designs/)
- `epiphany-agent-ledgers.md`
- `epiphany-development-process.md`
- `epiphany-meta-workflow.md`

## New docs
- `docs/process/document-governance.md`
- `docs/process/glossary.md`
- `docs/process/research.md`

## Intentionally unstaged
- `.lsp/.cache/db.transit.json` — LSP runtime cache, not repo-relevant

## Known regressions (pre-existing, not introduced this Π)
- Registration service now returns observation-wrapped records (`:observation/*`, `:repository/*` keys) but 6 tests expect the old flat map shape
- HTTP register endpoint returns 500 (cascading from registration shape change)
- Profile bootstrap returns observation-wrapped record but test expects flat map
