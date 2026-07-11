# AGENTS.md — Epiphany Engineering Guide

This repository contains **Epiphany**, a local-first, Git-backed knowledge archaeology system. The purpose of the system is to recover, search, compare, and review the history of ideas across Git-backed personal and technical corpora.

This is a greenfield project. Favor simple, explicit, rebuildable designs over premature generalization.

## Prime directive

**Do not silently turn similarity into identity.**

Epiphany must always distinguish:

1. **Observed facts** — directly obtained from Git or another canonical source.
2. **Derived signals** — parser outputs, text similarity, embeddings, AST features, entity extraction, rank scores, and model outputs.
3. **Provisional claims** — inferred candidate relationships or proposed epoch boundaries.
4. **Accepted interpretations** — explicit human review decisions.

No layer may promote a lower-certainty item to a higher-certainty item without an explicit, durable transition and preserved evidence.

## Current scope

Phase 1 covers local Git repositories and Markdown history.

Primary workflows:

- Register a repository
- Discover Git-backed Markdown history
- Extract revision-scoped source expressions
- Search lexical, semantic, and hybrid indexes
- Open exact historical evidence
- Compare expressions/revisions
- Trace a lineage
- Review proposed relationships
- Create evidence-backed research questions
- Inspect/replay derived projections

Do not expand Phase 1 into external data ingestion, autonomous agents, code AST systems, graph visualization, maps, or a native desktop product unless the task explicitly requires it.

## Architecture boundaries

### Canonical source and rebuildable projections

- Git is canonical for Git-originated commits, trees, blobs, and paths-at-commit.
- MongoDB is durable for platform observations, append-only events, review decisions, jobs, and projection checkpoints.
- Search indexes, vector indexes, graph projections, and caches are rebuildable derived state.
- A cache must never be the only copy of a source fact or user decision.

### Application boundary

CLI, TUI, REST, and the browser GUI are adapters over the same versioned command/query application boundary.

```text
CLI / TUI / REST / GUI
        -> command/query services
        -> ports
        -> Git, MongoDB, indexes, cache, dispatch
```

Never let a UI adapter directly access MongoDB collections, Git storage, search indexes, graph storage, queues, or caches.

### Direct and HTTP modes

- Direct mode is the CLI/TUI default and calls application services in-process.
- HTTP mode is explicit via `--profile` or `--api`.
- Never silently fall back between modes.
- A filesystem path is evaluated on the selected target machine. Do not imply that an API server can read a client machine’s local path.

## Data and identity rules

### Repository identity

The only intended Git-local Epiphany metadata is a minimal resource identity record:

```clojure
{:resource-id #uuid "..."}
```

It belongs under:

```text
<resolved-common-git-dir>/corpus-archaeology/repository.edn
```

Do not add cursors, paths, credentials, cache hints, user notes, configuration, or operational state to this file. Those belong in MongoDB.

### Git worktrees

Resolve the **common Git directory** before reading/writing Epiphany repository metadata. A worktree `.git` may be a pointer file.

### Paths

Preserve Git path strings exactly as observed.

Never:

- Unicode-normalize paths
- Transliterate paths
- Case-fold paths
- Rewrite separators
- Resolve symlinks into canonical spellings
- Substitute display aliases for canonical observed paths

Canonical identity must preserve paths such as `.ημ/architecture/identity.md` exactly.

### Repository families

Automatically join repository instances into the same family only when they share observed commit OIDs. Remote URLs, directory names, similar content, and matching templates are hints only.

Do family assessment on registration. Reassess only after a suspected rewrite/history replacement. Never silently merge or split families.

## Continuity rules

Do not assume a file path is an idea, a document, or a permanent semantic identity.

Keep these relations separate:

- Same path across an adjacent parent/child commit: observed path continuity
- Same blob OID at another path: exact relocation/copy evidence
- Similar content: derived similarity
- Semantic relationship: provisional or accepted relation
- Path repurposing: proposed/accepted epoch boundary

Git rename/copy detection is evidence from a comparison, not permanent Git identity. An exact content move is strong evidence but does not automatically establish document-purpose identity.

### Markdown continuity

Phase 1 Markdown continuity signals are:

- text similarity
- front-matter stability/change
- explicit-link overlap/change
- time gap
- named-entity overlap/change

Persist raw signals, policy/configuration version, and resulting score. Scores must be recomputable and must not overwrite prior decisions.

### Code continuity

Code is deferred. When introduced, it must use a distinct policy namespace; do not reuse Markdown thresholds or semantics.

## Public status vocabulary

Use the shared status labels consistently in CLI, TUI, REST, and GUI:

