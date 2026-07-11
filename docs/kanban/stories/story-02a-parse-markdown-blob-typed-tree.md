---
id: "01900d7c-7f3a-7e8b-9c4d-000000001201"
title: "ENG-002A: Parse a Markdown blob into a typed tree with source spans"
status: ready
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000002"
design: "docs/kanban/epics/epic-02-markdown-evidence-extraction.md"
points: 4
labels: [phase-1, markdown, parsing, spans]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000000000-a"]
---
# ENG-002A: Parse a Markdown blob into a typed tree with source spans

Pure parsing (`shape/`): one Markdown string in, one typed block tree out. Decide flexmark vs commonmark-java here and record why in the card.

## Acceptance criteria

- Headings preserve hierarchy; paragraphs, lists, tables, quotes, and fenced code are typed blocks; unsupported constructs become diagnostics, not crashes.
- Front matter is parsed separately from the body.
- Every node carries byte and line spans; the exact raw source slice is recoverable from blob + span.
- Output is deterministic for the same input and parser version.
- Unit-tested only — no Git, no Mongo.
