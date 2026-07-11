# Epiphany

**Epiphany** is a local-first, Git-backed knowledge archaeology system.

It turns long-lived repositories of notes, documentation, and eventually code into a searchable, evidence-preserving research environment. Epiphany is not merely “chat with my files”: it is designed to recover how ideas emerged, changed, diverged, and were reconsidered across Git history.

> Find what you already thought, inspect the evidence, trace its evolution, and decide what to research next.

## The problem

Over years, useful thinking accumulates across Markdown notes, architecture documents, commit messages, code comments, and abandoned branches. Conventional file search finds exact words; generic RAG systems often retrieve plausible passages without making their provenance, historical status, or continuity claims inspectable.

Epiphany addresses questions such as:

- What have I already written about this?
- When did this idea first appear?
- What did it become over time?
- Which notes genuinely continue a line of thought, and which are only superficially similar?
- Where are the contradictions, unresolved questions, and research gaps?
- What evidence supports the next question I should investigate?

## Core principles

1. **Evidence first.** Git observations, model inferences, and human interpretations are different things.
2. **Git remains canonical for Git-originated history.** Search indexes, embeddings, caches, and projections are rebuildable.
3. **Similarity is not identity.** An embedding score, AST similarity, or LLM assertion never silently establishes continuity.
4. **Human acceptance is explicit.** A review decision is a durable interpretive event, not a mutation of source facts.
5. **Paths are preserved exactly.** No path transliteration, case-folding, Unicode normalization, or replacement of observed Git path strings.
6. **Local-first and source-aware.** A filesystem path belongs to the machine that evaluates it.
7. **Interfaces share one domain boundary.** CLI, TUI, REST, and browser UI invoke the same command/query services.

## Knowledge status

Every source expression and relationship is presented with an explicit status:

| Status | Meaning |
| --- | --- |
| `OBSERVED` | Directly observed from Git or another canonical source |
| `PROVISIONAL` | Proposed by retrieval, heuristics, or a model; not established |
| `ACCEPTED` | Explicitly accepted through a user review decision |
| `REJECTED` | Explicitly rejected; retained for audit but hidden by default |
| `STALE` | Derived from an outdated extractor, model, or indexing policy |
| `UNAVAILABLE` | The source object or required service cannot currently be read |

## Phase 1

Phase 1 is deliberately narrow: **local Git repositories and Markdown history**.

The core loop is:

```text
Register a repository
  -> observe Git and Markdown history
  -> extract revision-scoped sections and source spans
  -> search historical and current material
  -> inspect exact evidence
  -> compare expressions
  -> trace lineage over time
  -> review candidate relationships
  -> create a research question
  -> export an evidence packet
```

### Phase 1 capabilities

- Register local Git repositories, including repositories moved on disk
- Preserve minimal repository identity in Git-local metadata
- Recognize shared commit history across repository instances
- Traverse historical Markdown without checking out every revision
- Extract front matter, heading hierarchy, blocks, sections, byte/line spans, and source metadata
- Search using lexical, semantic, and hybrid retrieval
- Compare historical expressions and revisions
- Model same-path continuity separately from semantic continuity
- Propose, review, accept, reject, or relabel lineage candidates
- Create user-curated concepts and evidence-backed research questions
- Export reproducible evidence packets
- Observe ingestion/projection health and rebuild derived data

## Identity and continuity

A repository gets a minimal durable identity record in its resolved common Git directory:

```clojure
{:resource-id #uuid "..."}
```

This lets Epiphany recognize the same repository after a directory move. All changing system knowledge—locations, commits seen, ingestion checkpoints, extraction records, caches, review decisions, and rewrite observations—belongs in the durable platform store.

Epiphany keeps these claims distinct:

- **Same path across adjacent commits:** observed path continuity
- **Same blob at another path:** exact relocation or exact copy evidence
- **Similar text/structure:** inferred similarity
- **Same idea or document purpose:** provisional or accepted semantic relation
- **Path repurposing:** proposed or accepted document-epoch boundary

For Markdown, continuity signals include text similarity, front-matter stability, explicit-link overlap, time gaps, and named-entity overlap. Code will have a separate policy later; Markdown and code are not forced into the same continuity model.

## Architecture

```text
CLI / TUI / Browser GUI
          |
          v
Versioned command/query application boundary
          |
          +--> Git object access (canonical Git facts)
          +--> MongoDB (events, observations, decisions, projections)
          +--> Search index (lexical, vectors, metadata, geo later)
          +--> Graph projection (entities and relations later)
          +--> Cache and work dispatch
```

### Storage roles

| Component | Role |
| --- | --- |
| Git object database | Canonical source for Git-backed commits, trees, blobs, and paths-at-commit |
| MongoDB | Durable resource records, observations, append-only decisions/events, jobs, and projection state |
| Search engine | Lexical, vector, metadata, and future geospatial retrieval |
| Graph store | Future explicit entity/relation traversal and graph-enriched retrieval |
| Cache | Disposable acceleration only; never canonical source or sole record of user decisions |

## Interfaces

The canonical executable is `epiphany`; `ep` is the supported short alias.

```bash
epiphany resource register ~/notes
ep ingest request res_...
ep search "repository identity"
ep evidence show sec_...
ep compare sec_... sec_...
ep lineage show sec_...
ep review decide cand_... accept
ep question create "What changed after the rewrite?" --evidence sec_...
ep tui
```

### Execution modes

The executable is dual-mode:

- **Direct mode (default):** CLI or TUI calls the application service in-process using the selected local adapters.
- **HTTP mode (explicit):** CLI or TUI calls the REST API using `--profile` or `--api`.

```bash
# Local source-owning machine
ep resource register ~/notes

# Remote server; the path is evaluated on that server
ep --api https://archivist.internal/api/v1 resource register /srv/corpus/notes
```

The system never silently falls back between modes.

## User interfaces

| Interface | Purpose |
| --- | --- |
| CLI | Automation, scripts, bootstrap, diagnostics, repair, and reproducible work |
| `ep tui` | Keyboard-first search, evidence triage, lineage, review, and operations |
| Browser GUI | Rich Markdown reading, side-by-side comparisons, timelines, health, and later visual exploration |
| Native desktop shell | Deferred; only if an offline bundled workstation becomes necessary |

Every durable TUI action must have a non-interactive CLI equivalent. The browser GUI uses the REST API and never bypasses the application boundary.

## Planned technology direction

Epiphany is Clojure-first. The intended deployment is a small local cluster with roles assigned by capability.

- Clojure services for domain logic, ingestion, orchestration, CLI, and TUI
- K3s or equivalent lightweight orchestration for multi-machine deployment
- MongoDB for durable operational state and event-oriented records
- Elasticsearch/OpenSearch-class search for full text, vectors, filters, and future geo search
- A graph store such as Neo4j for later graph projections
- JVM terminal UI via Lanterna or a compatible Clojure integration
- Local GPU inference for embeddings, reranking, extraction assistance, taxonomy prototyping, and evidence-grounded QA

## Development status

Epiphany is in greenfield design. The first implementation target is the Phase 1 corpus-archaeology loop, not a generalized autonomous research agent.

Read `AGENTS.md` before making changes. It defines architectural invariants, epistemic rules, and implementation guidance.

## Non-goals for Phase 1

- External scientific/geopolitical corpus ingestion
- Autonomous agent loops
- Code AST ingestion and cross-language n-gram analysis
- Full knowledge graph visualization
- Geospatial UI
- Multi-user collaboration and authorization
- Native desktop packaging

These are planned expansion areas, but they must not delay the evidence-preserving Git/Markdown foundation.
