---
title: Phase 1 — Corpus Archaeology
slug: phase-1-corpus-archaeology
created: 2026-07-11
source: docs/inbox/clojure Natural Language Processing.md
kind: design
parent: docs/notes/design/knowledge-platform-overview.md
research:
  - docs/notes/research/nlp-fundamentals-and-classifiers.md
  - docs/notes/research/nlp-and-code-intelligence-deep-research.md
  - docs/notes/research/vector-search-and-knowledge-graphs-deep-research.md
  - docs/notes/research/research-to-design-implications.md
---

# Phase 1 — Corpus Archaeology

## Objective

Given a current note, concept, or code-adjacent markdown file, reconstruct an evidence-backed timeline of where its ideas appeared, how they changed, what other notes overlap with it, and which claims might be redundant or in tension.

## In scope

- Git repositories and Markdown files.
- Git commit/revision history.
- Markdown section parsing.
- Immutable source artifact capture.
- Lexical, structural, and semantic retrieval.
- Candidate-level lineage, redundancy, and contradiction analysis.
- Review queues and temporal visualization.
- Provenance, observability, replay, and deletion/rebuild safety.

## Out of scope

- Full multi-language AST indexing.
- Automated note deletion.
- Autonomous external web crawling.
- General research agents.
- Weather, physics, geospatial intelligence, and behavior simulations.
- Fully automated claim truth adjudication.
- A universal graph ontology.

## Epics

| Epic | Name | Goal |
|---|---|---|
| Epic 1 | Archaeological Ledger | Ingest Git repositories and Markdown revisions into an immutable, replayable artifact ledger. |
| Epic 2 | Markdown Evidence Extraction | Turn every Markdown revision into stable, addressable evidence units without losing traceability. |
| Epic 3 | Retrieval Substrate | Build multi-signal retrieval over Markdown evidence units, with every result explainable. |
| Epic 4 | Temporal Idea Lineage | Infer candidate “same idea evolving over time” paths from revisions and extracted evidence. |
| Epic 5 | Redundancy and Tension Review | Surface note pairs or clusters that are duplicates, near-duplicates, supersessions, or possible contradictions. |
| Epic 6 | Temporal Research Workbench | Provide the first interface where archaeology is usable, reviewable, and genuinely interesting. |
| Cross-cutting | Operability | Every ingestion, extraction, embedding, projection, and review decision is observable, attributable, and replayable. |

## Epic 1: Archaeological Ledger

**Goal:** Ingest Git repositories and Markdown revisions into an immutable, replayable artifact ledger.

**User outcome:** “I can select a repository and see every Markdown artifact, its revisions, the commit that introduced each revision, and the exact source bytes that support it.”

### Scope

- Register a repository as a source.
- Discover tracked Markdown files, initially including `**/docs/**/*.md`, then configurable glob rules.
- Walk the reachable Git commit graph.
- Persist commits, parents, author/committer timestamps, message, tree hash; file-at-commit revisions; blob hash and path; add/modify/delete status; raw source blob in object storage; ingestion run, tool version, configuration, and failures.
- Create initial file-lineage candidates from Git diff rename detection at multiple similarity thresholds.
- Never overwrite an existing source observation; reruns add an ingestion run and deduplicate by content hash/provenance.

### Core events

```clojure
{:event/type :source/repository-registered
 :repository/id ...
 :repository/remote ...
 :repository/root ...}

{:event/type :git/commit-observed
 :commit/sha ...
 :commit/parents [...]
 :commit/authored-at ...
 :commit/committed-at ...
 :commit/message ...}

{:event/type :artifact/revision-observed
 :artifact/id ...
 :revision/blob-sha ...
 :revision/path ...
 :revision/commit-sha ...
 :revision/content-ref ...
 :revision/language :markdown}

{:event/type :artifact/lineage-candidate
 :from/revision-id ...
 :to/revision-id ...
 :relation :renamed-or-moved
 :similarity 0.87
 :detector {:name :git-diff :rename-threshold 0.70}}
```

