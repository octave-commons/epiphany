---
id: "01900d7c-7f3a-7e8b-9c4d-000000001506"
title: "ENG-005F: Export an evidence packet (`ep export`)"
status: ready
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000005"
design: "docs/kanban/epics/epic-05-redundancy-tension-review.md"
points: 3
labels: [phase-1, export, evidence, packet]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001404", "01900d7c-7f3a-7e8b-9c4d-000000001504"]
---
# ENG-005F: Export an evidence packet (`ep export`)

Export selected results, trace nodes, and decisions as Markdown plus EDN/JSON.

## Acceptance criteria

- The packet separates observed facts, inferred candidates, accepted interpretations, and open questions.
- Every claim carries an evidence reference or an explicit interpretation/no-direct-source label.
- Identifiers (resource ID, commit OID, path, spans, versions) suffice to reproduce every lookup locally.
