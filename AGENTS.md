# Epiphany — Engineering Guide

Local-first, Git-backed knowledge archaeology: recover, search, compare, and review the history of ideas across Git repositories. Markdown first (phase 1), code later. Greenfield **JVM Clojure** — favor simple, explicit, rebuildable over clever.

## Ground truth

- There is no application code yet. The first card is **US-000A** (scaffold + green tests). Until it lands, nothing below about commands is real — build it to make them real.
- Work comes from the board: `docs/kanban/` — pick from `ready`, respect `dependency:`. Full board contract: `docs/kanban/AGENTS.md`. Delivery order: `docs/kanban/BOARD-BREAKDOWN.md`.
- ADRs in `docs/adrs/` are architecturally authoritative. Cards and this file never override them.

## This is JVM Clojure, not ClojureScript

Most prior work in this space (`../eta-mu`, `../Truth`) skews CLJS. Here:

- No `^:async`, no `js/` interop, no shadow-cljs. Build tool is **Clojure CLI + `deps.edn`** aliases.
- Interop is Java: JGit, Lucene, the Mongo sync driver. Wrap interop at the `infra/` edge; never let Java types leak into `domain/`.
- Concurrency: virtual threads (JDK 21+) or `future`; no `core.async` unless a card justifies it.
- Tests run with **kaocha**. REPL is `clojure -M:repl` (nREPL for editor attach).

## Commands (canonical once US-000A lands)

```bash
clojure -M:test              # full suite — the green baseline
clojure -M:unit-test         # no Docker, no network
clojure -M:integration-test  # needs local services started, else exits UNAVAILABLE
clojure -M:repl              # dev REPL
clojure -M:run -- --help     # the epiphany executable (alias: ep)
```

## Namespace law (four quadrants, no junk drawers)

| Layer     | Contents                                            | Rule                        |
|-----------|-----------------------------------------------------|-----------------------------|
| `domain/` | Continuity, lineage, status, ranking decisions      | Pure. Zero I/O.             |
| `infra/`  | Git (JGit), MongoDB, Lucene, Ollama HTTP, CLI/HTTP  | No domain policy.           |
| `shape/`  | Parsing, spans, hashing, (de)serialization morphisms| Pure, domain-agnostic.      |
| `law/`    | Malli schemas, validators, guards                   | No I/O. Contracts only.     |

No `utils/`, no `helpers/`. Schemas before adapters — a `law/` contract exists before the `infra/` code that persists it. See `STYLE.md` for construction order and idioms.

## Dependency policy

Starting set — do not add beyond it without a one-line justification in the card and its design doc:

```clojure
org.clojure/clojure                {:mvn/version "1.12.x"}
metosin/malli                      ; law/ schemas
org.eclipse.jgit/org.eclipse.jgit  ; Git object access — read objects, never shell out, never checkout history
org.mongodb/mongodb-driver-sync    ; durable observations/events (direct interop; no wrapper lib)
org.apache.lucene/lucene-core      ; lexical index + KNN vectors (add analyzers/queryparser as needed)
com.vladsch.flexmark/flexmark      ; Markdown with exact source offsets (decide vs commonmark-java in ENG-002A)
lambdaisland/kaocha                ; tests
org.clojure/tools.cli              ; CLI parsing
```

Sanctioned when their card arrives, not before: `ring` + `reitit` + `jsonista` (ENG-006A HTTP), htmx-or-cljs choice for the workbench UI (ENG-006B). Ollama is called over plain HTTP (`java.net.http`) — no SDK.

## Local services

- **MongoDB** — already running on this machine. Connection settings come only from the profile/config boundary (US-000C); never hardcode, never commit credentials.
- **Ollama** — running locally (`localhost:11434`); embeddings and any bounded LLM step go through it. Model name + version are recorded on every derived record.
- A second machine exists for clustering later. Phase 1 targets **one strong machine**; don't design for the cluster yet.
- Profiles: `:local` = in-process/in-memory, `:services` = real local adapters. Selection is explicit; an unreachable selected service is `UNAVAILABLE`, never a silent fallback.

## Data authority (ADR-000)

Git is canonical for commits/trees/blobs/paths-at-commit. MongoDB is durable for observations, append-only events, review decisions, jobs, checkpoints. Lucene/vector/graph projections are rebuildable — a cache or index is never the only copy of a fact or decision.

## The one domain rule

**Never silently turn similarity into identity.** Every record is one of: *observed* (from Git/canonical source) → *derived* (parser/similarity/embedding output) → *provisional* (candidate relation, proposed boundary) → *accepted* (explicit human decision). Promotion up this ladder only happens through an explicit, durable event with preserved evidence. UI, ranking, and export must always show which tier a thing is.

## Identity & idempotency

- The only Git-local metadata is `<common-git-dir>/corpus-archaeology/repository.edn` containing `{:resource-id #uuid "..."}` — nothing else, ever (ADR-001).
- Resolve the **common Git directory** first; a worktree `.git` is a pointer file.
- Every durable command carries a request/idempotency ID; retry must not duplicate anything.
- Exact path strings are preserved byte-for-byte — no normalization, Unicode included (`.ημ/` is a test case).

## Quality gate

- Zero warnings: clj-kondo, tests, all of it. Warnings are failed contracts.
- Every adapter has parity tests (direct CLI vs HTTP produce equivalent outcomes) once HTTP exists.
- Idempotency and replay behavior are tested, not asserted.
- Done = acceptance criteria demonstrated + epistemic tier explicit + failure mode observable. Full checklist: `PROCESS.md`.

## Where everything else lives

| Doc | Purpose |
|-----|---------|
| `STYLE.md` | Clojure house rules, categories-vs-contracts, construction order |
| `PROCESS.md` | Intake→Ready→Done workflow, grounding chain |
| `docs/kanban/AGENTS.md` | Board contract: FSM, 5-point cap, how agents pick work |
| `docs/kanban/BOARD-BREAKDOWN.md` | Phase/gate delivery map, critical path |
| `docs/adrs/` | ADR-000 data boundary, ADR-001 identity, ADR-002 CLI/REST, ADR-003 unified executable |
| `docs/designs/` | Feature designs (cite research + ADRs) |
| `docs/AGENTS.md` | docs/ directory conventions and frontmatter |
