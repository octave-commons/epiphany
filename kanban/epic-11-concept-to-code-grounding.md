---
id: 01900d7c-7f3a-7e8b-9c4d-000000000012
title: "Epic 11: Concept-to-Code Grounding"
status: incoming
type: epic
priority: high
phase: 2
design: docs/notes/design/phase-2-code-comprehension.md
size: 8
labels: [grounding, concepts, code, links]
---

# Epic 11: Concept-to-Code Grounding

Link the human concepts expressed in Markdown to the code and configuration structures that implement, mention, test, or contradict them.

## User outcome

“I can start with an idea in my notes and find its implementation, tests, relevant configs, and historical transitions—or learn that it was never implemented.”

## Scope

Derive candidate links between Phase 1 concept/section units and Phase 2 code units using:

- lexical overlap
- embeddings
- docstrings/comments
- namespace/module naming
- keyword/configuration vocabulary
- Git temporal proximity
- commit-message overlap
- explicit links
- user labels

Classify candidate relations: `:implements`, `:describes`, `:tests`, `:configures`, `:depends-on-concept`, `:obsolete-implementation-of`, `:contradicts-design`, `:possibly-related`.

Support reverse queries: code → relevant design notes; note → implementation candidates; concept → tests/config/services.

## Acceptance criteria

- Every proposed code-concept relation contains evidence from both sides.
- A user can distinguish “the code uses the same vocabulary” from “this implements the design.”
- Human decisions on relationships are retained as events and incorporated into later candidate ranking.
- Temporal ordering is visible: a note can precede, follow, or co-evolve with an implementation.
- The system can identify “orphan concepts” with no accepted implementation and “orphan implementations” with no explanatory notes.
- The system never presents a semantic retrieval hit as confirmed implementation without status/provenance.

## Example question

“Which parts of the OpenPlanner/Graph-Weaver code were actually motivated by the semantic-gravity and ACO notes, and which parts only became adjacent because they evolved in the same repository?”

## Next step

Design the concept-to-code candidate scoring and review workflow.
