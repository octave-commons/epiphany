---
id: "01900d7c-7f3a-7e8b-9c4d-000000001503"
title: "ENG-005C: Detect deterministic redundancy and contradiction candidates"
status: done
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000005"
design: "docs/kanban/epics/epic-05-redundancy-tension-review.md"
points: 4
labels: [phase-1, duplication, contradiction, candidates]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001304"]
---
# ENG-005C: Detect deterministic redundancy and contradiction candidates

Classify pairs into :duplicate :near-duplicate :complementary :superseded :possible-contradiction :unclear using deterministic patterns (negation, mutually exclusive values, incompatible dates/decisions). Bounded-LLM claim comparison is a separate future card.

## Acceptance criteria

- Every proposed relationship carries at least two source spans and a score/rationale.
- Candidate volume and confidence thresholds are tunable.
- Classifier output can be evaluated against a human-labeled review set.
- Nothing is ever deleted or merged automatically.