### Non-goal

Do not attempt semantic idea lineage here. This epic establishes revision lineage: what Git can support with inspectable evidence.

## Epic 2: Markdown Evidence Extraction

**Goal:** Turn every Markdown revision into stable, addressable evidence units without losing the ability to trace them back to exact text.

**User outcome:** “I can search and inspect notes at the level of headings, paragraphs, lists, quotes, code blocks, and links—and every derived claim points to its source span.”

### Scope

- Parse Markdown into a normalized AST.
- Generate stable section IDs based on content/path/revision context.
- Extract document title, heading hierarchy, sections and paragraphs, lists, blockquotes, tables, code fences, links, tags, explicit wiki-style references, front matter, and line/column/character offsets.
- Preserve parent/child containment: repository → file lineage → revision → document → section → block → span.
- Emit canonical text for each retrieval unit.
- Define chunking as a pure, versioned function.

### Domain rule

A section is not “the concept.” It is an evidence-bearing expression of one or more concepts at a point in time.

## Epic 3: Retrieval Substrate

**Goal:** Build multi-signal retrieval over Markdown evidence units, with every result explainable.

**User outcome:** “I can find notes by phrase, topic, conceptual similarity, headings, tags, date, repository, and Git history—and see why each result ranked.”

### Scope

Implement three retrieval channels:

- **Lexical:** full-text index over title, headings, body, tags, commit messages, and selected metadata.
- **Structural:** heading/path/link/tag/repository/temporal filters and overlap relationships.
- **Semantic:** embeddings at section/block level, plus versioned model metadata.
- **Hybrid ranker:** combines signals rather than treating vector similarity as truth.

Every result carries a score breakdown:

```clojure
{:result/section-id ...
 :ranking {:lexical 0.71
           :semantic 0.84
           :structure 0.16
           :temporal 0.32
           :final 0.73}
 :evidence {:matched-terms ["event sourcing" "projection"]
            :semantic-neighbors [...]
            :shared-tags [:architecture :data]}}
```

### First benchmark

Create 30–50 questions from personal notes, such as:

- “Where did I first describe semantic gravity?”
- “Which notes argue for an event log as source of truth?”
- “Show me the early versions of the command-center idea.”
- “What did I mean by concept boundaries in namespaces?”
- “Find everything I wrote that might be a duplicate of this note.”

## Epic 4: Temporal Idea Lineage

**Goal:** Infer candidate “same idea evolving over time” paths from revisions and extracted evidence, while preserving uncertainty.

**User outcome:** “Starting from a current idea, I can walk backward through its likely predecessors and forward through branches, refinements, and descendants.”

### Candidate relations

- `:continues`
- `:refines`
- `:splits-into`
- `:merges-from`
- `:references`
- `:possibly-derived-from`
- `:supersedes`

### Approach

- Build temporal graphs from Git revisions, section containment, lexical similarity, semantic similarity, shared named concepts/tags, explicit links, commit co-occurrence, and rename/move candidates.
- Use deterministic candidate generation first.
- Add LLM analysis only after retrieval chooses a small evidence set; require structured output and provenance.
- Support human review: accept, reject, relabel, or annotate candidate relations.

### Design constraint

Lineage is not necessarily textual similarity. A later note may reject, clarify, operationalize, or split an earlier idea. Model “related historical transformation” rather than assuming similarity means continuity.

## Epic 5: Redundancy and Tension Review

**Goal:** Surface note pairs or clusters that are duplicates, near-duplicates, supersessions, or possible contradictions—and make review safe.

**User outcome:** “I can reduce note clutter without erasing the history of my thought, and I can identify where I have changed my mind or left incompatible assumptions unresolved.”

### Candidate classifications

- `:duplicate`
- `:near-duplicate`
- `:complementary`
- `:superseded`
- `:possible-contradiction`
- `:unclear`

