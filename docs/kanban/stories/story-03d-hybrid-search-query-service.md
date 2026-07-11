---
id: "01900d7c-7f3a-7e8b-9c4d-000000001304"
title: "ENG-003D: Compose the hybrid search query service"
status: ready
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000003"
design: "docs/kanban/epics/epic-03-retrieval-substrate.md"
points: 4
labels: [phase-1, search, hybrid, ranking]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001301", "01900d7c-7f3a-7e8b-9c4d-000000001303"]
---
# ENG-003D: Compose the hybrid search query service

One query boundary with lexical-only, semantic-only, and hybrid modes plus structural filters.

## Acceptance criteria

- Filters: repository family/instance, exact path prefix, date range, ref where available.
- Every result exposes source path, commit, heading path, exact excerpt span, retrieval mode, and per-signal score components.
- Mode selection is explicit; hybrid rank combination is a versioned, documented function.
- This is an application service — no CLI/HTTP concerns in it.
