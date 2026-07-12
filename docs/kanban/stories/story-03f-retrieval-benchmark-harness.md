---
id: "01900d7c-7f3a-7e8b-9c4d-000000001306"
title: "ENG-003F: Stand up the retrieval benchmark harness"
status: done
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000003"
design: "docs/kanban/epics/epic-03-retrieval-substrate.md"
points: 3
labels: [phase-1, benchmark, evaluation, retrieval]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001304"]
---
# ENG-003F: Stand up the retrieval benchmark harness

A checked-in query set (30–50 personal-corpus questions) and a runner that scores retrieval quality.

## Acceptance criteria

- Benchmark reports Recall@k, nDCG, and latency per retrieval mode.
- Query set and expected evidence live in version control as fixtures.
- Reports are comparable across index/model versions so regressions are visible.
