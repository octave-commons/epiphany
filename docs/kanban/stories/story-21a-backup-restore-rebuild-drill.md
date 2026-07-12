---
id: "01900d7c-7f3a-7e8b-9c4d-000000001902"
title: "ENG-021A: Prove backup, restore, and rebuild"
status: "in_progress"
type: "story"
priority: "P1"
phase: 1
parent: "story-21-recover-corpus-archaeology-view"
design: "docs/kanban/stories/story-21-recover-corpus-archaeology-view.md"
points: 3
labels: ["phase-1", "operations", "recovery", "durability"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001107", "01900d7c-7f3a-7e8b-9c4d-000000001301", "01900d7c-7f3a-7e8b-9c4d-000000001303"]
---

# ENG-021A: Prove backup, restore, and rebuild

Documented, scripted Mongo backup/restore plus index rebuild — verified by a drill, not asserted.

## Acceptance criteria

- Restore recovers registrations, observations, events, review decisions, and checkpoints.
- Lucene/vector indexes rebuild from Git + restored Mongo state via documented commands.
- A restore drill reproduces a previously exported evidence packet; inaccessible sources are recorded, not papered over.
- Cache/index loss demonstrably loses no source fact or user decision.

---
AUDIT 2026-07-12: status=in_progress — honest, keep it that way. Warning recorded before any done claim: epiphany.domain.backup/restore-drill (backup.clj:81-108) docstrings five stages (export, drop, import, re-export/compare, inaccessible-source check) but executes only export and returns {:drill-status :export-complete}. The card's own criteria ('verified by a drill, not asserted', 'inaccessible sources recorded, not papered over') are exactly what the stub fails. Also unrouted from the same review: import-from-file checks only :format — no manifest version/counts/checksum; logical-vs-physical collection-name mismatch may make inaccessible-sources inspect an empty list. Do not move past review until every documented stage executes in a test. Relations: requires ENG-017F (read/import validation); evidence: docs/notes/inbox-synthesis-2026-07-12-defect-inventory.md items 2, and claimed-set backup items. --tasks-dir docs/kanban
---