### Contradiction pipeline

1. Extract candidate claims from note sections.
2. Normalize claims into a schema: subject, predicate, object, scope, time, modality, source.
3. Retrieve candidates with shared entities/topics and potentially incompatible predicates.
4. Let deterministic rules identify obvious forms: opposite polarity, mutually exclusive values, incompatible timelines.
5. Use an LLM only as a triage/rationale generator, returning structured evidence.
6. Store a proposed `:contradicts` edge with confidence, scope, and evidence spans.
7. Require human review before promoting it to an accepted relation.

### Review actions

- keep
- link
- mark superseded
- merge into a synthesis note
- archive
- ignore
- create research question

### Quality rule

“Low quality” must never mean “not useful.” Distinguish low informational density, obsolete claim, unfinished fragment, duplicate expression, private emotional/contextual trace, and historically important precursor.

## Epic 6: Temporal Research Workbench

**Goal:** Provide the first interface where archaeology is usable, reviewable, and genuinely interesting.

**User outcome:** “I can follow an idea’s history as a map, pivot into original evidence, review candidate relationships, and ask grounded questions over my corpus.”

### Views

- **Timeline:** commits, revisions, section expressions, lineage candidates, accepted transitions.
- **Concept/idea map:** nodes clustered by hybrid retrieval; edges styled by relation type and confidence.
- **Evidence drawer:** exact source span, full section context, commit metadata, source diff.
- **Candidate review inbox:** duplicates, contradictions, supersessions, lineage suggestions.
- **Search workspace:** lexical/semantic/hybrid mode, filters, score explanation.
- **Corpus health panel:** unparsed revisions, extraction errors, index age, queue backlog, confidence distribution.

### Core loop

1. Search or select an idea.
2. Inspect evidence.
3. Traverse timeline/lineage.
4. Review suggested relationships.
5. Record a decision or research question.
6. Re-run projections and observe improved retrieval.

## Cross-cutting epic: Operability

**Goal:** Every ingestion, extraction, embedding, projection, and review decision is observable, attributable, and replayable.

### Required telemetry

- Per-run correlation ID.
- Repository, commit, blob, revision, section, and job IDs in structured logs.
- Queue depth, retry count, dead-letter count, and job latency.
- Parsing success/error rates by repository and extractor version.
- Embedding throughput, cache hit rates, GPU utilization, and index lag.
- Search latency and retrieval metrics by query class.
- Candidate edge volume, acceptance/rejection rate, and reviewer disagreement.
- Projection build time and replay time.

### Resilience properties

- Every derived projection is disposable and rebuildable from raw blobs + events.
- Each job is idempotent using content hash plus processor/configuration version.
- Failed tasks enter a visible quarantine/dead-letter state.
- Backups include raw artifacts, event records, graph/document collections, and configuration/version manifests.
- Human review is preserved as source data, not merely UI state.

## Delivery sequence

1. Epic 1: Archaeological Ledger
2. Epic 2: Markdown Evidence Extraction
3. Epic 3: Retrieval Substrate
4. Epic 6: Minimal Workbench (search + evidence view)
5. Epic 4: Temporal Idea Lineage
6. Epic 5: Redundancy and Tension Review
7. Expand workbench with timeline/map/review workflows

## Phase-one exit test

Phase 1 is complete when you can take one present-day note—say, the command-center / knowledge-graph system idea—and produce an inspectable report that shows:

- its earliest recovered evidence in Git/Markdown history;
- major section-level expressions and revision dates;
- candidate and accepted lineage transitions;
- branches where the idea split or acquired a distinct implementation;
- related notes ranked by lexical, semantic, and structural evidence;
- redundant/superseded notes separated from historically meaningful predecessors;
- at least one reviewed possible contradiction, with its scope and evidence;
- links to all source blobs and commits;
- enough telemetry to explain how that result was built.
