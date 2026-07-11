---
slug: phase-1-narrowed-decisions
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000202
title: "Phase 1 Narrowed Decision Set"
kind: design
description: "Narrowed Phase 1 decision list scoped to tracing one idea across two years of Git and Markdown with inspectable evidence."
labels: [architecture, phase-1, decisions]
created: "2026-07-11"
source: "docs/inbox/2026.07.11.04.43.46.md"
---

For **Phase 1 only**, we should narrow the decision set to what is necessary to trace one idea across two years of Git and Markdown with inspectable evidence. That means: no code intelligence, external research acquisition, geospatial data, simulation, or autonomous agents yet.

The open decisions below are ordered by dependency. We should settle the first four before writing an ADR or choosing Kubernetes/search/graph products.

## Phase 1 boundary

The Phase 1 success criterion is:

> Select an important present-day idea and reconstruct its evidence-backed history across Git commits and Markdown sections over roughly two years.

Required capabilities are:

- Git + Markdown ingestion
- immutable artifact preservation
- commit and revision history
- section-level Markdown extraction
- lexical plus semantic retrieval
- candidate idea-lineage links
- redundancy/contradiction review queue
- temporal evidence views.

Everything else is deferred unless it directly makes that loop work.

## Decisions to make

| Order | Open decision | Why it matters now | Output |
|---|---|---|---|
| 1 | Authoritative data and rebuildability boundary | Determines what can safely be reindexed, regenerated, or discarded | ADR 000 |
| 2 | Artifact and identity model | Determines how files, blobs, sections, and ideas survive time and path changes | ADR 001 |
| 3 | Event ledger scope in MongoDB | Determines how ingestion, review decisions, and replay are represented | ADR 002 |
| 4 | Immutable artifact storage | Determines how original Git/Markdown evidence is retained and restored | ADR 003 |
| 5 | Execution and deployment model | Determines whether Phase 1 needs K3s, a queue, workers, and node roles | ADR 004 |
| 6 | Markdown extraction contract | Determines what a section is and how it points to exact source spans | ADR 005 |
| 7 | Retrieval architecture | Determines lexical/vector search ownership and hybrid ranking | ADR 006 |
| 8 | Graph minimum | Determines whether Phase 1 needs a dedicated graph database | ADR 007 |
| 9 | Review and provenance model | Determines how lineage, redundancy, and contradiction candidates become trusted | ADR 008 |
| 10 | First interface | Determines how you inspect evidence and complete the success test | ADR 009 |
| 11 | Observability and backup baseline | Determines whether the platform is safely operable from the beginning | ADR 010 |

## 1. Authoritative data boundary

This is the first decision because it establishes what can never be lost versus what can always be regenerated.

### The choices

| Model | Canonical data | Rebuildable data | Main risk |
|---|---|---|---|
| Document-first | Mongo documents representing current notes/files | Search and graph | Historical evidence can become secondary or mutable |
| Artifact-first | Raw Git blobs and source snapshots | Everything else | Harder to represent user decisions and operational state |
| Event-first | Immutable event ledger | All state projections, including artifact metadata | Higher early design discipline |
| Hybrid artifact + event | Immutable source artifacts plus append-only domain events | Search, graph, embeddings, candidate inferences, UI views | Requires clear boundary rules |

For this project, the strongest candidate is **hybrid artifact + event**:

- Raw Git blob bytes and preserved source snapshots are the evidence base.
- MongoDB records artifact identity/metadata plus append-only events.
- Search results, embeddings, graph edges, lineage candidates, and summaries are all derived projections.
- Human review decisions are durable events, because they change the platform’s interpretation without rewriting the source evidence.

