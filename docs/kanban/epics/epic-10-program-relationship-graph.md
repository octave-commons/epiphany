---
id: 01900d7c-7f3a-7e8b-9c4d-000000000011
title: "Epic 10: Program Relationship Graph"
status: icebox
type: epic
priority: high
phase: 2
design: docs/notes/design/phase-2-code-comprehension.md
size: 8
labels: [graph, relationships, code, provenance]
---

# Epic 10: Program Relationship Graph

Build a versioned, multi-layer graph of structural, semantic, temporal, and conceptual relationships across source artifacts.

## User outcome

“I can traverse from a concept to notes, then to code symbols, dependent namespaces, tests, co-changing files, and historical implementation decisions.”

## Relationship layers

- **Containment:** Repository → revision → namespace → var
- **Syntax:** Declaration → AST subtree / structural fingerprint
- **Dependencies:** Namespace → requires → namespace
- **References:** Symbol → calls/references → symbol
- **Configuration:** Service → reads → EDN/YAML/JSON key
- **Verification:** Test → verifies → function/namespace
- **Temporal:** Revision → changed-with → revision
- **Co-change:** File/symbol → co-changes-with → file/symbol
- **Conceptual:** Note concept → described-by/implemented-by → code unit
- **Runtime:** Service/function → observed-to-interact-with → service/function

## Scope

- Implement relationship provenance as a first-class requirement.
- Preserve source revision and temporal validity for edges.
- Separate observed, tool-derived, probabilistic inference, and user-accepted architectural relations.
- Build graph projections optimized for local traversal, dependency impact, historical evolution, concept-to-code retrieval, and cluster computation.

## Acceptance criteria

- Every edge has type, evidence source, source revision, confidence/status, and extraction version.
- Users can filter graph traversal by relation type and evidence status.
- A query can answer: “Which notes, symbols, tests, config entries, and historical commits are connected to this concept?”
- Historical queries return relationships valid at a selected commit or time interval.
- Rebuilding graph projections from the artifact/event ledger preserves IDs and does not erase human review decisions.
- Graph queries are bounded by hop, edge-type, time, and result budgets.

## Domain rule

A graph edge is not automatically an architectural claim. Most edges are evidence. Architecture is the reviewed interpretation of many edges.

## Next step

Design the relationship-type vocabulary and graph projection schema.
