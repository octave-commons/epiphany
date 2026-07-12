---
id: 01900d7c-7f3a-7e8b-9c4d-000000000004
title: "Epic 4: Temporal Idea Lineage"
status: breakdown
type: epic
priority: high
phase: 1
design: docs/designs/phase-1-corpus-archaeology.md
size: 8
labels: [lineage, graph, temporal, inference, decomposed]
---

# Epic 4: Temporal Idea Lineage

Infer candidate “same idea evolving over time” paths from revisions and extracted evidence, while preserving uncertainty.

## User outcome

“Starting from a current idea, I can walk backward through its likely predecessors and forward through branches, refinements, and descendants.”

## Scope

Build temporal graphs from:

- Git revisions
- Section containment
- Lexical similarity
- Semantic similarity
- Shared named concepts/tags
- Explicit links
- Commit co-occurrence
- Rename/move candidates

Produce candidate relations: `:continues`, `:refines`, `:splits-into`, `:merges-from`, `:references`, `:possibly-derived-from`, `:supersedes`.

Use deterministic candidate generation first. Add LLM analysis only after retrieval chooses a small evidence set; require structured output and provenance. Support human review: accept, reject, relabel, or annotate candidate relations.

## Design constraint

Lineage is not necessarily textual similarity. A later note may reject, clarify, operationalize, or split an earlier idea. Model “related historical transformation” rather than assuming similarity means continuity.

## Acceptance criteria

- The system can produce a ranked lineage path for a selected section.
- Each edge includes date ordering, evidence spans, contributing scores, and inference version.
- A user can inspect why a predecessor/descendant was suggested.
- Human accept/reject decisions become first-class events and influence later ranking.
- The graph never converts a candidate edge into a fact without explicit status.
- Lineage queries handle forks: one earlier idea can yield multiple descendants, and multiple earlier threads can converge.

## Next step

Design the candidate-generation pipeline and the lineage edge schema.

---
Decomposed into 4 stories (ENG-004A–ENG-004D). All children are ready. Epic moves to breakdown per board contract. --tasks-dir docs/kanban
---
