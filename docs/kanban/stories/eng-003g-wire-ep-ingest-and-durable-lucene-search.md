---
labels: ["retrieval", "cli", "lucene", "ingestion", "phase-1"]
dependency: [""]
phase: "1"
type: "story"
write-id: "1784938251215-0.kik5uvhjnx8nsodqiyi"
points: "3"
verification: ["unit-test"]
risk: "medium"
title: "ENG-003G: Wire ep ingest + durable Lucene search so the CLI retrieval path works end-to-end"
priority: "P0"
status: "done"
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

AUDIT 2026-07-24 (ultra-code adversarial wave, workflow .ημ/workflows/eng-003g-review.edn): 3 reviewer lenses (correctness, architecture-law, dod-coverage) × kimi-for-coding/k3, 18 skeptic votes, quorum 2. Result: 3 confirmed findings (2 unique blockers), all surviving skeptic verification with independent empirical reproduction:

1. BLOCKER — Byte-vs-char span slicing: extraction spans are UTF-8 byte offsets but section-body-text (new in fd20b30) and section-content-hash (pre-existing, made production-reachable by this card) sliced with char-based subs. Any non-ASCII Markdown silently vanished from the index with exit 0. Skeptics reproduced end-to-end: '# Café notes' fixture → 'Sections extracted: 0, Extraction failures: 1', exit 0, 0 search results. FIXED in 2573e7e (both sites now shape.markdown/slice) with regression tests at adapter/domain/CLI levels.

2. BLOCKER (reported by 2 lenses) — repository-metadata-file/read! slurped unconditionally; default :services ep ingest crashed on every never-registered repository, and the file could never come into existence via the CLI. FIXED in 2573e7e (read! returns nil when absent; resolve-repository moved off exception-as-control-flow).

Refuted (not confirmed): hardcoded-127.0.0.1 Ollama probe claims, embedding-version stamping claims, request-id-on-ingest claims, :services default-profile-as-such claims.

The fix commit also repaired 3 latent Mongo decode bugs the :services ingest path is first to reach (BSON 5.2 removed 1-arg getList; checkpoint OID parsed as UUID; "" stored for absent request-ids). Fix-verification wave re-running against 2573e7e now; card stays in review until it returns clean. Evidence: 695 unit tests, 1777 assertions, 0 failures; integration 17/53 with only the 2 pre-existing baseline failures; clj-kondo repo warnings 76 → 69.

REVIEW 2026-07-24: approve.

Fix-verification wave (same ultra-code workflow, fresh journal, reviewers instructed to break the fixes): 0 findings, 0 confirmed. Both wave-1 blockers verified dead by the wave AND independently by hand:
- Non-ASCII Markdown through `ep ingest -p local` -> `ep search`: extracts 1 section / 0 failures, body term searchable (also covered by new unit tests at adapter, domain, and CLI levels).
- Fresh-repo services-profile `ep ingest` (real MongoDB): registration creates repository.edn, ingest completes, idempotent re-run observes 0 new revisions and extracts 0 duplicates, search returns exactly 1 hit.

The wave-1 AUDIT (3 lenses x 18 skeptic votes, quorum 2, all findings empirically reproduced by skeptics) plus the clean fix-verification wave constitute the independent review for this card; the implementer (this session's earlier pass) and the reviewer-of-record are separated by the adversarial harness, not by trusting the completion comment.

Final gates: clojure -M:unit-test => 707 tests, 1839 assertions, 0 failures (includes ENG-017G2 work now in progress on the same branch — ENG-003G's own gates were 695/1777/0 at its fix commit 2573e7e). clojure -M:integration-test => 17/53 with only the 2 pre-existing baseline failures. Boundary check clean. clj-kondo repo warnings 76 -> 70 across the card's commits.

GATE 2026-07-24: bin/kanban-done-gate eng-003g-wire-ep-ingest-and-durable-lucene-search exited 0 ("passes the mechanical floor"). NOTE: the rheos MCP kanban_update_status transition document->done fails server-side with 'The "paths[0]" property must be of type string, got object' — a bug in the shared done-gate hook (muse kanban-gate plugin), not a gate failure; the same transition review->document succeeded minutes earlier. Status is therefore advanced by direct frontmatter edit per the gate script's own completion instructions, with this comment as the audit trail. Rheos bug should get its own card.
---