| Status | Meaning |
| --- | --- |
| `OBSERVED` | Canonical-source observation |
| `PROVISIONAL` | Inferred candidate, not established |
| `ACCEPTED` | Explicit user review acceptance |
| `REJECTED` | Explicit review rejection; audit-visible |
| `STALE` | Derived by obsolete policy/model/extractor |
| `UNAVAILABLE` | Required source/service cannot be read |

Never present `PROVISIONAL` results as observed history. Never use color alone to express status.

## Commands, queries, and events

### Queries

Queries do not create durable state. They retrieve observations/projections.

Examples:

```clojure
{:query/type :corpus/search
 :query "repository identity"
 :mode :hybrid}

{:query/type :evidence/get
 :expression-id "sec_..."}
```

### Commands

Commands represent explicit durable intent. They create records, events, decisions, or work requests.

Examples:

```clojure
{:command/type :resource/register
 :location "/home/user/notes"}

{:command/type :review/record-decision
 :candidate-id "cand_..."
 :decision :accept}
```

Review decisions are append-only interpretive events. Do not model them as a generic mutable `PATCH` to a candidate’s status.

### Idempotency

All durable commands need a request/idempotency ID. Command retry must not duplicate registration, review decisions, ingest requests, or replay requests.

## Interface conventions

Canonical executable:

```bash
epiphany
```

Supported short alias:

```bash
ep
```

The alias must invoke the same artifact and behavior. Documentation should prefer `epiphany` when clarity matters and may use `ep` in compact examples.

The TUI is part of the executable:

```bash
ep tui
```

Every durable TUI action needs a scriptable CLI equivalent.

CLI output:

- results: stdout
- diagnostics/errors: stderr
- default: human-readable text/table
- machine output: `--format edn|json`
- verbose output includes target/profile/request ID and relevant projection versions

## API conventions

- REST is versioned under `/api/v1`.
- JSON is the default external format; EDN may be accepted for trusted local clients.
- Use RFC 9457-style `application/problem+json` for REST errors.
- Use resource-oriented reads and explicit command/request resources for writes and asynchronous work.
- Use `POST /review-decisions`, not mutable candidate status updates.
- `POST /searches` is acceptable for complex hybrid search requests.
- Keep OpenAPI in version control once the initial HTTP contract is stable; do not let it drift from application schemas.

## Clojure implementation guidance

- Prefer small pure functions around immutable maps at the domain edge.
- Use Malli schemas for versioned command/query contracts and external payload validation.
- Keep I/O behind ports/protocols or narrow adapter namespaces.
- Keep `clojure.tools.cli` at the parsing edge; it must not become the domain model.
- Keep TUI state ephemeral and separate from durable corpus records.
- Design jobs/projections to be idempotent and resumable.
- Store provenance: source identifiers, source spans, extractor/model/policy version, generated time, and configuration identity where relevant.

## Testing requirements

Every change involving a claim, projection, or interface should have tests at the appropriate boundary.

Minimum expectations:

- Unit tests for pure continuity, status, and schema behavior
- Integration tests for Git object traversal and exact path preservation
- Idempotency tests for durable commands
- Replay tests for projections
- Adapter parity tests: direct CLI and REST must produce equivalent application outcomes for the same command/query
- Tests that inferred candidates never appear as observed facts
- Tests for moved repository identity and worktree common-directory resolution
- Unicode path tests, including `.ημ/`

Do not require an HTTP server to test core application services.

## Documentation expectations

When adding a new feature, document:

- whether its output is observed, derived, provisional, or accepted;
- canonical source(s) and rebuild path;
- identifiers and provenance retained;
- idempotency/replay behavior;
- direct vs HTTP behavior, if it handles filesystem paths or commands;
- user-visible status and error behavior.

Write an ADR for decisions that change identity, continuity, source authority, storage responsibility, interface boundary, or long-term operational commitment.

## Avoid

- “Just use embeddings” designs
- Hidden inference presented as truth
- Direct database access from UI code
- Replacing exact Git facts with normalized display values
- Treating a path rename as proof of semantic identity
- Treating exact-copy detection as proof of ongoing document identity
- Deleting rejected candidates or old derived outputs without audit/rebuild policy
- Making one global cursor stand in for independent projection checkpoints
- Premature graph/map UI before evidence inspection and review work
- Adding an LLM to a pipeline without retaining prompt/model/version/evidence provenance

## Definition of done

A feature is not done until:

1. Its source authority and epistemic status are explicit.
2. Its input/output schemas are validated.
3. Its provenance and versioning are retained where it creates derived knowledge.
4. Its operation is idempotent or its non-idempotence is intentional and documented.
5. It is reachable through the proper application boundary.
6. Its failure mode is observable and actionable.
7. It does not silently convert similarity, model output, or UI convenience into identity.
