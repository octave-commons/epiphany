---
id: "01900d7c-7f3a-7e8b-9c4d-000000001006"
title: "US-006: Detect potential history replacement"
status: "breakdown"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/phase-1-corpus-archaeology.md"
points: 3
labels: [git, ingestion, provenance, history-rewrite, audit, decomposed]
category: "stories"
---

# US-006: Detect potential history replacement

## Acceptance Criteria

- If a known ref changes to a newly observed tip without expected ancestry, the system emits a `:repository/history-rewrite-suspected` event.
- The system preserves previously observed commit/path/blob records.
- It schedules repository-family reassessment only for this exception, not during ordinary indexing.
- The UI/CLI identifies old tip, new tip, affected repository instance, detection reason, and time.
- No repository-family merge, split, deletion, or reassignment occurs automatically as a consequence.

## Notes

**As a corpus owner,** I want the system to preserve historical observations and call attention to a likely rewrite instead of silently treating a rewritten repository as ordinary new commits.

---

Decomposed into 1 story (ENG-001H). Do not implement directly. --tasks-dir docs/kanban
---
