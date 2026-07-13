---
id: "01900d7c-7f3a-7e8b-9c4d-000000001501"
title: "ENG-005A: Record review decisions as append-only events"
status: "in_progress"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000005"
design: "docs/kanban/epics/epic-05-redundancy-tension-review.md"
points: 3
labels: ["phase-1", "review", "events", "provenance"]
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

---
AUDIT 2026-07-13 (found while investigating the ep show/diff/trace/inbox/export pattern): this card is marked done with ZERO completion-evidence comment -- the only card of the six affected that no prior audit caught. Independently verified it is not done: grep -rn 'review-decision|record-decision' src/epiphany/infra/ (in_memory.clj, mongo.clj, law/ports.clj) returns nothing. domain/review.clj has real pure functions (make-decision, by-candidate, by-decision-type, by-time-range, rejected-candidates, visible-decisions) and test/epiphany/domain/review_test.clj presumably exercises them in isolation, but there is no port anywhere that durably persists or queries a review decision. The AC bullet 'Decisions are queryable by candidate, relation type, and time' is unmet -- there is nothing to query. This is the actual root blocker for ENG-005B (ep inbox) and ENG-005F (ep export), which both depend on decision/candidate storage that was never built, despite this card claiming it's done. Demoting done->in_progress. Real remaining work: an observations-port write op (e.g. :record-review-decision!) plus a query capability, wired through the same schema-registry enforcement pattern as ENG-017A-C. --tasks-dir docs/kanban
---
