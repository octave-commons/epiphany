---
labels: ["retrieval", "cli", "lucene", "ingestion", "phase-1"]
dependency: [""]
phase: "1"
type: "story"
write-id: "1784933196328-0.ud0rv2yiapf8vwo80gu"
points: "3"
verification: ["unit-test"]
risk: "medium"
title: "ENG-003G: Wire ep ingest + durable Lucene search so the CLI retrieval path works end-to-end"
priority: "P0"
status: "review"
id: "ae0dc804-1cfa-4dd5-b3e9-99a4f4ec9885"
epic: "epic-03-retrieval-substrate"
design: "docs/designs/phase-1-corpus-archaeology.md"
uuid: "ae0dc804-1cfa-4dd5-b3e9-99a4f4ec9885"
created_at: "2026-07-24T00:00:00.000Z"
---

# ENG-003G: Wire ep ingest + durable Lucene search so the CLI retrieval path works end-to-end

## Intent

Stories 03a–03f delivered tested components (on-disk Lucene adapter, Ollama
embeddings adapter, hybrid-search domain, extraction projection runner), but
no production path wires them together:

- `ep search` built a **fresh in-memory adapter per JVM run**
  (`infra/main.clj` `make-search-adapters`), so the index was always empty
  and every query returned "0 results"; `--profile services` threw
  UNAVAILABLE.
- No CLI command ran ingestion, revision-at-path projection, or the
  extraction projection — `run-ingestion` and `run-extraction-projection`
  were only reachable from tests.
- The durable Lucene adapter (`infra/adapters/lucene.clj`) had no caller
  in `src/`.
- `ep serve` wired no `:index` / `:embeddings` ports, so workbench search
  could not work either.

## Scope

1. New `ep ingest <path>` subcommand:
   - registers the repository (idempotent) and resolves its resource-id
   - runs commit-graph ingestion (`domain.ingestion/run-ingestion`)
   - projects revision-at-path observations per commit (markdown selection
     + parent-tree evidence, deduplicated against already-recorded
     observations)
   - runs the section-extraction projection into a **durable on-disk
     Lucene index** (`--index-dir`, default `~/.epiphany/index`)
   - `--embed` flag additionally embeds extracted sections via Ollama and
     indexes KNN vectors
   - observations persist via the selected profile (`:local` in-memory,
     `:services` MongoDB); the Lucene index is durable in both
2. `ep search` queries the durable Lucene index (`--index-dir`, same
   default) for lexical; semantic/hybrid embed the query via Ollama and
   fail explicitly UNAVAILABLE when Ollama is unreachable — never a silent
   fallback.
3. `ep serve` gains `:index` + `:embeddings` ports backed by the same
   durable Lucene dir and Ollama so workbench search works.

## Acceptance criteria

- `ep ingest <repo> --index-dir <tmp>` followed by
  `ep search "<heading text>" --mode lexical --index-dir <tmp>` returns the
  ingested section (demonstrated in a unit test over a fixture Git repo).
- Re-running `ep ingest` does not duplicate revision-at-path observations
  or extraction records (idempotency by observation identity keys).
- `ep search --mode semantic` with Ollama down exits 1 with an explicit
  UNAVAILABLE-style error, not a fallback to lexical.
- `clojure -M:unit-test` stays green; zero new clj-kondo warnings.

## Out of scope

- Embedding projection checkpoints/resumability (extraction projection
  already checkpoints; embeddings are re-derivable).
- Per-resource index partitioning (single shared corpus index dir is the
  phase-1 default; partitioning is a later ops card).

---
IMPLEMENTED 2026-07-24.

What changed:
- New `ep ingest <path>` subcommand (src/epiphany/infra/main.clj): registers (idempotent), runs commit-graph ingestion, projects revision-at-path observations (deduped against recorded identities), runs the section-extraction projection into the durable on-disk Lucene index (--index-dir, default ~/.epiphany/index). --embed adds Ollama embeddings + KNN vectors; fails UNAVAILABLE when Ollama is unreachable.
- `ep search` now reads the durable Lucene index for lexical; semantic/hybrid embed the query via Ollama and exit 1 with an explicit UNAVAILABLE-style error when Ollama is down — no silent fallback to lexical.
- `ep serve` (:local and :services) now wires :index + :embeddings ports from the same durable Lucene dir, so workbench search has a real index behind it.
- Latent bug fixed in the Lucene adapter (story-03a): section->doc looked up :section/body, which never exists — extraction records carry body-span offsets only, so body text was NEVER indexed and lexical search only matched headings+paths. extract-revision now passes the blob content alongside the (unchanged, schema-valid) observation; section->doc slices body text from the recorded spans. Regression test: search-finds-body-content-test.
- Bug fixed in my own first cut: tools.cli does not run :parse-fn on :default values, so --refs defaulted to the string "HEAD" and tripped the ingestion-run schema's [:vector] — caught by the law/ validation gateway doing its job.
- Test hygiene: search/parity tests no longer depend on ambient Ollama or an empty ~/.epiphany/index (hermetic --index-dir temp dirs, --mode lexical).
- :local ingest caveat: observations are in-memory, so a second :local ingest into the same index dir re-extracts and duplicates index docs. Use :services (MongoDB) for incremental ingestion; :local is one-shot/demo. Known limitation, documented in --help.
- Pre-existing warning cleanup: removed an empty when in run-serve shutdown hook; two unused bindings in extraction_projection.

Evidence: clojure -M:unit-test => 691 tests, 1766 assertions, 0 failures. clj-kondo on touched files: 0 warnings (repo baseline 76 -> 74). Manual vertical slice: fixture repo -> ep ingest -> ep search "identity" --mode lexical returns the notes.md section with score/commit. Integration suite: 2 failures present on the untouched baseline too (pre-existing, not from this change).
---