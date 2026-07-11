---
id: 01900d7c-7f3a-7e8b-9c4d-000000000013
title: "Epic 12: Architectural Boundary Inference"
status: incoming
type: epic
priority: high
phase: 2
design: docs/notes/design/phase-2-code-comprehension.md
size: 8
labels: [architecture, boundaries, clustering, recommendations]
---

# Epic 12: Architectural Boundary Inference

Identify candidate subsystem and namespace boundaries using multiple independent views of the codebase, then turn them into human-reviewable architectural hypotheses.

## User outcome

“I can see why a group of files belongs together conceptually, why a namespace is likely misplaced, and what a low-risk reorganization would look like.”

## Signals for multi-view clustering

- directed namespace/import dependency
- resolved symbol reference
- shared protocol/multimethod participation
- shared domain vocabulary
- docstring/comment embeddings
- AST structural patterns
- shared config keys
- shared tests
- co-change history
- temporal co-emergence
- user labels and accepted concept-to-code links
- complexity/lint facts as weak diagnostic signals

## Candidate boundary relations

`:belongs-to-subsystem`, `:bridge-module`, `:adapter`, `:boundary-violation`, `:cyclic-coupling`, `:misplaced-by-concept`, `:overloaded-namespace`, `:candidate-extraction`, `:candidate-merge`.

## Acceptance criteria

- Every suggested cluster includes a signal breakdown rather than just a generated name.
- A user can view how cluster membership changes if one signal family is removed.
- The system identifies bridge modules separately from strongly cohesive clusters.
- Recommendations include a confidence level, expected effect, affected symbols/tests, and a proposed safe investigation—not an autonomous move.
- Users can pin accepted boundaries and label intentional exceptions.
- The system evaluates recommendations against historical refactors where possible.
- Boundary recommendations can be exported as an ADR/research memo draft with linked evidence.

## Critical principle

The filesystem is one coordinate system. Dependency topology, semantic vocabulary, tests, runtime behavior, and time are other coordinate systems. A useful boundary recommendation appears where those views converge—or where their mismatch reveals hidden architectural debt.

## Next step

Design the multi-view similarity model and clustering algorithm.
