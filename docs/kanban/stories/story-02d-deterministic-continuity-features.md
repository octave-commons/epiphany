---
id: "01900d7c-7f3a-7e8b-9c4d-000000001204"
title: "ENG-002D: Compute deterministic continuity features per revision transition"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000002"
design: "docs/kanban/epics/epic-02-markdown-evidence-extraction.md"
points: 5
labels: ["phase-1", "continuity", "features", "provenance"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001202"]
---

# ENG-002D: Compute deterministic continuity features per revision transition

For adjacent revisions of a path, store raw signals and a scored continuity result under a versioned policy. Deterministic only — no LLM, no embeddings.

## Acceptance criteria

- Raw measurements stored per transition: text similarity, front-matter delta, explicit-link overlap, time gap, shared name/token overlap.
- The continuity score names the policy/configuration version that produced it.
- Signals and scores are stored separately from observed Git facts and are individually inspectable.
- Recomputing under a new policy version creates new results; prior results and review decisions are untouched.