That aligns with your MongoDB-centered, event-log-plus-projection approach and keeps the data “touchable”: source and ledger records remain legible even if infrastructure changes around them. [perplexity](https://www.perplexity.ai/search/b4c9045c-cfa8-4d39-b0ac-c40ef4f8091a)

### Questions to settle

- Is **Git itself** always authoritative for repository history, with our object store being an acquisition cache/archive?
- Or, once ingested, is our stored Git blob snapshot authoritative even if the working repo later disappears or rewrites history?
- Are user labels, annotations, and accepted lineage links immutable events only, or do they also get a mutable convenience document?
- Can any source artifact ever be deleted, or only redacted/unlinked through a later event?
- Is the system required to answer “what did we know at time X?” from ledger/event history alone?

### Recommended position

Use this rule:

> **Source bytes and append-only interpretation events are durable authority. Every searchable, graph, semantic, visual, or model-derived representation is a versioned projection.**

That is narrowly sufficient for Phase 1 and does not prematurely dictate a graph DB, search engine, or orchestration system.

## 2. Artifact and identity model

The system cannot trace an idea reliably if identity is only a file path. A path changes; a filename is reused; a section moves; headings are renamed; two similar notes may diverge.

We need separate identities for:

```text
Repository
  -> Git commit
    -> tree entry/path-at-commit
      -> blob/content revision
        -> parsed document revision
          -> section revision
            -> source spans / blocks
```

The important distinction is:

- **Artifact identity:** “this is the same logical document or note lineage.”
- **Revision identity:** “this exact content existed in this exact blob/commit.”
- **Extraction identity:** “this parser version produced this section/block representation.”
- **Concept/idea identity:** “these separate expressions may concern the same idea”—always uncertain at first.

### Questions to settle

- Is a logical file identity generated on first observation, then continued through path changes using candidate lineage?
- Or do we avoid a permanent logical-file identity until a user accepts a rename/move relation?
- Are Markdown headings the identity of sections, or are sections revision-scoped only?
- What counts as a source span: byte offsets, character offsets, line/column, or all three?
- Do we preserve only Git-tracked Markdown or also untracked/local vault material?
- Do we model notebook-like notes outside Git as first-class sources in Phase 1, or explicitly defer them?

### Recommended position

Do **not** make an “idea” or “section” permanently identical merely because a parser sees similar text.

Use four levels:

```clojure
{:artifact/id ...}           ; logical source object, cautiously maintained
{:revision/id ...}           ; immutable blob at a commit/path
{:extraction/id ...}         ; parser/version output
{:section/id ...}            ; section expression within one extraction
{:lineage-candidate/id ...}  ; claim that two artifacts/revisions/sections connect
```

That design lets the platform be uncertain where it should be uncertain. A probable rename, section continuation, or idea lineage is a relationship with evidence—not an identity mutation.

## 3. MongoDB event-ledger scope

MongoDB is already a fixed constraint for central persistence; the decision is not “MongoDB or Postgres.” It is **how much of Phase 1 uses append-only events, and what remains ordinary document state**.

### Three reasonable scopes

| Scope | Event-sourced objects | Ordinary documents | Tradeoff |
|---|---|---|---|
| Minimal | Ingestion/audit events only | Most metadata and review state | Fastest start, weaker historical reconstruction |
| Core domain | Artifacts, revisions, reviews, labels, ingestion, projections | Caches and derived read views | Best Phase 1 fit |
| Universal | Every state mutation | Almost nothing | Maximum replayability, maximum ceremony |

### Recommended position

Choose **core-domain event sourcing**.

Append events for:

- source registered or source configuration changed
- commit/revision/blob observed
- artifact captured
- parse/extraction completed or failed
- embedding/index/graph projection requested or completed
- lineage/redundancy/contradiction candidate created
- candidate accepted, rejected, relabelled, or annotated
- projection checkpoint advanced
- manual correction or source redaction requested.

Keep mutable convenience documents for:

- current source configuration
- latest ingestion status
- job lease/heartbeat
- materialized current artifact summary
- current candidate-review queue state
- cache metadata.

This preserves the “event log + projection” model you already use conceptually: events show what happened; projections make current work practical. MongoDB Change Streams can later bridge persisted event insertion to worker dispatch, but that is an execution-model decision, not something ADR 002 needs to settle. [perplexity](https://www.perplexity.ai/search/036ac07c-8e6f-456e-8d89-3c241660cfd5)

## 4. Immutable artifact storage

MongoDB should not carry every raw blob, Markdown version, large file, or future PDF/dataset payload inside operational documents. We need to decide where immutable source bytes live and how they are addressed.

### Choices

| Approach | Benefit | Cost |
|---|---|---|
| Git repositories only | Lowest initial complexity | Loses resilience to deleted/remapped repos; weak cross-source abstraction |
| MongoDB GridFS | One database operationally | Mongo becomes the large-file storage and backup burden |
| Content-addressed filesystem | Simple and local | More custom replication, access, and metadata work |
| S3-compatible object storage | Clear blob boundary, snapshots, scalable artifact class | One additional stateful service |
| Hybrid: Git + object snapshots | Git remains native, object store preserves observed bytes | Requires policy for capture timing and duplication |

### Questions to settle

- Must every Git blob be copied immediately, or can Git object databases remain the initial byte source?
- Do we snapshot only Markdown blobs in Phase 1, or the full Git object graph?
- Is the artifact store content-addressed by SHA-256, Git blob SHA, or both?
- What source data can be reconstructed from Git and therefore need not be backed up separately?
- What is the restore target: one machine, a new cluster, or a new directory plus Mongo restore?
- Do you want object storage now, or should a content-addressed local filesystem be enough until external papers/datasets arrive in Phase 3?

### Recommended position

For Phase 1:

> Preserve the exact bytes of every ingested Markdown blob, plus ingestion manifests and hashes. Keep Git commit metadata in MongoDB. Do not yet require full-repository object mirroring unless a repository is unstable, private, or likely to disappear.

That gives you inspectable evidence without immediately turning Phase 1 into a general Git-hosting/archive system.

## 5. Execution model

This is the first infrastructure decision, but it comes *after* the data model because the data model should survive any scheduler or deployment change.

### Options

| Model | Best for | Weakness |
|---|---|---|
| Single JVM modular monolith | Fastest Phase 1 delivery | Weak horizontal separation |
| Modular monolith + local worker processes | Clear stages without cluster overhead | Manual supervision/deployment |
| Containerized services with Compose/systemd | Practical multi-machine operation | Less standardized scheduling |
| K3s from day one | Learning Kubernetes and future cluster work | Significant early operational surface |
| Custom actor/process manager | Deep Clojure/EDN alignment | Becomes its own large project |

Your cluster has two nodes that can do substantial work and two better suited to IO, storage, routing, backups, and telemetry. That makes **non-competing roles** more sensible than pretending all four nodes are equal compute workers.

### Questions to settle

- Is the goal of Phase 1 to validate corpus archaeology or to establish the eventual cluster operating environment?
- Do you need automatic cross-node placement before you have a workload benchmark?
- Is Kubernetes a learning/infrastructure goal in its own right, or only a means to run the application?
- Can Phase 1 run on one strong node with a second worker and cold-storage/backup nodes?
- Do you want to build around your EDN-based process/actor work now, or keep the archaeology platform independent of it until core ingestion exists? [perplexity](https://www.perplexity.ai/search/eed52251-a7f7-405c-a6a2-1b35308a6465)

### Recommended position

Start Phase 1 as a **modular Clojure monolith plus independently deployable workers**, containerized but not forced into K3s on day one.

That means:

```text
API / review application
Git + Markdown ingestion worker
Markdown extraction worker
Embedding/index worker
Projection worker
MongoDB
artifact storage
search engine, if chosen
```

Each process communicates through durable records/events and can later be scheduled under K3s. This lets you prove the data and retrieval model before paying Kubernetes’ coordination tax.

That recommendation does **not** reject K3s. It says “K3s adoption” should become a separate decision once Phase 1 has a known workload profile and a deployment shape worth orchestrating.

## 6. Markdown extraction contract

A “section” needs a concrete definition before we can index it, embed it, compare it, or use it as evidence.

### Questions to settle

- Are sections defined by heading hierarchy only?
- How do preamble text, title blocks, lists, tables, code fences, quotations, and front matter behave?
- Does a section include all nested subsections, or are parent/child sections separately indexed?
- Do we index paragraphs/blocks separately in addition to sections?
- Are tags, links, TODOs, dates, people, project names, and code identifiers extracted in Phase 1?
- What must an evidence citation contain to let you inspect the exact original text?

### Recommended position

Use a three-level extraction model:

```text
Document revision
  -> section expression (heading-delimited, hierarchy-aware)
    -> block expression (paragraph/list/table/quote/code fence)
```

Index sections as the default retrieval unit. Retain blocks for citations, contrast/duplicate review, and precise evidence. This is enough to trace ideas without prematurely modeling every sentence as a graph entity.

## 7. Retrieval architecture

The fundamental product question is:

> How does the platform retrieve the evidence set from which lineage and review decisions can be made?

You correctly identified that vector search is only one signal. The Phase 1 ranker needs at least:

- lexical matching;
- semantic similarity;
- time/date;
- repository/path;
- headings/tags;
- Git/revision relationship;
- explicit links;
- eventually, human labels and accepted relationships.

### Questions to settle

- Is Elasticsearch/OpenSearch necessary now, or is MongoDB Search adequate for the first corpus size?
- Is vector search colocated with lexical search or kept separate?
- Do we use one embedding model for all Markdown, or distinct models for notes, code blocks, and commit messages?
- Is retrieval at section level only, or section plus block?
- How does the system combine lexical and vector scores?
- What does a search result have to expose so you can trust it?

### Recommended position

Do not pick a stack by feature checklist alone. Write a benchmark corpus first:

- 30–50 questions you know your notes should answer;
- a small set of known related note/section pairs;
- a small set of known false friends;
- 10–20 cases where exact wording matters;
- 10–20 cases where paraphrase matters;
- at least a few time- and path-constrained queries.

Then compare:

1. MongoDB Search plus MongoDB vector search;
2. Elasticsearch/OpenSearch hybrid search;
3. any custom semantic-graph retrieval you want to test.

You have already explored MongoDB vector search and custom semantic graph indexing, so a measured Phase 1 evaluation is a better basis than importing Elasticsearch because it is conventional.

## 8. Graph minimum

The Phase 1 graph is smaller than “knowledge graph platform.”

It needs only:

```text
commit -> parent commit
commit -> revision observed
revision -> blob
revision -> document extraction
document -> section
section -> explicit link / tag
revision or section -> candidate lineage relation
candidate -> human decision
```

### Questions to settle

- Is MongoDB sufficient for these bounded relationships and evidence packets?
- Do you need arbitrary multi-hop traversal and interactive graph exploration on day one?
- Does the graph store need to exist before you have enough observed links to make it useful?
- Should lineage candidates be documents in MongoDB first, with graph projection later?
- What queries cannot be served acceptably by Mongo documents plus search results?

### Recommended position

**Defer a dedicated graph database until after basic ingestion, extraction, and retrieval exist.**

Phase 1 can represent revision/section/candidate relations in MongoDB documents, build focused lineage packets in application code, and measure what traversal queries become painful. This respects your preference for a central datastore and avoids declaring Neo4j a mandatory component before the corpus demonstrates the need.

A graph projection becomes justified when you need frequent, interactive, multi-hop questions such as:

- “show all evolving descendants of this idea through accepted and candidate links,”
- “find all note clusters reachable through two concept bridges but not through Git ancestry,”
- “compare topology at two historical dates.”

## 9. Review and provenance

No automatic lineage, redundancy, or contradiction result should become authoritative just because an embedding or LLM says so.

### Questions to settle

- What candidate relation types exist in Phase 1?
- What evidence threshold is required before showing a candidate?
- Which review actions are available?
- Are user decisions global, per-project, or private annotations?
- How do rejected relationships affect future ranking?
- How do you prevent the review queue from becoming an infinite pile?

### Recommended Phase 1 relation types

```clojure
:continues
:refines
:references
:possibly-derived-from
:near-duplicate
:possible-contradiction
:possibly-supersedes
:unrelated
```

### Recommended review actions

```clojure
:accept
:reject
:relabel
:defer
:ignore-similar-future-candidates
:add-note
:create-research-question
```

Each decision should append an event and preserve source spans, candidate-generation configuration, score components, and rationale. This is exactly where an event-ledger approach pays off: you can later revise an inference model without losing your historical judgment. [perplexity](https://www.perplexity.ai/search/b4c9045c-cfa8-4d39-b0ac-c40ef4f8091a)

## 10. First interface

The Phase 1 UI does not need to be a polished universal workbench. It needs to support the proof of value.

Minimum screens or CLI views:

1. **Search** — query, filters, lexical/semantic evidence.
2. **Evidence reader** — exact source span, surrounding section, raw revision, commit metadata.
3. **Timeline** — chronological note/revision expressions and candidate links.
4. **Lineage packet** — selected idea, evidence chain, confidence/status, human decisions.
5. **Review inbox** — candidates with accept/reject/relabel actions.
6. **Operations panel** — ingestion errors, queue/index state, replay status.

The first user interface is an architectural decision because it determines what evidence and provenance the backend must expose.

## Proposed discussion sequence

I suggest we discuss these one at a time, in this order:

1. **Authoritative data and rebuildability boundary**
2. **Artifact and identity model**
3. **Mongo event-ledger scope**
4. **Raw artifact storage**
5. **Execution model**
6. **Extraction contract**
7. **Retrieval benchmark and retrieval architecture**
8. **Graph deferral/adoption criteria**
9. **Review/provenance contract**
10. **First interface and operations baseline**

Let’s start with the first one: **when a repository is ingested, do you want the platform to preserve its own immutable copy of the Markdown blob bytes as evidence immediately, or treat the Git repository itself as canonical until a later archival step?**
