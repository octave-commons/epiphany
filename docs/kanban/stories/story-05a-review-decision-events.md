---
id: "01900d7c-7f3a-7e8b-9c4d-000000001501"
title: "ENG-005A: Record review decisions as append-only events"
status: done
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000005"
design: "docs/kanban/epics/epic-05-redundancy-tension-review.md"
points: 3
labels: [phase-1, review, events, provenance]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001403"]
---
# ENG-005A: Record review decisions as append-only events

Accept, reject, relabel, defer, annotate, or mark do-not-suggest — durably and idempotently.

## Acceptance criteria

- A review action appends an event; it never rewrites the candidate or Git evidence.
- Rejected candidates remain in audit mode; do-not-suggest suppresses similar candidates in default views.
- Events carry request IDs; retries do not duplicate decisions.
- Decisions are queryable by candidate, relation type, and time.
