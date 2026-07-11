---
id: 01900d7c-7f3a-7e8b-9c4d-000000000014
title: "Epic 13: Code Archaeology Workbench"
status: icebox
type: epic
priority: high
phase: 2
design: docs/notes/design/phase-2-code-comprehension.md
size: 8
labels: [ui, workbench, code, visualization]
---

# Epic 13: Code Archaeology Workbench

Extend the Phase 1 research workbench with source and architectural-comprehension workflows.

## User outcome

“I can explore the codebase as a living historical system rather than a directory tree, and move from a question to evidence to a reviewable design hypothesis.”

## Core views

- **Namespace map:** directed dependency graph, layering, cycles, inbound/outbound pressure.
- **Symbol explorer:** definition, references, callers/callees where supported, historical changes, tests, docs.
- **Concept-to-code view:** Markdown concepts and implementation candidates with evidence/status.
- **Co-change timeline:** files, namespaces, and symbols that changed together across commits.
- **Structural motif explorer:** AST pattern search and matching subtrees.
- **Boundary map:** candidate clusters, bridges, exceptions, and signal breakdown.
- **Historical architecture slider:** select a commit/time range and compare dependency/cluster structure.
- **Refactor review packet:** proposed investigation, impacted units, evidence, tests, and rollback strategy.

## Acceptance criteria

- From a note, the user can reach a related code symbol and inspect the evidence in no more than two pivots.
- From a namespace, the user can identify its core concepts, strongest dependencies, co-change peers, tests, and architectural candidates.
- Every recommendation separates observed facts from inferred interpretation.
- Graph visualizations support filtering, search, time range, relation-type selection, and evidence drill-down.
- Views remain usable at scale through progressive disclosure; do not render the whole graph by default.
- A user can export a bounded investigation packet rather than screenshotting an unbounded graph.

## Next step

Design the code workbench views and navigation model.
