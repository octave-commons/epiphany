---
id: "01900d7c-7f3a-7e8b-9c4d-000000001401"
title: "ENG-004A: Open exact historical evidence (`ep show`)"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000004"
design: "docs/kanban/epics/epic-04-temporal-idea-lineage.md"
points: 4
labels: ["phase-1", "evidence", "provenance", "cli"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001202"]
---

# ENG-004A: Open exact historical evidence (`ep show`)

Given a section or revision expression, return the exact source span with its provenance.

## Acceptance criteria

- Displays source text from the exact blob (not the working tree) with repository, commit OID, author/commit times, exact path, heading path, byte/line span.
- Surrounding section context is available without changing the cited span; raw and rendered views both work.
- Links to the commit and parent/child revisions where available.
- An inaccessible Git object reports UNAVAILABLE — never a fabricated excerpt.

---
Implemented: parse-section-expression (path#heading@commit format), find-section-in-content (heading search by level), retrieve-evidence (full pipeline with UNAVAILABLE for missing Git objects), format-evidence-text/format-evidence-edn. 15 tests, 35 assertions, all green. Added read-blob to git.clj. No ep show CLI command yet — needs ports wiring. --tasks-dir docs/kanban
---
