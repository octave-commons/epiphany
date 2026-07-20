# Π Handoff — 2026-07-20T17:23:49Z

- **Branch:** `main`
- **Base commit:** `5a18710`
- **Tests:** 586 tests, 1489 assertions, 0 failures
- **Lint:** 0 errors, 92 warnings (all pre-existing)
- **Boundary check:** clean
- **Interop ratchet:** clean

## ENG-017H: Static architecture and interop boundary gates (completed)

### New source
- `tools/epiphany/static/boundary_check.clj` — layer-boundary enforcement (law/shape/domain/application/infra)
- `tools/epiphany/static/interop_inventory.clj` — Java interop ratchet with per-namespace baselines

### New tests
- `test/epiphany/static/boundary_check_test.clj` — 8 tests, 19 assertions
- `test/epiphany/static/interop_inventory_test.clj` — 9 tests, 10 assertions

### New config
- `.clj-kondo/config.edn` — lint baseline: `discouraged-var` on `clojure.core/read-string` (ENG-017K gate)
- `.splint.edn` — splint defaults, non-gating (deferred adoption)
- `reports/interop.edn` — 45-namespace interop baseline

### Modified source
- `deps.edn` — added `:lint`, `:boundary-check`, `:interop-inventory`, `:splint` aliases; `tools` path in test aliases
- `.github/workflows/test.yml` — added `static` CI job (lint → boundary → interop)

## ENG-017G: CLI/HTTP command parity (in progress — boundary hardening)

### Modified source
- `src/epiphany/infra/http.clj` — `wrap-exceptions` no longer leaks internal messages to clients (ENG-017G boundary hardening); added `max-search-limit` + `valid-limit?` guard on search handler

### Modified docs
- `docs/kanban/stories/engineering-assurance-interface-command-parity.md` — status → `in_progress`

## ENG-017K: EDN boundary hardening (re-verified → done)

### Modified docs
- `docs/kanban/stories/engineering-assurance-edn-boundary-hardening.md` — re-verification appended
- `docs/kanban/stories/engineering-assurance-static-boundary-interop-gate.md` — implementation notes appended
- `docs/kanban/.events/ledger.edn` — re-verification + ENG-017H progress events

### Receipts
- `receipts.edn` — previous Π receipt appended

## Intentionally untracked
- `.mcp.json` — MCP server config
- `CLAUDE.md` — Claude Code guidance
- `opencode.json` — OpenCode config
- `.clj-kondo/.cache/` — lint cache (generated)

## Known regressions
- None introduced. Integration tests require running services (pre-existing).
