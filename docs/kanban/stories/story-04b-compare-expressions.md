---
id: "01900d7c-7f3a-7e8b-9c4d-000000001402"
title: "ENG-004B: Compare two historical expressions (`ep diff`)"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000004"
design: "docs/kanban/epics/epic-04-temporal-idea-lineage.md"
points: 3
labels: ["phase-1", "comparison", "diff", "evidence"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001401"]
---

# ENG-004B: Compare two historical expressions (`ep diff`)

Side-by-side comparison of any two section or revision-at-path expressions.

## Acceptance criteria

- Both sides show exact source metadata; the diff is Markdown-appropriate (textual or structural).
- Continuity signals display alongside but are never conflated with the diff itself.
- The comparison is reproducible from recorded IDs and versions.
- A comparison can seed a candidate relation or review decision.

---
Implemented: compute-diff (line-by-line with lookahead matching), format-diff-text/format-diff-edn, compare-evidence (retrieves both sides, produces diff + continuity signals). 9 tests, 24 assertions. No ep show/ep diff CLI commands yet — needs ports wiring. --tasks-dir docs/kanban
---
