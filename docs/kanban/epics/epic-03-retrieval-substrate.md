---
id: 01900d7c-7f3a-7e8b-9c4d-000000000003
title: "Epic 3: Retrieval Substrate"
status: breakdown
type: epic
priority: high
phase: 1
design: docs/designs/phase-1-corpus-archaeology.md
size: 8
labels: [search, retrieval, embeddings, hybrid, decomposed]
---

# Epic 3: Retrieval Substrate

Build multi-signal retrieval over Markdown evidence units, with every result explainable.

## User outcome

“I can find notes by phrase, topic, conceptual similarity, headings, tags, date, repository, and Git history—and see why each result ranked.”

## Scope

Implement three channels:

- **Lexical:** full-text index over title, headings, body, tags, commit messages, and selected metadata.
- **Structural:** heading/path/link/tag/repository/temporal filters and overlap relationships.
- **Semantic:** embeddings at section/block level, plus versioned model metadata.
- **Hybrid ranker:** combines signals rather than treating vector similarity as truth.

Every result carries a score breakdown (lexical, semantic, structure, temporal, final) and evidence references.

## First benchmark

Create 30–50 questions from personal notes, such as:

- “Where did I first describe semantic gravity?”
- “Which notes argue for an event log as source of truth?”
- “Show me the early versions of the command-center idea.”
- “What did I mean by concept boundaries in namespaces?”
- “Find everything I wrote that might be a duplicate of this note.”

## Acceptance criteria

- Exact phrases and heading terms retrieve the expected sections.
- Semantic retrieval finds paraphrases that lexical retrieval misses.
- Filters can restrict search by repository, time interval, path, tag, and revision state.
- Every result exposes contributing signals and links to source evidence.
- Embedding model changes produce a new index/projection version; they do not silently overwrite historical results.
- A benchmark query set can report Recall@k, nDCG, latency, and source-coverage metrics.

## Next step

Design the index mapping and choose the first embedding model for section-level retrieval.

---
Decomposed into 6 stories (ENG-003A–ENG-003F). All children are ready. Epic moves to breakdown per board contract. --tasks-dir docs/kanban
---
