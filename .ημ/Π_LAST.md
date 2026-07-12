# Π Handoff — 2026-07-12T08:40:42Z

- **Branch:** `main`
- **Base commit:** `0a99597`
- **Tests:** 488 tests, 1225 assertions, 0 failures

## New source (this Π)
- `src/epiphany/domain/benchmark.clj` — retrieval benchmark harness
- `src/epiphany/domain/boundary.clj` — path-repurpose boundary proposals
- `src/epiphany/domain/concept.clj` — concept curation and research questions
- `src/epiphany/domain/continuity.clj` — deterministic continuity features
- `src/epiphany/domain/diff.clj` — historical expression comparison
- `src/epiphany/domain/evidence.clj` — exact historical evidence reader
- `src/epiphany/domain/export.clj` — evidence packet export
- `src/epiphany/domain/extraction_projection.clj` — extraction as checkpointed projection
- `src/epiphany/domain/hybrid_search.clj` — lexical/semantic/hybrid search
- `src/epiphany/domain/inbox.clj` — review inbox service
- `src/epiphany/domain/lineage.clj` — candidate lineage links
- `src/epiphany/domain/lineage_trace.clj` — lineage chronology tracing
- `src/epiphany/domain/redundancy.clj` — redundancy and contradiction detection
- `src/epiphany/domain/research_gap.clj` — research gap surfacing
- `src/epiphany/domain/review.clj` — review decision events
- `src/epiphany/domain/status.clj` — cross-stage status query
- `src/epiphany/infra/http.clj` — reitit/ring HTTP API adapter

## Modified source
- `src/epiphany/infra/adapters/in_memory.clj` — expanded in-memory adapters
- `src/epiphany/infra/adapters/mongo.clj` — mongo adapter refinements
- `src/epiphany/infra/git.clj` — read-blob support
- `src/epiphany/infra/main.clj` — CLI expansion (search, status, show, diff, trace, inbox, export)
- `src/epiphany/law/ports.clj` — port definitions

## Tests (new)
- 17 new test files under `test/epiphany/domain/` and `test/epiphany/infra/`
- `docs/benchmarks/queries.edn` — 33-query benchmark set

## Tests (modified)
- `test/epiphany/infra/main_test.clj` — expanded CLI tests

## Modified docs
- Kanban stories: story-02c through story-20a — status and content updates
- Epic-03 — retrieval substrate updates
- `AGENTS.md`, `deps.edn`, `receipts.edn`

## Intentionally unstaged
- `.lsp/.cache/db.transit.json` — LSP runtime cache, not repo-relevant
